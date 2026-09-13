package net.lax1dude.eaglercraft.backend.server.standalone;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerJoinListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerPlayerInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineData;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayerInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.lax1dude.eaglercraft.backend.server.adapter.PipelineAttributes;
import net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData;

final class StandaloneBackendBridge extends ChannelInboundHandlerAdapter {
	private final EventLoopGroup worker;
	private final StandaloneConfig config;
	private final IEaglerXServerPlayerInitializer<IPipelineData, Object, StandalonePlayer> playerInitializer;
	private final IEaglerXServerJoinListener<StandalonePlayer> joinListener;
	private Channel backend;
	private IPlatformServer<StandalonePlayer> selectedServer;

	StandaloneBackendBridge(EventLoopGroup worker, StandaloneConfig config,
			IEaglerXServerPlayerInitializer<IPipelineData, Object, StandalonePlayer> playerInitializer,
			IEaglerXServerJoinListener<StandalonePlayer> joinListener) {
		this.worker = worker; this.config = config; this.playerInitializer = playerInitializer; this.joinListener = joinListener;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object message) {
		if (!(message instanceof ByteBuf buffer)) {
			ctx.fireChannelRead(message);
			return;
		}
		if (backend == null) {
			NettyPipelineData data = ctx.channel().attr(PipelineAttributes.<IPipelineData>pipelineData()).get() instanceof NettyPipelineData p ? p : null;
			String name = data != null && config.getServers().containsKey(data.requestedServer) ? data.requestedServer : config.getDefaultServer();
			selectedServer = new ServerLookup(config.getServers()).get(name);
			connect(ctx, selectedServer, buffer);
			return;
		}
		backend.writeAndFlush(buffer.retain());
		buffer.release();
	}

	private void connect(ChannelHandlerContext ctx, IPlatformServer<StandalonePlayer> server, ByteBuf first) {
		String address = ((StandaloneServer) server).getAddress();
		int split = address.lastIndexOf(':');
		Bootstrap bootstrap = new Bootstrap().group(worker).channel(io.netty.channel.socket.nio.NioSocketChannel.class)
				.remoteAddress(new InetSocketAddress(address.substring(0, split), Integer.parseInt(address.substring(split + 1))));
		bootstrap.handler(new ChannelInitializer<Channel>() {
			protected void initChannel(Channel channel) {
				channel.pipeline().addLast("frame-decoder", new VarIntFrameDecoder());
				channel.pipeline().addLast("frame-encoder", new VarIntFrameEncoder());
				channel.pipeline().addLast("backend-to-eagler", new ChannelInboundHandlerAdapter() {
					public void channelRead(ChannelHandlerContext backendCtx, Object message) {
						ctx.fireChannelRead(((ByteBuf) message).retain());
						((ByteBuf) message).release();
					}
					public void channelInactive(ChannelHandlerContext backendCtx) { ctx.close(); }
				});
			}
		});
		bootstrap.connect().addListener(future -> {
			if (!future.isSuccess()) { first.release(); ctx.close(); return; }
			backend = ((io.netty.channel.ChannelFuture) future).channel();
			backend.writeAndFlush(first.retain());
			first.release();
			initializePlayer(ctx);
		});
	}

	private void initializePlayer(ChannelHandlerContext ctx) {
		NettyPipelineData data = (NettyPipelineData) ctx.channel().attr(PipelineAttributes.<IPipelineData>pipelineData()).get();
		if (data == null || playerInitializer == null) return;
		StandalonePlayer player = new StandalonePlayer(ctx.channel(), data.uuid, data.username, selectedServer);
		playerInitializer.initializePlayer(new IPlatformPlayerInitializer<IPipelineData, Object, StandalonePlayer>() {
			public IPlatformPlayer<StandalonePlayer> getPlayer() { return player; }
			public IPipelineData getPipelineAttachment() { return data; }
			public void setPlayerAttachment(Object attachment) { player.setPlayerAttachment(attachment); }
			public void complete() { if (joinListener != null) joinListener.handlePostConnect(player, selectedServer); }
			public void cancel() { ctx.close(); }
		});
	}

	private static final class ServerLookup {
		private final Map<String, String> addresses;
		ServerLookup(Map<String, String> addresses) { this.addresses = addresses; }
		IPlatformServer<StandalonePlayer> get(String name) { return new StandaloneServer(name, addresses.get(name)); }
	}

	private static final class VarIntFrameDecoder extends ByteToMessageDecoder {
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) {
			in.markReaderIndex(); int value = 0; int shift = 0;
			while (in.isReadable() && shift < 35) { int b = in.readUnsignedByte(); value |= (b & 0x7f) << shift; if ((b & 0x80) == 0) { if (in.readableBytes() < value) { in.resetReaderIndex(); return; } out.add(in.readRetainedSlice(value)); return; } shift += 7; }
			ctx.close();
		}
	}
	private static final class VarIntFrameEncoder extends MessageToByteEncoder<ByteBuf> {
		protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) { int value = msg.readableBytes(); while ((value & ~0x7f) != 0) { out.writeByte((value & 0x7f) | 0x80); value >>>= 7; } out.writeByte(value); out.writeBytes(msg, msg.readerIndex(), msg.readableBytes()); }
	}
}
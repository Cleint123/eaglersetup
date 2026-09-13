package net.lax1dude.eaglercraft.backend.server.standalone;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.lax1dude.eaglercraft.backend.server.adapter.EnumAdapterPlatformType;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerCommandType;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerJoinListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerLoginInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerMessageChannel;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerNettyPipelineInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerPlayerCountHandler;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerPlayerInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineComponent;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineData;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatform;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformCommandSender;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentHelper;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformNettyPipelineInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformScheduler;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineComponent.EnumPipelineComponent;
import net.lax1dude.eaglercraft.backend.server.adapter.PipelineAttributes;
import net.lax1dude.eaglercraft.backend.server.adapter.JavaLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchAdapter;
import net.lax1dude.eaglercraft.backend.server.base.EaglerXServer;
import net.lax1dude.eaglercraft.backend.server.base.EaglerListener;
import net.lax1dude.eaglercraft.backend.server.config.EnumConfigFormat;

final class StandalonePlatform implements IPlatform<StandalonePlayer> {
	private final StandaloneConfig config;
	private final IPlatformLogger logger = new JavaLogger(Logger.getLogger("EaglerXServer"));
	private final EventLoopGroup boss = new NioEventLoopGroup(1);
	private final EventLoopGroup worker = new NioEventLoopGroup();
	private final StandaloneScheduler scheduler = new StandaloneScheduler();
	private final StandaloneComponentHelper components = new StandaloneComponentHelper();
	private final StandaloneEventDispatchAdapter events = new StandaloneEventDispatchAdapter();
	private final Map<String, IPlatformServer<StandalonePlayer>> servers;
	private IEaglerXServerNettyPipelineInitializer<IPipelineData> pipelineInitializer;
	private IEaglerXServerPlayerInitializer<IPipelineData, Object, StandalonePlayer> playerInitializer;
	private IEaglerXServerJoinListener<StandalonePlayer> joinListener;
	private IEaglerXServerListener listener;

	StandalonePlatform(StandaloneConfig config) { this.config = config; this.servers = StandaloneServer.createAll(this, config.getServers()); }

	void start() throws IOException {
		EaglerXServer<StandalonePlayer> server = new EaglerXServer<>();
		server.load(new IPlatform.InitNonProxying<StandalonePlayer>() {
			public void setOnServerEnable(Runnable enable) { }
			public void setOnServerDisable(Runnable disable) { }
			public void setEaglerPlayerChannels(Collection<IEaglerXServerMessageChannel<StandalonePlayer>> channels) { }
			@SuppressWarnings("unchecked") public void setPipelineInitializer(IEaglerXServerNettyPipelineInitializer<? extends IPipelineData> value) { pipelineInitializer = (IEaglerXServerNettyPipelineInitializer<IPipelineData>) value; }
			public void setConnectionInitializer(IEaglerXServerLoginInitializer<? extends IPipelineData> value) { }
			@SuppressWarnings("unchecked") public void setPlayerInitializer(IEaglerXServerPlayerInitializer<? extends IPipelineData, ?, StandalonePlayer> value) { playerInitializer = (IEaglerXServerPlayerInitializer<IPipelineData, Object, StandalonePlayer>) value; }
			public void setServerJoinListener(IEaglerXServerJoinListener<StandalonePlayer> value) { joinListener = value; }
			public void setCommandRegistry(Collection<IEaglerXServerCommandType<StandalonePlayer>> commands) { }
			public IPlatform<StandalonePlayer> getPlatform() { return StandalonePlatform.this; }
			public void setEaglerListener(IEaglerXServerListener value) { listener = value; }
			public SocketAddress getListenerAddress() { return parseAddress(config.getBind()); }
		});
		server.getWebServer().refreshBuiltinPages();
		List<IPipelineComponent> components = new ArrayList<>();
		components.add(new IPipelineComponent() {
			private final ChannelHandler handler = new io.netty.channel.ChannelInboundHandlerAdapter();
			public EnumPipelineComponent getIdentifiedType() { return EnumPipelineComponent.UNIDENTIFIED; }
			public String getName() { return "standalone"; }
			public ChannelHandler getHandle() { return handler; }
		});
		ServerBootstrap bootstrap = server.bootstrapServer(parseAddress(config.getBind()));
		bootstrap.childHandler(new ChannelInitializer<Channel>() {
			protected void initChannel(Channel channel) {
				channel.pipeline().addLast("standalone", new io.netty.channel.ChannelInboundHandlerAdapter());
				pipelineInitializer.initialize(new IPlatformNettyPipelineInitializer<IPipelineData>() {
					public void setAttachment(IPipelineData data) { channel.attr(PipelineAttributes.<IPipelineData>pipelineData()).set(data); }
					public List<IPipelineComponent> getPipeline() { return components; }
					public IEaglerXServerListener getListener() { return listener; }
					public Consumer<SocketAddress> realAddressHandle() { return ignored -> { }; }
					public Channel getChannel() { return channel; }
				});
				boolean tlsEnabled = listener instanceof EaglerListener eaglerListener && eaglerListener.isTLSEnabled();
				channel.pipeline().addFirst("standalone-raw-status",
						new StandaloneRawStatusHandler(config.getBind(), tlsEnabled));
				channel.pipeline().addLast("standalone-backend", new StandaloneBackendBridge(worker, config, playerInitializer, joinListener));
			}
		}).bind(parseAddress(config.getBind())).syncUninterruptibly().channel().closeFuture().syncUninterruptibly();
	}

	private static InetSocketAddress parseAddress(String address) {
		int split = address.lastIndexOf(':');
		if (split <= 0 || split == address.length() - 1) throw new IllegalArgumentException("Invalid address: " + address);
		return new InetSocketAddress(address.substring(0, split), Integer.parseInt(address.substring(split + 1)));
	}
	public EnumAdapterPlatformType getType() { return EnumAdapterPlatformType.STANDALONE; }
	public String getVersion() { return "standalone"; }
	public Class<StandalonePlayer> getPlayerClass() { return StandalonePlayer.class; }
	public String getPluginId() { return "EaglerXServer-Standalone"; }
	public File getDataFolder() { return new File("."); }
	public IPlatformLogger logger() { return logger; }
	public IPlatformCommandSender<StandalonePlayer> getConsole() { return new StandaloneConsole(logger); }
	public void forEachPlayer(Consumer<IPlatformPlayer<StandalonePlayer>> callback) { }
	public IPlatformPlayer<StandalonePlayer> getPlayer(StandalonePlayer playerObj) { return playerObj; }
	public IPlatformPlayer<StandalonePlayer> getPlayer(String username) { return null; }
	public IPlatformPlayer<StandalonePlayer> getPlayer(UUID uuid) { return null; }
	public Collection<IPlatformPlayer<StandalonePlayer>> getAllPlayers() { return Collections.emptyList(); }
	public Map<String, IPlatformServer<StandalonePlayer>> getRegisteredServers() { return servers; }
	public IPlatformServer<StandalonePlayer> getServer(String serverName) { return servers.get(serverName); }
	public IEventDispatchAdapter<StandalonePlayer, ?> eventDispatcher() { return events; }
	public IPlatformScheduler getScheduler() { return scheduler; }
	public Set<EnumConfigFormat> getConfigFormats() { return EnumConfigFormat.getSupported(); }
	public IPlatformComponentHelper getComponentHelper() { return components; }
	public boolean isOnlineMode() { return false; }
	public boolean isModernPluginChannelNamesOnly() { return false; }
	public int getPlayerTotal() { return 0; }
	public int getPlayerMax() { return 0; }
	public void setPlayerCountHandler(IEaglerXServerPlayerCountHandler handler) { }
	public Bootstrap setChannelFactory(Bootstrap bootstrap, SocketAddress address) { return bootstrap.channel(NioSocketChannel.class); }
	public ServerBootstrap setServerChannelFactory(ServerBootstrap bootstrap, SocketAddress address) { return bootstrap.channel(NioServerSocketChannel.class); }
	public EventLoopGroup getBossEventLoopGroup() { return boss; }
	public EventLoopGroup getWorkerEventLoopGroup() { return worker; }
}
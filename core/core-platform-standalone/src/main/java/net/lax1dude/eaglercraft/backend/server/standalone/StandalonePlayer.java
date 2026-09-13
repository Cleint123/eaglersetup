package net.lax1dude.eaglercraft.backend.server.standalone;

import java.net.SocketAddress;
import java.util.UUID;

import io.netty.channel.Channel;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;

final class StandalonePlayer implements IPlatformPlayer<StandalonePlayer> {
	private final Channel channel;
	private final UUID uuid;
	private final String username;
	private final IPlatformServer<StandalonePlayer> server;
	private Object attachment;
	StandalonePlayer(Channel channel, UUID uuid, String username, IPlatformServer<StandalonePlayer> server) {
		this.channel = channel; this.uuid = uuid; this.username = username; this.server = server;
	}
	public StandalonePlayer getPlayerObject() { return this; }
	public Channel getChannel() { return channel; }
	public IPlatformServer<StandalonePlayer> getServer() { return server; }
	public String getUsername() { return username; }
	public UUID getUniqueId() { return uuid; }
	public SocketAddress getSocketAddress() { return channel.remoteAddress(); }
	public int getMinecraftProtocol() { return 340; }
	public boolean isConnected() { return channel.isActive(); }
	public boolean isOnlineMode() { return false; }
	public String getMinecraftBrand() { return null; }
	public String getTexturesProperty() { return null; }
	public void sendDataClient(String channel, byte[] message) { }
	public void sendDataBackend(String channel, byte[] message) { }
	public boolean isSetViewDistanceSupportedPaper() { return false; }
	public void setViewDistancePaper(int distance) { }
	public void sendMessage(String message) { }
	public void disconnect() { channel.close(); }
	public void disconnect(String kickMessage) { channel.close(); }
	public <ComponentObject> void disconnect(ComponentObject kickMessage) { channel.close(); }
	@SuppressWarnings("unchecked") public <T> T getPlayerAttachment() { return (T) attachment; }
	void setPlayerAttachment(Object attachment) { this.attachment = attachment; }
	public boolean checkPermission(String permission) { return true; }
	public <ComponentObject> void sendMessage(ComponentObject component) { }
	public boolean isPlayer() { return true; }
	public IPlatformPlayer<StandalonePlayer> asPlayer() { return this; }
}
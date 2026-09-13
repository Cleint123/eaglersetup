package net.lax1dude.eaglercraft.backend.server.standalone;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformCommandSender;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;

final class StandaloneConsole implements IPlatformCommandSender<StandalonePlayer> {
	private final IPlatformLogger logger;
	StandaloneConsole(IPlatformLogger logger) { this.logger = logger; }
	public boolean checkPermission(String permission) { return true; }
	public <ComponentObject> void sendMessage(ComponentObject component) { logger.info(String.valueOf(component)); }
	public boolean isPlayer() { return false; }
	public IPlatformPlayer<StandalonePlayer> asPlayer() { return null; }
}
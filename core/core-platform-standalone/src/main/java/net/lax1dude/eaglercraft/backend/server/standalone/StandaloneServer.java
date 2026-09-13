package net.lax1dude.eaglercraft.backend.server.standalone;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;

final class StandaloneServer implements IPlatformServer<StandalonePlayer> {
	private final String name;
	private final String address;
	StandaloneServer(String name, String address) { this.name = name; this.address = address; }
	static Map<String, IPlatformServer<StandalonePlayer>> createAll(StandalonePlatform platform, Map<String, String> values) {
		Map<String, IPlatformServer<StandalonePlayer>> result = new LinkedHashMap<>();
		values.forEach((name, address) -> result.put(name, new StandaloneServer(name, address)));
		return Collections.unmodifiableMap(result);
	}
	String getAddress() { return address; }
	public boolean isEaglerRegistered() { return true; }
	public String getServerConfName() { return name; }
	public Collection<IPlatformPlayer<StandalonePlayer>> getAllPlayers() { return Collections.emptyList(); }
	public void forEachPlayer(Consumer<IPlatformPlayer<StandalonePlayer>> callback) { }
}
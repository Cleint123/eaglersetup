/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 */

package net.lax1dude.eaglercraft.backend.server.standalone;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;

public final class StandaloneConfig {

	private final String bind;
	private final Map<String, String> servers;
	private final String defaultServer;

	private StandaloneConfig(String bind, Map<String, String> servers, String defaultServer) {
		this.bind = bind;
		this.servers = Collections.unmodifiableMap(servers);
		this.defaultServer = defaultServer;
	}

	public static StandaloneConfig load(File file) throws IOException {
		if (!file.isFile()) {
			throw new IOException("Config file not found: " + file.getAbsolutePath());
		}
		CommentedFileConfig config = CommentedFileConfig.builder(file).sync().preserveInsertionOrder()
				.onFileNotFound(FileNotFoundAction.READ_NOTHING).build();
		try {
			config.load();
			String bind = config.getOrElse("bind", "0.0.0.0:8081");
			Object tryValue = config.get("try");
			String defaultServer;
			if (tryValue instanceof Iterable<?> values && values.iterator().hasNext()) {
				defaultServer = values.iterator().next().toString();
			} else {
				defaultServer = tryValue != null ? tryValue.toString() : "lobby";
			}
			Map<String, String> servers = new LinkedHashMap<>();
			CommentedConfig serverConfig = config.get("servers");
			if (serverConfig != null) {
				for (String key : serverConfig.valueMap().keySet()) {
					Object value = serverConfig.get(key);
					if (value != null) {
						servers.put(key, value.toString());
					}
				}
			}
			if (servers.isEmpty()) {
				throw new IOException("No backend servers are configured in " + file.getAbsolutePath());
			}
			if (!servers.containsKey(defaultServer)) {
				throw new IOException("Default backend is not present in [servers]: " + defaultServer);
			}
			return new StandaloneConfig(bind, servers, defaultServer);
		} finally {
			config.close();
		}
	}

	public String getBind() {
		return bind;
	}

	public Map<String, String> getServers() {
		return servers;
	}

	public String getDefaultServer() {
		return defaultServer;
	}

}
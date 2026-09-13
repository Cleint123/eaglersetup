/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 */

package net.lax1dude.eaglercraft.backend.server.standalone;

import java.io.File;
import java.io.IOException;

public final class StandaloneMain {

	private StandaloneMain() {
	}

	public static void main(String[] args) {
		File configFile = new File(args.length > 0 ? args[0] : "velocity.toml");
		try {
			System.setProperty("eaglerxserver.singleConfigFile", configFile.getPath());
			StandaloneConfig config = StandaloneConfig.load(configFile);
			new StandalonePlatform(config).start();
		} catch (IOException | RuntimeException ex) {
			System.err.println("Could not start standalone Eagler proxy: " + ex.getMessage());
			ex.printStackTrace(System.err);
			System.exit(1);
		}
	}

}
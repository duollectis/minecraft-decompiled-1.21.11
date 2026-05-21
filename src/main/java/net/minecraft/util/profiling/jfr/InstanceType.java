package net.minecraft.util.profiling.jfr;

import net.minecraft.server.MinecraftServer;

/**
 * {@code InstanceType}.
 */
public enum InstanceType {
	CLIENT("client"),
	SERVER("server");

	private final String name;

	private InstanceType(final String name) {
		this.name = name;
	}

	/**
	 * Get.
	 *
	 * @param server server
	 *
	 * @return InstanceType — 
	 */
	public static InstanceType get(MinecraftServer server) {
		return server.isDedicated() ? SERVER : CLIENT;
	}

	public String getName() {
		return this.name;
	}
}

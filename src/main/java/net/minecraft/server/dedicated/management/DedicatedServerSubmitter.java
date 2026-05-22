package net.minecraft.server.dedicated.management;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Класс Dedicated Server Submitter.
 */
public class DedicatedServerSubmitter implements Submitter {

	private final MinecraftDedicatedServer server;

	public DedicatedServerSubmitter(MinecraftDedicatedServer server) {
		this.server = server;
	}

	@Override
	public <V> CompletableFuture<V> submit(Supplier<V> task) {
		return this.server.submit(task);
	}

	@Override
	public CompletableFuture<Void> submit(Runnable task) {
		return this.server.submit(task);
	}
}

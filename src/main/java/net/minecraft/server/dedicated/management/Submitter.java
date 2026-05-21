package net.minecraft.server.dedicated.management;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code Submitter}.
 */
public interface Submitter {

	<V> CompletableFuture<V> submit(Supplier<V> task);

	CompletableFuture<Void> submit(Runnable task);
}

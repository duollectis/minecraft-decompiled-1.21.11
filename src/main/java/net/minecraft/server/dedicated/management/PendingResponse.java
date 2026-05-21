package net.minecraft.server.dedicated.management;

import com.google.gson.JsonElement;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * {@code PendingResponse}.
 */
public record PendingResponse<Result>(
		RegistryEntry.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method,
		CompletableFuture<Result> resultFuture,
		long timeoutTime
) {

	public void handleResponse(JsonElement result) {
		try {
			Result object = (Result) this.method.value().decodeResult(result);
			this.resultFuture.complete(Objects.requireNonNull(object));
		}
		catch (Exception var3) {
			this.resultFuture.completeExceptionally(var3);
		}
	}

	public boolean shouldTimeout(long time) {
		return time > this.timeoutTime;
	}
}

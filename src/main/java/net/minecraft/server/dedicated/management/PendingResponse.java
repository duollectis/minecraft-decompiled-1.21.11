package net.minecraft.server.dedicated.management;

import com.google.gson.JsonElement;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Класс Pending Response.
 */
public record PendingResponse<Result>(
		RegistryEntry.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method,
		CompletableFuture<Result> resultFuture,
		long timeoutTime
) {

	/**
	 * Обрабатывает response.
	 *
	 * @param result result
	 */
	public void handleResponse(JsonElement result) {
		try {
			Result object = (Result) this.method.value().decodeResult(result);
			this.resultFuture.complete(Objects.requireNonNull(object));
		}
		catch (Exception var3) {
			this.resultFuture.completeExceptionally(var3);
		}
	}

	/**
	 * Определяет, следует ли timeout.
	 *
	 * @param time time
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldTimeout(long time) {
		return time > this.timeoutTime;
	}
}

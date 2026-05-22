package net.minecraft.server.dedicated.management.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.dedicated.management.*;
import net.minecraft.server.dedicated.management.dispatch.ManagementHandlerDispatcher;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс Management Connection Handler.
 */
public class ManagementConnectionHandler extends SimpleChannelInboundHandler<JsonElement> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final AtomicInteger CONNECTION_ID = new AtomicInteger(0);
	private final ManagementLogger managementLogger;
	private final ManagementConnectionId remote;
	private final ManagementServer managementServer;
	private final Channel channel;
	private final ManagementHandlerDispatcher handlerDispatcher;
	private final AtomicInteger OUTGOING_REQUEST_ID = new AtomicInteger();
	private final Int2ObjectMap<PendingResponse<?>>
			pendingResponses =
			Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap());

	public ManagementConnectionHandler(
			Channel channel,
			ManagementServer managementServer,
			ManagementHandlerDispatcher handlerDispatcher,
			ManagementLogger managementLogger
	) {
		this.remote = ManagementConnectionId.of(CONNECTION_ID.incrementAndGet());
		this.managementServer = managementServer;
		this.handlerDispatcher = handlerDispatcher;
		this.channel = channel;
		this.managementLogger = managementLogger;
	}

	/**
	 * Обрабатывает timeouts.
	 */
	public void processTimeouts() {
		long l = Util.getMeasuringTimeMs();
		this.pendingResponses
				.int2ObjectEntrySet()
				.removeIf(
						responseEntry -> {
							boolean bl = ((PendingResponse) responseEntry.getValue()).shouldTimeout(l);
							if (bl) {
								((PendingResponse) responseEntry.getValue())
										.resultFuture()
										.completeExceptionally(
												new ReadTimeoutException(
														"RPC method " + ((PendingResponse) responseEntry.getValue())
																.method()
																.registryKey()
																.getValue() + " timed out waiting for response"
												)
										);
							}

							return bl;
						}
				);
	}

	/**
	 * Channel active.
	 *
	 * @param context context
	 */
	public void channelActive(ChannelHandlerContext context) throws Exception {
		this.managementLogger.logAction(
				this.remote,
				"Management connection opened for {}",
				this.channel.remoteAddress()
		);
		super.channelActive(context);
		this.managementServer.onConnectionOpen(this);
	}

	/**
	 * Channel inactive.
	 *
	 * @param context context
	 */
	public void channelInactive(ChannelHandlerContext context) throws Exception {
		this.managementLogger.logAction(
				this.remote,
				"Management connection closed for {}",
				this.channel.remoteAddress()
		);
		super.channelInactive(context);
		this.managementServer.onConnectionClose(this);
	}

	/**
	 * Exception caught.
	 *
	 * @param context context
	 * @param throwable throwable
	 */
	public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) throws Exception {
		if (throwable.getCause() instanceof JsonParseException) {
			this.channel.writeAndFlush(ManagementError.PARSE_ERROR.encode(throwable.getMessage()));
		}
		else {
			super.exceptionCaught(context, throwable);
			this.channel.close().awaitUninterruptibly();
		}
	}

	/**
	 * Channel read0.
	 *
	 * @param channelHandlerContext channel handler context
	 * @param jsonElement json element
	 */
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, JsonElement jsonElement) {
		if (jsonElement.isJsonObject()) {
			JsonObject jsonObject = this.handleMessage(jsonElement.getAsJsonObject());
			if (jsonObject != null) {
				this.channel.writeAndFlush(jsonObject);
			}
		}
		else if (jsonElement.isJsonArray()) {
			this.channel.writeAndFlush(this.handleEach(jsonElement.getAsJsonArray().asList()));
		}
		else {
			this.channel.writeAndFlush(ManagementError.INVALID_REQUEST.encode((String) null));
		}
	}

	private JsonArray handleEach(List<JsonElement> messages) {
		JsonArray jsonArray = new JsonArray();
		messages
				.stream()
				.map(message -> this.handleMessage(message.getAsJsonObject()))
				.filter(Objects::nonNull)
				.forEach(jsonArray::add);
		return jsonArray;
	}

	/**
	 * Отправляет notification.
	 *
	 * @param method method
	 */
	public void sendNotification(RegistryEntry.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
		this.sendRequest(method, null, false);
	}

	public <Params> void sendNotification(
			RegistryEntry.Reference<? extends OutgoingRpcMethod<Params, ?>> method,
			Params params
	) {
		this.sendRequest(method, params, false);
	}

	/**
	 * Отправляет request.
	 *
	 * @param method method
	 *
	 * @return CompletableFuture — результат операции
	 */
	public <Result> CompletableFuture<Result> sendRequest(RegistryEntry.Reference<? extends OutgoingRpcMethod<Void, Result>> method) {
		return this.sendRequest(method, null, true);
	}

	public <Params, Result> CompletableFuture<Result> sendRequest(
			RegistryEntry.Reference<? extends OutgoingRpcMethod<Params, Result>> method,
			Params params
	) {
		return this.sendRequest(method, params, true);
	}

	@Contract("_,_,false->null;_,_,true->!null")
	private <Params, Result> @Nullable CompletableFuture<Result> sendRequest(
			RegistryEntry.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> method,
			@Nullable Params params,
			boolean expectResponse
	) {
		List<JsonElement>
				list =
				params != null ? List.of(Objects.requireNonNull(method.value().encodeParams(params))) : List.of();
		if (expectResponse) {
			CompletableFuture<Result> completableFuture = new CompletableFuture<>();
			int i = this.OUTGOING_REQUEST_ID.incrementAndGet();
			long l = Util.nanoTimeSupplier.get(TimeUnit.MILLISECONDS);
			this.pendingResponses.put(i, new PendingResponse<>(method, completableFuture, l + 5000L));
			this.channel.writeAndFlush(JsonRpc.encodeRequest(i, method.registryKey().getValue(), list));
			return completableFuture;
		}
		else {
			this.channel.writeAndFlush(JsonRpc.encodeRequest(null, method.registryKey().getValue(), list));
			return null;
		}
	}

	@VisibleForTesting
	@Nullable JsonObject handleMessage(JsonObject request) {
		try {
			JsonElement jsonElement = JsonRpc.getId(request);
			String string = JsonRpc.getMethod(request);
			JsonElement jsonElement2 = JsonRpc.getResult(request);
			JsonElement jsonElement3 = JsonRpc.getParameters(request);
			JsonObject jsonObject = JsonRpc.getError(request);
			if (string != null && jsonElement2 == null && jsonObject == null) {
				return jsonElement != null && !isValidRequestId(jsonElement)
				       ? ManagementError.INVALID_REQUEST.encode(
						"Invalid request id - only String, Number and NULL supported")
				       : this.handleRequest(jsonElement, string, jsonElement3);
			}
			else if (string == null && jsonElement2 != null && jsonObject == null && jsonElement != null) {
				if (isValidResponseId(jsonElement)) {
					this.handleResponse(jsonElement.getAsInt(), jsonElement2);
				}
				else {
					LOGGER.warn("Received respose {} with id {} we did not request", jsonElement2, jsonElement);
				}

				return null;
			}
			else {
				return string == null && jsonElement2 == null && jsonObject != null
				       ? this.handleError(jsonElement, jsonObject)
				       : ManagementError.INVALID_REQUEST.encode(Objects.requireNonNullElse(
						       jsonElement,
						       JsonNull.INSTANCE
				       ));
			}
		}
		catch (Exception var7) {
			LOGGER.error("Error while handling rpc request", var7);
			return ManagementError.INTERNAL_ERROR.encode(
					"Unknown error handling request - check server logs for stack trace");
		}
	}

	private static boolean isValidRequestId(JsonElement json) {
		return json.isJsonNull() || JsonHelper.isNumber(json) || JsonHelper.isString(json);
	}

	private static boolean isValidResponseId(JsonElement json) {
		return JsonHelper.isNumber(json);
	}

	private @Nullable JsonObject handleRequest(
			@Nullable JsonElement json,
			String method,
			@Nullable JsonElement parameters
	) {
		boolean bl = json != null;

		try {
			JsonElement jsonElement = this.processRequest(method, parameters);
			return jsonElement != null && bl ? JsonRpc.encodeResult(json, jsonElement) : null;
		}
		catch (RpcException var6) {
			LOGGER.debug(
					"Invalid parameter invocation {}: {}, {}",
					new Object[]{method, parameters, var6.getMessage()}
			);
			return bl ? ManagementError.INVALID_PARAMS.encode(json, var6.getMessage()) : null;
		}
		catch (RpcEncodingException var7) {
			LOGGER.error("Failed to encode json rpc response {}: {}", method, var7.getMessage());
			return bl ? ManagementError.INTERNAL_ERROR.encode(json, var7.getMessage()) : null;
		}
		catch (InvalidRpcRequestException var8) {
			return bl ? ManagementError.INVALID_REQUEST.encode(json, var8.getMessage()) : null;
		}
		catch (RpcMethodNotFoundException var9) {
			return bl ? ManagementError.METHOD_NOT_FOUND.encode(json, var9.getMessage()) : null;
		}
		catch (Exception var10) {
			LOGGER.error("Error while dispatching rpc method {}", method, var10);
			return bl ? ManagementError.INTERNAL_ERROR.encode(json) : null;
		}
	}

	/**
	 * Обрабатывает request.
	 *
	 * @param method method
	 * @param json json
	 *
	 * @return @Nullable JsonElement — результат операции
	 */
	public @Nullable JsonElement processRequest(String method, @Nullable JsonElement json) {
		Identifier identifier = Identifier.tryParse(method);
		if (identifier == null) {
			throw new InvalidRpcRequestException("Failed to parse method value: " + method);
		}
		else {
			Optional<IncomingRpcMethod<?, ?>> optional = Registries.INCOMING_RPC_METHOD.getOptionalValue(identifier);
			if (optional.isEmpty()) {
				throw new RpcMethodNotFoundException("Method not found: " + method);
			}
			else if (optional.get().attributes().runOnMainThread()) {
				try {
					return this.handlerDispatcher
							.<JsonElement>submit(() -> optional.get().handle(this.handlerDispatcher, json, this.remote))
							.join();
				}
				catch (CompletionException var8) {
					if (var8.getCause() instanceof RuntimeException runtimeException) {
						throw runtimeException;
					}
					else {
						throw var8;
					}
				}
			}
			else {
				return optional.get().handle(this.handlerDispatcher, json, this.remote);
			}
		}
	}

	private void handleResponse(int id, JsonElement result) {
		PendingResponse<?> pendingResponse = (PendingResponse<?>) this.pendingResponses.remove(id);
		if (pendingResponse == null) {
			LOGGER.warn("Received unknown response (id: {}): {}", id, result);
		}
		else {
			pendingResponse.handleResponse(result);
		}
	}

	private @Nullable JsonObject handleError(@Nullable JsonElement json, JsonObject error) {
		if (json != null && isValidResponseId(json)) {
			PendingResponse<?> pendingResponse = (PendingResponse<?>) this.pendingResponses.remove(json.getAsInt());
			if (pendingResponse != null) {
				pendingResponse.resultFuture().completeExceptionally(new RpcRemoteErrorException(json, error));
			}
		}

		LOGGER.error("Received error (id: {}): {}", json, error);
		return null;
	}
}

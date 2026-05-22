package net.minecraft.server.dedicated.management;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.Registry;
import net.minecraft.server.dedicated.management.dispatch.ManagementHandlerDispatcher;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.dedicated.management.schema.RpcSchema;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

/**
 * Класс Incoming Rpc Method.
 */
public interface IncomingRpcMethod<Params, Result> {

	RpcMethodInfo<Params, Result> info();

	IncomingRpcMethod.Attributes attributes();

	JsonElement handle(
			ManagementHandlerDispatcher dispatcher,
			@Nullable JsonElement parameters,
			ManagementConnectionId remote
	);

	static <Result> IncomingRpcMethod.Builder<Void, Result> createParameterlessBuilder(IncomingRpcMethod.ParameterlessHandler<Result> handler) {
		return new IncomingRpcMethod.Builder<>(handler);
	}

	static <Params, Result> IncomingRpcMethod.Builder<Params, Result> createParameterizedBuilder(IncomingRpcMethod.ParameterizedHandler<Params, Result> handler) {
		return new IncomingRpcMethod.Builder<>(handler);
	}

	static <Result> IncomingRpcMethod.Builder<Void, Result> createParameterlessBuilder(Function<ManagementHandlerDispatcher, Result> handler) {
		return new IncomingRpcMethod.Builder<>(handler);
	}

	public record Attributes(boolean runOnMainThread, boolean discoverable) {
	}

	public static class Builder<Params, Result> {

		private String description = "";
		private @Nullable RpcRequestParameter<Params> params;
		private @Nullable RpcResponseResult<Result> result;
		private boolean runOnMainThread = true;
		private boolean discoverable = true;
		private IncomingRpcMethod.@Nullable ParameterlessHandler<Result> parameterlessHandler;
		private IncomingRpcMethod.@Nullable ParameterizedHandler<Params, Result> parameterizedHandler;

		public Builder(IncomingRpcMethod.ParameterlessHandler<Result> parameterlessHandler) {
			this.parameterlessHandler = parameterlessHandler;
		}

		public Builder(IncomingRpcMethod.ParameterizedHandler<Params, Result> parameterizedHandler) {
			this.parameterizedHandler = parameterizedHandler;
		}

		public Builder(Function<ManagementHandlerDispatcher, Result> parameterlessHandler) {
			this.parameterlessHandler = (dispatcher, remote) -> parameterlessHandler.apply(dispatcher);
		}

		public IncomingRpcMethod.Builder<Params, Result> description(String description) {
			this.description = description;
			return this;
		}

		public IncomingRpcMethod.Builder<Params, Result> result(String name, RpcSchema<Result> schema) {
			this.result = new RpcResponseResult<>(name, schema.copy());
			return this;
		}

		public IncomingRpcMethod.Builder<Params, Result> parameter(String name, RpcSchema<Params> schema) {
			this.params = new RpcRequestParameter<>(name, schema.copy());
			return this;
		}

		public IncomingRpcMethod.Builder<Params, Result> noRequireMainThread() {
			this.runOnMainThread = false;
			return this;
		}

		public IncomingRpcMethod.Builder<Params, Result> notDiscoverable() {
			this.discoverable = false;
			return this;
		}

		/**
		 * Build.
		 *
		 * @return IncomingRpcMethod — результат операции
		 */
		public IncomingRpcMethod<Params, Result> build() {
			if (this.result == null) {
				throw new IllegalStateException("No response defined");
			}
			else {
				IncomingRpcMethod.Attributes
						attributes =
						new IncomingRpcMethod.Attributes(this.discoverable, this.runOnMainThread);
				RpcMethodInfo<Params, Result>
						rpcMethodInfo =
						new RpcMethodInfo<>(this.description, this.params, this.result);
				if (this.parameterlessHandler != null) {
					return new IncomingRpcMethod.Parameterless<>(rpcMethodInfo, attributes, this.parameterlessHandler);
				}
				else if (this.parameterizedHandler != null) {
					if (this.params == null) {
						throw new IllegalStateException("No param schema defined");
					}
					else {
						return new IncomingRpcMethod.Parameterized<>(
								rpcMethodInfo,
								attributes,
								this.parameterizedHandler
						);
					}
				}
				else {
					throw new IllegalStateException("No method defined");
				}
			}
		}

		public IncomingRpcMethod<?, ?> buildAndRegisterVanilla(
				Registry<IncomingRpcMethod<?, ?>> registry,
				String path
		) {
			return this.buildAndRegister(registry, Identifier.ofVanilla(path));
		}

		private IncomingRpcMethod<?, ?> buildAndRegister(Registry<IncomingRpcMethod<?, ?>> registry, Identifier id) {
			return Registry.register(registry, id, this.build());
		}
	}

	public record Parameterized<Params, Result>(
			RpcMethodInfo<Params, Result> info,
			IncomingRpcMethod.Attributes attributes,
			IncomingRpcMethod.ParameterizedHandler<Params, Result> handler
	) implements IncomingRpcMethod<Params, Result> {

		@Override
		public JsonElement handle(
				ManagementHandlerDispatcher dispatcher,
				@Nullable JsonElement parameters,
				ManagementConnectionId remote
		) {
			if (parameters != null && (parameters.isJsonArray() || parameters.isJsonObject())) {
				if (this.info.params().isEmpty()) {
					throw new IllegalArgumentException("Method defined as having parameters without describing them");
				}
				else {
					JsonElement jsonElement2;
					if (parameters.isJsonObject()) {
						String string = this.info.params().get().name();
						JsonElement jsonElement = parameters.getAsJsonObject().get(string);
						if (jsonElement == null) {
							throw new RpcException(String.format(
									Locale.ROOT,
									"Params passed by-name, but expected param [%s] does not exist",
									string
							));
						}

						jsonElement2 = jsonElement;
					}
					else {
						JsonArray jsonArray = parameters.getAsJsonArray();
						if (jsonArray.isEmpty() || jsonArray.size() > 1) {
							throw new RpcException("Expected exactly one element in the params array");
						}

						jsonElement2 = jsonArray.get(0);
					}

					Params
							object =
							(Params) this.info
									.params()
									.get()
									.schema()
									.codec()
									.parse(JsonOps.INSTANCE, jsonElement2)
									.getOrThrow(RpcException::new);
					Result object2 = this.handler.apply(dispatcher, object, remote);
					if (this.info.result().isEmpty()) {
						throw new IllegalStateException("No result codec defined");
					}
					else {
						return (JsonElement) this.info
								.result()
								.get()
								.schema()
								.codec()
								.encodeStart(JsonOps.INSTANCE, object2)
								.getOrThrow(RpcEncodingException::new);
					}
				}
			}
			else {
				throw new RpcException("Expected params as array or named");
			}
		}
	}

	@FunctionalInterface
	public interface ParameterizedHandler<Params, Result> {

		Result apply(ManagementHandlerDispatcher dispatcher, Params params, ManagementConnectionId remote);
	}

	public record Parameterless<Params, Result>(
			RpcMethodInfo<Params, Result> info,
			IncomingRpcMethod.Attributes attributes,
			IncomingRpcMethod.ParameterlessHandler<Result> handler
	) implements IncomingRpcMethod<Params, Result> {

		@Override
		public JsonElement handle(
				ManagementHandlerDispatcher dispatcher,
				@Nullable JsonElement parameters,
				ManagementConnectionId remote
		) {
			if (parameters == null || parameters.isJsonArray() && parameters.getAsJsonArray().isEmpty()) {
				if (this.info.params().isPresent()) {
					throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
				}
				else {
					Result object = this.handler.apply(dispatcher, remote);
					if (this.info.result().isEmpty()) {
						throw new IllegalStateException("No result codec defined");
					}
					else {
						return (JsonElement) this.info
								.result()
								.get()
								.schema()
								.codec()
								.encodeStart(JsonOps.INSTANCE, object)
								.getOrThrow(RpcException::new);
					}
				}
			}
			else {
				throw new RpcException("Expected no params, or an empty array");
			}
		}
	}

	@FunctionalInterface
	public interface ParameterlessHandler<Result> {

		Result apply(ManagementHandlerDispatcher dispatcher, ManagementConnectionId remote);
	}
}

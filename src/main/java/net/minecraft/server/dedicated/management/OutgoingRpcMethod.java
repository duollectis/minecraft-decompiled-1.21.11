package net.minecraft.server.dedicated.management;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.dedicated.management.schema.RpcSchema;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

public interface OutgoingRpcMethod<Params, Result> {
   String NOTIFICATION_PREFIX = "notification/";

   RpcMethodInfo<Params, Result> info();

   OutgoingRpcMethod.Attributes attributes();

   default @Nullable JsonElement encodeParams(Params params) {
      return null;
   }

   default @Nullable Result decodeResult(JsonElement result) {
      return null;
   }

   static OutgoingRpcMethod.Builder<Void, Void> createSimpleBuilder() {
      return new OutgoingRpcMethod.Builder<>(OutgoingRpcMethod.Simple::new);
   }

   static <Params> OutgoingRpcMethod.Builder<Params, Void> createNotificationBuilder() {
      return new OutgoingRpcMethod.Builder<>(OutgoingRpcMethod.Notification::new);
   }

   static <Result> OutgoingRpcMethod.Builder<Void, Result> createParameterlessBuilder() {
      return new OutgoingRpcMethod.Builder<>(OutgoingRpcMethod.Parameterless::new);
   }

   static <Params, Result> OutgoingRpcMethod.Builder<Params, Result> createParameterizedBuilder() {
      return new OutgoingRpcMethod.Builder<>(OutgoingRpcMethod.Parameterized::new);
   }

   public record Attributes(boolean discoverable) {
   }

   public static class Builder<Params, Result> {
      public static final OutgoingRpcMethod.Attributes DEFAULT_ATTRIBUTES = new OutgoingRpcMethod.Attributes(true);
      private final OutgoingRpcMethod.Factory<Params, Result> factory;
      private String description = "";
      private @Nullable RpcRequestParameter<Params> requestParameter;
      private @Nullable RpcResponseResult<Result> responseResult;

      public Builder(OutgoingRpcMethod.Factory<Params, Result> factory) {
         this.factory = factory;
      }

      public OutgoingRpcMethod.Builder<Params, Result> description(String description) {
         this.description = description;
         return this;
      }

      public OutgoingRpcMethod.Builder<Params, Result> responseResult(String name, RpcSchema<Result> schema) {
         this.responseResult = new RpcResponseResult<>(name, schema);
         return this;
      }

      public OutgoingRpcMethod.Builder<Params, Result> requestParameter(String name, RpcSchema<Params> schema) {
         this.requestParameter = new RpcRequestParameter<>(name, schema);
         return this;
      }

      private OutgoingRpcMethod<Params, Result> build() {
         RpcMethodInfo<Params, Result> rpcMethodInfo = new RpcMethodInfo<>(this.description, this.requestParameter, this.responseResult);
         return this.factory.create(rpcMethodInfo, DEFAULT_ATTRIBUTES);
      }

      public RegistryEntry.Reference<OutgoingRpcMethod<Params, Result>> buildAndRegisterVanilla(String path) {
         return this.buildAndRegister(Identifier.ofVanilla("notification/" + path));
      }

      private RegistryEntry.Reference<OutgoingRpcMethod<Params, Result>> buildAndRegister(Identifier id) {
         return Registry.registerReference(Registries.OUTGOING_RPC_METHOD, id, this.build());
      }
   }

   @FunctionalInterface
   public interface Factory<Params, Result> {
      OutgoingRpcMethod<Params, Result> create(RpcMethodInfo<Params, Result> methodInfo, OutgoingRpcMethod.Attributes attributes);
   }

   public record Notification<Params>(RpcMethodInfo<Params, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Params, Void> {
      @Override
      public @Nullable JsonElement encodeParams(Params params) {
         if (this.info.params().isEmpty()) {
            throw new IllegalStateException("Method defined as having no parameters");
         } else {
            return (JsonElement)this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, params).getOrThrow();
         }
      }
   }

   public record Parameterized<Params, Result>(RpcMethodInfo<Params, Result> info, OutgoingRpcMethod.Attributes attributes)
      implements OutgoingRpcMethod<Params, Result> {
      @Override
      public @Nullable JsonElement encodeParams(Params params) {
         if (this.info.params().isEmpty()) {
            throw new IllegalStateException("Method defined as having no parameters");
         } else {
            return (JsonElement)this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, params).getOrThrow();
         }
      }

      @Override
      public Result decodeResult(JsonElement result) {
         if (this.info.result().isEmpty()) {
            throw new IllegalStateException("Method defined as having no result");
         } else {
            return (Result)this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, result).getOrThrow();
         }
      }
   }

   public record Parameterless<Result>(RpcMethodInfo<Void, Result> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Result> {
      @Override
      public Result decodeResult(JsonElement result) {
         if (this.info.result().isEmpty()) {
            throw new IllegalStateException("Method defined as having no result");
         } else {
            return (Result)this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, result).getOrThrow();
         }
      }
   }

   public record Simple(RpcMethodInfo<Void, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Void> {
   }
}

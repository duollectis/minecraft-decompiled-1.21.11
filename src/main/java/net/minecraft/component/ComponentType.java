package net.minecraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface ComponentType<T> {
   Codec<ComponentType<?>> CODEC = Codec.lazyInitialized(() -> Registries.DATA_COMPONENT_TYPE.getCodec());
   PacketCodec<RegistryByteBuf, ComponentType<?>> PACKET_CODEC = PacketCodec.recursive(
      packetCodec -> PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_TYPE)
   );
   Codec<ComponentType<?>> PERSISTENT_CODEC = CODEC.validate(
      componentType -> componentType.shouldSkipSerialization()
         ? DataResult.error(() -> "Encountered transient component " + Registries.DATA_COMPONENT_TYPE.getId(componentType))
         : DataResult.success(componentType)
   );
   Codec<Map<ComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = Codec.dispatchedMap(PERSISTENT_CODEC, ComponentType::getCodecOrThrow);

   static <T> ComponentType.Builder<T> builder() {
      return new ComponentType.Builder<>();
   }

   @Nullable Codec<T> getCodec();

   default Codec<T> getCodecOrThrow() {
      Codec<T> codec = this.getCodec();
      if (codec == null) {
         throw new IllegalStateException(this + " is not a persistent component");
      } else {
         return codec;
      }
   }

   default boolean shouldSkipSerialization() {
      return this.getCodec() == null;
   }

   boolean skipsHandAnimation();

   PacketCodec<? super RegistryByteBuf, T> getPacketCodec();

   public static class Builder<T> {
      private @Nullable Codec<T> codec;
      private @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec;
      private boolean cache;
      private boolean skipsHandAnimation;

      public ComponentType.Builder<T> codec(Codec<T> codec) {
         this.codec = codec;
         return this;
      }

      public ComponentType.Builder<T> packetCodec(PacketCodec<? super RegistryByteBuf, T> packetCodec) {
         this.packetCodec = packetCodec;
         return this;
      }

      public ComponentType.Builder<T> cache() {
         this.cache = true;
         return this;
      }

      public ComponentType<T> build() {
         PacketCodec<? super RegistryByteBuf, T> packetCodec = Objects.requireNonNullElseGet(
            this.packetCodec, () -> PacketCodecs.registryCodec(Objects.requireNonNull(this.codec, "Missing Codec for component"))
         );
         Codec<T> codec = this.cache && this.codec != null ? DataComponentTypes.CACHE.wrap(this.codec) : this.codec;
         return new ComponentType.Builder.SimpleDataComponentType<>(codec, packetCodec, this.skipsHandAnimation);
      }

      public ComponentType.Builder<T> skipsHandAnimation() {
         this.skipsHandAnimation = true;
         return this;
      }

      static class SimpleDataComponentType<T> implements ComponentType<T> {
         private final @Nullable Codec<T> codec;
         private final PacketCodec<? super RegistryByteBuf, T> packetCodec;
         private final boolean skipsHandAnimation;

         SimpleDataComponentType(@Nullable Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec, boolean skipsHandAnimation) {
            this.codec = codec;
            this.packetCodec = packetCodec;
            this.skipsHandAnimation = skipsHandAnimation;
         }

         @Override
         public boolean skipsHandAnimation() {
            return this.skipsHandAnimation;
         }

         @Override
         public @Nullable Codec<T> getCodec() {
            return this.codec;
         }

         @Override
         public PacketCodec<? super RegistryByteBuf, T> getPacketCodec() {
            return this.packetCodec;
         }

         @Override
         public String toString() {
            return Util.registryValueToString((Registry) Registries.DATA_COMPONENT_TYPE, this);
         }
      }
   }
}

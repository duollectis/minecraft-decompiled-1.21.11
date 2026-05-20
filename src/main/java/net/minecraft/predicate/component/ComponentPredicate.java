package net.minecraft.predicate.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;

public interface ComponentPredicate {
   Codec<Map<ComponentPredicate.Type<?>, ComponentPredicate>> PREDICATES_MAP_CODEC = Codec.dispatchedMap(
      ComponentPredicate.Type.CODEC, ComponentPredicate.Type::getPredicateCodec
   );
   PacketCodec<RegistryByteBuf, ComponentPredicate.Typed<?>> SINGLE_PREDICATE_PACKET_CODEC = ComponentPredicate.Type.PACKET_CODEC
      .dispatch(ComponentPredicate.Typed::type, ComponentPredicate.Type::getTypedPacketCodec);
   PacketCodec<RegistryByteBuf, Map<ComponentPredicate.Type<?>, ComponentPredicate>> PREDICATES_MAP_PACKET_CODEC = SINGLE_PREDICATE_PACKET_CODEC.collect(
         PacketCodecs.toList(64)
      )
      .xmap(
         list -> list.stream().collect(Collectors.toMap(ComponentPredicate.Typed::type, ComponentPredicate.Typed::predicate)),
         map -> map.entrySet().stream().<ComponentPredicate.Typed<?>>map(entry -> ComponentPredicate.Typed.fromEntry(entry)).toList()
      );

   static MapCodec<ComponentPredicate.Typed<?>> createCodec(String predicateFieldName) {
      return ComponentPredicate.Type.CODEC.dispatchMap(predicateFieldName, ComponentPredicate.Typed::type, ComponentPredicate.Type::getTypedCodec);
   }

   boolean test(ComponentsAccess components);

   public static final class OfExistence extends ComponentPredicate.TypeImpl<ComponentExistencePredicate> {
      private final ComponentExistencePredicate predicate;

      public OfExistence(ComponentExistencePredicate predicate) {
         super(MapCodec.unitCodec(predicate));
         this.predicate = predicate;
      }

      public ComponentExistencePredicate getPredicate() {
         return this.predicate;
      }

      public ComponentType<?> getComponentType() {
         return this.predicate.type();
      }

      public static ComponentPredicate.OfExistence toPredicateType(ComponentType<?> type) {
         return new ComponentPredicate.OfExistence(new ComponentExistencePredicate(type));
      }
   }

   public static final class OfValue<T extends ComponentPredicate> extends ComponentPredicate.TypeImpl<T> {
      public OfValue(Codec<T> codec) {
         super(codec);
      }
   }

   public interface Type<T extends ComponentPredicate> {
      Codec<ComponentPredicate.Type<?>> CODEC = Codec.either(Registries.DATA_COMPONENT_PREDICATE_TYPE.getCodec(), Registries.DATA_COMPONENT_TYPE.getCodec())
         .xmap(ComponentPredicate.Type::toType, ComponentPredicate.Type::fromType);
      PacketCodec<RegistryByteBuf, ComponentPredicate.Type<?>> PACKET_CODEC = PacketCodecs.either(
            PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_PREDICATE_TYPE), PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_TYPE)
         )
         .xmap(ComponentPredicate.Type::toType, ComponentPredicate.Type::fromType);

      private static <T extends ComponentPredicate.Type<?>> Either<T, ComponentType<?>> fromType(T type) {
         return type instanceof ComponentPredicate.OfExistence ofExistence ? Either.right(ofExistence.getComponentType()) : Either.left(type);
      }

      private static ComponentPredicate.Type<?> toType(Either<ComponentPredicate.Type<?>, ComponentType<?>> either) {
         return (ComponentPredicate.Type<?>)either.map(type -> type, ComponentPredicate.OfExistence::toPredicateType);
      }

      Codec<T> getPredicateCodec();

      MapCodec<ComponentPredicate.Typed<T>> getTypedCodec();

      PacketCodec<RegistryByteBuf, ComponentPredicate.Typed<T>> getTypedPacketCodec();
   }

   public abstract static class TypeImpl<T extends ComponentPredicate> implements ComponentPredicate.Type<T> {
      private final Codec<T> predicateCodec;
      private final MapCodec<ComponentPredicate.Typed<T>> typedCodec;
      private final PacketCodec<RegistryByteBuf, ComponentPredicate.Typed<T>> packetCodec;

      public TypeImpl(Codec<T> predicateCodec) {
         this.predicateCodec = predicateCodec;
         this.typedCodec = ComponentPredicate.Typed.getCodec(this, predicateCodec);
         this.packetCodec = PacketCodecs.<T>registryCodec(predicateCodec)
            .xmap(predicate -> new ComponentPredicate.Typed<>(this, (T)predicate), ComponentPredicate.Typed::predicate);
      }

      @Override
      public Codec<T> getPredicateCodec() {
         return this.predicateCodec;
      }

      @Override
      public MapCodec<ComponentPredicate.Typed<T>> getTypedCodec() {
         return this.typedCodec;
      }

      @Override
      public PacketCodec<RegistryByteBuf, ComponentPredicate.Typed<T>> getTypedPacketCodec() {
         return this.packetCodec;
      }
   }

   public record Typed<T extends ComponentPredicate>(ComponentPredicate.Type<T> type, T predicate) {
      static <T extends ComponentPredicate> MapCodec<ComponentPredicate.Typed<T>> getCodec(ComponentPredicate.Type<T> type, Codec<T> valueCodec) {
         return RecordCodecBuilder.mapCodec(
            instance -> instance.group(valueCodec.fieldOf("value").forGetter(ComponentPredicate.Typed::predicate))
               .apply(instance, predicate -> new ComponentPredicate.Typed<>(type, (T)predicate))
         );
      }

      private static <T extends ComponentPredicate> ComponentPredicate.Typed<T> fromEntry(Entry<ComponentPredicate.Type<?>, T> entry) {
         return new ComponentPredicate.Typed<>((ComponentPredicate.Type<T>)entry.getKey(), entry.getValue());
      }
   }
}

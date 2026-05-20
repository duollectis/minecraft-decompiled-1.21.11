package net.minecraft.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

public final class TypedEntityData<IdType> implements TooltipAppender {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String ID_KEY = "id";
   final IdType type;
   final NbtCompound nbt;

   public static <T> Codec<TypedEntityData<T>> createCodec(Codec<T> typeCodec) {
      return new Codec<TypedEntityData<T>>() {
         public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> ops, V value) {
            return NbtComponent.COMPOUND_CODEC
               .decode(ops, value)
               .flatMap(
                  pair -> {
                     NbtCompound nbtCompound = ((NbtCompound)pair.getFirst()).copy();
                     NbtElement nbtElement = nbtCompound.remove("id");
                     return nbtElement == null
                        ? DataResult.error(() -> "Expected 'id' field in " + value)
                        : typeCodec.parse(toNbtOps((DynamicOps<T>)ops), nbtElement)
                           .map(objectx -> Pair.of(new TypedEntityData<>(objectx, nbtCompound), pair.getSecond()));
                  }
               );
         }

         public <V> DataResult<V> encode(TypedEntityData<T> typedEntityData, DynamicOps<V> dynamicOps, V object) {
            return typeCodec.encodeStart(toNbtOps((DynamicOps<T>)dynamicOps), typedEntityData.type).flatMap(id -> {
               NbtCompound nbtCompound = typedEntityData.nbt.copy();
               nbtCompound.put("id", id);
               return NbtComponent.COMPOUND_CODEC.encode(nbtCompound, dynamicOps, object);
            });
         }

         private static <T> DynamicOps<NbtElement> toNbtOps(DynamicOps<T> ops) {
            return (DynamicOps<NbtElement>)(ops instanceof RegistryOps<T> registryOps ? registryOps.withDelegate(NbtOps.INSTANCE) : NbtOps.INSTANCE);
         }
      };
   }

   public static <B extends ByteBuf, T> PacketCodec<B, TypedEntityData<T>> createPacketCodec(PacketCodec<B, T> typePacketCodec) {
      return PacketCodec.tuple(
         typePacketCodec,
         (Function<TypedEntityData<T>, T>)(TypedEntityData::getType),
         PacketCodecs.NBT_COMPOUND,
         TypedEntityData::getNbtWithoutIdInternal,
         (BiFunction<T, NbtCompound, TypedEntityData<T>>)(TypedEntityData::new)
      );
   }

   TypedEntityData(IdType type, NbtCompound nbt) {
      this.type = type;
      this.nbt = stripId(nbt);
   }

   public static <T> TypedEntityData<T> create(T type, NbtCompound nbt) {
      return new TypedEntityData<>(type, nbt);
   }

   private static NbtCompound stripId(NbtCompound nbt) {
      if (nbt.contains("id")) {
         NbtCompound nbtCompound = nbt.copy();
         nbtCompound.remove("id");
         return nbtCompound;
      } else {
         return nbt;
      }
   }

   public IdType getType() {
      return this.type;
   }

   public boolean contains(String key) {
      return this.nbt.contains(key);
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else {
         return !(other instanceof TypedEntityData<?> typedEntityData) ? false : this.type == typedEntityData.type && this.nbt.equals(typedEntityData.nbt);
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.type.hashCode() + this.nbt.hashCode();
   }

   @Override
   public String toString() {
      return this.type + " " + this.nbt;
   }

   public void applyToEntity(Entity entity) {
      try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
         NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
         entity.writeData(nbtWriteView);
         NbtCompound nbtCompound = nbtWriteView.getNbt();
         UUID uUID = entity.getUuid();
         nbtCompound.copyFrom(this.getNbtWithoutId());
         entity.readData(NbtReadView.create(logging, entity.getRegistryManager(), nbtCompound));
         entity.setUuid(uUID);
      }
   }

   public boolean applyToBlockEntity(BlockEntity blockEntity, RegistryWrapper.WrapperLookup registryLookup) {
      boolean exception;
      try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
         NbtWriteView nbtWriteView = NbtWriteView.create(logging, registryLookup);
         blockEntity.writeComponentlessData(nbtWriteView);
         NbtCompound nbtCompound = nbtWriteView.getNbt();
         NbtCompound nbtCompound2 = nbtCompound.copy();
         nbtCompound.copyFrom(this.getNbtWithoutId());
         if (!nbtCompound.equals(nbtCompound2)) {
            try {
               blockEntity.readComponentlessData(NbtReadView.create(logging, registryLookup, nbtCompound));
               blockEntity.markDirty();
               return true;
            } catch (Exception var11) {
               LOGGER.warn("Failed to apply custom data to block entity at {}", blockEntity.getPos(), var11);

               try {
                  blockEntity.readComponentlessData(NbtReadView.create(logging.makeChild(() -> "(rollback)"), registryLookup, nbtCompound2));
               } catch (Exception var10) {
                  LOGGER.warn("Failed to rollback block entity at {} after failure", blockEntity.getPos(), var10);
               }
            }
         }

         exception = false;
      }

      return exception;
   }

   private NbtCompound getNbtWithoutIdInternal() {
      return this.nbt;
   }

   @Deprecated
   public NbtCompound getNbtWithoutId() {
      return this.nbt;
   }

   public NbtCompound copyNbtWithoutId() {
      return this.nbt.copy();
   }

   @Override
   public void appendTooltip(Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components) {
      if (this.type.getClass() == EntityType.class) {
         EntityType<?> entityType = (EntityType<?>)this.type;
         if (context.isDifficultyPeaceful() && !entityType.isAllowedInPeaceful()) {
            textConsumer.accept(Text.translatable("item.spawn_egg.peaceful").formatted(Formatting.RED));
         }
      }
   }
}

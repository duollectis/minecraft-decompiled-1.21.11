package net.minecraft.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
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

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Типизированные данные сущности: хранит тип (например, {@link EntityType}) и произвольный NBT-блок.
 * Используется для сериализации данных спавн-яиц и применения NBT к существующим сущностям/блок-сущностям.
 */
public final class TypedEntityData<IdType> implements TooltipAppender {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String ID_KEY = "id";
	final IdType type;
	final NbtCompound nbt;

	/**
	 * Создаёт {@link Codec} для сериализации {@code TypedEntityData}.
	 * При декодировании извлекает поле {@code "id"} из NBT-объекта и парсит его через {@code typeCodec}.
	 * При кодировании добавляет поле {@code "id"} обратно в NBT.
	 */
	public static <T> Codec<TypedEntityData<T>> createCodec(Codec<T> typeCodec) {
		return new Codec<TypedEntityData<T>>() {
			public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> ops, V value) {
				return NbtComponent.COMPOUND_CODEC
						.decode(ops, value)
						.flatMap(pair -> {
							NbtCompound compound = pair.getFirst().copy();
							NbtElement idElement = compound.remove(ID_KEY);

							return idElement == null
									? DataResult.error(() -> "Expected 'id' field in " + value)
									: typeCodec
											.parse(toNbtOps(ops), idElement)
											.map(parsed -> Pair.of(
													new TypedEntityData<>(parsed, compound),
													pair.getSecond()
											));
						});
			}

			public <V> DataResult<V> encode(TypedEntityData<T> data, DynamicOps<V> ops, V object) {
				return typeCodec.encodeStart(toNbtOps(ops), data.type).flatMap(id -> {
					NbtCompound compound = data.nbt.copy();
					compound.put(ID_KEY, id);
					return NbtComponent.COMPOUND_CODEC.encode(compound, ops, object);
				});
			}

			@SuppressWarnings("unchecked")
			private <V> DynamicOps<NbtElement> toNbtOps(DynamicOps<V> ops) {
				return ops instanceof RegistryOps<V> registryOps
						? (DynamicOps<NbtElement>) registryOps.withDelegate(NbtOps.INSTANCE)
						: NbtOps.INSTANCE;
			}
		};
	}

	/**
	 * Создаёт {@link PacketCodec} для передачи {@code TypedEntityData} по сети.
	 */
	public static <B extends ByteBuf, T> PacketCodec<B, TypedEntityData<T>> createPacketCodec(PacketCodec<B, T> typePacketCodec) {
		return PacketCodec.tuple(
				typePacketCodec,
				TypedEntityData::getType,
				PacketCodecs.NBT_COMPOUND,
				TypedEntityData::getNbtWithoutIdInternal,
				TypedEntityData::new
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
		if (!nbt.contains("id")) {
			return nbt;
		}

		NbtCompound copy = nbt.copy();
		copy.remove("id");
		return copy;
	}

	public IdType getType() {
		return type;
	}

	public boolean contains(String key) {
		return nbt.contains(key);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		return other instanceof TypedEntityData<?> typed
				&& type == typed.type
				&& nbt.equals(typed.nbt);
	}

	@Override
	public int hashCode() {
		return 31 * type.hashCode() + nbt.hashCode();
	}

	@Override
	public String toString() {
		return type + " " + nbt;
	}

	/**
	 * Применяет хранимый NBT к существующей сущности, сохраняя её UUID.
	 * Сначала читает текущие данные сущности, затем перезаписывает их данными из этого объекта.
	 */
	public void applyToEntity(Entity entity) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
			NbtWriteView writeView = NbtWriteView.create(logging, entity.getRegistryManager());
			entity.writeData(writeView);
			NbtCompound nbtCompound = writeView.getNbt();
			UUID savedUuid = entity.getUuid();
			nbtCompound.copyFrom(getNbtWithoutId());
			entity.readData(NbtReadView.create(logging, entity.getRegistryManager(), nbtCompound));
			entity.setUuid(savedUuid);
		}
	}

	/**
	 * Применяет хранимый NBT к блок-сущности с откатом при ошибке.
	 * Возвращает {@code true}, если данные были успешно применены и блок-сущность изменилась.
	 */
	public boolean applyToBlockEntity(BlockEntity blockEntity, RegistryWrapper.WrapperLookup registryLookup) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
			NbtWriteView writeView = NbtWriteView.create(logging, registryLookup);
			blockEntity.writeComponentlessData(writeView);
			NbtCompound current = writeView.getNbt();
			NbtCompound original = current.copy();
			current.copyFrom(getNbtWithoutId());

			if (current.equals(original)) {
				return false;
			}

			try {
				blockEntity.readComponentlessData(NbtReadView.create(logging, registryLookup, current));
				blockEntity.markDirty();
				return true;
			}
			catch (Exception applyException) {
				LOGGER.warn("Failed to apply custom data to block entity at {}", blockEntity.getPos(), applyException);

				try {
					blockEntity.readComponentlessData(NbtReadView.create(
							logging.makeChild(() -> "(rollback)"),
							registryLookup,
							original
					));
				}
				catch (Exception rollbackException) {
					LOGGER.warn("Failed to rollback block entity at {} after failure", blockEntity.getPos(), rollbackException);
				}

				return false;
			}
		}
	}

	private NbtCompound getNbtWithoutIdInternal() {
		return nbt;
	}

	@Deprecated
	public NbtCompound getNbtWithoutId() {
		return nbt;
	}

	public NbtCompound copyNbtWithoutId() {
		return nbt.copy();
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (type instanceof EntityType<?> entityType) {

			if (context.isDifficultyPeaceful() && !entityType.isAllowedInPeaceful()) {
				textConsumer.accept(Text.translatable("item.spawn_egg.peaceful").formatted(Formatting.RED));
			}
		}
	}
}

package net.minecraft.world.event;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Источник позиции, привязанный к сущности в мире.
 * <p>
 * Внутренне хранит сущность в виде {@link Either}: либо прямая ссылка на {@link Entity},
 * либо идентификатор — UUID (для десериализации с диска) или числовой ID (для пакетов).
 * При первом вызове {@link #getPos(World)} выполняет ленивый поиск сущности по идентификатору.
 */
public class EntityPositionSource implements PositionSource {

	public static final MapCodec<EntityPositionSource> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Uuids.INT_STREAM_CODEC.fieldOf("source_entity").forGetter(EntityPositionSource::getUuid),
			Codec.FLOAT
				.fieldOf("y_offset")
				.orElse(0.0F)
				.forGetter(source -> source.yOffset)
		).apply(
			instance,
			(uuid, yOffset) -> new EntityPositionSource(Either.right(Either.left(uuid)), yOffset)
		)
	);
	public static final PacketCodec<ByteBuf, EntityPositionSource> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		EntityPositionSource::getEntityId,
		PacketCodecs.FLOAT,
		source -> source.yOffset,
		(entityId, yOffset) -> new EntityPositionSource(Either.right(Either.right(entityId)), yOffset)
	);

	/**
	 * Хранит состояние источника: либо прямую ссылку на Entity,
	 * либо UUID (после десериализации), либо числовой ID (после получения пакета).
	 */
	private Either<Entity, Either<UUID, Integer>> source;
	private final float yOffset;

	public EntityPositionSource(Entity entity, float yOffset) {
		this(Either.left(entity), yOffset);
	}

	private EntityPositionSource(Either<Entity, Either<UUID, Integer>> source, float yOffset) {
		this.source = source;
		this.yOffset = yOffset;
	}

	@Override
	public Optional<Vec3d> getPos(World world) {
		if (source.left().isEmpty()) {
			findEntityInWorld(world);
		}

		return source.left().map(entity -> entity.getEntityPos().add(0.0, yOffset, 0.0));
	}

	/**
	 * Выполняет ленивый поиск сущности в мире по UUID или числовому ID.
	 * После нахождения заменяет идентификатор прямой ссылкой на сущность.
	 */
	@SuppressWarnings("unchecked")
	private void findEntityInWorld(World world) {
		((Optional<Entity>) source.map(
			Optional::of,
			entityId -> Optional.ofNullable(
				(Entity) entityId.map(
					uuid -> world instanceof ServerWorld serverWorld ? serverWorld.getEntity(uuid) : null,
					world::getEntityById
				)
			)
		)).ifPresent(entity -> source = Either.left(entity));
	}

	public UUID getUuid() {
		return source.map(
			Entity::getUuid,
			entityId -> entityId.map(
				Function.identity(),
				entityIdx -> {
					throw new RuntimeException("Unable to get UUID from numeric entity id");
				}
			)
		);
	}

	private int getEntityId() {
		return source.map(
			Entity::getId,
			entityId -> entityId.map(
				uuid -> {
					throw new IllegalStateException("Unable to get numeric entity id from UUID");
				},
				Function.identity()
			)
		);
	}

	@Override
	public PositionSourceType<EntityPositionSource> getType() {
		return PositionSourceType.ENTITY;
	}

	/**
	 * Реализация {@link PositionSourceType} для источника позиции на основе сущности.
	 */
	public static class Type implements PositionSourceType<EntityPositionSource> {

		@Override
		public MapCodec<EntityPositionSource> getCodec() {
			return EntityPositionSource.CODEC;
		}

		@Override
		public PacketCodec<ByteBuf, EntityPositionSource> getPacketCodec() {
			return EntityPositionSource.PACKET_CODEC;
		}
	}
}

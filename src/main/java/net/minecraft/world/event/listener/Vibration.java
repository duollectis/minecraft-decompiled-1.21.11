package net.minecraft.world.event.listener;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Запись о вибрационном сигнале, ожидающем обработки слушателем.
 * <p>
 * Хранит событие, расстояние до слушателя, позицию источника и идентификаторы
 * сущностей (UUID) для восстановления ссылок после десериализации.
 * Поле {@code entity} является транзиентным — не сериализуется, используется
 * только в оперативной памяти для ускорения поиска.
 */
public record Vibration(
	RegistryEntry<GameEvent> gameEvent,
	float distance,
	Vec3d pos,
	@Nullable UUID uuid,
	@Nullable UUID projectileOwnerUuid,
	@Nullable Entity entity
) {

	public static final Codec<Vibration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			GameEvent.CODEC.fieldOf("game_event").forGetter(Vibration::gameEvent),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("distance").forGetter(Vibration::distance),
			Vec3d.CODEC.fieldOf("pos").forGetter(Vibration::pos),
			Uuids.INT_STREAM_CODEC
				.lenientOptionalFieldOf("source")
				.forGetter(vibration -> Optional.ofNullable(vibration.uuid())),
			Uuids.INT_STREAM_CODEC
				.lenientOptionalFieldOf("projectile_owner")
				.forGetter(vibration -> Optional.ofNullable(vibration.projectileOwnerUuid()))
		).apply(
			instance,
			(event, distance, pos, uuid, projectileOwnerUuid) -> new Vibration(
				event,
				distance,
				pos,
				uuid.orElse(null),
				projectileOwnerUuid.orElse(null)
			)
		)
	);

	/** Конструктор для десериализации — без транзиентной ссылки на сущность. */
	public Vibration(
		RegistryEntry<GameEvent> gameEvent,
		float distance,
		Vec3d pos,
		@Nullable UUID uuid,
		@Nullable UUID projectileOwnerUuid
	) {
		this(gameEvent, distance, pos, uuid, projectileOwnerUuid, null);
	}

	/** Конструктор для создания вибрации в рантайме — с прямой ссылкой на сущность. */
	public Vibration(RegistryEntry<GameEvent> gameEvent, float distance, Vec3d pos, @Nullable Entity entity) {
		this(
			gameEvent,
			distance,
			pos,
			entity == null ? null : entity.getUuid(),
			getOwnerUuid(entity),
			entity
		);
	}

	private static @Nullable UUID getOwnerUuid(@Nullable Entity entity) {
		return entity instanceof ProjectileEntity projectile && projectile.getOwner() != null
			? projectile.getOwner().getUuid()
			: null;
	}

	/**
	 * Возвращает сущность-источник вибрации.
	 * Сначала проверяет транзиентную ссылку, затем ищет по UUID в мире.
	 */
	public Optional<Entity> getEntity(ServerWorld world) {
		return Optional.ofNullable(entity).or(() -> Optional.ofNullable(uuid).map(world::getEntity));
	}

	/**
	 * Возвращает владельца снаряда, если источником вибрации является снаряд.
	 * Сначала пытается получить владельца через живую ссылку на сущность,
	 * затем ищет по UUID владельца снаряда.
	 */
	public Optional<Entity> getOwner(ServerWorld world) {
		return getEntity(world)
			.filter(e -> e instanceof ProjectileEntity)
			.map(e -> (ProjectileEntity) e)
			.map(ProjectileEntity::getOwner)
			.or(() -> Optional.ofNullable(projectileOwnerUuid).map(world::getEntity));
	}
}

package net.minecraft.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityQueriable;
import net.minecraft.world.entity.UniquelyIdentifiable;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Ленивая ссылка на сущность, хранящая либо UUID (до разрешения), либо прямую ссылку на объект.
 * При первом обращении через {@link #resolve} UUID заменяется на живой объект.
 * Если сущность удалена из мира, ссылка автоматически деградирует обратно до UUID.
 *
 * @param <StoredEntityType> тип хранимой сущности
 */
public final class LazyEntityReference<StoredEntityType extends UniquelyIdentifiable> {

	private static final Codec<? extends LazyEntityReference<?>> CODEC =
		Uuids.INT_STREAM_CODEC.xmap(LazyEntityReference::new, LazyEntityReference::getUuid);
	private static final PacketCodec<ByteBuf, ? extends LazyEntityReference<?>> PACKET_CODEC =
		Uuids.PACKET_CODEC.xmap(LazyEntityReference::new, LazyEntityReference::getUuid);

	private Either<UUID, StoredEntityType> value;

	public static <Type extends UniquelyIdentifiable> Codec<LazyEntityReference<Type>> createCodec() {
		return (Codec<LazyEntityReference<Type>>) CODEC;
	}

	public static <Type extends UniquelyIdentifiable> PacketCodec<ByteBuf, LazyEntityReference<Type>> createPacketCodec() {
		return (PacketCodec<ByteBuf, LazyEntityReference<Type>>) PACKET_CODEC;
	}

	private LazyEntityReference(StoredEntityType value) {
		this.value = Either.right(value);
	}

	private LazyEntityReference(UUID value) {
		this.value = Either.left(value);
	}

	/**
	 * Создаёт ссылку на объект или возвращает {@code null}, если объект равен {@code null}.
	 *
	 * @param object объект для оборачивания
	 * @return ссылка или {@code null}
	 */
	public static <T extends UniquelyIdentifiable> @Nullable LazyEntityReference<T> of(@Nullable T object) {
		return object != null ? new LazyEntityReference<>(object) : null;
	}

	/**
	 * Создаёт ссылку по UUID без немедленного поиска сущности в мире.
	 *
	 * @param uuid UUID сущности
	 * @return ленивая ссылка
	 */
	public static <T extends UniquelyIdentifiable> LazyEntityReference<T> ofUUID(UUID uuid) {
		return new LazyEntityReference<>(uuid);
	}

	public UUID getUuid() {
		return (UUID) value.map(uuid -> uuid, UniquelyIdentifiable::getUuid);
	}

	/**
	 * Разрешает ссылку в живой объект через запрос к миру.
	 * Если кэшированный объект удалён, деградирует до UUID и выполняет поиск заново.
	 *
	 * @param world источник сущностей
	 * @param type  ожидаемый класс сущности
	 * @return живой объект или {@code null} если не найден
	 */
	public @Nullable StoredEntityType resolve(
		EntityQueriable<? extends UniquelyIdentifiable> world,
		Class<StoredEntityType> type
	) {
		Optional<StoredEntityType> cached = value.right();
		if (cached.isPresent()) {
			StoredEntityType entity = cached.get();
			if (!entity.isRemoved()) {
				return entity;
			}

			value = Either.left(entity.getUuid());
		}

		Optional<UUID> uuidOpt = value.left();
		if (uuidOpt.isPresent()) {
			StoredEntityType found = cast(world.lookup(uuidOpt.get()), type);
			if (found != null && !found.isRemoved()) {
				value = Either.right(found);
				return found;
			}
		}

		return null;
	}

	public @Nullable StoredEntityType getEntityByClass(World world, Class<StoredEntityType> clazz) {
		return PlayerEntity.class.isAssignableFrom(clazz)
			? resolve(world::getPlayerAnyDimension, clazz)
			: resolve(world::getEntityAnyDimension, clazz);
	}

	private @Nullable StoredEntityType cast(@Nullable UniquelyIdentifiable entity, Class<StoredEntityType> clazz) {
		return entity != null && clazz.isAssignableFrom(entity.getClass()) ? clazz.cast(entity) : null;
	}

	public boolean uuidEquals(StoredEntityType other) {
		return getUuid().equals(other.getUuid());
	}

	public void writeData(WriteView view, String key) {
		view.put(key, Uuids.INT_STREAM_CODEC, getUuid());
	}

	public static void writeData(@Nullable LazyEntityReference<?> entityRef, WriteView view, String key) {
		if (entityRef != null) {
			entityRef.writeData(view, key);
		}
	}

	public static <StoredEntityType extends UniquelyIdentifiable> @Nullable StoredEntityType resolve(
		@Nullable LazyEntityReference<StoredEntityType> entity, World world, Class<StoredEntityType> type
	) {
		return entity != null ? entity.getEntityByClass(world, type) : null;
	}

	public static @Nullable Entity getEntity(@Nullable LazyEntityReference<Entity> entityReference, World world) {
		return resolve(entityReference, world, Entity.class);
	}

	public static @Nullable LivingEntity getLivingEntity(
		@Nullable LazyEntityReference<LivingEntity> livingReference,
		World world
	) {
		return resolve(livingReference, world, LivingEntity.class);
	}

	public static @Nullable PlayerEntity getPlayerEntity(
		@Nullable LazyEntityReference<PlayerEntity> playerReference,
		World world
	) {
		return resolve(playerReference, world, PlayerEntity.class);
	}

	public static <StoredEntityType extends UniquelyIdentifiable> @Nullable LazyEntityReference<StoredEntityType> fromData(
		ReadView view,
		String key
	) {
		return view.<LazyEntityReference<StoredEntityType>>read(key, createCodec()).orElse(null);
	}

	/**
	 * Читает ссылку из NBT: сначала пробует UUID, затем строковое имя игрока.
	 * Используется для обратной совместимости со старыми форматами данных.
	 *
	 * @param view  источник данных
	 * @param key   ключ поля
	 * @param world мир для поиска игрока по имени
	 * @return ссылка или {@code null}
	 */
	public static <StoredEntityType extends UniquelyIdentifiable> @Nullable LazyEntityReference<StoredEntityType> fromDataOrPlayerName(
		ReadView view, String key, World world
	) {
		Optional<UUID> uuidOpt = view.read(key, Uuids.INT_STREAM_CODEC);
		return uuidOpt.isPresent()
			? ofUUID(uuidOpt.get())
			: view.getOptionalString(key)
				.map(name -> ServerConfigHandler.getPlayerUuidByName(world.getServer(), name))
				.<LazyEntityReference<StoredEntityType>>map(LazyEntityReference::new)
				.orElse(null);
	}

	@Override
	public boolean equals(Object object) {
		return object == this
			|| object instanceof LazyEntityReference<?> other && getUuid().equals(other.getUuid());
	}

	@Override
	public int hashCode() {
		return getUuid().hashCode();
	}
}

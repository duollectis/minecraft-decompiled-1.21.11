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
 * {@code LazyEntityReference}.
 */
public final class LazyEntityReference<StoredEntityType extends UniquelyIdentifiable> {

	private static final Codec<? extends LazyEntityReference<?>>
			CODEC =
			Uuids.INT_STREAM_CODEC.xmap(LazyEntityReference::new, LazyEntityReference::getUuid);
	private static final PacketCodec<ByteBuf, ? extends LazyEntityReference<?>> PACKET_CODEC = Uuids.PACKET_CODEC
			.xmap(LazyEntityReference::new, LazyEntityReference::getUuid);
	private Either<UUID, StoredEntityType> value;

	/**
	 * Создаёт codec.
	 *
	 * @return Codec> — результат операции
	 */
	public static <Type extends UniquelyIdentifiable> Codec<LazyEntityReference<Type>> createCodec() {
		return (Codec<LazyEntityReference<Type>>) CODEC;
	}

	/**
	 * Создаёт packet codec.
	 *
	 * @return PacketCodec> — результат операции
	 */
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
	 * Of.
	 *
	 * @param object object
	 *
	 * @return @Nullable LazyEntityReference — результат операции
	 */
	public static <T extends UniquelyIdentifiable> @Nullable LazyEntityReference<T> of(@Nullable T object) {
		return object != null ? new LazyEntityReference<>(object) : null;
	}

	/**
	 * Of u u i d.
	 *
	 * @param uuid uuid
	 *
	 * @return LazyEntityReference — результат операции
	 */
	public static <T extends UniquelyIdentifiable> LazyEntityReference<T> ofUUID(UUID uuid) {
		return new LazyEntityReference<>(uuid);
	}

	public UUID getUuid() {
		return (UUID) this.value.map(uuid -> uuid, UniquelyIdentifiable::getUuid);
	}

	public @Nullable StoredEntityType resolve(
			EntityQueriable<? extends UniquelyIdentifiable> world,
			Class<StoredEntityType> type
	) {
		Optional<StoredEntityType> optional = this.value.right();
		if (optional.isPresent()) {
			StoredEntityType uniquelyIdentifiable = optional.get();
			if (!uniquelyIdentifiable.isRemoved()) {
				return uniquelyIdentifiable;
			}

			this.value = Either.left(uniquelyIdentifiable.getUuid());
		}

		Optional<UUID> optional2 = this.value.left();
		if (optional2.isPresent()) {
			StoredEntityType uniquelyIdentifiable2 = this.cast(world.lookup(optional2.get()), type);
			if (uniquelyIdentifiable2 != null && !uniquelyIdentifiable2.isRemoved()) {
				this.value = Either.right(uniquelyIdentifiable2);
				return uniquelyIdentifiable2;
			}
		}

		return null;
	}

	public @Nullable StoredEntityType getEntityByClass(World world, Class<StoredEntityType> clazz) {
		return PlayerEntity.class.isAssignableFrom(clazz) ? this.resolve(world::getPlayerAnyDimension, clazz)
		                                                  : this.resolve(world::getEntityAnyDimension, clazz);
	}

	private @Nullable StoredEntityType cast(@Nullable UniquelyIdentifiable entity, Class<StoredEntityType> clazz) {
		return entity != null && clazz.isAssignableFrom(entity.getClass()) ? clazz.cast(entity) : null;
	}

	/**
	 * Uuid equals.
	 *
	 * @param o o
	 *
	 * @return boolean — результат операции
	 */
	public boolean uuidEquals(StoredEntityType o) {
		return this.getUuid().equals(o.getUuid());
	}

	/**
	 * Записывает data.
	 *
	 * @param view view
	 * @param key key
	 */
	public void writeData(WriteView view, String key) {
		view.put(key, Uuids.INT_STREAM_CODEC, this.getUuid());
	}

	/**
	 * Записывает data.
	 *
	 * @param entityRef entity ref
	 * @param view view
	 * @param key key
	 */
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

	public static <StoredEntityType extends UniquelyIdentifiable> @Nullable LazyEntityReference<StoredEntityType> fromDataOrPlayerName(
			ReadView view, String key, World world
	) {
		Optional<UUID> optional = view.read(key, Uuids.INT_STREAM_CODEC);
		return optional.isPresent()
		       ? ofUUID(optional.get())
		       : view.getOptionalString(key)
		             .map(name -> ServerConfigHandler.getPlayerUuidByName(world.getServer(), name))
		             .<LazyEntityReference<StoredEntityType>>map(LazyEntityReference::new)
		             .orElse(null);
	}

	@Override
	public boolean equals(Object object) {
		return object == this ? true : object instanceof LazyEntityReference<?> lazyEntityReference && this
		                                                                                               .getUuid()
		                                                                                               .equals(lazyEntityReference.getUuid());
	}

	@Override
	public int hashCode() {
		return this.getUuid().hashCode();
	}
}

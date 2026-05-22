package net.minecraft.world.event;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.listener.GameEventListener;
import org.jspecify.annotations.Nullable;

/**
 * Игровое событие — именованный сигнал, который сущности и блоки могут испускать в мир.
 * Каждое событие имеет радиус оповещения {@link #notificationRadius()}, в пределах которого
 * слушатели ({@link GameEventListener}) получают уведомление.
 */
public record GameEvent(int notificationRadius) {

	public static final int DEFAULT_RANGE = 16;

	public static final RegistryEntry.Reference<GameEvent> BLOCK_ACTIVATE = register("block_activate");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_ATTACH = register("block_attach");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_CHANGE = register("block_change");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_CLOSE = register("block_close");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_DEACTIVATE = register("block_deactivate");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_DESTROY = register("block_destroy");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_DETACH = register("block_detach");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_OPEN = register("block_open");
	public static final RegistryEntry.Reference<GameEvent> BLOCK_PLACE = register("block_place");
	public static final RegistryEntry.Reference<GameEvent> CONTAINER_CLOSE = register("container_close");
	public static final RegistryEntry.Reference<GameEvent> CONTAINER_OPEN = register("container_open");
	public static final RegistryEntry.Reference<GameEvent> DRINK = register("drink");
	public static final RegistryEntry.Reference<GameEvent> EAT = register("eat");
	public static final RegistryEntry.Reference<GameEvent> ELYTRA_GLIDE = register("elytra_glide");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_DAMAGE = register("entity_damage");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_DIE = register("entity_die");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_DISMOUNT = register("entity_dismount");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_INTERACT = register("entity_interact");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_MOUNT = register("entity_mount");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_PLACE = register("entity_place");
	public static final RegistryEntry.Reference<GameEvent> ENTITY_ACTION = register("entity_action");
	public static final RegistryEntry.Reference<GameEvent> EQUIP = register("equip");
	public static final RegistryEntry.Reference<GameEvent> EXPLODE = register("explode");
	public static final RegistryEntry.Reference<GameEvent> FLAP = register("flap");
	public static final RegistryEntry.Reference<GameEvent> FLUID_PICKUP = register("fluid_pickup");
	public static final RegistryEntry.Reference<GameEvent> FLUID_PLACE = register("fluid_place");
	public static final RegistryEntry.Reference<GameEvent> HIT_GROUND = register("hit_ground");
	public static final RegistryEntry.Reference<GameEvent> INSTRUMENT_PLAY = register("instrument_play");
	public static final RegistryEntry.Reference<GameEvent> ITEM_INTERACT_FINISH = register("item_interact_finish");
	public static final RegistryEntry.Reference<GameEvent> ITEM_INTERACT_START = register("item_interact_start");
	public static final RegistryEntry.Reference<GameEvent> JUKEBOX_PLAY = register("jukebox_play", 10);
	public static final RegistryEntry.Reference<GameEvent> JUKEBOX_STOP_PLAY = register("jukebox_stop_play", 10);
	public static final RegistryEntry.Reference<GameEvent> LIGHTNING_STRIKE = register("lightning_strike");
	public static final RegistryEntry.Reference<GameEvent> NOTE_BLOCK_PLAY = register("note_block_play");
	public static final RegistryEntry.Reference<GameEvent> PRIME_FUSE = register("prime_fuse");
	public static final RegistryEntry.Reference<GameEvent> PROJECTILE_LAND = register("projectile_land");
	public static final RegistryEntry.Reference<GameEvent> PROJECTILE_SHOOT = register("projectile_shoot");
	public static final RegistryEntry.Reference<GameEvent> SCULK_SENSOR_TENDRILS_CLICKING =
		register("sculk_sensor_tendrils_clicking");
	public static final RegistryEntry.Reference<GameEvent> SHEAR = register("shear");
	public static final RegistryEntry.Reference<GameEvent> SHRIEK = register("shriek", 32);
	public static final RegistryEntry.Reference<GameEvent> SPLASH = register("splash");
	public static final RegistryEntry.Reference<GameEvent> STEP = register("step");
	public static final RegistryEntry.Reference<GameEvent> SWIM = register("swim");
	public static final RegistryEntry.Reference<GameEvent> TELEPORT = register("teleport");
	public static final RegistryEntry.Reference<GameEvent> UNEQUIP = register("unequip");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_1 = register("resonate_1");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_2 = register("resonate_2");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_3 = register("resonate_3");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_4 = register("resonate_4");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_5 = register("resonate_5");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_6 = register("resonate_6");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_7 = register("resonate_7");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_8 = register("resonate_8");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_9 = register("resonate_9");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_10 = register("resonate_10");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_11 = register("resonate_11");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_12 = register("resonate_12");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_13 = register("resonate_13");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_14 = register("resonate_14");
	public static final RegistryEntry.Reference<GameEvent> RESONATE_15 = register("resonate_15");

	public static final Codec<RegistryEntry<GameEvent>> CODEC = RegistryFixedCodec.of(RegistryKeys.GAME_EVENT);

	/**
	 * Возвращает первое зарегистрированное событие — используется как значение по умолчанию
	 * при инициализации реестра.
	 */
	public static RegistryEntry<GameEvent> registerAndGetDefault(Registry<GameEvent> registry) {
		return BLOCK_ACTIVATE;
	}

	private static RegistryEntry.Reference<GameEvent> register(String id) {
		return register(id, DEFAULT_RANGE);
	}

	private static RegistryEntry.Reference<GameEvent> register(String id, int range) {
		return Registry.registerReference(Registries.GAME_EVENT, Identifier.ofVanilla(id), new GameEvent(range));
	}

	/**
	 * Описывает источник игрового события: сущность и/или состояние блока, которые его вызвали.
	 */
	public record Emitter(@Nullable Entity sourceEntity, @Nullable BlockState affectedState) {

		public static Emitter of(@Nullable Entity sourceEntity) {
			return new Emitter(sourceEntity, null);
		}

		public static Emitter of(@Nullable BlockState affectedState) {
			return new Emitter(null, affectedState);
		}

		public static Emitter of(@Nullable Entity sourceEntity, @Nullable BlockState affectedState) {
			return new Emitter(sourceEntity, affectedState);
		}
	}

	/**
	 * Сообщение о произошедшем игровом событии, адресованное конкретному слушателю.
	 * Реализует {@link Comparable} для сортировки по расстоянию от источника до слушателя,
	 * что позволяет обрабатывать ближайших слушателей первыми.
	 */
	public static final class Message implements Comparable<Message> {

		private final RegistryEntry<GameEvent> event;
		private final Vec3d emitterPos;
		private final Emitter emitter;
		private final GameEventListener listener;
		private final double distanceTraveled;

		public Message(
			RegistryEntry<GameEvent> event,
			Vec3d emitterPos,
			Emitter emitter,
			GameEventListener listener,
			Vec3d listenerPos
		) {
			this.event = event;
			this.emitterPos = emitterPos;
			this.emitter = emitter;
			this.listener = listener;
			this.distanceTraveled = emitterPos.squaredDistanceTo(listenerPos);
		}

		@Override
		public int compareTo(Message other) {
			return Double.compare(distanceTraveled, other.distanceTraveled);
		}

		public RegistryEntry<GameEvent> getEvent() {
			return event;
		}

		public Vec3d getEmitterPos() {
			return emitterPos;
		}

		public Emitter getEmitter() {
			return emitter;
		}

		public GameEventListener getListener() {
			return listener;
		}
	}
}

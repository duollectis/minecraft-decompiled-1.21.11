package net.minecraft.world.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.Vibration;
import net.minecraft.world.event.listener.VibrationSelector;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Интерфейс для объектов, способных принимать и обрабатывать вибрационные сигналы.
 * Реализуется блоками и сущностями (например, Sculk Sensor), которые реагируют
 * на игровые события в радиусе действия.
 */
public interface Vibrations {

	/** Максимальная сила сигнала компаратора. */
	int MAX_SIGNAL_STRENGTH = 15;

	/** Частота по умолчанию для событий, не имеющих явного маппинга. */
	int DEFAULT_FREQUENCY = 0;

	List<RegistryKey<GameEvent>> RESONATIONS = List.of(
		GameEvent.RESONATE_1.registryKey(),
		GameEvent.RESONATE_2.registryKey(),
		GameEvent.RESONATE_3.registryKey(),
		GameEvent.RESONATE_4.registryKey(),
		GameEvent.RESONATE_5.registryKey(),
		GameEvent.RESONATE_6.registryKey(),
		GameEvent.RESONATE_7.registryKey(),
		GameEvent.RESONATE_8.registryKey(),
		GameEvent.RESONATE_9.registryKey(),
		GameEvent.RESONATE_10.registryKey(),
		GameEvent.RESONATE_11.registryKey(),
		GameEvent.RESONATE_12.registryKey(),
		GameEvent.RESONATE_13.registryKey(),
		GameEvent.RESONATE_14.registryKey(),
		GameEvent.RESONATE_15.registryKey()
	);

	/**
	 * Таблица соответствия игровых событий частотам вибрации (1–15).
	 * Используется для определения сигнала компаратора у Sculk Sensor.
	 */
	ToIntFunction<RegistryKey<GameEvent>> FREQUENCIES = Util.make(
		new Reference2IntOpenHashMap<>(), frequencies -> {
			frequencies.defaultReturnValue(DEFAULT_FREQUENCY);
			frequencies.put(GameEvent.STEP.registryKey(), 1);
			frequencies.put(GameEvent.SWIM.registryKey(), 1);
			frequencies.put(GameEvent.FLAP.registryKey(), 1);
			frequencies.put(GameEvent.PROJECTILE_LAND.registryKey(), 2);
			frequencies.put(GameEvent.HIT_GROUND.registryKey(), 2);
			frequencies.put(GameEvent.SPLASH.registryKey(), 2);
			frequencies.put(GameEvent.ITEM_INTERACT_FINISH.registryKey(), 3);
			frequencies.put(GameEvent.PROJECTILE_SHOOT.registryKey(), 3);
			frequencies.put(GameEvent.INSTRUMENT_PLAY.registryKey(), 3);
			frequencies.put(GameEvent.ENTITY_ACTION.registryKey(), 4);
			frequencies.put(GameEvent.ELYTRA_GLIDE.registryKey(), 4);
			frequencies.put(GameEvent.UNEQUIP.registryKey(), 4);
			frequencies.put(GameEvent.ENTITY_DISMOUNT.registryKey(), 5);
			frequencies.put(GameEvent.EQUIP.registryKey(), 5);
			frequencies.put(GameEvent.ENTITY_INTERACT.registryKey(), 6);
			frequencies.put(GameEvent.SHEAR.registryKey(), 6);
			frequencies.put(GameEvent.ENTITY_MOUNT.registryKey(), 6);
			frequencies.put(GameEvent.ENTITY_DAMAGE.registryKey(), 7);
			frequencies.put(GameEvent.DRINK.registryKey(), 8);
			frequencies.put(GameEvent.EAT.registryKey(), 8);
			frequencies.put(GameEvent.CONTAINER_CLOSE.registryKey(), 9);
			frequencies.put(GameEvent.BLOCK_CLOSE.registryKey(), 9);
			frequencies.put(GameEvent.BLOCK_DEACTIVATE.registryKey(), 9);
			frequencies.put(GameEvent.BLOCK_DETACH.registryKey(), 9);
			frequencies.put(GameEvent.CONTAINER_OPEN.registryKey(), 10);
			frequencies.put(GameEvent.BLOCK_OPEN.registryKey(), 10);
			frequencies.put(GameEvent.BLOCK_ACTIVATE.registryKey(), 10);
			frequencies.put(GameEvent.BLOCK_ATTACH.registryKey(), 10);
			frequencies.put(GameEvent.PRIME_FUSE.registryKey(), 10);
			frequencies.put(GameEvent.NOTE_BLOCK_PLAY.registryKey(), 10);
			frequencies.put(GameEvent.BLOCK_CHANGE.registryKey(), 11);
			frequencies.put(GameEvent.BLOCK_DESTROY.registryKey(), 12);
			frequencies.put(GameEvent.FLUID_PICKUP.registryKey(), 12);
			frequencies.put(GameEvent.BLOCK_PLACE.registryKey(), 13);
			frequencies.put(GameEvent.FLUID_PLACE.registryKey(), 13);
			frequencies.put(GameEvent.ENTITY_PLACE.registryKey(), 14);
			frequencies.put(GameEvent.LIGHTNING_STRIKE.registryKey(), 14);
			frequencies.put(GameEvent.TELEPORT.registryKey(), 14);
			frequencies.put(GameEvent.ENTITY_DIE.registryKey(), 15);
			frequencies.put(GameEvent.EXPLODE.registryKey(), 15);

			for (int frequency = 1; frequency <= MAX_SIGNAL_STRENGTH; frequency++) {
				frequencies.put(getResonation(frequency), frequency);
			}
		}
	);

	ListenerData getVibrationListenerData();

	Callback getVibrationCallback();

	static int getFrequency(RegistryEntry<GameEvent> gameEvent) {
		return gameEvent.getKey().map(Vibrations::getFrequency).orElse(DEFAULT_FREQUENCY);
	}

	static int getFrequency(RegistryKey<GameEvent> gameEvent) {
		return FREQUENCIES.applyAsInt(gameEvent);
	}

	/**
	 * Возвращает ключ события резонанса для заданной частоты (1–15).
	 */
	static RegistryKey<GameEvent> getResonation(int frequency) {
		return RESONATIONS.get(frequency - 1);
	}

	/**
	 * Вычисляет силу сигнала компаратора на основе расстояния до источника вибрации.
	 * Чем ближе источник, тем сильнее сигнал (от 1 до 15).
	 *
	 * @param distance расстояние от слушателя до источника вибрации
	 * @param range    максимальный радиус обнаружения слушателя
	 * @return сила сигнала в диапазоне [1, 15]
	 */
	static int getSignalStrength(float distance, int range) {
		double distanceRatio = (double) MAX_SIGNAL_STRENGTH / range;
		return Math.max(1, MAX_SIGNAL_STRENGTH - MathHelper.floor(distanceRatio * distance));
	}

	/**
	 * Колбэк, реализуемый объектом-слушателем вибраций.
	 * Определяет, какие события принимаются, и обрабатывает принятую вибрацию.
	 */
	interface Callback {

		int getRange();

		PositionSource getPositionSource();

		/**
		 * Проверяет, принимает ли слушатель данное событие в данной позиции.
		 * Вызывается после базовой фильтрации {@link #canAccept}.
		 */
		boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter);

		/**
		 * Обрабатывает принятую вибрацию после истечения задержки распространения.
		 *
		 * @param sourceEntity  сущность-источник события (может быть null)
		 * @param entity        владелец снаряда или сама сущность (может быть null)
		 * @param distance      расстояние, пройденное вибрацией
		 */
		void accept(
			ServerWorld world,
			BlockPos pos,
			RegistryEntry<GameEvent> event,
			@Nullable Entity sourceEntity,
			@Nullable Entity entity,
			float distance
		);

		default TagKey<GameEvent> getTag() {
			return GameEventTags.VIBRATIONS;
		}

		default boolean triggersAvoidCriterion() {
			return false;
		}

		default boolean requiresTickingChunksAround() {
			return false;
		}

		default int getDelay(float distance) {
			return MathHelper.floor(distance);
		}

		/**
		 * Базовая фильтрация: проверяет тег события, режим наблюдателя,
		 * скрытность сущности и подавление вибраций шерстяными блоками.
		 */
		default boolean canAccept(RegistryEntry<GameEvent> gameEvent, GameEvent.Emitter emitter) {
			if (!gameEvent.isIn(getTag())) {
				return false;
			}

			Entity entity = emitter.sourceEntity();
			if (entity != null) {
				if (entity.isSpectator()) {
					return false;
				}

				if (entity.bypassesSteppingEffects() && gameEvent.isIn(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
					if (triggersAvoidCriterion() && entity instanceof ServerPlayerEntity serverPlayer) {
						Criteria.AVOID_VIBRATION.trigger(serverPlayer);
					}

					return false;
				}

				if (entity.occludeVibrationSignals()) {
					return false;
				}
			}

			return emitter.affectedState() == null || !emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS);
		}

		default void onListen() {
		}
	}

	/**
	 * Хранит текущее состояние слушателя вибраций: активную вибрацию,
	 * задержку до её обработки и селектор для выбора наиболее приоритетной вибрации.
	 */
	final class ListenerData {

		public static final String LISTENER_NBT_KEY = "listener";

		public static Codec<ListenerData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Vibration.CODEC
					.lenientOptionalFieldOf("event")
					.forGetter(data -> Optional.ofNullable(data.vibration)),
				VibrationSelector.CODEC.fieldOf("selector").forGetter(ListenerData::getSelector),
				Codecs.NON_NEGATIVE_INT
					.fieldOf("event_delay")
					.orElse(0)
					.forGetter(ListenerData::getDelay)
			).apply(
				instance,
				(vibration, selector, delay) -> new ListenerData(
					vibration.orElse(null), selector, delay, true
				)
			)
		);

		@Nullable Vibration vibration;
		private int delay;
		final VibrationSelector vibrationSelector;
		private boolean spawnParticle;

		private ListenerData(
			@Nullable Vibration vibration,
			VibrationSelector vibrationSelector,
			int delay,
			boolean spawnParticle
		) {
			this.vibration = vibration;
			this.delay = delay;
			this.vibrationSelector = vibrationSelector;
			this.spawnParticle = spawnParticle;
		}

		public ListenerData() {
			this(null, new VibrationSelector(), 0, false);
		}

		public VibrationSelector getSelector() {
			return vibrationSelector;
		}

		public @Nullable Vibration getVibration() {
			return vibration;
		}

		public void setVibration(@Nullable Vibration vibration) {
			this.vibration = vibration;
		}

		public int getDelay() {
			return delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

		public void tickDelay() {
			delay = Math.max(0, delay - 1);
		}

		public boolean shouldSpawnParticle() {
			return spawnParticle;
		}

		public void setSpawnParticle(boolean spawnParticle) {
			this.spawnParticle = spawnParticle;
		}
	}

	/**
	 * Утилитный интерфейс с логикой тика вибрационного слушателя.
	 * Управляет жизненным циклом вибрации: выбор → задержка → обработка → частицы.
	 */
	interface Ticker {

		/**
		 * Выполняет один тик обработки вибрации: выбирает новую вибрацию из селектора,
		 * отсчитывает задержку распространения и вызывает колбэк при её истечении.
		 */
		static void tick(World world, ListenerData listenerData, Callback callback) {
			if (!(world instanceof ServerWorld serverWorld)) {
				return;
			}

			if (listenerData.vibration == null) {
				tryListen(serverWorld, listenerData, callback);
			}

			if (listenerData.vibration != null) {
				boolean wasDelayed = listenerData.getDelay() > 0;
				spawnVibrationParticle(serverWorld, listenerData, callback);
				listenerData.tickDelay();

				if (listenerData.getDelay() <= 0) {
					wasDelayed = accept(serverWorld, listenerData, callback, listenerData.vibration);
				}

				if (wasDelayed) {
					callback.onListen();
				}
			}
		}

		/**
		 * Пытается извлечь вибрацию из селектора и начать её обработку:
		 * устанавливает задержку и спавнит начальную частицу.
		 */
		private static void tryListen(ServerWorld world, ListenerData listenerData, Callback callback) {
			listenerData.getSelector()
				.getVibrationToTick(world.getTime())
				.ifPresent(vibration -> {
					listenerData.setVibration(vibration);
					Vec3d vibrationPos = vibration.pos();
					listenerData.setDelay(callback.getDelay(vibration.distance()));
					world.spawnParticles(
						new VibrationParticleEffect(callback.getPositionSource(), listenerData.getDelay()),
						vibrationPos.x, vibrationPos.y, vibrationPos.z,
						1, 0.0, 0.0, 0.0, 0.0
					);
					callback.onListen();
					listenerData.getSelector().clear();
				});
		}

		/**
		 * Спавнит частицу вибрации, интерполируя её позицию между источником и слушателем
		 * в зависимости от оставшейся задержки.
		 */
		private static void spawnVibrationParticle(ServerWorld world, ListenerData listenerData, Callback callback) {
			if (!listenerData.shouldSpawnParticle()) {
				return;
			}

			if (listenerData.vibration == null) {
				listenerData.setSpawnParticle(false);
				return;
			}

			Vec3d vibrationPos = listenerData.vibration.pos();
			PositionSource positionSource = callback.getPositionSource();
			Vec3d listenerPos = positionSource.getPos(world).orElse(vibrationPos);
			int remainingDelay = listenerData.getDelay();
			int totalDelay = callback.getDelay(listenerData.vibration.distance());
			double progress = 1.0 - (double) remainingDelay / totalDelay;
			double particleX = MathHelper.lerp(progress, vibrationPos.x, listenerPos.x);
			double particleY = MathHelper.lerp(progress, vibrationPos.y, listenerPos.y);
			double particleZ = MathHelper.lerp(progress, vibrationPos.z, listenerPos.z);
			boolean spawned = world.spawnParticles(
				new VibrationParticleEffect(positionSource, remainingDelay),
				particleX, particleY, particleZ,
				1, 0.0, 0.0, 0.0, 0.0
			) > 0;

			if (spawned) {
				listenerData.setSpawnParticle(false);
			}
		}

		/**
		 * Передаёт вибрацию в колбэк слушателя, если чанки вокруг него тикают.
		 *
		 * @return {@code true}, если вибрация была успешно обработана
		 */
		private static boolean accept(
			ServerWorld world,
			ListenerData listenerData,
			Callback callback,
			Vibration vibration
		) {
			BlockPos vibrationBlockPos = BlockPos.ofFloored(vibration.pos());
			BlockPos listenerBlockPos = callback.getPositionSource()
				.getPos(world)
				.map(BlockPos::ofFloored)
				.orElse(vibrationBlockPos);

			if (callback.requiresTickingChunksAround() && !areChunksTickingAround(world, listenerBlockPos)) {
				return false;
			}

			callback.accept(
				world,
				vibrationBlockPos,
				vibration.gameEvent(),
				vibration.getEntity(world).orElse(null),
				vibration.getOwner(world).orElse(null),
				VibrationListener.getTravelDelay(vibrationBlockPos, listenerBlockPos)
			);
			listenerData.setVibration(null);
			return true;
		}

		/**
		 * Проверяет, тикают ли все 9 чанков (3×3) вокруг заданной позиции.
		 * Необходимо для корректной работы Sculk Shrieker.
		 */
		private static boolean areChunksTickingAround(World world, BlockPos pos) {
			ChunkPos chunkPos = new ChunkPos(pos);

			for (int chunkX = chunkPos.x - 1; chunkX <= chunkPos.x + 1; chunkX++) {
				for (int chunkZ = chunkPos.z - 1; chunkZ <= chunkPos.z + 1; chunkZ++) {
					if (!world.shouldTickBlocksInChunk(ChunkPos.toLong(chunkX, chunkZ))
						|| world.getChunkManager().getWorldChunk(chunkX, chunkZ) == null
					) {
						return false;
					}
				}
			}

			return true;
		}
	}

	/**
	 * Реализация {@link GameEventListener} для объектов, реализующих {@link Vibrations}.
	 * Принимает игровые события, фильтрует их и передаёт в {@link VibrationSelector}.
	 */
	class VibrationListener implements GameEventListener {

		private final Vibrations receiver;

		public VibrationListener(Vibrations receiver) {
			this.receiver = receiver;
		}

		@Override
		public PositionSource getPositionSource() {
			return receiver.getVibrationCallback().getPositionSource();
		}

		@Override
		public int getRange() {
			return receiver.getVibrationCallback().getRange();
		}

		/**
		 * Обрабатывает входящее игровое событие: проверяет все условия принятия
		 * и при успехе добавляет вибрацию в селектор.
		 *
		 * @return {@code true}, если событие было принято слушателем
		 */
		@Override
		public boolean listen(
			ServerWorld world,
			RegistryEntry<GameEvent> event,
			GameEvent.Emitter emitter,
			Vec3d emitterPos
		) {
			ListenerData listenerData = receiver.getVibrationListenerData();
			Callback callback = receiver.getVibrationCallback();

			if (listenerData.getVibration() != null) {
				return false;
			}

			if (!callback.canAccept(event, emitter)) {
				return false;
			}

			Optional<Vec3d> listenerPosOptional = callback.getPositionSource().getPos(world);
			if (listenerPosOptional.isEmpty()) {
				return false;
			}

			Vec3d listenerPos = listenerPosOptional.get();
			if (!callback.accepts(world, BlockPos.ofFloored(emitterPos), event, emitter)) {
				return false;
			}

			if (isOccluded(world, emitterPos, listenerPos)) {
				return false;
			}

			listen(world, listenerData, event, emitter, emitterPos, listenerPos);
			return true;
		}

		/**
		 * Принудительно регистрирует вибрацию, минуя стандартные проверки фильтрации.
		 * Используется для специальных случаев (например, резонанс амethyst).
		 */
		public void forceListen(
			ServerWorld world,
			RegistryEntry<GameEvent> event,
			GameEvent.Emitter emitter,
			Vec3d emitterPos
		) {
			receiver.getVibrationCallback()
				.getPositionSource()
				.getPos(world)
				.ifPresent(listenerPos -> listen(
					world,
					receiver.getVibrationListenerData(),
					event,
					emitter,
					emitterPos,
					listenerPos
				));
		}

		private void listen(
			ServerWorld world,
			ListenerData listenerData,
			RegistryEntry<GameEvent> event,
			GameEvent.Emitter emitter,
			Vec3d emitterPos,
			Vec3d listenerPos
		) {
			listenerData.vibrationSelector.tryAccept(
				new Vibration(
					event,
					(float) emitterPos.distanceTo(listenerPos),
					emitterPos,
					emitter.sourceEntity()
				),
				world.getTime()
			);
		}

		/**
		 * Вычисляет расстояние распространения вибрации между двумя блочными позициями.
		 */
		public static float getTravelDelay(BlockPos emitterPos, BlockPos listenerPos) {
			return (float) Math.sqrt(emitterPos.getSquaredDistance(listenerPos));
		}

		/**
		 * Проверяет, перекрыт ли путь вибрации блоками из тега {@code OCCLUDES_VIBRATION_SIGNALS}.
		 * Луч пускается из центра каждой из 6 граней блока-источника — если хотя бы один
		 * луч проходит свободно, вибрация не считается заблокированной.
		 */
		private static boolean isOccluded(World world, Vec3d emitterPos, Vec3d listenerPos) {
			Vec3d emitterCenter = new Vec3d(
				MathHelper.floor(emitterPos.x) + 0.5,
				MathHelper.floor(emitterPos.y) + 0.5,
				MathHelper.floor(emitterPos.z) + 0.5
			);
			Vec3d listenerCenter = new Vec3d(
				MathHelper.floor(listenerPos.x) + 0.5,
				MathHelper.floor(listenerPos.y) + 0.5,
				MathHelper.floor(listenerPos.z) + 0.5
			);

			for (Direction direction : Direction.values()) {
				Vec3d rayStart = emitterCenter.offset(direction, 1.0E-5F);
				boolean rayBlocked = world.raycast(
					new BlockStateRaycastContext(
						rayStart,
						listenerCenter,
						state -> state.isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS)
					)
				).getType() != HitResult.Type.BLOCK;

				if (rayBlocked) {
					return false;
				}
			}

			return true;
		}
	}
}

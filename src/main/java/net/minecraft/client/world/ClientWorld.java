package net.minecraft.client.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.EndLightFlashManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.sound.EndLightFlashSoundInstance;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.EntityHandler;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.TickManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Клиентская реализация игрового мира Minecraft.
 *
 * <p>Управляет рендерингом, тиками сущностей, частицами, звуками и синхронизацией
 * состояния блоков с сервером через {@link ClientPlayNetworkHandler}.
 * Не выполняет серверную логику — только отображает и интерполирует данные,
 * полученные по сети.</p>
 */
@Environment(EnvType.CLIENT)
public class ClientWorld extends World implements DataCache.CacheContext<ClientWorld> {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Text QUITTING_MULTIPLAYER_TEXT = Text.translatable("multiplayer.status.quitting");
	private static final double PARTICLE_Y_OFFSET = 0.05;
	private static final double BREAK_PARTICLE_STEP = 0.25;
	private static final float PARTICLE_OFFSET = 0.1F;
	private static final double DISTANCE_SOUND_THRESHOLD_SQ = 100.0;
	private static final int AMBIENT_TICK_INTERVAL = 10;
	private static final int AMBIENT_TICK_RANGE = 1000;
	final EntityList entityList = new EntityList();
	private final ClientEntityManager<Entity>
			entityManager =
			new ClientEntityManager<>(Entity.class, new ClientWorld.ClientEntityHandler());
	private final ClientPlayNetworkHandler networkHandler;
	private final WorldRenderer worldRenderer;
	private final WorldEventHandler worldEventHandler;
	private final ClientWorld.Properties clientWorldProperties;
	private final TickManager tickManager;
	private final @Nullable EndLightFlashManager endLightFlashManager;
	private final MinecraftClient client = MinecraftClient.getInstance();
	final List<AbstractClientPlayerEntity> players = Lists.newArrayList();
	final List<EnderDragonPart> enderDragonParts = Lists.newArrayList();
	private final Map<MapIdComponent, MapState> mapStates = Maps.newHashMap();
	private int lightningTicksLeft;
	private final Object2ObjectArrayMap<ColorResolver, BiomeColorCache> colorCache = Util.make(
			new Object2ObjectArrayMap(3), map -> {
				map.put(
						BiomeColors.GRASS_COLOR,
						new BiomeColorCache(pos -> this.calculateColor(pos, BiomeColors.GRASS_COLOR))
				);
				map.put(
						BiomeColors.FOLIAGE_COLOR,
						new BiomeColorCache(pos -> this.calculateColor(pos, BiomeColors.FOLIAGE_COLOR))
				);
				map.put(
						BiomeColors.DRY_FOLIAGE_COLOR,
						new BiomeColorCache(pos -> this.calculateColor(pos, BiomeColors.DRY_FOLIAGE_COLOR))
				);
				map.put(
						BiomeColors.WATER_COLOR,
						new BiomeColorCache(pos -> this.calculateColor(pos, BiomeColors.WATER_COLOR))
				);
			}
	);
	private final ClientChunkManager chunkManager;
	private final Deque<Runnable> chunkUpdaters = Queues.newArrayDeque();
	private int simulationDistance;
	private final PendingUpdateManager pendingUpdateManager = new PendingUpdateManager();
	private final Set<BlockEntity> blockEntities = new ReferenceOpenHashSet();
	private final BlockParticleEffectsManager blockParticlesManager = new BlockParticleEffectsManager();
	private final WorldBorder worldBorder = new WorldBorder();
	private final WorldEnvironmentAttributeAccess environmentAttributeAccess;
	private final int seaLevel;
	private boolean shouldTickTimeOfDay;
	private static final Set<Item> BLOCK_MARKER_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);

	/**
	 * Подтверждает серверное действие игрока и применяет все накопленные
	 * отложенные обновления блоков с порядковым номером ≤ {@code sequence}.
	 *
	 * @param sequence порядковый номер подтверждённого действия
	 */
	public void handlePlayerActionResponse(int sequence) {
		if (SharedConstants.BLOCK_BREAK) {
			LOGGER.debug("ACK {}", sequence);
		}

		pendingUpdateManager.processPendingUpdates(sequence, this);
	}

	@Override
	public void loadBlockEntity(BlockEntity blockEntity) {
		BlockEntityRenderer<BlockEntity, ?> blockEntityRenderer = client.getBlockEntityRenderDispatcher().get(blockEntity);
		if (blockEntityRenderer != null && blockEntityRenderer.rendersOutsideBoundingBox()) {
			blockEntities.add(blockEntity);
		}
	}

	public Set<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	/**
	 * Применяет обновление блока, полученное от сервера, если для данной позиции
	 * нет ожидающего клиентского предсказания.
	 *
	 * @param pos   позиция блока
	 * @param state новое состояние блока
	 * @param flags флаги обновления
	 */
	public void handleBlockUpdate(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		if (!pendingUpdateManager.hasPendingUpdate(pos, state)) {
			super.setBlockState(pos, state, flags, 512);
		}
	}

	/**
	 * Принудительно устанавливает состояние блока, корректируя позицию игрока
	 * при коллизии — используется для разрешения конфликтов предсказания.
	 *
	 * @param pos       позиция блока
	 * @param state     корректное состояние блока от сервера
	 * @param playerPos скорректированная позиция игрока
	 */
	public void processPendingUpdate(BlockPos pos, BlockState state, Vec3d playerPos) {
		BlockState currentState = getBlockState(pos);
		if (currentState == state) {
			return;
		}

		setBlockState(pos, state, 19);
		PlayerEntity playerEntity = client.player;
		if (this == playerEntity.getEntityWorld() && playerEntity.collidesWithStateAtPos(pos, state)) {
			playerEntity.updatePosition(playerPos.x, playerPos.y, playerPos.z);
		}
	}

	public PendingUpdateManager getPendingUpdateManager() {
		return pendingUpdateManager;
	}

	@Override
	public boolean setBlockState(
			BlockPos pos,
			BlockState state,
			@Block.SetBlockStateFlag int flags,
			int maxUpdateDepth
	) {
		if (!pendingUpdateManager.hasPendingSequence()) {
			return super.setBlockState(pos, state, flags, maxUpdateDepth);
		}

		BlockState previousState = getBlockState(pos);
		boolean updated = super.setBlockState(pos, state, flags, maxUpdateDepth);
		if (updated) {
			pendingUpdateManager.addPendingUpdate(pos, previousState, client.player);
		}

		return updated;
	}

	public ClientWorld(
			ClientPlayNetworkHandler networkHandler,
			ClientWorld.Properties properties,
			RegistryKey<World> registryRef,
			RegistryEntry<DimensionType> dimensionType,
			int loadDistance,
			int simulationDistance,
			WorldRenderer worldRenderer,
			boolean debugWorld,
			long seed,
			int seaLevel
	) {
		super(
				properties,
				registryRef,
				networkHandler.getRegistryManager(),
				dimensionType,
				true,
				debugWorld,
				seed,
				1000000
		);
		this.networkHandler = networkHandler;
		this.chunkManager = new ClientChunkManager(this, loadDistance);
		this.tickManager = new TickManager();
		this.clientWorldProperties = properties;
		this.worldRenderer = worldRenderer;
		this.seaLevel = seaLevel;
		this.worldEventHandler = new WorldEventHandler(client, this);
		this.endLightFlashManager = dimensionType.value().getSkybox() ? new EndLightFlashManager() : null;
		setSpawnPoint(WorldProperties.SpawnPoint.create(registryRef, new BlockPos(8, 64, 8), 0.0F, 0.0F));
		this.simulationDistance = simulationDistance;
		this.environmentAttributeAccess = addClientSideAttributes(WorldEnvironmentAttributeAccess.builder()).build();
		calculateAmbientDarkness();
		if (canHaveWeather()) {
			initWeatherGradients();
		}
	}

	private WorldEnvironmentAttributeAccess.Builder addClientSideAttributes(WorldEnvironmentAttributeAccess.Builder builder) {
		builder.world(this);
		int lightningColor = ColorHelper.getArgb(204, 204, 255);
		builder.timeBased(
				EnvironmentAttributes.SKY_COLOR_VISUAL,
				(color, time) -> getLightningTicksLeft() > 0 ? ColorHelper.lerp(0.22F, color, lightningColor) : color
		);
		builder.timeBased(
				EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL,
				(factor, time) -> getLightningTicksLeft() > 0 ? 1.0F : factor
		);
		return builder;
	}

	/**
	 * Добавляет задачу обновления чанка в очередь для отложенного выполнения.
	 *
	 * @param updater задача обновления
	 */
	public void enqueueChunkUpdate(Runnable updater) {
		chunkUpdaters.add(updater);
	}

	/**
	 * Выполняет пакет задач из очереди обновлений чанков.
	 * Размер пакета адаптируется к размеру очереди: при малой очереди
	 * обрабатывается минимум {@code AMBIENT_TICK_INTERVAL} задач за тик.
	 */
	public void runQueuedChunkUpdates() {
		int queueSize = chunkUpdaters.size();
		int batchSize = queueSize < AMBIENT_TICK_RANGE
				? Math.max(AMBIENT_TICK_INTERVAL, queueSize / AMBIENT_TICK_INTERVAL)
				: queueSize;

		for (int processed = 0; processed < batchSize; processed++) {
			Runnable updater = chunkUpdaters.poll();
			if (updater == null) {
				break;
			}

			updater.run();
		}
	}

	public @Nullable EndLightFlashManager getEndLightFlashManager() {
		return endLightFlashManager;
	}

	/**
	 * Выполняет основной тик клиентского мира: обновляет темноту, границу мира,
	 * время, молнии, вспышки Края, частицы блоков и чанки.
	 *
	 * @param shouldKeepTicking поставщик условия продолжения тика
	 */
	public void tick(BooleanSupplier shouldKeepTicking) {
		calculateAmbientDarkness();
		if (getTickManager().shouldTick()) {
			getWorldBorder().tick();
			tickTime();
		}

		if (lightningTicksLeft > 0) {
			setLightningTicksLeft(lightningTicksLeft - 1);
		}

		if (endLightFlashManager != null) {
			endLightFlashManager.tick(getTime());
			if (endLightFlashManager.shouldFlash() && !(client.currentScreen instanceof CreditsScreen)) {
				client
						.getSoundManager()
						.play(
								new EndLightFlashSoundInstance(
										SoundEvents.WEATHER_END_FLASH,
										SoundCategory.WEATHER,
										random,
										client.gameRenderer.getCamera(),
										endLightFlashManager.getPitch(),
										endLightFlashManager.getYaw()
								),
								30
						);
			}
		}

		blockParticlesManager.tick(this);

		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("blocks")) {
			chunkManager.tick(shouldKeepTicking, true);
		}

		FlightProfiler.INSTANCE.onClientFps(client.getCurrentFps());
		getEnvironmentAttributes().tick();
	}

	private void tickTime() {
		clientWorldProperties.setTime(clientWorldProperties.getTime() + 1L);
		if (shouldTickTimeOfDay) {
			clientWorldProperties.setTimeOfDay(clientWorldProperties.getTimeOfDay() + 1L);
		}
	}

	public void setTime(long time, long timeOfDay, boolean shouldTickTimeOfDay) {
		clientWorldProperties.setTime(time);
		clientWorldProperties.setTimeOfDay(timeOfDay);
		this.shouldTickTimeOfDay = shouldTickTimeOfDay;
	}

	public Iterable<Entity> getEntities() {
		return getEntityLookup().iterate();
	}

	/**
	 * Выполняет тик всех активных сущностей мира, пропуская пассажиров
	 * и сущности, которые должны быть пропущены менеджером тиков.
	 */
	public void tickEntities() {
		entityList.forEach(entity -> {
			if (!entity.isRemoved() && !entity.hasVehicle() && !tickManager.shouldSkipTick(entity)) {
				tickEntity(this::tickEntity, entity);
			}
		});
	}

	public boolean hasEntity(Entity entity) {
		return entityList.has(entity);
	}

	@Override
	public boolean shouldUpdatePostDeath(Entity entity) {
		return entity.getChunkPos().getChebyshevDistance(client.player.getChunkPos()) <= simulationDistance;
	}

	/**
	 * Выполняет тик сущности: сбрасывает позицию, увеличивает возраст,
	 * вызывает {@code tick()} и рекурсивно тикает всех пассажиров.
	 *
	 * @param entity сущность для тика
	 */
	public void tickEntity(Entity entity) {
		entity.resetPosition();
		entity.age++;
		Profilers.get().push(() -> Registries.ENTITY_TYPE.getId(entity.getType()).toString());
		entity.tick();
		Profilers.get().pop();

		for (Entity passenger : entity.getPassengerList()) {
			tickPassenger(entity, passenger);
		}
	}

	private void tickPassenger(Entity vehicle, Entity passenger) {
		if (passenger.isRemoved() || passenger.getVehicle() != vehicle) {
			passenger.stopRiding();
			return;
		}

		if (passenger instanceof PlayerEntity || entityList.has(passenger)) {
			passenger.resetPosition();
			passenger.age++;
			passenger.tickRiding();

			for (Entity nested : passenger.getPassengerList()) {
				tickPassenger(passenger, nested);
			}
		}
	}

	/**
	 * Выгружает блочные сущности чанка и отключает его освещение и тикинг.
	 *
	 * @param chunk выгружаемый чанк
	 */
	public void unloadBlockEntities(WorldChunk chunk) {
		chunk.clear();
		chunkManager.getLightingProvider().setColumnEnabled(chunk.getPos(), false);
		entityManager.stopTicking(chunk.getPos());
	}

	/**
	 * Сбрасывает кэш цветов биомов для указанного чанка и запускает тикинг сущностей.
	 *
	 * @param chunkPos позиция чанка
	 */
	public void resetChunkColor(ChunkPos chunkPos) {
		colorCache.forEach((resolver, cache) -> cache.reset(chunkPos.x, chunkPos.z));
		entityManager.startTicking(chunkPos);
	}

	/**
	 * Уведомляет рендерер о выгрузке секции чанка.
	 *
	 * @param sectionPos упакованная позиция секции
	 */
	public void onChunkUnload(long sectionPos) {
		worldRenderer.onChunkUnload(sectionPos);
	}

	/**
	 * Полностью сбрасывает кэш цветов биомов для всего мира.
	 */
	public void reloadColor() {
		colorCache.forEach((resolver, cache) -> cache.reset());
	}

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkZ) {
		return true;
	}

	public int getRegularEntityCount() {
		return entityManager.getEntityCount();
	}

	/**
	 * Добавляет сущность в мир, предварительно удаляя старую с тем же ID.
	 *
	 * @param entity добавляемая сущность
	 */
	public void addEntity(Entity entity) {
		removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
		entityManager.addEntity(entity);
	}

	/**
	 * Помечает сущность как удалённую по указанной причине.
	 *
	 * @param entityId      числовой ID сущности
	 * @param removalReason причина удаления
	 */
	public void removeEntity(int entityId, Entity.RemovalReason removalReason) {
		Entity entity = this.getEntityLookup().get(entityId);
		if (entity != null) {
			entity.setRemoved(removalReason);
			entity.onRemoved();
		}
	}

	@Override
	public List<Entity> getCrammedEntities(Entity entity, Box box) {
		ClientPlayerEntity clientPlayerEntity = this.client.player;
		return clientPlayerEntity != null
				       && clientPlayerEntity != entity
				       && clientPlayerEntity.getBoundingBox().intersects(box)
				       && EntityPredicates.canBePushedBy(entity).test(clientPlayerEntity)
		       ? List.of(clientPlayerEntity)
		       : List.of();
	}

	@Override
	public @Nullable Entity getEntityById(int id) {
		return this.getEntityLookup().get(id);
	}

	/**
	 * Disconnect.
	 *
	 * @param reasonText reason text
	 */
	public void disconnect(Text reasonText) {
		this.networkHandler.getConnection().disconnect(reasonText);
	}

	/**
	 * Do random block display ticks.
	 *
	 * @param centerX center x
	 * @param centerY center y
	 * @param centerZ center z
	 */
	public void doRandomBlockDisplayTicks(int centerX, int centerY, int centerZ) {
		Random tickRandom = Random.create();
		Block markerBlock = getBlockParticle();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int pass = 0; pass < 667; pass++) {
			randomBlockDisplayTick(centerX, centerY, centerZ, 16, tickRandom, markerBlock, mutable);
			randomBlockDisplayTick(centerX, centerY, centerZ, 32, tickRandom, markerBlock, mutable);
		}
	}

	private @Nullable Block getBlockParticle() {
		if (client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
			ItemStack itemStack = client.player.getMainHandStack();
			Item item = itemStack.getItem();
			if (BLOCK_MARKER_ITEMS.contains(item) && item instanceof BlockItem blockItem) {
				return blockItem.getBlock();
			}
		}

		return null;
	}

	public void randomBlockDisplayTick(
			int centerX,
			int centerY,
			int centerZ,
			int radius,
			Random random,
			@Nullable Block block,
			BlockPos.Mutable pos
	) {
		int x = centerX + this.random.nextInt(radius) - this.random.nextInt(radius);
		int y = centerY + this.random.nextInt(radius) - this.random.nextInt(radius);
		int z = centerZ + this.random.nextInt(radius) - this.random.nextInt(radius);
		pos.set(x, y, z);
		BlockState blockState = getBlockState(pos);
		blockState.getBlock().randomDisplayTick(blockState, this, pos, random);
		FluidState fluidState = getFluidState(pos);
		if (!fluidState.isEmpty()) {
			fluidState.randomDisplayTick(this, pos, random);
			ParticleEffect particleEffect = fluidState.getParticle();
			if (particleEffect != null && this.random.nextInt(AMBIENT_TICK_INTERVAL) == 0) {
				boolean solidBelow = blockState.isSideSolidFullSquare(this, pos, Direction.DOWN);
				BlockPos belowPos = pos.down();
				addParticle(belowPos, getBlockState(belowPos), particleEffect, solidBelow);
			}
		}

		if (block == blockState.getBlock()) {
			addParticleClient(
					new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, blockState),
					x + 0.5,
					y + 0.5,
					z + 0.5,
					0.0,
					0.0,
					0.0
			);
		}

		if (!blockState.isFullCube(this, pos)) {
			for (AmbientParticle ambientParticle : getEnvironmentAttributes()
					.getAttributeValue(EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL, pos)) {
				if (ambientParticle.shouldAddParticle(this.random)) {
					addParticleClient(
							ambientParticle.particle(),
							pos.getX() + this.random.nextDouble(),
							pos.getY() + this.random.nextDouble(),
							pos.getZ() + this.random.nextDouble(),
							0.0,
							0.0,
							0.0
					);
				}
			}
		}
	}

	private void addParticle(BlockPos pos, BlockState state, ParticleEffect parameters, boolean solidBelow) {
		if (!state.getFluidState().isEmpty()) {
			return;
		}

		VoxelShape shape = state.getCollisionShape(this, pos);
		double topY = shape.getMax(Direction.Axis.Y);
		if (topY < 1.0) {
			if (solidBelow) {
				addParticle(
						pos.getX(),
						pos.getX() + 1,
						pos.getZ(),
						pos.getZ() + 1,
						pos.getY() + 1 - PARTICLE_Y_OFFSET,
						parameters
				);
			}
		}
		else if (!state.isIn(BlockTags.IMPERMEABLE)) {
			double minY = shape.getMin(Direction.Axis.Y);
			if (minY > 0.0) {
				addParticle(pos, parameters, shape, pos.getY() + minY - PARTICLE_Y_OFFSET);
			}
			else {
				BlockPos belowPos = pos.down();
				BlockState belowState = getBlockState(belowPos);
				VoxelShape belowShape = belowState.getCollisionShape(this, belowPos);
				double belowTopY = belowShape.getMax(Direction.Axis.Y);
				if (belowTopY < 1.0 && belowState.getFluidState().isEmpty()) {
					addParticle(pos, parameters, shape, pos.getY() - PARTICLE_Y_OFFSET);
				}
			}
		}
	}

	private void addParticle(BlockPos pos, ParticleEffect parameters, VoxelShape shape, double y) {
		addParticle(
				pos.getX() + shape.getMin(Direction.Axis.X),
				pos.getX() + shape.getMax(Direction.Axis.X),
				pos.getZ() + shape.getMin(Direction.Axis.Z),
				pos.getZ() + shape.getMax(Direction.Axis.Z),
				y,
				parameters
		);
	}

	private void addParticle(double minX, double maxX, double minZ, double maxZ, double y, ParticleEffect parameters) {
		addParticleClient(
				parameters,
				MathHelper.lerp(random.nextDouble(), minX, maxX),
				y,
				MathHelper.lerp(random.nextDouble(), minZ, maxZ),
				0.0,
				0.0,
				0.0
		);
	}

	@Override
	public CrashReportSection addDetailsToCrashReport(CrashReport report) {
		CrashReportSection crashReportSection = super.addDetailsToCrashReport(report);
		crashReportSection.add("Server brand", () -> this.client.player.networkHandler.getBrand());
		crashReportSection.add(
				"Server type",
				() -> this.client.getServer() == null ? "Non-integrated multiplayer server"
				                                      : "Integrated singleplayer server"
		);
		crashReportSection.add("Tracked entity count", () -> String.valueOf(this.getRegularEntityCount()));
		return crashReportSection;
	}

	@Override
	public void playSound(
			@Nullable Entity source,
			double x,
			double y,
			double z,
			RegistryEntry<SoundEvent> sound,
			SoundCategory category,
			float volume,
			float pitch,
			long seed
	) {
		if (source == client.player) {
			playSound(x, y, z, sound.value(), category, volume, pitch, false, seed);
		}
	}

	@Override
	public void playSoundFromEntity(
			@Nullable Entity source,
			Entity entity,
			RegistryEntry<SoundEvent> sound,
			SoundCategory category,
			float volume,
			float pitch,
			long seed
	) {
		if (source == client.player) {
			client.getSoundManager()
					.play(new EntityTrackingSoundInstance(sound.value(), category, volume, pitch, entity, seed));
		}
	}

	@Override
	public void playSoundFromEntityClient(
			Entity entity,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch
	) {
		client.getSoundManager()
				.play(new EntityTrackingSoundInstance(sound, category, volume, pitch, entity, random.nextLong()));
	}

	@Override
	public void playSoundClient(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		if (client.player == null) {
			return;
		}

		client.getSoundManager()
				.play(new EntityTrackingSoundInstance(sound, category, volume, pitch, client.player, random.nextLong()));
	}

	@Override
	public void playSoundClient(
			double x,
			double y,
			double z,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			boolean useDistance
	) {
		playSound(x, y, z, sound, category, volume, pitch, useDistance, random.nextLong());
	}

	private void playSound(
			double x,
			double y,
			double z,
			SoundEvent event,
			SoundCategory category,
			float volume,
			float pitch,
			boolean useDistance,
			long seed
	) {
		double distanceSq = client.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(x, y, z);
		PositionedSoundInstance soundInstance = new PositionedSoundInstance(
				event, category, volume, pitch, Random.create(seed), x, y, z
		);
		if (useDistance && distanceSq > DISTANCE_SOUND_THRESHOLD_SQ) {
			double delay = Math.sqrt(distanceSq) / 40.0;
			client.getSoundManager().play(soundInstance, (int) (delay * 20.0));
		}
		else {
			client.getSoundManager().play(soundInstance);
		}
	}

	@Override
	public void addFireworkParticle(
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			List<FireworkExplosionComponent> explosions
	) {
		if (explosions.isEmpty()) {
			for (int count = 0; count < random.nextInt(3) + 2; count++) {
				addParticleClient(
						ParticleTypes.POOF,
						x,
						y,
						z,
						random.nextGaussian() * PARTICLE_Y_OFFSET,
						0.005,
						random.nextGaussian() * PARTICLE_Y_OFFSET
				);
			}
		}
		else {
			client.particleManager
					.addParticle(new FireworksSparkParticle.FireworkParticle(
							this,
							x,
							y,
							z,
							velocityX,
							velocityY,
							velocityZ,
							client.particleManager,
							explosions
					));
		}
	}

	@Override
	public void sendPacket(Packet<?> packet) {
		networkHandler.sendPacket(packet);
	}

	@Override
	public WorldBorder getWorldBorder() {
		return worldBorder;
	}

	@Override
	public RecipeManager getRecipeManager() {
		return networkHandler.getRecipeManager();
	}

	@Override
	public TickManager getTickManager() {
		return tickManager;
	}

	@Override
	public WorldEnvironmentAttributeAccess getEnvironmentAttributes() {
		return environmentAttributeAccess;
	}

	@Override
	public QueryableTickScheduler<Block> getBlockTickScheduler() {
		return EmptyTickSchedulers.getClientTickScheduler();
	}

	@Override
	public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
		return EmptyTickSchedulers.getClientTickScheduler();
	}

	public ClientChunkManager getChunkManager() {
		return chunkManager;
	}

	@Override
	public @Nullable MapState getMapState(MapIdComponent id) {
		return mapStates.get(id);
	}

	public void putClientsideMapState(MapIdComponent id, MapState state) {
		mapStates.put(id, state);
	}

	@Override
	public Scoreboard getScoreboard() {
		return networkHandler.getScoreboard();
	}

	@Override
	public void updateListeners(
			BlockPos pos,
			BlockState oldState,
			BlockState newState,
			@Block.SetBlockStateFlag int flags
	) {
		worldRenderer.updateBlock(this, pos, oldState, newState, flags);
	}

	@Override
	public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
		worldRenderer.scheduleBlockRerenderIfNeeded(pos, old, updated);
	}

	/**
	 * Планирует перерендер 3×3×3 чанков вокруг указанной позиции блока.
	 *
	 * @param x координата X блока
	 * @param y координата Y блока
	 * @param z координата Z блока
	 */
	public void scheduleBlockRenders(int x, int y, int z) {
		worldRenderer.scheduleChunkRenders3x3x3(x, y, z);
	}

	/**
	 * Планирует перерендер всех чанков в указанном диапазоне координат.
	 *
	 * @param minX минимальная X-координата
	 * @param minY минимальная Y-координата
	 * @param minZ минимальная Z-координата
	 * @param maxX максимальная X-координата
	 * @param maxY максимальная Y-координата
	 * @param maxZ максимальная Z-координата
	 */
	public void scheduleChunkRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		worldRenderer.scheduleChunkRenders(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
		worldRenderer.setBlockBreakingInfo(entityId, pos, progress);
	}

	@Override
	public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
		worldEventHandler.processGlobalEvent(eventId, pos, data);
	}

	@Override
	public void syncWorldEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {
		try {
			worldEventHandler.processWorldEvent(eventId, pos, data);
		}
		catch (Throwable ex) {
			CrashReport crashReport = CrashReport.create(ex, "Playing level event");
			CrashReportSection crashReportSection = crashReport.addElement("Level event being played");
			crashReportSection.add("Block coordinates", CrashReportSection.createPositionString(this, pos));
			crashReportSection.add("Event source", source);
			crashReportSection.add("Event type", eventId);
			crashReportSection.add("Event data", data);
			throw new CrashException(crashReport);
		}
	}

	@Override
	public void addParticleClient(
			ParticleEffect parameters,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		addParticle(
				parameters,
				parameters.getType().shouldAlwaysSpawn(),
				false,
				x,
				y,
				z,
				velocityX,
				velocityY,
				velocityZ
		);
	}

	@Override
	public void addParticleClient(
			ParticleEffect parameters,
			boolean force,
			boolean canSpawnOnMinimal,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		addParticle(
				parameters,
				parameters.getType().shouldAlwaysSpawn() || force,
				canSpawnOnMinimal,
				x,
				y,
				z,
				velocityX,
				velocityY,
				velocityZ
		);
	}

	@Override
	public void addImportantParticleClient(
			ParticleEffect parameters,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		addParticle(parameters, false, true, x, y, z, velocityX, velocityY, velocityZ);
	}

	@Override
	public void addImportantParticleClient(
			ParticleEffect parameters,
			boolean force,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		addParticle(
				parameters,
				parameters.getType().shouldAlwaysSpawn() || force,
				true,
				x,
				y,
				z,
				velocityX,
				velocityY,
				velocityZ
		);
	}

	private void addParticle(
			ParticleEffect particleEffect,
			boolean alwaysSpawn,
			boolean canSpawnOnMinimal,
			double x,
			double y,
			double z,
			double velX,
			double velY,
			double velZ
	) {
		try {
			Camera camera = client.gameRenderer.getCamera();
			ParticlesMode mode = getParticlesMode(canSpawnOnMinimal);
			if (alwaysSpawn) {
				client.particleManager.addParticle(particleEffect, x, y, z, velX, velY, velZ);
			}
			else if (camera.getCameraPos().squaredDistanceTo(x, y, z) <= 1024.0) {
				if (mode != ParticlesMode.MINIMAL) {
					client.particleManager.addParticle(particleEffect, x, y, z, velX, velY, velZ);
				}
			}
		}
		catch (Throwable ex) {
			CrashReport crashReport = CrashReport.create(ex, "Exception while adding particle");
			CrashReportSection crashReportSection = crashReport.addElement("Particle being added");
			crashReportSection.add("ID", Registries.PARTICLE_TYPE.getId(particleEffect.getType()));
			crashReportSection.add(
					"Parameters",
					() -> ParticleTypes.TYPE_CODEC
							.encodeStart(getRegistryManager().getOps(NbtOps.INSTANCE), particleEffect)
							.toString()
			);
			crashReportSection.add("Position", () -> CrashReportSection.createPositionString(this, x, y, z));
			throw new CrashException(crashReport);
		}
	}

	private ParticlesMode getParticlesMode(boolean canSpawnOnMinimal) {
		ParticlesMode mode = client.options.getParticles().getValue();
		if (canSpawnOnMinimal && mode == ParticlesMode.MINIMAL && random.nextInt(AMBIENT_TICK_INTERVAL) == 0) {
			mode = ParticlesMode.DECREASED;
		}

		if (mode == ParticlesMode.DECREASED && random.nextInt(3) == 0) {
			mode = ParticlesMode.MINIMAL;
		}

		return mode;
	}

	@Override
	public List<AbstractClientPlayerEntity> getPlayers() {
		return players;
	}

	public List<EnderDragonPart> getEnderDragonParts() {
		return enderDragonParts;
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
		return getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
	}

	private int getLightningTicksLeft() {
		return client.options.getHideLightningFlashes().getValue() ? 0 : lightningTicksLeft;
	}

	@Override
	public void setLightningTicksLeft(int lightningTicksLeft) {
		this.lightningTicksLeft = lightningTicksLeft;
	}

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		DimensionType.CardinalLightType cardinalLightType = getDimension().cardinalLightType();
		if (!shaded) {
			return cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
		}
		else {
			return switch (direction) {
				case DOWN -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 0.5F;
				case UP -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
				case NORTH, SOUTH -> 0.8F;
				case WEST, EAST -> 0.6F;
			};
		}
	}

	@Override
	public int getColor(BlockPos pos, ColorResolver colorResolver) {
		BiomeColorCache biomeColorCache = (BiomeColorCache) colorCache.get(colorResolver);
		return biomeColorCache.getBiomeColor(pos);
	}

	/**
	 * Вычисляет усреднённый цвет биома в радиусе смешивания вокруг позиции.
	 * При радиусе 0 возвращает точный цвет биома без усреднения.
	 *
	 * @param pos           позиция блока
	 * @param colorResolver резолвер цвета биома
	 * @return упакованный RGB-цвет
	 */
	public int calculateColor(BlockPos pos, ColorResolver colorResolver) {
		int blendRadius = MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue();
		if (blendRadius == 0) {
			return colorResolver.getColor(getBiome(pos).value(), pos.getX(), pos.getZ());
		}

		int sampleCount = (blendRadius * 2 + 1) * (blendRadius * 2 + 1);
		int red = 0;
		int green = 0;
		int blue = 0;
		CuboidBlockIterator iterator = new CuboidBlockIterator(
				pos.getX() - blendRadius,
				pos.getY(),
				pos.getZ() - blendRadius,
				pos.getX() + blendRadius,
				pos.getY(),
				pos.getZ() + blendRadius
		);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		while (iterator.step()) {
			mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());
			int color = colorResolver.getColor(getBiome(mutable).value(), mutable.getX(), mutable.getZ());
			red += (color & 0xFF0000) >> 16;
			green += (color & 0xFF00) >> 8;
			blue += color & 0xFF;
		}

		return (red / sampleCount & 0xFF) << 16 | (green / sampleCount & 0xFF) << 8 | blue / sampleCount & 0xFF;
	}

	@Override
	public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
		properties.setSpawnPoint(ensureWithinBorder(spawnPoint));
	}

	@Override
	public WorldProperties.SpawnPoint getSpawnPoint() {
		return properties.getSpawnPoint();
	}

	@Override
	public String toString() {
		return "ClientLevel";
	}

	public ClientWorld.Properties getLevelProperties() {
		return clientWorldProperties;
	}

	@Override
	public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {
	}

	public Map<MapIdComponent, MapState> getMapStates() {
		return ImmutableMap.copyOf(mapStates);
	}

	/**
	 * Добавляет все состояния карт из переданного словаря в локальный кэш.
	 *
	 * @param mapStates словарь состояний карт для добавления
	 */
	public void putMapStates(Map<MapIdComponent, MapState> mapStates) {
		this.mapStates.putAll(mapStates);
	}

	@Override
	protected EntityLookup<Entity> getEntityLookup() {
		return entityManager.getLookup();
	}

	@Override
	public String asString() {
		return "Chunks[C] W: " + chunkManager.getDebugString() + " E: " + entityManager.getDebugString();
	}

	@Override
	public void addBlockBreakParticles(BlockPos pos, BlockState state) {
		if (state.isAir() || !state.hasBlockBreakParticles()) {
			return;
		}

		VoxelShape voxelShape = state.getOutlineShape(this, pos);
		voxelShape.forEachBox(
				(minX, minY, minZ, maxX, maxY, maxZ) -> {
					double sizeX = Math.min(1.0, maxX - minX);
					double sizeY = Math.min(1.0, maxY - minY);
					double sizeZ = Math.min(1.0, maxZ - minZ);
					int countX = Math.max(2, MathHelper.ceil(sizeX / BREAK_PARTICLE_STEP));
					int countY = Math.max(2, MathHelper.ceil(sizeY / BREAK_PARTICLE_STEP));
					int countZ = Math.max(2, MathHelper.ceil(sizeZ / BREAK_PARTICLE_STEP));

					for (int xi = 0; xi < countX; xi++) {
						for (int yi = 0; yi < countY; yi++) {
							for (int zi = 0; zi < countZ; zi++) {
								double fracX = (xi + 0.5) / countX;
								double fracY = (yi + 0.5) / countY;
								double fracZ = (zi + 0.5) / countZ;
								double relX = fracX * sizeX + minX;
								double relY = fracY * sizeY + minY;
								double relZ = fracZ * sizeZ + minZ;
								client.particleManager
										.addParticle(new BlockDustParticle(
												this,
												pos.getX() + relX,
												pos.getY() + relY,
												pos.getZ() + relZ,
												fracX - 0.5,
												fracY - 0.5,
												fracZ - 0.5,
												state,
												pos
										));
							}
						}
					}
				}
		);
	}

	/**
	 * Создаёт частицу разрушения блока на грани, указанной направлением.
	 *
	 * @param pos       позиция блока
	 * @param direction грань блока, с которой вылетает частица
	 */
	public void spawnBlockBreakingParticle(BlockPos pos, Direction direction) {
		BlockState blockState = getBlockState(pos);
		if (blockState.getRenderType() == BlockRenderType.INVISIBLE || !blockState.hasBlockBreakParticles()) {
			return;
		}

		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();
		Box box = blockState.getOutlineShape(this, pos).getBoundingBox();
		double particleX = posX + random.nextDouble() * (box.maxX - box.minX - 2 * PARTICLE_OFFSET) + PARTICLE_OFFSET + box.minX;
		double particleY = posY + random.nextDouble() * (box.maxY - box.minY - 2 * PARTICLE_OFFSET) + PARTICLE_OFFSET + box.minY;
		double particleZ = posZ + random.nextDouble() * (box.maxZ - box.minZ - 2 * PARTICLE_OFFSET) + PARTICLE_OFFSET + box.minZ;

		if (direction == Direction.DOWN) {
			particleY = posY + box.minY - PARTICLE_OFFSET;
		}
		else if (direction == Direction.UP) {
			particleY = posY + box.maxY + PARTICLE_OFFSET;
		}
		else if (direction == Direction.NORTH) {
			particleZ = posZ + box.minZ - PARTICLE_OFFSET;
		}
		else if (direction == Direction.SOUTH) {
			particleZ = posZ + box.maxZ + PARTICLE_OFFSET;
		}
		else if (direction == Direction.WEST) {
			particleX = posX + box.minX - PARTICLE_OFFSET;
		}
		else if (direction == Direction.EAST) {
			particleX = posX + box.maxX + PARTICLE_OFFSET;
		}

		client.particleManager
				.addParticle(new BlockDustParticle(this, particleX, particleY, particleZ, 0.0, 0.0, 0.0, blockState, pos)
						.move(0.2F)
						.scale(0.6F));
	}

	public void setSimulationDistance(int simulationDistance) {
		this.simulationDistance = simulationDistance;
	}

	public int getSimulationDistance() {
		return simulationDistance;
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return networkHandler.getEnabledFeatures();
	}

	@Override
	public BrewingRecipeRegistry getBrewingRecipeRegistry() {
		return networkHandler.getBrewingRecipeRegistry();
	}

	@Override
	public FuelRegistry getFuelRegistry() {
		return networkHandler.getFuelRegistry();
	}

	@Override
	public void createExplosion(
			@Nullable Entity entity,
			@Nullable DamageSource damageSource,
			@Nullable ExplosionBehavior behavior,
			double x,
			double y,
			double z,
			float power,
			boolean createFire,
			World.ExplosionSourceType explosionSourceType,
			ParticleEffect smallParticle,
			ParticleEffect largeParticle,
			Pool<BlockParticleEffect> blockParticles,
			RegistryEntry<SoundEvent> soundEvent
	) {
	}

	@Override
	public int getSeaLevel() {
		return this.seaLevel;
	}

	@Override
	public int getBlockColor(BlockPos pos) {
		return MinecraftClient.getInstance().getBlockColors().getColor(this.getBlockState(pos), this, pos, 0);
	}

	@Override
	public void registerForCleaning(DataCache<ClientWorld, ?> dataCache) {
		this.networkHandler.registerForCleaning(dataCache);
	}

	public void addBlockParticleEffects(
			Vec3d center,
			float radius,
			int blockCount,
			Pool<BlockParticleEffect> particles
	) {
		blockParticlesManager.scheduleBlockParticles(center, radius, blockCount, particles);
	}

	/**
	 * Внутренний обработчик событий жизненного цикла сущностей клиентского мира.
	 * Синхронизирует списки игроков и частей Эндер-дракона при добавлении/удалении сущностей.
	 */
	@Environment(EnvType.CLIENT)
	final class ClientEntityHandler implements EntityHandler<Entity> {

		public void create(Entity entity) {
		}

		public void destroy(Entity entity) {
		}

		public void startTicking(Entity entity) {
			ClientWorld.this.entityList.add(entity);
		}

		public void stopTicking(Entity entity) {
			ClientWorld.this.entityList.remove(entity);
		}

		public void startTracking(Entity entity) {
			switch (entity) {
				case AbstractClientPlayerEntity player -> ClientWorld.this.players.add(player);
				case EnderDragonEntity dragon -> ClientWorld.this.enderDragonParts.addAll(
						Arrays.asList(dragon.getBodyParts())
				);
				default -> {
				}
			}
		}

		public void stopTracking(Entity entity) {
			entity.detach();
			switch (entity) {
				case AbstractClientPlayerEntity player -> ClientWorld.this.players.remove(player);
				case EnderDragonEntity dragon -> ClientWorld.this.enderDragonParts.removeAll(
						Arrays.asList(dragon.getBodyParts())
				);
				default -> {
				}
			}
		}

		public void updateLoadStatus(Entity entity) {
		}
	}

	/**
	 * Клиентская реализация свойств мира, синхронизируемых с сервером.
	 * Хранит сложность, режим хардкора, время и погоду.
	 */
	@Environment(EnvType.CLIENT)
	public static class Properties implements MutableWorldProperties {

		private final boolean hardcore;
		private final boolean flatWorld;
		private WorldProperties.SpawnPoint position;
		private long time;
		private long timeOfDay;
		private boolean raining;
		private Difficulty difficulty;
		private boolean difficultyLocked;

		public Properties(Difficulty difficulty, boolean hardcore, boolean flatWorld) {
			this.difficulty = difficulty;
			this.hardcore = hardcore;
			this.flatWorld = flatWorld;
		}

		@Override
		public WorldProperties.SpawnPoint getSpawnPoint() {
			return position;
		}

		@Override
		public long getTime() {
			return time;
		}

		@Override
		public long getTimeOfDay() {
			return timeOfDay;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public void setTimeOfDay(long timeOfDay) {
			this.timeOfDay = timeOfDay;
		}

		@Override
		public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
			this.position = spawnPoint;
		}

		@Override
		public boolean isThundering() {
			return false;
		}

		@Override
		public boolean isRaining() {
			return raining;
		}

		@Override
		public void setRaining(boolean raining) {
			this.raining = raining;
		}

		@Override
		public boolean isHardcore() {
			return hardcore;
		}

		@Override
		public Difficulty getDifficulty() {
			return difficulty;
		}

		@Override
		public boolean isDifficultyLocked() {
			return difficultyLocked;
		}

		@Override
		public void populateCrashReport(CrashReportSection reportSection, HeightLimitView world) {
			MutableWorldProperties.super.populateCrashReport(reportSection, world);
		}

		public void setDifficulty(Difficulty difficulty) {
			this.difficulty = difficulty;
		}

		public void setDifficultyLocked(boolean difficultyLocked) {
			this.difficultyLocked = difficultyLocked;
		}

		public double getSkyDarknessHeight(HeightLimitView world) {
			return flatWorld ? world.getBottomY() : 63.0;
		}

		public float getVoidDarknessRange() {
			return flatWorld ? 1.0F : 32.0F;
		}
	}
}

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

@Environment(EnvType.CLIENT)
/**
 * {@code ClientWorld}.
 */
public class ClientWorld extends World implements DataCache.CacheContext<ClientWorld> {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Text QUITTING_MULTIPLAYER_TEXT = Text.translatable("multiplayer.status.quitting");
	private static final double PARTICLE_Y_OFFSET = 0.05;
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

	public void handlePlayerActionResponse(int sequence) {
		if (SharedConstants.BLOCK_BREAK) {
			LOGGER.debug("ACK {}", sequence);
		}

		this.pendingUpdateManager.processPendingUpdates(sequence, this);
	}

	@Override
	public void loadBlockEntity(BlockEntity blockEntity) {
		BlockEntityRenderer<BlockEntity, ?>
				blockEntityRenderer =
				this.client.getBlockEntityRenderDispatcher().get(blockEntity);
		if (blockEntityRenderer != null && blockEntityRenderer.rendersOutsideBoundingBox()) {
			this.blockEntities.add(blockEntity);
		}
	}

	public Set<BlockEntity> getBlockEntities() {
		return this.blockEntities;
	}

	public void handleBlockUpdate(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		if (!this.pendingUpdateManager.hasPendingUpdate(pos, state)) {
			super.setBlockState(pos, state, flags, 512);
		}
	}

	public void processPendingUpdate(BlockPos pos, BlockState state, Vec3d playerPos) {
		BlockState blockState = this.getBlockState(pos);
		if (blockState != state) {
			this.setBlockState(pos, state, 19);
			PlayerEntity playerEntity = this.client.player;
			if (this == playerEntity.getEntityWorld() && playerEntity.collidesWithStateAtPos(pos, state)) {
				playerEntity.updatePosition(playerPos.x, playerPos.y, playerPos.z);
			}
		}
	}

	public PendingUpdateManager getPendingUpdateManager() {
		return this.pendingUpdateManager;
	}

	@Override
	public boolean setBlockState(
			BlockPos pos,
			BlockState state,
			@Block.SetBlockStateFlag int flags,
			int maxUpdateDepth
	) {
		if (this.pendingUpdateManager.hasPendingSequence()) {
			BlockState blockState = this.getBlockState(pos);
			boolean bl = super.setBlockState(pos, state, flags, maxUpdateDepth);
			if (bl) {
				this.pendingUpdateManager.addPendingUpdate(pos, blockState, this.client.player);
			}

			return bl;
		}
		else {
			return super.setBlockState(pos, state, flags, maxUpdateDepth);
		}
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
		this.worldEventHandler = new WorldEventHandler(this.client, this);
		this.endLightFlashManager = dimensionType.value().getSkybox() ? new EndLightFlashManager() : null;
		this.setSpawnPoint(WorldProperties.SpawnPoint.create(registryRef, new BlockPos(8, 64, 8), 0.0F, 0.0F));
		this.simulationDistance = simulationDistance;
		this.environmentAttributeAccess =
				this.addClientSideAttributes(WorldEnvironmentAttributeAccess.builder()).build();
		this.calculateAmbientDarkness();
		if (this.canHaveWeather()) {
			this.initWeatherGradients();
		}
	}

	private WorldEnvironmentAttributeAccess.Builder addClientSideAttributes(WorldEnvironmentAttributeAccess.Builder builder) {
		builder.world(this);
		int i = ColorHelper.getArgb(204, 204, 255);
		builder.timeBased(
				EnvironmentAttributes.SKY_COLOR_VISUAL,
				(color, time) -> this.getLightningTicksLeft() > 0 ? ColorHelper.lerp(0.22F, color, i) : color
		);
		builder.timeBased(
				EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL,
				(factor, time) -> this.getLightningTicksLeft() > 0 ? 1.0F : factor
		);
		return builder;
	}

	public void enqueueChunkUpdate(Runnable updater) {
		this.chunkUpdaters.add(updater);
	}

	public void runQueuedChunkUpdates() {
		int i = this.chunkUpdaters.size();
		int j = i < 1000 ? Math.max(10, i / 10) : i;

		for (int k = 0; k < j; k++) {
			Runnable runnable = this.chunkUpdaters.poll();
			if (runnable == null) {
				break;
			}

			runnable.run();
		}
	}

	public @Nullable EndLightFlashManager getEndLightFlashManager() {
		return this.endLightFlashManager;
	}

	public void tick(BooleanSupplier shouldKeepTicking) {
		this.calculateAmbientDarkness();
		if (this.getTickManager().shouldTick()) {
			this.getWorldBorder().tick();
			this.tickTime();
		}

		if (this.lightningTicksLeft > 0) {
			this.setLightningTicksLeft(this.lightningTicksLeft - 1);
		}

		if (this.endLightFlashManager != null) {
			this.endLightFlashManager.tick(this.getTime());
			if (this.endLightFlashManager.shouldFlash() && !(this.client.currentScreen instanceof CreditsScreen)) {
				this.client
						.getSoundManager()
						.play(
								new EndLightFlashSoundInstance(
										SoundEvents.WEATHER_END_FLASH,
										SoundCategory.WEATHER,
										this.random,
										this.client.gameRenderer.getCamera(),
										this.endLightFlashManager.getPitch(),
										this.endLightFlashManager.getYaw()
								),
								30
						);
			}
		}

		this.blockParticlesManager.tick(this);

		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("blocks")) {
			this.chunkManager.tick(shouldKeepTicking, true);
		}

		FlightProfiler.INSTANCE.onClientFps(this.client.getCurrentFps());
		this.getEnvironmentAttributes().tick();
	}

	private void tickTime() {
		this.clientWorldProperties.setTime(this.clientWorldProperties.getTime() + 1L);
		if (this.shouldTickTimeOfDay) {
			this.clientWorldProperties.setTimeOfDay(this.clientWorldProperties.getTimeOfDay() + 1L);
		}
	}

	public void setTime(long time, long timeOfDay, boolean shouldTickTimeOfDay) {
		this.clientWorldProperties.setTime(time);
		this.clientWorldProperties.setTimeOfDay(timeOfDay);
		this.shouldTickTimeOfDay = shouldTickTimeOfDay;
	}

	public Iterable<Entity> getEntities() {
		return this.getEntityLookup().iterate();
	}

	public void tickEntities() {
		this.entityList.forEach(entity -> {
			if (!entity.isRemoved() && !entity.hasVehicle() && !this.tickManager.shouldSkipTick(entity)) {
				this.tickEntity(this::tickEntity, entity);
			}
		});
	}

	public boolean hasEntity(Entity entity) {
		return this.entityList.has(entity);
	}

	@Override
	public boolean shouldUpdatePostDeath(Entity entity) {
		return entity.getChunkPos().getChebyshevDistance(this.client.player.getChunkPos()) <= this.simulationDistance;
	}

	public void tickEntity(Entity entity) {
		entity.resetPosition();
		entity.age++;
		Profilers.get().push(() -> Registries.ENTITY_TYPE.getId(entity.getType()).toString());
		entity.tick();
		Profilers.get().pop();

		for (Entity entity2 : entity.getPassengerList()) {
			this.tickPassenger(entity, entity2);
		}
	}

	private void tickPassenger(Entity entity, Entity passenger) {
		if (passenger.isRemoved() || passenger.getVehicle() != entity) {
			passenger.stopRiding();
		}
		else if (passenger instanceof PlayerEntity || this.entityList.has(passenger)) {
			passenger.resetPosition();
			passenger.age++;
			passenger.tickRiding();

			for (Entity entity2 : passenger.getPassengerList()) {
				this.tickPassenger(passenger, entity2);
			}
		}
	}

	public void unloadBlockEntities(WorldChunk chunk) {
		chunk.clear();
		this.chunkManager.getLightingProvider().setColumnEnabled(chunk.getPos(), false);
		this.entityManager.stopTicking(chunk.getPos());
	}

	public void resetChunkColor(ChunkPos chunkPos) {
		this.colorCache.forEach((resolver, cache) -> cache.reset(chunkPos.x, chunkPos.z));
		this.entityManager.startTicking(chunkPos);
	}

	public void onChunkUnload(long sectionPos) {
		this.worldRenderer.onChunkUnload(sectionPos);
	}

	public void reloadColor() {
		this.colorCache.forEach((resolver, cache) -> cache.reset());
	}

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkZ) {
		return true;
	}

	public int getRegularEntityCount() {
		return this.entityManager.getEntityCount();
	}

	public void addEntity(Entity entity) {
		this.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
		this.entityManager.addEntity(entity);
	}

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

	public void disconnect(Text reasonText) {
		this.networkHandler.getConnection().disconnect(reasonText);
	}

	public void doRandomBlockDisplayTicks(int centerX, int centerY, int centerZ) {
		int i = 32;
		Random random = Random.create();
		Block block = this.getBlockParticle();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int j = 0; j < 667; j++) {
			this.randomBlockDisplayTick(centerX, centerY, centerZ, 16, random, block, mutable);
			this.randomBlockDisplayTick(centerX, centerY, centerZ, 32, random, block, mutable);
		}
	}

	private @Nullable Block getBlockParticle() {
		if (this.client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
			ItemStack itemStack = this.client.player.getMainHandStack();
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
		int i = centerX + this.random.nextInt(radius) - this.random.nextInt(radius);
		int j = centerY + this.random.nextInt(radius) - this.random.nextInt(radius);
		int k = centerZ + this.random.nextInt(radius) - this.random.nextInt(radius);
		pos.set(i, j, k);
		BlockState blockState = this.getBlockState(pos);
		blockState.getBlock().randomDisplayTick(blockState, this, pos, random);
		FluidState fluidState = this.getFluidState(pos);
		if (!fluidState.isEmpty()) {
			fluidState.randomDisplayTick(this, pos, random);
			ParticleEffect particleEffect = fluidState.getParticle();
			if (particleEffect != null && this.random.nextInt(10) == 0) {
				boolean bl = blockState.isSideSolidFullSquare(this, pos, Direction.DOWN);
				BlockPos blockPos = pos.down();
				this.addParticle(blockPos, this.getBlockState(blockPos), particleEffect, bl);
			}
		}

		if (block == blockState.getBlock()) {
			this.addParticleClient(
					new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, blockState),
					i + 0.5,
					j + 0.5,
					k + 0.5,
					0.0,
					0.0,
					0.0
			);
		}

		if (!blockState.isFullCube(this, pos)) {
			for (AmbientParticle ambientParticle : this
					.getEnvironmentAttributes()
					.getAttributeValue(EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL, pos)) {
				if (ambientParticle.shouldAddParticle(this.random)) {
					this.addParticleClient(
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
		if (state.getFluidState().isEmpty()) {
			VoxelShape voxelShape = state.getCollisionShape(this, pos);
			double d = voxelShape.getMax(Direction.Axis.Y);
			if (d < 1.0) {
				if (solidBelow) {
					this.addParticle(
							pos.getX(),
							pos.getX() + 1,
							pos.getZ(),
							pos.getZ() + 1,
							pos.getY() + 1 - 0.05,
							parameters
					);
				}
			}
			else if (!state.isIn(BlockTags.IMPERMEABLE)) {
				double e = voxelShape.getMin(Direction.Axis.Y);
				if (e > 0.0) {
					this.addParticle(pos, parameters, voxelShape, pos.getY() + e - 0.05);
				}
				else {
					BlockPos blockPos = pos.down();
					BlockState blockState = this.getBlockState(blockPos);
					VoxelShape voxelShape2 = blockState.getCollisionShape(this, blockPos);
					double f = voxelShape2.getMax(Direction.Axis.Y);
					if (f < 1.0 && blockState.getFluidState().isEmpty()) {
						this.addParticle(pos, parameters, voxelShape, pos.getY() - 0.05);
					}
				}
			}
		}
	}

	private void addParticle(BlockPos pos, ParticleEffect parameters, VoxelShape shape, double y) {
		this.addParticle(
				pos.getX() + shape.getMin(Direction.Axis.X),
				pos.getX() + shape.getMax(Direction.Axis.X),
				pos.getZ() + shape.getMin(Direction.Axis.Z),
				pos.getZ() + shape.getMax(Direction.Axis.Z),
				y,
				parameters
		);
	}

	private void addParticle(double minX, double maxX, double minZ, double maxZ, double y, ParticleEffect parameters) {
		this.addParticleClient(
				parameters,
				MathHelper.lerp(this.random.nextDouble(), minX, maxX),
				y,
				MathHelper.lerp(this.random.nextDouble(), minZ, maxZ),
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
		if (source == this.client.player) {
			this.playSound(x, y, z, sound.value(), category, volume, pitch, false, seed);
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
		if (source == this.client.player) {
			this.client
					.getSoundManager()
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
		this.client
				.getSoundManager()
				.play(new EntityTrackingSoundInstance(sound, category, volume, pitch, entity, this.random.nextLong()));
	}

	@Override
	public void playSoundClient(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		if (this.client.player != null) {
			this.client
					.getSoundManager()
					.play(new EntityTrackingSoundInstance(
							sound,
							category,
							volume,
							pitch,
							this.client.player,
							this.random.nextLong()
					));
		}
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
		this.playSound(x, y, z, sound, category, volume, pitch, useDistance, this.random.nextLong());
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
		double d = this.client.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(x, y, z);
		PositionedSoundInstance
				positionedSoundInstance =
				new PositionedSoundInstance(event, category, volume, pitch, Random.create(seed), x, y, z);
		if (useDistance && d > 100.0) {
			double e = Math.sqrt(d) / 40.0;
			this.client.getSoundManager().play(positionedSoundInstance, (int) (e * 20.0));
		}
		else {
			this.client.getSoundManager().play(positionedSoundInstance);
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
			for (int i = 0; i < this.random.nextInt(3) + 2; i++) {
				this.addParticleClient(
						ParticleTypes.POOF,
						x,
						y,
						z,
						this.random.nextGaussian() * 0.05,
						0.005,
						this.random.nextGaussian() * 0.05
				);
			}
		}
		else {
			this.client
					.particleManager
					.addParticle(new FireworksSparkParticle.FireworkParticle(
							this,
							x,
							y,
							z,
							velocityX,
							velocityY,
							velocityZ,
							this.client.particleManager,
							explosions
					));
		}
	}

	@Override
	public void sendPacket(Packet<?> packet) {
		this.networkHandler.sendPacket(packet);
	}

	@Override
	public WorldBorder getWorldBorder() {
		return this.worldBorder;
	}

	@Override
	public RecipeManager getRecipeManager() {
		return this.networkHandler.getRecipeManager();
	}

	@Override
	public TickManager getTickManager() {
		return this.tickManager;
	}

	@Override
	public WorldEnvironmentAttributeAccess getEnvironmentAttributes() {
		return this.environmentAttributeAccess;
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
		return this.chunkManager;
	}

	@Override
	public @Nullable MapState getMapState(MapIdComponent id) {
		return this.mapStates.get(id);
	}

	public void putClientsideMapState(MapIdComponent id, MapState state) {
		this.mapStates.put(id, state);
	}

	@Override
	public Scoreboard getScoreboard() {
		return this.networkHandler.getScoreboard();
	}

	@Override
	public void updateListeners(
			BlockPos pos,
			BlockState oldState,
			BlockState newState,
			@Block.SetBlockStateFlag int flags
	) {
		this.worldRenderer.updateBlock(this, pos, oldState, newState, flags);
	}

	@Override
	public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
		this.worldRenderer.scheduleBlockRerenderIfNeeded(pos, old, updated);
	}

	public void scheduleBlockRenders(int x, int y, int z) {
		this.worldRenderer.scheduleChunkRenders3x3x3(x, y, z);
	}

	public void scheduleChunkRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.worldRenderer.scheduleChunkRenders(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
		this.worldRenderer.setBlockBreakingInfo(entityId, pos, progress);
	}

	@Override
	public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
		this.worldEventHandler.processGlobalEvent(eventId, pos, data);
	}

	@Override
	public void syncWorldEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {
		try {
			this.worldEventHandler.processWorldEvent(eventId, pos, data);
		}
		catch (Throwable var8) {
			CrashReport crashReport = CrashReport.create(var8, "Playing level event");
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
		this.addParticle(
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
		this.addParticle(
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
		this.addParticle(parameters, false, true, x, y, z, velocityX, velocityY, velocityZ);
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
		this.addParticle(
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
			boolean bl,
			boolean bl2,
			double d,
			double e,
			double f,
			double g,
			double h,
			double i
	) {
		try {
			Camera camera = this.client.gameRenderer.getCamera();
			ParticlesMode particlesMode = this.getParticlesMode(bl2);
			if (bl) {
				this.client.particleManager.addParticle(particleEffect, d, e, f, g, h, i);
			}
			else if (!(camera.getCameraPos().squaredDistanceTo(d, e, f) > 1024.0)) {
				if (particlesMode != ParticlesMode.MINIMAL) {
					this.client.particleManager.addParticle(particleEffect, d, e, f, g, h, i);
				}
			}
		}
		catch (Throwable var19) {
			CrashReport crashReport = CrashReport.create(var19, "Exception while adding particle");
			CrashReportSection crashReportSection = crashReport.addElement("Particle being added");
			crashReportSection.add("ID", Registries.PARTICLE_TYPE.getId(particleEffect.getType()));
			crashReportSection.add(
					"Parameters",
					() -> ParticleTypes.TYPE_CODEC
							.encodeStart(this.getRegistryManager().getOps(NbtOps.INSTANCE), particleEffect)
							.toString()
			);
			crashReportSection.add("Position", () -> CrashReportSection.createPositionString(this, d, e, f));
			throw new CrashException(crashReport);
		}
	}

	private ParticlesMode getParticlesMode(boolean bl) {
		ParticlesMode particlesMode = this.client.options.getParticles().getValue();
		if (bl && particlesMode == ParticlesMode.MINIMAL && this.random.nextInt(10) == 0) {
			particlesMode = ParticlesMode.DECREASED;
		}

		if (particlesMode == ParticlesMode.DECREASED && this.random.nextInt(3) == 0) {
			particlesMode = ParticlesMode.MINIMAL;
		}

		return particlesMode;
	}

	@Override
	public List<AbstractClientPlayerEntity> getPlayers() {
		return this.players;
	}

	public List<EnderDragonPart> getEnderDragonParts() {
		return this.enderDragonParts;
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
		return this.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
	}

	private int getLightningTicksLeft() {
		return this.client.options.getHideLightningFlashes().getValue() ? 0 : this.lightningTicksLeft;
	}

	@Override
	public void setLightningTicksLeft(int lightningTicksLeft) {
		this.lightningTicksLeft = lightningTicksLeft;
	}

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		DimensionType.CardinalLightType cardinalLightType = this.getDimension().cardinalLightType();
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
		BiomeColorCache biomeColorCache = (BiomeColorCache) this.colorCache.get(colorResolver);
		return biomeColorCache.getBiomeColor(pos);
	}

	public int calculateColor(BlockPos pos, ColorResolver colorResolver) {
		int i = MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue();
		if (i == 0) {
			return colorResolver.getColor(this.getBiome(pos).value(), pos.getX(), pos.getZ());
		}
		else {
			int j = (i * 2 + 1) * (i * 2 + 1);
			int k = 0;
			int l = 0;
			int m = 0;
			CuboidBlockIterator cuboidBlockIterator = new CuboidBlockIterator(
					pos.getX() - i, pos.getY(), pos.getZ() - i, pos.getX() + i, pos.getY(), pos.getZ() + i
			);
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			while (cuboidBlockIterator.step()) {
				mutable.set(cuboidBlockIterator.getX(), cuboidBlockIterator.getY(), cuboidBlockIterator.getZ());
				int n = colorResolver.getColor(this.getBiome(mutable).value(), mutable.getX(), mutable.getZ());
				k += (n & 0xFF0000) >> 16;
				l += (n & 0xFF00) >> 8;
				m += n & 0xFF;
			}

			return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | m / j & 0xFF;
		}
	}

	@Override
	public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
		this.properties.setSpawnPoint(this.ensureWithinBorder(spawnPoint));
	}

	@Override
	public WorldProperties.SpawnPoint getSpawnPoint() {
		return this.properties.getSpawnPoint();
	}

	@Override
	public String toString() {
		return "ClientLevel";
	}

	public ClientWorld.Properties getLevelProperties() {
		return this.clientWorldProperties;
	}

	@Override
	public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {
	}

	public Map<MapIdComponent, MapState> getMapStates() {
		return ImmutableMap.copyOf(this.mapStates);
	}

	public void putMapStates(Map<MapIdComponent, MapState> mapStates) {
		this.mapStates.putAll(mapStates);
	}

	@Override
	protected EntityLookup<Entity> getEntityLookup() {
		return this.entityManager.getLookup();
	}

	@Override
	public String asString() {
		return "Chunks[C] W: " + this.chunkManager.getDebugString() + " E: " + this.entityManager.getDebugString();
	}

	@Override
	public void addBlockBreakParticles(BlockPos pos, BlockState state) {
		if (!state.isAir() && state.hasBlockBreakParticles()) {
			VoxelShape voxelShape = state.getOutlineShape(this, pos);
			double d = 0.25;
			voxelShape.forEachBox(
					(minX, minY, minZ, maxX, maxY, maxZ) -> {
						double dx = Math.min(1.0, maxX - minX);
						double e = Math.min(1.0, maxY - minY);
						double f = Math.min(1.0, maxZ - minZ);
						int i = Math.max(2, MathHelper.ceil(dx / 0.25));
						int j = Math.max(2, MathHelper.ceil(e / 0.25));
						int k = Math.max(2, MathHelper.ceil(f / 0.25));

						for (int l = 0; l < i; l++) {
							for (int m = 0; m < j; m++) {
								for (int n = 0; n < k; n++) {
									double g = (l + 0.5) / i;
									double h = (m + 0.5) / j;
									double o = (n + 0.5) / k;
									double p = g * dx + minX;
									double q = h * e + minY;
									double r = o * f + minZ;
									this.client
											.particleManager
											.addParticle(new BlockDustParticle(
													this,
													pos.getX() + p,
													pos.getY() + q,
													pos.getZ() + r,
													g - 0.5,
													h - 0.5,
													o - 0.5,
													state,
													pos
											));
								}
							}
						}
					}
			);
		}
	}

	public void spawnBlockBreakingParticle(BlockPos pos, Direction direction) {
		BlockState blockState = this.getBlockState(pos);
		if (blockState.getRenderType() != BlockRenderType.INVISIBLE && blockState.hasBlockBreakParticles()) {
			int i = pos.getX();
			int j = pos.getY();
			int k = pos.getZ();
			float f = 0.1F;
			Box box = blockState.getOutlineShape(this, pos).getBoundingBox();
			double d = i + this.random.nextDouble() * (box.maxX - box.minX - 0.2F) + 0.1F + box.minX;
			double e = j + this.random.nextDouble() * (box.maxY - box.minY - 0.2F) + 0.1F + box.minY;
			double g = k + this.random.nextDouble() * (box.maxZ - box.minZ - 0.2F) + 0.1F + box.minZ;
			if (direction == Direction.DOWN) {
				e = j + box.minY - 0.1F;
			}

			if (direction == Direction.UP) {
				e = j + box.maxY + 0.1F;
			}

			if (direction == Direction.NORTH) {
				g = k + box.minZ - 0.1F;
			}

			if (direction == Direction.SOUTH) {
				g = k + box.maxZ + 0.1F;
			}

			if (direction == Direction.WEST) {
				d = i + box.minX - 0.1F;
			}

			if (direction == Direction.EAST) {
				d = i + box.maxX + 0.1F;
			}

			this.client.particleManager.addParticle(new BlockDustParticle(this, d, e, g, 0.0, 0.0, 0.0, blockState, pos)
					.move(0.2F)
					.scale(0.6F));
		}
	}

	public void setSimulationDistance(int simulationDistance) {
		this.simulationDistance = simulationDistance;
	}

	public int getSimulationDistance() {
		return this.simulationDistance;
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return this.networkHandler.getEnabledFeatures();
	}

	@Override
	public BrewingRecipeRegistry getBrewingRecipeRegistry() {
		return this.networkHandler.getBrewingRecipeRegistry();
	}

	@Override
	public FuelRegistry getFuelRegistry() {
		return this.networkHandler.getFuelRegistry();
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
		this.blockParticlesManager.scheduleBlockParticles(center, radius, blockCount, particles);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ClientEntityHandler}.
	 */
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
				case AbstractClientPlayerEntity abstractClientPlayerEntity:
					ClientWorld.this.players.add(abstractClientPlayerEntity);
					break;
				case EnderDragonEntity enderDragonEntity:
					ClientWorld.this.enderDragonParts.addAll(Arrays.asList(enderDragonEntity.getBodyParts()));
					break;
				default:
			}
		}

		public void stopTracking(Entity entity) {
			entity.detach();
			switch (entity) {
				case AbstractClientPlayerEntity abstractClientPlayerEntity:
					ClientWorld.this.players.remove(abstractClientPlayerEntity);
					break;
				case EnderDragonEntity enderDragonEntity:
					ClientWorld.this.enderDragonParts.removeAll(Arrays.asList(enderDragonEntity.getBodyParts()));
					break;
				default:
			}
		}

		public void updateLoadStatus(Entity entity) {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Properties}.
	 */
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
			return this.position;
		}

		@Override
		public long getTime() {
			return this.time;
		}

		@Override
		public long getTimeOfDay() {
			return this.timeOfDay;
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
			return this.raining;
		}

		@Override
		public void setRaining(boolean raining) {
			this.raining = raining;
		}

		@Override
		public boolean isHardcore() {
			return this.hardcore;
		}

		@Override
		public Difficulty getDifficulty() {
			return this.difficulty;
		}

		@Override
		public boolean isDifficultyLocked() {
			return this.difficultyLocked;
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
			return this.flatWorld ? world.getBottomY() : 63.0;
		}

		public float getVoidDarknessRange() {
			return this.flatWorld ? 1.0F : 32.0F;
		}
	}
}

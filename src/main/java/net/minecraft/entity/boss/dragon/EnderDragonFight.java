package net.minecraft.entity.boss.dragon;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.predicate.block.BlockPredicate;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.EndConfiguredFeatures;
import net.minecraft.world.gen.feature.EndPortalFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Управляет боссовым сражением с Драконом Края: отслеживает состояние дракона,
 * кристаллов, портала выхода, шлюзов и управляет возрождением дракона через
 * конечный автомат {@link EnderDragonSpawnState}.
 */
public class EnderDragonFight {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int CHECK_DRAGON_SEEN_INTERVAL = 1200;
	private static final int CRYSTAL_COUNTING_INTERVAL = 100;
	public static final int PLAYER_UPDATE_INTERVAL = 20;
	private static final int ISLAND_SIZE = 8;
	public static final int DRAGON_CHUNK_TICKET_LEVEL = 9;
	private static final int GATEWAY_PLACEMENT_RADIUS = 96;
	private static final int GATEWAY_COUNT = 20;
	public static final int SPAWN_Y = 128;
	private static final int GATEWAY_Y = 75;
	private static final int BEDROCK_SEARCH_DEPTH = 63;
	private static final int PORTAL_CHUNK_LIGHTING_RADIUS = 4;
	private static final int PORTAL_CHUNK_LIGHTING_CHUNK_SIZE = 16;
	private static final double BOSS_BAR_SHOW_RADIUS = 192.0;
	private static final int PORTAL_PATTERN_CENTER = 3;

	private final Predicate<Entity> showBossBarPredicate;
	private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(
			Text.translatable("entity.minecraft.ender_dragon"), BossBar.Color.PINK, BossBar.Style.PROGRESS
	)
			.setDragonMusic(true)
			.setThickenFog(true);
	private final ServerWorld world;
	private final BlockPos origin;
	private final ObjectArrayList<Integer> gateways = new ObjectArrayList<>();
	private final BlockPattern endPortalPattern;
	private int dragonSeenTimer;
	private int endCrystalsAlive;
	private int crystalCountTimer;
	private int playerUpdateTimer = 21;
	private boolean dragonKilled;
	private boolean previouslyKilled;
	private boolean skipChunksLoadedCheck = false;
	private @Nullable UUID dragonUuid;
	private boolean doLegacyCheck = true;
	private @Nullable BlockPos exitPortalLocation;
	private @Nullable EnderDragonSpawnState dragonSpawnState;
	private int spawnStateTimer;
	private @Nullable List<EndCrystalEntity> crystals;

	public EnderDragonFight(ServerWorld world, long gatewaysSeed, EnderDragonFight.Data data) {
		this(world, gatewaysSeed, data, BlockPos.ORIGIN);
	}

	public EnderDragonFight(ServerWorld world, long gatewaysSeed, EnderDragonFight.Data data, BlockPos origin) {
		this.world = world;
		this.origin = origin;
		showBossBarPredicate = EntityPredicates.VALID_ENTITY.and(
				EntityPredicates.maxDistance(origin.getX(), SPAWN_Y + origin.getY(), origin.getZ(), BOSS_BAR_SHOW_RADIUS)
		);
		doLegacyCheck = data.needsStateScanning;
		dragonUuid = data.dragonUUID.orElse(null);
		dragonKilled = data.dragonKilled;
		previouslyKilled = data.previouslyKilled;

		if (data.isRespawning) {
			dragonSpawnState = EnderDragonSpawnState.START;
		}

		exitPortalLocation = data.exitPortalLocation.orElse(null);
		gateways.addAll(data.gateways.orElseGet(() -> {
			ObjectArrayList<Integer> list = new ObjectArrayList<>(
					ContiguousSet.create(Range.closedOpen(0, GATEWAY_COUNT), DiscreteDomain.integers())
			);
			Util.shuffle(list, Random.create(gatewaysSeed));
			return list;
		}));
		endPortalPattern = BlockPatternBuilder.start()
				.aisle(
						"       ",
						"       ",
						"       ",
						"   #   ",
						"       ",
						"       ",
						"       "
				)
				.aisle(
						"       ",
						"       ",
						"       ",
						"   #   ",
						"       ",
						"       ",
						"       "
				)
				.aisle(
						"       ",
						"       ",
						"       ",
						"   #   ",
						"       ",
						"       ",
						"       "
				)
				.aisle(
						"  ###  ",
						" #   # ",
						"#     #",
						"#  #  #",
						"#     #",
						" #   # ",
						"  ###  "
				)
				.aisle(
						"       ",
						"  ###  ",
						" ##### ",
						" ##### ",
						" ##### ",
						"  ###  ",
						"       "
				)
				.where('#', CachedBlockPosition.matchesBlockState(BlockPredicate.make(Blocks.BEDROCK)))
				.build();
	}

	@Deprecated
	@VisibleForTesting
	public void setSkipChunksLoadedCheck() {
		skipChunksLoadedCheck = true;
	}

	public EnderDragonFight.Data toData() {
		return new EnderDragonFight.Data(
				doLegacyCheck,
				dragonKilled,
				previouslyKilled,
				false,
				Optional.ofNullable(dragonUuid),
				Optional.ofNullable(exitPortalLocation),
				Optional.of(gateways)
		);
	}

	/**
	 * Главный тик боссового сражения. Обновляет список игроков в радиусе видимости,
	 * управляет тикетом чанков, запускает состояние возрождения и проверяет наличие дракона.
	 */
	public void tick() {
		bossBar.setVisible(!dragonKilled);

		if (++playerUpdateTimer >= PLAYER_UPDATE_INTERVAL) {
			updatePlayers();
			playerUpdateTimer = 0;
		}

		if (bossBar.getPlayers().isEmpty()) {
			world.getChunkManager().removeTicket(ChunkTicketType.DRAGON, new ChunkPos(0, 0), DRAGON_CHUNK_TICKET_LEVEL);
			return;
		}

		world.getChunkManager().addTicket(ChunkTicketType.DRAGON, new ChunkPos(0, 0), DRAGON_CHUNK_TICKET_LEVEL);
		boolean chunksLoaded = areChunksLoaded();

		if (doLegacyCheck && chunksLoaded) {
			convertFromLegacy();
			doLegacyCheck = false;
		}

		if (dragonSpawnState != null) {
			if (crystals == null && chunksLoaded) {
				dragonSpawnState = null;
				respawnDragon();
			}

			dragonSpawnState.run(world, this, crystals, spawnStateTimer++, exitPortalLocation);
		}

		if (dragonKilled) {
			return;
		}

		if ((dragonUuid == null || ++dragonSeenTimer >= CHECK_DRAGON_SEEN_INTERVAL) && chunksLoaded) {
			checkDragonSeen();
			dragonSeenTimer = 0;
		}

		if (++crystalCountTimer >= CRYSTAL_COUNTING_INTERVAL && chunksLoaded) {
			countAliveCrystals();
			crystalCountTimer = 0;
		}
	}

	private void convertFromLegacy() {
		LOGGER.info("Scanning for legacy world dragon fight...");
		boolean portalExists = worldContainsEndPortal();

		if (portalExists) {
			LOGGER.info("Found that the dragon has been killed in this world already.");
			previouslyKilled = true;
		} else {
			LOGGER.info("Found that the dragon has not yet been killed in this world.");
			previouslyKilled = false;
			if (findEndPortal() == null) {
				generateEndPortal(false);
			}
		}

		List<? extends EnderDragonEntity> dragons = world.getAliveEnderDragons();

		if (dragons.isEmpty()) {
			dragonKilled = true;
		} else {
			EnderDragonEntity dragon = dragons.get(0);
			dragonUuid = dragon.getUuid();
			LOGGER.info("Found that there's a dragon still alive ({})", dragon);
			dragonKilled = false;

			if (!portalExists) {
				LOGGER.info("But we didn't have a portal, let's remove it.");
				dragon.discard();
				dragonUuid = null;
			}
		}

		if (!previouslyKilled && dragonKilled) {
			dragonKilled = false;
		}
	}

	private void checkDragonSeen() {
		List<? extends EnderDragonEntity> dragons = world.getAliveEnderDragons();

		if (dragons.isEmpty()) {
			LOGGER.debug("Haven't seen the dragon, respawning it");
			createDragon();
		} else {
			LOGGER.debug("Haven't seen our dragon, but found another one to use.");
			dragonUuid = dragons.get(0).getUuid();
		}
	}

	protected void setSpawnState(EnderDragonSpawnState spawnState) {
		if (dragonSpawnState == null) {
			throw new IllegalStateException("Dragon respawn isn't in progress, can't skip ahead in the animation.");
		}

		spawnStateTimer = 0;

		if (spawnState == EnderDragonSpawnState.END) {
			dragonSpawnState = null;
			dragonKilled = false;
			EnderDragonEntity dragon = createDragon();

			if (dragon != null) {
				for (ServerPlayerEntity player : bossBar.getPlayers()) {
					Criteria.SUMMONED_ENTITY.trigger(player, dragon);
				}
			}
		} else {
			dragonSpawnState = spawnState;
		}
	}

	private boolean worldContainsEndPortal() {
		for (int chunkX = -ISLAND_SIZE; chunkX <= ISLAND_SIZE; chunkX++) {
			for (int chunkZ = -ISLAND_SIZE; chunkZ <= ISLAND_SIZE; chunkZ++) {
				WorldChunk chunk = world.getChunk(chunkX, chunkZ);

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof EndPortalBlockEntity) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private BlockPattern.@Nullable Result findEndPortal() {
		ChunkPos chunkPos = new ChunkPos(origin);

		for (int chunkX = -ISLAND_SIZE + chunkPos.x; chunkX <= ISLAND_SIZE + chunkPos.x; chunkX++) {
			for (int chunkZ = -ISLAND_SIZE + chunkPos.z; chunkZ <= ISLAND_SIZE + chunkPos.z; chunkZ++) {
				WorldChunk chunk = world.getChunk(chunkX, chunkZ);

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (!(blockEntity instanceof EndPortalBlockEntity)) {
						continue;
					}

					BlockPattern.Result result = endPortalPattern.searchAround(world, blockEntity.getPos());
					if (result == null) {
						continue;
					}

					BlockPos portalCenter = result.translate(PORTAL_PATTERN_CENTER, PORTAL_PATTERN_CENTER, PORTAL_PATTERN_CENTER).getBlockPos();
					if (exitPortalLocation == null) {
						exitPortalLocation = portalCenter;
					}

					return result;
				}
			}
		}

		BlockPos searchOrigin = EndPortalFeature.offsetOrigin(origin);
		int topY = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, searchOrigin).getY();

		for (int scanY = topY; scanY >= world.getBottomY(); scanY--) {
			BlockPattern.Result result = endPortalPattern.searchAround(
					world, new BlockPos(searchOrigin.getX(), scanY, searchOrigin.getZ())
			);

			if (result == null) {
				continue;
			}

			if (exitPortalLocation == null) {
				exitPortalLocation = result.translate(PORTAL_PATTERN_CENTER, PORTAL_PATTERN_CENTER, PORTAL_PATTERN_CENTER).getBlockPos();
			}

			return result;
		}

		return null;
	}

	private boolean areChunksLoaded() {
		if (skipChunksLoadedCheck) {
			return true;
		}

		ChunkPos chunkPos = new ChunkPos(origin);

		// Баг в оригинале: внутренний цикл использовал j = 8 + chunkPos.z для обоих bounds.
		// Исправлено: нижняя граница -ISLAND_SIZE + chunkPos.z, верхняя ISLAND_SIZE + chunkPos.z.
		for (int chunkX = -ISLAND_SIZE + chunkPos.x; chunkX <= ISLAND_SIZE + chunkPos.x; chunkX++) {
			for (int chunkZ = -ISLAND_SIZE + chunkPos.z; chunkZ <= ISLAND_SIZE + chunkPos.z; chunkZ++) {
				Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

				if (!(chunk instanceof WorldChunk worldChunk)) {
					return false;
				}

				if (!worldChunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING)) {
					return false;
				}
			}
		}

		return true;
	}

	private void updatePlayers() {
		Set<ServerPlayerEntity> nearbyPlayers = Sets.newHashSet(world.getPlayers(showBossBarPredicate));

		for (ServerPlayerEntity player : nearbyPlayers) {
			bossBar.addPlayer(player);
		}

		Set<ServerPlayerEntity> toRemove = Sets.newHashSet(bossBar.getPlayers());
		toRemove.removeAll(nearbyPlayers);

		for (ServerPlayerEntity player : toRemove) {
			bossBar.removePlayer(player);
		}
	}

	private void countAliveCrystals() {
		crystalCountTimer = 0;
		endCrystalsAlive = 0;

		for (EndSpikeFeature.Spike spike : EndSpikeFeature.getSpikes(world)) {
			endCrystalsAlive += world.getNonSpectatingEntities(EndCrystalEntity.class, spike.getBoundingBox()).size();
		}

		LOGGER.debug("Found {} end crystals still alive", endCrystalsAlive);
	}

	/**
	 * Вызывается при гибели дракона. Генерирует портал выхода, новый шлюз,
	 * при первом убийстве размещает яйцо дракона.
	 */
	public void dragonKilled(EnderDragonEntity dragon) {
		if (!dragon.getUuid().equals(dragonUuid)) {
			return;
		}

		bossBar.setPercent(0.0F);
		bossBar.setVisible(false);
		generateEndPortal(true);
		generateNewEndGateway();

		if (!previouslyKilled) {
			world.setBlockState(
					world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, EndPortalFeature.offsetOrigin(origin)),
					Blocks.DRAGON_EGG.getDefaultState()
			);
		}

		previouslyKilled = true;
		dragonKilled = true;
	}

	@Deprecated
	@VisibleForTesting
	public void clearGatewaysList() {
		gateways.clear();
	}

	private void generateNewEndGateway() {
		if (gateways.isEmpty()) {
			return;
		}

		int gatewayIdx = gateways.remove(gateways.size() - 1);
		int gatewayX = MathHelper.floor(GATEWAY_PLACEMENT_RADIUS * Math.cos(2.0 * (-Math.PI + (Math.PI / GATEWAY_COUNT) * gatewayIdx)));
		int gatewayZ = MathHelper.floor(GATEWAY_PLACEMENT_RADIUS * Math.sin(2.0 * (-Math.PI + (Math.PI / GATEWAY_COUNT) * gatewayIdx)));
		generateEndGateway(new BlockPos(gatewayX, GATEWAY_Y, gatewayZ));
	}

	private void generateEndGateway(BlockPos pos) {
		world.syncWorldEvent(3000, pos, 0);
		world.getRegistryManager()
				.getOptional(RegistryKeys.CONFIGURED_FEATURE)
				.flatMap(registry -> registry.getOptional(EndConfiguredFeatures.END_GATEWAY_DELAYED))
				.ifPresent(reference -> reference
						.value()
						.generate(world, world.getChunkManager().getChunkGenerator(), Random.create(), pos));
	}

	private void generateEndPortal(boolean previouslyKilled) {
		EndPortalFeature endPortalFeature = new EndPortalFeature(previouslyKilled);

		if (exitPortalLocation == null) {
			exitPortalLocation = world
					.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(origin))
					.down();

			while (world.getBlockState(exitPortalLocation).isOf(Blocks.BEDROCK)
					&& exitPortalLocation.getY() > BEDROCK_SEARCH_DEPTH) {
				exitPortalLocation = exitPortalLocation.down();
			}

			exitPortalLocation = exitPortalLocation.withY(
					Math.max(world.getBottomY() + 1, exitPortalLocation.getY())
			);
		}

		if (endPortalFeature.generateIfValid(
				FeatureConfig.DEFAULT,
				world,
				world.getChunkManager().getChunkGenerator(),
				Random.create(),
				exitPortalLocation
		)) {
			int lightingRadius = MathHelper.ceilDiv(PORTAL_CHUNK_LIGHTING_RADIUS, PORTAL_CHUNK_LIGHTING_CHUNK_SIZE);
			world.getChunkManager().chunkLoadingManager.forceLighting(new ChunkPos(exitPortalLocation), lightingRadius);
		}
	}

	private @Nullable EnderDragonEntity createDragon() {
		world.getWorldChunk(new BlockPos(origin.getX(), SPAWN_Y + origin.getY(), origin.getZ()));
		EnderDragonEntity dragon = EntityType.ENDER_DRAGON.create(world, SpawnReason.EVENT);

		if (dragon == null) {
			return null;
		}

		dragon.setFight(this);
		dragon.setFightOrigin(origin);
		dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
		dragon.refreshPositionAndAngles(
				origin.getX(),
				SPAWN_Y + origin.getY(),
				origin.getZ(),
				world.random.nextFloat() * 360.0F,
				0.0F
		);
		world.spawnEntity(dragon);
		dragonUuid = dragon.getUuid();
		return dragon;
	}

	/**
	 * Обновляет полосу здоровья босса и сбрасывает таймер обнаружения дракона.
	 */
	public void updateFight(EnderDragonEntity dragon) {
		if (!dragon.getUuid().equals(dragonUuid)) {
			return;
		}

		bossBar.setPercent(dragon.getHealth() / dragon.getMaxHealth());
		dragonSeenTimer = 0;

		if (dragon.hasCustomName()) {
			bossBar.setName(dragon.getDisplayName());
		}
	}

	public int getAliveEndCrystals() {
		return endCrystalsAlive;
	}

	public void crystalDestroyed(EndCrystalEntity enderCrystal, DamageSource source) {
		if (dragonSpawnState != null && crystals.contains(enderCrystal)) {
			LOGGER.debug("Aborting respawn sequence");
			dragonSpawnState = null;
			spawnStateTimer = 0;
			resetEndCrystals();
			generateEndPortal(true);
			return;
		}

		countAliveCrystals();

		if (world.getEntity(dragonUuid) instanceof EnderDragonEntity dragon) {
			dragon.crystalDestroyed(world, enderCrystal, enderCrystal.getBlockPos(), source);
		}
	}

	public boolean hasPreviouslyKilled() {
		return previouslyKilled;
	}

	/**
	 * Запускает процесс возрождения дракона. Ищет портал выхода, проверяет наличие
	 * четырёх кристаллов на пьедесталах вокруг портала и инициирует анимацию возрождения.
	 */
	public void respawnDragon() {
		if (!dragonKilled || dragonSpawnState != null) {
			return;
		}

		BlockPos portalPos = exitPortalLocation;

		if (portalPos == null) {
			LOGGER.debug("Tried to respawn, but need to find the portal first.");
			BlockPattern.Result result = findEndPortal();

			if (result == null) {
				LOGGER.debug("Couldn't find a portal, so we made one.");
				generateEndPortal(true);
			} else {
				LOGGER.debug("Found the exit portal & saved its location for next time.");
			}

			portalPos = exitPortalLocation;
		}

		List<EndCrystalEntity> foundCrystals = new ArrayList<>();
		BlockPos abovePortal = portalPos.up(1);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			List<EndCrystalEntity> crystalsInDir = world.getNonSpectatingEntities(
					EndCrystalEntity.class,
					new Box(abovePortal.offset(direction, 2))
			);

			if (crystalsInDir.isEmpty()) {
				return;
			}

			foundCrystals.addAll(crystalsInDir);
		}

		LOGGER.debug("Found all crystals, respawning dragon.");
		respawnDragon(foundCrystals);
	}

	private void respawnDragon(List<EndCrystalEntity> crystals) {
		if (!dragonKilled || dragonSpawnState != null) {
			return;
		}

		for (BlockPattern.Result result = findEndPortal(); result != null; result = findEndPortal()) {
			for (int px = 0; px < endPortalPattern.getWidth(); px++) {
				for (int py = 0; py < endPortalPattern.getHeight(); py++) {
					for (int pz = 0; pz < endPortalPattern.getDepth(); pz++) {
						CachedBlockPosition cached = result.translate(px, py, pz);

						if (cached.getBlockState().isOf(Blocks.BEDROCK)
								|| cached.getBlockState().isOf(Blocks.END_PORTAL)) {
							world.setBlockState(cached.getBlockPos(), Blocks.END_STONE.getDefaultState());
						}
					}
				}
			}
		}

		dragonSpawnState = EnderDragonSpawnState.START;
		spawnStateTimer = 0;
		generateEndPortal(false);
		this.crystals = crystals;
	}

	public void resetEndCrystals() {
		for (EndSpikeFeature.Spike spike : EndSpikeFeature.getSpikes(world)) {
			for (EndCrystalEntity crystal : world.getNonSpectatingEntities(EndCrystalEntity.class, spike.getBoundingBox())) {
				crystal.setInvulnerable(false);
				crystal.setBeamTarget(null);
			}
		}
	}

	public @Nullable UUID getDragonUuid() {
		return dragonUuid;
	}

	/**
	 * Сериализуемые данные боссового сражения, сохраняемые в NBT мира.
	 */
	public record Data(
			boolean needsStateScanning,
			boolean dragonKilled,
			boolean previouslyKilled,
			boolean isRespawning,
			Optional<UUID> dragonUUID,
			Optional<BlockPos> exitPortalLocation,
			Optional<List<Integer>> gateways
	) {

		public static final Codec<EnderDragonFight.Data> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.BOOL.fieldOf("NeedsStateScanning").orElse(true).forGetter(Data::needsStateScanning),
						Codec.BOOL.fieldOf("DragonKilled").orElse(false).forGetter(Data::dragonKilled),
						Codec.BOOL.fieldOf("PreviouslyKilled").orElse(false).forGetter(Data::previouslyKilled),
						Codec.BOOL.lenientOptionalFieldOf("IsRespawning", false).forGetter(Data::isRespawning),
						Uuids.INT_STREAM_CODEC.lenientOptionalFieldOf("Dragon").forGetter(Data::dragonUUID),
						BlockPos.CODEC.lenientOptionalFieldOf("ExitPortalLocation").forGetter(Data::exitPortalLocation),
						Codec.list(Codec.INT).lenientOptionalFieldOf("Gateways").forGetter(Data::gateways)
				).apply(instance, Data::new)
		);

		public static final EnderDragonFight.Data DEFAULT = new EnderDragonFight.Data(
				true, false, false, false, Optional.empty(), Optional.empty(), Optional.empty()
		);
	}
}

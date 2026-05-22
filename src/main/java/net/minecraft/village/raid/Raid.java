package net.minecraft.village.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Rarity;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Рейд — волновое событие нападения иллагеров на деревню.
 * <p>
 * Управляет спавном волн рейдеров, полосой здоровья, наградами победителям
 * и переходами между состояниями ({@link Status}). Центр рейда автоматически
 * перемещается к ближайшей занятой точке интереса, если деревня сдвинулась.
 */
public class Raid {

	public static final SpawnLocation RAVAGER_SPAWN_LOCATION = SpawnRestriction.getLocation(EntityType.RAVAGER);

	public static final MapCodec<Raid> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.BOOL.fieldOf("started").forGetter(raid -> raid.started),
					Codec.BOOL.fieldOf("active").forGetter(raid -> raid.active),
					Codec.LONG.fieldOf("ticks_active").forGetter(raid -> raid.ticksActive),
					Codec.INT.fieldOf("raid_omen_level").forGetter(raid -> raid.raidOmenLevel),
					Codec.INT.fieldOf("groups_spawned").forGetter(raid -> raid.wavesSpawned),
					Codec.INT.fieldOf("cooldown_ticks").forGetter(raid -> raid.preRaidTicks),
					Codec.INT.fieldOf("post_raid_ticks").forGetter(raid -> raid.postRaidTicks),
					Codec.FLOAT.fieldOf("total_health").forGetter(raid -> raid.totalHealth),
					Codec.INT.fieldOf("group_count").forGetter(raid -> raid.waveCount),
					Raid.Status.CODEC.fieldOf("status").forGetter(raid -> raid.status),
					BlockPos.CODEC.fieldOf("center").forGetter(raid -> raid.center),
					Uuids.SET_CODEC.fieldOf("heroes_of_the_village").forGetter(raid -> raid.heroesOfTheVillage)
			).apply(instance, Raid::new)
	);

	private static final int MAX_WAVE_COUNT_HARD = 7;
	private static final int RAIDERS_REMAINING_THRESHOLD = 2;
	private static final int SPAWN_RADIUS = 32;
	private static final int MAX_ACTIVE_TICKS = 48000;
	private static final int MAX_SPAWN_FAILURES = 5;
	private static final int POST_RAID_VICTORY_TICKS = 40;
	private static final int DEFAULT_PRE_RAID_TICKS = 300;
	private static final int OUT_OF_RAID_COUNTER_MAX = 30;
	private static final int MAX_SPAWN_HEIGHT_DIFF = 96;
	private static final int SPAWN_LOCATION_CHECK_RADIUS = 10;
	private static final int SPAWN_LOCATION_RECALC_INTERVAL = 5;
	private static final int BAR_UPDATE_INTERVAL = 20;
	private static final float HORN_SOUND_DISTANCE = 13.0F;
	private static final float HORN_SOUND_VOLUME = 64.0F;
	private static final float SPAWN_RADIUS_SCALE = 0.22F;
	private static final float SPAWN_RADIUS_OFFSET = 0.24F;

	public static final int MAX_RAID_OMEN_LEVEL = 16;
	public static final int MAX_DESPAWN_COUNTER = 2400;
	public static final int FINISH_COOLDOWN_MAX_TICKS = 600;
	public static final int MAX_BAD_OMEN_LEVEL = 5;
	public static final int SQUARED_RAID_CENTER_DISTANCE = 9216;
	public static final int SQUARED_MAX_RAIDER_DISTANCE = 12544;

	private static final Text EVENT_TEXT = Text.translatable("event.minecraft.raid");
	private static final Text VICTORY_TITLE = Text.translatable("event.minecraft.raid.victory.full");
	private static final Text DEFEAT_TITLE = Text.translatable("event.minecraft.raid.defeat.full");
	private static final Text OMINOUS_BANNER_TRANSLATION_KEY = Text.translatable("block.minecraft.ominous_banner");

	private final Map<Integer, RaiderEntity> waveToCaptain = Maps.newHashMap();
	private final Map<Integer, Set<RaiderEntity>> waveToRaiders = Maps.newHashMap();
	private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
	private final ServerBossBar bar = new ServerBossBar(EVENT_TEXT, BossBar.Color.RED, BossBar.Style.NOTCHED_10);
	private final Random random = Random.create();
	private final int waveCount;

	private long ticksActive;
	private BlockPos center;
	private boolean started;
	private float totalHealth;
	private int raidOmenLevel;
	private boolean active;
	private int wavesSpawned;
	private int postRaidTicks;
	private int preRaidTicks;
	private Raid.Status status;
	private int finishCooldown;
	private Optional<BlockPos> preCalculatedRaidersSpawnLocation = Optional.empty();

	public Raid(BlockPos center, Difficulty difficulty) {
		active = true;
		preRaidTicks = DEFAULT_PRE_RAID_TICKS;
		bar.setPercent(0.0F);
		this.center = center;
		waveCount = getMaxWaves(difficulty);
		status = Raid.Status.ONGOING;
	}

	private Raid(
			boolean started,
			boolean active,
			long ticksActive,
			int raidOmenLevel,
			int wavesSpawned,
			int preRaidTicks,
			int postRaidTicks,
			float totalHealth,
			int waveCount,
			Raid.Status status,
			BlockPos center,
			Set<UUID> heroesOfTheVillage
	) {
		this.started = started;
		this.active = active;
		this.ticksActive = ticksActive;
		this.raidOmenLevel = raidOmenLevel;
		this.wavesSpawned = wavesSpawned;
		this.preRaidTicks = preRaidTicks;
		this.postRaidTicks = postRaidTicks;
		this.totalHealth = totalHealth;
		this.center = center;
		this.waveCount = waveCount;
		this.status = status;
		this.heroesOfTheVillage.addAll(heroesOfTheVillage);
	}

	public boolean isFinished() {
		return hasWon() || hasLost();
	}

	public boolean isPreRaid() {
		return hasSpawned() && getRaiderCount() == 0 && preRaidTicks > 0;
	}

	public boolean hasSpawned() {
		return wavesSpawned > 0;
	}

	public boolean hasStopped() {
		return status == Raid.Status.STOPPED;
	}

	public boolean hasWon() {
		return status == Raid.Status.VICTORY;
	}

	public boolean hasLost() {
		return status == Raid.Status.LOSS;
	}

	public float getTotalHealth() {
		return totalHealth;
	}

	public Set<RaiderEntity> getAllRaiders() {
		Set<RaiderEntity> all = Sets.newHashSet();

		for (Set<RaiderEntity> wave : waveToRaiders.values()) {
			all.addAll(wave);
		}

		return all;
	}

	public boolean hasStarted() {
		return started;
	}

	public int getGroupsSpawned() {
		return wavesSpawned;
	}

	private Predicate<ServerPlayerEntity> isInRaidDistance() {
		return player -> {
			BlockPos playerPos = player.getBlockPos();
			return player.isAlive() && player.getEntityWorld().getRaidAt(playerPos) == this;
		};
	}

	private void updateBarToPlayers(ServerWorld world) {
		Set<ServerPlayerEntity> currentPlayers = Sets.newHashSet(bar.getPlayers());
		List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(isInRaidDistance());

		for (ServerPlayerEntity player : nearbyPlayers) {
			if (!currentPlayers.contains(player)) {
				bar.addPlayer(player);
			}
		}

		for (ServerPlayerEntity player : currentPlayers) {
			if (!nearbyPlayers.contains(player)) {
				bar.removePlayer(player);
			}
		}
	}

	public int getMaxAcceptableBadOmenLevel() {
		return MAX_BAD_OMEN_LEVEL;
	}

	public int getBadOmenLevel() {
		return raidOmenLevel;
	}

	public void setBadOmenLevel(int badOmenLevel) {
		raidOmenLevel = badOmenLevel;
	}

	/**
	 * Добавляет уровень «Предзнаменования рейда» от игрока к текущему рейду.
	 * Засчитывает статистику и достижение при первом запуске.
	 *
	 * @param player игрок с эффектом «Предзнаменование рейда»
	 * @return {@code true}, если рейд успешно запущен
	 */
	public boolean start(ServerPlayerEntity player) {
		StatusEffectInstance raidOmen = player.getStatusEffect(StatusEffects.RAID_OMEN);

		if (raidOmen == null) {
			return false;
		}

		raidOmenLevel = MathHelper.clamp(
				raidOmenLevel + raidOmen.getAmplifier() + 1,
				0,
				getMaxAcceptableBadOmenLevel()
		);

		if (!hasSpawned()) {
			player.incrementStat(Stats.RAID_TRIGGER);
			Criteria.VOLUNTARY_EXILE.trigger(player);
		}

		return true;
	}

	public void invalidate() {
		active = false;
		bar.clearPlayers();
		status = Raid.Status.STOPPED;
	}

	/**
	 * Основной тик рейда: управляет фазами подготовки, спавна волн и завершения.
	 * <p>
	 * В фазе подготовки ({@code preRaidTicks > 0}) предварительно вычисляет точку спавна.
	 * После истечения таймера спавнит следующую волну. При победе раздаёт эффект
	 * «Герой деревни» всем участникам.
	 *
	 * @param world серверный мир
	 */
	public void tick(ServerWorld world) {
		if (hasStopped()) {
			return;
		}

		if (status == Raid.Status.ONGOING) {
			tickOngoing(world);
		} else if (isFinished()) {
			tickFinished(world);
		}
	}

	private void tickOngoing(ServerWorld world) {
		boolean wasActive = active;
		active = world.isChunkLoaded(center);

		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			invalidate();
			return;
		}

		if (wasActive != active) {
			bar.setVisible(active);
		}

		if (!active) {
			return;
		}

		if (!world.isNearOccupiedPointOfInterest(center)) {
			moveRaidCenter(world);
		}

		if (!world.isNearOccupiedPointOfInterest(center)) {
			if (wavesSpawned > 0) {
				status = Raid.Status.LOSS;
			} else {
				invalidate();
			}
		}

		ticksActive++;

		if (ticksActive >= MAX_ACTIVE_TICKS) {
			invalidate();
			return;
		}

		int raiderCount = getRaiderCount();

		if (raiderCount == 0 && shouldSpawnMoreGroups()) {
			tickPreRaid(world);
		}

		if (ticksActive % BAR_UPDATE_INTERVAL == 0L) {
			updateBarToPlayers(world);
			removeObsoleteRaiders(world);
			updateBarName(raiderCount);
		}

		if (SharedConstants.RAIDS) {
			bar.setName(
					EVENT_TEXT.copy()
							.append(" wave: ").append(wavesSpawned + "")
							.append(ScreenTexts.SPACE)
							.append("Raiders alive: ").append(getRaiderCount() + "")
							.append(ScreenTexts.SPACE)
							.append(getCurrentRaiderHealth() + "")
							.append(" / ").append(totalHealth + "")
							.append(" Is bonus? ").append((hasExtraWave() && hasSpawnedExtraWave()) + "")
							.append(" Status: ").append(status.asString())
			);
		}

		trySpawnWaves(world);

		if (hasStarted() && !shouldSpawnMoreGroups() && raiderCount == 0) {
			tickVictory(world);
		}

		markDirty(world);
	}

	private void tickPreRaid(ServerWorld world) {
		if (preRaidTicks <= 0) {
			if (preRaidTicks == 0 && wavesSpawned > 0) {
				preRaidTicks = DEFAULT_PRE_RAID_TICKS;
				bar.setName(EVENT_TEXT);
			}

			return;
		}

		boolean hasSpawnLocation = preCalculatedRaidersSpawnLocation.isPresent();
		boolean needsRecalculation = !hasSpawnLocation && preRaidTicks % SPAWN_LOCATION_RECALC_INTERVAL == 0;

		if (hasSpawnLocation && !world.shouldTickEntityAt(preCalculatedRaidersSpawnLocation.get())) {
			needsRecalculation = true;
		}

		if (needsRecalculation) {
			preCalculatedRaidersSpawnLocation = getRaidersSpawnLocation(world);
		}

		if (preRaidTicks == DEFAULT_PRE_RAID_TICKS || preRaidTicks % BAR_UPDATE_INTERVAL == 0) {
			updateBarToPlayers(world);
		}

		preRaidTicks--;
		bar.setPercent(MathHelper.clamp((DEFAULT_PRE_RAID_TICKS - preRaidTicks) / (float) DEFAULT_PRE_RAID_TICKS, 0.0F, 1.0F));
	}

	private void updateBarName(int raiderCount) {
		if (raiderCount > 0 && raiderCount <= RAIDERS_REMAINING_THRESHOLD) {
			bar.setName(
					EVENT_TEXT.copy()
							.append(" - ")
							.append(Text.translatable("event.minecraft.raid.raiders_remaining", raiderCount))
			);
		} else {
			bar.setName(EVENT_TEXT);
		}
	}

	private void trySpawnWaves(ServerWorld world) {
		boolean hornPlayed = false;
		int spawnFailures = 0;

		while (canSpawnRaiders()) {
			BlockPos spawnPos = preCalculatedRaidersSpawnLocation.orElseGet(
					() -> findRandomRaidersSpawnLocation(world, 20)
			);

			if (spawnPos != null) {
				started = true;
				spawnNextWave(world, spawnPos);

				if (!hornPlayed) {
					playRaidHorn(world, spawnPos);
					hornPlayed = true;
				}
			} else {
				spawnFailures++;
			}

			if (spawnFailures > MAX_SPAWN_FAILURES) {
				invalidate();
				break;
			}
		}
	}

	private void tickVictory(ServerWorld world) {
		if (postRaidTicks < POST_RAID_VICTORY_TICKS) {
			postRaidTicks++;
			return;
		}

		status = Raid.Status.VICTORY;

		for (UUID heroUuid : heroesOfTheVillage) {
			Entity entity = world.getEntity(heroUuid);

			if (entity instanceof LivingEntity hero && !entity.isSpectator()) {
				hero.addStatusEffect(new StatusEffectInstance(
						StatusEffects.HERO_OF_THE_VILLAGE,
						MAX_ACTIVE_TICKS,
						raidOmenLevel - 1,
						false,
						false,
						true
				));

				if (hero instanceof ServerPlayerEntity serverPlayer) {
					serverPlayer.incrementStat(Stats.RAID_WIN);
					Criteria.HERO_OF_THE_VILLAGE.trigger(serverPlayer);
				}
			}
		}
	}

	private void tickFinished(ServerWorld world) {
		finishCooldown++;

		if (finishCooldown >= FINISH_COOLDOWN_MAX_TICKS) {
			invalidate();
			return;
		}

		if (finishCooldown % BAR_UPDATE_INTERVAL == 0) {
			updateBarToPlayers(world);
			bar.setVisible(true);

			if (hasWon()) {
				bar.setPercent(0.0F);
				bar.setName(VICTORY_TITLE);
			} else {
				bar.setName(DEFEAT_TITLE);
			}
		}
	}

	private void moveRaidCenter(ServerWorld world) {
		Stream<ChunkSectionPos> stream = ChunkSectionPos.stream(ChunkSectionPos.from(center), 2);
		stream.filter(world::isNearOccupiedPointOfInterest)
				.map(ChunkSectionPos::getCenterPos)
				.min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)))
				.ifPresent(this::setCenter);
	}

	private Optional<BlockPos> getRaidersSpawnLocation(ServerWorld world) {
		BlockPos spawnPos = findRandomRaidersSpawnLocation(world, 8);
		return spawnPos != null ? Optional.of(spawnPos) : Optional.empty();
	}

	private boolean shouldSpawnMoreGroups() {
		return hasExtraWave() ? !hasSpawnedExtraWave() : !hasSpawnedFinalWave();
	}

	private boolean hasSpawnedFinalWave() {
		return getGroupsSpawned() == waveCount;
	}

	private boolean hasExtraWave() {
		return raidOmenLevel > 1;
	}

	private boolean hasSpawnedExtraWave() {
		return getGroupsSpawned() > waveCount;
	}

	private boolean isSpawningExtraWave() {
		return hasSpawnedFinalWave() && getRaiderCount() == 0 && hasExtraWave();
	}

	private void removeObsoleteRaiders(ServerWorld world) {
		Iterator<Set<RaiderEntity>> waveIterator = waveToRaiders.values().iterator();
		Set<RaiderEntity> toRemove = Sets.newHashSet();

		while (waveIterator.hasNext()) {
			Set<RaiderEntity> waveRaiders = waveIterator.next();

			for (RaiderEntity raider : waveRaiders) {
				BlockPos raiderPos = raider.getBlockPos();

				if (raider.isRemoved()
						|| raider.getEntityWorld().getRegistryKey() != world.getRegistryKey()
						|| center.getSquaredDistance(raiderPos) >= SQUARED_MAX_RAIDER_DISTANCE
				) {
					toRemove.add(raider);
					continue;
				}

				if (raider.age > FINISH_COOLDOWN_MAX_TICKS) {
					if (world.getEntity(raider.getUuid()) == null) {
						toRemove.add(raider);
					}

					if (!world.isNearOccupiedPointOfInterest(raiderPos)
							&& raider.getDespawnCounter() > MAX_DESPAWN_COUNTER
					) {
						raider.setOutOfRaidCounter(raider.getOutOfRaidCounter() + 1);
					}

					if (raider.getOutOfRaidCounter() >= OUT_OF_RAID_COUNTER_MAX) {
						toRemove.add(raider);
					}
				}
			}
		}

		for (RaiderEntity raider : toRemove) {
			removeFromWave(world, raider, true);

			if (raider.isPatrolLeader()) {
				removeLeader(raider.getWave());
			}
		}
	}

	private void playRaidHorn(ServerWorld world, BlockPos pos) {
		Collection<ServerPlayerEntity> barPlayers = bar.getPlayers();
		long seed = random.nextLong();

		for (ServerPlayerEntity player : world.getPlayers()) {
			Vec3d playerPos = player.getEntityPos();
			Vec3d spawnCenter = Vec3d.ofCenter(pos);
			double horizontalDist = Math.sqrt(
					(spawnCenter.x - playerPos.x) * (spawnCenter.x - playerPos.x)
							+ (spawnCenter.z - playerPos.z) * (spawnCenter.z - playerPos.z)
			);
			double soundX = playerPos.x + HORN_SOUND_DISTANCE / horizontalDist * (spawnCenter.x - playerPos.x);
			double soundZ = playerPos.z + HORN_SOUND_DISTANCE / horizontalDist * (spawnCenter.z - playerPos.z);

			if (horizontalDist <= HORN_SOUND_VOLUME || barPlayers.contains(player)) {
				player.networkHandler.sendPacket(new PlaySoundS2CPacket(
						SoundEvents.EVENT_RAID_HORN,
						SoundCategory.NEUTRAL,
						soundX,
						player.getY(),
						soundZ,
						HORN_SOUND_VOLUME,
						1.0F,
						seed
				));
			}
		}
	}

	private void spawnNextWave(ServerWorld world, BlockPos pos) {
		boolean captainAssigned = false;
		int nextWave = wavesSpawned + 1;
		totalHealth = 0.0F;
		LocalDifficulty localDifficulty = world.getLocalDifficulty(pos);
		boolean isExtraWave = isSpawningExtraWave();

		for (Raid.Member member : Raid.Member.VALUES) {
			int count = getCount(member, nextWave, isExtraWave)
					+ getBonusCount(member, random, nextWave, localDifficulty, isExtraWave);
			int ravagerRiderIndex = 0;

			for (int spawn = 0; spawn < count; spawn++) {
				RaiderEntity raider = member.type.create(world, SpawnReason.EVENT);

				if (raider == null) {
					break;
				}

				if (!captainAssigned && raider.canLead()) {
					raider.setPatrolLeader(true);
					setWaveCaptain(nextWave, raider);
					captainAssigned = true;
				}

				addRaider(world, nextWave, raider, pos, false);

				if (member.type == EntityType.RAVAGER) {
					RaiderEntity rider = spawnRavagerRider(world, nextWave, ravagerRiderIndex);
					ravagerRiderIndex++;

					if (rider != null) {
						addRaider(world, nextWave, rider, pos, false);
						rider.refreshPositionAndAngles(pos, 0.0F, 0.0F);
						rider.startRiding(raider, false, false);
					}
				}
			}
		}

		preCalculatedRaidersSpawnLocation = Optional.empty();
		wavesSpawned++;
		updateBar();
		markDirty(world);
	}

	private @Nullable RaiderEntity spawnRavagerRider(ServerWorld world, int wave, int riderIndex) {
		int normalWaveCount = getMaxWaves(Difficulty.NORMAL);
		int hardWaveCount = getMaxWaves(Difficulty.HARD);

		if (wave == normalWaveCount) {
			return EntityType.PILLAGER.create(world, SpawnReason.EVENT);
		}

		if (wave >= hardWaveCount) {
			return riderIndex == 0
					? EntityType.EVOKER.create(world, SpawnReason.EVENT)
					: EntityType.VINDICATOR.create(world, SpawnReason.EVENT);
		}

		return null;
	}

	public void addRaider(ServerWorld world, int wave, RaiderEntity raider, @Nullable BlockPos pos, boolean existing) {
		boolean added = addToWave(world, wave, raider);

		if (!added) {
			return;
		}

		raider.setRaid(this);
		raider.setWave(wave);
		raider.setAbleToJoinRaid(true);
		raider.setOutOfRaidCounter(0);

		if (!existing && pos != null) {
			raider.setPosition(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
			raider.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
			raider.addBonusForWave(world, wave, false);
			raider.setOnGround(true);
			world.spawnEntityAndPassengers(raider);
		}
	}

	public void updateBar() {
		bar.setPercent(MathHelper.clamp(getCurrentRaiderHealth() / totalHealth, 0.0F, 1.0F));
	}

	public float getCurrentRaiderHealth() {
		float health = 0.0F;

		for (Set<RaiderEntity> wave : waveToRaiders.values()) {
			for (RaiderEntity raider : wave) {
				health += raider.getHealth();
			}
		}

		return health;
	}

	private boolean canSpawnRaiders() {
		return preRaidTicks == 0
				&& (wavesSpawned < waveCount || isSpawningExtraWave())
				&& getRaiderCount() == 0;
	}

	public int getRaiderCount() {
		return waveToRaiders.values().stream().mapToInt(Set::size).sum();
	}

	public void removeFromWave(ServerWorld world, RaiderEntity raider, boolean countHealth) {
		Set<RaiderEntity> wave = waveToRaiders.get(raider.getWave());

		if (wave == null) {
			return;
		}

		boolean removed = wave.remove(raider);

		if (!removed) {
			return;
		}

		if (countHealth) {
			totalHealth -= raider.getHealth();
		}

		raider.setRaid(null);
		updateBar();
		markDirty(world);
	}

	private void markDirty(ServerWorld world) {
		world.getRaidManager().markDirty();
	}

	/**
	 * Создаёт стек «Зловещего знамени» с уникальным узором для капитана рейда.
	 *
	 * @param bannerPatternLookup реестр узоров баннеров
	 * @return готовый стек знамени
	 */
	public static ItemStack createOminousBanner(RegistryEntryLookup<BannerPattern> bannerPatternLookup) {
		ItemStack banner = new ItemStack(Items.WHITE_BANNER);
		BannerPatternsComponent patterns = new BannerPatternsComponent.Builder()
				.add(bannerPatternLookup, BannerPatterns.RHOMBUS, DyeColor.CYAN)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_CENTER, DyeColor.GRAY)
				.add(bannerPatternLookup, BannerPatterns.BORDER, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_MIDDLE, DyeColor.BLACK)
				.add(bannerPatternLookup, BannerPatterns.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.CIRCLE, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.BORDER, DyeColor.BLACK)
				.build();
			banner.set(DataComponentTypes.BANNER_PATTERNS, patterns);
			banner.set(
					DataComponentTypes.TOOLTIP_DISPLAY,
					TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.BANNER_PATTERNS, true)
			);
			banner.set(DataComponentTypes.ITEM_NAME, OMINOUS_BANNER_TRANSLATION_KEY);
			banner.set(DataComponentTypes.RARITY, Rarity.UNCOMMON);
			return banner;
		}
	
		public @Nullable RaiderEntity getCaptain(int wave) {
			return waveToCaptain.get(wave);
		}
	
		/**
			* Ищет случайную позицию для спавна рейдеров вблизи центра рейда.
			* <p>
			* Позиция должна быть загружена, находиться в пределах высоты {@link #MAX_SPAWN_HEIGHT_DIFF}
			* от центра и допускать спавн Равагера (или быть покрыта снегом).
			*
			* @param world     серверный мир
			* @param proximity количество попыток поиска
			* @return найденная позиция или {@code null}
			*/
		private @Nullable BlockPos findRandomRaidersSpawnLocation(ServerWorld world, int proximity) {
			int progressTicks = preRaidTicks / BAR_UPDATE_INTERVAL;
			float radiusScale = SPAWN_RADIUS_SCALE * progressTicks - SPAWN_RADIUS_OFFSET;
			BlockPos.Mutable mutable = new BlockPos.Mutable();
			float baseAngle = world.random.nextFloat() * (float) (Math.PI * 2);
	
			for (int attempt = 0; attempt < proximity; attempt++) {
				float angle = baseAngle + (float) Math.PI * attempt / 8.0F;
				int x = center.getX()
						+ MathHelper.floor(MathHelper.cos(angle) * SPAWN_RADIUS * radiusScale)
						+ world.random.nextInt(3) * MathHelper.floor(radiusScale);
				int z = center.getZ()
						+ MathHelper.floor(MathHelper.sin(angle) * SPAWN_RADIUS * radiusScale)
						+ world.random.nextInt(3) * MathHelper.floor(radiusScale);
				int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
	
				if (MathHelper.abs(y - center.getY()) > MAX_SPAWN_HEIGHT_DIFF) {
					continue;
				}
	
				mutable.set(x, y, z);
	
				if (world.isNearOccupiedPointOfInterest(mutable) && progressTicks > 7) {
					continue;
				}
	
				if (!world.isRegionLoaded(
						mutable.getX() - SPAWN_LOCATION_CHECK_RADIUS,
						mutable.getZ() - SPAWN_LOCATION_CHECK_RADIUS,
						mutable.getX() + SPAWN_LOCATION_CHECK_RADIUS,
						mutable.getZ() + SPAWN_LOCATION_CHECK_RADIUS
				)) {
					continue;
				}
	
				if (!world.shouldTickEntityAt(mutable)) {
					continue;
				}
	
				boolean validSpawn = RAVAGER_SPAWN_LOCATION.isSpawnPositionOk(world, mutable, EntityType.RAVAGER)
						|| (world.getBlockState(mutable.down()).isOf(Blocks.SNOW) && world.getBlockState(mutable).isAir());
	
				if (validSpawn) {
					return mutable;
				}
			}
	
			return null;
		}
	
		private boolean addToWave(ServerWorld world, int wave, RaiderEntity raider) {
			return addToWave(world, wave, raider, true);
		}
	
		public boolean addToWave(ServerWorld world, int wave, RaiderEntity raider, boolean countHealth) {
			waveToRaiders.computeIfAbsent(wave, w -> Sets.newHashSet());
			Set<RaiderEntity> waveRaiders = waveToRaiders.get(wave);
	
			// Заменяем существующего рейдера с тем же UUID (переспавн после перезагрузки чанка)
			RaiderEntity existing = null;
	
			for (RaiderEntity member : waveRaiders) {
				if (member.getUuid().equals(raider.getUuid())) {
					existing = member;
					break;
				}
			}
	
			if (existing != null) {
				waveRaiders.remove(existing);
			}
	
			waveRaiders.add(raider);
	
			if (countHealth) {
				totalHealth += raider.getHealth();
			}
	
			updateBar();
			markDirty(world);
			return true;
		}
	
		public void setWaveCaptain(int wave, RaiderEntity entity) {
			waveToCaptain.put(wave, entity);
			entity.equipStack(
					EquipmentSlot.HEAD,
					createOminousBanner(entity.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
			);
			entity.setEquipmentDropChance(EquipmentSlot.HEAD, 2.0F);
		}
	
		public void removeLeader(int wave) {
			waveToCaptain.remove(wave);
		}
	
		public BlockPos getCenter() {
			return center;
		}
	
		private void setCenter(BlockPos center) {
			this.center = center;
		}
	
		private int getCount(Raid.Member member, int wave, boolean extra) {
			return extra ? member.countInWave[waveCount] : member.countInWave[wave];
		}
	
		private int getBonusCount(
				Raid.Member member,
				Random random,
				int wave,
				LocalDifficulty localDifficulty,
				boolean extra
		) {
			Difficulty difficulty = localDifficulty.getGlobalDifficulty();
			boolean isEasy = difficulty == Difficulty.EASY;
			boolean isNormal = difficulty == Difficulty.NORMAL;
	
			int bonus = switch (member) {
				case VINDICATOR, PILLAGER -> isEasy ? random.nextInt(2) : isNormal ? 1 : 2;
				case WITCH -> {
					if (isEasy || wave <= 2 || wave == 4) {
						yield 0;
					}
	
					yield 1;
				}
				case RAVAGER -> !isEasy && extra ? 1 : 0;
				default -> 0;
			};
	
			return bonus > 0 ? random.nextInt(bonus + 1) : 0;
		}
	
		public boolean isActive() {
			return active;
		}
	
		public int getMaxWaves(Difficulty difficulty) {
			return switch (difficulty) {
				case PEACEFUL -> 0;
				case EASY -> 3;
				case NORMAL -> 5;
				case HARD -> MAX_WAVE_COUNT_HARD;
			};
		}
	
		/**
			* Возвращает шанс зачарования снаряжения рейдеров в зависимости от уровня «Предзнаменования».
			*
			* @return вероятность зачарования от 0.0 до 0.75
			*/
		public float getEnchantmentChance() {
			return switch (getBadOmenLevel()) {
				case 2 -> 0.1F;
				case 3 -> 0.25F;
				case 4 -> 0.5F;
				case 5 -> 0.75F;
				default -> 0.0F;
			};
		}
	
		public void addHero(Entity entity) {
			heroesOfTheVillage.add(entity.getUuid());
		}
	
		enum Member {
			VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
			EVOKER(EntityType.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
			PILLAGER(EntityType.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
			WITCH(EntityType.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
			RAVAGER(EntityType.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});
	
			static final Raid.Member[] VALUES = values();
			final EntityType<? extends RaiderEntity> type;
			final int[] countInWave;
	
			Member(final EntityType<? extends RaiderEntity> type, final int[] countInWave) {
				this.type = type;
				this.countInWave = countInWave;
			}
		}
	
		enum Status implements StringIdentifiable {
			ONGOING("ongoing"),
			VICTORY("victory"),
			LOSS("loss"),
			STOPPED("stopped");
	
			public static final Codec<Raid.Status> CODEC = StringIdentifiable.createCodec(Raid.Status::values);
			private final String id;
	
			Status(final String id) {
				this.id = id;
			}
	
			@Override
			public String asString() {
				return id;
			}
		}
	}

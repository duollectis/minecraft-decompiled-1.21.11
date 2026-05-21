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
 * {@code Raid}.
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
			                    )
			                    .apply(instance, Raid::new)
	);
	private static final int MAX_WAVE_COUNT_HARD = 7;
	private static final int RAIDERS_REMAINING_THRESHOLD = 2;
	private static final int SPAWN_RADIUS = 32;
	private static final int RAID_TIMEOUT_TICKS = 48000;
	private static final int MAX_SPAWN_FAILURES = 5;
	private static final Text OMINOUS_BANNER_TRANSLATION_KEY = Text.translatable("block.minecraft.ominous_banner");
	private static final String RAIDERS_REMAINING_TRANSLATION_KEY = "event.minecraft.raid.raiders_remaining";
	public static final int MAX_RAID_OMEN_LEVEL = 16;
	private static final int POST_RAID_VICTORY_TICKS = 40;
	private static final int DEFAULT_PRE_RAID_TICKS = 300;
	public static final int MAX_DESPAWN_COUNTER = 2400;
	public static final int FINISH_COOLDOWN_MAX_TICKS = 600;
	private static final int OUT_OF_RAID_COUNTER_MAX = 30;
	public static final int MAX_BAD_OMEN_LEVEL = 5;
	private static final int MIN_WAVE_FOR_WITCH_BONUS = 2;
	private static final Text EVENT_TEXT = Text.translatable("event.minecraft.raid");
	private static final Text VICTORY_TITLE = Text.translatable("event.minecraft.raid.victory.full");
	private static final Text DEFEAT_TITLE = Text.translatable("event.minecraft.raid.defeat.full");
	private static final int MAX_ACTIVE_TICKS = 48000;
	private static final int MAX_SPAWN_HEIGHT_DIFF = 96;
	public static final int SQUARED_RAID_CENTER_DISTANCE = 9216;
	public static final int SQUARED_MAX_RAIDER_DISTANCE = 12544;
	private final Map<Integer, RaiderEntity> waveToCaptain = Maps.newHashMap();
	private final Map<Integer, Set<RaiderEntity>> waveToRaiders = Maps.newHashMap();
	private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
	private long ticksActive;
	private BlockPos center;
	private boolean started;
	private float totalHealth;
	private int raidOmenLevel;
	private boolean active;
	private int wavesSpawned;
	private final ServerBossBar bar = new ServerBossBar(EVENT_TEXT, BossBar.Color.RED, BossBar.Style.NOTCHED_10);
	private int postRaidTicks;
	private int preRaidTicks;
	private final Random random = Random.create();
	private final int waveCount;
	private Raid.Status status;
	private int finishCooldown;
	private Optional<BlockPos> preCalculatedRaidersSpawnLocation = Optional.empty();

	public Raid(BlockPos center, Difficulty difficulty) {
		this.active = true;
		this.preRaidTicks = 300;
		this.bar.setPercent(0.0F);
		this.center = center;
		this.waveCount = this.getMaxWaves(difficulty);
		this.status = Raid.Status.ONGOING;
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
		return this.hasWon() || this.hasLost();
	}

	public boolean isPreRaid() {
		return this.hasSpawned() && this.getRaiderCount() == 0 && this.preRaidTicks > 0;
	}

	public boolean hasSpawned() {
		return this.wavesSpawned > 0;
	}

	public boolean hasStopped() {
		return this.status == Raid.Status.STOPPED;
	}

	public boolean hasWon() {
		return this.status == Raid.Status.VICTORY;
	}

	public boolean hasLost() {
		return this.status == Raid.Status.LOSS;
	}

	public float getTotalHealth() {
		return this.totalHealth;
	}

	public Set<RaiderEntity> getAllRaiders() {
		Set<RaiderEntity> set = Sets.newHashSet();

		for (Set<RaiderEntity> set2 : this.waveToRaiders.values()) {
			set.addAll(set2);
		}

		return set;
	}

	public boolean hasStarted() {
		return this.started;
	}

	public int getGroupsSpawned() {
		return this.wavesSpawned;
	}

	private Predicate<ServerPlayerEntity> isInRaidDistance() {
		return player -> {
			BlockPos blockPos = player.getBlockPos();
			return player.isAlive() && player.getEntityWorld().getRaidAt(blockPos) == this;
		};
	}

	private void updateBarToPlayers(ServerWorld world) {
		Set<ServerPlayerEntity> set = Sets.newHashSet(this.bar.getPlayers());
		List<ServerPlayerEntity> list = world.getPlayers(this.isInRaidDistance());

		for (ServerPlayerEntity serverPlayerEntity : list) {
			if (!set.contains(serverPlayerEntity)) {
				this.bar.addPlayer(serverPlayerEntity);
			}
		}

		for (ServerPlayerEntity serverPlayerEntityx : set) {
			if (!list.contains(serverPlayerEntityx)) {
				this.bar.removePlayer(serverPlayerEntityx);
			}
		}
	}

	public int getMaxAcceptableBadOmenLevel() {
		return 5;
	}

	public int getBadOmenLevel() {
		return this.raidOmenLevel;
	}

	public void setBadOmenLevel(int badOmenLevel) {
		this.raidOmenLevel = badOmenLevel;
	}

	public boolean start(ServerPlayerEntity player) {
		StatusEffectInstance statusEffectInstance = player.getStatusEffect(StatusEffects.RAID_OMEN);
		if (statusEffectInstance == null) {
			return false;
		}
		else {
			this.raidOmenLevel = this.raidOmenLevel + statusEffectInstance.getAmplifier() + 1;
			this.raidOmenLevel = MathHelper.clamp(this.raidOmenLevel, 0, this.getMaxAcceptableBadOmenLevel());
			if (!this.hasSpawned()) {
				player.incrementStat(Stats.RAID_TRIGGER);
				Criteria.VOLUNTARY_EXILE.trigger(player);
			}

			return true;
		}
	}

	public void invalidate() {
		this.active = false;
		this.bar.clearPlayers();
		this.status = Raid.Status.STOPPED;
	}

	public void tick(ServerWorld world) {
		if (!this.hasStopped()) {
			if (this.status == Raid.Status.ONGOING) {
				boolean bl = this.active;
				this.active = world.isChunkLoaded(this.center);
				if (world.getDifficulty() == Difficulty.PEACEFUL) {
					this.invalidate();
					return;
				}

				if (bl != this.active) {
					this.bar.setVisible(this.active);
				}

				if (!this.active) {
					return;
				}

				if (!world.isNearOccupiedPointOfInterest(this.center)) {
					this.moveRaidCenter(world);
				}

				if (!world.isNearOccupiedPointOfInterest(this.center)) {
					if (this.wavesSpawned > 0) {
						this.status = Raid.Status.LOSS;
					}
					else {
						this.invalidate();
					}
				}

				this.ticksActive++;
				if (this.ticksActive >= 48000L) {
					this.invalidate();
					return;
				}

				int i = this.getRaiderCount();
				if (i == 0 && this.shouldSpawnMoreGroups()) {
					if (this.preRaidTicks <= 0) {
						if (this.preRaidTicks == 0 && this.wavesSpawned > 0) {
							this.preRaidTicks = 300;
							this.bar.setName(EVENT_TEXT);
							return;
						}
					}
					else {
						boolean bl2 = this.preCalculatedRaidersSpawnLocation.isPresent();
						boolean bl3 = !bl2 && this.preRaidTicks % 5 == 0;
						if (bl2 && !world.shouldTickEntityAt(this.preCalculatedRaidersSpawnLocation.get())) {
							bl3 = true;
						}

						if (bl3) {
							this.preCalculatedRaidersSpawnLocation = this.getRaidersSpawnLocation(world);
						}

						if (this.preRaidTicks == 300 || this.preRaidTicks % 20 == 0) {
							this.updateBarToPlayers(world);
						}

						this.preRaidTicks--;
						this.bar.setPercent(MathHelper.clamp((300 - this.preRaidTicks) / 300.0F, 0.0F, 1.0F));
					}
				}

				if (this.ticksActive % 20L == 0L) {
					this.updateBarToPlayers(world);
					this.removeObsoleteRaiders(world);
					if (i > 0) {
						if (i <= 2) {
							this.bar.setName(EVENT_TEXT
									.copy()
									.append(" - ")
									.append(Text.translatable("event.minecraft.raid.raiders_remaining", i)));
						}
						else {
							this.bar.setName(EVENT_TEXT);
						}
					}
					else {
						this.bar.setName(EVENT_TEXT);
					}
				}

				if (SharedConstants.RAIDS) {
					this.bar
							.setName(
									EVENT_TEXT.copy()
									          .append(" wave: ")
									          .append(this.wavesSpawned + "")
									          .append(ScreenTexts.SPACE)
									          .append("Raiders alive: ")
									          .append(this.getRaiderCount() + "")
									          .append(ScreenTexts.SPACE)
									          .append(this.getCurrentRaiderHealth() + "")
									          .append(" / ")
									          .append(this.totalHealth + "")
									          .append(" Is bonus? ")
									          .append((this.hasExtraWave() && this.hasSpawnedExtraWave()) + "")
									          .append(" Status: ")
									          .append(this.status.asString())
							);
				}

				boolean bl2x = false;
				int j = 0;

				while (this.canSpawnRaiders()) {
					BlockPos
							blockPos =
							this.preCalculatedRaidersSpawnLocation.orElseGet(() -> this.findRandomRaidersSpawnLocation(
									world,
									20
							));
					if (blockPos != null) {
						this.started = true;
						this.spawnNextWave(world, blockPos);
						if (!bl2x) {
							this.playRaidHorn(world, blockPos);
							bl2x = true;
						}
					}
					else {
						j++;
					}

					if (j > 5) {
						this.invalidate();
						break;
					}
				}

				if (this.hasStarted() && !this.shouldSpawnMoreGroups() && i == 0) {
					if (this.postRaidTicks < 40) {
						this.postRaidTicks++;
					}
					else {
						this.status = Raid.Status.VICTORY;

						for (UUID uUID : this.heroesOfTheVillage) {
							Entity entity = world.getEntity(uUID);
							if (entity instanceof LivingEntity livingEntity && !entity.isSpectator()) {
								livingEntity.addStatusEffect(
										new StatusEffectInstance(
												StatusEffects.HERO_OF_THE_VILLAGE,
												48000,
												this.raidOmenLevel - 1,
												false,
												false,
												true
										)
								);
								if (livingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
									serverPlayerEntity.incrementStat(Stats.RAID_WIN);
									Criteria.HERO_OF_THE_VILLAGE.trigger(serverPlayerEntity);
								}
							}
						}
					}
				}

				this.markDirty(world);
			}
			else if (this.isFinished()) {
				this.finishCooldown++;
				if (this.finishCooldown >= 600) {
					this.invalidate();
					return;
				}

				if (this.finishCooldown % 20 == 0) {
					this.updateBarToPlayers(world);
					this.bar.setVisible(true);
					if (this.hasWon()) {
						this.bar.setPercent(0.0F);
						this.bar.setName(VICTORY_TITLE);
					}
					else {
						this.bar.setName(DEFEAT_TITLE);
					}
				}
			}
		}
	}

	private void moveRaidCenter(ServerWorld world) {
		Stream<ChunkSectionPos> stream = ChunkSectionPos.stream(ChunkSectionPos.from(this.center), 2);
		stream.filter(world::isNearOccupiedPointOfInterest)
		      .map(ChunkSectionPos::getCenterPos)
		      .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(this.center)))
		      .ifPresent(this::setCenter);
	}

	private Optional<BlockPos> getRaidersSpawnLocation(ServerWorld world) {
		BlockPos blockPos = this.findRandomRaidersSpawnLocation(world, 8);
		return blockPos != null ? Optional.of(blockPos) : Optional.empty();
	}

	private boolean shouldSpawnMoreGroups() {
		return this.hasExtraWave() ? !this.hasSpawnedExtraWave() : !this.hasSpawnedFinalWave();
	}

	private boolean hasSpawnedFinalWave() {
		return this.getGroupsSpawned() == this.waveCount;
	}

	private boolean hasExtraWave() {
		return this.raidOmenLevel > 1;
	}

	private boolean hasSpawnedExtraWave() {
		return this.getGroupsSpawned() > this.waveCount;
	}

	private boolean isSpawningExtraWave() {
		return this.hasSpawnedFinalWave() && this.getRaiderCount() == 0 && this.hasExtraWave();
	}

	private void removeObsoleteRaiders(ServerWorld world) {
		Iterator<Set<RaiderEntity>> iterator = this.waveToRaiders.values().iterator();
		Set<RaiderEntity> set = Sets.newHashSet();

		while (iterator.hasNext()) {
			Set<RaiderEntity> set2 = iterator.next();

			for (RaiderEntity raiderEntity : set2) {
				BlockPos blockPos = raiderEntity.getBlockPos();
				if (raiderEntity.isRemoved()
						|| raiderEntity.getEntityWorld().getRegistryKey() != world.getRegistryKey()
						|| this.center.getSquaredDistance(blockPos) >= 12544.0) {
					set.add(raiderEntity);
				}
				else if (raiderEntity.age > 600) {
					if (world.getEntity(raiderEntity.getUuid()) == null) {
						set.add(raiderEntity);
					}

					if (!world.isNearOccupiedPointOfInterest(blockPos) && raiderEntity.getDespawnCounter() > 2400) {
						raiderEntity.setOutOfRaidCounter(raiderEntity.getOutOfRaidCounter() + 1);
					}

					if (raiderEntity.getOutOfRaidCounter() >= 30) {
						set.add(raiderEntity);
					}
				}
			}
		}

		for (RaiderEntity raiderEntity2 : set) {
			this.removeFromWave(world, raiderEntity2, true);
			if (raiderEntity2.isPatrolLeader()) {
				this.removeLeader(raiderEntity2.getWave());
			}
		}
	}

	private void playRaidHorn(ServerWorld world, BlockPos pos) {
		float f = 13.0F;
		int i = 64;
		Collection<ServerPlayerEntity> collection = this.bar.getPlayers();
		long l = this.random.nextLong();

		for (ServerPlayerEntity serverPlayerEntity : world.getPlayers()) {
			Vec3d vec3d = serverPlayerEntity.getEntityPos();
			Vec3d vec3d2 = Vec3d.ofCenter(pos);
			double
					d =
					Math.sqrt(
							(vec3d2.x - vec3d.x) * (vec3d2.x - vec3d.x) + (vec3d2.z - vec3d.z) * (vec3d2.z - vec3d.z));
			double e = vec3d.x + 13.0 / d * (vec3d2.x - vec3d.x);
			double g = vec3d.z + 13.0 / d * (vec3d2.z - vec3d.z);
			if (d <= 64.0 || collection.contains(serverPlayerEntity)) {
				serverPlayerEntity.networkHandler
						.sendPacket(new PlaySoundS2CPacket(
								SoundEvents.EVENT_RAID_HORN,
								SoundCategory.NEUTRAL,
								e,
								serverPlayerEntity.getY(),
								g,
								64.0F,
								1.0F,
								l
						));
			}
		}
	}

	private void spawnNextWave(ServerWorld world, BlockPos pos) {
		boolean bl = false;
		int i = this.wavesSpawned + 1;
		this.totalHealth = 0.0F;
		LocalDifficulty localDifficulty = world.getLocalDifficulty(pos);
		boolean bl2 = this.isSpawningExtraWave();

		for (Raid.Member member : Raid.Member.VALUES) {
			int j = this.getCount(member, i, bl2) + this.getBonusCount(member, this.random, i, localDifficulty, bl2);
			int k = 0;

			for (int l = 0; l < j; l++) {
				RaiderEntity raiderEntity = member.type.create(world, SpawnReason.EVENT);
				if (raiderEntity == null) {
					break;
				}

				if (!bl && raiderEntity.canLead()) {
					raiderEntity.setPatrolLeader(true);
					this.setWaveCaptain(i, raiderEntity);
					bl = true;
				}

				this.addRaider(world, i, raiderEntity, pos, false);
				if (member.type == EntityType.RAVAGER) {
					RaiderEntity raiderEntity2 = null;
					if (i == this.getMaxWaves(Difficulty.NORMAL)) {
						raiderEntity2 = EntityType.PILLAGER.create(world, SpawnReason.EVENT);
					}
					else if (i >= this.getMaxWaves(Difficulty.HARD)) {
						if (k == 0) {
							raiderEntity2 = EntityType.EVOKER.create(world, SpawnReason.EVENT);
						}
						else {
							raiderEntity2 = EntityType.VINDICATOR.create(world, SpawnReason.EVENT);
						}
					}

					k++;
					if (raiderEntity2 != null) {
						this.addRaider(world, i, raiderEntity2, pos, false);
						raiderEntity2.refreshPositionAndAngles(pos, 0.0F, 0.0F);
						raiderEntity2.startRiding(raiderEntity, false, false);
					}
				}
			}
		}

		this.preCalculatedRaidersSpawnLocation = Optional.empty();
		this.wavesSpawned++;
		this.updateBar();
		this.markDirty(world);
	}

	public void addRaider(ServerWorld world, int wave, RaiderEntity raider, @Nullable BlockPos pos, boolean existing) {
		boolean bl = this.addToWave(world, wave, raider);
		if (bl) {
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
	}

	public void updateBar() {
		this.bar.setPercent(MathHelper.clamp(this.getCurrentRaiderHealth() / this.totalHealth, 0.0F, 1.0F));
	}

	public float getCurrentRaiderHealth() {
		float f = 0.0F;

		for (Set<RaiderEntity> set : this.waveToRaiders.values()) {
			for (RaiderEntity raiderEntity : set) {
				f += raiderEntity.getHealth();
			}
		}

		return f;
	}

	private boolean canSpawnRaiders() {
		return this.preRaidTicks == 0 && (this.wavesSpawned < this.waveCount || this.isSpawningExtraWave())
				&& this.getRaiderCount() == 0;
	}

	public int getRaiderCount() {
		return this.waveToRaiders.values().stream().mapToInt(Set::size).sum();
	}

	public void removeFromWave(ServerWorld world, RaiderEntity raider, boolean countHealth) {
		Set<RaiderEntity> set = this.waveToRaiders.get(raider.getWave());
		if (set != null) {
			boolean bl = set.remove(raider);
			if (bl) {
				if (countHealth) {
					this.totalHealth = this.totalHealth - raider.getHealth();
				}

				raider.setRaid(null);
				this.updateBar();
				this.markDirty(world);
			}
		}
	}

	private void markDirty(ServerWorld world) {
		world.getRaidManager().markDirty();
	}

	public static ItemStack createOminousBanner(RegistryEntryLookup<BannerPattern> bannerPatternLookup) {
		ItemStack itemStack = new ItemStack(Items.WHITE_BANNER);
		BannerPatternsComponent bannerPatternsComponent = new BannerPatternsComponent.Builder()
				.add(bannerPatternLookup, BannerPatterns.RHOMBUS, DyeColor.CYAN)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_CENTER, DyeColor.GRAY)
				.add(bannerPatternLookup, BannerPatterns.BORDER, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.STRIPE_MIDDLE, DyeColor.BLACK)
				.add(bannerPatternLookup, BannerPatterns.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.CIRCLE, DyeColor.LIGHT_GRAY)
				.add(bannerPatternLookup, BannerPatterns.BORDER, DyeColor.BLACK)
				.build();
		itemStack.set(DataComponentTypes.BANNER_PATTERNS, bannerPatternsComponent);
		itemStack.set(
				DataComponentTypes.TOOLTIP_DISPLAY,
				TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.BANNER_PATTERNS, true)
		);
		itemStack.set(DataComponentTypes.ITEM_NAME, OMINOUS_BANNER_TRANSLATION_KEY);
		itemStack.set(DataComponentTypes.RARITY, Rarity.UNCOMMON);
		return itemStack;
	}

	public @Nullable RaiderEntity getCaptain(int wave) {
		return this.waveToCaptain.get(wave);
	}

	private @Nullable BlockPos findRandomRaidersSpawnLocation(ServerWorld world, int proximity) {
		int i = this.preRaidTicks / 20;
		float f = 0.22F * i - 0.24F;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		float g = world.random.nextFloat() * (float) (Math.PI * 2);

		for (int j = 0; j < proximity; j++) {
			float h = g + (float) Math.PI * j / 8.0F;
			int
					k =
					this.center.getX() + MathHelper.floor(MathHelper.cos(h) * 32.0F * f)
							+ world.random.nextInt(3) * MathHelper.floor(f);
			int
					l =
					this.center.getZ() + MathHelper.floor(MathHelper.sin(h) * 32.0F * f)
							+ world.random.nextInt(3) * MathHelper.floor(f);
			int m = world.getTopY(Heightmap.Type.WORLD_SURFACE, k, l);
			if (MathHelper.abs(m - this.center.getY()) <= 96) {
				mutable.set(k, m, l);
				if (!world.isNearOccupiedPointOfInterest(mutable) || i <= 7) {
					int n = 10;
					if (world.isRegionLoaded(
							mutable.getX() - 10,
							mutable.getZ() - 10,
							mutable.getX() + 10,
							mutable.getZ() + 10
					)
							&& world.shouldTickEntityAt(mutable)
							&& (
							RAVAGER_SPAWN_LOCATION.isSpawnPositionOk(world, mutable, EntityType.RAVAGER)
									|| world.getBlockState(mutable.down()).isOf(Blocks.SNOW) && world
									.getBlockState(mutable)
									.isAir()
					)) {
						return mutable;
					}
				}
			}
		}

		return null;
	}

	private boolean addToWave(ServerWorld world, int wave, RaiderEntity raider) {
		return this.addToWave(world, wave, raider, true);
	}

	public boolean addToWave(ServerWorld world, int wave, RaiderEntity raider, boolean countHealth) {
		this.waveToRaiders.computeIfAbsent(wave, wavex -> Sets.newHashSet());
		Set<RaiderEntity> set = this.waveToRaiders.get(wave);
		RaiderEntity raiderEntity = null;

		for (RaiderEntity raiderEntity2 : set) {
			if (raiderEntity2.getUuid().equals(raider.getUuid())) {
				raiderEntity = raiderEntity2;
				break;
			}
		}

		if (raiderEntity != null) {
			set.remove(raiderEntity);
			set.add(raider);
		}

		set.add(raider);
		if (countHealth) {
			this.totalHealth = this.totalHealth + raider.getHealth();
		}

		this.updateBar();
		this.markDirty(world);
		return true;
	}

	public void setWaveCaptain(int wave, RaiderEntity entity) {
		this.waveToCaptain.put(wave, entity);
		entity.equipStack(
				EquipmentSlot.HEAD,
				createOminousBanner(entity.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
		);
		entity.setEquipmentDropChance(EquipmentSlot.HEAD, 2.0F);
	}

	public void removeLeader(int wave) {
		this.waveToCaptain.remove(wave);
	}

	public BlockPos getCenter() {
		return this.center;
	}

	private void setCenter(BlockPos center) {
		this.center = center;
	}

	private int getCount(Raid.Member member, int wave, boolean extra) {
		return extra ? member.countInWave[this.waveCount] : member.countInWave[wave];
	}

	private int getBonusCount(
			Raid.Member member,
			Random random,
			int wave,
			LocalDifficulty localDifficulty,
			boolean extra
	) {
		Difficulty difficulty = localDifficulty.getGlobalDifficulty();
		boolean bl = difficulty == Difficulty.EASY;
		boolean bl2 = difficulty == Difficulty.NORMAL;
		int i;
		switch (member) {
			case VINDICATOR:
			case PILLAGER:
				if (bl) {
					i = random.nextInt(2);
				}
				else if (bl2) {
					i = 1;
				}
				else {
					i = 2;
				}
				break;
			case EVOKER:
			default:
				return 0;
			case WITCH:
				if (bl || wave <= 2 || wave == 4) {
					return 0;
				}

				i = 1;
				break;
			case RAVAGER:
				i = !bl && extra ? 1 : 0;
		}

		return i > 0 ? random.nextInt(i + 1) : 0;
	}

	public boolean isActive() {
		return this.active;
	}

	public int getMaxWaves(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> 3;
			case NORMAL -> 5;
			case HARD -> 7;
		};
	}

	public float getEnchantmentChance() {
		int i = this.getBadOmenLevel();
		if (i == 2) {
			return 0.1F;
		}
		else if (i == 3) {
			return 0.25F;
		}
		else if (i == 4) {
			return 0.5F;
		}
		else {
			return i == 5 ? 0.75F : 0.0F;
		}
	}

	public void addHero(Entity entity) {
		this.heroesOfTheVillage.add(entity.getUuid());
	}

	/**
	 * {@code Member}.
	 */
	static enum Member {
		VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
		EVOKER(EntityType.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
		PILLAGER(EntityType.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
		WITCH(EntityType.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
		RAVAGER(EntityType.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

		static final Raid.Member[] VALUES = values();
		final EntityType<? extends RaiderEntity> type;
		final int[] countInWave;

		private Member(final EntityType<? extends RaiderEntity> type, final int[] countInWave) {
			this.type = type;
			this.countInWave = countInWave;
		}
	}

	/**
	 * {@code Status}.
	 */
	static enum Status implements StringIdentifiable {
		ONGOING("ongoing"),
		VICTORY("victory"),
		LOSS("loss"),
		STOPPED("stopped");

		public static final Codec<Raid.Status> CODEC = StringIdentifiable.createCodec(Raid.Status::values);
		private final String id;

		private Status(final String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return this.id;
		}
	}
}

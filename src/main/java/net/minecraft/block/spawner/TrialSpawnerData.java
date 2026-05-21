package net.minecraft.block.spawner;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LoadedEntityProcessor;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * {@code TrialSpawnerData}.
 */
public class TrialSpawnerData {

	private static final String SPAWN_DATA_KEY = "spawn_data";
	private static final String NEXT_MOB_SPAWNS_AT_KEY = "next_mob_spawns_at";
	private static final int DISPLAY_ENTITY_ROTATION_SPEED = 20;
	private static final int COOLDOWN_TICKS = 18000;
	public Set<UUID> players = new HashSet<>();
	public Set<UUID> spawnedMobsAlive = new HashSet<>();
	public long cooldownEnd;
	public long nextMobSpawnsAt;
	public int totalSpawnedMobs;
	public Optional<MobSpawnerEntry> spawnData = Optional.empty();
	public Optional<RegistryKey<LootTable>> rewardLootTable = Optional.empty();
	private @Nullable Entity displayEntity;
	private @Nullable Pool<ItemStack> itemsToDropWhenOminous;
	public double displayEntityRotation;
	public double lastDisplayEntityRotation;

	public TrialSpawnerData.Packed pack() {
		return new TrialSpawnerData.Packed(
				Set.copyOf(this.players),
				Set.copyOf(this.spawnedMobsAlive),
				this.cooldownEnd,
				this.nextMobSpawnsAt,
				this.totalSpawnedMobs,
				this.spawnData,
				this.rewardLootTable
		);
	}

	public void unpack(TrialSpawnerData.Packed packed) {
		this.players.clear();
		this.players.addAll(packed.detectedPlayers);
		this.spawnedMobsAlive.clear();
		this.spawnedMobsAlive.addAll(packed.currentMobs);
		this.cooldownEnd = packed.cooldownEndsAt;
		this.nextMobSpawnsAt = packed.nextMobSpawnsAt;
		this.totalSpawnedMobs = packed.totalMobsSpawned;
		this.spawnData = packed.nextSpawnData;
		this.rewardLootTable = packed.ejectingLootTable;
	}

	public void reset() {
		this.spawnedMobsAlive.clear();
		this.spawnData = Optional.empty();
		this.deactivate();
	}

	public void deactivate() {
		this.players.clear();
		this.totalSpawnedMobs = 0;
		this.nextMobSpawnsAt = 0L;
		this.cooldownEnd = 0L;
	}

	public boolean hasSpawnData(TrialSpawnerLogic logic, Random random) {
		boolean bl = this.getSpawnData(logic, random).getNbt().getString("id").isPresent();
		return bl || !logic.getConfig().spawnPotentials().isEmpty();
	}

	public boolean hasSpawnedAllMobs(TrialSpawnerConfig config, int additionalPlayers) {
		return this.totalSpawnedMobs >= config.getTotalMobs(additionalPlayers);
	}

	public boolean areMobsDead() {
		return this.spawnedMobsAlive.isEmpty();
	}

	public boolean canSpawnMore(ServerWorld world, TrialSpawnerConfig config, int additionalPlayers) {
		return world.getTime() >= this.nextMobSpawnsAt && this.spawnedMobsAlive.size() < config.getSimultaneousMobs(
				additionalPlayers);
	}

	public int getAdditionalPlayers(BlockPos pos) {
		if (this.players.isEmpty()) {
			Util.logErrorOrPause("Trial Spawner at " + pos + " has no detected players");
		}

		return Math.max(0, this.players.size() - 1);
	}

	public void updatePlayers(ServerWorld world, BlockPos pos, TrialSpawnerLogic logic) {
		boolean bl = (pos.asLong() + world.getTime()) % 20L != 0L;
		if (!bl) {
			if (!logic.getSpawnerState().equals(TrialSpawnerState.COOLDOWN) || !logic.isOminous()) {
				List<UUID>
						list =
						logic
								.getEntityDetector()
								.detect(world, logic.getEntitySelector(), pos, logic.getDetectionRadius(), true);
				boolean bl2;
				if (!logic.isOminous() && !list.isEmpty()) {
					Optional<Pair<PlayerEntity, RegistryEntry<StatusEffect>>>
							optional =
							findPlayerWithOmen(world, list);
					optional.ifPresent(pair -> {
						PlayerEntity playerEntity = (PlayerEntity) pair.getFirst();
						if (pair.getSecond() == StatusEffects.BAD_OMEN) {
							applyTrialOmen(playerEntity);
						}

						world.syncWorldEvent(3020, BlockPos.ofFloored(playerEntity.getEyePos()), 0);
						logic.setOminous(world, pos);
					});
					bl2 = optional.isPresent();
				}
				else {
					bl2 = false;
				}

				if (!logic.getSpawnerState().equals(TrialSpawnerState.COOLDOWN) || bl2) {
					boolean bl3 = logic.getData().players.isEmpty();
					List<UUID>
							list2 =
							bl3 ? list : logic
							             .getEntityDetector()
							             .detect(
									             world,
									             logic.getEntitySelector(),
									             pos,
									             logic.getDetectionRadius(),
									             false
							             );
					if (this.players.addAll(list2)) {
						this.nextMobSpawnsAt = Math.max(world.getTime() + 40L, this.nextMobSpawnsAt);
						if (!bl2) {
							int i = logic.isOminous() ? 3019 : 3013;
							world.syncWorldEvent(i, pos, this.players.size());
						}
					}
				}
			}
		}
	}

	private static Optional<Pair<PlayerEntity, RegistryEntry<StatusEffect>>> findPlayerWithOmen(
			ServerWorld world,
			List<UUID> players
	) {
		PlayerEntity playerEntity = null;

		for (UUID uUID : players) {
			PlayerEntity playerEntity2 = world.getPlayerByUuid(uUID);
			if (playerEntity2 != null) {
				RegistryEntry<StatusEffect> registryEntry = StatusEffects.TRIAL_OMEN;
				if (playerEntity2.hasStatusEffect(registryEntry)) {
					return Optional.of(Pair.of(playerEntity2, registryEntry));
				}

				if (playerEntity2.hasStatusEffect(StatusEffects.BAD_OMEN)) {
					playerEntity = playerEntity2;
				}
			}
		}

		return Optional.ofNullable(playerEntity).map(player -> Pair.of(player, StatusEffects.BAD_OMEN));
	}

	public void resetAndClearMobs(TrialSpawnerLogic logic, ServerWorld world) {
		this.spawnedMobsAlive.stream().map(world::getEntity).forEach(entity -> {
			if (entity != null) {
				world.syncWorldEvent(3012, entity.getBlockPos(), TrialSpawnerLogic.Type.NORMAL.getIndex());
				if (entity instanceof MobEntity mobEntity) {
					mobEntity.dropAllForeignEquipment(world);
				}

				entity.remove(Entity.RemovalReason.DISCARDED);
			}
		});
		if (!logic.getOminousConfig().spawnPotentials().isEmpty()) {
			this.spawnData = Optional.empty();
		}

		this.totalSpawnedMobs = 0;
		this.spawnedMobsAlive.clear();
		this.nextMobSpawnsAt = world.getTime() + logic.getOminousConfig().ticksBetweenSpawn();
		logic.updateListeners();
		this.cooldownEnd = world.getTime() + logic.getOminousConfig().getCooldownLength();
	}

	private static void applyTrialOmen(PlayerEntity player) {
		StatusEffectInstance statusEffectInstance = player.getStatusEffect(StatusEffects.BAD_OMEN);
		if (statusEffectInstance != null) {
			int i = statusEffectInstance.getAmplifier() + 1;
			int j = 18000 * i;
			player.removeStatusEffect(StatusEffects.BAD_OMEN);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.TRIAL_OMEN, j, 0));
		}
	}

	public boolean isCooldownPast(ServerWorld world, float f, int i) {
		long l = this.cooldownEnd - i;
		return (float) world.getTime() >= (float) l + f;
	}

	public boolean isCooldownAtRepeating(ServerWorld world, float f, int i) {
		long l = this.cooldownEnd - i;
		return (float) (world.getTime() - l) % f == 0.0F;
	}

	public boolean isCooldownOver(ServerWorld world) {
		return world.getTime() >= this.cooldownEnd;
	}

	protected MobSpawnerEntry getSpawnData(TrialSpawnerLogic logic, Random random) {
		if (this.spawnData.isPresent()) {
			return this.spawnData.get();
		}
		else {
			Pool<MobSpawnerEntry> pool = logic.getConfig().spawnPotentials();
			Optional<MobSpawnerEntry> optional = pool.isEmpty() ? this.spawnData : pool.getOrEmpty(random);
			this.spawnData = Optional.of(optional.orElseGet(MobSpawnerEntry::new));
			logic.updateListeners();
			return this.spawnData.get();
		}
	}

	public @Nullable Entity setDisplayEntity(TrialSpawnerLogic logic, World world, TrialSpawnerState state) {
		if (!state.doesDisplayRotate()) {
			return null;
		}
		else {
			if (this.displayEntity == null) {
				NbtCompound nbtCompound = this.getSpawnData(logic, world.getRandom()).getNbt();
				if (nbtCompound.getString("id").isPresent()) {
					this.displayEntity =
							EntityType.loadEntityWithPassengers(
									nbtCompound,
									world,
									SpawnReason.TRIAL_SPAWNER,
									LoadedEntityProcessor.NOOP
							);
				}
			}

			return this.displayEntity;
		}
	}

	public NbtCompound getSpawnDataNbt(TrialSpawnerState state) {
		NbtCompound nbtCompound = new NbtCompound();
		if (state == TrialSpawnerState.ACTIVE) {
			nbtCompound.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
		}

		this.spawnData.ifPresent(spawnData -> nbtCompound.put("spawn_data", MobSpawnerEntry.CODEC, spawnData));
		return nbtCompound;
	}

	public double getDisplayEntityRotation() {
		return this.displayEntityRotation;
	}

	public double getLastDisplayEntityRotation() {
		return this.lastDisplayEntityRotation;
	}

	public Pool<ItemStack> getItemsToDropWhenOminous(ServerWorld world, TrialSpawnerConfig config, BlockPos pos) {
		if (this.itemsToDropWhenOminous != null) {
			return this.itemsToDropWhenOminous;
		}
		else {
			LootTable
					lootTable =
					world.getServer().getReloadableRegistries().getLootTable(config.itemsToDropWhenOminous());
			LootWorldContext lootWorldContext = new LootWorldContext.Builder(world).build(LootContextTypes.EMPTY);
			long l = getLootSeed(world, pos);
			ObjectArrayList<ItemStack> objectArrayList = lootTable.generateLoot(lootWorldContext, l);
			if (objectArrayList.isEmpty()) {
				return Pool.empty();
			}
			else {
				Pool.Builder<ItemStack> builder = Pool.builder();
				ObjectListIterator var10 = objectArrayList.iterator();

				while (var10.hasNext()) {
					ItemStack itemStack = (ItemStack) var10.next();
					builder.add(itemStack.copyWithCount(1), itemStack.getCount());
				}

				this.itemsToDropWhenOminous = builder.build();
				return this.itemsToDropWhenOminous;
			}
		}
	}

	private static long getLootSeed(ServerWorld world, BlockPos pos) {
		BlockPos
				blockPos =
				new BlockPos(
						MathHelper.floor(pos.getX() / 30.0F),
						MathHelper.floor(pos.getY() / 20.0F),
						MathHelper.floor(pos.getZ() / 30.0F)
				);
		return world.getSeed() + blockPos.asLong();
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(
			Set<UUID> detectedPlayers,
			Set<UUID> currentMobs,
			long cooldownEndsAt,
			long nextMobSpawnsAt,
			int totalMobsSpawned,
			Optional<MobSpawnerEntry> nextSpawnData,
			Optional<RegistryKey<LootTable>> ejectingLootTable
	) {

		public static final MapCodec<TrialSpawnerData.Packed> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Uuids.SET_CODEC
								                    .lenientOptionalFieldOf("registered_players", Set.of())
								                    .forGetter(TrialSpawnerData.Packed::detectedPlayers),
						                    Uuids.SET_CODEC
								                    .lenientOptionalFieldOf("current_mobs", Set.of())
								                    .forGetter(TrialSpawnerData.Packed::currentMobs),
						                    Codec.LONG
								                    .lenientOptionalFieldOf("cooldown_ends_at", 0L)
								                    .forGetter(TrialSpawnerData.Packed::cooldownEndsAt),
						                    Codec.LONG
								                    .lenientOptionalFieldOf("next_mob_spawns_at", 0L)
								                    .forGetter(TrialSpawnerData.Packed::nextMobSpawnsAt),
						                    Codec
								                    .intRange(0, Integer.MAX_VALUE)
								                    .lenientOptionalFieldOf("total_mobs_spawned", 0)
								                    .forGetter(TrialSpawnerData.Packed::totalMobsSpawned),
						                    MobSpawnerEntry.CODEC
								                    .lenientOptionalFieldOf("spawn_data")
								                    .forGetter(TrialSpawnerData.Packed::nextSpawnData),
						                    LootTable.TABLE_KEY
								                    .lenientOptionalFieldOf("ejecting_loot_table")
								                    .forGetter(TrialSpawnerData.Packed::ejectingLootTable)
				                    )
				                    .apply(instance, TrialSpawnerData.Packed::new)
		);
	}
}

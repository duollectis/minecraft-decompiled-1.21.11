package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;

import java.util.Map;
import java.util.Optional;

/**
 * Задача мозга жителя, дарящего подарки игроку с эффектом «Герой деревни».
 * Выбирает таблицу лута по профессии жителя и бросает предметы в сторону героя.
 */
public class GiveGiftsToHeroTask extends MultiTickTask<VillagerEntity> {

	private static final int MAX_DISTANCE = 5;
	private static final int DEFAULT_DURATION = 600;
	private static final int MAX_EXTRA_GIFT_DELAY = 6001;
	private static final int RUN_TIME = 20;
	private static final float WALK_SPEED = 0.5F;
	private static final Map<RegistryKey<VillagerProfession>, RegistryKey<LootTable>> GIFTS =
			ImmutableMap.<RegistryKey<VillagerProfession>, RegistryKey<LootTable>>builder()
					.put(VillagerProfession.ARMORER, LootTables.HERO_OF_THE_VILLAGE_ARMORER_GIFT_GAMEPLAY)
					.put(VillagerProfession.BUTCHER, LootTables.HERO_OF_THE_VILLAGE_BUTCHER_GIFT_GAMEPLAY)
					.put(VillagerProfession.CARTOGRAPHER, LootTables.HERO_OF_THE_VILLAGE_CARTOGRAPHER_GIFT_GAMEPLAY)
					.put(VillagerProfession.CLERIC, LootTables.HERO_OF_THE_VILLAGE_CLERIC_GIFT_GAMEPLAY)
					.put(VillagerProfession.FARMER, LootTables.HERO_OF_THE_VILLAGE_FARMER_GIFT_GAMEPLAY)
					.put(VillagerProfession.FISHERMAN, LootTables.HERO_OF_THE_VILLAGE_FISHERMAN_GIFT_GAMEPLAY)
					.put(VillagerProfession.FLETCHER, LootTables.HERO_OF_THE_VILLAGE_FLETCHER_GIFT_GAMEPLAY)
					.put(VillagerProfession.LEATHERWORKER, LootTables.HERO_OF_THE_VILLAGE_LEATHERWORKER_GIFT_GAMEPLAY)
					.put(VillagerProfession.LIBRARIAN, LootTables.HERO_OF_THE_VILLAGE_LIBRARIAN_GIFT_GAMEPLAY)
					.put(VillagerProfession.MASON, LootTables.HERO_OF_THE_VILLAGE_MASON_GIFT_GAMEPLAY)
					.put(VillagerProfession.SHEPHERD, LootTables.HERO_OF_THE_VILLAGE_SHEPHERD_GIFT_GAMEPLAY)
					.put(VillagerProfession.TOOLSMITH, LootTables.HERO_OF_THE_VILLAGE_TOOLSMITH_GIFT_GAMEPLAY)
					.put(VillagerProfession.WEAPONSMITH, LootTables.HERO_OF_THE_VILLAGE_WEAPONSMITH_GIFT_GAMEPLAY)
					.build();

	private int ticksLeft = DEFAULT_DURATION;
	private boolean done;
	private long startTime;

	public GiveGiftsToHeroTask(int delay) {
		super(
				ImmutableMap.of(
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.INTERACTION_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.NEAREST_VISIBLE_PLAYER,
						MemoryModuleState.VALUE_PRESENT
				),
				delay
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		if (!isNearestPlayerHero(entity)) {
			return false;
		}

		if (ticksLeft > 0) {
			ticksLeft--;
			return false;
		}

		return true;
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		done = false;
		startTime = time;
		PlayerEntity hero = getNearestPlayerIfHero(entity).get();
		entity.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, hero);
		TargetUtil.lookAt(entity, hero);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return isNearestPlayerHero(entity) && !done;
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		PlayerEntity hero = getNearestPlayerIfHero(entity).get();
		TargetUtil.lookAt(entity, hero);

		if (isCloseEnough(entity, hero)) {
			if (time - startTime > RUN_TIME) {
				giveGifts(world, entity, hero);
				done = true;
			}
		} else {
			TargetUtil.walkTowards(entity, hero, WALK_SPEED, MAX_DISTANCE);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		ticksLeft = getNextGiftDelay(world);
		entity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		entity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
	}

	private void giveGifts(ServerWorld world, VillagerEntity villager, LivingEntity recipient) {
		villager.forEachGiftedItem(
				world,
				getGiftLootTable(villager),
				(w, stack) -> TargetUtil.give(villager, stack, recipient.getEntityPos())
		);
	}

	private static RegistryKey<LootTable> getGiftLootTable(VillagerEntity villager) {
		if (villager.isBaby()) {
			return LootTables.HERO_OF_THE_VILLAGE_BABY_GIFT_GAMEPLAY;
		}

		Optional<RegistryKey<VillagerProfession>> profession = villager.getVillagerData().profession().getKey();
		return profession.isEmpty()
				? LootTables.HERO_OF_THE_VILLAGE_UNEMPLOYED_GIFT_GAMEPLAY
				: GIFTS.getOrDefault(profession.get(), LootTables.HERO_OF_THE_VILLAGE_UNEMPLOYED_GIFT_GAMEPLAY);
	}

	private boolean isNearestPlayerHero(VillagerEntity villager) {
		return getNearestPlayerIfHero(villager).isPresent();
	}

	private Optional<PlayerEntity> getNearestPlayerIfHero(VillagerEntity villager) {
		return villager.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
				.filter(this::isHero);
	}

	private boolean isHero(PlayerEntity player) {
		return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
	}

	private boolean isCloseEnough(VillagerEntity villager, PlayerEntity player) {
		return villager.getBlockPos().isWithinDistance(player.getBlockPos(), MAX_DISTANCE);
	}

	private static int getNextGiftDelay(ServerWorld world) {
		return DEFAULT_DURATION + world.random.nextInt(MAX_EXTRA_GIFT_DELAY);
	}
}

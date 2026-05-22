package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Фабричный класс задачи мозга, организующей игру взрослого жителя с детьми-жителями.
 * Выбирает наименее популярного ребёнка для взаимодействия или бродит рядом с уже играющим.
 */
public class PlayWithVillagerBabiesTask {

	private static final int HORIZONTAL_RANGE = 20;
	private static final int VERTICAL_RANGE = 8;
	private static final float WALK_SPEED = 0.6F;
	private static final int MAX_BABY_INTERACTION_COUNT = 5;
	private static final int RUN_CHANCE = 10;

	public static Task<PathAwareEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(MemoryModuleType.VISIBLE_VILLAGER_BABIES),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.INTERACTION_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (visibleVillagerBabies, walkTarget, lookTarget, interactionTarget) -> (world, entity, time) -> {
							                  if (world.getRandom().nextInt(RUN_CHANCE) != 0) {
								                  return false;
							                  }

							                  List<LivingEntity> babies = context.getValue(visibleVillagerBabies);
							                  Optional<LivingEntity> alreadyPlaying = babies
									                  .stream()
									                  .filter(baby -> isInteractionTargetOf(entity, baby))
									                  .findAny();

							                  if (alreadyPlaying.isPresent()) {
								                  for (int attempt = 0; attempt < RUN_CHANCE; attempt++) {
									                  Vec3d pos = FuzzyTargeting.find(entity, HORIZONTAL_RANGE, VERTICAL_RANGE);

									                  if (pos != null && world.isNearOccupiedPointOfInterest(BlockPos.ofFloored(pos))) {
										                  walkTarget.remember(new WalkTarget(pos, WALK_SPEED, 0));
										                  break;
									                  }
								                  }

								                  return true;
							                  }

							                  Optional<LivingEntity> leastPopular = getLeastPopularBabyInteractionTarget(babies);

							                  if (leastPopular.isPresent()) {
								                  setPlayTarget(interactionTarget, lookTarget, walkTarget, leastPopular.get());
								                  return true;
							                  }

							                  babies.stream()
							                        .findAny()
							                        .ifPresent(baby -> setPlayTarget(interactionTarget, lookTarget, walkTarget, baby));

							                  return true;
						                  }
				                  )
		);
	}

	private static void setPlayTarget(
			MemoryQueryResult<?, LivingEntity> interactionTarget,
			MemoryQueryResult<?, LookTarget> lookTarget,
			MemoryQueryResult<?, WalkTarget> walkTarget,
			LivingEntity baby
	) {
		interactionTarget.remember(baby);
		lookTarget.remember(new EntityLookTarget(baby, true));
		walkTarget.remember(new WalkTarget(new EntityLookTarget(baby, false), WALK_SPEED, 1));
	}

	private static Optional<LivingEntity> getLeastPopularBabyInteractionTarget(List<LivingEntity> babies) {
		Map<LivingEntity, Integer> counts = getBabyInteractionTargetCounts(babies);

		return counts.entrySet()
		             .stream()
		             .sorted(Comparator.comparingInt(Entry::getValue))
		             .filter(entry -> entry.getValue() > 0 && entry.getValue() <= MAX_BABY_INTERACTION_COUNT)
		             .map(Entry::getKey)
		             .findFirst();
	}

	private static Map<LivingEntity, Integer> getBabyInteractionTargetCounts(List<LivingEntity> babies) {
		Map<LivingEntity, Integer> counts = new HashMap<>();

		babies.stream()
		      .filter(PlayWithVillagerBabiesTask::hasInteractionTarget)
		      .forEach(baby -> counts.compute(
				      getInteractionTarget(baby),
				      (target, count) -> count == null ? 1 : count + 1
		      ));

		return counts;
	}

	private static LivingEntity getInteractionTarget(LivingEntity baby) {
		return baby.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
	}

	private static boolean hasInteractionTarget(LivingEntity baby) {
		return baby.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
	}

	private static boolean isInteractionTargetOf(LivingEntity entity, LivingEntity baby) {
		return baby.getBrain()
		           .getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET)
		           .filter(target -> target == entity)
		           .isPresent();
	}
}

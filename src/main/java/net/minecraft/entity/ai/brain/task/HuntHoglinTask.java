package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;

import java.util.List;

/**
 * Фабричный класс задачи мозга пиглина, инициирующей групповую охоту на ближайшего хоглина.
 * Не запускает охоту, если пиглин — детёныш или кто-то из группы уже охотился недавно.
 */
public class HuntHoglinTask {

	public static SingleTickTask<PiglinEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN),
						                  context.queryMemoryAbsent(MemoryModuleType.ANGRY_AT),
						                  context.queryMemoryAbsent(MemoryModuleType.HUNTED_RECENTLY),
						                  context.queryMemoryOptional(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
				                  )
				                  .apply(
						                  context,
						                  (nearestVisibleHuntableHoglin, angryAt, huntedRecently, nearestVisibleAdultPiglins) -> (world, entity, time) -> {
							                  boolean groupAlreadyHunting = context
									                  .<List<AbstractPiglinEntity>>getOptionalValue(nearestVisibleAdultPiglins)
									                  .map(piglins -> piglins.stream().anyMatch(HuntHoglinTask::hasHuntedRecently))
									                  .isPresent();

							                  if (entity.isBaby() || groupAlreadyHunting) {
								                  return false;
							                  }

							                  HoglinEntity hoglin = context.getValue(nearestVisibleHuntableHoglin);
							                  PiglinBrain.becomeAngryWith(world, entity, hoglin);
							                  PiglinBrain.rememberHunting(entity);
							                  PiglinBrain.angerAtCloserTargets(world, entity, hoglin);
							                  context.<List<AbstractPiglinEntity>>getOptionalValue(nearestVisibleAdultPiglins)
							                         .ifPresent(piglins -> piglins.forEach(PiglinBrain::rememberHunting));

							                  return true;
						                  }
				                  )
		);
	}

	private static boolean hasHuntedRecently(AbstractPiglinEntity piglin) {
		return piglin.getBrain().hasMemoryModule(MemoryModuleType.HUNTED_RECENTLY);
	}
}

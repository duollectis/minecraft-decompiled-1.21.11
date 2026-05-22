package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.util.math.GlobalPos;

/**
 * Фабричный класс задачи мозга, инициирующей встречу жителя с другим жителем на месте собраний.
 * Запускается случайно (1/{@code MEET_CHANCE}) при нахождении рядом с точкой встречи.
 */
public class MeetVillagerTask {

	private static final float WALK_SPEED = 0.3F;
	private static final double MEETING_RANGE = 4.0;
	private static final double TALK_DISTANCE_SQ = 32.0;
	private static final int MEET_CHANCE = 100;

	public static SingleTickTask<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.MEETING_POINT),
						                  context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS),
						                  context.queryMemoryAbsent(MemoryModuleType.INTERACTION_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (walkTarget, lookTarget, meetingPoint, visibleMobs, interactionTarget) -> (world, entity, time) -> {
							                  GlobalPos meeting = context.getValue(meetingPoint);
							                  LivingTargetCache mobs = context.getValue(visibleMobs);
							                  boolean shouldMeet = world.getRandom().nextInt(MEET_CHANCE) == 0
									                  && world.getRegistryKey() == meeting.dimension()
									                  && meeting.pos().isWithinDistance(entity.getEntityPos(), MEETING_RANGE)
									                  && mobs.anyMatch(target -> EntityType.VILLAGER.equals(target.getType()));

							                  if (!shouldMeet) {
								                  return false;
							                  }

							                  mobs.findFirst(target ->
									                  EntityType.VILLAGER.equals(target.getType())
											                  && target.squaredDistanceTo(entity) <= TALK_DISTANCE_SQ
							                  ).ifPresent(target -> {
								                  interactionTarget.remember(target);
								                  lookTarget.remember(new EntityLookTarget(target, true));
								                  walkTarget.remember(new WalkTarget(new EntityLookTarget(target, false), WALK_SPEED, 1));
							                  });

							                  return true;
						                  }
				                  )
		);
	}
}

package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code NearestPlayersSensor}.
 */
public class NearestPlayersSensor extends Sensor<LivingEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(
				MemoryModuleType.NEAREST_PLAYERS,
				MemoryModuleType.NEAREST_VISIBLE_PLAYER,
				MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
				MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYERS
		);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		List<PlayerEntity> list = world.getPlayers()
		                               .stream()
		                               .filter(EntityPredicates.EXCEPT_SPECTATOR)
		                               .filter(player -> entity.isInRange(player, this.getFollowRange(entity)))
		                               .sorted(Comparator.comparingDouble(entity::squaredDistanceTo))
		                               .collect(Collectors.toList());
		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.NEAREST_PLAYERS, list);
		List<PlayerEntity>
				list2 =
				list.stream().filter(player -> testTargetPredicate(world, entity, player)).collect(Collectors.toList());
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_PLAYER, list2.isEmpty() ? null : list2.get(0));
		List<PlayerEntity>
				list3 =
				list2.stream().filter(player -> testAttackableTargetPredicate(world, entity, player)).toList();
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYERS, list3);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, list3.isEmpty() ? null : list3.get(0));
	}

	protected double getFollowRange(LivingEntity entity) {
		return entity.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
	}
}

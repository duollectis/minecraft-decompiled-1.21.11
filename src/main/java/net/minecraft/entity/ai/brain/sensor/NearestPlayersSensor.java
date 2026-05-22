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
 * Сенсор обнаружения ближайших игроков в радиусе {@code FOLLOW_RANGE}.
 * Записывает в память всех игроков, видимых, атакуемых и ближайшего атакуемого.
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
		List<PlayerEntity> players = world.getPlayers()
				.stream()
				.filter(EntityPredicates.EXCEPT_SPECTATOR)
				.filter(player -> entity.isInRange(player, getFollowRange(entity)))
				.sorted(Comparator.comparingDouble(entity::squaredDistanceTo))
				.collect(Collectors.toList());

		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.NEAREST_PLAYERS, players);

		List<PlayerEntity> visiblePlayers = players.stream()
				.filter(player -> testTargetPredicate(world, entity, player))
				.collect(Collectors.toList());
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_PLAYER, visiblePlayers.isEmpty() ? null : visiblePlayers.get(0));

		List<PlayerEntity> targetablePlayers = visiblePlayers.stream()
				.filter(player -> testAttackableTargetPredicate(world, entity, player))
				.toList();
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYERS, targetablePlayers);
		brain.remember(
				MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
				targetablePlayers.isEmpty() ? null : targetablePlayers.get(0)
		);
	}

	protected double getFollowRange(LivingEntity entity) {
		return entity.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
	}
}

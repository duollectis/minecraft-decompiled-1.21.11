package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code TemptationsSensor}.
 */
public class TemptationsSensor extends Sensor<PathAwareEntity> {

	private static final TargetPredicate TEMPTER_PREDICATE = TargetPredicate.createNonAttackable().ignoreVisibility();
	private final BiPredicate<PathAwareEntity, ItemStack> predicate;

	public TemptationsSensor(Predicate<ItemStack> predicate) {
		this((entity, stack) -> predicate.test(stack));
	}

	public static TemptationsSensor breedingItem() {
		return new TemptationsSensor((entity, stack) -> entity instanceof AnimalEntity animalEntity
		                                                ? animalEntity.isBreedingItem(stack) : false);
	}

	private TemptationsSensor(BiPredicate<PathAwareEntity, ItemStack> predicate) {
		this.predicate = predicate;
	}

	protected void sense(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
		Brain<?> brain = pathAwareEntity.getBrain();
		TargetPredicate
				targetPredicate =
				TEMPTER_PREDICATE
						.copy()
						.setBaseMaxDistance((float) pathAwareEntity.getAttributeValue(EntityAttributes.TEMPT_RANGE));
		List<PlayerEntity> list = serverWorld.getPlayers()
		                                     .stream()
		                                     .filter(EntityPredicates.EXCEPT_SPECTATOR)
		                                     .filter(player -> targetPredicate.test(
				                                     serverWorld,
				                                     pathAwareEntity,
				                                     player
		                                     ))
		                                     .filter(player -> this.test(pathAwareEntity, player))
		                                     .filter(playerx -> !pathAwareEntity.hasPassenger(playerx))
		                                     .sorted(Comparator.comparingDouble(pathAwareEntity::squaredDistanceTo))
		                                     .collect(Collectors.toList());
		if (!list.isEmpty()) {
			PlayerEntity playerEntity = list.get(0);
			brain.remember(MemoryModuleType.TEMPTING_PLAYER, playerEntity);
		}
		else {
			brain.forget(MemoryModuleType.TEMPTING_PLAYER);
		}
	}

	private boolean test(PathAwareEntity entity, PlayerEntity player) {
		return this.test(entity, player.getMainHandStack()) || this.test(entity, player.getOffHandStack());
	}

	private boolean test(PathAwareEntity entity, ItemStack stack) {
		return this.predicate.test(entity, stack);
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
	}
}

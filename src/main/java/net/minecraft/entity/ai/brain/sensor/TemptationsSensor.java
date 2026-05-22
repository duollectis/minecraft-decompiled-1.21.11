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
 * Сенсор обнаружения игрока, приманивающего сущность едой или предметом размножения.
 * Проверяет оба слота руки игрока через {@code temptPredicate} и записывает
 * ближайшего приманивающего игрока в память {@code TEMPTING_PLAYER}.
 */
public class TemptationsSensor extends Sensor<PathAwareEntity> {

	private static final TargetPredicate TEMPTER_PREDICATE = TargetPredicate.createNonAttackable().ignoreVisibility();

	private final BiPredicate<PathAwareEntity, ItemStack> temptPredicate;

	public TemptationsSensor(Predicate<ItemStack> predicate) {
		this((entity, stack) -> predicate.test(stack));
	}

	public static TemptationsSensor breedingItem() {
		return new TemptationsSensor(
				(entity, stack) -> entity instanceof AnimalEntity animal && animal.isBreedingItem(stack)
		);
	}

	private TemptationsSensor(BiPredicate<PathAwareEntity, ItemStack> temptPredicate) {
		this.temptPredicate = temptPredicate;
	}

	@Override
	protected void sense(ServerWorld world, PathAwareEntity entity) {
		Brain<?> brain = entity.getBrain();
		TargetPredicate rangedPredicate = TEMPTER_PREDICATE.copy()
				.setBaseMaxDistance((float) entity.getAttributeValue(EntityAttributes.TEMPT_RANGE));

		List<PlayerEntity> tempters = world.getPlayers()
				.stream()
				.filter(EntityPredicates.EXCEPT_SPECTATOR)
				.filter(player -> rangedPredicate.test(world, entity, player))
				.filter(player -> isTemptedBy(entity, player))
				.filter(player -> !entity.hasPassenger(player))
				.sorted(Comparator.comparingDouble(entity::squaredDistanceTo))
				.collect(Collectors.toList());

		if (tempters.isEmpty()) {
			brain.forget(MemoryModuleType.TEMPTING_PLAYER);
		} else {
			brain.remember(MemoryModuleType.TEMPTING_PLAYER, tempters.get(0));
		}
	}

	private boolean isTemptedBy(PathAwareEntity entity, PlayerEntity player) {
		return temptPredicate.test(entity, player.getMainHandStack())
				|| temptPredicate.test(entity, player.getOffHandStack());
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
	}
}

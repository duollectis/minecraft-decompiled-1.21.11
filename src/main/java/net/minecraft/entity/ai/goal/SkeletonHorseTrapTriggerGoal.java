package net.minecraft.entity.ai.goal;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import org.jspecify.annotations.Nullable;

/**
 * Цель-ловушка скелетной лошади: при приближении игрока на 10 блоков
 * вызывает молнию и спавнит трёх дополнительных скелетов-всадников.
 */
public class SkeletonHorseTrapTriggerGoal extends Goal {

	private static final double TRIGGER_RADIUS = 10.0;
	private static final int EXTRA_RIDERS = 3;
	private static final int REGEN_DELAY = 60;
	private static final double SCATTER_SPREAD = 1.1485;

	private final SkeletonHorseEntity skeletonHorse;

	public SkeletonHorseTrapTriggerGoal(SkeletonHorseEntity skeletonHorse) {
		this.skeletonHorse = skeletonHorse;
	}

	@Override
	public boolean canStart() {
		return skeletonHorse.getEntityWorld()
			.isPlayerInRange(skeletonHorse.getX(), skeletonHorse.getY(), skeletonHorse.getZ(), TRIGGER_RADIUS);
	}

	@Override
	public void tick() {
		ServerWorld serverWorld = (ServerWorld) skeletonHorse.getEntityWorld();
		LocalDifficulty difficulty = serverWorld.getLocalDifficulty(skeletonHorse.getBlockPos());

		skeletonHorse.setTrapped(false);
		skeletonHorse.setTame(true);
		skeletonHorse.setBreedingAge(0);

		LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(serverWorld, SpawnReason.TRIGGERED);

		if (lightning == null) {
			return;
		}

		lightning.refreshPositionAfterTeleport(skeletonHorse.getX(), skeletonHorse.getY(), skeletonHorse.getZ());
		lightning.setCosmetic(true);
		serverWorld.spawnEntity(lightning);

		SkeletonEntity mainRider = getSkeleton(difficulty, skeletonHorse);

		if (mainRider == null) {
			return;
		}

		mainRider.startRiding(skeletonHorse);
		serverWorld.spawnEntityAndPassengers(mainRider);

		for (int i = 0; i < EXTRA_RIDERS; i++) {
			AbstractHorseEntity extraHorse = getHorse(difficulty);

			if (extraHorse == null) {
				continue;
			}

			SkeletonEntity extraRider = getSkeleton(difficulty, extraHorse);

			if (extraRider == null) {
				continue;
			}

			extraRider.startRiding(extraHorse);
			extraHorse.addVelocity(
				skeletonHorse.getRandom().nextTriangular(0.0, SCATTER_SPREAD),
				0.0,
				skeletonHorse.getRandom().nextTriangular(0.0, SCATTER_SPREAD)
			);
			serverWorld.spawnEntityAndPassengers(extraHorse);
		}
	}

	private @Nullable AbstractHorseEntity getHorse(LocalDifficulty difficulty) {
		SkeletonHorseEntity horse = EntityType.SKELETON_HORSE.create(skeletonHorse.getEntityWorld(), SpawnReason.TRIGGERED);

		if (horse == null) {
			return null;
		}

		horse.initialize((ServerWorld) skeletonHorse.getEntityWorld(), difficulty, SpawnReason.TRIGGERED, null);
		horse.setPosition(skeletonHorse.getX(), skeletonHorse.getY(), skeletonHorse.getZ());
		horse.timeUntilRegen = REGEN_DELAY;
		horse.setPersistent();
		horse.setTame(true);
		horse.setBreedingAge(0);
		return horse;
	}

	private @Nullable SkeletonEntity getSkeleton(LocalDifficulty difficulty, AbstractHorseEntity vehicle) {
		SkeletonEntity skeleton = EntityType.SKELETON.create(vehicle.getEntityWorld(), SpawnReason.TRIGGERED);

		if (skeleton == null) {
			return null;
		}

		skeleton.initialize((ServerWorld) vehicle.getEntityWorld(), difficulty, SpawnReason.TRIGGERED, null);
		skeleton.setPosition(vehicle.getX(), vehicle.getY(), vehicle.getZ());
		skeleton.timeUntilRegen = REGEN_DELAY;
		skeleton.setPersistent();

		if (skeleton.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
			skeleton.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		}

		enchantEquipment(skeleton, EquipmentSlot.MAINHAND, difficulty);
		enchantEquipment(skeleton, EquipmentSlot.HEAD, difficulty);
		return skeleton;
	}

	private void enchantEquipment(SkeletonEntity rider, EquipmentSlot slot, LocalDifficulty difficulty) {
		ItemStack stack = rider.getEquippedStack(slot);
		stack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		EnchantmentHelper.applyEnchantmentProvider(
			stack,
			rider.getEntityWorld().getRegistryManager(),
			EnchantmentProviders.MOB_SPAWN_EQUIPMENT,
			difficulty,
			rider.getRandom()
		);
		rider.equipStack(slot, stack);
	}
}

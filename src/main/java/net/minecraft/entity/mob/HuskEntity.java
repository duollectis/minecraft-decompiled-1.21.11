package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Хаск — пустынный вариант зомби. Не горит на солнце. При ударе без оружия накладывает
 * эффект голода на цель. В воде превращается в обычного зомби. Может ехать верхом на верблюде-хаске.
 */
public class HuskEntity extends ZombieEntity {

	public HuskEntity(EntityType<? extends HuskEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HUSK_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ENTITY_HUSK_STEP;
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		boolean attacked = super.tryAttack(world, target);

		if (attacked && getMainHandStack().isEmpty() && target instanceof LivingEntity livingTarget) {
			float localDifficulty = world.getLocalDifficulty(getBlockPos()).getLocalDifficulty();
			livingTarget.addStatusEffect(
					new StatusEffectInstance(StatusEffects.HUNGER, 140 * (int) localDifficulty),
					this
			);
		}

		return attacked;
	}

	@Override
	protected boolean canConvertInWater() {
		return true;
	}

	@Override
	protected void convertInWater(ServerWorld world) {
		convertTo(world, EntityType.ZOMBIE);
		if (!isSilent()) {
			world.syncWorldEvent(null, 1041, getBlockPos(), 0);
		}
	}

	/**
	 * При естественном спавне с вероятностью 10% создаёт верблюда-хаска и сажает хаска верхом.
	 * Также спавнит иссохшего (Parched) как второго пассажира верблюда.
	 */
	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		entityData = super.initialize(world, difficulty, spawnReason, entityData);
		float clampedDifficulty = difficulty.getClampedLocalDifficulty();

		if (spawnReason != SpawnReason.CONVERSION) {
			setCanPickUpLoot(random.nextFloat() < 0.55F * clampedDifficulty);
		}

		if (entityData != null) {
			entityData = new HuskData((ZombieEntity.ZombieData) entityData);
			((HuskData) entityData).unnatural = spawnReason != SpawnReason.NATURAL;
		}

		if (entityData instanceof HuskData huskData && !huskData.unnatural) {
			BlockPos pos = getBlockPos();

			if (world.isSpaceEmpty(EntityType.CAMEL_HUSK.getSpawnBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
				huskData.unnatural = true;

				if (random.nextFloat() < 0.1F) {
					equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
					CamelHuskEntity camelHusk = EntityType.CAMEL_HUSK.create(getEntityWorld(), SpawnReason.NATURAL);

					if (camelHusk != null) {
						camelHusk.setPosition(getX(), getY(), getZ());
						camelHusk.initialize(world, difficulty, spawnReason, null);
						startRiding(camelHusk, true, true);
						world.spawnEntity(camelHusk);

						ParchedEntity parched = EntityType.PARCHED.create(getEntityWorld(), SpawnReason.NATURAL);

						if (parched != null) {
							parched.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
							parched.initialize(world, difficulty, spawnReason, null);
							parched.startRiding(camelHusk, false, false);
							world.spawnEntityAndPassengers(parched);
						}
					}
				}
			}
		}

		return entityData;
	}

	public static class HuskData extends ZombieEntity.ZombieData {

		public boolean unnatural = false;

		public HuskData(ZombieEntity.ZombieData data) {
			super(data.baby, data.tryChickenJockey);
		}
	}
}

package net.minecraft.entity.mob;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Бродяга — ледяной вариант скелета, стреляющий стрелами замедления.
 */
public class StrayEntity extends AbstractSkeletonEntity {

	public StrayEntity(EntityType<? extends StrayEntity> entityType, World world) {
		super(entityType, world);
	}

	public static boolean canSpawn(
			EntityType<StrayEntity> type,
			ServerWorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		BlockPos skyCheckPos = pos.up();
		while (world.getBlockState(skyCheckPos).isOf(Blocks.POWDER_SNOW)) {
			skyCheckPos = skyCheckPos.up();
		}

		return HostileEntity.canSpawnInDark(type, world, spawnReason, pos, random)
				&& (SpawnReason.isAnySpawner(spawnReason) || world.isSkyVisible(skyCheckPos.down()));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_STRAY_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_STRAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_STRAY_DEATH;
	}

	@Override
	SoundEvent getStepSound() {
		return SoundEvents.ENTITY_STRAY_STEP;
	}

	private static final int SLOWNESS_DURATION = 600;

	@Override
	protected PersistentProjectileEntity createArrowProjectile(
			ItemStack arrow,
			float damageModifier,
			@Nullable ItemStack shotFrom
	) {
		PersistentProjectileEntity projectile = super.createArrowProjectile(arrow, damageModifier, shotFrom);

		if (projectile instanceof ArrowEntity arrowEntity) {
			arrowEntity.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOWNESS_DURATION));
		}

		return projectile;
	}
}

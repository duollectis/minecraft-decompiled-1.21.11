package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.fluid.Fluid;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * Магмовый куб — огненный вариант слизня из Нижнего мира.
 */
public class MagmaCubeEntity extends SlimeEntity {

	public MagmaCubeEntity(EntityType<? extends MagmaCubeEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createMagmaCubeAttributes() {
		return HostileEntity.createHostileAttributes().add(EntityAttributes.MOVEMENT_SPEED, 0.2F);
	}

	public static boolean canMagmaCubeSpawn(
			EntityType<MagmaCubeEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getDifficulty() != Difficulty.PEACEFUL;
	}

	private static final float STRETCH_DECAY = 0.9F;
	private static final float JUMP_VELOCITY_SIZE_FACTOR = 0.1F;
	private static final float LAVA_SWIM_BASE_VELOCITY = 0.22F;
	private static final float LAVA_SWIM_SIZE_FACTOR = 0.05F;
	private static final int ARMOR_PER_SIZE = 3;

	@Override
	public void setSize(int size, boolean heal) {
		super.setSize(size, heal);
		getAttributeInstance(EntityAttributes.ARMOR).setBaseValue(size * ARMOR_PER_SIZE);
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	@Override
	protected ParticleEffect getParticles() {
		return ParticleTypes.FLAME;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	protected int getTicksUntilNextJump() {
		return super.getTicksUntilNextJump() * 4;
	}

	@Override
	protected void updateStretch() {
		targetStretch *= STRETCH_DECAY;
	}

	@Override
	public void jump() {
		Vec3d velocity = getVelocity();
		float sizeBonus = getSize() * JUMP_VELOCITY_SIZE_FACTOR;
		setVelocity(velocity.x, getJumpVelocity() + sizeBonus, velocity.z);
		velocityDirty = true;
	}

	@Override
	protected void swimUpward(TagKey<Fluid> fluid) {
		if (fluid != FluidTags.LAVA) {
			super.swimUpward(fluid);
			return;
		}

		Vec3d velocity = getVelocity();
		setVelocity(velocity.x, LAVA_SWIM_BASE_VELOCITY + getSize() * LAVA_SWIM_SIZE_FACTOR, velocity.z);
		velocityDirty = true;
	}

	@Override
	protected boolean canAttack() {
		return canActVoluntarily();
	}

	@Override
	protected float getDamageAmount() {
		return super.getDamageAmount() + 2.0F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isSmall() ? SoundEvents.ENTITY_MAGMA_CUBE_HURT_SMALL : SoundEvents.ENTITY_MAGMA_CUBE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isSmall() ? SoundEvents.ENTITY_MAGMA_CUBE_DEATH_SMALL : SoundEvents.ENTITY_MAGMA_CUBE_DEATH;
	}

	@Override
	protected SoundEvent getSquishSound() {
		return isSmall() ? SoundEvents.ENTITY_MAGMA_CUBE_SQUISH_SMALL : SoundEvents.ENTITY_MAGMA_CUBE_SQUISH;
	}

	@Override
	protected SoundEvent getJumpSound() {
		return SoundEvents.ENTITY_MAGMA_CUBE_JUMP;
	}
}

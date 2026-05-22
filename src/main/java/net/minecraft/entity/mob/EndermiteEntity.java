package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * Эндермит — маленький враждебный моб, появляющийся при телепортации.
 */
public class EndermiteEntity extends HostileEntity {

	private static final int DESPAWN_TIME = 2400;
	private int lifeTime;

	public EndermiteEntity(EntityType<? extends EndermiteEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 3;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(1, new PowderSnowJumpGoal(this, getEntityWorld()));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public static DefaultAttributeContainer.Builder createEndermiteAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 8.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 2.0);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ENDERMITE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ENDERMITE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ENDERMITE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_ENDERMITE_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		lifeTime = view.getInt("Lifetime", 0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Lifetime", lifeTime);
	}

	@Override
	public void tick() {
		bodyYaw = getYaw();
		super.tick();
	}

	@Override
	public void setBodyYaw(float bodyYaw) {
		setYaw(bodyYaw);
		super.setBodyYaw(bodyYaw);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getEntityWorld().isClient()) {
			for (int particleIndex = 0; particleIndex < 2; particleIndex++) {
				getEntityWorld()
					.addParticleClient(
						ParticleTypes.PORTAL,
						getParticleX(0.5),
						getRandomBodyY(),
						getParticleZ(0.5),
						(random.nextDouble() - 0.5) * 2.0,
						-random.nextDouble(),
						(random.nextDouble() - 0.5) * 2.0
					);
			}
		}
		else {
			if (!isPersistent()) {
				lifeTime++;
			}

			if (lifeTime >= DESPAWN_TIME) {
				discard();
			}
		}
	}

	public static boolean canSpawn(
			EntityType<EndermiteEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (!canSpawnIgnoreLightLevel(type, world, spawnReason, pos, random)) {
			return false;
		}

		if (SpawnReason.isAnySpawner(spawnReason)) {
			return true;
		}

		PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5.0, true);
		return nearestPlayer == null;
	}
}

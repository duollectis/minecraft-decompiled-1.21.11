package net.minecraft.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Равагер — мощный рейдовый моб. При получении удара с шансом 50% оглушается на {@code STUN_TICKS} тиков,
 * после чего издаёт рёв, отбрасывающий всех существ в радиусе 4 блоков.
 * Ломает листья при движении. Иммунен к жидкостям при спавне.
 */
public class RavagerEntity extends RaiderEntity {

	private static final Predicate<Entity>
			CAN_KNOCK_BACK_WITH_ROAR =
			entity -> !(entity instanceof RavagerEntity) && entity.isAlive();
	private static final Predicate<Entity>
			CAN_KNOCK_BACK_WITH_ROAR_NO_MOB_GRIEFING =
			entity -> CAN_KNOCK_BACK_WITH_ROAR.test(entity)
					&& !entity.getType().equals(EntityType.ARMOR_STAND);
	private static final Predicate<LivingEntity>
			CAN_KNOCK_BACK_WITH_ROAR_ON_CLIENT =
			entity -> !(entity instanceof RavagerEntity)
					&& entity.isAlive()
					&& entity.isLogicalSideForUpdatingMovement();
	private static final double BASE_MOVEMENT_SPEED = 0.3;
	private static final double CHASING_MOVEMENT_SPEED = 0.35;
	private static final int STUNNED_PARTICLE_COLOR = 8356754;
	private static final float STUNNED_PARTICLE_BLUE = 0.57254905F;
	private static final float STUNNED_PARTICLE_GREEN = 0.5137255F;
	private static final float STUNNED_PARTICLE_RED = 0.49803922F;
	public static final int ROAR_TRIGGER_TICK = 10;
	public static final int STUN_TICKS = 40;
	private static final int ROAR_DELAY_AFTER_STUN = 20;
	private static final byte ATTACK_STATUS = 4;
	private static final byte STUN_STATUS = 39;
	private static final byte ROAR_STATUS = 69;
	private int attackTick;
	private int stunTick;
	private int roarTick;

	public RavagerEntity(EntityType<? extends RavagerEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 20;
		setPathfindingPenalty(PathNodeType.LEAVES, 0.0F);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(4, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.4));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(ROAR_TRIGGER_TICK, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
		targetSelector.add(2, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(
				4,
				new ActiveTargetGoal<>(this, MerchantEntity.class, true, (entity, world) -> !entity.isBaby())
		);
		targetSelector.add(4, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	protected void updateGoalControls() {
		boolean canControl = !(getControllingPassenger() instanceof MobEntity)
				|| getControllingPassenger().getType().isIn(EntityTypeTags.RAIDERS);
		boolean notInBoat = !(getVehicle() instanceof AbstractBoatEntity);
		goalSelector.setControlEnabled(Goal.Control.MOVE, canControl);
		goalSelector.setControlEnabled(Goal.Control.JUMP, canControl && notInBoat);
		goalSelector.setControlEnabled(Goal.Control.LOOK, canControl);
		goalSelector.setControlEnabled(Goal.Control.TARGET, canControl);
	}

	public static DefaultAttributeContainer.Builder createRavagerAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 100.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
		                    .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.75)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 12.0)
		                    .add(EntityAttributes.ATTACK_KNOCKBACK, 1.5)
		                    .add(EntityAttributes.FOLLOW_RANGE, 32.0)
		                    .add(EntityAttributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("AttackTick", attackTick);
		view.putInt("StunTick", stunTick);
		view.putInt("RoarTick", roarTick);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		attackTick = view.getInt("AttackTick", 0);
		stunTick = view.getInt("StunTick", 0);
		roarTick = view.getInt("RoarTick", 0);
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_RAVAGER_CELEBRATE;
	}

	@Override
	public int getMaxHeadRotation() {
		return 45;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (!isAlive()) {
			return;
		}

		if (isImmobile()) {
			getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.0);
		} else {
			double targetSpeed = getTarget() != null ? CHASING_MOVEMENT_SPEED : BASE_MOVEMENT_SPEED;
			double currentSpeed = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).getBaseValue();
			getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(MathHelper.lerp(0.1, currentSpeed, targetSpeed));
		}

		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& horizontalCollision
				&& serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
		) {
			boolean brokeLeaves = false;
			Box box = getBoundingBox().expand(0.2);

			for (BlockPos blockPos : BlockPos.iterate(
					MathHelper.floor(box.minX),
					MathHelper.floor(box.minY),
					MathHelper.floor(box.minZ),
					MathHelper.floor(box.maxX),
					MathHelper.floor(box.maxY),
					MathHelper.floor(box.maxZ)
			)) {
				BlockState blockState = serverWorld.getBlockState(blockPos);
				if (blockState.getBlock() instanceof LeavesBlock) {
					brokeLeaves = serverWorld.breakBlock(blockPos, true, this) || brokeLeaves;
				}
			}

			if (!brokeLeaves && isOnGround()) {
				jump();
			}
		}

		if (roarTick > 0) {
			roarTick--;
			if (roarTick == ROAR_TRIGGER_TICK) {
				roar();
			}
		}

		if (attackTick > 0) {
			attackTick--;
		}

		if (stunTick > 0) {
			stunTick--;
			spawnStunnedParticles();
			if (stunTick == 0) {
				playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0F, 1.0F);
				roarTick = ROAR_DELAY_AFTER_STUN;
			}
		}
	}

	private void spawnStunnedParticles() {
		if (random.nextInt(6) != 0) {
			return;
		}

		double px = getX() - getWidth() * Math.sin(bodyYaw * (float) (Math.PI / 180.0)) + (random.nextDouble() * 0.6 - BASE_MOVEMENT_SPEED);
		double py = getY() + getHeight() - 0.3;
		double pz = getZ() + getWidth() * Math.cos(bodyYaw * (float) (Math.PI / 180.0)) + (random.nextDouble() * 0.6 - BASE_MOVEMENT_SPEED);
		getEntityWorld().addParticleClient(
				TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, STUNNED_PARTICLE_RED, STUNNED_PARTICLE_GREEN, STUNNED_PARTICLE_BLUE),
				px, py, pz, 0.0, 0.0, 0.0
		);
	}

	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || attackTick > 0 || stunTick > 0 || roarTick > 0;
	}

	@Override
	public boolean canSee(Entity entity) {
		return stunTick <= 0 && roarTick <= 0 && super.canSee(entity);
	}

	@Override
	protected void knockback(LivingEntity target) {
		if (roarTick != 0) {
			return;
		}

		if (random.nextDouble() < 0.5) {
			stunTick = STUN_TICKS;
			playSound(SoundEvents.ENTITY_RAVAGER_STUNNED, 1.0F, 1.0F);
			getEntityWorld().sendEntityStatus(this, STUN_STATUS);
			target.pushAwayFrom(this);
		} else {
			knockBack(target);
		}

		target.knockedBack = true;
	}

	private void roar() {
		if (!isAlive()) {
			return;
		}

		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Predicate<Entity> predicate = serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				? CAN_KNOCK_BACK_WITH_ROAR
				: CAN_KNOCK_BACK_WITH_ROAR_NO_MOB_GRIEFING;

		for (LivingEntity nearby : getEntityWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(4.0), predicate)) {
			if (!(nearby instanceof IllagerEntity)) {
				nearby.damage(serverWorld, getDamageSources().mobAttack(this), 6.0F);
			}

			if (!(nearby instanceof PlayerEntity)) {
				knockBack(nearby);
			}
		}

		emitGameEvent(GameEvent.ENTITY_ACTION);
		serverWorld.sendEntityStatus(this, ROAR_STATUS);
	}

	private void roarKnockBackOnClient() {
		for (LivingEntity nearby : getEntityWorld().getEntitiesByClass(
				LivingEntity.class,
				getBoundingBox().expand(4.0),
				CAN_KNOCK_BACK_WITH_ROAR_ON_CLIENT
		)) {
			knockBack(nearby);
		}
	}

	private void knockBack(Entity entity) {
		double dx = entity.getX() - getX();
		double dz = entity.getZ() - getZ();
		double distSq = Math.max(dx * dx + dz * dz, 0.001);
		entity.addVelocity(dx / distSq * 4.0, Entity.MOVEMENT_SPEED_THRESHOLD, dz / distSq * 4.0);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == ATTACK_STATUS) {
			attackTick = ROAR_TRIGGER_TICK;
			playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0F, 1.0F);
		} else if (status == STUN_STATUS) {
			stunTick = STUN_TICKS;
		} else if (status == ROAR_STATUS) {
			addRoarParticlesOnClient();
			roarKnockBackOnClient();
		}

		super.handleStatus(status);
	}

	private void addRoarParticlesOnClient() {
		Vec3d center = getBoundingBox().getCenter();

		for (int particleIndex = 0; particleIndex < STUN_TICKS; particleIndex++) {
			double vx = random.nextGaussian() * 0.2;
			double vy = random.nextGaussian() * 0.2;
			double vz = random.nextGaussian() * 0.2;
			getEntityWorld().addParticleClient(ParticleTypes.POOF, center.x, center.y, center.z, vx, vy, vz);
		}
	}

	public int getAttackTick() {
		return attackTick;
	}

	public int getStunTick() {
		return stunTick;
	}

	public int getRoarTick() {
		return roarTick;
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		attackTick = ROAR_TRIGGER_TICK;
		world.sendEntityStatus(this, ATTACK_STATUS);
		playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0F, 1.0F);
		return super.tryAttack(world, target);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_RAVAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_RAVAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_RAVAGER_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return !world.containsFluid(getBoundingBox());
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
	}

	@Override
	public boolean canLead() {
		return false;
	}

	@Override
	protected Box getAttackBox(double attackRange) {
		Box box = super.getAttackBox(attackRange);
		return box.contract(0.05, 0.0, 0.05);
	}
}

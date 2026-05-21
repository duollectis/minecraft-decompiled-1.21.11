package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * {@code VexEntity}.
 */
public class VexEntity extends HostileEntity implements Ownable {

	public static final float WING_FLAP_ANGULAR_VELOCITY = 45.836624F;
	public static final int WING_FLAP_TICKS = MathHelper.ceil((float) (Math.PI * 5.0 / 4.0));
	protected static final TrackedData<Byte>
			VEX_FLAGS =
			DataTracker.registerData(VexEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final int CHARGING_FLAG = 1;
	private @Nullable LazyEntityReference<MobEntity> owner;
	private @Nullable BlockPos bounds;
	private boolean alive;
	private int lifeTicks;

	public VexEntity(EntityType<? extends VexEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new VexEntity.VexMoveControl(this);
		this.experiencePoints = 3;
	}

	@Override
	public boolean isFlappingWings() {
		return this.age % WING_FLAP_TICKS == 0;
	}

	@Override
	protected boolean shouldTickBlockCollision() {
		return !this.isRemoved();
	}

	@Override
	public void tick() {
		this.noClip = true;
		super.tick();
		this.noClip = false;
		this.setNoGravity(true);
		if (this.alive && --this.lifeTicks <= 0) {
			this.lifeTicks = 20;
			this.serverDamage(this.getDamageSources().starve(), 1.0F);
		}
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(4, new VexEntity.ChargeTargetGoal());
		this.goalSelector.add(8, new VexEntity.LookAtTargetGoal());
		this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
		this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
		this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		this.targetSelector.add(2, new VexEntity.TrackOwnerTargetGoal(this));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public static DefaultAttributeContainer.Builder createVexAttributes() {
		return HostileEntity
				.createHostileAttributes()
				.add(EntityAttributes.MAX_HEALTH, 14.0)
				.add(EntityAttributes.ATTACK_DAMAGE, 4.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VEX_FLAGS, (byte) 0);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.bounds = view.<BlockPos>read("bound_pos", BlockPos.CODEC).orElse(null);
		view.getOptionalInt("life_ticks").ifPresentOrElse(this::setLifeTicks, () -> this.alive = false);
		this.owner = LazyEntityReference.fromData(view, "owner");
	}

	@Override
	public void copyFrom(Entity original) {
		super.copyFrom(original);
		if (original instanceof VexEntity vexEntity) {
			this.owner = vexEntity.owner;
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("bound_pos", BlockPos.CODEC, this.bounds);
		if (this.alive) {
			view.putInt("life_ticks", this.lifeTicks);
		}

		LazyEntityReference.writeData(this.owner, view, "owner");
	}

	public @Nullable MobEntity getOwner() {
		return LazyEntityReference.resolve(this.owner, this.getEntityWorld(), MobEntity.class);
	}

	public @Nullable BlockPos getBounds() {
		return this.bounds;
	}

	public void setBounds(@Nullable BlockPos bounds) {
		this.bounds = bounds;
	}

	private boolean areFlagsSet(int mask) {
		int i = this.dataTracker.get(VEX_FLAGS);
		return (i & mask) != 0;
	}

	private void setVexFlag(int mask, boolean value) {
		int i = this.dataTracker.get(VEX_FLAGS);
		if (value) {
			i |= mask;
		}
		else {
			i &= ~mask;
		}

		this.dataTracker.set(VEX_FLAGS, (byte) (i & 0xFF));
	}

	public boolean isCharging() {
		return this.areFlagsSet(1);
	}

	public void setCharging(boolean charging) {
		this.setVexFlag(1, charging);
	}

	public void setOwner(MobEntity owner) {
		this.owner = LazyEntityReference.of(owner);
	}

	public void setLifeTicks(int lifeTicks) {
		this.alive = true;
		this.lifeTicks = lifeTicks;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VEX_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VEX_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_VEX_HURT;
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		this.initEquipment(random, difficulty);
		this.updateEnchantments(world, random, difficulty);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	/**
	 * {@code ChargeTargetGoal}.
	 */
	class ChargeTargetGoal extends Goal {

		public ChargeTargetGoal() {
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			LivingEntity livingEntity = VexEntity.this.getTarget();
			return livingEntity != null
					       && livingEntity.isAlive()
					       && !VexEntity.this.getMoveControl().isMoving()
					       && VexEntity.this.random.nextInt(toGoalTicks(7)) == 0
			       ? VexEntity.this.squaredDistanceTo(livingEntity) > 4.0
			       : false;
		}

		@Override
		public boolean shouldContinue() {
			return VexEntity.this.getMoveControl().isMoving()
					&& VexEntity.this.isCharging()
					&& VexEntity.this.getTarget() != null
					&& VexEntity.this.getTarget().isAlive();
		}

		@Override
		public void start() {
			LivingEntity livingEntity = VexEntity.this.getTarget();
			if (livingEntity != null) {
				Vec3d vec3d = livingEntity.getEyePos();
				VexEntity.this.moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
			}

			VexEntity.this.setCharging(true);
			VexEntity.this.playSound(SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 1.0F);
		}

		@Override
		public void stop() {
			VexEntity.this.setCharging(false);
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity livingEntity = VexEntity.this.getTarget();
			if (livingEntity != null) {
				if (VexEntity.this.getBoundingBox().intersects(livingEntity.getBoundingBox())) {
					VexEntity.this.tryAttack(castToServerWorld(VexEntity.this.getEntityWorld()), livingEntity);
					VexEntity.this.setCharging(false);
				}
				else {
					double d = VexEntity.this.squaredDistanceTo(livingEntity);
					if (d < 9.0) {
						Vec3d vec3d = livingEntity.getEyePos();
						VexEntity.this.moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
					}
				}
			}
		}
	}

	/**
	 * {@code LookAtTargetGoal}.
	 */
	class LookAtTargetGoal extends Goal {

		public LookAtTargetGoal() {
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return !VexEntity.this.getMoveControl().isMoving() && VexEntity.this.random.nextInt(toGoalTicks(7)) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void tick() {
			BlockPos blockPos = VexEntity.this.getBounds();
			if (blockPos == null) {
				blockPos = VexEntity.this.getBlockPos();
			}

			for (int i = 0; i < 3; i++) {
				BlockPos blockPos2 = blockPos.add(
						VexEntity.this.random.nextInt(15) - 7,
						VexEntity.this.random.nextInt(11) - 5,
						VexEntity.this.random.nextInt(15) - 7
				);
				if (VexEntity.this.getEntityWorld().isAir(blockPos2)) {
					VexEntity.this.moveControl.moveTo(
							blockPos2.getX() + 0.5,
							blockPos2.getY() + 0.5,
							blockPos2.getZ() + 0.5,
							0.25
					);
					if (VexEntity.this.getTarget() == null) {
						VexEntity.this
								.getLookControl()
								.lookAt(
										blockPos2.getX() + 0.5,
										blockPos2.getY() + 0.5,
										blockPos2.getZ() + 0.5,
										180.0F,
										20.0F
								);
					}
					break;
				}
			}
		}
	}

	/**
	 * {@code TrackOwnerTargetGoal}.
	 */
	class TrackOwnerTargetGoal extends TrackTargetGoal {

		private final TargetPredicate
				targetPredicate =
				TargetPredicate.createNonAttackable().ignoreVisibility().ignoreDistanceScalingFactor();

		public TrackOwnerTargetGoal(final PathAwareEntity mob) {
			super(mob, false);
		}

		@Override
		public boolean canStart() {
			MobEntity mobEntity = VexEntity.this.getOwner();
			return mobEntity != null && mobEntity.getTarget() != null && this.canTrack(
					mobEntity.getTarget(),
					this.targetPredicate
			);
		}

		@Override
		public void start() {
			MobEntity mobEntity = VexEntity.this.getOwner();
			VexEntity.this.setTarget(mobEntity != null ? mobEntity.getTarget() : null);
			super.start();
		}
	}

	/**
	 * {@code VexMoveControl}.
	 */
	class VexMoveControl extends MoveControl {

		public VexMoveControl(final VexEntity owner) {
			super(owner);
		}

		@Override
		public void tick() {
			if (this.state == MoveControl.State.MOVE_TO) {
				Vec3d
						vec3d =
						new Vec3d(
								this.targetX - VexEntity.this.getX(),
								this.targetY - VexEntity.this.getY(),
								this.targetZ - VexEntity.this.getZ()
						);
				double d = vec3d.length();
				if (d < VexEntity.this.getBoundingBox().getAverageSideLength()) {
					this.state = MoveControl.State.WAIT;
					VexEntity.this.setVelocity(VexEntity.this.getVelocity().multiply(0.5));
				}
				else {
					VexEntity.this.setVelocity(VexEntity.this.getVelocity().add(vec3d.multiply(this.speed * 0.05 / d)));
					if (VexEntity.this.getTarget() == null) {
						Vec3d vec3d2 = VexEntity.this.getVelocity();
						VexEntity.this.setYaw(
								-((float) MathHelper.atan2(vec3d2.x, vec3d2.z)) * (180.0F / (float) Math.PI));
						VexEntity.this.bodyYaw = VexEntity.this.getYaw();
					}
					else {
						double e = VexEntity.this.getTarget().getX() - VexEntity.this.getX();
						double f = VexEntity.this.getTarget().getZ() - VexEntity.this.getZ();
						VexEntity.this.setYaw(-((float) MathHelper.atan2(e, f)) * (180.0F / (float) Math.PI));
						VexEntity.this.bodyYaw = VexEntity.this.getYaw();
					}
				}
			}
		}
	}
}

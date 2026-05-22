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
 * Вексы — летающие фамильяры эвокера, атакующие цели хозяина в ближнем бою.
 * Проходят сквозь блоки (noClip), не подвержены гравитации. Если задан
 * {@code lifeTicks}, каждые {@value #LIFE_TICK_INTERVAL} тиков получают урон
 * от голода, пока не погибнут.
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

	private static final int LIFE_TICK_INTERVAL = 20;

	public VexEntity(EntityType<? extends VexEntity> entityType, World world) {
		super(entityType, world);
		moveControl = new VexEntity.VexMoveControl(this);
		experiencePoints = 3;
	}

	@Override
	public boolean isFlappingWings() {
		return age % WING_FLAP_TICKS == 0;
	}

	@Override
	protected boolean shouldTickBlockCollision() {
		return !isRemoved();
	}

	@Override
	public void tick() {
		noClip = true;
		super.tick();
		noClip = false;
		setNoGravity(true);
		if (alive && --lifeTicks <= 0) {
			lifeTicks = LIFE_TICK_INTERVAL;
			serverDamage(getDamageSources().starve(), 1.0F);
		}
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(4, new VexEntity.ChargeTargetGoal());
		goalSelector.add(8, new VexEntity.LookAtTargetGoal());
		goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(2, new VexEntity.TrackOwnerTargetGoal(this));
		targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
		bounds = view.<BlockPos>read("bound_pos", BlockPos.CODEC).orElse(null);
		view.getOptionalInt("life_ticks").ifPresentOrElse(this::setLifeTicks, () -> alive = false);
		owner = LazyEntityReference.fromData(view, "owner");
	}

	@Override
	public void copyFrom(Entity original) {
		super.copyFrom(original);
		if (original instanceof VexEntity vexEntity) {
			owner = vexEntity.owner;
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("bound_pos", BlockPos.CODEC, bounds);
		if (alive) {
			view.putInt("life_ticks", lifeTicks);
		}

		LazyEntityReference.writeData(owner, view, "owner");
	}

	public @Nullable MobEntity getOwner() {
		return LazyEntityReference.resolve(owner, getEntityWorld(), MobEntity.class);
	}

	public @Nullable BlockPos getBounds() {
		return bounds;
	}

	public void setBounds(@Nullable BlockPos bounds) {
		this.bounds = bounds;
	}

	private boolean areFlagsSet(int mask) {
		int flags = dataTracker.get(VEX_FLAGS);
		return (flags & mask) != 0;
	}

	private void setVexFlag(int mask, boolean value) {
		int flags = dataTracker.get(VEX_FLAGS);
		if (value) {
			flags |= mask;
		}
		else {
			flags &= ~mask;
		}

		dataTracker.set(VEX_FLAGS, (byte) (flags & 0xFF));
	}

	public boolean isCharging() {
		return areFlagsSet(CHARGING_FLAG);
	}

	public void setCharging(boolean charging) {
		setVexFlag(CHARGING_FLAG, charging);
	}

	public void setOwner(MobEntity owner) {
		this.owner = LazyEntityReference.of(owner);
	}

	public void setLifeTicks(int lifeTicks) {
		alive = true;
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
		initEquipment(random, difficulty);
		updateEnchantments(world, random, difficulty);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	class ChargeTargetGoal extends Goal {

		public ChargeTargetGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = VexEntity.this.getTarget();
			if (target == null
					|| !target.isAlive()
					|| VexEntity.this.getMoveControl().isMoving()
					|| VexEntity.this.random.nextInt(toGoalTicks(7)) != 0
			) {
				return false;
			}

			return VexEntity.this.squaredDistanceTo(target) > 4.0;
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

	class LookAtTargetGoal extends Goal {

		public LookAtTargetGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
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

			for (int attempt = 0; attempt < 3; attempt++) {
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

	class VexMoveControl extends MoveControl {

		public VexMoveControl(final VexEntity owner) {
			super(owner);
		}

		@Override
		public void tick() {
			if (state != MoveControl.State.MOVE_TO) {
				return;
			}

			Vec3d delta = new Vec3d(
					this.targetX - VexEntity.this.getX(),
					this.targetY - VexEntity.this.getY(),
					this.targetZ - VexEntity.this.getZ()
			);
			double dist = delta.length();

			if (dist < VexEntity.this.getBoundingBox().getAverageSideLength()) {
				this.state = MoveControl.State.WAIT;
				VexEntity.this.setVelocity(VexEntity.this.getVelocity().multiply(0.5));
				return;
			}

			VexEntity.this.setVelocity(VexEntity.this.getVelocity().add(delta.multiply(this.speed * 0.05 / dist)));

			if (VexEntity.this.getTarget() == null) {
				Vec3d velocity = VexEntity.this.getVelocity();
				VexEntity.this.setYaw(-((float) MathHelper.atan2(velocity.x, velocity.z)) * (180.0F / (float) Math.PI));
				VexEntity.this.bodyYaw = VexEntity.this.getYaw();
			} else {
				double targetDx = VexEntity.this.getTarget().getX() - VexEntity.this.getX();
				double targetDz = VexEntity.this.getTarget().getZ() - VexEntity.this.getZ();
				VexEntity.this.setYaw(-((float) MathHelper.atan2(targetDx, targetDz)) * (180.0F / (float) Math.PI));
				VexEntity.this.bodyYaw = VexEntity.this.getYaw();
			}
		}
	}
}

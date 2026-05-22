package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Паук — моб, умеющий карабкаться по стенам.
 * Атакует только в темноте (яркость < 0.5). На Hard-сложности может спавниться с эффектом статуса.
 * Иммунен к яду. Может спавниться с наездником-скелетом (1% шанс).
 */
public class SpiderEntity extends HostileEntity {

	private static final TrackedData<Byte>
			SPIDER_FLAGS =
			DataTracker.registerData(SpiderEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final float EFFECT_SPAWN_CHANCE_MULTIPLIER = 0.1F;

	public SpiderEntity(EntityType<? extends SpiderEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(
			2,
			new FleeEntityGoal<>(
				this,
				ArmadilloEntity.class,
				6.0F,
				1.0,
				1.2,
				entity -> !((ArmadilloEntity) entity).isNotIdle()
			)
		);
		goalSelector.add(3, new PounceAtTargetGoal(this, 0.4F));
		goalSelector.add(4, new SpiderEntity.AttackGoal(this));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new SpiderEntity.TargetGoal<>(this, PlayerEntity.class));
		targetSelector.add(3, new SpiderEntity.TargetGoal<>(this, IronGolemEntity.class));
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new SpiderNavigation(this, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SPIDER_FLAGS, (byte) 0);
	}

	@Override
	public void tick() {
		super.tick();

		if (!getEntityWorld().isClient()) {
			setClimbingWall(horizontalCollision);
		}
	}

	public static DefaultAttributeContainer.Builder createSpiderAttributes() {
		return HostileEntity
				.createHostileAttributes()
				.add(EntityAttributes.MAX_HEALTH, 16.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SPIDER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SPIDER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean isClimbing() {
		return isClimbingWall();
	}

	@Override
	public void slowMovement(BlockState state, Vec3d multiplier) {
		if (!state.isOf(Blocks.COBWEB)) {
			super.slowMovement(state, multiplier);
		}
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return !effect.equals(StatusEffects.POISON) && super.canHaveStatusEffect(effect);
	}

	public boolean isClimbingWall() {
		return (dataTracker.get(SPIDER_FLAGS) & 1) != 0;
	}

	public void setClimbingWall(boolean climbing) {
		byte flags = dataTracker.get(SPIDER_FLAGS);
		flags = climbing
			? (byte) (flags | 1)
			: (byte) (flags & -2);

		dataTracker.set(SPIDER_FLAGS, flags);
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		entityData = super.initialize(world, difficulty, spawnReason, entityData);
		Random random = world.getRandom();

		if (random.nextInt(100) == 0) {
			SkeletonEntity skeleton = EntityType.SKELETON.create(getEntityWorld(), SpawnReason.JOCKEY);

			if (skeleton != null) {
				skeleton.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
				skeleton.initialize(world, difficulty, spawnReason, null);
				skeleton.startRiding(this, false, false);
			}
		}

		if (entityData == null) {
			entityData = new SpiderEntity.SpiderData();

			if (world.getDifficulty() == Difficulty.HARD
				&& random.nextFloat() < EFFECT_SPAWN_CHANCE_MULTIPLIER * difficulty.getClampedLocalDifficulty()
			) {
				((SpiderEntity.SpiderData) entityData).setEffect(random);
			}
		}

		if (entityData instanceof SpiderEntity.SpiderData spiderData) {
			RegistryEntry<StatusEffect> effect = spiderData.effect;

			if (effect != null) {
				addStatusEffect(new StatusEffectInstance(effect, -1));
			}
		}

		return entityData;
	}

	@Override
	public Vec3d getVehicleAttachmentPos(Entity vehicle) {
		return vehicle.getWidth() <= getWidth()
			? new Vec3d(0.0, 0.3125 * getScale(), 0.0)
			: super.getVehicleAttachmentPos(vehicle);
	}

	static class AttackGoal extends MeleeAttackGoal {

		public AttackGoal(SpiderEntity spider) {
			super(spider, 1.0, true);
		}

		@Override
		public boolean canStart() {
			return super.canStart() && !mob.hasPassengers();
		}

		@Override
		public boolean shouldContinue() {
			float brightness = mob.getBrightnessAtEyes();

			if (brightness >= 0.5F && mob.getRandom().nextInt(100) == 0) {
				mob.setTarget(null);
				return false;
			}

			return super.shouldContinue();
		}
	}

	public static class SpiderData implements EntityData {

		public @Nullable RegistryEntry<StatusEffect> effect;

		public void setEffect(Random random) {
			int roll = random.nextInt(5);

			if (roll <= 1) {
				effect = StatusEffects.SPEED;
			} else if (roll <= 2) {
				effect = StatusEffects.STRENGTH;
			} else if (roll <= 3) {
				effect = StatusEffects.REGENERATION;
			} else if (roll <= 4) {
				effect = StatusEffects.INVISIBILITY;
			}
		}
	}

	static class TargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {

		public TargetGoal(SpiderEntity spider, Class<T> targetEntityClass) {
			super(spider, targetEntityClass, true);
		}

		@Override
		public boolean canStart() {
			float brightness = mob.getBrightnessAtEyes();

			return brightness < 0.5F && super.canStart();
		}
	}
}

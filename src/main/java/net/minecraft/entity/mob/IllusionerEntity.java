package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Иллюзионист — иллагер дальнего боя с луком. Накладывает на себя невидимость
 * и создаёт 4 зеркальные копии. При атаке ослепляет цель на нормальной и сложной
 * сложности. Копии плавно смещаются при получении урона или каждые 1200 тиков.
 */
public class IllusionerEntity extends SpellcastingIllagerEntity implements RangedAttackMob {

	private static final int MIRROR_COPY_COUNT = 4;
	private static final int MIRROR_SPELL_TIMER_DURATION = 3;
	public static final int MIRROR_SPELL_DURATION = 3;
	private int mirrorSpellTimer;
	private final Vec3d[][] mirrorCopyOffsets;

	public IllusionerEntity(EntityType<? extends IllusionerEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
		mirrorCopyOffsets = new Vec3d[2][MIRROR_COPY_COUNT];

		for (int copyIndex = 0; copyIndex < MIRROR_COPY_COUNT; copyIndex++) {
			mirrorCopyOffsets[0][copyIndex] = Vec3d.ZERO;
			mirrorCopyOffsets[1][copyIndex] = Vec3d.ZERO;
		}
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new SpellcastingIllagerEntity.LookAtTargetGoal());
		goalSelector.add(3, new FleeEntityGoal<>(this, CreakingEntity.class, 8.0F, 1.0, 1.2));
		goalSelector.add(4, new IllusionerEntity.GiveInvisibilityGoal());
		goalSelector.add(5, new IllusionerEntity.BlindTargetGoal());
		goalSelector.add(6, new BowAttackGoal<>(this, 0.5, 20, 15.0F));
		goalSelector.add(8, new WanderAroundGoal(this, 0.6));
		goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(
				2,
				new ActiveTargetGoal<>(this, PlayerEntity.class, true).setMaxTimeWithoutVisibility(300)
		);
		targetSelector.add(
				3,
				new ActiveTargetGoal<>(this, MerchantEntity.class, false).setMaxTimeWithoutVisibility(300)
		);
		targetSelector.add(
				3,
				new ActiveTargetGoal<>(this, IronGolemEntity.class, false).setMaxTimeWithoutVisibility(300)
		);
	}

	public static DefaultAttributeContainer.Builder createIllusionerAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.5)
		                    .add(EntityAttributes.FOLLOW_RANGE, 18.0)
		                    .add(EntityAttributes.MAX_HEALTH, 32.0);
	}

	@Override
	public EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!getEntityWorld().isClient() || !isInvisible()) {
			return;
		}

		mirrorSpellTimer--;
		if (mirrorSpellTimer < 0) {
			mirrorSpellTimer = 0;
		}

		if (hurtTime == 1 || age % 1200 == 0) {
			mirrorSpellTimer = MIRROR_SPELL_TIMER_DURATION;

			for (int copyIndex = 0; copyIndex < MIRROR_COPY_COUNT; copyIndex++) {
				mirrorCopyOffsets[0][copyIndex] = mirrorCopyOffsets[1][copyIndex];
				mirrorCopyOffsets[1][copyIndex] = new Vec3d(
						(-6.0F + random.nextInt(13)) * 0.5,
						Math.max(0, random.nextInt(6) - 4),
						(-6.0F + random.nextInt(13)) * 0.5
				);
			}

			for (int particleIndex = 0; particleIndex < 16; particleIndex++) {
				getEntityWorld().addParticleClient(
						ParticleTypes.CLOUD,
						getParticleX(0.5),
						getRandomBodyY(),
						getBodyZ(0.5),
						0.0, 0.0, 0.0
				);
			}

			getEntityWorld().playSoundClient(
					getX(), getY(), getZ(),
					SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
					getSoundCategory(),
					1.0F, 1.0F, false
			);
		} else if (hurtTime == maxHurtTime - 1) {
			mirrorSpellTimer = MIRROR_SPELL_TIMER_DURATION;

			for (int copyIndex = 0; copyIndex < MIRROR_COPY_COUNT; copyIndex++) {
				mirrorCopyOffsets[0][copyIndex] = mirrorCopyOffsets[1][copyIndex];
				mirrorCopyOffsets[1][copyIndex] = Vec3d.ZERO;
			}
		}
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_ILLUSIONER_AMBIENT;
	}

	public Vec3d[] getMirrorCopyOffsets(float tickProgress) {
		if (mirrorSpellTimer <= 0) {
			return mirrorCopyOffsets[1];
		}

		double blend = Math.pow((mirrorSpellTimer - tickProgress) / MIRROR_SPELL_DURATION, 0.25);
		Vec3d[] result = new Vec3d[MIRROR_COPY_COUNT];

		for (int copyIndex = 0; copyIndex < MIRROR_COPY_COUNT; copyIndex++) {
			result[copyIndex] = mirrorCopyOffsets[1][copyIndex]
					.multiply(1.0 - blend)
					.add(mirrorCopyOffsets[0][copyIndex].multiply(blend));
		}

		return result;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ILLUSIONER_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ILLUSIONER_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ILLUSIONER_HURT;
	}

	@Override
	protected SoundEvent getCastSpellSound() {
		return SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL;
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		ItemStack bowStack = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));
		ItemStack arrowStack = getProjectileType(bowStack);
		PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(this, arrowStack, pullProgress, bowStack);
		double dx = target.getX() - getX();
		double dy = target.getBodyY(0.3333333333333333) - arrow.getY();
		double dz = target.getZ() - getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(
					arrow,
					serverWorld,
					arrowStack,
					dx,
					dy + horizDist * 0.2F,
					dz,
					1.6F,
					14 - serverWorld.getDifficulty().getId() * 4
			);
		}

		playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public IllagerEntity.State getState() {
		if (isSpellcasting()) {
			return IllagerEntity.State.SPELLCASTING;
		}

		return isAttacking() ? IllagerEntity.State.BOW_AND_ARROW : IllagerEntity.State.CROSSED;
	}

	class BlindTargetGoal extends SpellcastingIllagerEntity.CastSpellGoal {

		private int targetId;

		@Override
		public boolean canStart() {
			if (!super.canStart()) {
				return false;
			}

			if (IllusionerEntity.this.getTarget() == null) {
				return false;
			}

			if (IllusionerEntity.this.getTarget().getId() == targetId) {
				return false;
			}

			return getServerWorld(IllusionerEntity.this)
					.getLocalDifficulty(IllusionerEntity.this.getBlockPos())
					.isHarderThan(Difficulty.NORMAL.ordinal());
		}

		@Override
		public void start() {
			super.start();
			LivingEntity livingEntity = IllusionerEntity.this.getTarget();
			if (livingEntity != null) {
				this.targetId = livingEntity.getId();
			}
		}

		@Override
		protected int getSpellTicks() {
			return 20;
		}

		@Override
		protected int startTimeDelay() {
			return 180;
		}

		@Override
		protected void castSpell() {
			IllusionerEntity.this
					.getTarget()
					.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 400), IllusionerEntity.this);
		}

		@Override
		protected SoundEvent getSoundPrepare() {
			return SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS;
		}

		@Override
		protected SpellcastingIllagerEntity.Spell getSpell() {
			return SpellcastingIllagerEntity.Spell.BLINDNESS;
		}
	}

	class GiveInvisibilityGoal extends SpellcastingIllagerEntity.CastSpellGoal {

		@Override
		public boolean canStart() {
			if (!super.canStart()) {
				return false;
			}

			return !IllusionerEntity.this.hasStatusEffect(StatusEffects.INVISIBILITY);
		}

		@Override
		protected int getSpellTicks() {
			return 20;
		}

		@Override
		protected int startTimeDelay() {
			return 340;
		}

		@Override
		protected void castSpell() {
			IllusionerEntity.this.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 1200));
		}

		@Override
		protected @Nullable SoundEvent getSoundPrepare() {
			return SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR;
		}

		@Override
		protected SpellcastingIllagerEntity.Spell getSpell() {
			return SpellcastingIllagerEntity.Spell.DISAPPEAR;
		}
	}
}

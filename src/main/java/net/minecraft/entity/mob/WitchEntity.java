package net.minecraft.entity.mob;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Ведьма — рейдер дальнего боя, бросающая зелья. В бою выбирает тип зелья
 * в зависимости от состояния цели и дистанции. Периодически пьёт зелья
 * для самолечения и защиты (огнестойкость, скорость, дыхание под водой).
 */
public class WitchEntity extends RaiderEntity implements RangedAttackMob {

	private static final float WITCH_SPEED = 0.25F;
	private static final float SPEED_PENALTY = -0.25F;
	private static final float PARTICLE_CHANCE = 7.5E-4F;
	private static final byte PARTICLE_STATUS = 15;
	private static final double SWIFTNESS_RANGE_SQUARED = 121.0;
	private static final Identifier DRINKING_SPEED_PENALTY_MODIFIER_ID = Identifier.ofVanilla("drinking");
	private static final EntityAttributeModifier DRINKING_SPEED_PENALTY_MODIFIER = new EntityAttributeModifier(
			DRINKING_SPEED_PENALTY_MODIFIER_ID, SPEED_PENALTY, EntityAttributeModifier.Operation.ADD_VALUE
	);
	private static final TrackedData<Boolean>
			DRINKING =
			DataTracker.registerData(WitchEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private int drinkTimeLeft;
	private RaidGoal<RaiderEntity> raidGoal;
	private DisableableFollowTargetGoal<PlayerEntity> attackPlayerGoal;

	public WitchEntity(EntityType<? extends WitchEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		raidGoal = new RaidGoal<>(
				this,
				RaiderEntity.class,
				true,
				(target, world) -> hasActiveRaid() && target.getType() != EntityType.WITCH
		);
		attackPlayerGoal = new DisableableFollowTargetGoal<>(this, PlayerEntity.class, 10, true, false, null);
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 60, 10.0F));
		goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(3, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class));
		targetSelector.add(2, raidGoal);
		targetSelector.add(3, attackPlayerGoal);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(DRINKING, false);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITCH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITCH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITCH_DEATH;
	}

	public void setDrinking(boolean drinking) {
		getDataTracker().set(DRINKING, drinking);
	}

	public boolean isDrinking() {
		return getDataTracker().get(DRINKING);
	}

	public static DefaultAttributeContainer.Builder createWitchAttributes() {
		return HostileEntity
				.createHostileAttributes()
				.add(EntityAttributes.MAX_HEALTH, 26.0)
				.add(EntityAttributes.MOVEMENT_SPEED, WITCH_SPEED);
	}

	@Override
	public void tickMovement() {
		if (!getEntityWorld().isClient() && isAlive()) {
			raidGoal.decreaseCooldown();
			attackPlayerGoal.setEnabled(raidGoal.getCooldown() <= 0);

			if (isDrinking()) {
				tickDrinking();
			} else {
				tickPotionSelection();
			}

			if (random.nextFloat() < PARTICLE_CHANCE) {
				getEntityWorld().sendEntityStatus(this, PARTICLE_STATUS);
			}
		}

		super.tickMovement();
	}

	private void tickDrinking() {
		if (drinkTimeLeft-- > 0) {
			return;
		}

		setDrinking(false);
		ItemStack drinkStack = getMainHandStack();
		equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		PotionContentsComponent potionContents = drinkStack.get(DataComponentTypes.POTION_CONTENTS);

		if (drinkStack.isOf(Items.POTION) && potionContents != null) {
			potionContents.forEachEffect(
					this::addStatusEffect,
					drinkStack.getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F)
			);
		}

		emitGameEvent(GameEvent.DRINK);
		getAttributeInstance(EntityAttributes.MOVEMENT_SPEED)
				.removeModifier(DRINKING_SPEED_PENALTY_MODIFIER.id());
	}

	private void tickPotionSelection() {
		RegistryEntry<Potion> potion = selectSelfPotion();
		if (potion == null) {
			return;
		}

		equipStack(EquipmentSlot.MAINHAND, PotionContentsComponent.createStack(Items.POTION, potion));
		drinkTimeLeft = getMainHandStack().getMaxUseTime(this);
		setDrinking(true);

		if (!isSilent()) {
			getEntityWorld().playSound(
					null,
					getX(),
					getY(),
					getZ(),
					SoundEvents.ENTITY_WITCH_DRINK,
					getSoundCategory(),
					1.0F,
					0.8F + random.nextFloat() * 0.4F
			);
		}

		EntityAttributeInstance speedAttr = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		speedAttr.removeModifier(DRINKING_SPEED_PENALTY_MODIFIER_ID);
		speedAttr.addTemporaryModifier(DRINKING_SPEED_PENALTY_MODIFIER);
	}

	/**
	 * Выбирает зелье для самолечения/защиты ведьмы на основе текущего состояния.
	 * Возвращает {@code null}, если ни одно условие не выполнено.
	 */
	private RegistryEntry<Potion> selectSelfPotion() {
		if (random.nextFloat() < 0.15F && isSubmergedIn(FluidTags.WATER) && !hasStatusEffect(StatusEffects.WATER_BREATHING)) {
			return Potions.WATER_BREATHING;
		}

		DamageSource recentDamage = getRecentDamageSource();
		if (random.nextFloat() < 0.15F
				&& (isOnFire() || (recentDamage != null && recentDamage.isIn(DamageTypeTags.IS_FIRE)))
				&& !hasStatusEffect(StatusEffects.FIRE_RESISTANCE)
		) {
			return Potions.FIRE_RESISTANCE;
		}

		if (random.nextFloat() < 0.05F && getHealth() < getMaxHealth()) {
			return Potions.HEALING;
		}

		LivingEntity currentTarget = getTarget();
		if (random.nextFloat() < 0.5F
				&& currentTarget != null
				&& !hasStatusEffect(StatusEffects.SPEED)
				&& currentTarget.squaredDistanceTo(this) > SWIFTNESS_RANGE_SQUARED
		) {
			return Potions.SWIFTNESS;
		}

		return null;
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_WITCH_CELEBRATE;
	}

	@Override
	public void handleStatus(byte status) {
		if (status != PARTICLE_STATUS) {
			super.handleStatus(status);
			return;
		}

		int count = random.nextInt(35) + 10;
		for (int particleIndex = 0; particleIndex < count; particleIndex++) {
			getEntityWorld().addParticleClient(
					ParticleTypes.WITCH,
					getX() + random.nextGaussian() * 0.13F,
					getBoundingBox().maxY + 0.5 + random.nextGaussian() * 0.13F,
					getZ() + random.nextGaussian() * 0.13F,
					0.0,
					0.0,
					0.0
			);
		}
	}

	@Override
	protected float modifyAppliedDamage(DamageSource source, float amount) {
		amount = super.modifyAppliedDamage(source, amount);

		if (source.getAttacker() == this) {
			amount = 0.0F;
		}

		if (source.isIn(DamageTypeTags.WITCH_RESISTANT_TO)) {
			amount *= 0.15F;
		}

		return amount;
	}

	/**
	 * Выбирает тип зелья для броска в зависимости от цели и дистанции,
	 * затем создаёт снаряд {@link SplashPotionEntity} с нужной траекторией.
	 */
	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		if (isDrinking()) {
			return;
		}

		Vec3d targetVelocity = target.getVelocity();
		double dx = target.getX() + targetVelocity.x - getX();
		double dy = target.getEyeY() - 1.1F - getY();
		double dz = target.getZ() + targetVelocity.z - getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);

		RegistryEntry<Potion> potion = selectAttackPotion(target, horizDist);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			ItemStack potionStack = PotionContentsComponent.createStack(Items.SPLASH_POTION, potion);
			ProjectileEntity.spawnWithVelocity(
					SplashPotionEntity::new,
					serverWorld,
					potionStack,
					this,
					dx,
					dy + horizDist * 0.2,
					dz,
					0.75F,
					8.0F
			);
		}

		if (!isSilent()) {
			getEntityWorld().playSound(
					null,
					getX(),
					getY(),
					getZ(),
					SoundEvents.ENTITY_WITCH_THROW,
					getSoundCategory(),
					1.0F,
					0.8F + random.nextFloat() * 0.4F
			);
		}
	}

	/**
	 * Выбирает зелье для атаки на основе типа цели, её здоровья и дистанции.
	 * Если цель — союзный рейдер, ведьма лечит его и сбрасывает свою цель.
	 */
	private RegistryEntry<Potion> selectAttackPotion(LivingEntity target, double horizDist) {
		if (target instanceof RaiderEntity) {
			RegistryEntry<Potion> allyPotion = target.getHealth() <= 4.0F
					? Potions.HEALING
					: Potions.REGENERATION;
			setTarget(null);
			return allyPotion;
		}

		if (horizDist >= 8.0 && !target.hasStatusEffect(StatusEffects.SLOWNESS)) {
			return Potions.SLOWNESS;
		}

		if (target.getHealth() >= 8.0F && !target.hasStatusEffect(StatusEffects.POISON)) {
			return Potions.POISON;
		}

		if (horizDist <= 3.0 && !target.hasStatusEffect(StatusEffects.WEAKNESS) && random.nextFloat() < 0.25F) {
			return Potions.WEAKNESS;
		}

		return Potions.HARMING;
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
	}

	@Override
	public boolean canLead() {
		return false;
	}
}

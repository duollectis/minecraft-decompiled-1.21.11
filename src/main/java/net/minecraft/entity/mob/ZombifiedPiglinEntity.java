package net.minecraft.entity.mob;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

/**
 * Зомбированный пиглин — нейтральный моб Нижнего мира. Атакует группой при агрессии к одному.
 * При атаке получает бонус к скорости. Не превращается в воде. Не спавнится на блоках варт.
 */
public class ZombifiedPiglinEntity extends ZombieEntity implements Angerable {

	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.ZOMBIFIED_PIGLIN.getDimensions().scaled(0.5F).withEyeHeight(0.97F);
	private static final Identifier ATTACKING_SPEED_MODIFIER_ID = Identifier.ofVanilla("attacking");
	private static final EntityAttributeModifier ATTACKING_SPEED_BOOST = new EntityAttributeModifier(
			ATTACKING_SPEED_MODIFIER_ID, 0.05, EntityAttributeModifier.Operation.ADD_VALUE
	);
	private static final UniformIntProvider ANGRY_SOUND_DELAY_RANGE = TimeHelper.betweenSeconds(0, 1);
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
	private static final int ANGER_TARGET_CHANCE = 10;
	private static final UniformIntProvider ANGER_PASSING_COOLDOWN_RANGE = TimeHelper.betweenSeconds(4, 6);

	private int angrySoundDelay;
	private long angerEndTime;
	private @Nullable LazyEntityReference<LivingEntity> angryAt;
	private int angerPassingCooldown;

	public ZombifiedPiglinEntity(EntityType<? extends ZombifiedPiglinEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
	}

	@Override
	protected void initCustomGoals() {
		goalSelector.add(1, new ChargeKineticWeaponGoal<>(this, 1.0, 1.0, 10.0F, 2.0F));
		goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
		goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(
				2,
				new ActiveTargetGoal<>(this, PlayerEntity.class, ANGER_TARGET_CHANCE, true, false, this::shouldAngerAt)
		);
		targetSelector.add(3, new UniversalAngerGoal<>(this, true));
	}

	public static DefaultAttributeContainer.Builder createZombifiedPiglinAttributes() {
		return ZombieEntity.createZombieAttributes()
				.add(EntityAttributes.SPAWN_REINFORCEMENTS, 0.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.23F)
				.add(EntityAttributes.ATTACK_DAMAGE, 5.0);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		EntityAttributeInstance speedAttribute = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

		if (hasAngerTime()) {
			if (!isBaby() && !speedAttribute.hasModifier(ATTACKING_SPEED_MODIFIER_ID)) {
				speedAttribute.addTemporaryModifier(ATTACKING_SPEED_BOOST);
			}

			tickAngrySound();
		}
		else if (speedAttribute.hasModifier(ATTACKING_SPEED_MODIFIER_ID)) {
			speedAttribute.removeModifier(ATTACKING_SPEED_MODIFIER_ID);
		}

		tickAngerLogic(world, true);

		if (getTarget() != null) {
			tickAngerPassing();
		}

		super.mobTick(world);
	}

	private void tickAngrySound() {
		if (angrySoundDelay <= 0) {
			return;
		}

		angrySoundDelay--;

		if (angrySoundDelay == 0) {
			playAngrySound();
		}
	}

	private void tickAngerPassing() {
		if (angerPassingCooldown > 0) {
			angerPassingCooldown--;
			return;
		}

		if (getVisibilityCache().canSee(getTarget())) {
			angerNearbyZombifiedPiglins();
		}

		angerPassingCooldown = ANGER_PASSING_COOLDOWN_RANGE.get(random);
	}

	private void angerNearbyZombifiedPiglins() {
		double followRange = getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		Box searchBox = Box.from(getEntityPos()).expand(followRange, 10.0, followRange);
		LivingEntity currentTarget = getTarget();

		getEntityWorld()
				.getEntitiesByClass(ZombifiedPiglinEntity.class, searchBox, EntityPredicates.EXCEPT_SPECTATOR)
				.stream()
				.filter(piglin -> piglin != this)
				.filter(piglin -> piglin.getTarget() == null)
				.filter(piglin -> !piglin.isTeammate(currentTarget))
				.forEach(piglin -> piglin.setTarget(currentTarget));
	}

	private void playAngrySound() {
		playSound(
				SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_ANGRY,
				getSoundVolume() * 2.0F,
				getSoundPitch() * 1.8F
		);
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (getTarget() == null && target != null) {
			angrySoundDelay = ANGRY_SOUND_DELAY_RANGE.get(random);
			angerPassingCooldown = ANGER_PASSING_COOLDOWN_RANGE.get(random);
		}

		super.setTarget(target);
	}

	@Override
	public void chooseRandomAngerTime() {
		setAngerDuration(ANGER_TIME_RANGE.get(random));
	}

	public static boolean canSpawn(
			EntityType<ZombifiedPiglinEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getDifficulty() != Difficulty.PEACEFUL
				&& !world.getBlockState(pos.down()).isOf(Blocks.NETHER_WART_BLOCK);
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return world.doesNotIntersectEntities(this) && !world.containsFluid(getBoundingBox());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		writeAngerToData(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		readAngerFromData(getEntityWorld(), view);
	}

	@Override
	public void setAngerEndTime(long angerEndTime) {
		this.angerEndTime = angerEndTime;
	}

	@Override
	public long getAngerEndTime() {
		return angerEndTime;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return hasAngerTime()
				? SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_ANGRY
				: SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH;
	}

	@Override
	public void initEquipment(Random random, LocalDifficulty localDifficulty) {
		equipStack(
				EquipmentSlot.MAINHAND,
				new ItemStack(random.nextInt(20) == 0 ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD)
		);
	}

	@Override
	protected void initAttributes() {
		getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS).setBaseValue(0.0);
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return angryAt;
	}

	@Override
	public boolean isAngryAt(ServerWorld world, PlayerEntity player) {
		return shouldAngerAt(player, world);
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		return canPickupItem(stack);
	}
}

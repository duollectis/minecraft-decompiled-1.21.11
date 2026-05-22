package net.minecraft.entity.mob;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Карательный иллагер — ближний боец рейда с железным топором.
 * При имени «Johnny» атакует всех живых существ (режим Johnny).
 * На нормальной и сложной сложности умеет ломать двери во время рейда.
 */
public class VindicatorEntity extends IllagerEntity {

	private static final String JOHNNY_KEY = "Johnny";
	static final Predicate<Difficulty>
			DIFFICULTY_ALLOWS_DOOR_BREAKING_PREDICATE =
			difficulty -> difficulty == Difficulty.NORMAL
					|| difficulty == Difficulty.HARD;
	boolean johnny;

	public VindicatorEntity(EntityType<? extends VindicatorEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new FleeEntityGoal<>(this, CreakingEntity.class, 8.0F, 1.0, 1.2));
		goalSelector.add(2, new VindicatorEntity.BreakDoorGoal(this));
		goalSelector.add(3, new IllagerEntity.LongDoorInteractGoal(this));
		goalSelector.add(4, new RaiderEntity.PatrolApproachGoal(this, 10.0F));
		goalSelector.add(5, new MeleeAttackGoal(this, 1.0, false));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
		targetSelector.add(4, new VindicatorEntity.TargetGoal(this));
		goalSelector.add(8, new WanderAroundGoal(this, 0.6));
		goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
	}

	@Override
	protected void mobTick(ServerWorld world) {
		if (!isAiDisabled() && NavigationConditions.hasMobNavigation(this)) {
			getNavigation().setCanOpenDoors(world.hasRaidAt(getBlockPos()));
		}

		super.mobTick(world);
	}

	public static DefaultAttributeContainer.Builder createVindicatorAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.35F)
		                    .add(EntityAttributes.FOLLOW_RANGE, 12.0)
		                    .add(EntityAttributes.MAX_HEALTH, 24.0)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 5.0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		if (johnny) {
			view.putBoolean(JOHNNY_KEY, true);
		}
	}

	@Override
	public IllagerEntity.State getState() {
		if (isAttacking()) {
			return IllagerEntity.State.ATTACKING;
		}

		return isCelebrating() ? IllagerEntity.State.CELEBRATING : IllagerEntity.State.CROSSED;
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		johnny = view.getBoolean(JOHNNY_KEY, false);
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_VINDICATOR_CELEBRATE;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		EntityData result = super.initialize(world, difficulty, spawnReason, entityData);
		getNavigation().setCanOpenDoors(true);
		Random random = world.getRandom();
		initEquipment(random, difficulty);
		updateEnchantments(world, random, difficulty);
		return result;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		if (getRaid() == null) {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
		}
	}

	@Override
	public void setCustomName(@Nullable Text name) {
		super.setCustomName(name);
		if (!johnny && name != null && name.getString().equals(JOHNNY_KEY)) {
			johnny = true;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VINDICATOR_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VINDICATOR_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_VINDICATOR_HURT;
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		Raid raid = getRaid();

		if (random.nextFloat() <= raid.getEnchantmentChance()) {
			RegistryKey<EnchantmentProvider> enchantKey = wave > raid.getMaxWaves(Difficulty.NORMAL)
					? EnchantmentProviders.VINDICATOR_POST_WAVE_5_RAID
					: EnchantmentProviders.VINDICATOR_RAID;
			EnchantmentHelper.applyEnchantmentProvider(
					axe,
					world.getRegistryManager(),
					enchantKey,
					world.getLocalDifficulty(getBlockPos()),
					random
			);
		}

		equipStack(EquipmentSlot.MAINHAND, axe);
	}

	static class BreakDoorGoal extends net.minecraft.entity.ai.goal.BreakDoorGoal {

		public BreakDoorGoal(MobEntity mobEntity) {
			super(mobEntity, 6, VindicatorEntity.DIFFICULTY_ALLOWS_DOOR_BREAKING_PREDICATE);
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean shouldContinue() {
			VindicatorEntity vindicatorEntity = (VindicatorEntity) this.mob;
			return vindicatorEntity.hasActiveRaid() && super.shouldContinue();
		}

		@Override
		public boolean canStart() {
			VindicatorEntity vindicatorEntity = (VindicatorEntity) this.mob;
			return vindicatorEntity.hasActiveRaid() && vindicatorEntity.random.nextInt(toGoalTicks(10)) == 0
					&& super.canStart();
		}

		@Override
		public void start() {
			super.start();
			this.mob.setDespawnCounter(0);
		}
	}

	static class TargetGoal extends ActiveTargetGoal<LivingEntity> {

		public TargetGoal(VindicatorEntity vindicator) {
			super(vindicator, LivingEntity.class, 0, true, true, (target, world) -> target.isMobOrPlayer());
		}

		@Override
		public boolean canStart() {
			return ((VindicatorEntity) this.mob).johnny && super.canStart();
		}

		@Override
		public void start() {
			super.start();
			this.mob.setDespawnCounter(0);
		}
	}
}

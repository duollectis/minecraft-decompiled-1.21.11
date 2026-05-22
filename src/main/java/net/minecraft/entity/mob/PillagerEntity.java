package net.minecraft.entity.mob;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

/**
 * Мародёр — иллагер дальнего боя с арбалетом. Подбирает знамёна капитана рейда
 * и хранит их в инвентаре. При рейде получает зачарованный арбалет в зависимости
 * от волны. С шансом 1/300 при спавне получает зачарование «Мультивыстрел».
 */
public class PillagerEntity extends IllagerEntity implements CrossbowUser, InventoryOwner {

	private static final TrackedData<Boolean>
			CHARGING =
			DataTracker.registerData(PillagerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final int INVENTORY_SIZE = 5;
	private static final int MULTISHOT_ENCHANT_CHANCE_DENOMINATOR = 300;
	/** Смещение слота для доступа к инвентарю через {@link #getStackReference}. */
	private static final int INVENTORY_SLOT_OFFSET = 300;
	private final SimpleInventory inventory = new SimpleInventory(INVENTORY_SIZE);

	public PillagerEntity(EntityType<? extends PillagerEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new FleeEntityGoal<>(this, CreakingEntity.class, 8.0F, 1.0, 1.2));
		goalSelector.add(2, new RaiderEntity.PatrolApproachGoal(this, 10.0F));
		goalSelector.add(3, new CrossbowAttackGoal<>(this, 1.0, 8.0F));
		goalSelector.add(8, new WanderAroundGoal(this, 0.6));
		goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F, 1.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 15.0F));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	public static DefaultAttributeContainer.Builder createPillagerAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.35F)
		                    .add(EntityAttributes.MAX_HEALTH, 24.0)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
		                    .add(EntityAttributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CHARGING, false);
	}

	@Override
	public boolean canUseRangedWeapon(ItemStack stack) {
		return stack.getItem() == Items.CROSSBOW;
	}

	public boolean isCharging() {
		return dataTracker.get(CHARGING);
	}

	@Override
	public void setCharging(boolean charging) {
		dataTracker.set(CHARGING, charging);
	}

	@Override
	public void postShoot() {
		despawnCounter = 0;
	}

	@Override
	public TagKey<Item> getPreferredWeapons() {
		return ItemTags.PILLAGER_PREFERRED_WEAPONS;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		writeInventory(view);
	}

	@Override
	public IllagerEntity.State getState() {
		if (isCharging()) {
			return IllagerEntity.State.CROSSBOW_CHARGE;
		}

		if (isHolding(Items.CROSSBOW)) {
			return IllagerEntity.State.CROSSBOW_HOLD;
		}

		return isAttacking() ? IllagerEntity.State.ATTACKING : IllagerEntity.State.NEUTRAL;
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		readInventory(view);
		setCanPickUpLoot(true);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	@Override
	public int getLimitPerChunk() {
		return 1;
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
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
	}

	@Override
	protected void enchantMainHandItem(ServerWorldAccess world, Random random, LocalDifficulty localDifficulty) {
		super.enchantMainHandItem(world, random, localDifficulty);
		if (random.nextInt(MULTISHOT_ENCHANT_CHANCE_DENOMINATOR) == 0) {
			ItemStack crossbow = getMainHandStack();
			if (crossbow.isOf(Items.CROSSBOW)) {
				EnchantmentHelper.applyEnchantmentProvider(
						crossbow,
						world.getRegistryManager(),
						EnchantmentProviders.PILLAGER_SPAWN_CROSSBOW,
						localDifficulty,
						random
				);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PILLAGER_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PILLAGER_HURT;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		shoot(this, 1.6F);
	}

	@Override
	public SimpleInventory getInventory() {
		return inventory;
	}

	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		ItemStack stack = itemEntity.getStack();
		if (stack.getItem() instanceof BannerItem) {
			super.loot(world, itemEntity);
		} else if (isRaidCaptain(stack)) {
			triggerItemPickedUpByEntityCriteria(itemEntity);
			ItemStack remainder = inventory.addStack(stack);
			if (remainder.isEmpty()) {
				itemEntity.discard();
			} else {
				stack.setCount(remainder.getCount());
			}
		}
	}

	private boolean isRaidCaptain(ItemStack stack) {
		return hasActiveRaid() && stack.isOf(Items.WHITE_BANNER);
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		int inventorySlot = slot - INVENTORY_SLOT_OFFSET;
		return inventorySlot >= 0 && inventorySlot < inventory.size()
				? inventory.getStackReference(inventorySlot)
				: super.getStackReference(slot);
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
		Raid raid = getRaid();
		if (random.nextFloat() > raid.getEnchantmentChance()) {
			return;
		}

		ItemStack crossbow = new ItemStack(Items.CROSSBOW);
		RegistryKey<EnchantmentProvider> enchantKey;
		if (wave > raid.getMaxWaves(Difficulty.NORMAL)) {
			enchantKey = EnchantmentProviders.PILLAGER_POST_WAVE_5_RAID;
		} else if (wave > raid.getMaxWaves(Difficulty.EASY)) {
			enchantKey = EnchantmentProviders.PILLAGER_POST_WAVE_3_RAID;
		} else {
			enchantKey = null;
		}

		if (enchantKey != null) {
			EnchantmentHelper.applyEnchantmentProvider(
					crossbow,
					world.getRegistryManager(),
					enchantKey,
					world.getLocalDifficulty(getBlockPos()),
					getRandom()
			);
			equipStack(EquipmentSlot.MAINHAND, crossbow);
		}
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_PILLAGER_CELEBRATE;
	}
}

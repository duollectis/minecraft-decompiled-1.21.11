package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Овца — животное, которое можно стричь для получения шерсти.
 * Цвет шерсти зависит от биома при спавне и смешивается при размножении.
 * Флаги цвета и состояния стрижки упакованы в один байт трекера {@code COLOR}.
 */
public class SheepEntity extends AnimalEntity implements Shearable {

	private static final int MAX_GRASS_TIMER = 40;
	private static final int EAT_GRASS_STATUS_ID = 10;
	private static final int GRASS_EATING_START_TICK = 4;
	private static final int GRASS_EATING_END_TICK = 36;
	private static final int GRASS_EATING_GROW_TICKS = 60;

	/** Маска для извлечения индекса цвета из байта трекера (биты 0–3). */
	private static final int COLOR_INDEX_MASK = 0x0F;
	/** Маска для извлечения флага «острижена» из байта трекера (бит 4). */
	private static final int SHEARED_FLAG_MASK = 0x10;
	/** Инвертированная маска для сброса флага «острижена». */
	private static final int SHEARED_FLAG_CLEAR_MASK = ~SHEARED_FLAG_MASK;
	/** Маска для сохранения старших битов при установке цвета. */
	private static final int COLOR_UPPER_BITS_MASK = 0xF0;

	private static final TrackedData<Byte> COLOR = DataTracker.registerData(
		SheepEntity.class,
		TrackedDataHandlerRegistry.BYTE
	);
	private static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;

	private int eatGrassTimer;
	private EatGrassGoal eatGrassGoal;

	public SheepEntity(EntityType<? extends SheepEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		eatGrassGoal = new EatGrassGoal(this);
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
		goalSelector.add(2, new AnimalMateGoal(this, 1.0));
		goalSelector.add(3, new TemptGoal(this, 1.1, stack -> stack.isIn(ItemTags.SHEEP_FOOD), false));
		goalSelector.add(4, new FollowParentGoal(this, 1.1));
		goalSelector.add(5, eatGrassGoal);
		goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(8, new LookAroundGoal(this));
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.SHEEP_FOOD);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		eatGrassTimer = eatGrassGoal.getTimer();
		super.mobTick(world);
	}

	@Override
	public void tickMovement() {
		if (getEntityWorld().isClient()) {
			eatGrassTimer = Math.max(0, eatGrassTimer - 1);
		}

		super.tickMovement();
	}

	public static DefaultAttributeContainer.Builder createSheepAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MAX_HEALTH, 8.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.23F);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(COLOR, (byte) 0);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == EAT_GRASS_STATUS_ID) {
			eatGrassTimer = MAX_GRASS_TIMER;
		} else {
			super.handleStatus(status);
		}
	}

	/**
	 * Возвращает угол наклона шеи для анимации поедания травы.
	 * Плавно опускается в начале и поднимается в конце анимации.
	 */
	public float getNeckAngle(float tickProgress) {
		if (eatGrassTimer <= 0) {
			return 0.0F;
		}

		if (eatGrassTimer >= GRASS_EATING_START_TICK && eatGrassTimer <= GRASS_EATING_END_TICK) {
			return 1.0F;
		}

		return eatGrassTimer < GRASS_EATING_START_TICK
			? (eatGrassTimer - tickProgress) / GRASS_EATING_START_TICK
			: -(eatGrassTimer - MAX_GRASS_TIMER - tickProgress) / GRASS_EATING_START_TICK;
	}

	/**
	 * Возвращает угол наклона головы для анимации поедания травы.
	 * Использует синусоиду для плавного покачивания в середине анимации.
	 */
	public float getHeadAngle(float tickProgress) {
		if (eatGrassTimer > GRASS_EATING_START_TICK && eatGrassTimer <= GRASS_EATING_END_TICK) {
			float progress = (eatGrassTimer - GRASS_EATING_START_TICK - tickProgress) / 32.0F;
			return (float) (Math.PI / 5) + 0.21991149F * MathHelper.sin(progress * 28.7F);
		}

		return eatGrassTimer > 0
			? (float) (Math.PI / 5)
			: getLerpedPitch(tickProgress) * (float) (Math.PI / 180.0);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack heldStack = player.getStackInHand(hand);
		if (heldStack.isOf(Items.SHEARS)) {
			if (getEntityWorld() instanceof ServerWorld serverWorld && isShearable()) {
				sheared(serverWorld, SoundCategory.PLAYERS, heldStack);
				emitGameEvent(GameEvent.SHEAR, player);
				heldStack.damage(1, player, hand.getEquipmentSlot());
				return ActionResult.SUCCESS_SERVER;
			}

			return ActionResult.CONSUME;
		}

		return super.interactMob(player, hand);
	}

	@Override
	public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
		world.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
		forEachShearedItem(world, LootTables.SHEEP_SHEARING, shears, (dropWorld, stack) -> {
			for (int count = 0; count < stack.getCount(); count++) {
				ItemEntity dropped = dropStack(dropWorld, stack.copyWithCount(1), 1.0F);
				if (dropped != null) {
					dropped.setVelocity(dropped.getVelocity().add(
						(random.nextFloat() - random.nextFloat()) * 0.1F,
						random.nextFloat() * 0.05F,
						(random.nextFloat() - random.nextFloat()) * 0.1F
					));
				}
			}
		});
		setSheared(true);
	}

	@Override
	public boolean isShearable() {
		return isAlive() && !isSheared() && !isBaby();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Sheared", isSheared());
		view.put("Color", DyeColor.INDEX_CODEC, getColor());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setSheared(view.getBoolean("Sheared", false));
		setColor(view.<DyeColor>read("Color", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLOR));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SHEEP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SHEEP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SHEEP_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_SHEEP_STEP, 0.15F, 1.0F);
	}

	public DyeColor getColor() {
		return DyeColor.byIndex(dataTracker.get(COLOR) & COLOR_INDEX_MASK);
	}

	public void setColor(DyeColor color) {
		byte colorFlags = dataTracker.get(COLOR);
		dataTracker.set(COLOR, (byte) (colorFlags & COLOR_UPPER_BITS_MASK | color.getIndex() & COLOR_INDEX_MASK));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.SHEEP_COLOR
			? castComponentValue((ComponentType<T>) type, getColor())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.SHEEP_COLOR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.SHEEP_COLOR) {
			setColor(castComponentValue(DataComponentTypes.SHEEP_COLOR, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	public boolean isSheared() {
		return (dataTracker.get(COLOR) & SHEARED_FLAG_MASK) != 0;
	}

	public void setSheared(boolean sheared) {
		byte colorFlags = dataTracker.get(COLOR);
		dataTracker.set(COLOR, sheared
			? (byte) (colorFlags | SHEARED_FLAG_MASK)
			: (byte) (colorFlags & SHEARED_FLAG_CLEAR_MASK)
		);
	}

	public static DyeColor selectSpawnColor(ServerWorldAccess world, BlockPos pos) {
		RegistryEntry<Biome> biome = world.getBiome(pos);
		return SheepColors.select(biome, world.getRandom());
	}

	/**
	 * Создаёт детёныша при размножении. Цвет шерсти смешивается из цветов обоих родителей.
	 */
	@Override
	public @Nullable SheepEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		SheepEntity baby = EntityType.SHEEP.create(serverWorld, SpawnReason.BREEDING);
		if (baby != null) {
			DyeColor myColor = getColor();
			DyeColor otherColor = ((SheepEntity) passiveEntity).getColor();
			baby.setColor(DyeColor.mixColors(serverWorld, myColor, otherColor));
		}

		return baby;
	}

	@Override
	public void onEatingGrass() {
		super.onEatingGrass();
		setSheared(false);
		if (isBaby()) {
			growUp(GRASS_EATING_GROW_TICKS);
		}
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		setColor(selectSpawnColor(world, getBlockPos()));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}
}

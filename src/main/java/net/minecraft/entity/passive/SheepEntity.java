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
 * {@code SheepEntity}.
 */
public class SheepEntity extends AnimalEntity implements Shearable {

	private static final int MAX_GRASS_TIMER = 40;
	private static final TrackedData<Byte>
			COLOR =
			DataTracker.registerData(SheepEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;
	private static final boolean DEFAULT_SHEARED = false;
	private int eatGrassTimer;
	private EatGrassGoal eatGrassGoal;

	public SheepEntity(EntityType<? extends SheepEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		this.eatGrassGoal = new EatGrassGoal(this);
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
		this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(3, new TemptGoal(this, 1.1, stack -> stack.isIn(ItemTags.SHEEP_FOOD), false));
		this.goalSelector.add(4, new FollowParentGoal(this, 1.1));
		this.goalSelector.add(5, this.eatGrassGoal);
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.SHEEP_FOOD);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		this.eatGrassTimer = this.eatGrassGoal.getTimer();
		super.mobTick(world);
	}

	@Override
	public void tickMovement() {
		if (this.getEntityWorld().isClient()) {
			this.eatGrassTimer = Math.max(0, this.eatGrassTimer - 1);
		}

		super.tickMovement();
	}

	public static DefaultAttributeContainer.Builder createSheepAttributes() {
		return AnimalEntity
				.createAnimalAttributes()
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
		if (status == 10) {
			this.eatGrassTimer = 40;
		}
		else {
			super.handleStatus(status);
		}
	}

	public float getNeckAngle(float tickProgress) {
		if (this.eatGrassTimer <= 0) {
			return 0.0F;
		}
		else if (this.eatGrassTimer >= 4 && this.eatGrassTimer <= 36) {
			return 1.0F;
		}
		else {
			return this.eatGrassTimer < 4 ? (this.eatGrassTimer - tickProgress) / 4.0F
			                              : -(this.eatGrassTimer - 40 - tickProgress) / 4.0F;
		}
	}

	public float getHeadAngle(float tickProgress) {
		if (this.eatGrassTimer > 4 && this.eatGrassTimer <= 36) {
			float f = (this.eatGrassTimer - 4 - tickProgress) / 32.0F;
			return (float) (Math.PI / 5) + 0.21991149F * MathHelper.sin(f * 28.7F);
		}
		else {
			return this.eatGrassTimer > 0 ? (float) (Math.PI / 5)
			                              : this.getLerpedPitch(tickProgress) * (float) (Math.PI / 180.0);
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.SHEARS)) {
			if (this.getEntityWorld() instanceof ServerWorld serverWorld && this.isShearable()) {
				this.sheared(serverWorld, SoundCategory.PLAYERS, itemStack);
				this.emitGameEvent(GameEvent.SHEAR, player);
				itemStack.damage(1, player, hand.getEquipmentSlot());
				return ActionResult.SUCCESS_SERVER;
			}
			else {
				return ActionResult.CONSUME;
			}
		}
		else {
			return super.interactMob(player, hand);
		}
	}

	@Override
	public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
		world.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
		this.forEachShearedItem(
				world,
				LootTables.SHEEP_SHEARING,
				shears,
				(worldx, stack) -> {
					for (int i = 0; i < stack.getCount(); i++) {
						ItemEntity itemEntity = this.dropStack(worldx, stack.copyWithCount(1), 1.0F);
						if (itemEntity != null) {
							itemEntity.setVelocity(
									itemEntity.getVelocity()
									          .add(
											          (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
											          this.random.nextFloat() * 0.05F,
											          (this.random.nextFloat() - this.random.nextFloat()) * 0.1F
									          )
							);
						}
					}
				}
		);
		this.setSheared(true);
	}

	@Override
	public boolean isShearable() {
		return this.isAlive() && !this.isSheared() && !this.isBaby();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Sheared", this.isSheared());
		view.put("Color", DyeColor.INDEX_CODEC, this.getColor());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setSheared(view.getBoolean("Sheared", false));
		this.setColor(view.<DyeColor>read("Color", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLOR));
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
		this.playSound(SoundEvents.ENTITY_SHEEP_STEP, 0.15F, 1.0F);
	}

	public DyeColor getColor() {
		return DyeColor.byIndex(this.dataTracker.get(COLOR) & 15);
	}

	public void setColor(DyeColor color) {
		byte b = this.dataTracker.get(COLOR);
		this.dataTracker.set(COLOR, (byte) (b & 240 | color.getIndex() & 15));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.SHEEP_COLOR ? castComponentValue((ComponentType<T>) type, this.getColor())
		                                              : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.SHEEP_COLOR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.SHEEP_COLOR) {
			this.setColor(castComponentValue(DataComponentTypes.SHEEP_COLOR, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	public boolean isSheared() {
		return (this.dataTracker.get(COLOR) & 16) != 0;
	}

	public void setSheared(boolean sheared) {
		byte b = this.dataTracker.get(COLOR);
		if (sheared) {
			this.dataTracker.set(COLOR, (byte) (b | 16));
		}
		else {
			this.dataTracker.set(COLOR, (byte) (b & -17));
		}
	}

	/**
	 * Select spawn color.
	 *
	 * @param world world
	 * @param pos pos
	 *
	 * @return DyeColor — результат операции
	 */
	public static DyeColor selectSpawnColor(ServerWorldAccess world, BlockPos pos) {
		RegistryEntry<Biome> registryEntry = world.getBiome(pos);
		return SheepColors.select(registryEntry, world.getRandom());
	}

	/**
	 * Создаёт child.
	 *
	 * @param serverWorld server world
	 * @param passiveEntity passive entity
	 *
	 * @return @Nullable SheepEntity — результат операции
	 */
	public @Nullable SheepEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		SheepEntity sheepEntity = EntityType.SHEEP.create(serverWorld, SpawnReason.BREEDING);
		if (sheepEntity != null) {
			DyeColor dyeColor = this.getColor();
			DyeColor dyeColor2 = ((SheepEntity) passiveEntity).getColor();
			sheepEntity.setColor(DyeColor.mixColors(serverWorld, dyeColor, dyeColor2));
		}

		return sheepEntity;
	}

	@Override
	public void onEatingGrass() {
		super.onEatingGrass();
		this.setSheared(false);
		if (this.isBaby()) {
			this.growUp(60);
		}
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		this.setColor(selectSpawnColor(world, this.getBlockPos()));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}
}

package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Лама — вьючное животное с поддержкой каравана и плевком в качестве атаки.
 * Сила (strength) определяет количество колонок инвентаря при наличии сундука (1–5).
 * Ламы выстраиваются в каравана, следуя друг за другом через {@link #follow(LlamaEntity)}.
 */
public class LlamaEntity extends AbstractDonkeyEntity implements RangedAttackMob {

	private static final int MAX_STRENGTH = 5;
	private static final int RARE_MAX_STRENGTH = 5;
	private static final float RARE_STRENGTH_CHANCE = 0.04F;
	private static final int COMMON_MAX_STRENGTH_ROLL = 3;
	private static final float RARE_CHILD_STRENGTH_BONUS_CHANCE = 0.03F;
	private static final int MAX_TEMPER = 30;
	private static final double SPIT_ARC_FACTOR = 0.2;
	private static final float SPIT_SPEED = 1.5F;
	private static final float SPIT_INACCURACY = 10.0F;
	private static final double SPIT_TARGET_BODY_FRACTION = 0.3333333333333333;

	private static final TrackedData<Integer> STRENGTH = DataTracker.registerData(
		LlamaEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(
		LlamaEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);
	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.LLAMA
		.getDimensions()
		.withAttachments(
			EntityAttachments.builder()
				.add(EntityAttachmentType.PASSENGER, 0.0F, EntityType.LLAMA.getHeight() - 0.8125F, -0.3F)
		)
		.scaled(0.5F);

	boolean spit;
	private @Nullable LlamaEntity following;
	private @Nullable LlamaEntity follower;

	public LlamaEntity(EntityType<? extends LlamaEntity> entityType, World world) {
		super(entityType, world);
		getNavigation().setMaxFollowRange(40.0F);
	}

	public boolean isTrader() {
		return false;
	}

	private void setStrength(int strength) {
		dataTracker.set(STRENGTH, Math.max(1, Math.min(MAX_STRENGTH, strength)));
	}

	/**
	 * Инициализирует силу ламы при спавне: с вероятностью 4% сила равна 5,
	 * иначе — случайное значение от 1 до 3.
	 */
	private void initializeStrength(Random random) {
		int maxRoll = random.nextFloat() < RARE_STRENGTH_CHANCE ? RARE_MAX_STRENGTH : COMMON_MAX_STRENGTH_ROLL;
		setStrength(1 + random.nextInt(maxRoll));
	}

	public int getStrength() {
		return dataTracker.get(STRENGTH);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Variant", Variant.INDEX_CODEC, getVariant());
		view.putInt("Strength", getStrength());
	}

	@Override
	protected void readCustomData(ReadView view) {
		setStrength(view.getInt("Strength", 0));
		super.readCustomData(view);
		setVariant(view.<Variant>read("Variant", Variant.INDEX_CODEC).orElse(Variant.DEFAULT));
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new HorseBondWithPlayerGoal(this, 1.2));
		goalSelector.add(2, new FormCaravanGoal(this, 2.1F));
		goalSelector.add(3, new ProjectileAttackGoal(this, 1.25, 40, 20.0F));
		goalSelector.add(3, new EscapeDangerGoal(this, 1.2));
		goalSelector.add(4, new AnimalMateGoal(this, 1.0));
		goalSelector.add(5, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.LLAMA_TEMPT_ITEMS), false));
		goalSelector.add(6, new FollowParentGoal(this, 1.0));
		goalSelector.add(7, new WanderAroundFarGoal(this, 0.7));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(9, new LookAroundGoal(this));
		targetSelector.add(1, new SpitRevengeGoal(this));
		targetSelector.add(2, new ChaseWolvesGoal(this));
	}

	public static DefaultAttributeContainer.Builder createLlamaAttributes() {
		return createAbstractDonkeyAttributes();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(STRENGTH, 0);
		builder.add(VARIANT, 0);
	}

	public Variant getVariant() {
		return Variant.byIndex(dataTracker.get(VARIANT));
	}

	private void setVariant(Variant variant) {
		dataTracker.set(VARIANT, variant.index);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.LLAMA_VARIANT
			? castComponentValue((ComponentType<T>) type, getVariant())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.LLAMA_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.LLAMA_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.LLAMA_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.LLAMA_FOOD);
	}

	@Override
	protected boolean receiveFood(PlayerEntity player, ItemStack item) {
		int growTicks = 0;
		int temperBonus = 0;
		float healAmount = 0.0F;
		boolean consumed = false;

		if (item.isOf(Items.WHEAT)) {
			growTicks = 10;
			temperBonus = 3;
			healAmount = 2.0F;
		} else if (item.isOf(Blocks.HAY_BLOCK.asItem())) {
			growTicks = 90;
			temperBonus = 6;
			healAmount = 10.0F;
			if (isTame() && getBreedingAge() == 0 && canEat()) {
				consumed = true;
				lovePlayer(player);
			}
		}

		if (getHealth() < getMaxHealth() && healAmount > 0.0F) {
			heal(healAmount);
			consumed = true;
		}

		if (isBaby() && growTicks > 0) {
			getEntityWorld().addParticleClient(
				ParticleTypes.HAPPY_VILLAGER,
				getParticleX(1.0),
				getRandomBodyY() + 0.5,
				getParticleZ(1.0),
				0.0,
				0.0,
				0.0
			);
			if (!getEntityWorld().isClient()) {
				growUp(growTicks);
				consumed = true;
			}
		}

		if (temperBonus > 0
			&& (consumed || !isTame())
			&& getTemper() < getMaxTemper()
			&& !getEntityWorld().isClient()
		) {
			addTemper(temperBonus);
			consumed = true;
		}

		if (consumed && !isSilent()) {
			SoundEvent eatSound = getEatSound();
			if (eatSound != null) {
				getEntityWorld().playSound(
					null,
					getX(),
					getY(),
					getZ(),
					getEatSound(),
					getSoundCategory(),
					1.0F,
					1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F
				);
			}
		}

		return consumed;
	}

	@Override
	public boolean isImmobile() {
		return isDead() || isEatingGrass();
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		initializeStrength(random);
		Variant variant;
		if (entityData instanceof LlamaData llamaData) {
			variant = llamaData.variant;
		} else {
			variant = Util.getRandom(Variant.values(), random);
			entityData = new LlamaData(variant);
		}

		setVariant(variant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected boolean shouldAmbientStand() {
		return false;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_LLAMA_ANGRY;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_LLAMA_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_LLAMA_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_LLAMA_DEATH;
	}

	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_LLAMA_EAT;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_LLAMA_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void playAddChestSound() {
		playSound(
			SoundEvents.ENTITY_LLAMA_CHEST,
			1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
		);
	}

	@Override
	public int getInventoryColumns() {
		return hasChest() ? getStrength() : 0;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}

	@Override
	public int getMaxTemper() {
		return MAX_TEMPER;
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}

		if (other instanceof LlamaEntity otherLlama) {
			return canBreed() && otherLlama.canBreed();
		}

		return false;
	}

	/**
	 * Создаёт детёныша ламы. Сила потомка — случайное значение от 1 до максимума
	 * из сил родителей, с редким шансом +1 бонуса.
	 */
	public @Nullable LlamaEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		LlamaEntity child = createChild();
		if (child == null) {
			return null;
		}

		setChildAttributes(passiveEntity, child);
		LlamaEntity otherParent = (LlamaEntity) passiveEntity;
		int strength = random.nextInt(Math.max(getStrength(), otherParent.getStrength())) + 1;
		if (random.nextFloat() < RARE_CHILD_STRENGTH_BONUS_CHANCE) {
			strength++;
		}

		child.setStrength(strength);
		child.setVariant(random.nextBoolean() ? getVariant() : otherParent.getVariant());

		return child;
	}

	protected @Nullable LlamaEntity createChild() {
		return EntityType.LLAMA.create(getEntityWorld(), SpawnReason.BREEDING);
	}

	/**
	 * Плюёт в цель: создаёт снаряд {@link LlamaSpitEntity} с параболической траекторией.
	 */
	private void spitAt(LivingEntity target) {
		LlamaSpitEntity spitEntity = new LlamaSpitEntity(getEntityWorld(), this);
		double dx = target.getX() - getX();
		double dy = target.getBodyY(SPIT_TARGET_BODY_FRACTION) - spitEntity.getY();
		double dz = target.getZ() - getZ();
		double arcHeight = Math.sqrt(dx * dx + dz * dz) * SPIT_ARC_FACTOR;
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(spitEntity, serverWorld, ItemStack.EMPTY, dx, dy + arcHeight, dz, SPIT_SPEED, SPIT_INACCURACY);
		}

		if (!isSilent()) {
			getEntityWorld().playSound(
				null,
				getX(),
				getY(),
				getZ(),
				SoundEvents.ENTITY_LLAMA_SPIT,
				getSoundCategory(),
				1.0F,
				1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F
			);
		}

		spit = true;
	}

	void setSpit(boolean spit) {
		this.spit = spit;
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		int damage = computeFallDamage(fallDistance, damagePerDistance);
		if (damage <= 0) {
			return false;
		}

		if (fallDistance >= 6.0) {
			serverDamage(damageSource, damage);
			handleFallDamageForPassengers(fallDistance, damagePerDistance, damageSource);
		}

		playBlockFallSound();
		return true;
	}

	public void stopFollowing() {
		if (following != null) {
			following.follower = null;
		}

		following = null;
	}

	public void follow(LlamaEntity llama) {
		following = llama;
		following.follower = this;
	}

	public boolean hasFollower() {
		return follower != null;
	}

	public boolean isFollowing() {
		return following != null;
	}

	public @Nullable LlamaEntity getFollowing() {
		return following;
	}

	@Override
	protected double getFollowLeashSpeed() {
		return 2.0;
	}

	@Override
	public boolean canUseQuadLeashAttachmentPoint() {
		return false;
	}

	@Override
	protected void walkToParent(ServerWorld world) {
		if (!isFollowing() && isBaby()) {
			super.walkToParent(world);
		}
	}

	@Override
	public boolean eatsGrass() {
		return false;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		spitAt(target);
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.75 * getStandingEyeHeight(), getWidth() * 0.5);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return getPassengerAttachmentPos(this, passenger, dimensions.attachments());
	}

	static class ChaseWolvesGoal extends ActiveTargetGoal<WolfEntity> {

		public ChaseWolvesGoal(LlamaEntity llama) {
			super(llama, WolfEntity.class, 16, false, true, (wolf, world) -> !((WolfEntity) wolf).isTamed());
		}

		@Override
		protected double getFollowRange() {
			return super.getFollowRange() * 0.25;
		}
	}

	static class LlamaData extends PassiveEntity.PassiveData {

		public final Variant variant;

		LlamaData(Variant variant) {
			super(true);
			this.variant = variant;
		}
	}

	static class SpitRevengeGoal extends RevengeGoal {

		public SpitRevengeGoal(LlamaEntity llama) {
			super(llama);
		}

		@Override
		public boolean shouldContinue() {
			if (mob instanceof LlamaEntity llamaEntity && llamaEntity.spit) {
				llamaEntity.setSpit(false);
				return false;
			}

			return super.shouldContinue();
		}
	}

	/**
	 * Вариант (окрас) ламы. Хранится как индекс в DataTracker.
	 */
	public enum Variant implements StringIdentifiable {
		CREAMY(0, "creamy"),
		WHITE(1, "white"),
		BROWN(2, "brown"),
		GRAY(3, "gray");

		public static final Variant DEFAULT = CREAMY;
		public static final Codec<Variant> CODEC = StringIdentifiable.createCodec(Variant::values);
		private static final IntFunction<Variant> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			Variant::getIndex,
			values(),
			ValueLists.OutOfBoundsHandling.CLAMP
		);
		public static final PacketCodec<ByteBuf, Variant> PACKET_CODEC = PacketCodecs.indexed(
			INDEX_MAPPER,
			Variant::getIndex
		);

		@Deprecated
		public static final Codec<Variant> INDEX_CODEC = Codec.INT.xmap(INDEX_MAPPER::apply, Variant::getIndex);

		final int index;
		private final String id;

		Variant(int index, String id) {
			this.index = index;
			this.id = id;
		}

		public int getIndex() {
			return index;
		}

		public static Variant byIndex(int index) {
			return INDEX_MAPPER.apply(index);
		}

		@Override
		public String asString() {
			return id;
		}
	}
}

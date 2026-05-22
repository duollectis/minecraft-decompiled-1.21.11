package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Панда — пассивное существо с генетической системой, определяющей поведение и внешний вид.
 * Поддерживает 7 генотипов: NORMAL, LAZY, WORRIED, PLAYFUL, BROWN, WEAK, AGGRESSIVE.
 * Продуктовый ген вычисляется из главного и скрытого генов по правилам доминантности/рецессивности.
 */
public class PandaEntity extends AnimalEntity {

	private static final TrackedData<Integer> ASK_FOR_BAMBOO_TICKS = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Integer> SNEEZE_PROGRESS = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Integer> EATING_TICKS = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Byte> MAIN_GENE = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.BYTE
	);
	private static final TrackedData<Byte> HIDDEN_GENE = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.BYTE
	);
	private static final TrackedData<Byte> PANDA_FLAGS = DataTracker.registerData(
		PandaEntity.class, TrackedDataHandlerRegistry.BYTE
	);

	static final TargetPredicate ASK_FOR_BAMBOO_TARGET = TargetPredicate.createNonAttackable().setBaseMaxDistance(8.0);

	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.PANDA
		.getDimensions()
		.scaled(0.5F)
		.withAttachments(EntityAttachments.builder().add(EntityAttachmentType.PASSENGER, 0.0F, 0.40625F, 0.0F));

	private static final int SNEEZING_FLAG = 2;
	private static final int PLAYING_FLAG = 4;
	private static final int SITTING_FLAG = 8;
	private static final int LYING_ON_BACK_FLAG = 16;
	private static final int EATING_ANIMATION_INTERVAL = 5;
	private static final int EATING_PARTICLES_COUNT = 6;
	private static final int MAX_GENE_ID = 6;
	private static final int BAMBOO_SEARCH_RADIUS = 8;
	private static final int BAMBOO_SEARCH_HEIGHT = 3;

	public static final int MAIN_GENE_MUTATION_CHANCE = 32;
	private static final int HIDDEN_GENE_MUTATION_CHANCE = 32;

	boolean shouldGetRevenge;
	boolean shouldAttack;
	public int playingTicks;
	private Vec3d playingJump;
	private float sittingAnimationProgress;
	private float lastSittingAnimationProgress;
	private float lieOnBackAnimationProgress;
	private float lastLieOnBackAnimationProgress;
	private float rollOverAnimationProgress;
	private float lastRollOverAnimationProgress;
	PandaEntity.LookAtEntityGoal lookAtPlayerGoal;

	public PandaEntity(EntityType<? extends PandaEntity> entityType, World world) {
		super(entityType, world);
		moveControl = new PandaEntity.PandaMoveControl(this);
		if (!isBaby()) {
			setCanPickUpLoot(true);
		}
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND && canPickUpLoot();
	}

	public int getAskForBambooTicks() {
		return dataTracker.get(ASK_FOR_BAMBOO_TICKS);
	}

	public void setAskForBambooTicks(int askForBambooTicks) {
		dataTracker.set(ASK_FOR_BAMBOO_TICKS, askForBambooTicks);
	}

	public boolean isSneezing() {
		return hasPandaFlag(SNEEZING_FLAG);
	}

	public boolean isSitting() {
		return hasPandaFlag(SITTING_FLAG);
	}

	public void setSitting(boolean sitting) {
		setPandaFlag(SITTING_FLAG, sitting);
	}

	public boolean isLyingOnBack() {
		return hasPandaFlag(LYING_ON_BACK_FLAG);
	}

	public void setLyingOnBack(boolean lyingOnBack) {
		setPandaFlag(LYING_ON_BACK_FLAG, lyingOnBack);
	}

	public boolean isEating() {
		return dataTracker.get(EATING_TICKS) > 0;
	}

	public void setEating(boolean eating) {
		dataTracker.set(EATING_TICKS, eating ? 1 : 0);
	}

	private int getEatingTicks() {
		return dataTracker.get(EATING_TICKS);
	}

	private void setEatingTicks(int eatingTicks) {
		dataTracker.set(EATING_TICKS, eatingTicks);
	}

	public void setSneezing(boolean sneezing) {
		setPandaFlag(SNEEZING_FLAG, sneezing);
		if (!sneezing) {
			setSneezeProgress(0);
		}
	}

	public int getSneezeProgress() {
		return dataTracker.get(SNEEZE_PROGRESS);
	}

	public void setSneezeProgress(int sneezeProgress) {
		dataTracker.set(SNEEZE_PROGRESS, sneezeProgress);
	}

	public PandaEntity.Gene getMainGene() {
		return PandaEntity.Gene.byId(dataTracker.get(MAIN_GENE));
	}

	public void setMainGene(PandaEntity.Gene gene) {
		if (gene.getId() > MAX_GENE_ID) {
			gene = PandaEntity.Gene.createRandom(random);
		}

		dataTracker.set(MAIN_GENE, (byte) gene.getId());
	}

	public PandaEntity.Gene getHiddenGene() {
		return PandaEntity.Gene.byId(dataTracker.get(HIDDEN_GENE));
	}

	public void setHiddenGene(PandaEntity.Gene gene) {
		if (gene.getId() > MAX_GENE_ID) {
			gene = PandaEntity.Gene.createRandom(random);
		}

		dataTracker.set(HIDDEN_GENE, (byte) gene.getId());
	}

	public boolean isPlaying() {
		return hasPandaFlag(PLAYING_FLAG);
	}

	public void setPlaying(boolean playing) {
		setPandaFlag(PLAYING_FLAG, playing);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ASK_FOR_BAMBOO_TICKS, 0);
		builder.add(SNEEZE_PROGRESS, 0);
		builder.add(MAIN_GENE, (byte) 0);
		builder.add(HIDDEN_GENE, (byte) 0);
		builder.add(PANDA_FLAGS, (byte) 0);
		builder.add(EATING_TICKS, 0);
	}

	private boolean hasPandaFlag(int bitmask) {
		return (dataTracker.get(PANDA_FLAGS) & bitmask) != 0;
	}

	private void setPandaFlag(int mask, boolean value) {
		byte flags = dataTracker.get(PANDA_FLAGS);
		dataTracker.set(PANDA_FLAGS, value ? (byte) (flags | mask) : (byte) (flags & ~mask));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("MainGene", PandaEntity.Gene.CODEC, getMainGene());
		view.put("HiddenGene", PandaEntity.Gene.CODEC, getHiddenGene());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setMainGene(view.<PandaEntity.Gene>read("MainGene", PandaEntity.Gene.CODEC).orElse(PandaEntity.Gene.NORMAL));
		setHiddenGene(view.<PandaEntity.Gene>read("HiddenGene", PandaEntity.Gene.CODEC).orElse(PandaEntity.Gene.NORMAL));
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		PandaEntity child = EntityType.PANDA.create(world, SpawnReason.BREEDING);
		if (child == null) {
			return null;
		}

		if (entity instanceof PandaEntity otherParent) {
			child.initGenes(this, otherParent);
		}

		child.resetAttributes();
		return child;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new PandaEntity.PandaEscapeDangerGoal(this, 2.0));
		goalSelector.add(2, new PandaEntity.PandaMateGoal(this, 1.0));
		goalSelector.add(3, new PandaEntity.AttackGoal(this, 1.2F, true));
		goalSelector.add(4, new TemptGoal(this, 1.0, stack -> stack.isIn(ItemTags.PANDA_FOOD), false));
		goalSelector.add(6, new PandaEntity.PandaFleeGoal<>(this, PlayerEntity.class, 8.0F, 2.0, 2.0));
		goalSelector.add(6, new PandaEntity.PandaFleeGoal<>(this, HostileEntity.class, 4.0F, 2.0, 2.0));
		goalSelector.add(7, new PandaEntity.PickUpFoodGoal());
		goalSelector.add(8, new PandaEntity.LieOnBackGoal(this));
		goalSelector.add(8, new PandaEntity.SneezeGoal(this));
		lookAtPlayerGoal = new PandaEntity.LookAtEntityGoal(this, PlayerEntity.class, 6.0F);
		goalSelector.add(9, lookAtPlayerGoal);
		goalSelector.add(10, new LookAroundGoal(this));
		goalSelector.add(12, new PandaEntity.PlayGoal(this));
		goalSelector.add(13, new FollowParentGoal(this, 1.25));
		goalSelector.add(14, new WanderAroundFarGoal(this, 1.0));
		targetSelector.add(1, new PandaEntity.PandaRevengeGoal(this).setGroupRevenge());
	}

	public static DefaultAttributeContainer.Builder createPandaAttributes() {
		return AnimalEntity
			.createAnimalAttributes()
			.add(EntityAttributes.MOVEMENT_SPEED, 0.15F)
			.add(EntityAttributes.ATTACK_DAMAGE, 6.0);
	}

	public PandaEntity.Gene getProductGene() {
		return PandaEntity.Gene.getProductGene(getMainGene(), getHiddenGene());
	}

	public boolean isLazy() {
		return getProductGene() == PandaEntity.Gene.LAZY;
	}

	public boolean isWorried() {
		return getProductGene() == PandaEntity.Gene.WORRIED;
	}

	public boolean isPlayful() {
		return getProductGene() == PandaEntity.Gene.PLAYFUL;
	}

	public boolean isBrown() {
		return getProductGene() == PandaEntity.Gene.BROWN;
	}

	public boolean isWeak() {
		return getProductGene() == PandaEntity.Gene.WEAK;
	}

	@Override
	public boolean isAttacking() {
		return getProductGene() == PandaEntity.Gene.AGGRESSIVE;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!isAttacking()) {
			shouldAttack = true;
		}

		return super.tryAttack(world, target);
	}

	@Override
	public void playAttackSound() {
		playSound(SoundEvents.ENTITY_PANDA_BITE, 1.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (isWorried()) {
			if (getEntityWorld().isThundering() && !isTouchingWater()) {
				setSitting(true);
				setEating(false);
			} else if (!isEating()) {
				setSitting(false);
			}
		}

		LivingEntity target = getTarget();
		if (target == null) {
			shouldGetRevenge = false;
			shouldAttack = false;
		}

		if (getAskForBambooTicks() > 0) {
			if (target != null) {
				lookAtEntity(target, 90.0F, 90.0F);
			}

			int bambooTicks = getAskForBambooTicks();
			if (bambooTicks == 29 || bambooTicks == 14) {
				playSound(SoundEvents.ENTITY_PANDA_CANT_BREED, 1.0F, 1.0F);
			}

			setAskForBambooTicks(bambooTicks - 1);
		}

		if (isSneezing()) {
			int sneezeProgress = getSneezeProgress() + 1;
			setSneezeProgress(sneezeProgress);
			if (sneezeProgress > 20) {
				setSneezing(false);
				sneeze();
			} else if (sneezeProgress == 1) {
				playSound(SoundEvents.ENTITY_PANDA_PRE_SNEEZE, 1.0F, 1.0F);
			}
		}

		if (isPlaying()) {
			updatePlaying();
		} else {
			playingTicks = 0;
		}

		if (isSitting()) {
			setPitch(0.0F);
		}

		updateSittingAnimation();
		updateEatingAnimation();
		updateLieOnBackAnimation();
		updateRollOverAnimation();
	}

	public boolean isScaredByThunderstorm() {
		return isWorried() && getEntityWorld().isThundering();
	}

	private void updateEatingAnimation() {
		if (!isEating()
			&& isSitting()
			&& !isScaredByThunderstorm()
			&& !getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()
			&& random.nextInt(80) == 1
		) {
			setEating(true);
		} else if (getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() || !isSitting()) {
			setEating(false);
		}

		if (!isEating()) {
			return;
		}

		playEatingAnimation();

		if (!getEntityWorld().isClient() && getEatingTicks() > 80 && random.nextInt(20) == 1) {
			if (getEatingTicks() > 100 && getEquippedStack(EquipmentSlot.MAINHAND).isIn(ItemTags.PANDA_EATS_FROM_GROUND)) {
				if (!getEntityWorld().isClient()) {
					equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
					emitGameEvent(GameEvent.EAT);
				}

				setSitting(false);
			}

			setEating(false);
			return;
		}

		setEatingTicks(getEatingTicks() + 1);
	}

	private void playEatingAnimation() {
		if (getEatingTicks() % EATING_ANIMATION_INTERVAL != 0) {
			return;
		}

		playSound(
			SoundEvents.ENTITY_PANDA_EAT,
			0.5F + 0.5F * random.nextInt(2),
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
		);

		for (int particleIndex = 0; particleIndex < EATING_PARTICLES_COUNT; particleIndex++) {
			Vec3d velocity = new Vec3d(
				(random.nextFloat() - 0.5) * 0.1,
				random.nextFloat() * 0.1 + 0.1,
				(random.nextFloat() - 0.5) * 0.1
			);
			velocity = velocity.rotateX(-getPitch() * (float) (Math.PI / 180.0));
			velocity = velocity.rotateY(-getYaw() * (float) (Math.PI / 180.0));

			double offsetY = -random.nextFloat() * 0.6 - 0.3;
			Vec3d particlePos = new Vec3d(
				(random.nextFloat() - 0.5) * 0.8,
				offsetY,
				1.0 + (random.nextFloat() - 0.5) * 0.4
			);
			particlePos = particlePos.rotateY(-bodyYaw * (float) (Math.PI / 180.0));
			particlePos = particlePos.add(getX(), getEyeY() + 1.0, getZ());

			getEntityWorld().addParticleClient(
				new ItemStackParticleEffect(ParticleTypes.ITEM, getEquippedStack(EquipmentSlot.MAINHAND)),
				particlePos.x,
				particlePos.y,
				particlePos.z,
				velocity.x,
				velocity.y + 0.05,
				velocity.z
			);
		}
	}

	private void updateSittingAnimation() {
		lastSittingAnimationProgress = sittingAnimationProgress;
		sittingAnimationProgress = isSitting()
			? Math.min(1.0F, sittingAnimationProgress + 0.15F)
			: Math.max(0.0F, sittingAnimationProgress - 0.19F);
	}

	private void updateLieOnBackAnimation() {
		lastLieOnBackAnimationProgress = lieOnBackAnimationProgress;
		lieOnBackAnimationProgress = isLyingOnBack()
			? Math.min(1.0F, lieOnBackAnimationProgress + 0.15F)
			: Math.max(0.0F, lieOnBackAnimationProgress - 0.19F);
	}

	private void updateRollOverAnimation() {
		lastRollOverAnimationProgress = rollOverAnimationProgress;
		rollOverAnimationProgress = isPlaying()
			? Math.min(1.0F, rollOverAnimationProgress + 0.15F)
			: Math.max(0.0F, rollOverAnimationProgress - 0.19F);
	}

	public float getSittingAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastSittingAnimationProgress, sittingAnimationProgress);
	}

	public float getLieOnBackAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastLieOnBackAnimationProgress, lieOnBackAnimationProgress);
	}

	public float getRollOverAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastRollOverAnimationProgress, rollOverAnimationProgress);
	}

	private void updatePlaying() {
		playingTicks++;
		if (playingTicks > 32) {
			setPlaying(false);
			return;
		}

		if (getEntityWorld().isClient()) {
			return;
		}

		Vec3d velocity = getVelocity();
		if (playingTicks == 1) {
			float yawRad = getYaw() * (float) (Math.PI / 180.0);
			float jumpStrength = isBaby() ? 0.1F : 0.2F;
			playingJump = new Vec3d(
				velocity.x + -MathHelper.sin(yawRad) * jumpStrength,
				0.0,
				velocity.z + MathHelper.cos(yawRad) * jumpStrength
			);
			setVelocity(playingJump.add(0.0, 0.27, 0.0));
		} else if (playingTicks != 7 && playingTicks != 15 && playingTicks != 23) {
			setVelocity(playingJump.x, velocity.y, playingJump.z);
		} else {
			setVelocity(0.0, isOnGround() ? 0.27 : velocity.y, 0.0);
		}
	}

	private void sneeze() {
		Vec3d velocity = getVelocity();
		World world = getEntityWorld();
		world.addParticleClient(
			ParticleTypes.SNEEZE,
			getX() - (getWidth() + 1.0F) * 0.5 * MathHelper.sin(bodyYaw * (float) (Math.PI / 180.0)),
			getEyeY() - 0.1F,
			getZ() + (getWidth() + 1.0F) * 0.5 * MathHelper.cos(bodyYaw * (float) (Math.PI / 180.0)),
			velocity.x,
			0.0,
			velocity.z
		);
		playSound(SoundEvents.ENTITY_PANDA_SNEEZE, 1.0F, 1.0F);

		for (PandaEntity nearby : world.getNonSpectatingEntities(PandaEntity.class, getBoundingBox().expand(10.0))) {
			if (!nearby.isBaby() && nearby.isOnGround() && !nearby.isTouchingWater() && nearby.isIdle()) {
				nearby.jump();
			}
		}

		if (world instanceof ServerWorld serverWorld && serverWorld.getGameRules().getValue(GameRules.DO_MOB_LOOT)) {
			forEachGiftedItem(serverWorld, LootTables.PANDA_SNEEZE_GAMEPLAY, this::dropStack);
		}
	}

	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		if (getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() && canEatFromGround(itemEntity)) {
			triggerItemPickedUpByEntityCriteria(itemEntity);
			ItemStack stack = itemEntity.getStack();
			equipStack(EquipmentSlot.MAINHAND, stack);
			setDropGuaranteed(EquipmentSlot.MAINHAND);
			sendPickup(itemEntity, stack.getCount());
			itemEntity.discard();
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		setSitting(false);
		return super.damage(world, source, amount);
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Random spawnRandom = world.getRandom();
		setMainGene(PandaEntity.Gene.createRandom(spawnRandom));
		setHiddenGene(PandaEntity.Gene.createRandom(spawnRandom));
		resetAttributes();
		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(0.2F);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Инициализирует гены панды на основе генов родителей.
	 * Если отец отсутствует, один ген берётся от матери, второй — случайный.
	 * С вероятностью 1/32 каждый ген может мутировать в случайный.
	 *
	 * @param mother мать панды
	 * @param father отец панды (может быть null при спавне без пары)
	 */
	public void initGenes(PandaEntity mother, @Nullable PandaEntity father) {
		if (father == null) {
			if (random.nextBoolean()) {
				setMainGene(mother.getRandomGene());
				setHiddenGene(PandaEntity.Gene.createRandom(random));
			} else {
				setMainGene(PandaEntity.Gene.createRandom(random));
				setHiddenGene(mother.getRandomGene());
			}
		} else if (random.nextBoolean()) {
			setMainGene(mother.getRandomGene());
			setHiddenGene(father.getRandomGene());
		} else {
			setMainGene(father.getRandomGene());
			setHiddenGene(mother.getRandomGene());
		}

		if (random.nextInt(MAIN_GENE_MUTATION_CHANCE) == 0) {
			setMainGene(PandaEntity.Gene.createRandom(random));
		}

		if (random.nextInt(HIDDEN_GENE_MUTATION_CHANCE) == 0) {
			setHiddenGene(PandaEntity.Gene.createRandom(random));
		}
	}

	private PandaEntity.Gene getRandomGene() {
		return random.nextBoolean() ? getMainGene() : getHiddenGene();
	}

	/**
	 * Применяет модификаторы атрибутов в зависимости от продуктового гена.
	 * WEAK снижает максимальное здоровье до 10, LAZY снижает скорость передвижения.
	 */
	public void resetAttributes() {
		if (isWeak()) {
			getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(10.0);
		}

		if (isLazy()) {
			getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.07F);
		}
	}

	void stop() {
		if (isTouchingWater()) {
			return;
		}

		setForwardSpeed(0.0F);
		getNavigation().stop();
		setSitting(true);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (isScaredByThunderstorm()) {
			return ActionResult.PASS;
		}

		if (isLyingOnBack()) {
			setLyingOnBack(false);
			return ActionResult.SUCCESS;
		}

		if (!isBreedingItem(stack)) {
			return ActionResult.PASS;
		}

		if (getTarget() != null) {
			shouldGetRevenge = true;
		}

		if (isBaby()) {
			eat(player, hand, stack);
			growUp((int) (-getBreedingAge() / 20 * 0.1F), true);
			return ActionResult.SUCCESS;
		}

		if (!getEntityWorld().isClient() && getBreedingAge() == 0 && canEat()) {
			eat(player, hand, stack);
			lovePlayer(player);
			return ActionResult.SUCCESS;
		}

		if (!(getEntityWorld() instanceof ServerWorld serverWorld) || isSitting() || isTouchingWater()) {
			return ActionResult.PASS;
		}

		stop();
		setEating(true);
		ItemStack held = getEquippedStack(EquipmentSlot.MAINHAND);
		if (!held.isEmpty() && !player.isInCreativeMode()) {
			dropStack(serverWorld, held);
		}

		equipStack(EquipmentSlot.MAINHAND, new ItemStack(stack.getItem(), 1));
		eat(player, hand, stack);
		return ActionResult.SUCCESS_SERVER;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (isAttacking()) {
			return SoundEvents.ENTITY_PANDA_AGGRESSIVE_AMBIENT;
		}

		return isWorried() ? SoundEvents.ENTITY_PANDA_WORRIED_AMBIENT : SoundEvents.ENTITY_PANDA_AMBIENT;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_PANDA_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.PANDA_FOOD);
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PANDA_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PANDA_HURT;
	}

	public boolean isIdle() {
		return !isLyingOnBack() && !isScaredByThunderstorm() && !isEating() && !isPlaying() && !isSitting();
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	private static boolean canEatFromGround(ItemEntity itemEntity) {
		return itemEntity.getStack().isIn(ItemTags.PANDA_EATS_FROM_GROUND)
			&& itemEntity.isAlive()
			&& !itemEntity.cannotPickup();
	}

	static class AttackGoal extends MeleeAttackGoal {

		private final PandaEntity panda;

		public AttackGoal(PandaEntity panda, double speed, boolean pauseWhenMobIdle) {
			super(panda, speed, pauseWhenMobIdle);
			this.panda = panda;
		}

		@Override
		public boolean canStart() {
			return panda.isIdle() && super.canStart();
		}
	}

	/**
		* Генотип панды. Рецессивные гены (BROWN, WEAK) проявляются только при совпадении
		* главного и скрытого генов. Доминантные гены проявляются всегда.
		*/
	public enum Gene implements StringIdentifiable {
		NORMAL(0, "normal", false),
		LAZY(1, "lazy", false),
		WORRIED(2, "worried", false),
		PLAYFUL(3, "playful", false),
		BROWN(4, "brown", true),
		WEAK(5, "weak", true),
		AGGRESSIVE(6, "aggressive", false);

		public static final Codec<PandaEntity.Gene> CODEC = StringIdentifiable.createCodec(PandaEntity.Gene::values);
		private static final IntFunction<PandaEntity.Gene> BY_ID = ValueLists.createIndexToValueFunction(
			PandaEntity.Gene::getId, values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		private static final int MAX_GENE_ID = 6;

		private final int id;
		private final String name;
		private final boolean recessive;

		Gene(int id, String name, boolean recessive) {
			this.id = id;
			this.name = name;
			this.recessive = recessive;
		}

		public int getId() {
			return id;
		}

		@Override
		public String asString() {
			return name;
		}

		public boolean isRecessive() {
			return recessive;
		}

		/**
		 * Вычисляет продуктовый ген по правилам доминантности.
		 * Рецессивный ген проявляется только если оба гена одинаковы.
		 */
		static PandaEntity.Gene getProductGene(PandaEntity.Gene mainGene, PandaEntity.Gene hiddenGene) {
			return mainGene.isRecessive()
				? (mainGene == hiddenGene ? mainGene : NORMAL)
				: mainGene;
		}

		public static PandaEntity.Gene byId(int id) {
			return BY_ID.apply(id);
		}

		public static PandaEntity.Gene createRandom(Random random) {
			int roll = random.nextInt(16);
			if (roll == 0) {
				return LAZY;
			} else if (roll == 1) {
				return WORRIED;
			} else if (roll == 2) {
				return PLAYFUL;
			} else if (roll == 4) {
				return AGGRESSIVE;
			} else if (roll < 9) {
				return WEAK;
			} else {
				return roll < 11 ? BROWN : NORMAL;
			}
		}
	}

	static class LieOnBackGoal extends Goal {

		private final PandaEntity panda;
		private int nextLieOnBackAge;

		public LieOnBackGoal(PandaEntity panda) {
			this.panda = panda;
		}

		@Override
		public boolean canStart() {
			return nextLieOnBackAge < panda.age
				&& panda.isLazy()
				&& panda.isIdle()
				&& panda.random.nextInt(toGoalTicks(400)) == 1;
		}

		@Override
		public boolean shouldContinue() {
			if (panda.isTouchingWater()) {
				return false;
			}

			if (panda.isLazy() || panda.random.nextInt(toGoalTicks(600)) != 1) {
				return panda.random.nextInt(toGoalTicks(2000)) != 1;
			}

			return false;
		}

		@Override
		public void start() {
			panda.setLyingOnBack(true);
			nextLieOnBackAge = 0;
		}

		@Override
		public void stop() {
			panda.setLyingOnBack(false);
			nextLieOnBackAge = panda.age + 200;
		}
	}

	static class LookAtEntityGoal extends net.minecraft.entity.ai.goal.LookAtEntityGoal {

		private final PandaEntity panda;

		public LookAtEntityGoal(PandaEntity panda, Class<? extends LivingEntity> targetType, float range) {
			super(panda, targetType, range);
			this.panda = panda;
		}

		public void setTarget(LivingEntity target) {
			this.target = target;
		}

		@Override
		public boolean shouldContinue() {
			return target != null && super.shouldContinue();
		}

		@Override
		public boolean canStart() {
			if (mob.getRandom().nextFloat() >= chance) {
				return false;
			}

			if (target == null) {
				ServerWorld serverWorld = getServerWorld(mob);
				if (targetType == PlayerEntity.class) {
					target = serverWorld.getClosestPlayer(
						targetPredicate, mob, mob.getX(), mob.getEyeY(), mob.getZ()
					);
				} else {
					target = serverWorld.getClosestEntity(
						mob.getEntityWorld()
							.getEntitiesByClass(
								targetType,
								mob.getBoundingBox().expand(range, 3.0, range),
								livingEntity -> true
							),
						targetPredicate,
						mob,
						mob.getX(),
						mob.getEyeY(),
						mob.getZ()
					);
				}
			}

			return panda.isIdle() && target != null;
		}

		@Override
		public void tick() {
			if (target != null) {
				super.tick();
			}
		}
	}

	static class PandaEscapeDangerGoal extends EscapeDangerGoal {

		private final PandaEntity panda;

		public PandaEscapeDangerGoal(PandaEntity panda, double speed) {
			super(panda, speed, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
			this.panda = panda;
		}

		@Override
		public boolean shouldContinue() {
			if (panda.isSitting()) {
				panda.getNavigation().stop();
				return false;
			}

			return super.shouldContinue();
		}
	}

	static class PandaFleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final PandaEntity panda;

		public PandaFleeGoal(
			PandaEntity panda,
			Class<T> fleeFromType,
			float distance,
			double slowSpeed,
			double fastSpeed
		) {
			super(panda, fleeFromType, distance, slowSpeed, fastSpeed, EntityPredicates.EXCEPT_SPECTATOR);
			this.panda = panda;
		}

		@Override
		public boolean canStart() {
			return panda.isWorried() && panda.isIdle() && super.canStart();
		}
	}

	static class PandaMateGoal extends AnimalMateGoal {

		private final PandaEntity panda;
		private int nextAskPlayerForBambooAge;

		public PandaMateGoal(PandaEntity panda, double chance) {
			super(panda, chance);
			this.panda = panda;
		}

		@Override
		public boolean canStart() {
			if (!super.canStart() || panda.getAskForBambooTicks() != 0) {
				return false;
			}

			if (isBambooClose()) {
				return true;
			}

			if (nextAskPlayerForBambooAge <= panda.age) {
				panda.setAskForBambooTicks(32);
				nextAskPlayerForBambooAge = panda.age + 600;
				if (panda.canActVoluntarily()) {
					PlayerEntity nearestPlayer = world.getClosestPlayer(PandaEntity.ASK_FOR_BAMBOO_TARGET, panda);
					panda.lookAtPlayerGoal.setTarget(nearestPlayer);
				}
			}

			return false;
		}

		/**
		 * Проверяет наличие бамбука в радиусе 8 блоков по горизонтали и 3 блоков по вертикали.
		 * Использует спиральный обход координат для равномерного покрытия области.
		 */
		private boolean isBambooClose() {
			BlockPos center = panda.getBlockPos();
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (int y = 0; y < BAMBOO_SEARCH_HEIGHT; y++) {
				for (int radius = 0; radius < BAMBOO_SEARCH_RADIUS; radius++) {
					for (int dx = 0; dx <= radius; dx = dx > 0 ? -dx : 1 - dx) {
						for (int dz = dx < radius && dx > -radius ? radius : 0; dz <= radius; dz = dz > 0 ? -dz : 1 - dz) {
							mutable.set(center, dx, y, dz);
							if (world.getBlockState(mutable).isOf(Blocks.BAMBOO)) {
								return true;
							}
						}
					}
				}
			}

			return false;
		}
	}

	static class PandaMoveControl extends MoveControl {

		private final PandaEntity panda;

		public PandaMoveControl(PandaEntity panda) {
			super(panda);
			this.panda = panda;
		}

		@Override
		public void tick() {
			if (panda.isIdle()) {
				super.tick();
			}
		}
	}

	static class PandaRevengeGoal extends RevengeGoal {

		private final PandaEntity panda;

		public PandaRevengeGoal(PandaEntity panda, Class<?>... noRevengeTypes) {
			super(panda, noRevengeTypes);
			this.panda = panda;
		}

		@Override
		public boolean shouldContinue() {
			if (panda.shouldGetRevenge || panda.shouldAttack) {
				panda.setTarget(null);
				return false;
			}

			return super.shouldContinue();
		}

		@Override
		protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
			if (mob instanceof PandaEntity pandaMob && pandaMob.isAttacking()) {
				mob.setTarget(target);
			}
		}
	}

	class PickUpFoodGoal extends Goal {

		private int startAge;

		public PickUpFoodGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (startAge > PandaEntity.this.age
				|| PandaEntity.this.isBaby()
				|| PandaEntity.this.isTouchingWater()
				|| !PandaEntity.this.isIdle()
				|| PandaEntity.this.getAskForBambooTicks() > 0
			) {
				return false;
			}

			if (!PandaEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
				return true;
			}

			return !PandaEntity.this.getEntityWorld()
				.getEntitiesByClass(
					ItemEntity.class,
					PandaEntity.this.getBoundingBox().expand(6.0, 6.0, 6.0),
					PandaEntity::canEatFromGround
				)
				.isEmpty();
		}

		@Override
		public boolean shouldContinue() {
			if (PandaEntity.this.isTouchingWater()) {
				return false;
			}

			if (PandaEntity.this.isLazy() || PandaEntity.this.random.nextInt(toGoalTicks(600)) != 1) {
				return PandaEntity.this.random.nextInt(toGoalTicks(2000)) != 1;
			}

			return false;
		}

		@Override
		public void tick() {
			if (!PandaEntity.this.isSitting() && !PandaEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
				PandaEntity.this.stop();
			}
		}

		@Override
		public void start() {
			if (PandaEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
				List<ItemEntity> nearbyFood = PandaEntity.this.getEntityWorld()
					.getEntitiesByClass(
						ItemEntity.class,
						PandaEntity.this.getBoundingBox().expand(8.0, 8.0, 8.0),
						PandaEntity::canEatFromGround
					);
				if (!nearbyFood.isEmpty()) {
					PandaEntity.this.getNavigation().startMovingTo(nearbyFood.getFirst(), 1.2F);
				}
			} else {
				PandaEntity.this.stop();
			}

			startAge = 0;
		}

		@Override
		public void stop() {
			ItemStack held = PandaEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
			if (!held.isEmpty()) {
				PandaEntity.this.dropStack(castToServerWorld(PandaEntity.this.getEntityWorld()), held);
				PandaEntity.this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				int cooldownTicks = PandaEntity.this.isLazy()
					? PandaEntity.this.random.nextInt(50) + 10
					: PandaEntity.this.random.nextInt(150) + 10;
				startAge = PandaEntity.this.age + cooldownTicks * 20;
			}

			PandaEntity.this.setSitting(false);
		}
	}

	static class PlayGoal extends Goal {

		private final PandaEntity panda;

		public PlayGoal(PandaEntity panda) {
			this.panda = panda;
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
		}

		@Override
		public boolean canStart() {
			if ((!panda.isBaby() && !panda.isPlayful()) || !panda.isOnGround() || !panda.isIdle()) {
				return false;
			}

			float yawRad = panda.getYaw() * (float) (Math.PI / 180.0);
			float sinYaw = -MathHelper.sin(yawRad);
			float cosYaw = MathHelper.cos(yawRad);
			int dx = Math.abs(sinYaw) > 0.5 ? MathHelper.sign(sinYaw) : 0;
			int dz = Math.abs(cosYaw) > 0.5 ? MathHelper.sign(cosYaw) : 0;

			if (panda.getEntityWorld().getBlockState(panda.getBlockPos().add(dx, -1, dz)).isAir()) {
				return true;
			}

			return panda.isPlayful()
				? panda.random.nextInt(toGoalTicks(60)) == 1
				: panda.random.nextInt(toGoalTicks(500)) == 1;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void start() {
			panda.setPlaying(true);
		}

		@Override
		public boolean canStop() {
			return false;
		}
	}

	static class SneezeGoal extends Goal {

		private final PandaEntity panda;

		public SneezeGoal(PandaEntity panda) {
			this.panda = panda;
		}

		@Override
		public boolean canStart() {
			if (!panda.isBaby() || !panda.isIdle()) {
				return false;
			}

			return panda.isWeak()
				? panda.random.nextInt(toGoalTicks(500)) == 1
				: panda.random.nextInt(toGoalTicks(6000)) == 1;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void start() {
			panda.setSneezing(true);
		}
	}
}

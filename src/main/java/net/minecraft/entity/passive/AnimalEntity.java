package net.minecraft.entity.passive;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Базовый класс для всех животных, поддерживающих размножение.
 * Управляет системой «влюблённости» (love ticks) и логикой спаривания.
 */
public abstract class AnimalEntity extends PassiveEntity {

	protected static final int BREEDING_COOLDOWN = 6000;
	private static final int LOVE_TICKS_DURATION = 600;
	private static final int LOVE_PARTICLE_INTERVAL = 10;
	private static final int BREED_STATUS_ID = 18;
	private static final int BREED_PARTICLE_COUNT = 7;
	private static final int MIN_LIGHT_LEVEL_FOR_SPAWN = 8;
	private static final int MAX_BREEDING_XP = 7;

	private int loveTicks = 0;
	private @Nullable LazyEntityReference<ServerPlayerEntity> lovingPlayer;

	protected AnimalEntity(EntityType<? extends AnimalEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
	}

	public static DefaultAttributeContainer.Builder createAnimalAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.TEMPT_RANGE, 10.0);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		if (getBreedingAge() != 0) {
			loveTicks = 0;
		}

		super.mobTick(world);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getBreedingAge() != 0) {
			loveTicks = 0;
		}

		if (loveTicks > 0) {
			loveTicks--;
			if (loveTicks % LOVE_PARTICLE_INTERVAL == 0) {
				double vx = random.nextGaussian() * 0.02;
				double vy = random.nextGaussian() * 0.02;
				double vz = random.nextGaussian() * 0.02;
				getEntityWorld().addParticleClient(
					ParticleTypes.HEART,
					getParticleX(1.0),
					getRandomBodyY() + 0.5,
					getParticleZ(1.0),
					vx,
					vy,
					vz
				);
			}
		}
	}

	@Override
	protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
		resetLoveTicks();
		super.applyDamage(world, source, amount);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK) ? 10.0F : world.getPhototaxisFavor(pos);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("InLove", loveTicks);
		LazyEntityReference.writeData(lovingPlayer, view, "LoveCause");
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		loveTicks = view.getInt("InLove", 0);
		lovingPlayer = LazyEntityReference.fromData(view, "LoveCause");
	}

	public static boolean isValidNaturalSpawn(
		EntityType<? extends AnimalEntity> type,
		WorldAccess world,
		SpawnReason spawnReason,
		BlockPos pos,
		Random random
	) {
		boolean lightValid = SpawnReason.isTrialSpawner(spawnReason) || isLightLevelValidForNaturalSpawn(world, pos);
		return world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON) && lightValid;
	}

	protected static boolean isLightLevelValidForNaturalSpawn(BlockRenderView world, BlockPos pos) {
		return world.getBaseLightLevel(pos, 0) > MIN_LIGHT_LEVEL_FOR_SPAWN;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 120;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return 1 + random.nextInt(3);
	}

	public abstract boolean isBreedingItem(ItemStack stack);

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack heldStack = player.getStackInHand(hand);
		if (!isBreedingItem(heldStack)) {
			return super.interactMob(player, hand);
		}

		int age = getBreedingAge();
		if (player instanceof ServerPlayerEntity serverPlayer && age == 0 && canEat()) {
			eat(player, hand, heldStack);
			lovePlayer(serverPlayer);
			playEatSound();
			return ActionResult.SUCCESS_SERVER;
		}

		if (isBaby()) {
			eat(player, hand, heldStack);
			growUp(toGrowUpAge(-age), true);
			playEatSound();
			return ActionResult.SUCCESS;
		}

		if (getEntityWorld().isClient()) {
			return ActionResult.CONSUME;
		}

		return super.interactMob(player, hand);
	}

	protected void playEatSound() {
	}

	public boolean canEat() {
		return loveTicks <= 0;
	}

	/**
	 * Переводит животное в режим «влюблённости», запуская таймер размножения.
	 *
	 * @param player игрок, который покормил животное (может быть {@code null})
	 */
	public void lovePlayer(@Nullable PlayerEntity player) {
		loveTicks = LOVE_TICKS_DURATION;
		if (player instanceof ServerPlayerEntity serverPlayer) {
			lovingPlayer = LazyEntityReference.of(serverPlayer);
		}

		getEntityWorld().sendEntityStatus(this, (byte) BREED_STATUS_ID);
	}

	public void setLoveTicks(int loveTicks) {
		this.loveTicks = loveTicks;
	}

	public int getLoveTicks() {
		return loveTicks;
	}

	public @Nullable ServerPlayerEntity getLovingPlayer() {
		return LazyEntityReference.resolve(lovingPlayer, getEntityWorld(), ServerPlayerEntity.class);
	}

	public boolean isInLove() {
		return loveTicks > 0;
	}

	public void resetLoveTicks() {
		loveTicks = 0;
	}

	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}

		return other.getClass() == getClass() && isInLove() && other.isInLove();
	}

	/**
	 * Создаёт детёныша и спавнит его в мире.
	 * Вызывается целевой целью размножения после того, как оба родителя «влюблены».
	 */
	public void breed(ServerWorld world, AnimalEntity other) {
		PassiveEntity baby = createChild(world, other);
		if (baby == null) {
			return;
		}

		baby.setBaby(true);
		baby.refreshPositionAndAngles(getX(), getY(), getZ(), 0.0F, 0.0F);
		breed(world, other, baby);
		world.spawnEntityAndPassengers(baby);
	}

	/**
	 * Обрабатывает результат размножения: выдаёт достижения, сбрасывает таймеры,
	 * спавнит опыт.
	 *
	 * @param world серверный мир
	 * @param other второй родитель
	 * @param baby  детёныш (может быть {@code null} если создание не удалось)
	 */
	public void breed(ServerWorld world, AnimalEntity other, @Nullable PassiveEntity baby) {
		Optional
			.ofNullable(getLovingPlayer())
			.or(() -> Optional.ofNullable(other.getLovingPlayer()))
			.ifPresent(player -> {
				player.incrementStat(Stats.ANIMALS_BRED);
				Criteria.BRED_ANIMALS.trigger(player, this, other, baby);
			});

		setBreedingAge(BREEDING_COOLDOWN);
		other.setBreedingAge(BREEDING_COOLDOWN);
		resetLoveTicks();
		other.resetLoveTicks();
		world.sendEntityStatus(this, (byte) BREED_STATUS_ID);

		if (world.getGameRules().getValue(GameRules.DO_MOB_LOOT)) {
			world.spawnEntity(new ExperienceOrbEntity(
				world,
				getX(),
				getY(),
				getZ(),
				random.nextInt(MAX_BREEDING_XP) + 1
			));
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status != BREED_STATUS_ID) {
			super.handleStatus(status);
			return;
		}

		for (int count = 0; count < BREED_PARTICLE_COUNT; count++) {
			double vx = random.nextGaussian() * 0.02;
			double vy = random.nextGaussian() * 0.02;
			double vz = random.nextGaussian() * 0.02;
			getEntityWorld().addParticleClient(
				ParticleTypes.HEART,
				getParticleX(1.0),
				getRandomBodyY() + 0.5,
				getParticleZ(1.0),
				vx,
				vy,
				vz
			);
		}
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Direction direction = getMovementDirection();
		if (direction.getAxis() == Direction.Axis.Y) {
			return super.updatePassengerForDismount(passenger);
		}

		int[][] dismountOffsets = Dismounting.getDismountOffsets(direction);
		BlockPos blockPos = getBlockPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (EntityPose entityPose : passenger.getPoses()) {
			Box box = passenger.getBoundingBox(entityPose);

			for (int[] offset : dismountOffsets) {
				mutable.set(blockPos.getX() + offset[0], blockPos.getY(), blockPos.getZ() + offset[1]);
				double dismountHeight = getEntityWorld().getDismountHeight(mutable);
				if (Dismounting.canDismountInBlock(dismountHeight)) {
					Vec3d dismountPos = Vec3d.ofCenter(mutable, dismountHeight);
					if (Dismounting.canPlaceEntityAt(getEntityWorld(), passenger, box.offset(dismountPos))) {
						passenger.setPose(entityPose);
						return dismountPos;
					}
				}
			}
		}

		return super.updatePassengerForDismount(passenger);
	}
}

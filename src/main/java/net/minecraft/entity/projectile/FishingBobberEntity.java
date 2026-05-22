package net.minecraft.entity.projectile;

import com.mojang.logging.LogUtils;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Поплавок удочки — снаряд, управляющий всей логикой рыбалки.
 * <p>
 * Проходит три состояния: {@link State#FLYING} (полёт после броска),
 * {@link State#HOOKED_IN_ENTITY} (зацеплена сущность) и {@link State#BOBBING}
 * (покачивание на воде с ожиданием поклёвки). Логика поклёвки реализована в
 * {@link #tickFishingLogic}, генерация лута — через таблицу {@link LootTables#FISHING_GAMEPLAY}.
 */
public class FishingBobberEntity extends ProjectileEntity {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final TrackedData<Integer> HOOK_ENTITY_ID =
			DataTracker.registerData(FishingBobberEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> CAUGHT_FISH =
			DataTracker.registerData(FishingBobberEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Максимальное количество тиков вне открытой воды до потери статуса «открытой воды». */
	private static final int MAX_OUT_OF_WATER_TICKS = 10;

	/** Максимальное количество тиков на земле до автоматического уничтожения поплавка. */
	private static final int MAX_GROUND_TICKS = 1200;

	/** Статус-байт для клиента: подтянуть зацепленную сущность. */
	private static final byte PULL_STATUS = 31;

	/** Вероятность появления эндермита при телепортации через жемчуг (не используется здесь, но для контекста). */
	private static final float RAIN_SPEED_BONUS_CHANCE = 0.25F;
	private static final float SKY_SPEED_PENALTY_CHANCE = 0.5F;

	/** Коэффициент замедления скорости поплавка на воде. */
	private static final double BOBBING_VELOCITY_DAMPING = 0.9;

	/** Коэффициент случайного вертикального смещения при покачивании. */
	private static final float BOBBING_VERTICAL_RANDOM_FACTOR = 0.2F;

	/** Порог «почти нулевого» смещения при покачивании. */
	private static final double BOBBING_SNAP_THRESHOLD = 0.01;

	/** Коррекция знака при нулевом смещении. */
	private static final double BOBBING_SNAP_CORRECTION = 0.1;

	/** Коэффициент замедления скорости поплавка в воздухе. */
	private static final double AIR_VELOCITY_DAMPING = 0.92;

	/** Гравитация поплавка в воздухе (вне воды). */
	private static final double AIR_GRAVITY = 0.03;

	/** Коэффициент замедления скорости поплавка при покачивании. */
	private static final float CAUGHT_FISH_PULL_FACTOR = 0.1F;

	/** Максимальная дистанция до игрока, при которой поплавок не уничтожается. */
	private static final double MAX_PLAYER_DISTANCE_SQUARED = 1024.0;

	/** Коэффициент притяжения зацепленной сущности к игроку. */
	private static final double PULL_VELOCITY_FACTOR = 0.1;

	/** Дополнительный вертикальный импульс при подтягивании. */
	private static final double PULL_VERTICAL_BOOST_FACTOR = 0.08;

	/** Базовая вероятность поклёвки при ожидании. */
	private static final float BITE_BASE_CHANCE = 0.15F;

	/** Диапазон ожидания поклёвки (в тиках). */
	private static final int WAIT_MIN = 100;
	private static final int WAIT_MAX = 600;

	/** Диапазон путешествия рыбы к поплавку (в тиках). */
	private static final int TRAVEL_MIN = 20;
	private static final int TRAVEL_MAX = 80;

	/** Диапазон задержки поклёвки (в тиках). */
	private static final int HOOK_MIN = 20;
	private static final int HOOK_MAX = 40;

	/** Угловой разброс направления рыбы (в градусах). */
	private static final double FISH_ANGLE_SPREAD = 9.188;

	/** Масштаб скорости рыбы к поплавку. */
	private static final float FISH_TRAVEL_SCALE = 0.1F;

	/** Вероятность появления пузырьков при движении рыбы. */
	private static final float BUBBLE_CHANCE = 0.15F;

	/** Масштаб частиц рыбалки. */
	private static final float FISHING_PARTICLE_SCALE = 0.04F;

	/** Диапазон угла появления брызг при ожидании. */
	private static final float SPLASH_ANGLE_MAX = 360.0F;
	private static final float SPLASH_DISTANCE_MIN = 25.0F;
	private static final float SPLASH_DISTANCE_MAX = 60.0F;
	private static final float SPLASH_SCALE = 0.1F;

	private final Random velocityRandom = Random.create();
	private boolean caughtFish;
	private int outOfOpenWaterTicks;
	private int removalTimer;
	private int hookCountdown;
	private int waitCountdown;
	private int fishTravelCountdown;
	private float fishAngle;
	private boolean inOpenWater = true;
	private @Nullable Entity hookedEntity;
	private State state = State.FLYING;
	private final int luckBonus;
	private final int waitTimeReductionTicks;
	private final PositionInterpolator positionInterpolator = new PositionInterpolator(this);

	public FishingBobberEntity(
			EntityType<? extends FishingBobberEntity> type,
			World world,
			int luckBonus,
			int waitTimeReductionTicks
	) {
		super(type, world);
		this.luckBonus = Math.max(0, luckBonus);
		this.waitTimeReductionTicks = Math.max(0, waitTimeReductionTicks);
	}

	public FishingBobberEntity(EntityType<? extends FishingBobberEntity> entityType, World world) {
		this(entityType, world, 0, 0);
	}

	/**
	 * Создаёт поплавок, брошенный игроком, с расчётом начальной скорости по направлению взгляда.
	 *
	 * @param thrower              игрок, бросивший удочку
	 * @param world                мир
	 * @param luckBonus            бонус удачи (от зачарования «Удача моря»)
	 * @param waitTimeReductionTicks уменьшение времени ожидания поклёвки (от зачарования «Приманка»)
	 */
	public FishingBobberEntity(PlayerEntity thrower, World world, int luckBonus, int waitTimeReductionTicks) {
		this(EntityType.FISHING_BOBBER, world, luckBonus, waitTimeReductionTicks);
		setOwner(thrower);

		float pitch = thrower.getPitch();
		float yaw = thrower.getYaw();
		float cosYaw = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float sinYaw = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float cosPitch = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
		float sinPitch = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));

		double spawnX = thrower.getX() - sinYaw * 0.3;
		double spawnY = thrower.getEyeY();
		double spawnZ = thrower.getZ() - cosYaw * 0.3;
		refreshPositionAndAngles(spawnX, spawnY, spawnZ, yaw, pitch);

		Vec3d direction = new Vec3d(-sinYaw, MathHelper.clamp(-(sinPitch / cosPitch), -5.0F, 5.0F), -cosYaw);
		double dirLength = direction.length();
		direction = direction.multiply(
				0.6 / dirLength + random.nextTriangular(0.5, 0.0103365),
				0.6 / dirLength + random.nextTriangular(0.5, 0.0103365),
				0.6 / dirLength + random.nextTriangular(0.5, 0.0103365)
		);
		setVelocity(direction);
		setYaw((float) (MathHelper.atan2(direction.x, direction.z) * 180.0F / (float) Math.PI));
		setPitch((float) (MathHelper.atan2(direction.y, direction.horizontalLength()) * 180.0F / (float) Math.PI));
		lastYaw = getYaw();
		lastPitch = getPitch();
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return positionInterpolator;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(HOOK_ENTITY_ID, 0);
		builder.add(CAUGHT_FISH, false);
	}

	@Override
	protected boolean deflectsAgainstWorldBorder() {
		return true;
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (HOOK_ENTITY_ID.equals(data)) {
			int entityId = getDataTracker().get(HOOK_ENTITY_ID);
			hookedEntity = entityId > 0 ? getEntityWorld().getEntityById(entityId - 1) : null;
		}

		if (CAUGHT_FISH.equals(data)) {
			caughtFish = getDataTracker().get(CAUGHT_FISH);
			if (caughtFish) {
				setVelocity(
						getVelocity().x,
						-0.4F * MathHelper.nextFloat(velocityRandom, 0.6F, 1.0F),
						getVelocity().z
				);
			}
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 4096.0;
	}

	@Override
	public void tick() {
		velocityRandom.setSeed(getUuid().getLeastSignificantBits() ^ getEntityWorld().getTime());
		getInterpolator().tick();
		super.tick();

		PlayerEntity player = getPlayerOwner();
		if (player == null) {
			discard();
			return;
		}

		if (getEntityWorld().isClient() || !removeIfInvalid(player)) {
			tickGroundTimer();
			tickStateLogic();
		}
	}

	private void tickGroundTimer() {
		if (isOnGround()) {
			removalTimer++;
			if (removalTimer >= MAX_GROUND_TICKS) {
				discard();
			}
		} else {
			removalTimer = 0;
		}
	}

	private void tickStateLogic() {
		BlockPos blockPos = getBlockPos();
		FluidState fluidState = getEntityWorld().getFluidState(blockPos);
		float waterHeight = fluidState.isIn(FluidTags.WATER) ? fluidState.getHeight(getEntityWorld(), blockPos) : 0.0F;
		boolean inWater = waterHeight > 0.0F;

		switch (state) {
			case FLYING -> tickFlying(inWater);
			case HOOKED_IN_ENTITY -> tickHookedInEntity();
			case BOBBING -> tickBobbing(blockPos, fluidState, inWater, waterHeight);
		}

		if (!fluidState.isIn(FluidTags.WATER) && !isOnGround() && hookedEntity == null) {
			setVelocity(getVelocity().add(0.0, -AIR_GRAVITY, 0.0));
		}

		move(MovementType.SELF, getVelocity());
		tickBlockCollision();
		updateRotation();

		if (state == State.FLYING && (isOnGround() || horizontalCollision)) {
			setVelocity(Vec3d.ZERO);
		}

		setVelocity(getVelocity().multiply(AIR_VELOCITY_DAMPING));
		refreshPosition();
	}

	private void tickFlying(boolean inWater) {
		if (hookedEntity != null) {
			setVelocity(Vec3d.ZERO);
			state = State.HOOKED_IN_ENTITY;
			return;
		}

		if (inWater) {
			setVelocity(getVelocity().multiply(0.3, 0.2, 0.3));
			state = State.BOBBING;
			return;
		}

		checkForCollision();
	}

	private void tickHookedInEntity() {
		if (hookedEntity == null) {
			return;
		}

		boolean entityValid = !hookedEntity.isRemoved()
				&& hookedEntity.isInteractable()
				&& hookedEntity.getEntityWorld().getRegistryKey() == getEntityWorld().getRegistryKey();

		if (entityValid) {
			setPosition(hookedEntity.getX(), hookedEntity.getBodyY(0.8), hookedEntity.getZ());
		} else {
			updateHookedEntityId(null);
			state = State.FLYING;
		}
	}

	private void tickBobbing(BlockPos blockPos, FluidState fluidState, boolean inWater, float waterHeight) {
		Vec3d velocity = getVelocity();
		double verticalOffset = getY() + velocity.y - blockPos.getY() - waterHeight;

		if (Math.abs(verticalOffset) < BOBBING_SNAP_THRESHOLD) {
			verticalOffset += Math.signum(verticalOffset) * BOBBING_SNAP_CORRECTION;
		}

		setVelocity(
				velocity.x * BOBBING_VELOCITY_DAMPING,
				velocity.y - verticalOffset * random.nextFloat() * BOBBING_VERTICAL_RANDOM_FACTOR,
				velocity.z * BOBBING_VELOCITY_DAMPING
		);

		if (hookCountdown <= 0 && fishTravelCountdown <= 0) {
			inOpenWater = true;
		} else {
			inOpenWater = inOpenWater
					&& outOfOpenWaterTicks < MAX_OUT_OF_WATER_TICKS
					&& isOpenOrWaterAround(blockPos);
		}

		if (inWater) {
			outOfOpenWaterTicks = Math.max(0, outOfOpenWaterTicks - 1);
			if (caughtFish) {
				setVelocity(getVelocity().add(
						0.0,
						-CAUGHT_FISH_PULL_FACTOR * velocityRandom.nextFloat() * velocityRandom.nextFloat(),
						0.0
				));
			}

			if (!getEntityWorld().isClient()) {
				tickFishingLogic(blockPos);
			}
		} else {
			outOfOpenWaterTicks = Math.min(MAX_OUT_OF_WATER_TICKS, outOfOpenWaterTicks + 1);
		}
	}

	private boolean removeIfInvalid(PlayerEntity player) {
		if (player.isInteractable()) {
			ItemStack mainHand = player.getMainHandStack();
			ItemStack offHand = player.getOffHandStack();
			boolean hasFishingRod = mainHand.isOf(Items.FISHING_ROD) || offHand.isOf(Items.FISHING_ROD);

			if (hasFishingRod && squaredDistanceTo(player) <= MAX_PLAYER_DISTANCE_SQUARED) {
				return false;
			}
		}

		discard();
		return true;
	}

	private void checkForCollision() {
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		hitOrDeflect(hitResult);
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) || entity.isAlive() && entity instanceof ItemEntity;
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!getEntityWorld().isClient()) {
			updateHookedEntityId(entityHitResult.getEntity());
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		setVelocity(getVelocity().normalize().multiply(blockHitResult.squaredDistanceTo(this)));
	}

	private void updateHookedEntityId(@Nullable Entity entity) {
		hookedEntity = entity;
		getDataTracker().set(HOOK_ENTITY_ID, entity == null ? 0 : entity.getId() + 1);
	}

	/**
	 * Основная логика рыбалки: управляет таймерами ожидания, путешествия рыбы и поклёвки.
	 * Вызывается только на сервере, пока поплавок находится в воде.
	 *
	 * @param pos позиция поплавка в мире
	 */
	private void tickFishingLogic(BlockPos pos) {
		ServerWorld serverWorld = (ServerWorld) getEntityWorld();
		int speedModifier = 1;
		BlockPos abovePos = pos.up();

		if (random.nextFloat() < RAIN_SPEED_BONUS_CHANCE && getEntityWorld().hasRain(abovePos)) {
			speedModifier++;
		}

		if (random.nextFloat() < SKY_SPEED_PENALTY_CHANCE && !getEntityWorld().isSkyVisible(abovePos)) {
			speedModifier--;
		}

		if (hookCountdown > 0) {
			hookCountdown--;
			if (hookCountdown <= 0) {
				waitCountdown = 0;
				fishTravelCountdown = 0;
				getDataTracker().set(CAUGHT_FISH, false);
			}
		} else if (fishTravelCountdown > 0) {
			tickFishTravel(serverWorld, speedModifier);
		} else if (waitCountdown > 0) {
			tickWaiting(serverWorld, speedModifier);
		} else {
			waitCountdown = MathHelper.nextInt(random, WAIT_MIN, WAIT_MAX) - waitTimeReductionTicks;
		}
	}

	private void tickFishTravel(ServerWorld serverWorld, int speedModifier) {
		fishTravelCountdown -= speedModifier;

		if (fishTravelCountdown > 0) {
			fishAngle += (float) random.nextTriangular(0.0, FISH_ANGLE_SPREAD);
			float angleRad = fishAngle * (float) (Math.PI / 180.0);
			float sinAngle = MathHelper.sin(angleRad);
			float cosAngle = MathHelper.cos(angleRad);
			double particleX = getX() + sinAngle * fishTravelCountdown * FISH_TRAVEL_SCALE;
			double particleY = MathHelper.floor(getY()) + 1.0F;
			double particleZ = getZ() + cosAngle * fishTravelCountdown * FISH_TRAVEL_SCALE;
			BlockState blockState = serverWorld.getBlockState(BlockPos.ofFloored(particleX, particleY - 1.0, particleZ));

			if (blockState.isOf(Blocks.WATER)) {
				if (random.nextFloat() < BUBBLE_CHANCE) {
					serverWorld.spawnParticles(
							ParticleTypes.BUBBLE,
							particleX,
							particleY - 0.1F,
							particleZ,
							1,
							sinAngle,
							0.1,
							cosAngle,
							0.0
					);
				}

				float particleOffsetX = sinAngle * FISHING_PARTICLE_SCALE;
				float particleOffsetZ = cosAngle * FISHING_PARTICLE_SCALE;
				serverWorld.spawnParticles(ParticleTypes.FISHING, particleX, particleY, particleZ, 0, particleOffsetZ, 0.01, -particleOffsetX, 1.0);
				serverWorld.spawnParticles(ParticleTypes.FISHING, particleX, particleY, particleZ, 0, -particleOffsetZ, 0.01, particleOffsetX, 1.0);
			}
		} else {
			playSound(
					SoundEvents.ENTITY_FISHING_BOBBER_SPLASH,
					0.25F,
					1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F
			);
			double splashY = getY() + 0.5;
			int particleCount = (int) (1.0F + getWidth() * 20.0F);
			serverWorld.spawnParticles(ParticleTypes.BUBBLE, getX(), splashY, getZ(), particleCount, getWidth(), 0.0, getWidth(), 0.2F);
			serverWorld.spawnParticles(ParticleTypes.FISHING, getX(), splashY, getZ(), particleCount, getWidth(), 0.0, getWidth(), 0.2F);
			hookCountdown = MathHelper.nextInt(random, HOOK_MIN, HOOK_MAX);
			getDataTracker().set(CAUGHT_FISH, true);
		}
	}

	private void tickWaiting(ServerWorld serverWorld, int speedModifier) {
		waitCountdown -= speedModifier;
		float biteChance = BITE_BASE_CHANCE;

		if (waitCountdown < 20) {
			biteChance += (20 - waitCountdown) * 0.05F;
		} else if (waitCountdown < 40) {
			biteChance += (40 - waitCountdown) * 0.02F;
		} else if (waitCountdown < 60) {
			biteChance += (60 - waitCountdown) * 0.01F;
		}

		if (random.nextFloat() < biteChance) {
			float splashAngle = MathHelper.nextFloat(random, 0.0F, SPLASH_ANGLE_MAX) * (float) (Math.PI / 180.0);
			float splashDist = MathHelper.nextFloat(random, SPLASH_DISTANCE_MIN, SPLASH_DISTANCE_MAX);
			double splashX = getX() + MathHelper.sin(splashAngle) * splashDist * SPLASH_SCALE;
			double splashY = MathHelper.floor(getY()) + 1.0F;
			double splashZ = getZ() + MathHelper.cos(splashAngle) * splashDist * SPLASH_SCALE;
			BlockState blockState = serverWorld.getBlockState(BlockPos.ofFloored(splashX, splashY - 1.0, splashZ));

			if (blockState.isOf(Blocks.WATER)) {
				serverWorld.spawnParticles(
						ParticleTypes.SPLASH,
						splashX,
						splashY,
						splashZ,
						2 + random.nextInt(2),
						0.1F,
						0.0,
						0.1F,
						0.0
				);
			}
		}

		if (waitCountdown <= 0) {
			fishAngle = MathHelper.nextFloat(random, 0.0F, SPLASH_ANGLE_MAX);
			fishTravelCountdown = MathHelper.nextInt(random, TRAVEL_MIN, TRAVEL_MAX);
		}
	}

	private boolean isOpenOrWaterAround(BlockPos pos) {
		PositionType positionType = PositionType.INVALID;

		for (int yOffset = -1; yOffset <= 2; yOffset++) {
			PositionType layerType = getPositionType(pos.add(-2, yOffset, -2), pos.add(2, yOffset, 2));
			switch (layerType) {
				case ABOVE_WATER:
					if (positionType == PositionType.INVALID) {
						return false;
					}
					break;
				case INSIDE_WATER:
					if (positionType == PositionType.ABOVE_WATER) {
						return false;
					}
					break;
				case INVALID:
					return false;
			}

			positionType = layerType;
		}

		return true;
	}

	private PositionType getPositionType(BlockPos start, BlockPos end) {
		return BlockPos.stream(start, end)
				.map(this::getPositionType)
				.reduce((a, b) -> a == b ? a : PositionType.INVALID)
				.orElse(PositionType.INVALID);
	}

	private PositionType getPositionType(BlockPos pos) {
		BlockState blockState = getEntityWorld().getBlockState(pos);
		if (!blockState.isAir() && !blockState.isOf(Blocks.LILY_PAD)) {
			FluidState fluidState = blockState.getFluidState();
			return fluidState.isIn(FluidTags.WATER)
					&& fluidState.isStill()
					&& blockState.getCollisionShape(getEntityWorld(), pos).isEmpty()
					? PositionType.INSIDE_WATER
					: PositionType.INVALID;
		}

		return PositionType.ABOVE_WATER;
	}

	public boolean isInOpenWater() {
		return inOpenWater;
	}

	@Override
	protected void writeCustomData(WriteView view) {
	}

	@Override
	protected void readCustomData(ReadView view) {
	}

	/**
	 * Обрабатывает подтягивание удочки игроком.
	 * <p>
	 * Если зацеплена сущность — подтягивает её к игроку.
	 * Если есть поклёвка — генерирует лут через таблицу {@link LootTables#FISHING_GAMEPLAY}
	 * и бросает предметы в сторону игрока.
	 *
	 * @param usedItem предмет (удочка), которым игрок подтягивает поплавок
	 * @return код результата: 0 — ничего, 1 — лут, 2 — на земле, 3 — предмет, 5 — сущность
	 */
	public int use(ItemStack usedItem) {
		PlayerEntity player = getPlayerOwner();
		if (getEntityWorld().isClient() || player == null || removeIfInvalid(player)) {
			return 0;
		}

		int result = 0;

		if (hookedEntity != null) {
			pullHookedEntity(hookedEntity);
			Criteria.FISHING_ROD_HOOKED.trigger(
					(ServerPlayerEntity) player,
					usedItem,
					this,
					Collections.emptyList()
			);
			getEntityWorld().sendEntityStatus(this, PULL_STATUS);
			result = hookedEntity instanceof ItemEntity ? 3 : 5;
		} else if (hookCountdown > 0) {
			result = generateLoot(player, usedItem);
		}

		if (isOnGround()) {
			result = 2;
		}

		discard();
		return result;
	}

	private int generateLoot(PlayerEntity player, ItemStack usedItem) {
		LootWorldContext lootContext = new LootWorldContext.Builder((ServerWorld) getEntityWorld())
				.add(LootContextParameters.ORIGIN, getEntityPos())
				.add(LootContextParameters.TOOL, usedItem)
				.add(LootContextParameters.THIS_ENTITY, this)
				.luck(luckBonus + player.getLuck())
				.build(LootContextTypes.FISHING);

		LootTable lootTable = getEntityWorld()
				.getServer()
				.getReloadableRegistries()
				.getLootTable(LootTables.FISHING_GAMEPLAY);
		List<ItemStack> loot = lootTable.generateLoot(lootContext);
		Criteria.FISHING_ROD_HOOKED.trigger((ServerPlayerEntity) player, usedItem, this, loot);

		for (ItemStack itemStack : loot) {
			ItemEntity itemEntity = new ItemEntity(
					getEntityWorld(),
					getX(),
					getY(),
					getZ(),
					itemStack
			);
			double deltaX = player.getX() - getX();
			double deltaY = player.getY() - getY();
			double deltaZ = player.getZ() - getZ();
			itemEntity.setVelocity(
					deltaX * PULL_VELOCITY_FACTOR,
					deltaY * PULL_VELOCITY_FACTOR + Math.sqrt(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)) * PULL_VERTICAL_BOOST_FACTOR,
					deltaZ * PULL_VELOCITY_FACTOR
			);
			getEntityWorld().spawnEntity(itemEntity);
			player.getEntityWorld().spawnEntity(new ExperienceOrbEntity(
					player.getEntityWorld(),
					player.getX(),
					player.getY() + 0.5,
					player.getZ() + 0.5,
					random.nextInt(6) + 1
			));

			if (itemStack.isIn(ItemTags.FISHES)) {
				player.increaseStat(Stats.FISH_CAUGHT, 1);
			}
		}

		return 1;
	}

	@Override
	public void handleStatus(byte status) {
		if (status == PULL_STATUS
				&& getEntityWorld().isClient()
				&& hookedEntity instanceof PlayerEntity playerEntity
				&& playerEntity.isMainPlayer()) {
			pullHookedEntity(hookedEntity);
		}

		super.handleStatus(status);
	}

	/**
	 * Подтягивает зацепленную сущность к владельцу поплавка.
	 * Добавляет к скорости сущности вектор, направленный к игроку, умноженный на {@link #PULL_VELOCITY_FACTOR}.
	 *
	 * @param entity зацепленная сущность
	 */
	protected void pullHookedEntity(Entity entity) {
		Entity owner = getOwner();
		if (owner == null) {
			return;
		}

		Vec3d pullVector = new Vec3d(
				owner.getX() - getX(),
				owner.getY() - getY(),
				owner.getZ() - getZ()
		).multiply(PULL_VELOCITY_FACTOR);
		entity.setVelocity(entity.getVelocity().add(pullVector));
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		setPlayerFishHook(null);
		super.remove(reason);
	}

	@Override
	public void onRemoved() {
		setPlayerFishHook(null);
	}

	@Override
	public void setOwner(@Nullable Entity owner) {
		super.setOwner(owner);
		setPlayerFishHook(this);
	}

	private void setPlayerFishHook(@Nullable FishingBobberEntity fishingBobber) {
		PlayerEntity player = getPlayerOwner();
		if (player != null) {
			player.fishHook = fishingBobber;
		}
	}

	public @Nullable PlayerEntity getPlayerOwner() {
		return getOwner() instanceof PlayerEntity player ? player : null;
	}

	public @Nullable Entity getHookedEntity() {
		return hookedEntity;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		Entity owner = getOwner();
		return new EntitySpawnS2CPacket(this, entityTrackerEntry, owner == null ? getId() : owner.getId());
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		if (getPlayerOwner() == null) {
			int ownerId = packet.getEntityData();
			LOGGER.error(
					"Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.",
					getEntityWorld().getEntityById(ownerId),
					ownerId
			);
			discard();
		}
	}

	enum PositionType {
		ABOVE_WATER,
		INSIDE_WATER,
		INVALID
	}

	enum State {
		FLYING,
		HOOKED_IN_ENTITY,
		BOBBING
	}
}

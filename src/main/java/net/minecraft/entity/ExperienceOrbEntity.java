package net.minecraft.entity;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Сущность шара опыта. Притягивается к ближайшему игроку в радиусе 8 блоков,
 * может сливаться с другими шарами того же размера, исчезает через 6000 тиков.
 * При подборе игроком сначала чинит зачарованное снаряжение через
 * {@link net.minecraft.enchantment.EnchantmentHelper#getRepairWithExperience}, затем даёт опыт.
 */
public class ExperienceOrbEntity extends Entity {

	protected static final TrackedData<Integer> VALUE =
		DataTracker.registerData(ExperienceOrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int DESPAWN_AGE = 6000;
	private static final int EXPENSIVE_UPDATE_INTERVAL = 20;
	private static final int PLAYER_TRACKING_RANGE = 8;
	private static final int MERGING_CHANCE_FRACTION = 40;
	private static final double MERGE_SEARCH_RADIUS = 0.5;
	private static final short DEFAULT_HEALTH = 5;
	private static final short DEFAULT_AGE = 0;
	private static final short DEFAULT_VALUE = 0;
	private static final int DEFAULT_COUNT = 1;
	private int orbAge = 0;
	private int health = 5;
	private int pickingCount = 1;
	private @Nullable PlayerEntity target;
	private final PositionInterpolator interpolator = new PositionInterpolator(this);

	public ExperienceOrbEntity(World world, double x, double y, double z, int amount) {
		this(world, new Vec3d(x, y, z), Vec3d.ZERO, amount);
	}

	public ExperienceOrbEntity(World world, Vec3d pos, Vec3d velocity, int amount) {
		this(EntityType.EXPERIENCE_ORB, world);
		setPosition(pos);
		if (!world.isClient()) {
			setYaw(random.nextFloat() * 360.0F);
			Vec3d spawnVelocity = new Vec3d(
				(random.nextDouble() * 0.2 - 0.1) * 2.0,
				random.nextDouble() * 0.2 * 2.0,
				(random.nextDouble() * 0.2 - 0.1) * 2.0
			);
			if (velocity.lengthSquared() > 0.0 && velocity.dotProduct(spawnVelocity) < 0.0) {
				spawnVelocity = spawnVelocity.multiply(-1.0);
			}

			double boundingBoxLength = getBoundingBox().getAverageSideLength();
			setPosition(pos.add(velocity.normalize().multiply(boundingBoxLength * 0.5)));
			setVelocity(spawnVelocity);
			if (!world.isSpaceEmpty(getBoundingBox())) {
				tryMoveToOpenSpace(boundingBoxLength);
			}
		}

		setValue(amount);
	}

	public ExperienceOrbEntity(EntityType<? extends ExperienceOrbEntity> entityType, World world) {
		super(entityType, world);
	}

	protected void tryMoveToOpenSpace(double boundingBoxLength) {
		Vec3d center = getEntityPos().add(0.0, getHeight() / 2.0, 0.0);
		VoxelShape searchShape = VoxelShapes.cuboid(Box.of(center, boundingBoxLength, boundingBoxLength, boundingBoxLength));
		getEntityWorld()
			.findClosestCollision(this, searchShape, center, getWidth(), getHeight(), getWidth())
			.ifPresent(pos -> setPosition(pos.add(0.0, -getHeight() / 2.0, 0.0)));
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(VALUE, 0);
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}

	@Override
	public void tick() {
		interpolator.tick();
		if (firstUpdate && getEntityWorld().isClient()) {
			firstUpdate = false;
			return;
		}

		super.tick();
		boolean isInsideBlock = !getEntityWorld().isSpaceEmpty(getBoundingBox());
		if (isSubmergedIn(FluidTags.WATER)) {
			applyWaterMovement();
		}
		else if (!isInsideBlock) {
			applyGravity();
		}

		if (getEntityWorld().getFluidState(getBlockPos()).isIn(FluidTags.LAVA)) {
			setVelocity(
				(random.nextFloat() - random.nextFloat()) * 0.2F,
				0.2F,
				(random.nextFloat() - random.nextFloat()) * 0.2F
			);
		}

		if (age % EXPENSIVE_UPDATE_INTERVAL == 1) {
			expensiveUpdate();
		}

		moveTowardsPlayer();
		if (target == null && !getEntityWorld().isClient() && isInsideBlock) {
			boolean isNextPosInsideBlock = !getEntityWorld().isSpaceEmpty(getBoundingBox().offset(getVelocity()));
			if (isNextPosInsideBlock) {
				pushOutOfBlocks(
					getX(),
					(getBoundingBox().minY + getBoundingBox().maxY) / 2.0,
					getZ()
				);
				velocityDirty = true;
			}
		}

		double prevVelocityY = getVelocity().y;
		move(MovementType.SELF, getVelocity());
		tickBlockCollision();
		float friction = isOnGround()
			? getEntityWorld().getBlockState(getVelocityAffectingPos()).getBlock().getSlipperiness() * 0.98F
			: 0.98F;

		setVelocity(getVelocity().multiply(friction));
		if (groundCollision && prevVelocityY < -getFinalGravity()) {
			setVelocity(new Vec3d(getVelocity().x, -prevVelocityY * 0.4, getVelocity().z));
		}

		orbAge++;
		if (orbAge >= DESPAWN_AGE) {
			discard();
		}
	}

	private void moveTowardsPlayer() {
		if (target == null || target.isSpectator() || target.squaredDistanceTo(this) > 64.0) {
			PlayerEntity nearest = getEntityWorld().getClosestPlayer(this, PLAYER_TRACKING_RANGE);
			target = (nearest != null && !nearest.isSpectator() && !nearest.isDead()) ? nearest : null;
		}

		if (target == null) {
			return;
		}

		Vec3d toTarget = new Vec3d(
			target.getX() - getX(),
			target.getY() + target.getStandingEyeHeight() / 2.0 - getY(),
			target.getZ() - getZ()
		);
		double distSquared = toTarget.lengthSquared();
		double attractionFactor = 1.0 - Math.sqrt(distSquared) / PLAYER_TRACKING_RANGE;
		setVelocity(getVelocity().add(toTarget.normalize().multiply(attractionFactor * attractionFactor * 0.1)));
	}

	@Override
	public BlockPos getVelocityAffectingPos() {
		return getPosWithYOffset(0.999999F);
	}

	private void expensiveUpdate() {
		if (!(getEntityWorld() instanceof ServerWorld)) {
			return;
		}

		for (ExperienceOrbEntity nearby : getEntityWorld().getEntitiesByType(
			TypeFilter.instanceOf(ExperienceOrbEntity.class),
			getBoundingBox().expand(MERGE_SEARCH_RADIUS),
			this::isMergeable
		)) {
			merge(nearby);
		}
	}

	public static void spawn(ServerWorld world, Vec3d pos, int amount) {
		spawn(world, pos, Vec3d.ZERO, amount);
	}

	public static void spawn(ServerWorld world, Vec3d pos, Vec3d velocity, int amount) {
		while (amount > 0) {
			int i = roundToOrbSize(amount);
			amount -= i;
			if (!wasMergedIntoExistingOrb(world, pos, i)) {
				world.spawnEntity(new ExperienceOrbEntity(world, pos, velocity, i));
			}
		}
	}

	private static boolean wasMergedIntoExistingOrb(ServerWorld world, Vec3d pos, int amount) {
		Box searchBox = Box.of(pos, 1.0, 1.0, 1.0);
		int seed = world.getRandom().nextInt(MERGING_CHANCE_FRACTION);
		List<ExperienceOrbEntity> candidates = world.getEntitiesByType(
			TypeFilter.instanceOf(ExperienceOrbEntity.class),
			searchBox,
			orb -> isMergeable(orb, seed, amount)
		);
		if (candidates.isEmpty()) {
			return false;
		}

		ExperienceOrbEntity target = candidates.get(0);
		target.pickingCount++;
		target.orbAge = 0;
		return true;
	}

	private boolean isMergeable(ExperienceOrbEntity other) {
		return other != this && isMergeable(other, this.getId(), this.getValue());
	}

	private static boolean isMergeable(ExperienceOrbEntity orb, int seed, int amount) {
		return !orb.isRemoved() && (orb.getId() - seed) % MERGING_CHANCE_FRACTION == 0 && orb.getValue() == amount;
	}

	private void merge(ExperienceOrbEntity other) {
		this.pickingCount = this.pickingCount + other.pickingCount;
		this.orbAge = Math.min(this.orbAge, other.orbAge);
		other.discard();
	}

	private void applyWaterMovement() {
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.x * 0.99F, Math.min(vec3d.y + 5.0E-4F, 0.06F), vec3d.z * 0.99F);
	}

	@Override
	protected void onSwimmingStart() {
	}

	@Override
	public final boolean clientDamage(DamageSource source) {
		return !isAlwaysInvulnerableTo(source);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		scheduleVelocityUpdate();
		health = (int) (health - amount);
		if (health <= 0) {
			discard();
		}

		return true;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putShort("Health", (short) health);
		view.putShort("Age", (short) orbAge);
		view.putShort("Value", (short) getValue());
		view.putInt("Count", pickingCount);
	}

	@Override
	protected void readCustomData(ReadView view) {
		health = view.getShort("Health", DEFAULT_HEALTH);
		orbAge = view.getShort("Age", DEFAULT_AGE);
		setValue(view.getShort("Value", DEFAULT_VALUE));
		pickingCount = view.<Integer>read("Count", Codecs.POSITIVE_INT).orElse(DEFAULT_COUNT);
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		if (player.experiencePickUpDelay != 0) {
			return;
		}

		player.experiencePickUpDelay = 2;
		player.sendPickup(this, 1);
		int remainingXp = repairPlayerGears(serverPlayer, getValue());
		if (remainingXp > 0) {
			player.addExperience(remainingXp);
		}

		pickingCount--;
		if (pickingCount == 0) {
			discard();
		}
	}

	/**
	 * Рекурсивно чинит зачарованное снаряжение игрока за счёт опыта шара.
	 * Выбирает случайный предмет с зачарованием REPAIR_WITH_XP, вычисляет
	 * количество опыта, потраченного на починку, и возвращает остаток.
	 *
	 * @param player игрок, чьё снаряжение чинится
	 * @param amount количество опыта, доступного для починки
	 * @return остаток опыта после починки (0 если весь опыт ушёл на починку)
	 */
	private int repairPlayerGears(ServerPlayerEntity player, int amount) {
		Optional<EnchantmentEffectContext> repairTarget = EnchantmentHelper.chooseEquipmentWith(
				EnchantmentEffectComponentTypes.REPAIR_WITH_XP, player, ItemStack::isDamaged
		);
		if (repairTarget.isEmpty()) {
			return amount;
		}

		ItemStack itemStack = repairTarget.get().stack();
		int repairPoints = EnchantmentHelper.getRepairWithExperience(player.getEntityWorld(), itemStack, amount);
		int actualRepair = Math.min(repairPoints, itemStack.getDamage());
		itemStack.setDamage(itemStack.getDamage() - actualRepair);
		if (actualRepair > 0) {
			int remaining = amount - actualRepair * amount / repairPoints;
			if (remaining > 0) {
				return this.repairPlayerGears(player, remaining);
			}
		}

		return 0;
	}

	public int getValue() {
		return dataTracker.get(VALUE);
	}

	private void setValue(int value) {
		dataTracker.set(VALUE, value);
	}

	/**
	 * Возвращает визуальный размер шара (0–10) на основе количества опыта.
	 * Используется рендером для выбора текстуры шара.
	 */
	public int getOrbSize() {
		int value = this.getValue();
		if (value >= 2477) {
			return 10;
		}
		else if (value >= 1237) {
			return 9;
		}
		else if (value >= 617) {
			return 8;
		}
		else if (value >= 307) {
			return 7;
		}
		else if (value >= 149) {
			return 6;
		}
		else if (value >= 73) {
			return 5;
		}
		else if (value >= 37) {
			return 4;
		}
		else if (value >= 17) {
			return 3;
		}
		else if (value >= 7) {
			return 2;
		}
		else {
			return value >= 3 ? 1 : 0;
		}
	}

	/**
	 * Округляет количество опыта до ближайшего стандартного размера шара.
	 * Стандартные размеры: 1, 3, 7, 17, 37, 73, 149, 307, 617, 1237, 2477.
	 * Используется при спавне шаров для разбивки большого количества опыта.
	 */
	public static int roundToOrbSize(int value) {
		if (value >= 2477) {
			return 2477;
		}
		else if (value >= 1237) {
			return 1237;
		}
		else if (value >= 617) {
			return 617;
		}
		else if (value >= 307) {
			return 307;
		}
		else if (value >= 149) {
			return 149;
		}
		else if (value >= 73) {
			return 73;
		}
		else if (value >= 37) {
			return 37;
		}
		else if (value >= 17) {
			return 17;
		}
		else if (value >= 7) {
			return 7;
		}
		else {
			return value >= 3 ? 3 : 1;
		}
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.AMBIENT;
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return interpolator;
	}
}

package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Самоходная вагонетка-печь. Потребляет топливо из тега {@code furnace_minecart_fuel}
 * и движется в направлении, противоположном от игрока, заправившего её.
 * Скорость ограничена по сравнению с обычной вагонеткой.
 */
public class FurnaceMinecartEntity extends AbstractMinecartEntity {

	private static final TrackedData<Boolean> LIT =
			DataTracker.registerData(FurnaceMinecartEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private static final int FUEL_PER_ITEM = 3600;
	private static final int MAX_FUEL = 32000;
	private static final double SPEED_FACTOR_NORMAL = 0.5;
	private static final double SPEED_FACTOR_WATER = 0.75;
	private static final double PUSH_VEC_ALIGN_MIN_SPEED_SQ = 0.001;
	private static final double PUSH_VEC_ALIGN_MIN_LENGTH_SQ = 1.0E-4;
	private static final float SMOKE_PARTICLE_Y_OFFSET = 0.8F;
	private static final int SMOKE_PARTICLE_CHANCE = 4;
	private static final float SLOWDOWN_WITH_PUSH = 0.8F;
	private static final float SLOWDOWN_WITHOUT_PUSH = 0.98F;
	private static final float WATER_SLOWDOWN = 0.1F;

	private int fuel = 0;
	public Vec3d pushVec = Vec3d.ZERO;

	public FurnaceMinecartEntity(EntityType<? extends FurnaceMinecartEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public boolean isSelfPropelling() {
		return true;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LIT, false);
	}

	@Override
	public void tick() {
		super.tick();

		if (!getEntityWorld().isClient()) {
			if (fuel > 0) {
				fuel--;
			}

			if (fuel <= 0) {
				pushVec = Vec3d.ZERO;
			}

			setLit(fuel > 0);
		}

		if (isLit() && random.nextInt(SMOKE_PARTICLE_CHANCE) == 0) {
			getEntityWorld().addParticleClient(
					ParticleTypes.LARGE_SMOKE,
					getX(), getY() + SMOKE_PARTICLE_Y_OFFSET, getZ(),
					0.0, 0.0, 0.0
			);
		}
	}

	@Override
	protected double getMaxSpeed(ServerWorld world) {
		double factor = isTouchingWater() ? SPEED_FACTOR_WATER : SPEED_FACTOR_NORMAL;
		return super.getMaxSpeed(world) * factor;
	}

	@Override
	protected Item asItem() {
		return Items.FURNACE_MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.FURNACE_MINECART);
	}

	/**
	 * Применяет замедление с учётом вектора толчка.
	 * Если вектор толчка ненулевой — выравнивает его по направлению скорости и добавляет к движению.
	 */
	@Override
	protected Vec3d applySlowdown(Vec3d velocity) {
		Vec3d result;

		if (pushVec.lengthSquared() > 1.0E-7) {
			pushVec = alignPushVec(velocity);
			result = velocity.multiply(SLOWDOWN_WITH_PUSH, 0.0, SLOWDOWN_WITH_PUSH).add(pushVec);

			if (isTouchingWater()) {
				result = result.multiply(WATER_SLOWDOWN);
			}
		} else {
			result = velocity.multiply(SLOWDOWN_WITHOUT_PUSH, 0.0, SLOWDOWN_WITHOUT_PUSH);
		}

		return super.applySlowdown(result);
	}

	/**
	 * Выравнивает вектор толчка по направлению текущей скорости для плавного движения.
	 * Если скорость или длина вектора слишком малы — возвращает исходный вектор без изменений.
	 */
	private Vec3d alignPushVec(Vec3d velocity) {
		return pushVec.horizontalLengthSquared() > PUSH_VEC_ALIGN_MIN_LENGTH_SQ
				&& velocity.horizontalLengthSquared() > PUSH_VEC_ALIGN_MIN_SPEED_SQ
				? pushVec.projectOnto(velocity).normalize().multiply(pushVec.length())
				: pushVec;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		if (addFuel(player.getEntityPos(), stack)) {
			stack.decrementUnlessCreative(1, player);
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Добавляет топливо в вагонетку и задаёт вектор толчка от позиции игрока.
	 * Принимает только предметы из тега {@code furnace_minecart_fuel} и не переполняет бак.
	 *
	 * @param playerPos позиция игрока (используется для вычисления направления движения)
	 * @param stack предмет в руке игрока
	 * @return {@code true} если топливо было добавлено
	 */
	public boolean addFuel(Vec3d playerPos, ItemStack stack) {
		if (!stack.isIn(ItemTags.FURNACE_MINECART_FUEL) || fuel + FUEL_PER_ITEM > MAX_FUEL) {
			return false;
		}

		fuel += FUEL_PER_ITEM;

		if (fuel > 0) {
			pushVec = getEntityPos().subtract(playerPos).getHorizontal();
		}

		return true;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putDouble("PushX", pushVec.x);
		view.putDouble("PushZ", pushVec.z);
		view.putShort("Fuel", (short) fuel);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		double pushX = view.getDouble("PushX", 0.0);
		double pushZ = view.getDouble("PushZ", 0.0);
		pushVec = new Vec3d(pushX, 0.0, pushZ);
		fuel = view.getShort("Fuel", (short) 0);
	}

	protected boolean isLit() {
		return dataTracker.get(LIT);
	}

	protected void setLit(boolean lit) {
		dataTracker.set(LIT, lit);
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.FURNACE
				.getDefaultState()
				.with(FurnaceBlock.FACING, Direction.NORTH)
				.with(FurnaceBlock.LIT, isLit());
	}
}

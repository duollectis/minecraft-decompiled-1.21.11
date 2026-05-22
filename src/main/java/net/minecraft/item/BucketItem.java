package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Предмет ведра. Поддерживает набор жидкости из мира (пустое ведро)
 * и выливание жидкости в мир (ведро с жидкостью).
 */
public class BucketItem extends Item implements FluidModificationItem {

	/** Количество частиц дыма при испарении воды в горячем биоме. */
	private static final int EVAPORATION_SMOKE_PARTICLES = 8;
	/** Флаги обновления блока при установке жидкости. */
	private static final int FLUID_PLACE_FLAGS = 11;

	private final Fluid fluid;

	public BucketItem(Fluid fluid, Item.Settings settings) {
		super(settings);
		this.fluid = fluid;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		RaycastContext.FluidHandling fluidHandling = fluid == Fluids.EMPTY
			? RaycastContext.FluidHandling.SOURCE_ONLY
			: RaycastContext.FluidHandling.NONE;
		BlockHitResult hitResult = raycast(world, user, fluidHandling);

		if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() != HitResult.Type.BLOCK) {
			return ActionResult.PASS;
		}

		BlockPos hitPos = hitResult.getBlockPos();
		Direction side = hitResult.getSide();
		BlockPos adjacentPos = hitPos.offset(side);

		if (!world.canEntityModifyAt(user, hitPos) || !user.canPlaceOn(adjacentPos, side, stack)) {
			return ActionResult.FAIL;
		}

		if (fluid == Fluids.EMPTY) {
			return tryPickupFluid(world, user, stack, hitPos);
		}

		return tryPlaceFluid(world, user, stack, hitPos, adjacentPos, hitResult);
	}

	private ActionResult tryPickupFluid(World world, PlayerEntity user, ItemStack stack, BlockPos hitPos) {
		BlockState blockState = world.getBlockState(hitPos);

		if (!(blockState.getBlock() instanceof FluidDrainable drainable)) {
			return ActionResult.FAIL;
		}

		ItemStack filledStack = drainable.tryDrainFluid(user, world, hitPos, blockState);

		if (filledStack.isEmpty()) {
			return ActionResult.FAIL;
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		drainable.getBucketFillSound().ifPresent(sound -> user.playSound(sound, 1.0F, 1.0F));
		world.emitGameEvent(user, GameEvent.FLUID_PICKUP, hitPos);
		ItemStack resultStack = ItemUsage.exchangeStack(stack, user, filledStack);

		if (!world.isClient()) {
			Criteria.FILLED_BUCKET.trigger((ServerPlayerEntity) user, filledStack);
		}

		return ActionResult.SUCCESS.withNewHandStack(resultStack);
	}

	private ActionResult tryPlaceFluid(
		World world,
		PlayerEntity user,
		ItemStack stack,
		BlockPos hitPos,
		BlockPos adjacentPos,
		BlockHitResult hitResult
	) {
		BlockState blockState = world.getBlockState(hitPos);
		// Вода может заполнять FluidFillable блоки напрямую, остальные жидкости — только соседний блок
		BlockPos targetPos = blockState.getBlock() instanceof FluidFillable && fluid == Fluids.WATER
			? hitPos
			: adjacentPos;

		if (!placeFluid(user, world, targetPos, hitResult)) {
			return ActionResult.FAIL;
		}

		onEmptied(user, world, stack, targetPos);

		if (user instanceof ServerPlayerEntity serverPlayer) {
			Criteria.PLACED_BLOCK.trigger(serverPlayer, targetPos, stack);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		ItemStack emptiedStack = ItemUsage.exchangeStack(stack, user, getEmptiedStack(stack, user));
		return ActionResult.SUCCESS.withNewHandStack(emptiedStack);
	}

	public static ItemStack getEmptiedStack(ItemStack stack, PlayerEntity player) {
		return !player.isInCreativeMode() ? new ItemStack(Items.BUCKET) : stack;
	}

	@Override
	public void onEmptied(@Nullable LivingEntity user, World world, ItemStack stack, BlockPos pos) {
	}

	/**
	 * Размещает жидкость из ведра в мире. Обрабатывает испарение воды в горячих биомах,
	 * заполнение {@link FluidFillable} блоков и стандартную установку блока жидкости.
	 */
	@Override
	public boolean placeFluid(
		@Nullable LivingEntity user,
		World world,
		BlockPos pos,
		@Nullable BlockHitResult hitResult
	) {
		if (!(fluid instanceof FlowableFluid flowableFluid)) {
			return false;
		}

		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();
		boolean canBucketPlace = blockState.canBucketPlace(fluid);
		boolean isSneaking = user != null && user.isSneaking();
		boolean canFillable = canBucketPlace || block instanceof FluidFillable fillable
			&& fillable.canFillWithFluid(user, world, pos, blockState, fluid);
		boolean canPlace = blockState.isAir() || canFillable && (!isSneaking || hitResult == null);

		if (!canPlace) {
			return hitResult != null && placeFluid(
				user,
				world,
				hitResult.getBlockPos().offset(hitResult.getSide()),
				null
			);
		}

		if (world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY, pos)
			&& fluid.isIn(FluidTags.WATER)
		) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			world.playSound(
				user,
				pos,
				SoundEvents.BLOCK_FIRE_EXTINGUISH,
				SoundCategory.BLOCKS,
				0.5F,
				2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F
			);

			for (int index = 0; index < EVAPORATION_SMOKE_PARTICLES; index++) {
				world.addParticleClient(
					ParticleTypes.LARGE_SMOKE,
					x + world.random.nextFloat(),
					y + world.random.nextFloat(),
					z + world.random.nextFloat(),
					0.0, 0.0, 0.0
				);
			}

			return true;
		}

		if (block instanceof FluidFillable fillable && fluid == Fluids.WATER) {
			fillable.tryFillWithFluid(world, pos, blockState, flowableFluid.getStill(false));
			playEmptyingSound(user, world, pos);
			return true;
		}

		if (!world.isClient() && canBucketPlace && !blockState.isLiquid()) {
			world.breakBlock(pos, true);
		}

		if (!world.setBlockState(pos, fluid.getDefaultState().getBlockState(), FLUID_PLACE_FLAGS)
			&& !blockState.getFluidState().isStill()
		) {
			return false;
		}

		playEmptyingSound(user, world, pos);
		return true;
	}

	protected void playEmptyingSound(@Nullable LivingEntity user, WorldAccess world, BlockPos pos) {
		SoundEvent sound = fluid.isIn(FluidTags.LAVA)
			? SoundEvents.ITEM_BUCKET_EMPTY_LAVA
			: SoundEvents.ITEM_BUCKET_EMPTY;
		world.playSound(user, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.emitGameEvent(user, GameEvent.FLUID_PLACE, pos);
	}

	public Fluid getFluid() {
		return fluid;
	}
}

package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public abstract class AbstractCauldronBlock extends Block {

	protected static final int WALL_HEIGHT = 4;

	private static final VoxelShape RAYCAST_SHAPE = Block.createColumnShape(12.0, 4.0, 16.0);

	protected static final VoxelShape OUTLINE_SHAPE = Util.make(
		() -> VoxelShapes.combineAndSimplify(
			VoxelShapes.fullCube(),
			VoxelShapes.union(
				Block.createColumnShape(16.0, 8.0, 0.0, 3.0),
				Block.createColumnShape(8.0, 16.0, 0.0, 3.0),
				Block.createColumnShape(12.0, 0.0, 3.0),
				RAYCAST_SHAPE
			),
			BooleanBiFunction.ONLY_FIRST
		)
	);

	protected final CauldronBehavior.CauldronBehaviorMap behaviorMap;

	public AbstractCauldronBlock(AbstractBlock.Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
		super(settings);
		this.behaviorMap = behaviorMap;
	}

	@Override
	protected abstract MapCodec<? extends AbstractCauldronBlock> getCodec();

	/**
	 * Возвращает высоту поверхности жидкости внутри котла в мировых координатах
	 * (относительно нижней грани блока). Переопределяется подклассами для
	 * отображения корректного уровня воды/лавы в зависимости от заполненности.
	 */
	protected double getFluidHeight(BlockState state) {
		return 0.0;
	}

	/**
	 * Возвращает {@code true}, если котёл полностью заполнен жидкостью.
	 * Используется, в частности, для определения возможности наполнения
	 * из дрипстоуна и для компараторного сигнала.
	 */
	public abstract boolean isFull(BlockState state);

	@Override
	protected ActionResult onUseWithItem(
		ItemStack stack,
		BlockState state,
		World world,
		BlockPos pos,
		PlayerEntity player,
		Hand hand,
		BlockHitResult hit
	) {
		CauldronBehavior cauldronBehavior = behaviorMap.map().get(stack.getItem());
		return cauldronBehavior.interact(state, world, pos, player, hand, stack);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE_SHAPE;
	}

	@Override
	protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return RAYCAST_SHAPE;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	/**
	 * Обрабатывает наполнение котла каплями из сталактита (pointed dripstone).
	 * Ищет ближайший дрипстоун над котлом, определяет тип капающей жидкости
	 * и вызывает {@link #fillFromDripstone}, если жидкость допустима для данного котла.
	 */
	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos dripPos = PointedDripstoneBlock.getDripPos(world, pos);
		if (dripPos == null) {
			return;
		}

		Fluid drippingFluid = PointedDripstoneBlock.getDripFluid(world, dripPos);
		if (drippingFluid != Fluids.EMPTY && canBeFilledByDripstone(drippingFluid)) {
			fillFromDripstone(state, world, pos, drippingFluid);
		}
	}

	/**
	 * Хук-предикат: определяет, может ли данный тип котла наполняться
	 * каплями указанной жидкости из сталактита. По умолчанию запрещено;
	 * переопределяется, например, в водяном и лавовом котлах.
	 */
	protected boolean canBeFilledByDripstone(Fluid fluid) {
		return false;
	}

	/**
	 * Хук-действие: выполняет фактическое наполнение котла жидкостью из сталактита.
	 * Вызывается только если {@link #canBeFilledByDripstone} вернул {@code true}.
	 * Подклассы обязаны переопределить этот метод вместе с {@link #canBeFilledByDripstone}.
	 */
	protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
	}
}

package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Предмет «Строительные леса». Переопределяет логику размещения: при клике на
 * уже существующий блок лесов ищет первую свободную позицию вдоль направления
 * размещения (до 7 блоков). Ограничивает строительство выше максимальной высоты мира.
 */
public class ScaffoldingItem extends BlockItem {

	private static final int MAX_SCAFFOLD_SEARCH = 7;
	private static final int MAX_SCAFFOLD_DISTANCE = 7;

	public ScaffoldingItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	/**
	 * Вычисляет контекст размещения лесов. Если целевой блок — не леса, проверяет
	 * допустимую дистанцию от опоры. Если целевой блок — леса, ищет первую свободную
	 * позицию вдоль направления (горизонтально или вверх).
	 */
	@Override
	public @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		BlockState blockState = world.getBlockState(pos);
		Block scaffoldBlock = getBlock();

		if (!blockState.isOf(scaffoldBlock)) {
			return ScaffoldingBlock.calculateDistance(world, pos) == MAX_SCAFFOLD_DISTANCE ? null : context;
		}

		Direction direction = resolveExtendDirection(context);
		int horizontalCount = 0;
		BlockPos.Mutable mutable = pos.mutableCopy().move(direction);

		while (horizontalCount < MAX_SCAFFOLD_SEARCH) {
			if (!world.isClient() && !world.isInBuildLimit(mutable)) {
				notifyBuildLimitIfNeeded(context, world);
				break;
			}

			BlockState stateAt = world.getBlockState(mutable);

			if (!stateAt.isOf(scaffoldBlock)) {
				if (stateAt.canReplace(context)) {
					return ItemPlacementContext.offset(context, mutable, direction);
				}

				break;
			}

			mutable.move(direction);

			if (direction.getAxis().isHorizontal()) {
				horizontalCount++;
			}
		}

		return null;
	}

	private Direction resolveExtendDirection(ItemPlacementContext context) {
		if (context.shouldCancelInteraction()) {
			return context.hitsInsideBlock()
				? context.getSide().getOpposite()
				: context.getSide();
		}

		return context.getSide() == Direction.UP
			? context.getHorizontalPlayerFacing()
			: Direction.UP;
	}

	private void notifyBuildLimitIfNeeded(ItemPlacementContext context, World world) {
		PlayerEntity player = context.getPlayer();
		int topY = world.getTopYInclusive();

		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.sendMessageToClient(
				Text.translatable("build.tooHigh", topY).formatted(Formatting.RED),
				true
			);
		}
	}

	@Override
	protected boolean checkStatePlacement() {
		return false;
	}
}

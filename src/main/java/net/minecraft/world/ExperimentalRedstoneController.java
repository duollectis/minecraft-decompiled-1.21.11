package net.minecraft.world;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Экспериментальный контроллер редстоуна с детерминированным порядком обновлений.
 * Использует очереди увеличения и уменьшения мощности для корректной
 * последовательной обработки изменений сигнала.
 *
 * <p>Мощность и ориентация провода упакованы в одно int-значение:
 * старшие биты — ordinal ориентации, младшие 4 бита — мощность (0–15).
 */
public class ExperimentalRedstoneController extends RedstoneController {

	/** Маска для извлечения мощности из упакованного значения (4 бита). */
	private static final int POWER_MASK = 0xF;

	/** Сдвиг для извлечения/записи ordinal ориентации. */
	private static final int ORIENTATION_SHIFT = 4;

	/** Значение, означающее отсутствие записи в кэше. */
	private static final int ABSENT = -1;

	/** Флаг блокировки уведомления соседей при первом блоке в цепочке. */
	private static final int FLAG_NO_NEIGHBOR_DROPS = 128;

	/** Базовый флаг обновления блока. */
	private static final int FLAG_BLOCK_UPDATE = 2;

	private final Deque<BlockPos> powerIncreaseQueue = new ArrayDeque<>();
	private final Deque<BlockPos> powerDecreaseQueue = new ArrayDeque<>();
	private final Object2IntMap<BlockPos> wireOrientationsAndPowers = new Object2IntLinkedOpenHashMap<>();

	public ExperimentalRedstoneController(RedstoneWireBlock redstoneWireBlock) {
		super(redstoneWireBlock);
	}

	@Override
	public void update(
		World world,
		BlockPos pos,
		BlockState state,
		@Nullable WireOrientation orientation,
		boolean blockAdded
	) {
		WireOrientation wireOrientation = tweakOrientation(world, orientation);
		propagatePowerUpdates(world, pos, wireOrientation);

		boolean firstEntry = true;
		var iterator = wireOrientationsAndPowers.object2IntEntrySet().iterator();

		while (iterator.hasNext()) {
			Object2IntMap.Entry<BlockPos> entry = iterator.next();
			BlockPos wirePos = entry.getKey();
			int packed = entry.getIntValue();
			int power = unpackPower(packed);
			BlockState wireState = world.getBlockState(wirePos);

			if (wireState.isOf(wire) && !wireState.get(RedstoneWireBlock.POWER).equals(power)) {
				int flags = FLAG_BLOCK_UPDATE;

				if (!blockAdded || !firstEntry) {
					flags |= FLAG_NO_NEIGHBOR_DROPS;
				}

				world.setBlockState(wirePos, wireState.with(RedstoneWireBlock.POWER, power), flags);
			} else {
				iterator.remove();
			}

			firstEntry = false;
		}

		applyNeighborUpdates(world);
	}

	private void applyNeighborUpdates(World world) {
		wireOrientationsAndPowers.forEach((pos, packed) -> {
			WireOrientation wireOrientation = unpackOrientation(packed);
			BlockState blockState = world.getBlockState(pos);

			for (Direction direction : wireOrientation.getDirectionsByPriority()) {
				if (!canProvidePowerTo(blockState, direction)) {
					continue;
				}

				BlockPos neighborPos = pos.offset(direction);
				BlockState neighborState = world.getBlockState(neighborPos);
				WireOrientation neighborOrientation = wireOrientation.withFrontIfNotUp(direction);

				world.updateNeighbor(neighborState, neighborPos, wire, neighborOrientation, false);

				if (neighborState.isSolidBlock(world, neighborPos)) {
					for (Direction dir2 : neighborOrientation.getDirectionsByPriority()) {
						if (dir2 != direction.getOpposite()) {
							world.updateNeighbor(
								neighborPos.offset(dir2),
								wire,
								neighborOrientation.withFrontIfNotUp(dir2)
							);
						}
					}
				}
			}
		});

		if (world instanceof ServerWorld serverWorld
			&& serverWorld.getSubscriptionTracker().isSubscribed(DebugSubscriptionTypes.REDSTONE_WIRE_ORIENTATIONS)
		) {
			wireOrientationsAndPowers.forEach((pos, packed) ->
				serverWorld.getSubscriptionTracker().sendBlockDebugData(
					pos,
					DebugSubscriptionTypes.REDSTONE_WIRE_ORIENTATIONS,
					unpackOrientation(packed)
				)
			);
		}
	}

	private static boolean canProvidePowerTo(BlockState wireState, Direction direction) {
		EnumProperty<WireConnection> property = RedstoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction);
		return property == null ? direction == Direction.DOWN : wireState.get(property).isConnected();
	}

	private static WireOrientation tweakOrientation(World world, @Nullable WireOrientation orientation) {
		WireOrientation base = orientation != null ? orientation : WireOrientation.random(world.random);
		return base.withUp(Direction.UP).withSideBias(WireOrientation.SideBias.LEFT);
	}

	/**
	 * Распространяет обновления мощности от заданной позиции по всей связанной сети проводов.
	 * Сначала обрабатывает очередь увеличения мощности, затем — уменьшения.
	 */
	private void propagatePowerUpdates(World world, BlockPos pos, WireOrientation orientation) {
		BlockState blockState = world.getBlockState(pos);

		if (blockState.isOf(wire)) {
			updatePowerAt(pos, blockState.get(RedstoneWireBlock.POWER), orientation);
			powerIncreaseQueue.add(pos);
		} else {
			spreadPowerUpdateToNeighbors(world, pos, 0, orientation, true);
		}

		while (!powerIncreaseQueue.isEmpty()) {
			BlockPos current = powerIncreaseQueue.removeFirst();
			int packed = wireOrientationsAndPowers.getInt(current);
			WireOrientation currentOrientation = unpackOrientation(packed);
			int currentPower = unpackPower(packed);
			int strongPower = getStrongPowerAt(world, current);
			int wirePower = calculateWirePowerAt(world, current);
			int maxPower = Math.max(strongPower, wirePower);

			int newPower;

			if (maxPower < currentPower) {
				if (strongPower > 0 && !powerDecreaseQueue.contains(current)) {
					powerDecreaseQueue.add(current);
				}

				newPower = 0;
			} else {
				newPower = maxPower;
			}

			if (newPower != currentPower) {
				updatePowerAt(current, newPower, currentOrientation);
			}

			spreadPowerUpdateToNeighbors(world, current, newPower, currentOrientation, currentPower > maxPower);
		}

		while (!powerDecreaseQueue.isEmpty()) {
			BlockPos current = powerDecreaseQueue.removeFirst();
			int packed = wireOrientationsAndPowers.getInt(current);
			int currentPower = unpackPower(packed);
			int strongPower = getStrongPowerAt(world, current);
			int wirePower = calculateWirePowerAt(world, current);
			int maxPower = Math.max(strongPower, wirePower);
			WireOrientation currentOrientation = unpackOrientation(packed);

			if (maxPower > currentPower) {
				updatePowerAt(current, maxPower, currentOrientation);
			} else if (maxPower < currentPower) {
				throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
			}

			spreadPowerUpdateToNeighbors(world, current, maxPower, currentOrientation, false);
		}
	}

	private static int packOrientationAndPower(WireOrientation orientation, int power) {
		return orientation.ordinal() << ORIENTATION_SHIFT | power;
	}

	private static WireOrientation unpackOrientation(int packed) {
		return WireOrientation.fromOrdinal(packed >> ORIENTATION_SHIFT);
	}

	private static int unpackPower(int packed) {
		return packed & POWER_MASK;
	}

	private void updatePowerAt(BlockPos pos, int power, WireOrientation defaultOrientation) {
		wireOrientationsAndPowers.compute(pos, (key, existing) ->
			existing == null
				? packOrientationAndPower(defaultOrientation, power)
				: packOrientationAndPower(unpackOrientation(existing), power)
		);
	}

	private void spreadPowerUpdateToNeighbors(
		World world,
		BlockPos pos,
		int power,
		WireOrientation orientation,
		boolean canIncreasePower
	) {
		for (Direction direction : orientation.getHorizontalDirections()) {
			spreadPowerUpdateTo(world, pos.offset(direction), power, orientation.withFront(direction), canIncreasePower);
		}

		for (Direction direction : orientation.getVerticalDirections()) {
			BlockPos verticalNeighbor = pos.offset(direction);
			boolean isSolid = world.getBlockState(verticalNeighbor).isSolidBlock(world, verticalNeighbor);

			for (Direction horizontal : orientation.getHorizontalDirections()) {
				BlockPos horizontalNeighbor = pos.offset(horizontal);

				if (direction == Direction.UP && !isSolid) {
					spreadPowerUpdateTo(
						world,
						verticalNeighbor.offset(horizontal),
						power,
						orientation.withFront(horizontal),
						canIncreasePower
					);
				} else if (direction == Direction.DOWN
					&& !world.getBlockState(horizontalNeighbor).isSolidBlock(world, horizontalNeighbor)
				) {
					spreadPowerUpdateTo(
						world,
						verticalNeighbor.offset(horizontal),
						power,
						orientation.withFront(horizontal),
						canIncreasePower
					);
				}
			}
		}
	}

	private void spreadPowerUpdateTo(
		World world,
		BlockPos neighborPos,
		int power,
		WireOrientation orientation,
		boolean canIncreasePower
	) {
		BlockState blockState = world.getBlockState(neighborPos);

		if (!blockState.isOf(wire)) {
			return;
		}

		int neighborPower = getWirePowerAt(neighborPos, blockState);

		if (neighborPower < power - 1 && !powerDecreaseQueue.contains(neighborPos)) {
			powerDecreaseQueue.add(neighborPos);
			updatePowerAt(neighborPos, neighborPower, orientation);
		}

		if (canIncreasePower && neighborPower > power && !powerIncreaseQueue.contains(neighborPos)) {
			powerIncreaseQueue.add(neighborPos);
			updatePowerAt(neighborPos, neighborPower, orientation);
		}
	}

	@Override
	protected int getWirePowerAt(BlockPos pos, BlockState state) {
		int packed = wireOrientationsAndPowers.getOrDefault(pos, ABSENT);
		return packed != ABSENT ? unpackPower(packed) : super.getWirePowerAt(pos, state);
	}
}

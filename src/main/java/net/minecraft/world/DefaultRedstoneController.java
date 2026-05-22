package net.minecraft.world;

import com.google.common.collect.Sets;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Стандартный контроллер редстоуна.
 * При изменении мощности провода обновляет его состояние и рассылает
 * уведомления всем соседним блокам.
 */
public class DefaultRedstoneController extends RedstoneController {

	public DefaultRedstoneController(RedstoneWireBlock redstoneWireBlock) {
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
		int newPower = calculateTotalPowerAt(world, pos);

		if (state.get(RedstoneWireBlock.POWER).equals(newPower)) {
			return;
		}

		if (world.getBlockState(pos) == state) {
			world.setBlockState(pos, state.with(RedstoneWireBlock.POWER, newPower), 2);
		}

		Set<BlockPos> toUpdate = Sets.newHashSet();
		toUpdate.add(pos);

		for (Direction direction : Direction.values()) {
			toUpdate.add(pos.offset(direction));
		}

		for (BlockPos blockPos : toUpdate) {
			world.updateNeighbors(blockPos, wire);
		}
	}

	private int calculateTotalPowerAt(World world, BlockPos pos) {
		int strongPower = getStrongPowerAt(world, pos);
		return strongPower == 15 ? strongPower : Math.max(strongPower, calculateWirePowerAt(world, pos));
	}
}

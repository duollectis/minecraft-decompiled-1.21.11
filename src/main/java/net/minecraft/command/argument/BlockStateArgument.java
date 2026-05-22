package net.minecraft.command.argument;

import com.mojang.logging.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code BlockStateArgument}.
 */
public class BlockStateArgument implements Predicate<CachedBlockPosition> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final BlockState state;
	private final Set<Property<?>> properties;
	private final @Nullable NbtCompound data;

	public BlockStateArgument(BlockState state, Set<Property<?>> properties, @Nullable NbtCompound data) {
		this.state = state;
		this.properties = properties;
		this.data = data;
	}

	public BlockState getBlockState() {
		return this.state;
	}

	public Set<Property<?>> getProperties() {
		return this.properties;
	}

	public boolean test(CachedBlockPosition cachedBlockPosition) {
		BlockState blockState = cachedBlockPosition.getBlockState();
		if (!blockState.isOf(this.state.getBlock())) {
			return false;
		}
		else {
			for (Property<?> property : this.properties) {
				if (blockState.get(property) != this.state.get(property)) {
					return false;
				}
			}

			if (this.data == null) {
				return true;
			}
			else {
				BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
				return blockEntity != null
						&& NbtHelper.matches(
						this.data,
						blockEntity.createNbtWithIdentifyingData(cachedBlockPosition.getWorld().getRegistryManager()),
						true
				);
			}
		}
	}

	public boolean test(ServerWorld world, BlockPos pos) {
		return this.test(new CachedBlockPosition(world, pos, false));
	}

	public boolean setBlockState(ServerWorld world, BlockPos pos, @Block.SetBlockStateFlag int flags) {
		BlockState blockState = (flags & Block.FORCE_STATE) != 0 ? this.state : Block.postProcessState(this.state, world, pos);
		if (blockState.isAir()) {
			blockState = this.state;
		}

		blockState = this.copyPropertiesTo(blockState);
		boolean bl = false;
		if (world.setBlockState(pos, blockState, flags)) {
			bl = true;
		}

		if (this.data != null) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity != null) {
				try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
					RegistryWrapper.WrapperLookup wrapperLookup = world.getRegistryManager();
					ErrorReporter errorReporter = logging.makeChild(blockEntity.getReporterContext());
					NbtWriteView
							nbtWriteView =
							NbtWriteView.create(errorReporter.makeChild(() -> "(before)"), wrapperLookup);
					blockEntity.writeDataWithoutId(nbtWriteView);
					NbtCompound nbtCompound = nbtWriteView.getNbt();
					blockEntity.read(NbtReadView.create(logging, wrapperLookup, this.data));
					NbtWriteView
							nbtWriteView2 =
							NbtWriteView.create(errorReporter.makeChild(() -> "(after)"), wrapperLookup);
					blockEntity.writeDataWithoutId(nbtWriteView2);
					NbtCompound nbtCompound2 = nbtWriteView2.getNbt();
					if (!nbtCompound2.equals(nbtCompound)) {
						bl = true;
						blockEntity.markDirty();
						world.getChunkManager().markForUpdate(pos);
					}
				}
			}
		}

		return bl;
	}

	private BlockState copyPropertiesTo(BlockState state) {
		if (state == this.state) {
			return state;
		}
		else {
			for (Property<?> property : this.properties) {
				state = copyProperty(state, this.state, property);
			}

			return state;
		}
	}

	private static <T extends Comparable<T>> BlockState copyProperty(
			BlockState to,
			BlockState from,
			Property<T> property
	) {
		return to.withIfExists(property, from.get(property));
	}
}

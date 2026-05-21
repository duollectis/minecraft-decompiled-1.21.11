package net.minecraft.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code MovingBlockRenderState}.
 */
public class MovingBlockRenderState implements BlockRenderView, FabricRenderState {

	public BlockPos fallingBlockPos = BlockPos.ORIGIN;
	public BlockPos entityBlockPos = BlockPos.ORIGIN;
	public BlockState blockState = Blocks.AIR.getDefaultState();
	public @Nullable RegistryEntry<Biome> biome;
	public BlockRenderView world = EmptyBlockRenderView.INSTANCE;

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		return this.world.getBrightness(direction, shaded);
	}

	@Override
	public LightingProvider getLightingProvider() {
		return this.world.getLightingProvider();
	}

	@Override
	public int getColor(BlockPos pos, ColorResolver colorResolver) {
		return this.biome == null ? -1 : colorResolver.getColor(this.biome.value(), pos.getX(), pos.getZ());
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return pos.equals(this.entityBlockPos) ? this.blockState : Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return 1;
	}

	@Override
	public int getBottomY() {
		return this.entityBlockPos.getY();
	}
}

package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.enums.PistonType;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.block.entity.state.PistonBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code PistonBlockEntityRenderer}.
 */
public class PistonBlockEntityRenderer implements BlockEntityRenderer<PistonBlockEntity, PistonBlockEntityRenderState> {

	/**
	 * Создаёт render state.
	 *
	 * @return PistonBlockEntityRenderState — результат операции
	 */
	public PistonBlockEntityRenderState createRenderState() {
		return new PistonBlockEntityRenderState();
	}

	public void updateRenderState(
			PistonBlockEntity pistonBlockEntity,
			PistonBlockEntityRenderState pistonBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				pistonBlockEntity,
				pistonBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		pistonBlockEntityRenderState.offsetX = pistonBlockEntity.getRenderOffsetX(f);
		pistonBlockEntityRenderState.offsetY = pistonBlockEntity.getRenderOffsetY(f);
		pistonBlockEntityRenderState.offsetZ = pistonBlockEntity.getRenderOffsetZ(f);
		pistonBlockEntityRenderState.pushedState = null;
		pistonBlockEntityRenderState.extendedPistonState = null;
		BlockState blockState = pistonBlockEntity.getPushedBlock();
		World world = pistonBlockEntity.getWorld();
		if (world != null && !blockState.isAir()) {
			BlockPos
					blockPos =
					pistonBlockEntity.getPos().offset(pistonBlockEntity.getMovementDirection().getOpposite());
			RegistryEntry<Biome> registryEntry = world.getBiome(blockPos);
			if (blockState.isOf(Blocks.PISTON_HEAD) && pistonBlockEntity.getProgress(f) <= 4.0F) {
				blockState = blockState.with(PistonHeadBlock.SHORT, pistonBlockEntity.getProgress(f) <= 0.5F);
				pistonBlockEntityRenderState.pushedState = renderModel(blockPos, blockState, registryEntry, world);
			}
			else if (pistonBlockEntity.isSource() && !pistonBlockEntity.isExtending()) {
				PistonType pistonType = blockState.isOf(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
				BlockState blockState2 = Blocks.PISTON_HEAD
						.getDefaultState()
						.with(PistonHeadBlock.TYPE, pistonType)
						.with(PistonHeadBlock.FACING, blockState.get(PistonBlock.FACING));
				blockState2 = blockState2.with(PistonHeadBlock.SHORT, pistonBlockEntity.getProgress(f) >= 0.5F);
				pistonBlockEntityRenderState.pushedState = renderModel(blockPos, blockState2, registryEntry, world);
				BlockPos blockPos2 = blockPos.offset(pistonBlockEntity.getMovementDirection());
				blockState = blockState.with(PistonBlock.EXTENDED, true);
				pistonBlockEntityRenderState.extendedPistonState =
						renderModel(blockPos2, blockState, registryEntry, world);
			}
			else {
				pistonBlockEntityRenderState.pushedState = renderModel(blockPos, blockState, registryEntry, world);
			}
		}
	}

	public void render(
			PistonBlockEntityRenderState pistonBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (pistonBlockEntityRenderState.pushedState != null) {
			matrixStack.push();
			matrixStack.translate(
					pistonBlockEntityRenderState.offsetX,
					pistonBlockEntityRenderState.offsetY,
					pistonBlockEntityRenderState.offsetZ
			);
			orderedRenderCommandQueue.submitMovingBlock(matrixStack, pistonBlockEntityRenderState.pushedState);
			matrixStack.pop();
			if (pistonBlockEntityRenderState.extendedPistonState != null) {
				orderedRenderCommandQueue.submitMovingBlock(
						matrixStack,
						pistonBlockEntityRenderState.extendedPistonState
				);
			}
		}
	}

	private static MovingBlockRenderState renderModel(
			BlockPos pos,
			BlockState state,
			RegistryEntry<Biome> biome,
			World world
	) {
		MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
		movingBlockRenderState.fallingBlockPos = pos;
		movingBlockRenderState.entityBlockPos = pos;
		movingBlockRenderState.blockState = state;
		movingBlockRenderState.biome = biome;
		movingBlockRenderState.world = world;
		return movingBlockRenderState;
	}

	@Override
	public int getRenderDistance() {
		return 68;
	}
}

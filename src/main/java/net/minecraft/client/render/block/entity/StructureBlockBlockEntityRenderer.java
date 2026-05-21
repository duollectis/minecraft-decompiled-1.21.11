package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.StructureBoxRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.block.entity.state.StructureBlockBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code StructureBlockBlockEntityRenderer}.
 */
public class StructureBlockBlockEntityRenderer<T extends BlockEntity & StructureBoxRendering>
		implements BlockEntityRenderer<T, StructureBlockBlockEntityRenderState> {

	public static final int STRUCTURE_VOID_FACE_COLOR = ColorHelper.fromFloats(0.2F, 0.75F, 0.75F, 1.0F);

	/**
	 * Создаёт render state.
	 *
	 * @return StructureBlockBlockEntityRenderState — результат операции
	 */
	public StructureBlockBlockEntityRenderState createRenderState() {
		return new StructureBlockBlockEntityRenderState();
	}

	public void updateRenderState(
			T blockEntity,
			StructureBlockBlockEntityRenderState structureBlockBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				blockEntity,
				structureBlockBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		updateStructureBoxRenderState(blockEntity, structureBlockBlockEntityRenderState);
	}

	public static <T extends BlockEntity & StructureBoxRendering> void updateStructureBoxRenderState(
			T blockEntity,
			StructureBlockBlockEntityRenderState state
	) {
		ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
		state.visible = clientPlayerEntity.isCreativeLevelTwoOp() || clientPlayerEntity.isSpectator();
		state.structureBox = blockEntity.getStructureBox();
		state.renderMode = blockEntity.getRenderMode();
		BlockPos blockPos = state.structureBox.localPos();
		Vec3i vec3i = state.structureBox.size();
		BlockPos blockPos2 = state.pos;
		BlockPos blockPos3 = blockPos2.add(blockPos);
		if (state.visible && blockEntity.getWorld() != null
				&& state.renderMode == StructureBoxRendering.RenderMode.BOX_AND_INVISIBLE_BLOCKS) {
			state.invisibleBlocks =
					new StructureBlockBlockEntityRenderState.InvisibleRenderType[vec3i.getX() * vec3i.getY()
							* vec3i.getZ()];

			for (int i = 0; i < vec3i.getX(); i++) {
				for (int j = 0; j < vec3i.getY(); j++) {
					for (int k = 0; k < vec3i.getZ(); k++) {
						int l = k * vec3i.getX() * vec3i.getY() + j * vec3i.getX() + i;
						BlockState blockState = blockEntity.getWorld().getBlockState(blockPos3.add(i, j, k));
						if (blockState.isAir()) {
							state.invisibleBlocks[l] = StructureBlockBlockEntityRenderState.InvisibleRenderType.AIR;
						}
						else if (blockState.isOf(Blocks.STRUCTURE_VOID)) {
							state.invisibleBlocks[l] =
									StructureBlockBlockEntityRenderState.InvisibleRenderType.STRUCTURE_VOID;
						}
						else if (blockState.isOf(Blocks.BARRIER)) {
							state.invisibleBlocks[l] = StructureBlockBlockEntityRenderState.InvisibleRenderType.BARRIER;
						}
						else if (blockState.isOf(Blocks.LIGHT)) {
							state.invisibleBlocks[l] = StructureBlockBlockEntityRenderState.InvisibleRenderType.LIGHT;
						}
					}
				}
			}
		}
		else {
			state.invisibleBlocks = null;
		}

		if (state.visible) {
		}

		state.structureVoidBlocks = null;
	}

	public void render(
			StructureBlockBlockEntityRenderState structureBlockBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (structureBlockBlockEntityRenderState.visible) {
			StructureBoxRendering.RenderMode renderMode = structureBlockBlockEntityRenderState.renderMode;
			if (renderMode != StructureBoxRendering.RenderMode.NONE) {
				StructureBoxRendering.StructureBox structureBox = structureBlockBlockEntityRenderState.structureBox;
				BlockPos blockPos = structureBox.localPos();
				Vec3i vec3i = structureBox.size();
				if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
					float f = 1.0F;
					float g = 0.9F;
					BlockPos blockPos2 = blockPos.add(vec3i);
					GizmoDrawing.box(
							new Box(
									blockPos.getX(),
									blockPos.getY(),
									blockPos.getZ(),
									blockPos2.getX(),
									blockPos2.getY(),
									blockPos2.getZ()
							)
									.offset(structureBlockBlockEntityRenderState.pos),
							DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.9F, 0.9F, 0.9F)),
							true
					);
					this.renderInvisibleBlocks(structureBlockBlockEntityRenderState, blockPos, vec3i);
				}
			}
		}
	}

	private void renderInvisibleBlocks(StructureBlockBlockEntityRenderState state, BlockPos pos, Vec3i size) {
		if (state.invisibleBlocks != null) {
			BlockPos blockPos = state.pos;
			BlockPos blockPos2 = blockPos.add(pos);

			for (int i = 0; i < size.getX(); i++) {
				for (int j = 0; j < size.getY(); j++) {
					for (int k = 0; k < size.getZ(); k++) {
						int l = k * size.getX() * size.getY() + j * size.getX() + i;
						StructureBlockBlockEntityRenderState.InvisibleRenderType
								invisibleRenderType =
								state.invisibleBlocks[l];
						if (invisibleRenderType != null) {
							float
									f =
									invisibleRenderType == StructureBlockBlockEntityRenderState.InvisibleRenderType.AIR
									? 0.05F : 0.0F;
							double d = blockPos2.getX() + i + 0.45F - f;
							double e = blockPos2.getY() + j + 0.45F - f;
							double g = blockPos2.getZ() + k + 0.45F - f;
							double h = blockPos2.getX() + i + 0.55F + f;
							double m = blockPos2.getY() + j + 0.55F + f;
							double n = blockPos2.getZ() + k + 0.55F + f;
							Box box = new Box(d, e, g, h, m, n);
							if (invisibleRenderType == StructureBlockBlockEntityRenderState.InvisibleRenderType.AIR) {
								GizmoDrawing.box(
										box,
										DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.5F, 0.5F, 1.0F))
								);
							}
							else if (invisibleRenderType
									== StructureBlockBlockEntityRenderState.InvisibleRenderType.STRUCTURE_VOID) {
								GizmoDrawing.box(
										box,
										DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 1.0F, 0.75F, 0.75F))
								);
							}
							else if (invisibleRenderType
									== StructureBlockBlockEntityRenderState.InvisibleRenderType.BARRIER) {
								GizmoDrawing.box(box, DrawStyle.stroked(-65536));
							}
							else if (invisibleRenderType
									== StructureBlockBlockEntityRenderState.InvisibleRenderType.LIGHT) {
								GizmoDrawing.box(box, DrawStyle.stroked(-256));
							}
						}
					}
				}
			}
		}
	}

	private void renderStructureVoids(StructureBlockBlockEntityRenderState state, BlockPos pos, Vec3i size) {
		if (state.structureVoidBlocks != null) {
			VoxelSet voxelSet = new BitSetVoxelSet(size.getX(), size.getY(), size.getZ());

			for (int i = 0; i < size.getX(); i++) {
				for (int j = 0; j < size.getY(); j++) {
					for (int k = 0; k < size.getZ(); k++) {
						int l = k * size.getX() * size.getY() + j * size.getX() + i;
						if (state.structureVoidBlocks[l]) {
							voxelSet.set(i, j, k);
						}
					}
				}
			}

			voxelSet.forEachDirection((direction, ix, jx, kx) -> {
				float f = 0.48F;
				float g = ix + pos.getX() + 0.5F - 0.48F;
				float h = jx + pos.getY() + 0.5F - 0.48F;
				float lx = kx + pos.getZ() + 0.5F - 0.48F;
				float m = ix + pos.getX() + 0.5F + 0.48F;
				float n = jx + pos.getY() + 0.5F + 0.48F;
				float o = kx + pos.getZ() + 0.5F + 0.48F;
				GizmoDrawing.face(new Vec3d(g, h, lx), new Vec3d(m, n, o), direction, DrawStyle.filled(STRUCTURE_VOID_FACE_COLOR));
			});
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox() {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return 96;
	}
}

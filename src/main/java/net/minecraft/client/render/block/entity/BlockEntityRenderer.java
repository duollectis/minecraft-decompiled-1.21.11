package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockEntityRenderer}.
 */
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {

	S createRenderState();

	default void updateRenderState(
			T blockEntity,
			S state,
			float tickProgress,
			Vec3d cameraPos,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
	) {
		BlockEntityRenderState.updateBlockEntityRenderState(blockEntity, state, crumblingOverlay);
	}

	void render(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState);

	default boolean rendersOutsideBoundingBox() {
		return false;
	}

	default int getRenderDistance() {
		return 64;
	}

	default boolean isInRenderDistance(T blockEntity, Vec3d pos) {
		return Vec3d.ofCenter(blockEntity.getPos()).isInRange(pos, this.getRenderDistance());
	}
}

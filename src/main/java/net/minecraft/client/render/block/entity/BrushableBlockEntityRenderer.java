package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.state.BrushableBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BrushableBlockEntityRenderer}.
 */
public class BrushableBlockEntityRenderer implements BlockEntityRenderer<BrushableBlockEntity, BrushableBlockEntityRenderState> {

	private final ItemModelManager itemModelManager;

	public BrushableBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.itemModelManager = context.itemModelManager();
	}

	/**
	 * Создаёт render state.
	 *
	 * @return BrushableBlockEntityRenderState — результат операции
	 */
	public BrushableBlockEntityRenderState createRenderState() {
		return new BrushableBlockEntityRenderState();
	}

	public void updateRenderState(
			BrushableBlockEntity brushableBlockEntity,
			BrushableBlockEntityRenderState brushableBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				brushableBlockEntity,
				brushableBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		brushableBlockEntityRenderState.face = brushableBlockEntity.getHitDirection();
		brushableBlockEntityRenderState.dusted = brushableBlockEntity.getCachedState().get(Properties.DUSTED);
		if (brushableBlockEntity.getWorld() != null && brushableBlockEntity.getHitDirection() != null) {
			brushableBlockEntityRenderState.lightmapCoordinates = WorldRenderer.getLightmapCoordinates(
					WorldRenderer.BrightnessGetter.DEFAULT,
					brushableBlockEntity.getWorld(),
					brushableBlockEntity.getCachedState(),
					brushableBlockEntity.getPos().offset(brushableBlockEntity.getHitDirection())
			);
		}

		this.itemModelManager
				.clearAndUpdate(
						brushableBlockEntityRenderState.itemRenderState,
						brushableBlockEntity.getItem(),
						ItemDisplayContext.FIXED,
						brushableBlockEntity.getWorld(),
						null,
						0
				);
	}

	public void render(
			BrushableBlockEntityRenderState brushableBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (brushableBlockEntityRenderState.dusted > 0
				&& brushableBlockEntityRenderState.face != null
				&& !brushableBlockEntityRenderState.itemRenderState.isEmpty()) {
			matrixStack.push();
			matrixStack.translate(0.0F, 0.5F, 0.0F);
			float[]
					fs =
					this.getTranslation(brushableBlockEntityRenderState.face, brushableBlockEntityRenderState.dusted);
			matrixStack.translate(fs[0], fs[1], fs[2]);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0F));
			boolean
					bl =
					brushableBlockEntityRenderState.face == Direction.EAST
							|| brushableBlockEntityRenderState.face == Direction.WEST;
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((bl ? 90 : 0) + 11));
			matrixStack.scale(0.5F, 0.5F, 0.5F);
			brushableBlockEntityRenderState.itemRenderState
					.render(
							matrixStack,
							orderedRenderCommandQueue,
							brushableBlockEntityRenderState.lightmapCoordinates,
							OverlayTexture.DEFAULT_UV,
							0
					);
			matrixStack.pop();
		}
	}

	private float[] getTranslation(Direction direction, int dustedLevel) {
		float[] fs = new float[]{0.5F, 0.0F, 0.5F};
		float f = dustedLevel / 10.0F * 0.75F;
		switch (direction) {
			case EAST:
				fs[0] = 0.73F + f;
				break;
			case WEST:
				fs[0] = 0.25F - f;
				break;
			case UP:
				fs[1] = 0.25F + f;
				break;
			case DOWN:
				fs[1] = -0.23F - f;
				break;
			case NORTH:
				fs[2] = 0.25F - f;
				break;
			case SOUTH:
				fs[2] = 0.73F + f;
		}

		return fs;
	}
}

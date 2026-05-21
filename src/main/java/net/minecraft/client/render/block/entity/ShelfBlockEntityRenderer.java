package net.minecraft.client.render.block.entity;

import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShelfBlock;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.state.ShelfBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ShelfBlockEntityRenderer}.
 */
public class ShelfBlockEntityRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfBlockEntityRenderState> {

	private static final float ITEM_SCALE = 0.25F;
	private static final float BOTTOM_ALIGNED_OFFSET = -0.25F;
	private final ItemModelManager itemModelManager;

	public ShelfBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.itemModelManager = context.itemModelManager();
	}

	public ShelfBlockEntityRenderState createRenderState() {
		return new ShelfBlockEntityRenderState();
	}

	public void updateRenderState(
			ShelfBlockEntity shelfBlockEntity,
			ShelfBlockEntityRenderState shelfBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				shelfBlockEntity,
				shelfBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		shelfBlockEntityRenderState.alignItemsToBottom = shelfBlockEntity.shouldAlignItemsToBottom();
		DefaultedList<ItemStack> defaultedList = shelfBlockEntity.getHeldStacks();
		int i = HashCommon.long2int(shelfBlockEntity.getPos().asLong());

		for (int j = 0; j < defaultedList.size(); j++) {
			ItemStack itemStack = defaultedList.get(j);
			if (!itemStack.isEmpty()) {
				ItemRenderState itemRenderState = new ItemRenderState();
				this.itemModelManager
						.clearAndUpdate(
								itemRenderState,
								itemStack,
								ItemDisplayContext.ON_SHELF,
								shelfBlockEntity.getEntityWorld(),
								shelfBlockEntity,
								i + j
						);
				shelfBlockEntityRenderState.itemRenderStates[j] = itemRenderState;
			}
		}
	}

	public void render(
			ShelfBlockEntityRenderState shelfBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		Direction direction = shelfBlockEntityRenderState.blockState.get(ShelfBlock.FACING);
		float f = direction.getAxis().isHorizontal() ? -direction.getPositiveHorizontalDegrees() : 180.0F;

		for (int i = 0; i < shelfBlockEntityRenderState.itemRenderStates.length; i++) {
			ItemRenderState itemRenderState = shelfBlockEntityRenderState.itemRenderStates[i];
			if (itemRenderState != null) {
				this.renderItem(
						shelfBlockEntityRenderState,
						itemRenderState,
						matrixStack,
						orderedRenderCommandQueue,
						i,
						f
				);
			}
		}
	}

	private void renderItem(
			ShelfBlockEntityRenderState state,
			ItemRenderState itemRenderState,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int overlay,
			float rotationDegrees
	) {
		float f = (overlay - 1) * 0.3125F;
		Vec3d vec3d = new Vec3d(f, state.alignItemsToBottom ? -0.25 : 0.0, -0.25);
		matrices.push();
		matrices.translate(0.5F, 0.5F, 0.5F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDegrees));
		matrices.translate(vec3d);
		matrices.scale(0.25F, 0.25F, 0.25F);
		Box box = itemRenderState.getModelBoundingBox();
		double d = -box.minY;
		if (!state.alignItemsToBottom) {
			d += -(box.maxY - box.minY) / 2.0;
		}

		matrices.translate(0.0, d, 0.0);
		itemRenderState.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0);
		matrices.pop();
	}
}

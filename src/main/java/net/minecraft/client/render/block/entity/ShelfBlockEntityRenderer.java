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

/**
 * Рендерер блока полки. Отображает предметы на полке с учётом направления блока
 * и флага выравнивания по нижнему краю.
 */
@Environment(EnvType.CLIENT)
public class ShelfBlockEntityRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfBlockEntityRenderState> {

	private static final float ITEM_SCALE = 0.25F;
	private static final float BOTTOM_ALIGNED_OFFSET = -0.25F;
	private static final float SLOT_STRIDE = 0.3125F;
	private static final float SLOT_INDEX_OFFSET = 1;

	private final ItemModelManager itemModelManager;

	public ShelfBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.itemModelManager = context.itemModelManager();
	}

	@Override
	public ShelfBlockEntityRenderState createRenderState() {
		return new ShelfBlockEntityRenderState();
	}

	@Override
	public void updateRenderState(
			ShelfBlockEntity shelfBlockEntity,
			ShelfBlockEntityRenderState shelfBlockEntityRenderState,
			float tickProgress,
			Vec3d cameraPos,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				shelfBlockEntity,
				shelfBlockEntityRenderState,
				tickProgress,
				cameraPos,
				crumblingOverlayCommand
		);
		shelfBlockEntityRenderState.alignItemsToBottom = shelfBlockEntity.shouldAlignItemsToBottom();

		DefaultedList<ItemStack> heldStacks = shelfBlockEntity.getHeldStacks();
		int positionSeed = HashCommon.long2int(shelfBlockEntity.getPos().asLong());

		for (int slotIndex = 0; slotIndex < heldStacks.size(); slotIndex++) {
			ItemStack itemStack = heldStacks.get(slotIndex);
			if (itemStack.isEmpty()) {
				continue;
			}

			ItemRenderState itemRenderState = new ItemRenderState();
			this.itemModelManager.clearAndUpdate(
					itemRenderState,
					itemStack,
					ItemDisplayContext.ON_SHELF,
					shelfBlockEntity.getEntityWorld(),
					shelfBlockEntity,
					positionSeed + slotIndex
			);
			shelfBlockEntityRenderState.itemRenderStates[slotIndex] = itemRenderState;
		}
	}

	@Override
	public void render(
			ShelfBlockEntityRenderState shelfBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		Direction facing = shelfBlockEntityRenderState.blockState.get(ShelfBlock.FACING);
		float rotationDegrees = facing.getAxis().isHorizontal()
				? -facing.getPositiveHorizontalDegrees()
				: 180.0F;

		for (int slotIndex = 0; slotIndex < shelfBlockEntityRenderState.itemRenderStates.length; slotIndex++) {
			ItemRenderState itemRenderState = shelfBlockEntityRenderState.itemRenderStates[slotIndex];
			if (itemRenderState == null) {
				continue;
			}

			renderItem(shelfBlockEntityRenderState, itemRenderState, matrixStack, orderedRenderCommandQueue, slotIndex, rotationDegrees);
		}
	}

	private void renderItem(
			ShelfBlockEntityRenderState state,
			ItemRenderState itemRenderState,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int slotIndex,
			float rotationDegrees
	) {
		float slotOffset = (slotIndex - SLOT_INDEX_OFFSET) * SLOT_STRIDE;
		Vec3d itemOffset = new Vec3d(slotOffset, state.alignItemsToBottom ? BOTTOM_ALIGNED_OFFSET : 0.0, BOTTOM_ALIGNED_OFFSET);

		matrices.push();
		matrices.translate(0.5F, 0.5F, 0.5F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDegrees));
		matrices.translate(itemOffset);
		matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

		Box modelBounds = itemRenderState.getModelBoundingBox();
		double verticalOffset = -modelBounds.minY;
		if (!state.alignItemsToBottom) {
			verticalOffset += -(modelBounds.maxY - modelBounds.minY) / 2.0;
		}

		matrices.translate(0.0, verticalOffset, 0.0);
		itemRenderState.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0);
		matrices.pop();
	}
}

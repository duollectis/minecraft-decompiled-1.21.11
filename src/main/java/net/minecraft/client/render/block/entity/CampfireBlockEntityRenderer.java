package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.state.CampfireBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code CampfireBlockEntityRenderer}.
 */
public class CampfireBlockEntityRenderer implements BlockEntityRenderer<CampfireBlockEntity, CampfireBlockEntityRenderState> {

	private static final float SCALE = 0.375F;
	private final ItemModelManager itemModelManager;

	public CampfireBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.itemModelManager = ctx.itemModelManager();
	}

	/**
	 * Создаёт render state.
	 *
	 * @return CampfireBlockEntityRenderState — результат операции
	 */
	public CampfireBlockEntityRenderState createRenderState() {
		return new CampfireBlockEntityRenderState();
	}

	public void updateRenderState(
			CampfireBlockEntity campfireBlockEntity,
			CampfireBlockEntityRenderState campfireBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				campfireBlockEntity,
				campfireBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		campfireBlockEntityRenderState.facing = campfireBlockEntity.getCachedState().get(CampfireBlock.FACING);
		int i = (int) campfireBlockEntity.getPos().asLong();
		campfireBlockEntityRenderState.cookedItemStates = new ArrayList<>();

		for (int j = 0; j < campfireBlockEntity.getItemsBeingCooked().size(); j++) {
			ItemRenderState itemRenderState = new ItemRenderState();
			this.itemModelManager
					.clearAndUpdate(
							itemRenderState,
							campfireBlockEntity.getItemsBeingCooked().get(j),
							ItemDisplayContext.FIXED,
							campfireBlockEntity.getWorld(),
							null,
							i + j
					);
			campfireBlockEntityRenderState.cookedItemStates.add(itemRenderState);
		}
	}

	public void render(
			CampfireBlockEntityRenderState campfireBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		Direction direction = campfireBlockEntityRenderState.facing;
		List<ItemRenderState> list = campfireBlockEntityRenderState.cookedItemStates;

		for (int i = 0; i < list.size(); i++) {
			ItemRenderState itemRenderState = list.get(i);
			if (!itemRenderState.isEmpty()) {
				matrixStack.push();
				matrixStack.translate(0.5F, 0.44921875F, 0.5F);
				Direction
						direction2 =
						Direction.fromHorizontalQuarterTurns((i + direction.getHorizontalQuarterTurns()) % 4);
				float f = -direction2.getPositiveHorizontalDegrees();
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f));
				matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
				matrixStack.translate(-0.3125F, -0.3125F, 0.0F);
				matrixStack.scale(0.375F, 0.375F, 0.375F);
				itemRenderState.render(
						matrixStack,
						orderedRenderCommandQueue,
						campfireBlockEntityRenderState.lightmapCoordinates,
						OverlayTexture.DEFAULT_UV,
						0
				);
				matrixStack.pop();
			}
		}
	}
}

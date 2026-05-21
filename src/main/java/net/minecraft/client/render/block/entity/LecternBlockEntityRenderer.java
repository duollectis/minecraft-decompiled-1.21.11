package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.state.LecternBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code LecternBlockEntityRenderer}.
 */
public class LecternBlockEntityRenderer implements BlockEntityRenderer<LecternBlockEntity, LecternBlockEntityRenderState> {

	private final SpriteHolder spriteHolder;
	private final BookModel book;
	private final BookModel.BookModelState bookModelState = new BookModel.BookModelState(0.0F, 0.1F, 0.9F, 1.2F);

	public LecternBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.spriteHolder = ctx.spriteHolder();
		this.book = new BookModel(ctx.getLayerModelPart(EntityModelLayers.BOOK));
	}

	public LecternBlockEntityRenderState createRenderState() {
		return new LecternBlockEntityRenderState();
	}

	public void updateRenderState(
			LecternBlockEntity lecternBlockEntity,
			LecternBlockEntityRenderState lecternBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				lecternBlockEntity,
				lecternBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		lecternBlockEntityRenderState.hasBook = lecternBlockEntity.getCachedState().get(LecternBlock.HAS_BOOK);
		lecternBlockEntityRenderState.bookRotationDegrees = lecternBlockEntity.getCachedState()
		                                                                      .get(LecternBlock.FACING)
		                                                                      .rotateYClockwise()
		                                                                      .getPositiveHorizontalDegrees();
	}

	public void render(
			LecternBlockEntityRenderState lecternBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (lecternBlockEntityRenderState.hasBook) {
			matrixStack.push();
			matrixStack.translate(0.5F, 1.0625F, 0.5F);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-lecternBlockEntityRenderState.bookRotationDegrees));
			matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(67.5F));
			matrixStack.translate(0.0F, -0.125F, 0.0F);
			orderedRenderCommandQueue.submitModel(
					this.book,
					this.bookModelState,
					matrixStack,
					EnchantingTableBlockEntityRenderer.BOOK_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
					lecternBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					-1,
					this.spriteHolder.getSprite(EnchantingTableBlockEntityRenderer.BOOK_TEXTURE),
					0,
					lecternBlockEntityRenderState.crumblingOverlay
			);
			matrixStack.pop();
		}
	}
}

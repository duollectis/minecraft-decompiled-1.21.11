package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.state.special.BannerResultGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
/**
 * {@code BannerResultGuiElementRenderer}.
 */
public class BannerResultGuiElementRenderer extends SpecialGuiElementRenderer<BannerResultGuiElementRenderState> {

	private final SpriteHolder sprite;

	public BannerResultGuiElementRenderer(VertexConsumerProvider.Immediate immediate, SpriteHolder sprite) {
		super(immediate);
		this.sprite = sprite;
	}

	@Override
	public Class<BannerResultGuiElementRenderState> getElementClass() {
		return BannerResultGuiElementRenderState.class;
	}

	protected void render(
			BannerResultGuiElementRenderState bannerResultGuiElementRenderState,
			MatrixStack matrixStack
	) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);
		matrixStack.translate(0.0F, 0.25F, 0.0F);
		RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
		OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl = renderDispatcher.getQueue();
		BannerBlockEntityRenderer.renderCanvas(
				this.sprite,
				matrixStack,
				orderedRenderCommandQueueImpl,
				15728880,
				OverlayTexture.DEFAULT_UV,
				bannerResultGuiElementRenderState.flag(),
				0.0F,
				ModelBaker.BANNER_BASE,
				true,
				bannerResultGuiElementRenderState.baseColor(),
				bannerResultGuiElementRenderState.resultBannerPatterns(),
				false,
				null,
				0
		);
		renderDispatcher.render();
	}

	@Override
	protected String getName() {
		return "banner result";
	}
}

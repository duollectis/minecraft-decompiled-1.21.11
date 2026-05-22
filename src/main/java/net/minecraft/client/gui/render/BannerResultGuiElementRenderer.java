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

/**
 * Рендерер результата крафта баннера в GUI.
 * Отображает итоговый баннер с применёнными паттернами в интерфейсе ткацкого станка.
 */
@Environment(EnvType.CLIENT)
public class BannerResultGuiElementRenderer extends SpecialGuiElementRenderer<BannerResultGuiElementRenderState> {

	private static final float BANNER_Y_OFFSET = 0.25F;

	private final SpriteHolder sprite;

	public BannerResultGuiElementRenderer(VertexConsumerProvider.Immediate immediate, SpriteHolder sprite) {
		super(immediate);
		this.sprite = sprite;
	}

	@Override
	public Class<BannerResultGuiElementRenderState> getElementClass() {
		return BannerResultGuiElementRenderState.class;
	}

	@Override
	protected void render(BannerResultGuiElementRenderState state, MatrixStack matrices) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);

		matrices.translate(0.0F, BANNER_Y_OFFSET, 0.0F);

		RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
		OrderedRenderCommandQueueImpl commandQueue = renderDispatcher.getQueue();

		BannerBlockEntityRenderer.renderCanvas(
				sprite,
				matrices,
				commandQueue,
				15728880,
				OverlayTexture.DEFAULT_UV,
				state.flag(),
				0.0F,
				ModelBaker.BANNER_BASE,
				true,
				state.baseColor(),
				state.resultBannerPatterns(),
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

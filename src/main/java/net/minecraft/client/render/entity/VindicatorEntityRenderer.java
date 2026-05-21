package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code VindicatorEntityRenderer}.
 */
public class VindicatorEntityRenderer extends IllagerEntityRenderer<VindicatorEntity, IllagerEntityRenderState> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/illager/vindicator.png");

	public VindicatorEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new IllagerEntityModel<>(context.getPart(EntityModelLayers.VINDICATOR)), 0.5F);
		this.addFeature(
				new HeldItemFeatureRenderer<IllagerEntityRenderState, IllagerEntityModel<IllagerEntityRenderState>>(this) {
					public void render(
							MatrixStack matrixStack,
							OrderedRenderCommandQueue orderedRenderCommandQueue,
							int i,
							IllagerEntityRenderState illagerEntityRenderState,
							float f,
							float g
					) {
						if (illagerEntityRenderState.attacking) {
							super.render(matrixStack, orderedRenderCommandQueue, i, illagerEntityRenderState, f, g);
						}
					}
				}
		);
	}

	public Identifier getTexture(IllagerEntityRenderState illagerEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return IllagerEntityRenderState — результат операции
	 */
	public IllagerEntityRenderState createRenderState() {
		return new IllagerEntityRenderState();
	}
}

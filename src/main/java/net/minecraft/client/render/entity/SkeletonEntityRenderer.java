package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code SkeletonEntityRenderer}.
 */
public class SkeletonEntityRenderer extends AbstractSkeletonEntityRenderer<SkeletonEntity, SkeletonEntityRenderState> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");

	public SkeletonEntityRenderer(EntityRendererFactory.Context context) {
		super(context, EntityModelLayers.SKELETON, EntityModelLayers.SKELETON_EQUIPMENT);
	}

	public Identifier getTexture(SkeletonEntityRenderState skeletonEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return SkeletonEntityRenderState — результат операции
	 */
	public SkeletonEntityRenderState createRenderState() {
		return new SkeletonEntityRenderState();
	}
}

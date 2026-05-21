package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code SkeletonEntityRenderState}.
 */
public class SkeletonEntityRenderState extends BipedEntityRenderState {

	public boolean attacking;
	public boolean shaking;
	public boolean holdingBow;
}

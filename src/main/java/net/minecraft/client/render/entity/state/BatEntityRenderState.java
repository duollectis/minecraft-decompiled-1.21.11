package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.AnimationState;

@Environment(EnvType.CLIENT)
/**
 * {@code BatEntityRenderState}.
 */
public class BatEntityRenderState extends LivingEntityRenderState {

	public boolean roosting;
	public final AnimationState flyingAnimationState = new AnimationState();
	public final AnimationState roostingAnimationState = new AnimationState();
}

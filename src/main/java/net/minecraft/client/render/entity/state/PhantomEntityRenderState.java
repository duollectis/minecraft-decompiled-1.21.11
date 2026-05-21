package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code PhantomEntityRenderState}.
 */
public class PhantomEntityRenderState extends LivingEntityRenderState {

	public float wingFlapProgress;
	public int size;
}

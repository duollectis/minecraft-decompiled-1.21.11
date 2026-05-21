package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code BeeEntityRenderState}.
 */
public class BeeEntityRenderState extends LivingEntityRenderState {

	public float bodyPitch;
	public boolean hasStinger = true;
	public boolean stoppedOnGround;
	public boolean angry;
	public boolean hasNectar;
}

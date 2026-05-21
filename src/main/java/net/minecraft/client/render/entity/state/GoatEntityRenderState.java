package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code GoatEntityRenderState}.
 */
public class GoatEntityRenderState extends LivingEntityRenderState {

	public boolean hasLeftHorn = true;
	public boolean hasRightHorn = true;
	public float headPitch;
}

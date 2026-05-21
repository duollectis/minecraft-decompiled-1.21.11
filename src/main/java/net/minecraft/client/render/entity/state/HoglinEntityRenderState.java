package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code HoglinEntityRenderState}.
 */
public class HoglinEntityRenderState extends LivingEntityRenderState {

	public int movementCooldownTicks;
	public boolean canConvert;
}

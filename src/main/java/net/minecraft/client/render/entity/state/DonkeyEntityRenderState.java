package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code DonkeyEntityRenderState}.
 */
public class DonkeyEntityRenderState extends LivingHorseEntityRenderState {

	public boolean hasChest;
}

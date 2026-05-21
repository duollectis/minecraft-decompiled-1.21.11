package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombieEntityRenderState}.
 */
public class ZombieEntityRenderState extends LancerEntityRenderState {

	public boolean attacking;
	public boolean convertingInWater;
}

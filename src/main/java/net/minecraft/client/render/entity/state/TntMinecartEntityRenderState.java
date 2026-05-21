package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code TntMinecartEntityRenderState}.
 */
public class TntMinecartEntityRenderState extends MinecartEntityRenderState {

	public float fuseTicks = -1.0F;
}

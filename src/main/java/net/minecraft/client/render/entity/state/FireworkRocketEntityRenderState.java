package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code FireworkRocketEntityRenderState}.
 */
public class FireworkRocketEntityRenderState extends EntityRenderState {

	public boolean shotAtAngle;
	public final ItemRenderState stack = new ItemRenderState();
}

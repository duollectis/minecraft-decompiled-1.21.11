package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code ItemEntityRenderState}.
 */
public class ItemEntityRenderState extends ItemStackEntityRenderState {

	public float uniqueOffset;
}

package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.MovingBlockRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code FallingBlockEntityRenderState}.
 */
public class FallingBlockEntityRenderState extends EntityRenderState {

	public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}

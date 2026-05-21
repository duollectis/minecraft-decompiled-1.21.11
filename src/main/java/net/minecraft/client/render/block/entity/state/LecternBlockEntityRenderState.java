package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code LecternBlockEntityRenderState}.
 */
public class LecternBlockEntityRenderState extends BlockEntityRenderState {

	public boolean hasBook;
	public float bookRotationDegrees;
}

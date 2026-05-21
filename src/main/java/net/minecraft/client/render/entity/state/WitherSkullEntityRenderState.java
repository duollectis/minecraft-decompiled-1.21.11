package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;

@Environment(EnvType.CLIENT)
/**
 * {@code WitherSkullEntityRenderState}.
 */
public class WitherSkullEntityRenderState extends EntityRenderState {

	public boolean charged;
	public final SkullBlockEntityModel.SkullModelState skullState = new SkullBlockEntityModel.SkullModelState();
}

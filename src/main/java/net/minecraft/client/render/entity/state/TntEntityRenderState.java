package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code TntEntityRenderState}.
 */
public class TntEntityRenderState extends EntityRenderState {

	public float fuse;
	public @Nullable BlockState blockState;
}

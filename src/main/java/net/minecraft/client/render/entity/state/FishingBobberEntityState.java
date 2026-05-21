package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
/**
 * {@code FishingBobberEntityState}.
 */
public class FishingBobberEntityState extends EntityRenderState {

	public Vec3d pos = Vec3d.ZERO;
}

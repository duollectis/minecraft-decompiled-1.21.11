package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code EndCrystalEntityRenderState}.
 */
public class EndCrystalEntityRenderState extends EntityRenderState {

	public boolean baseVisible = true;
	public @Nullable Vec3d beamOffset;
}

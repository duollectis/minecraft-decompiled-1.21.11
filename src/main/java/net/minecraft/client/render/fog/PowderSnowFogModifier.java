package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code PowderSnowFogModifier}.
 */
public class PowderSnowFogModifier extends FogModifier {

	private static final int FOG_COLOR = -6308916;

	@Override
	public int getFogColor(ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {
		return FOG_COLOR;
	}

	@Override
	public void applyStartEndModifier(
			FogData data,
			Camera camera,
			ClientWorld clientWorld,
			float f,
			RenderTickCounter renderTickCounter
	) {
		if (camera.getFocusedEntity().isSpectator()) {
			data.environmentalStart = -8.0F;
			data.environmentalEnd = f * 0.5F;
		}
		else {
			data.environmentalStart = 0.0F;
			data.environmentalEnd = 2.0F;
		}

		data.skyEnd = data.environmentalEnd;
		data.cloudEnd = data.environmentalEnd;
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return submersionType == CameraSubmersionType.POWDER_SNOW;
	}
}

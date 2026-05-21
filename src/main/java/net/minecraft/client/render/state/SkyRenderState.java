package net.minecraft.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.dimension.DimensionType;

@Environment(EnvType.CLIENT)
/**
 * {@code SkyRenderState}.
 */
public class SkyRenderState implements FabricRenderState {

	public DimensionType.Skybox skybox = DimensionType.Skybox.NONE;
	public boolean shouldRenderSkyDark;
	public float sunAngle;
	public float moonAngle;
	public float starAngle;
	public float rainGradient;
	public float starBrightness;
	public int sunriseAndSunsetColor;
	public MoonPhase moonPhase = MoonPhase.FULL_MOON;
	public int skyColor;
	public float endFlashIntensity;
	public float endFlashPitch;
	public float endFlashYaw;

	/**
	 * Clear.
	 */
	public void clear() {
		this.skybox = DimensionType.Skybox.NONE;
	}
}

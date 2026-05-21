package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code FogModifier}.
 */
public abstract class FogModifier {

	public abstract void applyStartEndModifier(
			FogData data,
			Camera camera,
			ClientWorld clientWorld,
			float f,
			RenderTickCounter renderTickCounter
	);

	public boolean isColorSource() {
		return true;
	}

	public int getFogColor(ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {
		return -1;
	}

	public boolean isDarknessModifier() {
		return false;
	}

	/**
	 * Применяет darkness modifier.
	 *
	 * @param cameraEntity camera entity
	 * @param darkness darkness
	 * @param tickProgress tick progress
	 *
	 * @return float — результат операции
	 */
	public float applyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress) {
		return darkness;
	}

	/**
	 * Определяет, следует ли apply.
	 *
	 * @param submersionType submersion type
	 * @param cameraEntity camera entity
	 *
	 * @return boolean — результат операции
	 */
	public abstract boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity);
}

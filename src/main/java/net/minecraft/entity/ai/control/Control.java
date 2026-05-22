package net.minecraft.entity.ai.control;

import net.minecraft.util.math.MathHelper;

/** Базовый интерфейс для всех контроллеров моба (движение, взгляд, прыжок). */
public interface Control {

	default float changeAngle(float start, float end, float maxChange) {
		float delta = MathHelper.subtractAngles(start, end);
		float clamped = MathHelper.clamp(delta, -maxChange, maxChange);
		return start + clamped;
	}
}

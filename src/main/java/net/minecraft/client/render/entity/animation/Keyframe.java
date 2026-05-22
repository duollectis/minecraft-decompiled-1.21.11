package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3fc;

/**
 * Ключевой кадр анимации: момент времени и целевое значение трансформации.
 * <p>
 * Хранит два вектора — {@code preTarget} (значение на выходе из предыдущего кадра)
 * и {@code postTarget} (значение на входе в следующий кадр). Для большинства
 * кадров они совпадают; разные значения используются при кубической интерполяции
 * для управления касательными кривой Катмулла–Рома.
 */
@Environment(EnvType.CLIENT)
public record Keyframe(
		float timestamp,
		Vector3fc preTarget,
		Vector3fc postTarget,
		Transformation.Interpolation interpolation
) {

	/** Создаёт кадр с одинаковыми {@code preTarget} и {@code postTarget}. */
	public Keyframe(float timestamp, Vector3fc target, Transformation.Interpolation interpolation) {
		this(timestamp, target, target, interpolation);
	}
}

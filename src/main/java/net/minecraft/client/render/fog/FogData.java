package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Изменяемый контейнер параметров тумана для одного кадра.
 * <p>
 * Заполняется цепочкой {@link FogModifier}: каждый модификатор может
 * переопределить начало и конец тумана окружения, дальности прорисовки,
 * неба и облаков. Итоговые значения передаются в UBO шейдера тумана.
 */
@Environment(EnvType.CLIENT)
public class FogData {

	public float environmentalStart;
	public float renderDistanceStart;
	public float environmentalEnd;
	public float renderDistanceEnd;
	public float skyEnd;
	public float cloudEnd;
}

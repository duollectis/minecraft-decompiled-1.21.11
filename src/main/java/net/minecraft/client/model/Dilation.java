package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Описывает расширение (раздутие) кубоида модели по каждой из трёх осей.
 * Используется при построении {@link ModelPartData} для создания слоёв брони
 * и других эффектов, требующих увеличения геометрии поверх базовой модели.
 */
@Environment(EnvType.CLIENT)
public class Dilation {

	public static final Dilation NONE = new Dilation(0.0F);

	final float radiusX;
	final float radiusY;
	final float radiusZ;

	public Dilation(float radiusX, float radiusY, float radiusZ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.radiusZ = radiusZ;
	}

	public Dilation(float radius) {
		this(radius, radius, radius);
	}

	/** Возвращает новый экземпляр с равномерно увеличенным радиусом по всем осям. */
	public Dilation add(float radius) {
		return new Dilation(radiusX + radius, radiusY + radius, radiusZ + radius);
	}

	/** Возвращает новый экземпляр с независимым увеличением радиуса по каждой оси. */
	public Dilation add(float addX, float addY, float addZ) {
		return new Dilation(radiusX + addX, radiusY + addY, radiusZ + addZ);
	}
}

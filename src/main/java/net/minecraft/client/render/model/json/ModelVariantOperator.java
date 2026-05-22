package net.minecraft.client.render.model.json;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;

import java.util.function.UnaryOperator;

/**
 * Функциональный интерфейс для применения трансформации к {@link ModelVariant}.
 * Предоставляет готовые экземпляры {@link Settings} для каждого параметра варианта:
 * вращение по осям X/Y/Z, идентификатор модели и UV-lock.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface ModelVariantOperator extends UnaryOperator<ModelVariant> {

	ModelVariantOperator.Settings<AxisRotation> ROTATION_X = ModelVariant::withRotationX;

	ModelVariantOperator.Settings<AxisRotation> ROTATION_Y = ModelVariant::withRotationY;

	ModelVariantOperator.Settings<AxisRotation> ROTATION_GETTER = ModelVariant::withRotationZ;

	ModelVariantOperator.Settings<Identifier> MODEL = ModelVariant::withModel;

	ModelVariantOperator.Settings<Boolean> UV_LOCK = ModelVariant::withUVLock;

	default ModelVariantOperator then(ModelVariantOperator variant) {
		return variantx -> variant.apply(this.apply(variantx));
	}

	/**
	 * Параметризованная настройка варианта модели: связывает конкретное значение типа {@code T}
	 * с соответствующим полем {@link ModelVariant} через метод {@link #withValue}.
	 *
	 * @param <T> тип значения настройки (AxisRotation, Identifier, Boolean)
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface Settings<T> {

		ModelVariant apply(ModelVariant variant, T value);

		default ModelVariantOperator withValue(T value) {
			return setting -> this.apply(setting, value);
		}
	}
}

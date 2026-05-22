package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.ModelTransformer;

import java.util.function.UnaryOperator;

/**
 * Иерархическое описание модели в виде дерева {@link ModelPartData}.
 * Используется как промежуточное представление при построении модели:
 * сначала создаётся {@code ModelData}, затем из него строится {@link ModelPart}.
 * Поддерживает применение трансформаций ко всему дереву через {@link #transform}.
 */
@Environment(EnvType.CLIENT)
public class ModelData {

	private final ModelPartData data;

	public ModelData() {
		this(new ModelPartData(ImmutableList.of(), ModelTransform.NONE));
	}

	private ModelData(ModelPartData data) {
		this.data = data;
	}

	public ModelPartData getRoot() {
		return data;
	}

	/**
	 * Применяет функцию трансформации ко всем частям дерева и возвращает новый экземпляр.
	 *
	 * @param transformer функция преобразования {@link ModelTransform}
	 * @return новый {@code ModelData} с применёнными трансформациями
	 */
	public ModelData transform(UnaryOperator<ModelTransform> transformer) {
		return new ModelData(data.applyTransformer(transformer));
	}

	/**
	 * Применяет {@link ModelTransformer} к данному экземпляру и возвращает результат.
	 *
	 * @param transformer трансформер модели
	 * @return преобразованный {@code ModelData}
	 */
	public ModelData transform(ModelTransformer transformer) {
		return transformer.apply(this);
	}
}

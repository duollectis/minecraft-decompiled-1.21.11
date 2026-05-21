package net.minecraft.client.model;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelPartData}.
 */
public class ModelPartData {

	private final List<ModelCuboidData> cuboidData;
	private final ModelTransform transform;
	private final Map<String, ModelPartData> children = Maps.newHashMap();

	ModelPartData(List<ModelCuboidData> cuboidData, ModelTransform transform) {
		this.cuboidData = cuboidData;
		this.transform = transform;
	}

	/**
	 * Добавляет child.
	 *
	 * @param name name
	 * @param builder builder
	 * @param transform transform
	 *
	 * @return ModelPartData — результат операции
	 */
	public ModelPartData addChild(String name, ModelPartBuilder builder, ModelTransform transform) {
		ModelPartData modelPartData = new ModelPartData(builder.build(), transform);
		return this.addChild(name, modelPartData);
	}

	/**
	 * Добавляет child.
	 *
	 * @param name name
	 * @param data data
	 *
	 * @return ModelPartData — результат операции
	 */
	public ModelPartData addChild(String name, ModelPartData data) {
		ModelPartData modelPartData = this.children.put(name, data);
		if (modelPartData != null) {
			data.children.putAll(modelPartData.children);
		}

		return data;
	}

	/**
	 * Сбрасывает children parts.
	 *
	 * @return ModelPartData — результат операции
	 */
	public ModelPartData resetChildrenParts() {
		for (String string : this.children.keySet()) {
			this.resetChildrenParts(string).resetChildrenParts();
		}

		return this;
	}

	/**
	 * Сбрасывает children parts.
	 *
	 * @param name name
	 *
	 * @return ModelPartData — результат операции
	 */
	public ModelPartData resetChildrenParts(String name) {
		ModelPartData modelPartData = this.children.get(name);
		if (modelPartData == null) {
			throw new IllegalArgumentException("No child with name: " + name);
		}
		else {
			return this.addChild(name, ModelPartBuilder.create(), modelPartData.transform);
		}
	}

	/**
	 * Сбрасывает children except.
	 *
	 * @param names names
	 */
	public void resetChildrenExcept(Set<String> names) {
		for (Entry<String, ModelPartData> entry : this.children.entrySet()) {
			ModelPartData modelPartData = entry.getValue();
			if (!names.contains(entry.getKey())) {
				this
						.addChild(entry.getKey(), ModelPartBuilder.create(), modelPartData.transform)
						.resetChildrenExcept(names);
			}
		}
	}

	/**
	 * Сбрасывает children except exact.
	 *
	 * @param names names
	 */
	public void resetChildrenExceptExact(Set<String> names) {
		for (Entry<String, ModelPartData> entry : this.children.entrySet()) {
			ModelPartData modelPartData = entry.getValue();
			if (names.contains(entry.getKey())) {
				modelPartData.resetChildrenParts();
			}
			else {
				this
						.addChild(entry.getKey(), ModelPartBuilder.create(), modelPartData.transform)
						.resetChildrenExceptExact(names);
			}
		}
	}

	/**
	 * Создаёт part.
	 *
	 * @param textureWidth texture width
	 * @param textureHeight texture height
	 *
	 * @return ModelPart — результат операции
	 */
	public ModelPart createPart(int textureWidth, int textureHeight) {
		Object2ObjectArrayMap<String, ModelPart> object2ObjectArrayMap = this.children
				.entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								Entry::getKey,
								entry -> ((ModelPartData) entry.getValue()).createPart(textureWidth, textureHeight),
								(name, partData) -> name,
								Object2ObjectArrayMap::new
						)
				);
		List<ModelPart.Cuboid>
				list =
				this.cuboidData.stream().map(data -> data.createCuboid(textureWidth, textureHeight)).toList();
		ModelPart modelPart = new ModelPart(list, object2ObjectArrayMap);
		modelPart.setDefaultTransform(this.transform);
		modelPart.setTransform(this.transform);
		return modelPart;
	}

	public ModelPartData getChild(String name) {
		return this.children.get(name);
	}

	public Set<Entry<String, ModelPartData>> getChildren() {
		return this.children.entrySet();
	}

	/**
	 * Применяет transformer.
	 *
	 * @param transformer transformer
	 *
	 * @return ModelPartData — результат операции
	 */
	public ModelPartData applyTransformer(UnaryOperator<ModelTransform> transformer) {
		ModelPartData modelPartData = new ModelPartData(this.cuboidData, transformer.apply(this.transform));
		modelPartData.children.putAll(this.children);
		return modelPartData;
	}
}

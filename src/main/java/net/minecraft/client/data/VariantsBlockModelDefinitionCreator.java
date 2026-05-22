package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.json.BlockModelDefinition;
import net.minecraft.client.render.model.json.ModelVariantOperator;
import net.minecraft.client.render.model.json.WeightedVariant;
import net.minecraft.state.property.Property;

import java.util.*;
import java.util.stream.Stream;

/**
 * Реализация {@link BlockModelDefinitionCreator}, генерирующая определение блока на основе
 * набора вариантов модели, сопоставленных с конкретными комбинациями значений свойств блока.
 */
@Environment(EnvType.CLIENT)
public class VariantsBlockModelDefinitionCreator implements BlockModelDefinitionCreator {

	private final Block block;
	private final List<VariantsBlockModelDefinitionCreator.Entry> variants;
	private final Set<Property<?>> definedProperties;

	VariantsBlockModelDefinitionCreator(
			Block block,
			List<VariantsBlockModelDefinitionCreator.Entry> variants,
			Set<Property<?>> definedProperties
	) {
		this.block = block;
		this.variants = variants;
		this.definedProperties = definedProperties;
	}

	static Set<Property<?>> validateAndAddProperties(
			Set<Property<?>> definedProperties,
			Block block,
			BlockStateVariantMap<?> variantMap
	) {
		List<Property<?>> properties = variantMap.getProperties();

		properties.forEach(property -> {
			if (block.getStateManager().getProperty(property.getName()) != property) {
				throw new IllegalStateException("Property " + property + " is not defined for block " + block);
			}

			if (definedProperties.contains(property)) {
				throw new IllegalStateException(
						"Values of property " + property + " already defined for block " + block);
			}
		});

		Set<Property<?>> merged = new HashSet<>(definedProperties);
		merged.addAll(properties);

		return merged;
	}

	public VariantsBlockModelDefinitionCreator apply(BlockStateVariantMap<ModelVariantOperator> operators) {
		Set<Property<?>> merged = validateAndAddProperties(definedProperties, block, operators);
		List<Entry> applied = variants.stream()
				.flatMap(variant -> variant.apply(operators))
				.toList();

		return new VariantsBlockModelDefinitionCreator(block, applied, merged);
	}

	public VariantsBlockModelDefinitionCreator apply(ModelVariantOperator operator) {
		List<Entry> applied = variants.stream()
				.flatMap(variant -> variant.apply(operator))
				.toList();

		return new VariantsBlockModelDefinitionCreator(block, applied, definedProperties);
	}

	@Override
	public BlockModelDefinition createBlockModelDefinition() {
		Map<String, BlockStateModel.Unbaked> variantModels = new HashMap<>();

		for (Entry entry : variants) {
			variantModels.put(entry.properties.asString(), entry.variant.toModel());
		}

		return new BlockModelDefinition(Optional.of(new BlockModelDefinition.Variants(variantModels)), Optional.empty());
	}

	@Override
	public Block getBlock() {
		return block;
	}

	public static Empty of(Block block) {
		return new Empty(block);
	}

	public static VariantsBlockModelDefinitionCreator of(Block block, WeightedVariant model) {
		return new VariantsBlockModelDefinitionCreator(
				block,
				List.of(new Entry(PropertiesMap.EMPTY, model)),
				Set.of()
		);
	}

	/**
	 * Промежуточный строитель для создания {@link VariantsBlockModelDefinitionCreator}
	 * без предварительно заданных вариантов — принимает карту вариантов через {@link #with}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Empty {

		private final Block block;

		public Empty(Block block) {
			this.block = block;
		}

		public VariantsBlockModelDefinitionCreator with(BlockStateVariantMap<WeightedVariant> variantMap) {
			Set<Property<?>> merged = validateAndAddProperties(Set.of(), block, variantMap);
			List<Entry> entries = variantMap.getVariants()
					.entrySet()
					.stream()
					.map(e -> new Entry(e.getKey(), e.getValue()))
					.toList();

			return new VariantsBlockModelDefinitionCreator(block, entries, merged);
		}
	}

	/**
	 * Пара «карта свойств → взвешенный вариант модели», используемая при построении
	 * финального {@link net.minecraft.client.render.model.json.BlockModelDefinition}.
	 */
	@Environment(EnvType.CLIENT)
	record Entry(PropertiesMap properties, WeightedVariant variant) {

		public Stream<Entry> apply(BlockStateVariantMap<ModelVariantOperator> operatorMap) {
			return operatorMap.getVariants().entrySet().stream().map(e -> {
				PropertiesMap merged = properties.copyOf(e.getKey());
				WeightedVariant applied = variant.apply(e.getValue());

				return new Entry(merged, applied);
			});
		}

		public Stream<Entry> apply(ModelVariantOperator operator) {
			return Stream.of(new Entry(properties, variant.apply(operator)));
		}
	}
}

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

@Environment(EnvType.CLIENT)
/**
 * {@code VariantsBlockModelDefinitionCreator}.
 */
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
		List<Property<?>> list = variantMap.getProperties();
		list.forEach(property -> {
			if (block.getStateManager().getProperty(property.getName()) != property) {
				throw new IllegalStateException("Property " + property + " is not defined for block " + block);
			}
			else if (definedProperties.contains(property)) {
				throw new IllegalStateException(
						"Values of property " + property + " already defined for block " + block);
			}
		});
		Set<Property<?>> set = new HashSet<>(definedProperties);
		set.addAll(list);
		return set;
	}

	public VariantsBlockModelDefinitionCreator apply(BlockStateVariantMap<ModelVariantOperator> operators) {
		Set<Property<?>> set = validateAndAddProperties(this.definedProperties, this.block, operators);
		List<VariantsBlockModelDefinitionCreator.Entry>
				list =
				this.variants.stream().flatMap(variant -> variant.apply(operators)).toList();
		return new VariantsBlockModelDefinitionCreator(this.block, list, set);
	}

	public VariantsBlockModelDefinitionCreator apply(ModelVariantOperator operator) {
		List<VariantsBlockModelDefinitionCreator.Entry>
				list =
				this.variants.stream().flatMap(variant -> variant.apply(operator)).toList();
		return new VariantsBlockModelDefinitionCreator(this.block, list, this.definedProperties);
	}

	@Override
	public BlockModelDefinition createBlockModelDefinition() {
		Map<String, BlockStateModel.Unbaked> map = new HashMap<>();

		for (VariantsBlockModelDefinitionCreator.Entry entry : this.variants) {
			map.put(entry.properties.asString(), entry.variant.toModel());
		}

		return new BlockModelDefinition(Optional.of(new BlockModelDefinition.Variants(map)), Optional.empty());
	}

	@Override
	public Block getBlock() {
		return this.block;
	}

	public static VariantsBlockModelDefinitionCreator.Empty of(Block block) {
		return new VariantsBlockModelDefinitionCreator.Empty(block);
	}

	public static VariantsBlockModelDefinitionCreator of(Block block, WeightedVariant model) {
		return new VariantsBlockModelDefinitionCreator(
				block,
				List.of(new VariantsBlockModelDefinitionCreator.Entry(PropertiesMap.EMPTY, model)),
				Set.of()
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Empty}.
	 */
	public static class Empty {

		private final Block block;

		public Empty(Block block) {
			this.block = block;
		}

		public VariantsBlockModelDefinitionCreator with(BlockStateVariantMap<WeightedVariant> variantMap) {
			Set<Property<?>>
					set =
					VariantsBlockModelDefinitionCreator.validateAndAddProperties(Set.of(), this.block, variantMap);
			List<VariantsBlockModelDefinitionCreator.Entry> list = variantMap.getVariants()
			                                                                 .entrySet()
			                                                                 .stream()
			                                                                 .map(entry -> new VariantsBlockModelDefinitionCreator.Entry(
					                                                                 entry.getKey(),
					                                                                 entry.getValue()
			                                                                 ))
			                                                                 .toList();
			return new VariantsBlockModelDefinitionCreator(this.block, list, set);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	record Entry(PropertiesMap properties, WeightedVariant variant) {

		public Stream<VariantsBlockModelDefinitionCreator.Entry> apply(BlockStateVariantMap<ModelVariantOperator> operatorMap) {
			return operatorMap.getVariants().entrySet().stream().map(variant -> {
				PropertiesMap propertiesMap = this.properties.copyOf(variant.getKey());
				WeightedVariant weightedVariant = this.variant.apply(variant.getValue());
				return new VariantsBlockModelDefinitionCreator.Entry(propertiesMap, weightedVariant);
			});
		}

		public Stream<VariantsBlockModelDefinitionCreator.Entry> apply(ModelVariantOperator operator) {
			return Stream.of(new VariantsBlockModelDefinitionCreator.Entry(
					this.properties,
					this.variant.apply(operator)
			));
		}
	}
}

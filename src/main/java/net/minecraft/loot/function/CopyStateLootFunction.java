package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Property;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Функция лута, копирующая свойства состояния блока в компонент предмета. */
public class CopyStateLootFunction extends ConditionalLootFunction {

	public static final MapCodec<CopyStateLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				Registries.BLOCK.getEntryCodec().fieldOf("block").forGetter(function -> function.block),
				Codec.STRING.listOf().fieldOf("properties").forGetter(
					function -> function.properties.stream().map(Property::getName).toList()
				)
			))
			.apply(instance, CopyStateLootFunction::new)
	);

	private final RegistryEntry<Block> block;
	private final Set<Property<?>> properties;

	CopyStateLootFunction(List<LootCondition> conditions, RegistryEntry<Block> block, List<String> propertyNames) {
		super(conditions);
		this.block = block;
		this.properties = propertyNames.stream()
			.map(name -> block.value().getStateManager().getProperty(name))
			.filter(Objects::nonNull)
			.collect(Collectors.toUnmodifiableSet());
	}

	CopyStateLootFunction(List<LootCondition> conditions, RegistryEntry<Block> block, Set<Property<?>> properties) {
		super(conditions);
		this.block = block;
		this.properties = ImmutableSet.copyOf(properties);
	}

	@Override
	public LootFunctionType<CopyStateLootFunction> getType() {
		return LootFunctionTypes.COPY_STATE;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.BLOCK_STATE);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		BlockState blockState = context.get(LootContextParameters.BLOCK_STATE);

		if (blockState == null) {
			return stack;
		}

		BlockStateComponent component = stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);

		for (Property<?> property : properties) {
			if (blockState.contains(property)) {
				component = copyProperty(component, blockState, property);
			}
		}

		stack.set(DataComponentTypes.BLOCK_STATE, component);

		return stack;
	}

	private static <T extends Comparable<T>> BlockStateComponent copyProperty(
		BlockStateComponent component,
		BlockState state,
		Property<T> property
	) {
		return component.with(property, state.get(property));
	}

	public static Builder builder(Block block) {
		return new Builder(Registries.BLOCK.getEntry(block));
	}

	/** Строитель функции копирования свойств блока. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private final RegistryEntry<Block> block;
		private final ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

		Builder(RegistryEntry<Block> block) {
			this.block = block;
		}

		public Builder addProperty(Property<?> property) {
			if (!block.value().getStateManager().getProperties().contains(property)) {
				throw new IllegalArgumentException(
					"Property " + property + " is not present on block " + block.value()
				);
			}

			properties.add(property);

			return this;
		}

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return new CopyStateLootFunction(getConditions(), block, properties.build());
		}
	}
}

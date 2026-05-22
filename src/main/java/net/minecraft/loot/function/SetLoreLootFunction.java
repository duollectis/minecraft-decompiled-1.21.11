package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.collection.ListOperation;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Функция лута, устанавливающая или изменяющая описание (lore) предмета.
 * Поддерживает разрешение текстовых компонентов через источник-сущность.
 */
public class SetLoreLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetLoreLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					TextCodecs.CODEC
						.sizeLimitedListOf(256)
						.fieldOf("lore")
						.forGetter(function -> function.lore),
					ListOperation.createCodec(256).forGetter(function -> function.operation),
					LootContext.EntityReference.CODEC
						.optionalFieldOf("entity")
						.forGetter(function -> function.entity)
				)
			)
			.apply(instance, SetLoreLootFunction::new)
	);

	private final List<Text> lore;
	private final ListOperation operation;
	private final Optional<LootContext.EntityReference> entity;

	public SetLoreLootFunction(
		List<LootCondition> conditions,
		List<Text> lore,
		ListOperation operation,
		Optional<LootContext.EntityReference> entity
	) {
		super(conditions);
		this.lore = List.copyOf(lore);
		this.operation = operation;
		this.entity = entity;
	}

	@Override
	public LootFunctionType<SetLoreLootFunction> getType() {
		return LootFunctionTypes.SET_LORE;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return entity.<Set<ContextParameter<?>>>map(ref -> Set.of(ref.contextParam())).orElseGet(Set::of);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(
			DataComponentTypes.LORE,
			LoreComponent.DEFAULT,
			component -> new LoreComponent(buildNewLore(component, context))
		);
		return stack;
	}

	private List<Text> buildNewLore(@Nullable LoreComponent current, LootContext context) {
		if (current == null && lore.isEmpty()) {
			return List.of();
		}

		UnaryOperator<Text> textResolver = SetNameLootFunction.applySourceEntity(context, entity.orElse(null));
		List<Text> resolvedLore = lore.stream().map(textResolver).toList();
		return operation.apply(current.lines(), resolvedLore, 256);
	}

	public static SetLoreLootFunction.Builder builder() {
		return new SetLoreLootFunction.Builder();
	}

	/** Строитель функции установки описания предмета. */
	public static class Builder extends ConditionalLootFunction.Builder<SetLoreLootFunction.Builder> {

		private Optional<LootContext.EntityReference> target = Optional.empty();
		private final ImmutableList.Builder<Text> lore = ImmutableList.builder();
		private ListOperation operation = ListOperation.Append.INSTANCE;

		public SetLoreLootFunction.Builder operation(ListOperation operation) {
			this.operation = operation;
			return this;
		}

		public SetLoreLootFunction.Builder target(LootContext.EntityReference target) {
			this.target = Optional.of(target);
			return this;
		}

		public SetLoreLootFunction.Builder lore(Text loreText) {
			lore.add(loreText);
			return this;
		}

		@Override
		protected SetLoreLootFunction.Builder getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetLoreLootFunction(getConditions(), lore.build(), operation, target);
		}
	}
}

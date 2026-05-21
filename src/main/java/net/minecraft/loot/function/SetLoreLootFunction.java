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
 * {@code SetLoreLootFunction}.
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
		return this.entity.<Set<ContextParameter<?>>>map(entity -> Set.of(entity.contextParam())).orElseGet(Set::of);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(
				DataComponentTypes.LORE,
				LoreComponent.DEFAULT,
				component -> new LoreComponent(this.getNewLoreTexts(component, context))
		);
		return stack;
	}

	private List<Text> getNewLoreTexts(@Nullable LoreComponent current, LootContext context) {
		if (current == null && this.lore.isEmpty()) {
			return List.of();
		}
		else {
			UnaryOperator<Text>
					unaryOperator =
					SetNameLootFunction.applySourceEntity(context, this.entity.orElse(null));
			List<Text> list = this.lore.stream().map(unaryOperator).toList();
			return this.operation.apply(current.lines(), list, 256);
		}
	}

	public static SetLoreLootFunction.Builder builder() {
		return new SetLoreLootFunction.Builder();
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder extends ConditionalLootFunction.Builder<SetLoreLootFunction.Builder> {

		private Optional<LootContext.EntityReference> target = Optional.empty();
		private final com.google.common.collect.ImmutableList.Builder<Text> lore = ImmutableList.builder();
		private ListOperation operation = ListOperation.Append.INSTANCE;

		public SetLoreLootFunction.Builder operation(ListOperation operation) {
			this.operation = operation;
			return this;
		}

		public SetLoreLootFunction.Builder target(LootContext.EntityReference target) {
			this.target = Optional.of(target);
			return this;
		}

		public SetLoreLootFunction.Builder lore(Text lore) {
			this.lore.add(lore);
			return this;
		}

		protected SetLoreLootFunction.Builder getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetLoreLootFunction(this.getConditions(), this.lore.build(), this.operation, this.target);
		}
	}
}

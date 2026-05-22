package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Функция лута, добавляющая модификаторы атрибутов к предмету. */
public class SetAttributesLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetAttributesLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				Attribute.CODEC
					.listOf()
					.fieldOf("modifiers")
					.forGetter(function -> function.attributes),
				Codec.BOOL
					.optionalFieldOf("replace", true)
					.forGetter(function -> function.replace)
			))
			.apply(instance, SetAttributesLootFunction::new)
	);

	private final List<Attribute> attributes;
	private final boolean replace;

	SetAttributesLootFunction(
		List<LootCondition> conditions,
		List<Attribute> attributes,
		boolean replace
	) {
		super(conditions);
		this.attributes = List.copyOf(attributes);
		this.replace = replace;
	}

	@Override
	public LootFunctionType<SetAttributesLootFunction> getType() {
		return LootFunctionTypes.SET_ATTRIBUTES;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return attributes
			.stream()
			.flatMap(attribute -> attribute.amount.getAllowedParameters().stream())
			.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (replace) {
			stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, applyTo(context, AttributeModifiersComponent.DEFAULT));
		} else {
			stack.apply(
				DataComponentTypes.ATTRIBUTE_MODIFIERS,
				AttributeModifiersComponent.DEFAULT,
				component -> applyTo(context, component)
			);
		}

		return stack;
	}

	private AttributeModifiersComponent applyTo(LootContext context, AttributeModifiersComponent component) {
		Random random = context.getRandom();

		for (Attribute attribute : attributes) {
			AttributeModifierSlot slot = Util.getRandom(attribute.slots, random);
			component = component.with(
				attribute.attribute,
				new EntityAttributeModifier(attribute.id, attribute.amount.nextFloat(context), attribute.operation),
				slot
			);
		}

		return component;
	}

	public static AttributeBuilder attributeBuilder(
		Identifier id,
		RegistryEntry<EntityAttribute> attribute,
		EntityAttributeModifier.Operation operation,
		LootNumberProvider amountRange
	) {
		return new AttributeBuilder(id, attribute, operation, amountRange);
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Описание одного модификатора атрибута для функции лута. */
	record Attribute(
		Identifier id,
		RegistryEntry<EntityAttribute> attribute,
		EntityAttributeModifier.Operation operation,
		LootNumberProvider amount,
		List<AttributeModifierSlot> slots
	) {

		private static final Codec<List<AttributeModifierSlot>> EQUIPMENT_SLOT_LIST_CODEC =
			Codecs.nonEmptyList(Codecs.listOrSingle(AttributeModifierSlot.CODEC));

		public static final Codec<Attribute> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Identifier.CODEC.fieldOf("id").forGetter(Attribute::id),
				EntityAttribute.CODEC.fieldOf("attribute").forGetter(Attribute::attribute),
				EntityAttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(Attribute::operation),
				LootNumberProviderTypes.CODEC.fieldOf("amount").forGetter(Attribute::amount),
				EQUIPMENT_SLOT_LIST_CODEC.fieldOf("slot").forGetter(Attribute::slots)
			).apply(instance, Attribute::new)
		);
	}

	/** Строитель одного модификатора атрибута. */
	public static class AttributeBuilder {

		private final Identifier id;
		private final RegistryEntry<EntityAttribute> attribute;
		private final EntityAttributeModifier.Operation operation;
		private final LootNumberProvider amount;
		private final Set<AttributeModifierSlot> slots = EnumSet.noneOf(AttributeModifierSlot.class);

		public AttributeBuilder(
			Identifier id,
			RegistryEntry<EntityAttribute> attribute,
			EntityAttributeModifier.Operation operation,
			LootNumberProvider amount
		) {
			this.id = id;
			this.attribute = attribute;
			this.operation = operation;
			this.amount = amount;
		}

		public AttributeBuilder slot(AttributeModifierSlot slot) {
			slots.add(slot);
			return this;
		}

		public Attribute build() {
			return new Attribute(id, attribute, operation, amount, List.copyOf(slots));
		}
	}

	/** Строитель функции установки атрибутов. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private final boolean replace;
		private final List<Attribute> attributes = Lists.newArrayList();

		public Builder(boolean replace) {
			this.replace = replace;
		}

		public Builder() {
			this(false);
		}

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		public Builder attribute(AttributeBuilder attribute) {
			attributes.add(attribute.build());
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetAttributesLootFunction(getConditions(), attributes, replace);
		}
	}
}

package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Мигрирует компонент {@code minecraft:food}: выносит поля {@code eat_seconds}
 * и {@code effects} в новый компонент {@code minecraft:consumable}, а поле
 * {@code using_converts_to} переносит в {@code minecraft:use_remainder}.
 * Исходный компонент {@code minecraft:food} очищается от перенесённых полей.
 */
public class FoodToConsumableFix extends DataFix {

	public FoodToConsumableFix(Schema schema) {
		super(schema, true);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return writeFixAndRead(
			"Food to consumable fix",
			getInputSchema().getType(TypeReferences.DATA_COMPONENTS),
			getOutputSchema().getType(TypeReferences.DATA_COMPONENTS),
			this::fixFoodComponent
		);
	}

	private Dynamic<?> fixFoodComponent(Dynamic<?> components) {
		Optional<? extends Dynamic<?>> foodComponent = components.get("minecraft:food").result();

		if (foodComponent.isEmpty()) {
			return components;
		}

		Dynamic<?> food = foodComponent.get();
		float eatSeconds = food.get("eat_seconds").asFloat(1.6F);

		Stream<? extends Dynamic<?>> consumeEffects = food.get("effects").asStream().map(effect ->
			effect.emptyMap()
				.set("type", effect.createString("minecraft:apply_effects"))
				.set("effects", effect.createList(effect.get("effect").result().stream()))
				.set("probability", effect.createFloat(effect.get("probability").asFloat(1.0F)))
		);

		Dynamic<?> result = Dynamic.copyField(food, "using_converts_to", components, "minecraft:use_remainder");

		result = result.set(
			"minecraft:food",
			food.remove("eat_seconds").remove("effects").remove("using_converts_to")
		);

		return result.set(
			"minecraft:consumable",
			result.emptyMap()
				.set("consume_seconds", result.createFloat(eatSeconds))
				.set("on_consume_effects", result.createList(consumeEffects))
		);
	}
}

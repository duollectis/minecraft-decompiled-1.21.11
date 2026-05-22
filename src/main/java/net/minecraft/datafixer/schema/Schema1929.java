package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1929 (Minecraft 1.14 — Village & Pillage).
 * <p>
 * Регистрирует типы данных для сущностей, добавленных в обновлении 1.14:
 * странствующего торговца ({@code wandering_trader}) с инвентарём и торговыми
 * предложениями, а также торговой ламы ({@code trader_llama}) с предметами снаряжения.
 */
public class Schema1929 extends IdentifierNormalizingSchema {

	public Schema1929(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);

		schema.register(
			entityTypes,
			"minecraft:wandering_trader",
			name -> DSL.optionalFields(
				"Inventory",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
				"Offers",
				DSL.optionalFields("Recipes", DSL.list(TypeReferences.VILLAGER_TRADE.in(schema)))
			)
		);
		schema.register(
			entityTypes,
			"minecraft:trader_llama",
			name -> DSL.optionalFields(
				"Items",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
				"SaddleItem",
				TypeReferences.ITEM_STACK.in(schema),
				"DecorItem",
				TypeReferences.ITEM_STACK.in(schema)
			)
		);

		return entityTypes;
	}
}

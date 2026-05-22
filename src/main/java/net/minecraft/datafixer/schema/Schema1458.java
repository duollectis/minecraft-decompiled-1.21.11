package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1458: переопределяет тип {@code ENTITY} с поддержкой
 * {@code CustomName} как текстового компонента, добавляет {@code CustomName}
 * к ряду блок-сущностей (сундуки, печи, зелья и т.д.).
 */
public class Schema1458 extends IdentifierNormalizingSchema {

	public Schema1458(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				true,
				TypeReferences.ENTITY,
				() -> DSL.and(
						TypeReferences.ENTITY_EQUIPMENT.in(schema),
						DSL.optionalFields(
								"CustomName",
								TypeReferences.TEXT_COMPONENT.in(schema),
								DSL.taggedChoiceLazy("id", getIdentifierType(), entityTypes)
						)
				)
		);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.register(map, "minecraft:beacon", () -> customName(schema));
		schema.register(map, "minecraft:banner", () -> customName(schema));
		schema.register(map, "minecraft:brewing_stand", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:chest", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:trapped_chest", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:dispenser", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:dropper", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:enchanting_table", () -> customName(schema));
		schema.register(map, "minecraft:furnace", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:hopper", () -> itemsAndCustomName(schema));
		schema.register(map, "minecraft:shulker_box", () -> itemsAndCustomName(schema));
		return map;
	}

	public static TypeTemplate itemsAndCustomName(Schema schema) {
		return DSL.optionalFields(
				"Items",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
				"CustomName",
				TypeReferences.TEXT_COMPONENT.in(schema)
		);
	}

	public static TypeTemplate customName(Schema schema) {
		return DSL.optionalFields("CustomName", TypeReferences.TEXT_COMPONENT.in(schema));
	}
}

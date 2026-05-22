package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2100 (Minecraft 1.15 — Buzzy Bees).
 * <p>
 * Регистрирует типы данных для сущностей и блок-сущностей, добавленных в обновлении 1.15:
 * пчелы ({@code minecraft:bee}), жала пчелы ({@code minecraft:bee_stinger}),
 * а также улья ({@code minecraft:beehive}) с вложенными данными сущностей пчёл.
 */
public class Schema2100 extends IdentifierNormalizingSchema {

	public Schema2100(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static void registerEntity(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, String name) {
		schema.registerSimple(entityTypes, name);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		registerEntity(schema, entityTypes, "minecraft:bee");
		registerEntity(schema, entityTypes, "minecraft:bee_stinger");
		return entityTypes;
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:beehive",
			() -> DSL.optionalFields(
				"Bees",
				DSL.list(DSL.optionalFields("EntityData", TypeReferences.ENTITY_TREE.in(schema)))
			)
		);
		return blockEntityTypes;
	}
}

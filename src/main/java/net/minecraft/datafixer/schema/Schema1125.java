package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1125: добавляет блок-сущность {@code minecraft:bed} и регистрирует
 * типы {@code ADVANCEMENTS}, {@code BIOME} и {@code ENTITY_NAME}.
 * Тип {@code ADVANCEMENTS} описывает критерии достижений, привязанные к биомам
 * и именам сущностей.
 */
public class Schema1125 extends IdentifierNormalizingSchema {

	public Schema1125(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.registerSimple(map, "minecraft:bed");
		return map;
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				false,
				TypeReferences.ADVANCEMENTS,
				() -> DSL.optionalFields(
						"minecraft:adventure/adventuring_time",
						DSL.optionalFields(
								"criteria",
								DSL.compoundList(TypeReferences.BIOME.in(schema), DSL.constType(DSL.string()))
						),
						"minecraft:adventure/kill_a_mob",
						DSL.optionalFields(
								"criteria",
								DSL.compoundList(TypeReferences.ENTITY_NAME.in(schema), DSL.constType(DSL.string()))
						),
						"minecraft:adventure/kill_all_mobs",
						DSL.optionalFields(
								"criteria",
								DSL.compoundList(TypeReferences.ENTITY_NAME.in(schema), DSL.constType(DSL.string()))
						),
						"minecraft:husbandry/bred_all_animals",
						DSL.optionalFields(
								"criteria",
								DSL.compoundList(TypeReferences.ENTITY_NAME.in(schema), DSL.constType(DSL.string()))
						)
				)
		);
		schema.registerType(false, TypeReferences.BIOME, () -> DSL.constType(getIdentifierType()));
		schema.registerType(false, TypeReferences.ENTITY_NAME, () -> DSL.constType(getIdentifierType()));
	}
}

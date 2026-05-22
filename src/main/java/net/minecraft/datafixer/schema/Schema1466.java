package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1466: расширяет тип {@code CHUNK} полем {@code Structures}
 * с вложенными {@code Starts} — данными о начатых структурах в чанке.
 * Также добавляет заглушку {@code "DUMMY"} в блок-сущности для обратной совместимости.
 */
public class Schema1466 extends IdentifierNormalizingSchema {

	public Schema1466(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				false,
				TypeReferences.CHUNK,
				() -> DSL.fields(
						"Level",
						DSL.optionalFields(
								"Entities",
								DSL.list(TypeReferences.ENTITY_TREE.in(schema)),
								"TileEntities",
								DSL.list(DSL.or(TypeReferences.BLOCK_ENTITY.in(schema), DSL.remainder())),
								"TileTicks",
								DSL.list(DSL.fields("i", TypeReferences.BLOCK_NAME.in(schema))),
								"Sections",
								DSL.list(DSL.optionalFields(
										"Palette",
										DSL.list(TypeReferences.BLOCK_STATE.in(schema))
								)),
								"Structures",
								DSL.optionalFields(
										"Starts",
										DSL.compoundList(TypeReferences.STRUCTURE_FEATURE.in(schema))
								)
						)
				)
		);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		map.put("DUMMY", DSL::remainder);
		return map;
	}
}

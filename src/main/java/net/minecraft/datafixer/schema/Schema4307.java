package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4307, добавляющая в компоненты данных предметов поддержку
 * {@code minecraft:can_place_on} и {@code minecraft:can_break} — предикатов блоков,
 * которые могут быть одиночным именем блока или списком имён.
 */
public class Schema4307 extends IdentifierNormalizingSchema {

	public Schema4307(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public static SequencedMap<String, Supplier<TypeTemplate>> createDataComponentsMap(Schema schema) {
		SequencedMap<String, Supplier<TypeTemplate>> sequencedMap = Schema4059.createDataComponentsMap(schema);
		sequencedMap.put("minecraft:can_place_on", () -> createBlockPredicateTemplate(schema));
		sequencedMap.put("minecraft:can_break", () -> createBlockPredicateTemplate(schema));
		return sequencedMap;
	}

	private static TypeTemplate createBlockPredicateTemplate(Schema schema) {
		TypeTemplate
				typeTemplate =
				DSL.optionalFields(
						"blocks",
						DSL.or(TypeReferences.BLOCK_NAME.in(schema), DSL.list(TypeReferences.BLOCK_NAME.in(schema)))
				);
		return DSL.or(typeTemplate, DSL.list(typeTemplate));
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> map,
			Map<String, Supplier<TypeTemplate>> map2
	) {
		super.registerTypes(schema, map, map2);
		schema.registerType(true, TypeReferences.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(createDataComponentsMap(schema)));
	}
}

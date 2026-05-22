package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1488: обновляет блок-сущность {@code minecraft:command_block},
 * добавляя поддержку {@code CustomName} как текстового компонента
 * в дополнение к уже существующему {@code LastOutput}.
 */
public class Schema1488 extends IdentifierNormalizingSchema {

	public Schema1488(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.register(
				map,
				"minecraft:command_block",
				() -> DSL.optionalFields(
						"CustomName",
						TypeReferences.TEXT_COMPONENT.in(schema),
						"LastOutput",
						TypeReferences.TEXT_COMPONENT.in(schema)
				)
		);
		return map;
	}
}

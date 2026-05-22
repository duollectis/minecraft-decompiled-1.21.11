package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1909: добавляет блок-сущность {@code minecraft:jigsaw}
 * с полем {@code final_state}, хранящим финальное состояние блока
 * после сборки структуры.
 */
public class Schema1909 extends IdentifierNormalizingSchema {

	public Schema1909(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
				blockEntityTypes,
				"minecraft:jigsaw",
				() -> DSL.optionalFields("final_state", TypeReferences.FLAT_BLOCK_STATE.in(schema))
		);
		return blockEntityTypes;
	}
}

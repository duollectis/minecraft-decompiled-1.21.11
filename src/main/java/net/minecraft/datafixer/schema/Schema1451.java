package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1451: добавляет блок-сущность {@code minecraft:trapped_chest}
 * с поддержкой инвентаря, отделяя её от обычного сундука в системе типов.
 */
public class Schema1451 extends IdentifierNormalizingSchema {

	public Schema1451(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.register(
				map,
				"minecraft:trapped_chest",
				() -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return map;
	}
}

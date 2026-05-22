package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4306, разделяющая единый тип снаряда {@code minecraft:potion}
 * на два отдельных: {@code minecraft:splash_potion} и {@code minecraft:lingering_potion},
 * каждый из которых несёт поле {@code Item} типа {@code ITEM_STACK}.
 */
public class Schema4306 extends IdentifierNormalizingSchema {

	public Schema4306(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		map.remove("minecraft:potion");
		schema.register(
				map,
				"minecraft:splash_potion",
				() -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
		);
		schema.register(
				map,
				"minecraft:lingering_potion",
				() -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
		);
		return map;
	}
}

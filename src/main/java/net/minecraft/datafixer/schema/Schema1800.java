package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1800: добавляет сущности обновления Village &amp; Pillage —
 * панду {@code minecraft:panda} и разбойника {@code minecraft:pillager}
 * с инвентарём.
 */
public class Schema1800 extends IdentifierNormalizingSchema {

	public Schema1800(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:panda");
		schema.register(
				map,
				"minecraft:pillager",
				name -> DSL.optionalFields("Inventory", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return map;
	}
}

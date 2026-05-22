package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4070, добавляющая поддержку сущностей лодок из бледного дуба:
 * {@code minecraft:pale_oak_boat} и {@code minecraft:pale_oak_chest_boat} с инвентарём.
 */
public class Schema4070 extends IdentifierNormalizingSchema {

	public Schema4070(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:pale_oak_boat");
		schema.register(
				map,
				"minecraft:pale_oak_chest_boat",
				string -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return map;
	}
}

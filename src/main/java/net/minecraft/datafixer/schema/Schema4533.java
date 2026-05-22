package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4533, регистрирующая блок-сущность {@code minecraft:shelf}
 * (полка) с инвентарём {@code Items}.
 */
public class Schema4533 extends IdentifierNormalizingSchema {

	public Schema4533(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.register(
				map,
				"minecraft:shelf",
				() -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return map;
	}
}

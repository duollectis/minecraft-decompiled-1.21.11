package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema2688}.
 */
public class Schema2688 extends IdentifierNormalizingSchema {

	public Schema2688(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:glow_squid");
		schema.register(
				map,
				"minecraft:glow_item_frame",
				string -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
		);
		return map;
	}
}

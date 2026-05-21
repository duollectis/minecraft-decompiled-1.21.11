package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema3808}.
 */
public class Schema3808 extends IdentifierNormalizingSchema {

	public Schema3808(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(
				map,
				"minecraft:horse",
				string -> DSL.optionalFields("SaddleItem", TypeReferences.ITEM_STACK.in(schema))
		);
		return map;
	}
}

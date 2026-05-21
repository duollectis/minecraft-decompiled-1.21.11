package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema3325}.
 */
public class Schema3325 extends IdentifierNormalizingSchema {

	public Schema3325(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(
				map,
				"minecraft:item_display",
				string -> DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema))
		);
		schema.register(
				map,
				"minecraft:block_display",
				string -> DSL.optionalFields("block_state", TypeReferences.BLOCK_STATE.in(schema))
		);
		schema.register(
				map,
				"minecraft:text_display",
				() -> DSL.optionalFields("text", TypeReferences.TEXT_COMPONENT.in(schema))
		);
		return map;
	}
}

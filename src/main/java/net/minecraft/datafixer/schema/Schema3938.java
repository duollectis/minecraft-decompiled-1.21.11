package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema3938}.
 */
public class Schema3938 extends IdentifierNormalizingSchema {

	public Schema3938(int i, Schema schema) {
		super(i, schema);
	}

	protected static TypeTemplate createArrowTemplate(Schema schema) {
		return DSL.optionalFields(
				"inBlockState",
				TypeReferences.BLOCK_STATE.in(schema),
				"item",
				TypeReferences.ITEM_STACK.in(schema),
				"weapon",
				TypeReferences.ITEM_STACK.in(schema)
		);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(map, "minecraft:spectral_arrow", () -> createArrowTemplate(schema));
		schema.register(map, "minecraft:arrow", () -> createArrowTemplate(schema));
		return map;
	}
}

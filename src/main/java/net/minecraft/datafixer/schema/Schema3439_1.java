package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema3439_1}.
 */
public class Schema3439_1 extends IdentifierNormalizingSchema {

	public Schema3439_1(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		this.register(map, "minecraft:hanging_sign", () -> Schema3439.createSignTemplate(schema));
		return map;
	}
}

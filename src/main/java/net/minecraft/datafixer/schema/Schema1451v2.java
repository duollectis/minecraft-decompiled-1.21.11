package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1451v2: добавляет блок-сущность {@code minecraft:piston}
 * с полем {@code blockState}, хранящим состояние блока, захваченного поршнем.
 */
public class Schema1451v2 extends IdentifierNormalizingSchema {

	public Schema1451v2(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.register(
				map,
				"minecraft:piston",
				name -> DSL.optionalFields("blockState", TypeReferences.BLOCK_STATE.in(schema))
		);
		return map;
	}
}

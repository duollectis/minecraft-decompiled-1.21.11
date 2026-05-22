package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4071, регистрирующая новые сущности Creaking и Creaking Transient
 * (мобы, связанные с бледным дубом), а также блок-сущность Creaking Heart.
 */
public class Schema4071 extends IdentifierNormalizingSchema {

	public Schema4071(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:creaking");
		schema.registerSimple(map, "minecraft:creaking_transient");
		return map;
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		this.registerSimple(map, "minecraft:creaking_heart");
		return map;
	}
}

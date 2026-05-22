package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1486: переименовывает сущности {@code minecraft:cod_mob} → {@code minecraft:cod}
 * и {@code minecraft:salmon_mob} → {@code minecraft:salmon} для соответствия финальным именам.
 */
public class Schema1486 extends IdentifierNormalizingSchema {

	public Schema1486(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		map.put("minecraft:cod", map.remove("minecraft:cod_mob"));
		map.put("minecraft:salmon", map.remove("minecraft:salmon_mob"));
		return map;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1483: переименовывает сущность {@code minecraft:puffer_fish}
 * в {@code minecraft:pufferfish} — исправление несоответствия имени.
 */
public class Schema1483 extends IdentifierNormalizingSchema {

	public Schema1483(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		map.put("minecraft:pufferfish", map.remove("minecraft:puffer_fish"));
		return map;
	}
}

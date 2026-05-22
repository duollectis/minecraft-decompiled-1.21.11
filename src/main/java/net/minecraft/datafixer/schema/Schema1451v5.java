package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1451v5: удаляет устаревшие блок-сущности {@code minecraft:flower_pot}
 * и {@code minecraft:noteblock}, данные которых были перенесены в состояния блоков.
 */
public class Schema1451v5 extends IdentifierNormalizingSchema {

	public Schema1451v5(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		map.remove("minecraft:flower_pot");
		map.remove("minecraft:noteblock");
		return map;
	}
}

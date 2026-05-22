package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 143: удаляет устаревший тип сущности {@code "TippedArrow"},
 * заменённый стандартной стрелой с данными зелья в NBT.
 */
public class Schema143 extends Schema {

	public Schema143(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		map.remove("TippedArrow");
		return map;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4543, регистрирующая новую сущность
 * {@code minecraft:mannequin} — манекен для отображения снаряжения.
 */
public class Schema4543 extends IdentifierNormalizingSchema {

	public Schema4543(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:mannequin");
		return map;
	}
}

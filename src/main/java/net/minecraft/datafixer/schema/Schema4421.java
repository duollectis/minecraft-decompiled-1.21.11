package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4421, регистрирующая новую сущность
 * {@code minecraft:happy_ghast} — дружелюбную версию гаста.
 */
public class Schema4421 extends IdentifierNormalizingSchema {

	public Schema4421(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:happy_ghast");
		return map;
	}
}

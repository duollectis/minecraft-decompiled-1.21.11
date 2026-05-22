package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1928: переименовывает сущность {@code minecraft:illager_beast}
 * в {@code minecraft:ravager} — финальное имя равагера после бета-периода.
 */
public class Schema1928 extends IdentifierNormalizingSchema {

	public Schema1928(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static void targetEntityItems(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
		schema.registerSimple(map, entityId);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		entityTypes.remove("minecraft:illager_beast");
		targetEntityItems(schema, entityTypes, "minecraft:ravager");
		return entityTypes;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2509 (Minecraft 1.16 — Nether Update).
 * <p>
 * Выполняет переименование сущности: удаляет устаревший тип
 * {@code minecraft:zombie_pigman} и регистрирует его преемника —
 * зомбифицированного пиглина ({@code minecraft:zombified_piglin}).
 */
public class Schema2509 extends IdentifierNormalizingSchema {

	public Schema2509(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		entityTypes.remove("minecraft:zombie_pigman");
		schema.registerSimple(entityTypes, "minecraft:zombified_piglin");
		return entityTypes;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2707 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Регистрирует тип данных для сущности-маркера ({@code minecraft:marker}) —
 * невидимой сущности без физики, предназначенной для разработчиков карт
 * и используемой в командных блоках для точечной привязки данных.
 */
public class Schema2707 extends IdentifierNormalizingSchema {

	public Schema2707(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		registerSimple(entityTypes, "minecraft:marker");
		return entityTypes;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3328 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Регистрирует тип данных для сущности взаимодействия ({@code minecraft:interaction}) —
 * невидимой сущности с настраиваемой зоной столкновения, предназначенной для
 * создания пользовательских интерактивных областей на картах.
 */
public class Schema3328 extends IdentifierNormalizingSchema {

	public Schema3328(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:interaction");
		return entityTypes;
	}
}

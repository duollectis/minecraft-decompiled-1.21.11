package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3816 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных для сущности болотного скелета ({@code minecraft:bogged}),
 * добавленного в обновлении 1.21 как разновидность скелета, обитающего в болотах
 * и стреляющего стрелами с замедлением.
 */
public class Schema3816 extends IdentifierNormalizingSchema {

	public Schema3816(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:bogged");
		return entityTypes;
	}
}

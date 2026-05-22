package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3076 (Minecraft 1.19 — The Wild Update).
 * <p>
 * Регистрирует тип данных для блок-сущности катализатора скалка
 * ({@code minecraft:sculk_catalyst}), добавленного в обновлении 1.19.
 * Катализатор распространяет скалк при гибели мобов поблизости.
 */
public class Schema3076 extends IdentifierNormalizingSchema {

	public Schema3076(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.registerSimple(blockEntityTypes, "minecraft:sculk_catalyst");
		return blockEntityTypes;
	}
}

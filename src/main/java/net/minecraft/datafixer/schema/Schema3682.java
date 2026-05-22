package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3682 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных для блок-сущности автокрафтера ({@code minecraft:crafter}),
 * добавленного в обновлении 1.21. Использует стандартный шаблон контейнера
 * с предметами и пользовательским именем из {@link Schema1458#itemsAndCustomName(Schema)}.
 */
public class Schema3682 extends IdentifierNormalizingSchema {

	public Schema3682(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(blockEntityTypes, "minecraft:crafter", () -> Schema1458.itemsAndCustomName(schema));
		return blockEntityTypes;
	}
}

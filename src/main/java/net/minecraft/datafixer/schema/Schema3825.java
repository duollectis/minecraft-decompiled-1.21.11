package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3825 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных для сущности зловещего спаунера предметов
 * ({@code minecraft:ominous_item_spawner}), добавленного в обновлении 1.21.
 * Эта сущность появляется во время зловещего испытания и выбрасывает предметы
 * в игроков, хранит вложенный стек предмета в поле {@code item}.
 */
public class Schema3825 extends IdentifierNormalizingSchema {

	public Schema3825(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:ominous_item_spawner",
			() -> DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema))
		);
		return entityTypes;
	}
}

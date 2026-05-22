package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2501 (Minecraft 1.16 — Nether Update).
 * <p>
 * Регистрирует обновлённые типы данных для блок-сущностей печей:
 * обычной ({@code minecraft:furnace}), коптильни ({@code minecraft:smoker})
 * и доменной печи ({@code minecraft:blast_furnace}). Все три теперь хранят
 * поле {@code RecipesUsed} — список использованных рецептов с количеством применений.
 */
public class Schema2501 extends IdentifierNormalizingSchema {

	public Schema2501(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	private static void registerFurnace(Schema schema, Map<String, Supplier<TypeTemplate>> blockEntityTypes, String name) {
		schema.register(
			blockEntityTypes,
			name,
			() -> DSL.optionalFields(
				"Items",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
				"CustomName",
				TypeReferences.TEXT_COMPONENT.in(schema),
				"RecipesUsed",
				DSL.compoundList(TypeReferences.RECIPE.in(schema), DSL.constType(DSL.intType()))
			)
		);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		registerFurnace(schema, blockEntityTypes, "minecraft:furnace");
		registerFurnace(schema, blockEntityTypes, "minecraft:smoker");
		registerFurnace(schema, blockEntityTypes, "minecraft:blast_furnace");
		return blockEntityTypes;
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3938 (Minecraft 1.21.2 — Bundles of Bravery).
 * <p>
 * Обновляет типы данных для стрел: спектральной ({@code minecraft:spectral_arrow})
 * и обычной ({@code minecraft:arrow}). По сравнению с {@link Schema3685}, добавляется
 * новое поле {@code weapon} — стек предмета оружия, которым была выпущена стрела,
 * что необходимо для корректного расчёта урона с учётом зачарований.
 */
public class Schema3938 extends IdentifierNormalizingSchema {

	public Schema3938(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static TypeTemplate createArrowTemplate(Schema schema) {
		return DSL.optionalFields(
			"inBlockState",
			TypeReferences.BLOCK_STATE.in(schema),
			"item",
			TypeReferences.ITEM_STACK.in(schema),
			"weapon",
			TypeReferences.ITEM_STACK.in(schema)
		);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(entityTypes, "minecraft:spectral_arrow", () -> createArrowTemplate(schema));
		schema.register(entityTypes, "minecraft:arrow", () -> createArrowTemplate(schema));
		return entityTypes;
	}
}

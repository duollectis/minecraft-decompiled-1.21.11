package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3202 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Регистрирует тип данных для блок-сущности висячей таблички
 * ({@code minecraft:hanging_sign}), добавленной в обновлении 1.20.
 * Использует шаблон таблички из {@link Schema99#createHangingSignTypeTemplate(Schema)}.
 */
public class Schema3202 extends IdentifierNormalizingSchema {

	public Schema3202(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		blockEntityTypes.put("minecraft:hanging_sign", () -> Schema99.createHangingSignTypeTemplate(schema));
		return blockEntityTypes;
	}
}

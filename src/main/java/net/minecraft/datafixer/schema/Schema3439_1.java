package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3439 подверсии 1 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Применяет новый двусторонний формат таблички к висячей табличке
 * ({@code minecraft:hanging_sign}), используя тот же шаблон из
 * {@link Schema3439#createSignTemplate(Schema)}, что и для обычной таблички.
 */
public class Schema3439_1 extends IdentifierNormalizingSchema {

	public Schema3439_1(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		register(blockEntityTypes, "minecraft:hanging_sign", () -> Schema3439.createSignTemplate(schema));
		return blockEntityTypes;
	}
}

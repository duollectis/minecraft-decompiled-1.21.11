package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3438 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Выполняет переименование блок-сущности: {@code minecraft:suspicious_sand}
 * переименовывается в {@code minecraft:brushable_block} для унификации
 * с подозрительной гравием. Также регистрирует новую блок-сущность
 * откалиброванного датчика скалка ({@code minecraft:calibrated_sculk_sensor}).
 */
public class Schema3438 extends IdentifierNormalizingSchema {

	public Schema3438(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		blockEntityTypes.put("minecraft:brushable_block", blockEntityTypes.remove("minecraft:suspicious_sand"));
		schema.registerSimple(blockEntityTypes, "minecraft:calibrated_sculk_sensor");
		return blockEntityTypes;
	}
}

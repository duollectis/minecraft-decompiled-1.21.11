package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3683 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных сущности TNT ({@code minecraft:tnt}): теперь она хранит
 * поле {@code block_state} для поддержки кастомных состояний блока взрывчатки,
 * что позволяет DataFixer корректно мигрировать данные при обновлении.
 */
public class Schema3683 extends IdentifierNormalizingSchema {

	public Schema3683(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:tnt",
			() -> DSL.optionalFields("block_state", TypeReferences.BLOCK_STATE.in(schema))
		);
		return entityTypes;
	}
}

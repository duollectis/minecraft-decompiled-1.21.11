package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3818 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных улья ({@code minecraft:beehive}): поле {@code Bees}
 * переименовывается в {@code bees}, а {@code EntityData} — в {@code entity_data},
 * приводя структуру к единому стилю именования в нижнем регистре.
 */
public class Schema3818 extends IdentifierNormalizingSchema {

	public Schema3818(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:beehive",
			() -> DSL.optionalFields(
				"bees",
				DSL.list(DSL.optionalFields("entity_data", TypeReferences.ENTITY_TREE.in(schema)))
			)
		);
		return blockEntityTypes;
	}
}

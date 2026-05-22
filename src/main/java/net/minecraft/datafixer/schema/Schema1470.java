package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1470: добавляет водных мобов Update Aquatic — черепаху, треску,
 * тропическую рыбу, лосося, рыбу-шар, фантома, дельфина, утопленника,
 * а также трезубец как сущность-снаряд.
 */
public class Schema1470 extends IdentifierNormalizingSchema {

	public Schema1470(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static void targetEntityItems(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
		schema.registerSimple(map, entityId);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		targetEntityItems(schema, map, "minecraft:turtle");
		targetEntityItems(schema, map, "minecraft:cod_mob");
		targetEntityItems(schema, map, "minecraft:tropical_fish");
		targetEntityItems(schema, map, "minecraft:salmon_mob");
		targetEntityItems(schema, map, "minecraft:puffer_fish");
		targetEntityItems(schema, map, "minecraft:phantom");
		targetEntityItems(schema, map, "minecraft:dolphin");
		targetEntityItems(schema, map, "minecraft:drowned");
		schema.register(
				map,
				"minecraft:trident",
				name -> DSL.optionalFields(
						"inBlockState",
						TypeReferences.BLOCK_STATE.in(schema),
						"Trident",
						TypeReferences.ITEM_STACK.in(schema)
				)
		);
		return map;
	}
}

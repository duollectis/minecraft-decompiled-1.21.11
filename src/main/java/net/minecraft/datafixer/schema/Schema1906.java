package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1906: добавляет блок-сущности {@code minecraft:barrel},
 * {@code minecraft:smoker}, {@code minecraft:blast_furnace} (с инвентарём
 * и CustomName), {@code minecraft:lectern} (с полем Book) и {@code minecraft:bell}.
 */
public class Schema1906 extends IdentifierNormalizingSchema {

	public Schema1906(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		registerInventoryWithCustomName(schema, blockEntityTypes, "minecraft:barrel");
		registerInventoryWithCustomName(schema, blockEntityTypes, "minecraft:smoker");
		registerInventoryWithCustomName(schema, blockEntityTypes, "minecraft:blast_furnace");
		schema.register(
				blockEntityTypes,
				"minecraft:lectern",
				name -> DSL.optionalFields("Book", TypeReferences.ITEM_STACK.in(schema))
		);
		schema.registerSimple(blockEntityTypes, "minecraft:bell");
		return blockEntityTypes;
	}

	protected static void registerInventoryWithCustomName(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
		schema.register(map, name, () -> Schema1458.itemsAndCustomName(schema));
	}
}

package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1920: добавляет блок-сущность {@code minecraft:campfire}
 * с полем {@code Items} — слотами для предметов, готовящихся на костре.
 */
public class Schema1920 extends IdentifierNormalizingSchema {

	public Schema1920(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static void registerContainerBlockEntity(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
		schema.register(map, name, () -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		registerContainerBlockEntity(schema, blockEntityTypes, "minecraft:campfire");
		return blockEntityTypes;
	}
}

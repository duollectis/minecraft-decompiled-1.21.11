package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema4312}.
 */
public class Schema4312 extends IdentifierNormalizingSchema {

	public Schema4312(int i, Schema schema) {
		super(i, schema);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> map,
			Map<String, Supplier<TypeTemplate>> map2
	) {
		super.registerTypes(schema, map, map2);
		schema.registerType(
				false,
				TypeReferences.PLAYER,
				() -> DSL.and(
						TypeReferences.ENTITY_EQUIPMENT.in(schema),
						DSL.optionalFields(
								new Pair[]{
										Pair.of(
												"RootVehicle",
												DSL.optionalFields("Entity", TypeReferences.ENTITY_TREE.in(schema))
										),
										Pair.of("ender_pearls", DSL.list(TypeReferences.ENTITY_TREE.in(schema))),
										Pair.of("Inventory", DSL.list(TypeReferences.ITEM_STACK.in(schema))),
										Pair.of("EnderItems", DSL.list(TypeReferences.ITEM_STACK.in(schema))),
										Pair.of("ShoulderEntityLeft", TypeReferences.ENTITY_TREE.in(schema)),
										Pair.of("ShoulderEntityRight", TypeReferences.ENTITY_TREE.in(schema)),
										Pair.of(
												"recipeBook",
												DSL.optionalFields(
														"recipes",
														DSL.list(TypeReferences.RECIPE.in(schema)),
														"toBeDisplayed",
														DSL.list(TypeReferences.RECIPE.in(schema))
												)
										)
								}
						)
				)
		);
	}
}

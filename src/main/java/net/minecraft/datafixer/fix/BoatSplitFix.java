package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;

/**
 * Разделяет устаревший тип сущности {@code minecraft:boat} на отдельные типы
 * по материалу дерева (например, {@code minecraft:spruce_boat}).
 * Аналогично обрабатывает {@code minecraft:chest_boat}.
 */
public class BoatSplitFix extends DataFix {

	public BoatSplitFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	private static boolean isBoat(String id) {
		return id.equals("minecraft:boat");
	}

	private static boolean isChestBoat(String id) {
		return id.equals("minecraft:chest_boat");
	}

	private static boolean isBoatOrChestBoat(String id) {
		return isBoat(id) || isChestBoat(id);
	}

	private static String getNewBoatIdFromOldType(String woodType) {
		return switch (woodType) {
			case "spruce" -> "minecraft:spruce_boat";
			case "birch" -> "minecraft:birch_boat";
			case "jungle" -> "minecraft:jungle_boat";
			case "acacia" -> "minecraft:acacia_boat";
			case "cherry" -> "minecraft:cherry_boat";
			case "dark_oak" -> "minecraft:dark_oak_boat";
			case "mangrove" -> "minecraft:mangrove_boat";
			case "bamboo" -> "minecraft:bamboo_raft";
			default -> "minecraft:oak_boat";
		};
	}

	private static String getNewChestBoatIdFromOldType(String woodType) {
		return switch (woodType) {
			case "spruce" -> "minecraft:spruce_chest_boat";
			case "birch" -> "minecraft:birch_chest_boat";
			case "jungle" -> "minecraft:jungle_chest_boat";
			case "acacia" -> "minecraft:acacia_chest_boat";
			case "cherry" -> "minecraft:cherry_chest_boat";
			case "dark_oak" -> "minecraft:dark_oak_chest_boat";
			case "mangrove" -> "minecraft:mangrove_chest_boat";
			case "bamboo" -> "minecraft:bamboo_chest_raft";
			default -> "minecraft:oak_chest_boat";
		};
	}

	public TypeRewriteRule makeRule() {
		OpticFinder<String> idFinder = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
		Type<?> inputType = getInputSchema().getType(TypeReferences.ENTITY);
		Type<?> outputType = getOutputSchema().getType(TypeReferences.ENTITY);

		return fixTypeEverywhereTyped(
				"BoatSplitFix", inputType, outputType, typed -> {
					Optional<String> entityId = typed.getOptional(idFinder);

					if (entityId.isPresent() && isBoatOrChestBoat(entityId.get())) {
						Dynamic<?> remainder = (Dynamic<?>) typed.getOrCreate(DSL.remainderFinder());
						Optional<String> woodType = remainder.get("Type").asString().result();
						String newId = isChestBoat(entityId.get())
						               ? woodType.map(BoatSplitFix::getNewChestBoatIdFromOldType)
								               .orElse("minecraft:oak_chest_boat")
						               : woodType.map(BoatSplitFix::getNewBoatIdFromOldType)
								               .orElse("minecraft:oak_boat");

						return FixUtil
								.withType(outputType, typed)
								.update(DSL.remainderFinder(), dynamic -> dynamic.remove("Type"))
								.set(idFinder, newId);
					}

					return FixUtil.withType(outputType, typed);
				}
		);
	}
}

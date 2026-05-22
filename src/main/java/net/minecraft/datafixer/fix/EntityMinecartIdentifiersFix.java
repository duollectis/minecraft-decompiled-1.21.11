package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

/**
 * Разделяет старый тип {@code Minecart} на отдельные типы по полю {@code Type}:
 * {@code MinecartRideable}, {@code MinecartChest}, {@code MinecartFurnace}.
 */
public class EntityMinecartIdentifiersFix extends EntityTransformFix {

	public EntityMinecartIdentifiersFix(Schema outputSchema) {
		super("EntityMinecartIdentifiersFix", outputSchema, true);
	}

	@Override
	protected Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped) {
		if (!choice.equals("Minecart")) {
			return Pair.of(choice, entityTyped);
		}

		int minecartType = ((Dynamic) entityTyped.getOrCreate(DSL.remainderFinder())).get("Type").asInt(0);

		String newEntityId = switch (minecartType) {
			case 1 -> "MinecartChest";
			case 2 -> "MinecartFurnace";
			default -> "MinecartRideable";
		};

		Type<?> outputType = (Type<?>) getOutputSchema()
				.findChoiceType(TypeReferences.ENTITY)
				.types()
				.get(newEntityId);

		return Pair.of(newEntityId, Util.apply(entityTyped, outputType, d -> d.remove("Type")));
	}
}

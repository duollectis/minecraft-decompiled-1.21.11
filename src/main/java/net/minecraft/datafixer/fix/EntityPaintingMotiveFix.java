package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Нормализует поле {@code Motive} картины: приводит к нижнему регистру и переименовывает
 * устаревшие идентификаторы мотивов (например, {@code donkeykong} → {@code donkey_kong}).
 */
public class EntityPaintingMotiveFix extends ChoiceFix {

	private static final Map<String, String> RENAMED_MOTIVES = Map.of(
		"donkeykong", "donkey_kong",
		"burningskull", "burning_skull",
		"skullandroses", "skull_and_roses"
	);

	public EntityPaintingMotiveFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType, "EntityPaintingMotiveFix", TypeReferences.ENTITY, "minecraft:painting");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::renameMotive);
	}

	private Dynamic<?> renameMotive(Dynamic<?> painting) {
		Optional<String> motive = painting.get("Motive").asString().result();

		if (motive.isEmpty()) {
			return painting;
		}

		String normalizedMotive = motive.get().toLowerCase(Locale.ROOT);

		return painting.set(
				"Motive",
				painting.createString(
						IdentifierNormalizingSchema.normalize(RENAMED_MOTIVES.getOrDefault(normalizedMotive, normalizedMotive))
				)
		);
	}
}

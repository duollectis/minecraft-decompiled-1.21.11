package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Добавляет тег {@code blending_data} в чанки Overworld для корректного смешивания
 * старого и нового рельефа при обновлении мира. Чанки с ранними статусами генерации
 * или с тегом {@code below_zero_retrogen} получают данные для устаревшего диапазона высот.
 */
public class BlendingDataFix extends DataFix {

	private static final Set<String> SKIP_BLENDING_STATUSES = Set.of(
		"minecraft:empty",
		"minecraft:structure_starts",
		"minecraft:structure_references",
		"minecraft:biomes"
	);

	private static final String OVERWORLD_DIMENSION = "minecraft:overworld";
	private static final int OVERWORLD_HEIGHT = 384;
	private static final int OVERWORLD_MIN_Y = -64;
	private static final int LEGACY_HEIGHT = 256;
	private static final int LEGACY_MIN_Y = 0;

	private final String name;

	public BlendingDataFix(Schema outputSchema) {
		super(outputSchema, false);
		name = "Blending Data Fix v" + outputSchema.getVersionKey();
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getOutputSchema().getType(TypeReferences.CHUNK);

		return fixTypeEverywhereTyped(
			name,
			chunkType,
			typed -> typed.update(DSL.remainderFinder(), chunk -> update(chunk, chunk.get("__context")))
		);
	}

	private static Dynamic<?> update(Dynamic<?> chunk, OptionalDynamic<?> context) {
		chunk = chunk.remove("blending_data");

		boolean isOverworld = OVERWORLD_DIMENSION.equals(
			context.get("dimension").asString().result().orElse("")
		);

		if (!isOverworld) {
			return chunk;
		}

		Optional<? extends Dynamic<?>> statusOpt = chunk.get("Status").result();

		if (statusOpt.isEmpty()) {
			return chunk;
		}

		String status = IdentifierNormalizingSchema.normalize(statusOpt.get().asString("empty"));

		if (!SKIP_BLENDING_STATUSES.contains(status)) {
			return setSections(chunk, OVERWORLD_HEIGHT, OVERWORLD_MIN_Y);
		}

		Optional<? extends Dynamic<?>> belowZeroOpt = chunk.get("below_zero_retrogen").result();

		if (belowZeroOpt.isEmpty()) {
			return chunk;
		}

		Dynamic<?> belowZero = belowZeroOpt.get();
		String targetStatus = IdentifierNormalizingSchema.normalize(belowZero.get("target_status").asString("empty"));

		return SKIP_BLENDING_STATUSES.contains(targetStatus)
			? chunk
			: setSections(chunk, LEGACY_HEIGHT, LEGACY_MIN_Y);
	}

	private static Dynamic<?> setSections(Dynamic<?> dynamic, int height, int minY) {
		return dynamic.set(
			"blending_data",
			dynamic.createMap(
				Map.of(
					dynamic.createString("min_section"), dynamic.createInt(ChunkSectionPos.getSectionCoord(minY)),
					dynamic.createString("max_section"), dynamic.createInt(ChunkSectionPos.getSectionCoord(minY + height))
				)
			)
		);
	}
}

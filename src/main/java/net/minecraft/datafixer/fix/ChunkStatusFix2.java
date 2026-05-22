package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.Objects;

/**
 * Переименовывает устаревшие статусы генерации чанка в новые имена согласно {@link #STATUS_MAP}.
 */
public class ChunkStatusFix2 extends DataFix {

	private static final Map<String, String> STATUS_MAP = ImmutableMap.<String, String>builder()
			.put("structure_references", "empty")
			.put("biomes", "empty")
			.put("base", "surface")
			.put("carved", "carvers")
			.put("liquid_carved", "liquid_carvers")
			.put("decorated", "features")
			.put("lighted", "light")
			.put("mobs_spawned", "spawn")
			.put("finalized", "heightmaps")
			.put("fullchunk", "full")
			.build();

	public ChunkStatusFix2(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkType.findFieldType("Level");
		OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);

		return fixTypeEverywhereTyped(
				"ChunkStatusFix2",
				chunkType,
				getOutputSchema().getType(TypeReferences.CHUNK),
				chunkTyped -> chunkTyped.updateTyped(
						levelFinder,
						levelTyped -> {
							Dynamic<?> levelDynamic = (Dynamic<?>) levelTyped.get(DSL.remainderFinder());
							String oldStatus = levelDynamic.get("Status").asString("empty");
							String newStatus = STATUS_MAP.getOrDefault(oldStatus, "empty");

							return Objects.equals(oldStatus, newStatus)
									? levelTyped
									: levelTyped.set(
											DSL.remainderFinder(),
											levelDynamic.set("Status", levelDynamic.createString(newStatus))
									);
						}
				)
		);
	}
}

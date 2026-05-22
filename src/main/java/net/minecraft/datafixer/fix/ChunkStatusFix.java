package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Objects;

/**
 * Переименовывает устаревший статус генерации чанка {@code postprocessed} в {@code fullchunk}.
 */
public class ChunkStatusFix extends DataFix {

	public ChunkStatusFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkType.findFieldType("Level");
		OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);

		return fixTypeEverywhereTyped(
				"ChunkStatusFix",
				chunkType,
				getOutputSchema().getType(TypeReferences.CHUNK),
				chunkTyped -> chunkTyped.updateTyped(
						levelFinder,
						levelTyped -> {
							Dynamic<?> levelDynamic = (Dynamic<?>) levelTyped.get(DSL.remainderFinder());
							String status = levelDynamic.get("Status").asString("empty");

							if (Objects.equals(status, "postprocessed")) {
								levelDynamic = levelDynamic.set("Status", levelDynamic.createString("fullchunk"));
							}

							return levelTyped.set(DSL.remainderFinder(), levelDynamic);
						}
				)
		);
	}
}

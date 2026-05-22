package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Переименовывает устаревшие ключи карт высот чанка:
 * {@code LIQUID} → {@code WORLD_SURFACE_WG},
 * {@code SOLID} → {@code OCEAN_FLOOR_WG} + {@code OCEAN_FLOOR},
 * {@code LIGHT} → {@code LIGHT_BLOCKING},
 * {@code RAIN} → {@code MOTION_BLOCKING} + {@code MOTION_BLOCKING_NO_LEAVES}.
 */
public class HeightmapRenamingFix extends DataFix {

	public HeightmapRenamingFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = chunkType.findField("Level");

		return fixTypeEverywhereTyped(
			"HeightmapRenamingFix",
			chunkType,
			chunk -> chunk.updateTyped(
				levelFinder,
				level -> level.update(DSL.remainderFinder(), this::renameHeightmapTags)
			)
		);
	}

	private Dynamic<?> renameHeightmapTags(Dynamic<?> level) {
		Optional<? extends Dynamic<?>> heightmapsOpt = level.get("Heightmaps").result();

		if (heightmapsOpt.isEmpty()) {
			return level;
		}

		Dynamic<?> heightmaps = heightmapsOpt.get();

		Optional<? extends Dynamic<?>> liquid = heightmaps.get("LIQUID").result();
		if (liquid.isPresent()) {
			heightmaps = heightmaps.remove("LIQUID").set("WORLD_SURFACE_WG", liquid.get());
		}

		Optional<? extends Dynamic<?>> solid = heightmaps.get("SOLID").result();
		if (solid.isPresent()) {
			heightmaps = heightmaps.remove("SOLID")
				.set("OCEAN_FLOOR_WG", solid.get())
				.set("OCEAN_FLOOR", solid.get());
		}

		Optional<? extends Dynamic<?>> light = heightmaps.get("LIGHT").result();
		if (light.isPresent()) {
			heightmaps = heightmaps.remove("LIGHT").set("LIGHT_BLOCKING", light.get());
		}

		Optional<? extends Dynamic<?>> rain = heightmaps.get("RAIN").result();
		if (rain.isPresent()) {
			heightmaps = heightmaps.remove("RAIN")
				.set("MOTION_BLOCKING", rain.get())
				.set("MOTION_BLOCKING_NO_LEAVES", rain.get());
		}

		return level.set("Heightmaps", heightmaps);
	}
}

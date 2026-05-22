package net.minecraft.datafixer.fix;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.HashMap;

/**
 * Инжектирует блок-сущности кроватей в чанки, которые содержат блок кровати (id=26) в старом формате.
 * До флаттенинга кровати не имели блок-сущности — этот фикс создаёт их с цветом по умолчанию (красный).
 */
public class BedBlockEntityFix extends DataFix {

	/**
	 * Числовой идентификатор блока кровати в старом формате (до флаттенинга).
	 * Значение 416 = 0x1A0 соответствует блоку minecraft:bed (id=26) со сдвигом на 4 бита.
	 */
	private static final int LEGACY_BED_BLOCK_ID_SHIFTED = 416;

	/** Цвет кровати по умолчанию при инжекции (красный = 14). */
	private static final short DEFAULT_BED_COLOR = 14;

	public BedBlockEntityFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		Type<?> chunkType = getOutputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkType.findFieldType("Level");

		if (!(levelType.findFieldType("TileEntities") instanceof ListType<?> listType)) {
			throw new IllegalStateException("Tile entity type is not a list type.");
		}

		return fix(levelType, listType);
	}

	@SuppressWarnings("unchecked")
	private <TE> TypeRewriteRule fix(Type<?> levelType, ListType<TE> blockEntitiesType) {
		Type<TE> elementType = blockEntitiesType.getElement();
		OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);
		OpticFinder<List<TE>> tileEntitiesFinder = DSL.fieldFinder("TileEntities", blockEntitiesType);

		return TypeRewriteRule.seq(
			fixTypeEverywhere(
				"InjectBedBlockEntityType",
				(TaggedChoiceType<String>) getInputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY),
				(TaggedChoiceType<String>) getOutputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY),
				dynamicOps -> pair -> pair
			),
			fixTypeEverywhereTyped(
				"BedBlockEntityInjecter",
				getOutputSchema().getType(TypeReferences.CHUNK),
				typed -> {
					Typed<?> levelTyped = typed.getTyped(levelFinder);
					Dynamic<?> levelDynamic = (Dynamic<?>) levelTyped.get(DSL.remainderFinder());
					int chunkX = levelDynamic.get("xPos").asInt(0);
					int chunkZ = levelDynamic.get("zPos").asInt(0);
					List<TE> tileEntities = Lists.newArrayList((Iterable<TE>) levelTyped.getOrCreate(tileEntitiesFinder));

					for (Dynamic<?> section : levelDynamic.get("Sections").asList(Function.identity())) {
						int sectionY = section.get("Y").asInt(0);

						Streams.mapWithIndex(
							section.get("Blocks").asIntStream(),
							(blockData, index) -> {
								if (LEGACY_BED_BLOCK_ID_SHIFTED != (blockData & 0xFF) << 4) {
									return null;
								}

								int flatIndex = (int) index;
								int localX = flatIndex & 15;
								int localY = flatIndex >> 8 & 15;
								int localZ = flatIndex >> 4 & 15;

								Map<Dynamic<?>, Dynamic<?>> beData = new HashMap<>();
								beData.put(section.createString("id"), section.createString("minecraft:bed"));
								beData.put(section.createString("x"), section.createInt(localX + (chunkX << 4)));
								beData.put(section.createString("y"), section.createInt(localY + (sectionY << 4)));
								beData.put(section.createString("z"), section.createInt(localZ + (chunkZ << 4)));
								beData.put(section.createString("color"), section.createShort(DEFAULT_BED_COLOR));

								return beData;
							}
						).forEachOrdered(beData -> {
							if (beData == null) {
								return;
							}

							tileEntities.add(
								(TE) ((Pair<?, ?>) elementType.read(section.createMap(beData))
									.result()
									.orElseThrow(() -> new IllegalStateException("Could not parse newly created bed block entity."))
								).getFirst()
							);
						});
					}

					return tileEntities.isEmpty()
						? typed
						: typed.set(levelFinder, levelTyped.set(tileEntitiesFinder, tileEntities));
				}
			)
		);
	}
}

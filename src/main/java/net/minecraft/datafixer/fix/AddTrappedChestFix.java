package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.datafixer.TypeReferences;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Исправление DataFixer, которое разделяет обычные сундуки ({@code minecraft:chest})
 * и ловушечные сундуки ({@code minecraft:trapped_chest}).
 * <p>
 * До версии 1.8 оба типа сундуков хранились под одним идентификатором блока.
 * Это исправление сканирует секции чанка, находит блоки с ID ловушечного сундука
 * и обновляет соответствующие блочные сущности, меняя их тип на {@code minecraft:trapped_chest}.
 */
public class AddTrappedChestFix extends DataFix {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int CHUNK_SECTION_SIZE = 4096;
	/** Количество бит для кодирования локальной Y-координаты секции в упакованном индексе блока. */
	private static final int SECTION_Y_BIT_SHIFT = 12;

	public AddTrappedChestFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		Type<?> chunkOutputType = getOutputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkOutputType.findFieldType("Level");

		if (!(levelType.findFieldType("TileEntities") instanceof ListType<?> tileEntityListType)) {
			throw new IllegalStateException("Tile entity type is not a list type.");
		}

		OpticFinder<? extends List<?>> tileEntitiesFinder = DSL.fieldFinder("TileEntities", tileEntityListType);
		Type<?> chunkInputType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = chunkInputType.findField("Level");
		OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
		Type<?> sectionsType = sectionsFinder.type();

		if (!(sectionsType instanceof ListType)) {
			throw new IllegalStateException("Expecting sections to be a list.");
		}

		Type<?> sectionElementType = ((ListType) sectionsType).getElement();
		OpticFinder<?> sectionFinder = DSL.typeFinder(sectionElementType);

		return TypeRewriteRule.seq(
				new ChoiceTypesFix(getOutputSchema(), "AddTrappedChestFix", TypeReferences.BLOCK_ENTITY).makeRule(),
				fixTypeEverywhereTyped(
						"Trapped Chest fix", chunkInputType, typed -> typed.updateTyped(
								levelFinder, levelTyped -> {
									Optional<? extends Typed<?>> sectionsOpt = levelTyped.getOptionalTyped(sectionsFinder);

									if (sectionsOpt.isEmpty()) {
										return levelTyped;
									}

									List<? extends Typed<?>> sections = sectionsOpt.get().getAllTyped(sectionFinder);
									IntSet trappedChestPositions = new IntOpenHashSet();

									for (Typed<?> sectionTyped : sections) {
										AddTrappedChestFix.ListFixer fixer = new AddTrappedChestFix.ListFixer(
												sectionTyped, getInputSchema()
										);

										if (!fixer.isFixed()) {
											for (int blockIndex = 0; blockIndex < CHUNK_SECTION_SIZE; blockIndex++) {
												int blockState = fixer.blockStateAt(blockIndex);

												if (fixer.isTarget(blockState)) {
													trappedChestPositions.add(fixer.getY() << SECTION_Y_BIT_SHIFT | blockIndex);
												}
											}
										}
									}

									Dynamic<?> levelDynamic = (Dynamic<?>) levelTyped.get(DSL.remainderFinder());
									int chunkX = levelDynamic.get("xPos").asInt(0);
									int chunkZ = levelDynamic.get("zPos").asInt(0);

									@SuppressWarnings("unchecked")
									TaggedChoiceType<String> blockEntityChoiceType = (TaggedChoiceType<String>) getInputSchema()
											.findChoiceType(TypeReferences.BLOCK_ENTITY);

									return levelTyped.updateTyped(
											tileEntitiesFinder, tileEntitiesTyped -> tileEntitiesTyped.updateTyped(
													blockEntityChoiceType.finder(), blockEntityTyped -> {
														Dynamic<?> entityDynamic = (Dynamic<?>) blockEntityTyped.getOrCreate(
																DSL.remainderFinder()
														);
														int localX = entityDynamic.get("x").asInt(0) - (chunkX << 4);
														int localY = entityDynamic.get("y").asInt(0);
														int localZ = entityDynamic.get("z").asInt(0) - (chunkZ << 4);

														if (!trappedChestPositions.contains(LeavesFix.packLocalPos(localX, localY, localZ))) {
															return blockEntityTyped;
														}

														return blockEntityTyped.update(
																blockEntityChoiceType.finder(),
																pair -> pair.mapFirst(entityId -> {
																	if (!Objects.equals(entityId, "minecraft:chest")) {
																		LOGGER.warn("Block Entity was expected to be a chest");
																	}

																	return "minecraft:trapped_chest";
																})
														);
													}
											)
									);
								}
						)
				)
		);
	}

	/**
	 * Вспомогательный класс для сканирования секции чанка на наличие ловушечных сундуков.
	 * Собирает индексы блоков с именем {@code minecraft:trapped_chest} в палитре.
	 */
	public static final class ListFixer extends LeavesFix.ListFixer {

		private @Nullable IntSet targets;

		public ListFixer(Typed<?> typed, Schema schema) {
			super(typed, schema);
		}

		@Override
		protected boolean computeIsFixed() {
			targets = new IntOpenHashSet();

			for (int paletteIndex = 0; paletteIndex < properties.size(); paletteIndex++) {
				Dynamic<?> blockState = properties.get(paletteIndex);
				String blockName = blockState.get("Name").asString("");

				if (Objects.equals(blockName, "minecraft:trapped_chest")) {
					targets.add(paletteIndex);
				}
			}

			return targets.isEmpty();
		}

		public boolean isTarget(int index) {
			return targets.contains(index);
		}
	}
}

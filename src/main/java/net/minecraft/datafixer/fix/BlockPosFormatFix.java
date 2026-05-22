package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Мигрирует формат координат блоков из устаревшего составного NBT-тега
 * в новый компактный формат для различных типов сущностей и блок-сущностей.
 * Также обновляет поля карт (фреймы, баннеры) и компас (цель лодестона).
 */
public class BlockPosFormatFix extends DataFix {

	private static final List<String> PATROL_TARGET_ENTITY_IDS = List.of(
			"minecraft:witch",
			"minecraft:ravager",
			"minecraft:pillager",
			"minecraft:illusioner",
			"minecraft:evoker",
			"minecraft:vindicator"
	);

	public BlockPosFormatFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	private Typed<?> fixOldBlockPosFormat(Typed<?> typed, Map<String, String> oldToNewKey) {
		return typed.update(
				DSL.remainderFinder(), dynamic -> {
					for (Entry<String, String> entry : oldToNewKey.entrySet()) {
						dynamic = dynamic.renameAndFixField(entry.getKey(), entry.getValue(), FixUtil::fixBlockPos);
					}

					return dynamic;
				}
		);
	}

	private <T> Dynamic<T> fixMapItemFrames(Dynamic<T> dynamic) {
		return dynamic
				.update(
						"frames", frames -> frames.createList(frames.asStream().map(frame -> {
							frame = frame.renameAndFixField("Pos", "pos", FixUtil::fixBlockPos);
							frame = frame.renameField("Rotation", "rotation");
							return frame.renameField("EntityId", "entity_id");
						}))
				)
				.update(
						"banners", banners -> banners.createList(banners.asStream().map(banner -> {
							banner = banner.renameField("Pos", "pos");
							banner = banner.renameField("Color", "color");
							return banner.renameField("Name", "name");
						}))
				);
	}

	public TypeRewriteRule makeRule() {
		List<TypeRewriteRule> rules = new ArrayList<>();
		addEntityFixes(rules);
		addBlockEntityFixes(rules);

		rules.add(
				writeFixAndRead(
						"BlockPos format for map frames",
						getInputSchema().getType(TypeReferences.SAVED_DATA_MAP_DATA),
						getOutputSchema().getType(TypeReferences.SAVED_DATA_MAP_DATA),
						dynamic -> dynamic.update("data", this::fixMapItemFrames)
				)
		);

		Type<?> itemType = getInputSchema().getType(TypeReferences.ITEM_STACK);
		rules.add(
				fixTypeEverywhereTyped(
						"BlockPos format for compass target",
						itemType,
						ItemNbtFix.fixNbt(
								itemType,
								"minecraft:compass"::equals,
								typed -> typed.update(
										DSL.remainderFinder(),
										dynamic -> dynamic.update("LodestonePos", FixUtil::fixBlockPos)
								)
						)
				)
		);

		return TypeRewriteRule.seq(rules);
	}

	private void addEntityFixes(List<TypeRewriteRule> rules) {
		rules.add(createFixRule(
				TypeReferences.ENTITY,
				"minecraft:bee",
				Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")
		));
		rules.add(createFixRule(
				TypeReferences.ENTITY,
				"minecraft:end_crystal",
				Map.of("BeamTarget", "beam_target")
		));
		rules.add(createFixRule(
				TypeReferences.ENTITY,
				"minecraft:wandering_trader",
				Map.of("WanderTarget", "wander_target")
		));

		for (String entityId : PATROL_TARGET_ENTITY_IDS) {
			rules.add(createFixRule(TypeReferences.ENTITY, entityId, Map.of("PatrolTarget", "patrol_target")));
		}

		rules.add(
				fixTypeEverywhereTyped(
						"BlockPos format in Leash for mobs",
						getInputSchema().getType(TypeReferences.ENTITY),
						typed -> typed.update(
								DSL.remainderFinder(),
								entityDynamic -> entityDynamic.renameAndFixField("Leash", "leash", FixUtil::fixBlockPos)
						)
				)
		);
	}

	private void addBlockEntityFixes(List<TypeRewriteRule> rules) {
		rules.add(createFixRule(
				TypeReferences.BLOCK_ENTITY,
				"minecraft:beehive",
				Map.of("FlowerPos", "flower_pos")
		));
		rules.add(createFixRule(
				TypeReferences.BLOCK_ENTITY,
				"minecraft:end_gateway",
				Map.of("ExitPortal", "exit_portal")
		));
	}

	private TypeRewriteRule createFixRule(TypeReference typeReference, String id, Map<String, String> oldToNewKey) {
		String fixName =
				"BlockPos format in " + oldToNewKey.keySet() + " for " + id + " (" + typeReference.typeName() + ")";
		OpticFinder<?> entityFinder = DSL.namedChoice(id, getInputSchema().getChoiceType(typeReference, id));

		return fixTypeEverywhereTyped(
				fixName,
				getInputSchema().getType(typeReference),
				typed -> typed.updateTyped(entityFinder, inner -> fixOldBlockPosFormat(inner, oldToNewKey))
		);
	}
}

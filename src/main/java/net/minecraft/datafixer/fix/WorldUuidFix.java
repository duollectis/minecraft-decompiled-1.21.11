package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import org.slf4j.Logger;

/**
 * {@code WorldUuidFix}.
 */
public class WorldUuidFix extends AbstractUuidFix {

	private static final Logger LOGGER = LogUtils.getLogger();

	public WorldUuidFix(Schema outputSchema) {
		super(outputSchema, TypeReferences.LEVEL);
	}

	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(this.typeReference);
		OpticFinder<?> opticFinder = type.findField("CustomBossEvents");
		OpticFinder<?> opticFinder2 = DSL.typeFinder(
				DSL.and(
						DSL.optional(DSL.field(
								"Name",
								this.getInputSchema().getTypeRaw(TypeReferences.TEXT_COMPONENT)
						)), DSL.remainderType()
				)
		);
		return this.fixTypeEverywhereTyped(
				"LevelUUIDFix", type, typed -> typed
						.update(
								DSL.remainderFinder(), dynamic -> {
									dynamic = this.fixDragonUuid(dynamic);
									return this.fixWanderingTraderId(dynamic);
								}
						)
						.updateTyped(
								opticFinder,
								typedx -> typedx.updateTyped(
										opticFinder2,
										typedxx -> typedxx.update(DSL.remainderFinder(), this::fixCustomBossEvents)
								)
						)
		);
	}

	private Dynamic<?> fixWanderingTraderId(Dynamic<?> levelDynamic) {
		return updateStringUuid(levelDynamic, "WanderingTraderId", "WanderingTraderId").orElse(levelDynamic);
	}

	private Dynamic<?> fixDragonUuid(Dynamic<?> levelDynamic) {
		return levelDynamic.update(
				"DimensionData",
				dimensionDataDynamic -> dimensionDataDynamic.updateMapValues(
						entry -> entry.mapSecond(
								dimensionDataValueDynamic -> dimensionDataValueDynamic.update(
										"DragonFight",
										dragonFightDynamic -> updateRegularMostLeast(
												dragonFightDynamic,
												"DragonUUID",
												"Dragon"
										).orElse(dragonFightDynamic)
								)
						)
				)
		);
	}

	private Dynamic<?> fixCustomBossEvents(Dynamic<?> levelDynamic) {
		return levelDynamic.update(
				"Players",
				dynamic2 -> levelDynamic.createList(dynamic2
						.asStream()
						.map(dynamicx -> (Dynamic<?>) createArrayFromCompoundUuid(dynamicx).orElseGet(() -> {
							LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
							return dynamicx;
						})))
		);
	}
}

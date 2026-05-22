package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет индексацию слотов инвентаря у лошадей с сундуком (лама, мул, осёл):
 * слоты начинались с индекса 2 вместо 0, поэтому здесь вычитается 2 из каждого
 * значения поля {@code Slot}.
 */
public class HorseChestIndexingFix extends DataFix {

	private static final int SLOT_INDEX_OFFSET = 2;

	public HorseChestIndexingFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected TypeRewriteRule makeRule() {
		OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>
			itemStackFinder =
			(OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>) DSL.typeFinder(
				getInputSchema().getType(TypeReferences.ITEM_STACK)
			);

		Type<?> entityType = getInputSchema().getType(TypeReferences.ENTITY);

		return TypeRewriteRule.seq(
			fixIndexing(itemStackFinder, entityType, "minecraft:llama"),
			new TypeRewriteRule[]{
				fixIndexing(itemStackFinder, entityType, "minecraft:trader_llama"),
				fixIndexing(itemStackFinder, entityType, "minecraft:mule"),
				fixIndexing(itemStackFinder, entityType, "minecraft:donkey")
			}
		);
	}

	private TypeRewriteRule fixIndexing(
		OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> itemStackFinder,
		Type<?> entityType,
		String entityId
	) {
		Type<?> specificEntityType = getInputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		OpticFinder<?> entityFinder = DSL.namedChoice(entityId, specificEntityType);
		OpticFinder<?> itemsFinder = specificEntityType.findField("Items");

		return fixTypeEverywhereTyped(
			"Fix non-zero indexing in chest horse type " + entityId,
			entityType,
			entity -> entity.updateTyped(
				entityFinder,
				specificEntity -> specificEntity.updateTyped(
					itemsFinder,
					items -> items.update(
						itemStackFinder,
						itemStack -> itemStack.mapSecond(
							outer -> outer.mapSecond(
								inner -> inner.mapSecond(
									itemData -> itemData.update(
										"Slot",
										slot -> slot.createByte((byte) (slot.asInt(SLOT_INDEX_OFFSET) - SLOT_INDEX_OFFSET))
									)
								)
							)
						)
					)
				)
			)
		);
	}
}

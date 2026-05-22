package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Мигрирует формат снаряжения сущностей: объединяет раздельные списки
 * {@code ArmorItems}, {@code HandItems}, {@code body_armor_item} и {@code saddle}
 * в единый объект {@code equipment} с именованными слотами
 * ({@code mainhand}, {@code offhand}, {@code feet}, {@code legs}, {@code chest},
 * {@code head}, {@code body}, {@code saddle}).
 * Если все слоты пусты — поле {@code equipment} не создаётся.
 */
public class EquipmentFormatFix extends DataFix {

	public EquipmentFormatFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> inputItemType = getInputSchema().getTypeRaw(TypeReferences.ITEM_STACK);
		Type<?> outputItemType = getOutputSchema().getTypeRaw(TypeReferences.ITEM_STACK);
		OpticFinder<?> idFinder = inputItemType.findField("id");
		return buildEquipmentRewriteRule(inputItemType, outputItemType, idFinder);
	}

	@SuppressWarnings("unchecked")
	private <ItemStackOld, ItemStackNew> TypeRewriteRule buildEquipmentRewriteRule(
		Type<ItemStackOld> inputItemType,
		Type<ItemStackNew> outputItemType,
		OpticFinder<?> idFinder
	) {
		Type<Pair<String, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>>>
			inputEquipmentType =
			DSL.named(
				TypeReferences.ENTITY_EQUIPMENT.typeName(),
				DSL.and(
					DSL.optional(DSL.field("ArmorItems", DSL.list(inputItemType))),
					DSL.optional(DSL.field("HandItems", DSL.list(inputItemType))),
					DSL.optional(DSL.field("body_armor_item", inputItemType)),
					DSL.optional(DSL.field("saddle", inputItemType))
				)
			);

		Type<Pair<String, Either<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>>>
			outputEquipmentType =
			DSL.named(
				TypeReferences.ENTITY_EQUIPMENT.typeName(),
				DSL.optional(
					DSL.field(
						"equipment",
						DSL.and(
							DSL.optional(DSL.field("mainhand", outputItemType)),
							DSL.optional(DSL.field("offhand", outputItemType)),
							DSL.optional(DSL.field("feet", outputItemType)),
							DSL.and(
								DSL.optional(DSL.field("legs", outputItemType)),
								DSL.optional(DSL.field("chest", outputItemType)),
								DSL.optional(DSL.field("head", outputItemType)),
								DSL.and(
									DSL.optional(DSL.field("body", outputItemType)),
									DSL.optional(DSL.field("saddle", outputItemType)),
									DSL.remainderType()
								)
							)
						)
					)
				)
			);

		if (!inputEquipmentType.equals(getInputSchema().getType(TypeReferences.ENTITY_EQUIPMENT))) {
			throw new IllegalStateException("Input entity_equipment type does not match expected");
		}

		if (!outputEquipmentType.equals(getOutputSchema().getType(TypeReferences.ENTITY_EQUIPMENT))) {
			throw new IllegalStateException("Output entity_equipment type does not match expected");
		}

		return this.<Pair<String, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>>, Pair<String, Either<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>>>fixTypeEverywhere(
			"EquipmentFormatFix",
			inputEquipmentType,
			outputEquipmentType,
			dynamicOps -> {
				Predicate<ItemStackOld> isEmptyItem = item -> {
					Typed<ItemStackOld> typed = new Typed<>(inputItemType, dynamicOps, item);
					return typed.getOptional(idFinder).isEmpty();
				};

				return pair -> {
					String entityId = (String) pair.getFirst();
					Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>
						slots = (Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>) pair.getSecond();

					List<ItemStackOld> armorItems = (List<ItemStackOld>) ((Either) slots.getFirst()).map(
						Function.identity(),
						unit -> List.of()
					);
					List<ItemStackOld> handItems = (List<ItemStackOld>) ((Either) ((Pair) slots.getSecond()).getFirst()).map(
						Function.identity(),
						unit -> List.of()
					);

					Either<ItemStackOld, Unit> bodyArmorItem =
						(Either<ItemStackOld, Unit>) ((Pair) ((Pair) slots.getSecond()).getSecond()).getFirst();
					Either<ItemStackOld, Unit> saddleItem =
						(Either<ItemStackOld, Unit>) ((Pair) ((Pair) slots.getSecond()).getSecond()).getSecond();

					Either<ItemStackOld, Unit> feetSlot = getEquipmentSlotItem(0, armorItems, isEmptyItem);
					Either<ItemStackOld, Unit> legsSlot = getEquipmentSlotItem(1, armorItems, isEmptyItem);
					Either<ItemStackOld, Unit> chestSlot = getEquipmentSlotItem(2, armorItems, isEmptyItem);
					Either<ItemStackOld, Unit> headSlot = getEquipmentSlotItem(3, armorItems, isEmptyItem);
					Either<ItemStackOld, Unit> mainhandSlot = getEquipmentSlotItem(0, handItems, isEmptyItem);
					Either<ItemStackOld, Unit> offhandSlot = getEquipmentSlotItem(1, handItems, isEmptyItem);

					if (areAllSlotsEmpty(bodyArmorItem, saddleItem, feetSlot, legsSlot, chestSlot, headSlot, mainhandSlot, offhandSlot)) {
						return Pair.of(
							entityId,
							Either.<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>right(Unit.INSTANCE)
						);
					}

					return Pair.of(
						entityId,
						Either.<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>left(
							Pair.of(
								(Either<ItemStackNew, Unit>) mainhandSlot,
								Pair.of(
									(Either<ItemStackNew, Unit>) offhandSlot,
									Pair.of(
										(Either<ItemStackNew, Unit>) feetSlot,
										Pair.of(
											(Either<ItemStackNew, Unit>) legsSlot,
											Pair.of(
												(Either<ItemStackNew, Unit>) chestSlot,
												Pair.of(
													(Either<ItemStackNew, Unit>) headSlot,
													Pair.of(
														(Either<ItemStackNew, Unit>) bodyArmorItem,
														Pair.of(
															(Either<ItemStackNew, Unit>) saddleItem,
															new Dynamic<>(dynamicOps)
														)
													)
												)
											)
										)
									)
								)
							)
						)
					);
				};
			}
		);
	}

	@SafeVarargs
	private static boolean areAllSlotsEmpty(Either<?, Unit>... slots) {
		for (Either<?, Unit> slot : slots) {
			if (slot.right().isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private static <ItemStack> Either<ItemStack, Unit> getEquipmentSlotItem(
		int slotIndex,
		List<ItemStack> items,
		Predicate<ItemStack> isEmptyItem
	) {
		if (slotIndex >= items.size()) {
			return Either.right(Unit.INSTANCE);
		}

		ItemStack item = items.get(slotIndex);
		return isEmptyItem.test(item) ? Either.right(Unit.INSTANCE) : Either.left(item);
	}
}

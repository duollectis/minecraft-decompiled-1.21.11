package net.minecraft.datafixer.fix;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Мигрирует старый формат снаряжения сущностей: поле {@code Equipment} (единый список)
 * разделяется на {@code ArmorItems}, {@code HandItems}, {@code body_armor_item} и {@code saddle}.
 * Также переносит шансы выпадения из {@code DropChances} в {@code HandDropChances} и {@code ArmorDropChances}.
 */
public class EntityEquipmentToArmorAndHandFix extends DataFix {

	public EntityEquipmentToArmorAndHandFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@Override
	public TypeRewriteRule makeRule() {
		return fixEquipment(
				getInputSchema().getTypeRaw(TypeReferences.ITEM_STACK),
				getOutputSchema().getTypeRaw(TypeReferences.ITEM_STACK)
		);
	}

	/**
	 * Строит правило перезаписи, которое конвертирует тип {@code ENTITY_EQUIPMENT}
	 * из единого списка {@code Equipment} в четыре отдельных поля нового формата.
	 *
	 * @param oldItemType тип предмета во входной схеме
	 * @param newItemType тип предмета в выходной схеме
	 * @param <ItemStackOld> параметр типа предмета старой схемы
	 * @param <ItemStackNew> параметр типа предмета новой схемы
	 * @return составное правило перезаписи
	 */
	private <ItemStackOld, ItemStackNew> TypeRewriteRule fixEquipment(
			Type<ItemStackOld> oldItemType,
			Type<ItemStackNew> newItemType
	) {
		Type<Pair<String, Either<List<ItemStackOld>, Unit>>> inputEquipmentType = DSL.named(
				TypeReferences.ENTITY_EQUIPMENT.typeName(),
				DSL.optional(DSL.field("Equipment", DSL.list(oldItemType)))
		);
		Type<Pair<String, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<ItemStackNew, Unit>, Either<ItemStackNew, Unit>>>>>>
				outputEquipmentType = DSL.named(
				TypeReferences.ENTITY_EQUIPMENT.typeName(),
				DSL.and(
						DSL.optional(DSL.field("ArmorItems", DSL.list(newItemType))),
						DSL.optional(DSL.field("HandItems", DSL.list(newItemType))),
						DSL.optional(DSL.field("body_armor_item", newItemType)),
						DSL.optional(DSL.field("saddle", newItemType))
				)
		);

		if (!inputEquipmentType.equals(getInputSchema().getType(TypeReferences.ENTITY_EQUIPMENT))) {
			throw new IllegalStateException("Input entity_equipment type does not match expected");
		}

		if (!outputEquipmentType.equals(getOutputSchema().getType(TypeReferences.ENTITY_EQUIPMENT))) {
			throw new IllegalStateException("Output entity_equipment type does not match expected");
		}

		return TypeRewriteRule.seq(
				fixTypeEverywhereTyped(
						"EntityEquipmentToArmorAndHandFix - drop chances",
						getInputSchema().getType(TypeReferences.ENTITY),
						typed -> typed.update(DSL.remainderFinder(), EntityEquipmentToArmorAndHandFix::fixEquipmentData)
				),
				fixTypeEverywhere(
						"EntityEquipmentToArmorAndHandFix - equipment",
						inputEquipmentType,
						outputEquipmentType,
						dynamicOps -> {
							ItemStackNew emptyItem = (ItemStackNew) ((Pair) newItemType
									.read(new Dynamic<>(dynamicOps).emptyMap())
									.result()
									.orElseThrow(() -> new IllegalStateException(
											"Could not parse newly created empty itemstack."
									)))
									.getFirst();

							Either<ItemStackNew, Unit> absentItem = Either.right(DSL.unit());

							return pair -> pair.mapSecond(equipmentEither -> {
								List<ItemStackOld> equipment = (List<ItemStackOld>) equipmentEither.map(
										Function.identity(),
										unit -> List.of()
								);

								Either<List<ItemStackNew>, Unit> handItems = Either.right(DSL.unit());
								Either<List<ItemStackNew>, Unit> armorItems = Either.right(DSL.unit());

								if (!equipment.isEmpty()) {
									handItems = Either.left(new ArrayList<>(Arrays.asList(
											(ItemStackNew) equipment.getFirst(),
											emptyItem
									)));
								}

								if (equipment.size() > 1) {
									List<ItemStackNew> armorSlots = Lists.newArrayList(
											emptyItem,
											emptyItem,
											emptyItem,
											emptyItem
									);

									for (int slotIndex = 1; slotIndex < Math.min(equipment.size(), 5); slotIndex++) {
										armorSlots.set(slotIndex - 1, (ItemStackNew) equipment.get(slotIndex));
									}

									armorItems = Either.left(armorSlots);
								}

								return Pair.of(armorItems, Pair.of(handItems, Pair.of(absentItem, absentItem)));
							});
						}
				)
		);
	}

	private static Dynamic<?> fixEquipmentData(Dynamic<?> entity) {
		Optional<? extends Stream<? extends Dynamic<?>>> dropChances = entity.get("DropChances").asStreamOpt().result();
		entity = entity.remove("DropChances");

		if (dropChances.isEmpty()) {
			return entity;
		}

		Iterator<Float> chances = Stream
				.concat(dropChances.get().map(d -> d.asFloat(0.0F)), Stream.generate(() -> 0.0F))
				.iterator();

		float mainHandChance = chances.next();

		if (entity.get("HandDropChances").result().isEmpty()) {
			entity = entity.set(
					"HandDropChances",
					entity.createList(Stream.of(mainHandChance, 0.0F).map(entity::createFloat))
			);
		}

		if (entity.get("ArmorDropChances").result().isEmpty()) {
			entity = entity.set(
					"ArmorDropChances",
					entity.createList(
							Stream.of(chances.next(), chances.next(), chances.next(), chances.next())
									.map(entity::createFloat)
					)
			);
		}

		return entity;
	}
}

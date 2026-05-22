package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;

/**
 * Мигрирует старый булев флаг {@code Saddle} у лошади в полноценный предмет {@code SaddleItem}.
 * До этого фикса сёдла хранились как простой флаг, а не как предмет в слоте снаряжения.
 */
public class EntityHorseSaddleFix extends ChoiceFix {

	public EntityHorseSaddleFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType, "EntityHorseSaddleFix", TypeReferences.ENTITY, "EntityHorse");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder(
				"id",
				DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		Type<?> itemStackType = getInputSchema().getTypeRaw(TypeReferences.ITEM_STACK);
		OpticFinder<?> saddleItemFinder = DSL.fieldFinder("SaddleItem", itemStackType);

		Optional<? extends Typed<?>> existingSaddle = inputTyped.getOptionalTyped(saddleItemFinder);
		Dynamic<?> horse = (Dynamic<?>) inputTyped.get(DSL.remainderFinder());

		if (existingSaddle.isPresent() || !horse.get("Saddle").asBoolean(false)) {
			return inputTyped;
		}

		Typed<?> saddleItem = (Typed<?>) itemStackType
				.pointTyped(inputTyped.getOps())
				.orElseThrow(IllegalStateException::new);

		saddleItem = saddleItem.set(idFinder, Pair.of(TypeReferences.ITEM_NAME.typeName(), "minecraft:saddle"));

		Dynamic<?> saddleNbt = horse.emptyMap();
		saddleNbt = saddleNbt.set("Count", saddleNbt.createByte((byte) 1));
		saddleNbt = saddleNbt.set("Damage", saddleNbt.createShort((short) 0));
		saddleItem = saddleItem.set(DSL.remainderFinder(), saddleNbt);

		horse = horse.remove("Saddle");

		return inputTyped
				.set(saddleItemFinder, saddleItem)
				.set(DSL.remainderFinder(), horse);
	}
}

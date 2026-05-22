package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Абстрактный фикс для модификации NBT-тегов предметов по их идентификатору.
 * Подклассы реализуют {@link #fix(Typed)} для конкретного преобразования данных.
 */
public abstract class ItemNbtFix extends DataFix {

	private final String name;
	private final Predicate<String> itemIdPredicate;

	public ItemNbtFix(Schema outputSchema, String name, Predicate<String> itemIdPredicate) {
		super(outputSchema, false);
		this.name = name;
		this.itemIdPredicate = itemIdPredicate;
	}

	@Override
	public final TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ITEM_STACK);
		return fixTypeEverywhereTyped(name, type, fixNbt(type, itemIdPredicate, this::fix));
	}

	public static UnaryOperator<Typed<?>> fixNbt(
			Type<?> itemStackType,
			Predicate<String> itemIdPredicate,
			UnaryOperator<Typed<?>> nbtFixer
	) {
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<?> opticFinder2 = itemStackType.findField("tag");
		return itemStackTyped -> {
			Optional<Pair<String, String>> optional = itemStackTyped.getOptional(opticFinder);
			return optional.isPresent() && itemIdPredicate.test((String) optional.get().getSecond())
			       ? itemStackTyped.updateTyped(opticFinder2, nbtFixer)
			       : itemStackTyped;
		};
	}

	protected abstract Typed<?> fix(Typed<?> typed);
}

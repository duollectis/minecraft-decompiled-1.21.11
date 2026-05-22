package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * Исправление DataFixer, которое добавляет поле {@code item} к снарядам-стрелам и трезубцам.
 * <p>
 * До версии 1.20.5 стрелы хранили информацию о зелье в поле {@code Potion},
 * а трезубцы не имели явного поля предмета. Это исправление создаёт стек предмета
 * в поле {@code item} для {@code minecraft:arrow}, {@code minecraft:spectral_arrow}
 * и {@code minecraft:trident}.
 */
public class ProjectileItemTypeFix extends DataFix {

	private static final String EMPTY_ID = "minecraft:empty";

	public ProjectileItemTypeFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ENTITY);
		Type<?> type2 = getOutputSchema().getType(TypeReferences.ENTITY);
		return fixTypeEverywhereTyped(
				"Fix AbstractArrow item type",
				type,
				type2,
				FixUtil.compose(
						this.createFixApplier("minecraft:trident", ProjectileItemTypeFix::fixTrident),
						this.createFixApplier("minecraft:arrow", ProjectileItemTypeFix::fixArrow),
						this.createFixApplier("minecraft:spectral_arrow", ProjectileItemTypeFix::fixSpectralArrow)
				)
		);
	}

	private Function<Typed<?>, Typed<?>> createFixApplier(String id, ProjectileItemTypeFix.Fixer<?> fixer) {
		Type<?> type = getInputSchema().getChoiceType(TypeReferences.ENTITY, id);
		Type<?> type2 = getOutputSchema().getChoiceType(TypeReferences.ENTITY, id);
		return createFixApplier(id, fixer, type, type2);
	}

	@SuppressWarnings("unchecked")
	private static <T> Function<Typed<?>, Typed<?>> createFixApplier(
			String id,
			ProjectileItemTypeFix.Fixer<?> fixer,
			Type<?> inputType,
			Type<T> outputType
	) {
		OpticFinder<?> opticFinder = DSL.namedChoice(id, inputType);
		ProjectileItemTypeFix.Fixer<T> typedFixer = (ProjectileItemTypeFix.Fixer<T>) fixer;
		return typed -> typed.updateTyped(opticFinder, outputType, typedx -> typedFixer.fix(typedx, outputType));
	}

	private static <T> Typed<T> fixArrow(Typed<?> typed, Type<T> type) {
		return Util.apply(typed, type, data -> data.set("item", createStack(data, getArrowId(data))));
	}

	private static String getArrowId(Dynamic<?> arrowData) {
		return arrowData.get("Potion").asString(EMPTY_ID).equals(EMPTY_ID)
				? "minecraft:arrow"
				: "minecraft:tipped_arrow";
	}

	private static <T> Typed<T> fixSpectralArrow(Typed<?> typed, Type<T> type) {
		return Util.apply(typed, type, data -> data.set("item", createStack(data, "minecraft:spectral_arrow")));
	}

	private static Dynamic<?> createStack(Dynamic<?> projectileData, String id) {
		return projectileData.createMap(
				ImmutableMap.of(
						projectileData.createString("id"),
						projectileData.createString(id),
						projectileData.createString("Count"),
						projectileData.createInt(1)
				)
		);
	}

	private static <T> Typed<T> fixTrident(Typed<?> typed, Type<T> type) {
		return new Typed(type, typed.getOps(), typed.getValue());
	}

	/**
	 * Функциональный интерфейс для применения исправления к конкретному типу снаряда.
	 */
	interface Fixer<F> {

		Typed<F> fix(Typed<?> typed, Type<F> type);
	}
}

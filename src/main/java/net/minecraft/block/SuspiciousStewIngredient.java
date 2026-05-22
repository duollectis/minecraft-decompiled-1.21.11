package net.minecraft.block;

import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Маркерный интерфейс для блоков-цветков, которые могут быть ингредиентом подозрительного рагу.
 */
public interface SuspiciousStewIngredient {

	SuspiciousStewEffectsComponent getStewEffects();

	static List<SuspiciousStewIngredient> getAll() {
		return Registries.ITEM
				.stream()
				.map(SuspiciousStewIngredient::of)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Возвращает ингредиент рагу для предмета, если он является блоком-цветком
	 * или напрямую реализует этот интерфейс. Иначе возвращает {@code null}.
	 */
	static @Nullable SuspiciousStewIngredient of(ItemConvertible item) {
		if (item.asItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof SuspiciousStewIngredient ingredient) {
			return ingredient;
		}

		return item.asItem() instanceof SuspiciousStewIngredient ingredient ? ingredient : null;
	}
}

package net.minecraft.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.loot.slot.ItemStream;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Источник ссылок на стаки предметов по индексу слота.
 * Используется для унифицированного доступа к предметам в инвентарях,
 * экипировке и других контейнерах через {@link StackReference}.
 */
public interface StackReferenceGetter {

	@Nullable StackReference getStackReference(int slot);

	/**
	 * Возвращает поток ссылок на стаки для указанного набора слотов.
	 * Слоты, для которых {@link #getStackReference} вернул {@code null}, пропускаются.
	 *
	 * @param slots список идентификаторов слотов
	 * @return поток валидных ссылок на стаки
	 */
	default ItemStream getStackReferences(IntList slots) {
		List<StackReference> references = slots.intStream()
			.mapToObj(this::getStackReference)
			.filter(Objects::nonNull)
			.toList();

		return ItemStream.of(references);
	}
}

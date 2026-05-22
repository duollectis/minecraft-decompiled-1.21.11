package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.item.ItemPredicate;

/**
 * Источник слотов, фильтрующий предметы из дочернего источника по предикату {@link ItemPredicate}.
 */
public class FilteredSlotSource extends TransformSlotSource {

	public static final MapCodec<FilteredSlotSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> addSlotSourceField(instance)
					.and(ItemPredicate.CODEC.fieldOf("item_filter").forGetter(source -> source.itemFilter))
					.apply(instance, FilteredSlotSource::new)
	);
	private final ItemPredicate itemFilter;

	private FilteredSlotSource(SlotSource slotSource, ItemPredicate itemFilter) {
		super(slotSource);
		this.itemFilter = itemFilter;
	}

	@Override
	public MapCodec<FilteredSlotSource> getCodec() {
		return CODEC;
	}

	@Override
	protected ItemStream transform(ItemStream stream) {
		return stream.filter(itemFilter);
	}
}

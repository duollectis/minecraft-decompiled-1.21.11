package net.minecraft.loot.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * {@code SlotSources}.
 */
public interface SlotSources {

	Codec<SlotSource>
			BASE_CODEC =
			Registries.SLOT_SOURCE_TYPE.getCodec().dispatch(SlotSource::getCodec, codec -> codec);

	Codec<SlotSource>
			CODEC =
			Codec.lazyInitialized(() -> Codec.withAlternative(BASE_CODEC, GroupSlotSource.INLINE_CODEC));

	static MapCodec<? extends SlotSource> registerAndGetDefault(Registry<MapCodec<? extends SlotSource>> registry) {
		Registry.register(registry, "group", GroupSlotSource.CODEC);
		Registry.register(registry, "filtered", FilteredSlotSource.CODEC);
		Registry.register(registry, "limit_slots", LimitSlotsSlotSource.CODEC);
		Registry.register(registry, "slot_range", SlotRangeSlotSource.CODEC);
		Registry.register(registry, "contents", ContentsSlotSource.CODEC);
		return Registry.register(registry, "empty", EmptySlotSourceType.CODEC);
	}

	static Function<LootContext, ItemStream> concat(Collection<? extends SlotSource> sources) {
		List<SlotSource> list = List.copyOf(sources);

		return switch (list.size()) {
			case 0 -> context -> ItemStream.EMPTY;
			case 1 -> list.getFirst()::stream;
			case 2 -> {
				SlotSource slotSource = list.get(0);
				SlotSource slotSource2 = list.get(1);
				yield context -> ItemStream.concat(slotSource.stream(context), slotSource2.stream(context));
			}
			default -> context -> {
				List<ItemStream> list2 = new ArrayList<>();

				for (SlotSource slotSourcex : list) {
					list2.add(slotSourcex.stream(context));
				}

				return ItemStream.concat(list2);
			};
		};
	}
}

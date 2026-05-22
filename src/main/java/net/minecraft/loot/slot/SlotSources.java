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
 * Реестр и утилиты для работы с {@link SlotSource}.
 * Поддерживает инлайн-кодек для {@link GroupSlotSource} (сокращённая JSON-запись в виде массива).
 */
public interface SlotSources {

	Codec<SlotSource> BASE_CODEC = Registries.SLOT_SOURCE_TYPE.getCodec().dispatch(SlotSource::getCodec, codec -> codec);

	Codec<SlotSource> CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(BASE_CODEC, GroupSlotSource.INLINE_CODEC));

	static MapCodec<? extends SlotSource> registerAndGetDefault(Registry<MapCodec<? extends SlotSource>> registry) {
		Registry.register(registry, "group", GroupSlotSource.CODEC);
		Registry.register(registry, "filtered", FilteredSlotSource.CODEC);
		Registry.register(registry, "limit_slots", LimitSlotsSlotSource.CODEC);
		Registry.register(registry, "slot_range", SlotRangeSlotSource.CODEC);
		Registry.register(registry, "contents", ContentsSlotSource.CODEC);
		return Registry.register(registry, "empty", EmptySlotSourceType.CODEC);
	}

	/**
	 * Компилирует коллекцию источников слотов в единую функцию конкатенации потоков.
	 * Оптимизирует частные случаи (0, 1, 2 источника) без создания промежуточных списков.
	 *
	 * @param sources коллекция источников слотов
	 * @return функция, принимающая контекст лута и возвращающая объединённый поток предметов
	 */
	static Function<LootContext, ItemStream> concat(Collection<? extends SlotSource> sources) {
		List<SlotSource> frozen = List.copyOf(sources);

		return switch (frozen.size()) {
			case 0 -> context -> ItemStream.EMPTY;
			case 1 -> frozen.getFirst()::stream;
			case 2 -> {
				SlotSource first = frozen.get(0);
				SlotSource second = frozen.get(1);
				yield context -> ItemStream.concat(first.stream(context), second.stream(context));
			}
			default -> context -> {
				List<ItemStream> streams = new ArrayList<>();

				for (SlotSource source : frozen) {
					streams.add(source.stream(context));
				}

				return ItemStream.concat(streams);
			};
		};
	}
}

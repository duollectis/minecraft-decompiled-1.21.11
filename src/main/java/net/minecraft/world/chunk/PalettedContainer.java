package net.minecraft.world.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.thread.LockHelper;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;

/**
 * Потокобезопасный контейнер данных секции чанка с палитрой.
 * Хранит блок-стейты или биомы в сжатом виде: каждый элемент занимает
 * фиксированное число бит, а реальные объекты хранятся в палитре.
 *
 * <p>Автоматически расширяет палитру при добавлении новых значений,
 * переключаясь между реализациями: Singular → Array → BiMap → IdList.
 */
public class PalettedContainer<T> implements PaletteResizeListener<T>, ReadableContainer<T> {

	private static final int INITIAL_BITS = 0;

	private volatile PalettedContainer.Data<T> data;
	private final PaletteProvider<T> paletteProvider;
	private final LockHelper lockHelper = new LockHelper("PalettedContainer");

	public void lock() {
		lockHelper.lock();
	}

	public void unlock() {
		lockHelper.unlock();
	}

	public static <T> Codec<PalettedContainer<T>> createPalettedContainerCodec(
		Codec<T> entryCodec,
		PaletteProvider<T> provider,
		T defaultValue
	) {
		ReadableContainer.Reader<T, PalettedContainer<T>> reader = PalettedContainer::read;
		return createCodec(entryCodec, provider, defaultValue, reader);
	}

	public static <T> Codec<ReadableContainer<T>> createReadableContainerCodec(
		Codec<T> entryCodec,
		PaletteProvider<T> provider,
		T defaultValue
	) {
		ReadableContainer.Reader<T, ReadableContainer<T>> reader =
			(paletteProvider, serialized) -> read(paletteProvider, serialized).map(result -> result);
		return createCodec(entryCodec, provider, defaultValue, reader);
	}

	private static <T, C extends ReadableContainer<T>> Codec<C> createCodec(
		Codec<T> entryCodec,
		PaletteProvider<T> provider,
		T defaultValue,
		ReadableContainer.Reader<T, C> reader
	) {
		return RecordCodecBuilder.<ReadableContainer.Serialized<T>>create(
			instance -> instance.group(
				entryCodec
					.mapResult(Codecs.orElsePartial(defaultValue))
					.listOf()
					.fieldOf("palette")
					.forGetter(ReadableContainer.Serialized::paletteEntries),
				Codec.LONG_STREAM
					.lenientOptionalFieldOf("data")
					.forGetter(ReadableContainer.Serialized::storage)
			).apply(instance, ReadableContainer.Serialized::new)
		).comapFlatMap(
			serialized -> reader.read(provider, serialized),
			container -> container.serialize(provider)
		);
	}

	private PalettedContainer(
		PaletteProvider<T> paletteProvider,
		PaletteType type,
		PaletteStorage storage,
		Palette<T> palette
	) {
		this.paletteProvider = paletteProvider;
		this.data = new PalettedContainer.Data<>(type, storage, palette);
	}

	private PalettedContainer(PalettedContainer<T> container) {
		this.paletteProvider = container.paletteProvider;
		this.data = container.data.copy();
	}

	public PalettedContainer(T defaultValue, PaletteProvider<T> paletteProvider) {
		this.paletteProvider = paletteProvider;
		this.data = getCompatibleData(null, INITIAL_BITS);
		this.data.palette.index(defaultValue, this);
	}

	private PalettedContainer.Data<T> getCompatibleData(PalettedContainer.@Nullable Data<T> previousData, int bits) {
		PaletteType paletteType = paletteProvider.createType(bits);
		if (previousData != null && paletteType.equals(previousData.configuration())) {
			return previousData;
		}

		PaletteStorage paletteStorage = paletteType.bitsInMemory() == 0
			? new EmptyPaletteStorage(paletteProvider.getSize())
			: new PackedIntegerArray(paletteType.bitsInMemory(), paletteProvider.getSize());
		Palette<T> palette = paletteType.createPalette(paletteProvider, List.of());
		return new PalettedContainer.Data<>(paletteType, paletteStorage, palette);
	}

	@Override
	public int onResize(int newBits, T object) {
		PalettedContainer.Data<T> oldData = data;
		PalettedContainer.Data<T> newData = getCompatibleData(oldData, newBits);
		newData.importFrom(oldData.palette, oldData.storage);
		data = newData;
		return newData.palette.index(object, PaletteResizeListener.throwing());
	}

	/**
	 * Атомарно заменяет значение по координатам и возвращает предыдущее.
	 * Потокобезопасен — использует внутреннюю блокировку.
	 */
	public T swap(int x, int y, int z, T value) {
		lock();

		T previous;
		try {
			previous = swap(paletteProvider.computeIndex(x, y, z), value);
		} finally {
			unlock();
		}

		return previous;
	}

	/** Небезопасная версия {@link #swap} без блокировки — для однопоточного использования. */
	public T swapUnsafe(int x, int y, int z, T value) {
		return swap(paletteProvider.computeIndex(x, y, z), value);
	}

	@SuppressWarnings("unchecked")
	private T swap(int index, T value) {
		int paletteId = data.palette.index(value, this);
		int previousId = data.storage.swap(index, paletteId);
		return data.palette.get(previousId);
	}

	public void set(int x, int y, int z, T value) {
		lock();

		try {
			set(paletteProvider.computeIndex(x, y, z), value);
		} finally {
			unlock();
		}
	}

	private void set(int index, T value) {
		int paletteId = data.palette.index(value, this);
		data.storage.set(index, paletteId);
	}

	@Override
	public T get(int x, int y, int z) {
		return get(paletteProvider.computeIndex(x, y, z));
	}

	protected T get(int index) {
		PalettedContainer.Data<T> snapshot = data;
		return snapshot.palette.get(snapshot.storage.get(index));
	}

	@Override
	public void forEachValue(Consumer<T> action) {
		Palette<T> palette = data.palette();
		IntSet usedIds = new IntArraySet();
		data.storage.forEach(usedIds::add);
		usedIds.forEach(id -> action.accept(palette.get(id)));
	}

	public void readPacket(PacketByteBuf buf) {
		lock();

		try {
			int bits = buf.readByte();
			PalettedContainer.Data<T> newData = getCompatibleData(data, bits);
			newData.palette.readPacket(buf, paletteProvider.getIdList());
			buf.readFixedLengthLongArray(newData.storage.getData());
			data = newData;
		} finally {
			unlock();
		}
	}

	@Override
	public void writePacket(PacketByteBuf buf) {
		lock();

		try {
			data.writePacket(buf, paletteProvider.getIdList());
		} finally {
			unlock();
		}
	}

	/**
	 * Десериализует контейнер из сериализованного представления.
	 * При необходимости перепаковывает данные (если биты хранения отличаются от памяти).
	 */
	@VisibleForTesting
	public static <T> DataResult<PalettedContainer<T>> read(
		PaletteProvider<T> provider,
		ReadableContainer.Serialized<T> serialized
	) {
		List<T> paletteEntries = serialized.paletteEntries();
		int containerSize = provider.getSize();
		PaletteType paletteType = provider.createTypeFromSize(paletteEntries.size());
		int bitsInStorage = paletteType.bitsInStorage();
		if (serialized.bitsPerEntry() != ReadableContainer.Serialized.MISSING_BITS_PER_ENTRY
				&& bitsInStorage != serialized.bitsPerEntry()) {
			return DataResult.error(() -> "Invalid bit count, calculated " + bitsInStorage
				+ ", but container declared " + serialized.bitsPerEntry());
		}

		if (paletteType.bitsInMemory() == 0) {
			Palette<T> palette = paletteType.createPalette(provider, paletteEntries);
			PaletteStorage storage = new EmptyPaletteStorage(containerSize);
			return DataResult.success(new PalettedContainer<>(provider, paletteType, storage, palette));
		}

		Optional<LongStream> storageData = serialized.storage();
		if (storageData.isEmpty()) {
			return DataResult.error(() -> "Missing values for non-zero storage");
		}

		long[] rawData = storageData.get().toArray();
		try {
			PaletteStorage storage;
			Palette<T> palette;
			if (!paletteType.shouldRepack() && paletteType.bitsInMemory() == bitsInStorage) {
				palette = paletteType.createPalette(provider, paletteEntries);
				storage = new PackedIntegerArray(paletteType.bitsInMemory(), containerSize, rawData);
			} else {
				Palette<T> sourcePalette = new BiMapPalette<>(bitsInStorage, paletteEntries);
				PackedIntegerArray sourceStorage = new PackedIntegerArray(bitsInStorage, containerSize, rawData);
				palette = paletteType.createPalette(provider, paletteEntries);
				int[] repacked = repack(sourceStorage, sourcePalette, palette);
				storage = new PackedIntegerArray(paletteType.bitsInMemory(), containerSize, repacked);
			}

			return DataResult.success(new PalettedContainer<>(provider, paletteType, storage, palette));
		} catch (PackedIntegerArray.InvalidLengthException ex) {
			return DataResult.error(() -> "Failed to read PalettedContainer: " + ex.getMessage());
		}
	}

	@Override
	public ReadableContainer.Serialized<T> serialize(PaletteProvider<T> provider) {
		lock();

		try {
			BiMapPalette<T> exportPalette = new BiMapPalette<>(data.storage.getElementBits());
			int containerSize = provider.getSize();
			int[] repacked = repack(data.storage, data.palette, exportPalette);
			PaletteType exportType = provider.createTypeFromSize(exportPalette.getSize());
			int bitsInStorage = exportType.bitsInStorage();
			Optional<LongStream> storageStream;
			if (bitsInStorage != 0) {
				PackedIntegerArray packed = new PackedIntegerArray(bitsInStorage, containerSize, repacked);
				storageStream = Optional.of(Arrays.stream(packed.getData()));
			} else {
				storageStream = Optional.empty();
			}

			return new ReadableContainer.Serialized<>(exportPalette.getElements(), storageStream, bitsInStorage);
		} finally {
			unlock();
		}
	}

	/**
	 * Перепаковывает индексы из одной палитры в другую.
	 * Оптимизирован: кэширует последний обработанный индекс для пропуска повторных поисков.
	 */
	private static <T> int[] repack(PaletteStorage storage, Palette<T> oldPalette, Palette<T> newPalette) {
		int[] indices = new int[storage.getSize()];
		storage.writePaletteIndices(indices);
		PaletteResizeListener<T> noResize = PaletteResizeListener.throwing();
		int lastOldId = -1;
		int lastNewId = -1;

		for (int i = 0; i < indices.length; i++) {
			int oldId = indices[i];
			if (oldId != lastOldId) {
				lastOldId = oldId;
				lastNewId = newPalette.index(oldPalette.get(oldId), noResize);
			}

			indices[i] = lastNewId;
		}

		return indices;
	}

	@Override
	public int getPacketSize() {
		return data.getPacketSize(paletteProvider.getIdList());
	}

	@Override
	public int getElementBits() {
		return data.storage().getElementBits();
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		return data.palette.hasAny(predicate);
	}

	@Override
	public PalettedContainer<T> copy() {
		return new PalettedContainer<>(this);
	}

	@Override
	public PalettedContainer<T> slice() {
		return new PalettedContainer<>(data.palette.get(0), paletteProvider);
	}

	@Override
	public void count(PalettedContainer.Counter<T> counter) {
		if (data.palette.getSize() == 1) {
			counter.accept(data.palette.get(0), data.storage.getSize());
			return;
		}

		Int2IntOpenHashMap countMap = new Int2IntOpenHashMap();
		data.storage.forEach(key -> countMap.addTo(key, 1));
		countMap.int2IntEntrySet()
			.forEach(entry -> counter.accept(data.palette.get(entry.getIntKey()), entry.getIntValue()));
	}

	@FunctionalInterface
	public interface Counter<T> {

		void accept(T object, int count);
	}

	record Data<T>(PaletteType configuration, PaletteStorage storage, Palette<T> palette) {

		/**
		 * Импортирует все элементы из другой пары палитра+хранилище в текущую.
		 * Используется при расширении палитры (onResize).
		 */
		public void importFrom(Palette<T> sourcePalette, PaletteStorage sourceStorage) {
			PaletteResizeListener<T> noResize = PaletteResizeListener.throwing();

			for (int i = 0; i < sourceStorage.getSize(); i++) {
				T object = sourcePalette.get(sourceStorage.get(i));
				storage.set(i, palette.index(object, noResize));
			}
		}

		public int getPacketSize(IndexedIterable<T> idList) {
			return 1 + palette.getPacketSize(idList) + storage.getData().length * 8;
		}

		public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
			buf.writeByte(storage.getElementBits());
			palette.writePacket(buf, idList);
			buf.writeFixedLengthLongArray(storage.getData());
		}

		public PalettedContainer.Data<T> copy() {
			return new PalettedContainer.Data<>(configuration, storage.copy(), palette.copy());
		}
	}
}

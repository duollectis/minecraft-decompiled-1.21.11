package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Контекст чтения NBT-данных, хранящий реестры и операции сериализации.
 * <p>
 * Также является фабрикой пустых (null-object) представлений для случаев,
 * когда запрошенный ключ отсутствует в хранилище — это позволяет избежать
 * проверок на {@code null} в коде чтения.
 */
public class ReadContext {

	final RegistryWrapper.WrapperLookup registries;
	private final DynamicOps<NbtElement> ops;

	final ReadView.ListReadView emptyListReadView = new ReadView.ListReadView() {
		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public Stream<ReadView> stream() {
			return Stream.empty();
		}

		@Override
		public Iterator<ReadView> iterator() {
			return Collections.emptyIterator();
		}
	};

	private final ReadView.TypedListReadView<Object> emptyTypedListReadView = new ReadView.TypedListReadView<>() {
		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public Stream<Object> stream() {
			return Stream.empty();
		}

		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyIterator();
		}
	};

	private final ReadView emptyReadView = new ReadView() {
		@Override
		public <T> Optional<T> read(String key, Codec<T> codec) {
			return Optional.empty();
		}

		@Override
		public <T> Optional<T> read(MapCodec<T> mapCodec) {
			return Optional.empty();
		}

		@Override
		public Optional<ReadView> getOptionalReadView(String key) {
			return Optional.empty();
		}

		@Override
		public ReadView getReadView(String key) {
			return this;
		}

		@Override
		public Optional<ReadView.ListReadView> getOptionalListReadView(String key) {
			return Optional.empty();
		}

		@Override
		public ReadView.ListReadView getListReadView(String key) {
			return ReadContext.this.emptyListReadView;
		}

		@Override
		public <T> Optional<ReadView.TypedListReadView<T>> getOptionalTypedListView(String key, Codec<T> typeCodec) {
			return Optional.empty();
		}

		@Override
		public <T> ReadView.TypedListReadView<T> getTypedListView(String key, Codec<T> typeCodec) {
			return ReadContext.this.getEmptyTypedListReadView();
		}

		@Override
		public boolean getBoolean(String key, boolean fallback) {
			return fallback;
		}

		@Override
		public byte getByte(String key, byte fallback) {
			return fallback;
		}

		@Override
		public int getShort(String key, short fallback) {
			return fallback;
		}

		@Override
		public Optional<Integer> getOptionalInt(String key) {
			return Optional.empty();
		}

		@Override
		public int getInt(String key, int fallback) {
			return fallback;
		}

		@Override
		public long getLong(String key, long fallback) {
			return fallback;
		}

		@Override
		public Optional<Long> getOptionalLong(String key) {
			return Optional.empty();
		}

		@Override
		public float getFloat(String key, float fallback) {
			return fallback;
		}

		@Override
		public double getDouble(String key, double fallback) {
			return fallback;
		}

		@Override
		public Optional<String> getOptionalString(String key) {
			return Optional.empty();
		}

		@Override
		public String getString(String key, String fallback) {
			return fallback;
		}

		@Override
		public RegistryWrapper.WrapperLookup getRegistries() {
			return ReadContext.this.registries;
		}

		@Override
		public Optional<int[]> getOptionalIntArray(String key) {
			return Optional.empty();
		}
	};

	/**
	 * Создаёт контекст чтения с указанными реестрами и базовыми операциями.
	 * Операции оборачиваются через {@link RegistryWrapper.WrapperLookup#getOps},
	 * чтобы обеспечить корректное разрешение ссылок на реестровые объекты.
	 *
	 * @param registries реестры для разрешения ссылок при декодировании
	 * @param ops        базовые операции сериализации (например, {@link net.minecraft.nbt.NbtOps#INSTANCE})
	 */
	public ReadContext(RegistryWrapper.WrapperLookup registries, DynamicOps<NbtElement> ops) {
		this.registries = registries;
		this.ops = registries.getOps(ops);
	}

	public DynamicOps<NbtElement> getOps() {
		return ops;
	}

	public RegistryWrapper.WrapperLookup getRegistries() {
		return registries;
	}

	public ReadView getEmptyReadView() {
		return emptyReadView;
	}

	public ReadView.ListReadView getEmptyListReadView() {
		return emptyListReadView;
	}

	@SuppressWarnings("unchecked")
	public <T> ReadView.TypedListReadView<T> getEmptyTypedListReadView() {
		return (ReadView.TypedListReadView<T>) emptyTypedListReadView;
	}
}

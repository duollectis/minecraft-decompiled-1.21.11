package net.minecraft.entity.data;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.collection.Class2IntMap;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Хранилище синхронизируемых данных сущности.
 * Каждое поле регистрируется через {@link #registerData} и получает уникальный числовой id.
 * Изменения помечаются как «грязные» и отправляются клиенту при следующей синхронизации.
 */
public class DataTracker {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_DATA_VALUE_ID = 254;
	static final Class2IntMap CLASS_TO_LAST_ID = new Class2IntMap();

	private final DataTracked trackedEntity;
	private final DataTracker.Entry<?>[] entries;
	private boolean dirty;

	DataTracker(DataTracked trackedEntity, DataTracker.Entry<?>[] entries) {
		this.trackedEntity = trackedEntity;
		this.entries = entries;
	}

	/**
	 * Регистрирует новое отслеживаемое поле для указанного класса сущности.
	 * Метод должен вызываться строго из статического инициализатора того же класса,
	 * что передаётся в {@code entityClass}, иначе в debug-режиме будет залогировано предупреждение.
	 *
	 * @param entityClass класс сущности, которому принадлежит поле
	 * @param dataHandler обработчик сериализации типа поля
	 * @return дескриптор зарегистрированного поля
	 * @throws IllegalArgumentException если превышен лимит {@value MAX_DATA_VALUE_ID} полей
	 */
	public static <T> TrackedData<T> registerData(
			Class<? extends DataTracked> entityClass,
			TrackedDataHandler<T> dataHandler
	) {
		if (LOGGER.isDebugEnabled()) {
			try {
				Class<?> callerClass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
				if (!callerClass.equals(entityClass)) {
					LOGGER.debug(
							"defineId called for: {} from {}",
							new Object[]{entityClass, callerClass, new RuntimeException()}
					);
				}
			} catch (ClassNotFoundException ignored) {
			}
		}

		int nextId = CLASS_TO_LAST_ID.put(entityClass);
		if (nextId > MAX_DATA_VALUE_ID) {
			throw new IllegalArgumentException("Data value id is too big with " + nextId + "! (Max is 254)");
		}

		return dataHandler.create(nextId);
	}

	@SuppressWarnings("unchecked")
	private <T> DataTracker.Entry<T> getEntry(TrackedData<T> key) {
		return (DataTracker.Entry<T>) entries[key.id()];
	}

	public <T> T get(TrackedData<T> data) {
		return getEntry(data).get();
	}

	public <T> void set(TrackedData<T> key, T value) {
		set(key, value, false);
	}

	/**
	 * Устанавливает значение отслеживаемого поля.
	 * Если {@code force} равен {@code false}, обновление происходит только при изменении значения.
	 */
	public <T> void set(TrackedData<T> key, T value, boolean force) {
		DataTracker.Entry<T> entry = getEntry(key);
		if (force || ObjectUtils.notEqual(value, entry.get())) {
			entry.set(value);
			trackedEntity.onTrackedDataSet(key);
			entry.setDirty(true);
			dirty = true;
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Возвращает список изменённых с последней синхронизации записей и сбрасывает флаги dirty.
	 * Возвращает {@code null}, если изменений не было.
	 */
	public @Nullable List<DataTracker.SerializedEntry<?>> getDirtyEntries() {
		if (!dirty) {
			return null;
		}

		dirty = false;
		List<DataTracker.SerializedEntry<?>> changed = new ArrayList<>();

		for (DataTracker.Entry<?> entry : entries) {
			if (entry.isDirty()) {
				entry.setDirty(false);
				changed.add(entry.toSerialized());
			}
		}

		return changed;
	}

	/**
	 * Возвращает список всех записей, значение которых отличается от начального.
	 * Используется при первоначальной отправке данных новому клиенту.
	 */
	public @Nullable List<DataTracker.SerializedEntry<?>> getChangedEntries() {
		List<DataTracker.SerializedEntry<?>> changed = null;

		for (DataTracker.Entry<?> entry : entries) {
			if (!entry.isUnchanged()) {
				if (changed == null) {
					changed = new ArrayList<>();
				}

				changed.add(entry.toSerialized());
			}
		}

		return changed;
	}

	public void writeUpdatedEntries(List<DataTracker.SerializedEntry<?>> entries) {
		for (DataTracker.SerializedEntry<?> serializedEntry : entries) {
			DataTracker.Entry<?> entry = this.entries[serializedEntry.id];
			copyToFrom(entry, serializedEntry);
			trackedEntity.onTrackedDataSet(entry.getData());
		}

		trackedEntity.onDataTrackerUpdate(entries);
	}

	private <T> void copyToFrom(DataTracker.Entry<T> to, DataTracker.SerializedEntry<?> from) {
		if (!Objects.equals(from.handler(), to.data.dataType())) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
							to.data.id(),
							trackedEntity,
							to.value,
							to.value.getClass(),
							from.value,
							from.value.getClass()
					)
			);
		}

		to.set((T) from.value);
	}

	/**
	 * Строитель {@link DataTracker}.
	 * Все поля, зарегистрированные через {@link DataTracker#registerData}, должны быть добавлены
	 * через {@link #add} до вызова {@link #build}.
	 */
	public static class Builder {

		private final DataTracked entity;
		private final DataTracker.@Nullable Entry<?>[] entries;

		public Builder(DataTracked entity) {
			this.entity = entity;
			entries = new DataTracker.Entry[DataTracker.CLASS_TO_LAST_ID.getNext(entity.getClass())];
		}

		public <T> DataTracker.Builder add(TrackedData<T> data, T value) {
			int dataId = data.id();
			if (dataId > entries.length) {
				throw new IllegalArgumentException(
						"Data value id is too big with " + dataId + "! (Max is " + entries.length + ")");
			}

			if (entries[dataId] != null) {
				throw new IllegalArgumentException("Duplicate id value for " + dataId + "!");
			}

			if (TrackedDataHandlerRegistry.getId(data.dataType()) < 0) {
				throw new IllegalArgumentException(
						"Unregistered serializer " + data.dataType() + " for " + dataId + "!");
			}

			entries[data.id()] = new DataTracker.Entry<>(data, value);
			return this;
		}

		public DataTracker build() {
			for (int index = 0; index < entries.length; index++) {
				if (entries[index] == null) {
					throw new IllegalStateException(
							"Entity " + entity.getClass() + " has not defined synched data value " + index);
				}
			}

			return new DataTracker(entity, entries);
		}
	}

	/**
	 * Запись об одном отслеживаемом поле сущности.
	 */
	public static class Entry<T> {

		final TrackedData<T> data;
		T value;
		private final T initialValue;
		private boolean dirty;

		public Entry(TrackedData<T> data, T value) {
			this.data = data;
			initialValue = value;
			this.value = value;
		}

		public TrackedData<T> getData() {
			return data;
		}

		public void set(T value) {
			this.value = value;
		}

		public T get() {
			return value;
		}

		public boolean isDirty() {
			return dirty;
		}

		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		public boolean isUnchanged() {
			return initialValue.equals(value);
		}

		public DataTracker.SerializedEntry<T> toSerialized() {
			return DataTracker.SerializedEntry.of(data, value);
		}
	}

	/**
	 * Сериализованное представление одной записи для передачи по сети.
	 */
	public record SerializedEntry<T>(int id, TrackedDataHandler<T> handler, T value) {

		public static <T> DataTracker.SerializedEntry<T> of(TrackedData<T> data, T value) {
			TrackedDataHandler<T> handler = data.dataType();
			return new DataTracker.SerializedEntry<>(data.id(), handler, handler.copy(value));
		}

		/**
		 * Записывает запись в буфер пакета.
		 * Формат: byte(id) + varint(handler_id) + encoded_value.
		 */
		public void write(RegistryByteBuf buf) {
			int handlerId = TrackedDataHandlerRegistry.getId(handler);
			if (handlerId < 0) {
				throw new EncoderException("Unknown serializer type " + handler);
			}

			buf.writeByte(id);
			buf.writeVarInt(handlerId);
			handler.codec().encode(buf, value);
		}

		public static DataTracker.SerializedEntry<?> fromBuf(RegistryByteBuf buf, int id) {
			int handlerId = buf.readVarInt();
			TrackedDataHandler<?> handler = TrackedDataHandlerRegistry.get(handlerId);
			if (handler == null) {
				throw new DecoderException("Unknown serializer type " + handlerId);
			}

			return fromBuf(buf, id, handler);
		}

		private static <T> DataTracker.SerializedEntry<T> fromBuf(
				RegistryByteBuf buf,
				int id,
				TrackedDataHandler<T> handler
		) {
			return new DataTracker.SerializedEntry<>(id, handler, handler.codec().decode(buf));
		}
	}
}

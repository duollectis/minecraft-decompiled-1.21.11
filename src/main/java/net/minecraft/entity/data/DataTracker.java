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
 * {@code DataTracker}.
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

	public static <T> TrackedData<T> registerData(
			Class<? extends DataTracked> entityClass,
			TrackedDataHandler<T> dataHandler
	) {
		if (LOGGER.isDebugEnabled()) {
			try {
				Class<?> class_ = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
				if (!class_.equals(entityClass)) {
					LOGGER.debug(
							"defineId called for: {} from {}",
							new Object[]{entityClass, class_, new RuntimeException()}
					);
				}
			}
			catch (ClassNotFoundException var3) {
			}
		}

		int i = CLASS_TO_LAST_ID.put(entityClass);
		if (i > 254) {
			throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
		}
		else {
			return dataHandler.create(i);
		}
	}

	private <T> DataTracker.Entry<T> getEntry(TrackedData<T> key) {
		return (DataTracker.Entry<T>) this.entries[key.id()];
	}

	/**
	 * Get.
	 *
	 * @param data data
	 *
	 * @return T — 
	 */
	public <T> T get(TrackedData<T> data) {
		return this.getEntry(data).get();
	}

	/**
	 * Set.
	 *
	 * @param key key
	 * @param value value
	 *
	 * @return void — результат операции
	 */
	public <T> void set(TrackedData<T> key, T value) {
		this.set(key, value, false);
	}

	/**
	 * Set.
	 *
	 * @param key key
	 * @param value value
	 * @param force force
	 *
	 * @return void — результат операции
	 */
	public <T> void set(TrackedData<T> key, T value, boolean force) {
		DataTracker.Entry<T> entry = this.getEntry(key);
		if (force || ObjectUtils.notEqual(value, entry.get())) {
			entry.set(value);
			this.trackedEntity.onTrackedDataSet(key);
			entry.setDirty(true);
			this.dirty = true;
		}
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public @Nullable List<DataTracker.SerializedEntry<?>> getDirtyEntries() {
		if (!this.dirty) {
			return null;
		}
		else {
			this.dirty = false;
			List<DataTracker.SerializedEntry<?>> list = new ArrayList<>();

			for (DataTracker.Entry<?> entry : this.entries) {
				if (entry.isDirty()) {
					entry.setDirty(false);
					list.add(entry.toSerialized());
				}
			}

			return list;
		}
	}

	public @Nullable List<DataTracker.SerializedEntry<?>> getChangedEntries() {
		List<DataTracker.SerializedEntry<?>> list = null;

		for (DataTracker.Entry<?> entry : this.entries) {
			if (!entry.isUnchanged()) {
				if (list == null) {
					list = new ArrayList<>();
				}

				list.add(entry.toSerialized());
			}
		}

		return list;
	}

	/**
	 * Записывает updated entries.
	 *
	 * @param entries entries
	 */
	public void writeUpdatedEntries(List<DataTracker.SerializedEntry<?>> entries) {
		for (DataTracker.SerializedEntry<?> serializedEntry : entries) {
			DataTracker.Entry<?> entry = this.entries[serializedEntry.id];
			this.copyToFrom(entry, serializedEntry);
			this.trackedEntity.onTrackedDataSet(entry.getData());
		}

		this.trackedEntity.onDataTrackerUpdate(entries);
	}

	private <T> void copyToFrom(DataTracker.Entry<T> to, DataTracker.SerializedEntry<?> from) {
		if (!Objects.equals(from.handler(), to.data.dataType())) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
							to.data.id(),
							this.trackedEntity,
							to.value,
							to.value.getClass(),
							from.value,
							from.value.getClass()
					)
			);
		}
		else {
			to.set((T) from.value);
		}
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final DataTracked entity;
		private final DataTracker.@Nullable Entry<?>[] entries;

		public Builder(DataTracked entity) {
			this.entity = entity;
			this.entries = new DataTracker.Entry[DataTracker.CLASS_TO_LAST_ID.getNext(entity.getClass())];
		}

		public <T> DataTracker.Builder add(TrackedData<T> data, T value) {
			int i = data.id();
			if (i > this.entries.length) {
				throw new IllegalArgumentException(
						"Data value id is too big with " + i + "! (Max is " + this.entries.length + ")");
			}
			else if (this.entries[i] != null) {
				throw new IllegalArgumentException("Duplicate id value for " + i + "!");
			}
			else if (TrackedDataHandlerRegistry.getId(data.dataType()) < 0) {
				throw new IllegalArgumentException("Unregistered serializer " + data.dataType() + " for " + i + "!");
			}
			else {
				this.entries[data.id()] = new DataTracker.Entry<>(data, value);
				return this;
			}
		}

		/**
		 * Build.
		 *
		 * @return DataTracker — результат операции
		 */
		public DataTracker build() {
			for (int i = 0; i < this.entries.length; i++) {
				if (this.entries[i] == null) {
					throw new IllegalStateException(
							"Entity " + this.entity.getClass() + " has not defined synched data value " + i);
				}
			}

			return new DataTracker(this.entity, this.entries);
		}
	}

	/**
	 * {@code Entry}.
	 */
	public static class Entry<T> {

		final TrackedData<T> data;
		T value;
		private final T initialValue;
		private boolean dirty;

		public Entry(TrackedData<T> data, T value) {
			this.data = data;
			this.initialValue = value;
			this.value = value;
		}

		public TrackedData<T> getData() {
			return this.data;
		}

		/**
		 * Set.
		 *
		 * @param value value
		 */
		public void set(T value) {
			this.value = value;
		}

		/**
		 * Get.
		 *
		 * @return T — 
		 */
		public T get() {
			return this.value;
		}

		public boolean isDirty() {
			return this.dirty;
		}

		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		public boolean isUnchanged() {
			return this.initialValue.equals(this.value);
		}

		public DataTracker.SerializedEntry<T> toSerialized() {
			return DataTracker.SerializedEntry.of(this.data, this.value);
		}
	}

	/**
	 * {@code SerializedEntry}.
	 */
	public record SerializedEntry<T>(int id, TrackedDataHandler<T> handler, T value) {

		public static <T> DataTracker.SerializedEntry<T> of(TrackedData<T> data, T value) {
			TrackedDataHandler<T> trackedDataHandler = data.dataType();
			return new DataTracker.SerializedEntry<>(data.id(), trackedDataHandler, trackedDataHandler.copy(value));
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(RegistryByteBuf buf) {
			int i = TrackedDataHandlerRegistry.getId(this.handler);
			if (i < 0) {
				throw new EncoderException("Unknown serializer type " + this.handler);
			}
			else {
				buf.writeByte(this.id);
				buf.writeVarInt(i);
				this.handler.codec().encode(buf, this.value);
			}
		}

		public static DataTracker.SerializedEntry<?> fromBuf(RegistryByteBuf buf, int id) {
			int i = buf.readVarInt();
			TrackedDataHandler<?> trackedDataHandler = TrackedDataHandlerRegistry.get(i);
			if (trackedDataHandler == null) {
				throw new DecoderException("Unknown serializer type " + i);
			}
			else {
				return fromBuf(buf, id, trackedDataHandler);
			}
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

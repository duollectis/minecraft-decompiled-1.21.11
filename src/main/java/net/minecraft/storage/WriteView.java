package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.serialization.v1.view.FabricWriteView;
import org.jspecify.annotations.Nullable;

/**
 * {@code WriteView}.
 */
public interface WriteView extends FabricWriteView {

	<T> void put(String key, Codec<T> codec, T value);

	<T> void putNullable(String key, Codec<T> codec, @Nullable T value);

	@Deprecated
	<T> void put(MapCodec<T> codec, T value);

	void putBoolean(String key, boolean value);

	void putByte(String key, byte value);

	void putShort(String key, short value);

	void putInt(String key, int value);

	void putLong(String key, long value);

	void putFloat(String key, float value);

	void putDouble(String key, double value);

	void putString(String key, String value);

	void putIntArray(String key, int[] value);

	WriteView get(String key);

	WriteView.ListView getList(String key);

	<T> WriteView.ListAppender<T> getListAppender(String key, Codec<T> codec);

	void remove(String key);

	boolean isEmpty();

	/**
	 * {@code ListAppender}.
	 */
	public interface ListAppender<T> {

		void add(T value);

		boolean isEmpty();
	}

	/**
	 * {@code ListView}.
	 */
	public interface ListView {

		WriteView add();

		void removeLast();

		boolean isEmpty();
	}
}

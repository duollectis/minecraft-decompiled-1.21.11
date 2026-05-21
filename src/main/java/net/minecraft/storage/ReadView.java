package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.serialization.v1.view.FabricReadView;
import net.minecraft.registry.RegistryWrapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@code ReadView}.
 */
public interface ReadView extends FabricReadView {

	<T> Optional<T> read(String key, Codec<T> codec);

	@Deprecated
	<T> Optional<T> read(MapCodec<T> mapCodec);

	Optional<ReadView> getOptionalReadView(String key);

	ReadView getReadView(String key);

	Optional<ReadView.ListReadView> getOptionalListReadView(String key);

	ReadView.ListReadView getListReadView(String key);

	<T> Optional<ReadView.TypedListReadView<T>> getOptionalTypedListView(String key, Codec<T> typeCodec);

	<T> ReadView.TypedListReadView<T> getTypedListView(String key, Codec<T> typeCodec);

	boolean getBoolean(String key, boolean fallback);

	byte getByte(String key, byte fallback);

	int getShort(String key, short fallback);

	Optional<Integer> getOptionalInt(String key);

	int getInt(String key, int fallback);

	long getLong(String key, long fallback);

	Optional<Long> getOptionalLong(String key);

	float getFloat(String key, float fallback);

	double getDouble(String key, double fallback);

	Optional<String> getOptionalString(String key);

	String getString(String key, String fallback);

	Optional<int[]> getOptionalIntArray(String key);

	@Deprecated
	RegistryWrapper.WrapperLookup getRegistries();

	/**
	 * {@code ListReadView}.
	 */
	public interface ListReadView extends Iterable<ReadView> {

		boolean isEmpty();

		Stream<ReadView> stream();
	}

	/**
	 * {@code TypedListReadView}.
	 */
	public interface TypedListReadView<T> extends Iterable<T> {

		boolean isEmpty();

		Stream<T> stream();
	}
}

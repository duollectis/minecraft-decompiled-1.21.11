package net.minecraft.world.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.minecraft.util.FixedBufferInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * {@code ChunkCompressionFormat}.
 */
public class ChunkCompressionFormat {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Int2ObjectMap<ChunkCompressionFormat> FORMATS = new Int2ObjectOpenHashMap();
	private static final Object2ObjectMap<String, ChunkCompressionFormat>
			FORMAT_BY_NAME =
			new Object2ObjectOpenHashMap();
	public static final ChunkCompressionFormat GZIP = add(
			new ChunkCompressionFormat(
					1,
					null,
					stream -> new FixedBufferInputStream(new GZIPInputStream(stream)),
					stream -> new BufferedOutputStream(new GZIPOutputStream(stream))
			)
	);
	public static final ChunkCompressionFormat DEFLATE = add(
			new ChunkCompressionFormat(
					2,
					"deflate",
					stream -> new FixedBufferInputStream(new InflaterInputStream(stream)),
					stream -> new BufferedOutputStream(new DeflaterOutputStream(stream))
			)
	);
	public static final ChunkCompressionFormat
			UNCOMPRESSED =
			add(new ChunkCompressionFormat(3, "none", FixedBufferInputStream::new, BufferedOutputStream::new));
	public static final ChunkCompressionFormat LZ4 = add(
			new ChunkCompressionFormat(
					4,
					"lz4",
					stream -> new FixedBufferInputStream(new LZ4BlockInputStream(stream)),
					stream -> new BufferedOutputStream(new LZ4BlockOutputStream(stream))
			)
	);
	public static final ChunkCompressionFormat CUSTOM = add(new ChunkCompressionFormat(
			127, null, stream -> {
		throw new UnsupportedOperationException();
	}, stream -> {
		throw new UnsupportedOperationException();
	}
	));
	public static final ChunkCompressionFormat DEFAULT_FORMAT = DEFLATE;
	private static volatile ChunkCompressionFormat currentFormat = DEFAULT_FORMAT;
	private final int id;
	private final @Nullable String name;
	private final ChunkCompressionFormat.Wrapper<InputStream> inputStreamWrapper;
	private final ChunkCompressionFormat.Wrapper<OutputStream> outputStreamWrapper;

	private ChunkCompressionFormat(
			int id,
			@Nullable String name,
			ChunkCompressionFormat.Wrapper<InputStream> inputStreamWrapper,
			ChunkCompressionFormat.Wrapper<OutputStream> outputStreamWrapper
	) {
		this.id = id;
		this.name = name;
		this.inputStreamWrapper = inputStreamWrapper;
		this.outputStreamWrapper = outputStreamWrapper;
	}

	private static ChunkCompressionFormat add(ChunkCompressionFormat version) {
		FORMATS.put(version.id, version);
		if (version.name != null) {
			FORMAT_BY_NAME.put(version.name, version);
		}

		return version;
	}

	/**
	 * Get.
	 *
	 * @param id id
	 *
	 * @return @Nullable ChunkCompressionFormat — 
	 */
	public static @Nullable ChunkCompressionFormat get(int id) {
		return (ChunkCompressionFormat) FORMATS.get(id);
	}

	public static void setCurrentFormat(String name) {
		ChunkCompressionFormat chunkCompressionFormat = (ChunkCompressionFormat) FORMAT_BY_NAME.get(name);
		if (chunkCompressionFormat != null) {
			currentFormat = chunkCompressionFormat;
		}
		else {
			LOGGER.error(
					"Invalid `region-file-compression` value `{}` in server.properties. Please use one of: {}",
					name,
					String.join(", ", FORMAT_BY_NAME.keySet())
			);
		}
	}

	public static ChunkCompressionFormat getCurrentFormat() {
		return currentFormat;
	}

	/**
	 * Exists.
	 *
	 * @param id id
	 *
	 * @return boolean — результат операции
	 */
	public static boolean exists(int id) {
		return FORMATS.containsKey(id);
	}

	public int getId() {
		return this.id;
	}

	/**
	 * Wrap.
	 *
	 * @param outputStream output stream
	 *
	 * @return OutputStream — результат операции
	 */
	public OutputStream wrap(OutputStream outputStream) throws IOException {
		return this.outputStreamWrapper.wrap(outputStream);
	}

	/**
	 * Wrap.
	 *
	 * @param inputStream input stream
	 *
	 * @return InputStream — результат операции
	 */
	public InputStream wrap(InputStream inputStream) throws IOException {
		return this.inputStreamWrapper.wrap(inputStream);
	}

	@FunctionalInterface
	/**
	 * {@code Wrapper}.
	 */
	interface Wrapper<O> {

		O wrap(O object) throws IOException;
	}
}

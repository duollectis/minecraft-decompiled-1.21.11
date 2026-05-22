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
 * Реестр форматов сжатия данных чанков в файлах региона (.mca).
 * Каждый формат идентифицируется числовым ID, хранящимся в заголовке сектора.
 * Текущий формат по умолчанию — {@link #DEFLATE}; может быть переопределён
 * через {@link #setCurrentFormat(String)} из server.properties.
 */
public class ChunkCompressionFormat {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Int2ObjectMap<ChunkCompressionFormat> FORMATS = new Int2ObjectOpenHashMap<>();
	private static final Object2ObjectMap<String, ChunkCompressionFormat> FORMAT_BY_NAME = new Object2ObjectOpenHashMap<>();

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
	public static final ChunkCompressionFormat UNCOMPRESSED = add(
		new ChunkCompressionFormat(3, "none", FixedBufferInputStream::new, BufferedOutputStream::new)
	);
	public static final ChunkCompressionFormat LZ4 = add(
		new ChunkCompressionFormat(
			4,
			"lz4",
			stream -> new FixedBufferInputStream(new LZ4BlockInputStream(stream)),
			stream -> new BufferedOutputStream(new LZ4BlockOutputStream(stream))
		)
	);
	// Формат-заглушка для нераспознанных пользовательских алгоритмов сжатия
	public static final ChunkCompressionFormat CUSTOM = add(
		new ChunkCompressionFormat(
			127,
			null,
			stream -> { throw new UnsupportedOperationException(); },
			stream -> { throw new UnsupportedOperationException(); }
		)
	);

	public static final ChunkCompressionFormat DEFAULT_FORMAT = DEFLATE;
	private static volatile ChunkCompressionFormat currentFormat = DEFAULT_FORMAT;

	private final int id;
	private final @Nullable String name;
	private final Wrapper<InputStream> inputStreamWrapper;
	private final Wrapper<OutputStream> outputStreamWrapper;

	private ChunkCompressionFormat(
		int id,
		@Nullable String name,
		Wrapper<InputStream> inputStreamWrapper,
		Wrapper<OutputStream> outputStreamWrapper
	) {
		this.id = id;
		this.name = name;
		this.inputStreamWrapper = inputStreamWrapper;
		this.outputStreamWrapper = outputStreamWrapper;
	}

	private static ChunkCompressionFormat add(ChunkCompressionFormat format) {
		FORMATS.put(format.id, format);

		if (format.name != null) {
			FORMAT_BY_NAME.put(format.name, format);
		}

		return format;
	}

	public static @Nullable ChunkCompressionFormat get(int id) {
		return FORMATS.get(id);
	}

	/**
	 * Устанавливает текущий формат сжатия по имени из server.properties.
	 * Если имя не распознано — логирует ошибку и оставляет предыдущий формат.
	 *
	 * @param name имя формата (например, "deflate", "lz4", "none")
	 */
	public static void setCurrentFormat(String name) {
		ChunkCompressionFormat format = FORMAT_BY_NAME.get(name);

		if (format == null) {
			LOGGER.error(
				"Invalid `region-file-compression` value `{}` in server.properties. Please use one of: {}",
				name,
				String.join(", ", FORMAT_BY_NAME.keySet())
			);
			return;
		}

		currentFormat = format;
	}

	public static ChunkCompressionFormat getCurrentFormat() {
		return currentFormat;
	}

	public static boolean exists(int id) {
		return FORMATS.containsKey(id);
	}

	public int getId() {
		return id;
	}

	public OutputStream wrap(OutputStream outputStream) throws IOException {
		return outputStreamWrapper.wrap(outputStream);
	}

	public InputStream wrap(InputStream inputStream) throws IOException {
		return inputStreamWrapper.wrap(inputStream);
	}

	@FunctionalInterface
	interface Wrapper<O> {

		O wrap(O object) throws IOException;
	}
}

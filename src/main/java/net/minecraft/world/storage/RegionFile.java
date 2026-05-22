package net.minecraft.world.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Представляет один файл региона (.mca), содержащий данные до 1024 чанков.
 *
 * <p>Формат файла:
 * <ul>
 *   <li>Байты 0–4095: таблица смещений секторов (по 4 байта на чанк, 1024 записи)</li>
 *   <li>Байты 4096–8191: таблица временных меток последнего сохранения</li>
 *   <li>Байты 8192+: данные чанков, выровненные по секторам ({@value #SECTOR_SIZE} байт)</li>
 * </ul>
 *
 * <p>Чанки, превышающие {@value #HEADER_SIZE} секторов, сохраняются во внешние файлы (.mcc).
 */
public class RegionFile implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int SECTOR_SIZE = 4096;
	private static final int HEADER_SECTORS = 2;
	private static final int HEADER_BYTE_SIZE = SECTOR_SIZE * HEADER_SECTORS;
	private static final int CHUNK_HEADER_BYTE_SIZE = 5;
	private static final int EXTERNAL_CHUNK_FLAG = 128;
	private static final int EXTERNAL_FLAG_MASK = -129;
	private static final String EXTERNAL_CHUNK_EXTENSION = ".mcc";
	private static final ByteBuffer ZERO = ByteBuffer.allocateDirect(1);

	@VisibleForTesting
	protected static final int SECTOR_DATA_LIMIT = 1024;
	/** Максимальное число секторов для одного чанка; при превышении — внешний файл. */
	private static final int HEADER_SIZE = 256;

	final StorageKey storageKey;
	private final Path path;
	private final FileChannel channel;
	private final Path directory;
	final ChunkCompressionFormat compressionFormat;
	private final ByteBuffer header = ByteBuffer.allocateDirect(HEADER_BYTE_SIZE);
	private final IntBuffer sectorData;
	private final IntBuffer saveTimes;

	@VisibleForTesting
	protected final SectorMap sectors = new SectorMap();

	public RegionFile(StorageKey storageKey, Path directory, Path path, boolean dsync) throws IOException {
		this(storageKey, directory, path, ChunkCompressionFormat.getCurrentFormat(), dsync);
	}

	/**
	 * Открывает или создаёт файл региона, читает заголовок и восстанавливает карту секторов.
	 * При обнаружении повреждённых записей в заголовке — обнуляет их и логирует предупреждение.
	 *
	 * @param storageKey ключ хранилища для профилировщика
	 * @param path путь к файлу .mca
	 * @param directory директория файла (для внешних .mcc чанков)
	 * @param compressionFormat формат сжатия данных чанков
	 * @param dsync использовать O_DSYNC для гарантии записи на диск
	 */
	public RegionFile(
		StorageKey storageKey,
		Path path,
		Path directory,
		ChunkCompressionFormat compressionFormat,
		boolean dsync
	) throws IOException {
		this.storageKey = storageKey;
		this.path = path;
		this.compressionFormat = compressionFormat;

		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("Expected directory, got " + directory.toAbsolutePath());
		}

		this.directory = directory;
		this.sectorData = header.asIntBuffer();
		this.sectorData.limit(SECTOR_DATA_LIMIT);
		this.header.position(SECTOR_SIZE);
		this.saveTimes = header.asIntBuffer();

		this.channel = dsync
			? FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC)
			: FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);

		// Первые два сектора всегда заняты заголовком
		sectors.allocate(0, HEADER_SECTORS);
		header.position(0);

		int bytesRead = channel.read(header, 0L);

		if (bytesRead == -1) {
			return;
		}

		if (bytesRead != HEADER_BYTE_SIZE) {
			LOGGER.warn("Region file {} has truncated header: {}", path, bytesRead);
		}

		long fileSize = Files.size(path);

		for (int index = 0; index < SECTOR_DATA_LIMIT; index++) {
			int packed = sectorData.get(index);

			if (packed == 0) {
				continue;
			}

			int sectorOffset = getOffset(packed);
			int sectorCount = getSize(packed);

			if (sectorOffset < HEADER_SECTORS) {
				LOGGER.warn(
					"Region file {} has invalid sector at index: {}; sector {} overlaps with header",
					new Object[]{path, index, sectorOffset}
				);
				sectorData.put(index, 0);
			}
			else if (sectorCount == 0) {
				LOGGER.warn(
					"Region file {} has an invalid sector at index: {}; size has to be > 0",
					path,
					index
				);
				sectorData.put(index, 0);
			}
			else if ((long) sectorOffset * SECTOR_SIZE > fileSize) {
				LOGGER.warn(
					"Region file {} has an invalid sector at index: {}; sector {} is out of bounds",
					new Object[]{path, index, sectorOffset}
				);
				sectorData.put(index, 0);
			}
			else {
				sectors.allocate(sectorOffset, sectorCount);
			}
		}
	}

	public Path getPath() {
		return path;
	}

	private Path getExternalChunkPath(ChunkPos chunkPos) {
		return directory.resolve("c." + chunkPos.x + "." + chunkPos.z + EXTERNAL_CHUNK_EXTENSION);
	}

	/**
	 * Открывает поток чтения для данных чанка.
	 * Возвращает {@code null}, если чанк не существует или повреждён.
	 */
	public synchronized @Nullable DataInputStream getChunkInputStream(ChunkPos pos) throws IOException {
		int packed = getSectorData(pos);

		if (packed == 0) {
			return null;
		}

		int sectorOffset = getOffset(packed);
		int sectorCount = getSize(packed);
		int totalBytes = sectorCount * SECTOR_SIZE;
		ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
		channel.read(buffer, (long) sectorOffset * SECTOR_SIZE);
		buffer.flip();

		if (buffer.remaining() < CHUNK_HEADER_BYTE_SIZE) {
			LOGGER.error(
				"Chunk {} header is truncated: expected {} but read {}",
				new Object[]{pos, totalBytes, buffer.remaining()}
			);
			return null;
		}

		int dataLength = buffer.getInt();
		byte flags = buffer.get();

		if (dataLength == 0) {
			LOGGER.warn("Chunk {} is allocated, but stream is missing", pos);
			return null;
		}

		if (hasChunkStreamVersionId(flags)) {
			if (dataLength != 1) {
				LOGGER.warn("Chunk has both internal and external streams");
			}

			return getInputStream(pos, getChunkStreamVersionId(flags));
		}

		int payloadLength = dataLength - 1;

		if (payloadLength > buffer.remaining()) {
			LOGGER.error(
				"Chunk {} stream is truncated: expected {} but read {}",
				new Object[]{pos, payloadLength, buffer.remaining()}
			);
			return null;
		}

		if (payloadLength < 0) {
			LOGGER.error("Declared size {} of chunk {} is negative", dataLength, pos);
			return null;
		}

		FlightProfiler.INSTANCE.onChunkRegionRead(storageKey, pos, compressionFormat, payloadLength);
		return decompress(pos, flags, getInputStream(buffer, payloadLength));
	}

	private static int getEpochTimeSeconds() {
		return (int) (Util.getEpochTimeMs() / 1000L);
	}

	private static boolean hasChunkStreamVersionId(byte flags) {
		return (flags & EXTERNAL_CHUNK_FLAG) != 0;
	}

	private static byte getChunkStreamVersionId(byte flags) {
		return (byte) (flags & EXTERNAL_FLAG_MASK);
	}

	private @Nullable DataInputStream decompress(ChunkPos pos, byte flags, InputStream stream) throws IOException {
		ChunkCompressionFormat format = ChunkCompressionFormat.get(flags);

		if (format == ChunkCompressionFormat.CUSTOM) {
			String compressionId = new DataInputStream(stream).readUTF();
			Identifier identifier = Identifier.tryParse(compressionId);

			if (identifier != null) {
				LOGGER.error("Unrecognized custom compression {}", identifier);
			}
			else {
				LOGGER.error("Invalid custom compression id {}", compressionId);
			}

			return null;
		}

		if (format == null) {
			LOGGER.error("Chunk {} has invalid chunk stream version {}", pos, flags);
			return null;
		}

		return new DataInputStream(format.wrap(stream));
	}

	private @Nullable DataInputStream getInputStream(ChunkPos pos, byte flags) throws IOException {
		Path externalPath = getExternalChunkPath(pos);

		if (!Files.isRegularFile(externalPath)) {
			LOGGER.error("External chunk path {} is not file", externalPath);
			return null;
		}

		return decompress(pos, flags, Files.newInputStream(externalPath));
	}

	private static ByteArrayInputStream getInputStream(ByteBuffer buffer, int length) {
		return new ByteArrayInputStream(buffer.array(), buffer.position(), length);
	}

	private int packSectorData(int offset, int size) {
		return offset << 8 | size;
	}

	private static int getSize(int packed) {
		return packed & 0xFF;
	}

	private static int getOffset(int packed) {
		return packed >> 8 & 0xFFFFFF;
	}

	private static int getSectorCount(int byteCount) {
		return (byteCount + SECTOR_SIZE - 1) / SECTOR_SIZE;
	}

	public boolean isChunkValid(ChunkPos pos) {
		int packed = getSectorData(pos);

		if (packed == 0) {
			return false;
		}

		int sectorOffset = getOffset(packed);
		int sectorCount = getSize(packed);
		ByteBuffer buffer = ByteBuffer.allocate(CHUNK_HEADER_BYTE_SIZE);

		try {
			channel.read(buffer, (long) sectorOffset * SECTOR_SIZE);
			buffer.flip();

			if (buffer.remaining() != CHUNK_HEADER_BYTE_SIZE) {
				return false;
			}

			int dataLength = buffer.getInt();
			byte flags = buffer.get();

			if (hasChunkStreamVersionId(flags)) {
				return ChunkCompressionFormat.exists(getChunkStreamVersionId(flags))
					&& Files.isRegularFile(getExternalChunkPath(pos));
			}

			if (!ChunkCompressionFormat.exists(flags)) {
				return false;
			}

			if (dataLength == 0) {
				return false;
			}

			int payloadLength = dataLength - 1;
			return payloadLength >= 0 && payloadLength <= SECTOR_SIZE * sectorCount;
		}
		catch (IOException exception) {
			return false;
		}
	}

	public DataOutputStream getChunkOutputStream(ChunkPos pos) throws IOException {
		return new DataOutputStream(compressionFormat.wrap(new ChunkBuffer(pos)));
	}

	public void sync() throws IOException {
		channel.force(true);
	}

	public void delete(ChunkPos pos) throws IOException {
		int index = getIndex(pos);
		int packed = sectorData.get(index);

		if (packed == 0) {
			return;
		}

		sectorData.put(index, 0);
		saveTimes.put(index, getEpochTimeSeconds());
		writeHeader();
		Files.deleteIfExists(getExternalChunkPath(pos));
		sectors.free(getOffset(packed), getSize(packed));
	}

	/**
	 * Записывает данные чанка в файл региона.
	 * Если размер превышает {@value #HEADER_SIZE} секторов — сохраняет во внешний .mcc файл.
	 *
	 * @param pos позиция чанка
	 * @param buf буфер с данными (включая 5-байтный заголовок)
	 */
	protected synchronized void writeChunk(ChunkPos pos, ByteBuffer buf) throws IOException {
		int index = getIndex(pos);
		int oldPacked = sectorData.get(index);
		int oldOffset = getOffset(oldPacked);
		int oldSize = getSize(oldPacked);
		int dataSize = buf.remaining();
		int requiredSectors = getSectorCount(dataSize);

		int newOffset;
		OutputAction postWriteAction;

		if (requiredSectors >= HEADER_SIZE) {
			Path externalPath = getExternalChunkPath(pos);
			LOGGER.warn(
				"Saving oversized chunk {} ({} bytes} to external file {}",
				new Object[]{pos, dataSize, externalPath}
			);
			requiredSectors = 1;
			newOffset = sectors.allocate(requiredSectors);
			postWriteAction = writeSafely(externalPath, buf);
			ByteBuffer headerBuf = getHeaderBuf();
			channel.write(headerBuf, (long) newOffset * SECTOR_SIZE);
		}
		else {
			newOffset = sectors.allocate(requiredSectors);
			postWriteAction = () -> Files.deleteIfExists(getExternalChunkPath(pos));
			channel.write(buf, (long) newOffset * SECTOR_SIZE);
		}

		sectorData.put(index, packSectorData(newOffset, requiredSectors));
		saveTimes.put(index, getEpochTimeSeconds());
		writeHeader();
		postWriteAction.run();

		if (oldOffset != 0) {
			sectors.free(oldOffset, oldSize);
		}
	}

	private ByteBuffer getHeaderBuf() {
		ByteBuffer buf = ByteBuffer.allocate(CHUNK_HEADER_BYTE_SIZE);
		buf.putInt(1);
		buf.put((byte) (compressionFormat.getId() | EXTERNAL_CHUNK_FLAG));
		buf.flip();
		return buf;
	}

	private OutputAction writeSafely(Path path, ByteBuffer buf) throws IOException {
		Path tempFile = Files.createTempFile(directory, "tmp", null);

		try (FileChannel tempChannel = FileChannel.open(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			buf.position(CHUNK_HEADER_BYTE_SIZE);
			tempChannel.write(buf);
		}

		return () -> Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
	}

	private void writeHeader() throws IOException {
		header.position(0);
		channel.write(header, 0L);
	}

	private int getSectorData(ChunkPos pos) {
		return sectorData.get(getIndex(pos));
	}

	public boolean hasChunk(ChunkPos pos) {
		return getSectorData(pos) != 0;
	}

	private static int getIndex(ChunkPos pos) {
		return pos.getRegionRelativeX() + pos.getRegionRelativeZ() * 32;
	}

	@Override
	public void close() throws IOException {
		try {
			fillLastSector();
		}
		finally {
			try {
				channel.force(true);
			}
			finally {
				channel.close();
			}
		}
	}

	private void fillLastSector() throws IOException {
		int currentSize = (int) channel.size();
		int alignedSize = getSectorCount(currentSize) * SECTOR_SIZE;

		if (currentSize == alignedSize) {
			return;
		}

		ByteBuffer zeroBuf = ZERO.duplicate();
		zeroBuf.position(0);
		channel.write(zeroBuf, alignedSize - 1);
	}

	/**
	 * Буфер записи чанка. При закрытии автоматически записывает данные в файл региона.
	 * Первые 5 байт зарезервированы под заголовок (4 байта длины + 1 байт формата сжатия).
	 */
	class ChunkBuffer extends ByteArrayOutputStream {

		private final ChunkPos pos;

		public ChunkBuffer(ChunkPos pos) {
			super(8096);
			// Резервируем место под заголовок: 4 байта длины + 1 байт ID формата сжатия
			super.write(0);
			super.write(0);
			super.write(0);
			super.write(0);
			super.write(RegionFile.this.compressionFormat.getId());
			this.pos = pos;
		}

		@Override
		public void close() throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.wrap(buf, 0, count);
			// Длина = (count - 5 заголовочных байт) + 1 байт ID формата
			int payloadLength = count - CHUNK_HEADER_BYTE_SIZE + 1;
			FlightProfiler.INSTANCE.onChunkRegionWrite(
				RegionFile.this.storageKey,
				pos,
				RegionFile.this.compressionFormat,
				payloadLength
			);
			byteBuffer.putInt(0, payloadLength);
			RegionFile.this.writeChunk(pos, byteBuffer);
		}
	}

	interface OutputAction {

		void run() throws IOException;
	}
}

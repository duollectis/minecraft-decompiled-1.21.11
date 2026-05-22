package net.minecraft.util.logging;

import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Управляет директорией лог-файлов: создаёт новые файлы, сжимает старые
 * и удаляет устаревшие по истечении срока хранения.
 */
public class LogFileCompressor {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final int COMPRESSION_BUFFER_SIZE = 4096;
	private static final String GZ_EXTENSION = ".gz";

	private final Path directory;
	private final String extension;

	private LogFileCompressor(Path directory, String extension) {
		this.directory = directory;
		this.extension = extension;
	}

	public static LogFileCompressor create(Path directory, String extension) throws IOException {
		Files.createDirectories(directory);
		return new LogFileCompressor(directory, extension);
	}

	public LogFileIterable getAll() throws IOException {
		try (Stream<Path> stream = Files.list(directory)) {
			return new LogFileIterable(
					stream.filter(Files::isRegularFile)
							.map(this::get)
							.filter(Objects::nonNull)
							.toList()
			);
		}
	}

	private @Nullable LogFile get(Path path) {
		String fileName = path.getFileName().toString();
		int dotIndex = fileName.indexOf('.');
		if (dotIndex == -1) {
			return null;
		}

		LogId logId = LogId.fromFileName(fileName.substring(0, dotIndex));
		if (logId == null) {
			return null;
		}

		String suffix = fileName.substring(dotIndex);
		if (suffix.equals(extension)) {
			return new Uncompressed(path, logId);
		}

		if (suffix.equals(extension + GZ_EXTENSION)) {
			return new Compressed(path, logId);
		}

		return null;
	}

	static void compress(Path from, Path to) throws IOException {
		if (Files.exists(to)) {
			throw new IOException("Compressed target file already exists: " + to);
		}

		try (FileChannel fileChannel = FileChannel.open(from, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			FileLock lock = fileChannel.tryLock();
			if (lock == null) {
				throw new IOException("Raw log file is already locked, cannot compress: " + from);
			}

			compress(fileChannel, to);
			fileChannel.truncate(0L);
		}

		Files.delete(from);
	}

	private static void compress(ReadableByteChannel source, Path outputPath) throws IOException {
		try (OutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(outputPath))) {
			byte[] buffer = new byte[COMPRESSION_BUFFER_SIZE];
			ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

			while (source.read(byteBuffer) >= 0) {
				byteBuffer.flip();
				outputStream.write(buffer, 0, byteBuffer.limit());
				byteBuffer.clear();
			}
		}
	}

	public Uncompressed createLogFile(LocalDate date) throws IOException {
		Set<LogId> existingIds = getAll().toIdSet();
		int index = 1;
		LogId logId;

		do {
			logId = new LogId(date, index++);
		} while (existingIds.contains(logId));

		Uncompressed uncompressed = new Uncompressed(directory.resolve(logId.getFileName(extension)), logId);
		Files.createFile(uncompressed.path());
		return uncompressed;
	}

	/**
	 * Сжатый лог-файл в формате GZIP.
	 */
	public record Compressed(Path path, LogId id) implements LogFile {

		@Override
		public @Nullable Reader getReader() throws IOException {
			return Files.exists(path)
					? new BufferedReader(new InputStreamReader(
							new GZIPInputStream(Files.newInputStream(path)),
							StandardCharsets.UTF_8
					))
					: null;
		}

		@Override
		public Compressed compress() {
			return this;
		}
	}

	/**
	 * Контракт лог-файла: предоставляет путь, идентификатор, читатель и возможность сжатия.
	 */
	public interface LogFile {

		Path path();

		LogId id();

		@Nullable Reader getReader() throws IOException;

		Compressed compress() throws IOException;
	}

	/**
	 * Коллекция лог-файлов с операциями массовой обработки:
	 * удаление устаревших и сжатие несжатых.
	 */
	public static class LogFileIterable implements Iterable<LogFile> {

		private final List<LogFile> logs;

		LogFileIterable(List<LogFile> logs) {
			this.logs = new ArrayList<>(logs);
		}

		public LogFileIterable removeExpired(LocalDate currentDate, int retentionDays) {
			logs.removeIf(log -> {
				LocalDate expiryDate = log.id().date().plusDays(retentionDays);
				if (currentDate.isBefore(expiryDate)) {
					return false;
				}

				try {
					Files.delete(log.path());
					return true;
				} catch (IOException e) {
					LOGGER.warn("Failed to delete expired event log file: {}", log.path(), e);
					return false;
				}
			});
			return this;
		}

		public LogFileIterable compressAll() {
			ListIterator<LogFile> iterator = logs.listIterator();

			while (iterator.hasNext()) {
				LogFile logFile = iterator.next();
				try {
					iterator.set(logFile.compress());
				} catch (IOException e) {
					LOGGER.warn("Failed to compress event log file: {}", logFile.path(), e);
				}
			}

			return this;
		}

		@Override
		public Iterator<LogFile> iterator() {
			return logs.iterator();
		}

		public Stream<LogFile> stream() {
			return logs.stream();
		}

		public Set<LogId> toIdSet() {
			return logs.stream().map(LogFile::id).collect(Collectors.toSet());
		}
	}

	/**
	 * Идентификатор лог-файла: дата создания и порядковый номер в рамках дня.
	 * Формат имени файла: {@code YYYYMMDD-N}, например {@code 20240115-1}.
	 */
	public record LogId(LocalDate date, int index) {

		private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

		public static @Nullable LogId fromFileName(String fileName) {
			int dashIndex = fileName.indexOf('-');
			if (dashIndex == -1) {
				return null;
			}

			String datePart = fileName.substring(0, dashIndex);
			String indexPart = fileName.substring(dashIndex + 1);

			try {
				return new LogId(LocalDate.parse(datePart, DATE_FORMATTER), Integer.parseInt(indexPart));
			} catch (DateTimeParseException | NumberFormatException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return DATE_FORMATTER.format(date) + "-" + index;
		}

		public String getFileName(String ext) {
			return this + ext;
		}
	}

	/**
	 * Несжатый лог-файл, доступный для записи и последующего сжатия.
	 */
	public record Uncompressed(Path path, LogId id) implements LogFile {

		public FileChannel open() throws IOException {
			return FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
		}

		@Override
		public @Nullable Reader getReader() throws IOException {
			return Files.exists(path) ? Files.newBufferedReader(path) : null;
		}

		@Override
		public Compressed compress() throws IOException {
			Path compressedPath = path.resolveSibling(path.getFileName().toString() + GZ_EXTENSION);
			LogFileCompressor.compress(path, compressedPath);
			return new Compressed(compressedPath, id);
		}
	}
}

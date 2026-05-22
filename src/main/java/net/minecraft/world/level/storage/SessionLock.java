package net.minecraft.world.level.storage;

import net.minecraft.util.path.PathUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Эксклюзивная блокировка директории мира через файл {@code session.lock}.
 * Предотвращает одновременное открытие одного мира несколькими экземплярами игры.
 *
 * <p>При создании записывает символ снеговика (☃) в файл блокировки и захватывает
 * {@link FileLock}. Реализует {@link AutoCloseable} для использования в try-with-resources.
 */
public class SessionLock implements AutoCloseable {

	public static final String SESSION_LOCK_FILE = "session.lock";

	private static final ByteBuffer SNOWMAN;

	static {
		byte[] snowmanBytes = "☃".getBytes(StandardCharsets.UTF_8);
		SNOWMAN = ByteBuffer.allocateDirect(snowmanBytes.length);
		SNOWMAN.put(snowmanBytes);
		SNOWMAN.flip();
	}

	private final FileChannel channel;
	private final FileLock lock;

	private SessionLock(FileChannel channel, FileLock lock) {
		this.channel = channel;
		this.lock = lock;
	}

	/**
	 * Создаёт и захватывает блокировку для указанной директории мира.
	 * Если файл уже заблокирован другим процессом — выбрасывает {@link AlreadyLockedException}.
	 *
	 * @param path директория мира
	 * @return активная блокировка
	 * @throws IOException если не удалось создать или заблокировать файл
	 */
	public static SessionLock create(Path path) throws IOException {
		Path lockFile = path.resolve(SESSION_LOCK_FILE);
		PathUtil.createDirectories(path);

		FileChannel fileChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		try {
			fileChannel.write(SNOWMAN.duplicate());
			fileChannel.force(true);

			FileLock fileLock = fileChannel.tryLock();
			if (fileLock == null) {
				throw AlreadyLockedException.create(lockFile);
			}

			return new SessionLock(fileChannel, fileLock);
		} catch (IOException exception) {
			try {
				fileChannel.close();
			} catch (IOException closeException) {
				exception.addSuppressed(closeException);
			}

			throw exception;
		}
	}

	/**
	 * Проверяет, заблокирована ли директория мира другим процессом.
	 * Возвращает {@code false} если файл блокировки не существует.
	 *
	 * @param path директория мира
	 * @return {@code true} если мир уже открыт другим процессом
	 * @throws IOException при ошибке доступа к файловой системе (кроме отсутствия файла)
	 */
	public static boolean isLocked(Path path) throws IOException {
		Path lockFile = path.resolve(SESSION_LOCK_FILE);

		try (
			FileChannel fileChannel = FileChannel.open(lockFile, StandardOpenOption.WRITE);
			FileLock fileLock = fileChannel.tryLock()
		) {
			return fileLock == null;
		} catch (AccessDeniedException exception) {
			return true;
		} catch (NoSuchFileException exception) {
			return false;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			if (lock.isValid()) {
				lock.release();
			}
		} finally {
			if (channel.isOpen()) {
				channel.close();
			}
		}
	}

	public boolean isValid() {
		return lock.isValid();
	}

	/**
	 * Исключение, выбрасываемое при попытке открыть уже заблокированный мир.
	 */
	public static class AlreadyLockedException extends IOException {

		private AlreadyLockedException(Path path, String message) {
			super(path.toAbsolutePath() + ": " + message);
		}

		public static AlreadyLockedException create(Path path) {
			return new AlreadyLockedException(path, "already locked (possibly by other Minecraft instance?)");
		}
	}
}

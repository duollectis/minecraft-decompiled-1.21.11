package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.DelegatingDataOutput;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Утилитарный класс для чтения и записи NBT-данных в бинарном формате.
 * <p>
 * Поддерживает как сжатый (GZIP), так и несжатый формат, а также
 * потоковое сканирование через {@link NbtScanner} без полной десериализации.
 */
public class NbtIo {

	private static final OpenOption[] OPEN_OPTIONS = new OpenOption[]{
		StandardOpenOption.SYNC,
		StandardOpenOption.WRITE,
		StandardOpenOption.CREATE,
		StandardOpenOption.TRUNCATE_EXISTING
	};

	/**
	 * Читает сжатый (GZIP) NBT-компаунд из файла.
	 *
	 * @param path           путь к файлу
	 * @param tagSizeTracker трекер размера для защиты от переполнения
	 * @return прочитанный {@link NbtCompound}
	 * @throws IOException при ошибке чтения или превышении лимита размера
	 */
	public static NbtCompound readCompressed(Path path, NbtSizeTracker tagSizeTracker) throws IOException {
		try (
			InputStream inputStream = Files.newInputStream(path);
			InputStream buffered = new FixedBufferInputStream(inputStream)
		) {
			return readCompressed(buffered, tagSizeTracker);
		}
	}

	private static DataInputStream decompress(InputStream stream) throws IOException {
		return new DataInputStream(new FixedBufferInputStream(new GZIPInputStream(stream)));
	}

	private static DataOutputStream compress(OutputStream stream) throws IOException {
		return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream)));
	}

	/**
	 * Читает сжатый (GZIP) NBT-компаунд из потока.
	 *
	 * @param stream         входной поток
	 * @param tagSizeTracker трекер размера
	 * @return прочитанный {@link NbtCompound}
	 * @throws IOException при ошибке чтения
	 */
	public static NbtCompound readCompressed(InputStream stream, NbtSizeTracker tagSizeTracker) throws IOException {
		try (DataInputStream dataInputStream = decompress(stream)) {
			return readCompound(dataInputStream, tagSizeTracker);
		}
	}

	/**
	 * Сканирует сжатый (GZIP) NBT из файла через {@link NbtScanner} без полной десериализации.
	 *
	 * @param path    путь к файлу
	 * @param scanner сканер для обхода структуры
	 * @param tracker трекер размера
	 * @throws IOException при ошибке чтения
	 */
	public static void scanCompressed(Path path, NbtScanner scanner, NbtSizeTracker tracker) throws IOException {
		try (
			InputStream inputStream = Files.newInputStream(path);
			InputStream buffered = new FixedBufferInputStream(inputStream)
		) {
			scanCompressed(buffered, scanner, tracker);
		}
	}

	public static void scanCompressed(InputStream stream, NbtScanner scanner, NbtSizeTracker tracker)
	throws IOException {
		try (DataInputStream dataInputStream = decompress(stream)) {
			scan(dataInputStream, scanner, tracker);
		}
	}

	/**
	 * Записывает NBT-компаунд в файл в сжатом (GZIP) формате.
	 * Использует {@link StandardOpenOption#SYNC} для гарантии записи на диск.
	 *
	 * @param nbt  компаунд для записи
	 * @param path путь к файлу
	 * @throws IOException при ошибке записи
	 */
	public static void writeCompressed(NbtCompound nbt, Path path) throws IOException {
		try (
			OutputStream outputStream = Files.newOutputStream(path, OPEN_OPTIONS);
			OutputStream buffered = new BufferedOutputStream(outputStream)
		) {
			writeCompressed(nbt, buffered);
		}
	}

	/**
	 * Записывает NBT-компаунд в поток в сжатом (GZIP) формате.
	 *
	 * @param nbt    компаунд для записи
	 * @param stream выходной поток
	 * @throws IOException при ошибке записи
	 */
	public static void writeCompressed(NbtCompound nbt, OutputStream stream) throws IOException {
		try (DataOutputStream dataOutputStream = compress(stream)) {
			writeCompound(nbt, dataOutputStream);
		}
	}

	/**
	 * Записывает NBT-компаунд в файл в несжатом формате.
	 *
	 * @param nbt  компаунд для записи
	 * @param path путь к файлу
	 * @throws IOException при ошибке записи
	 */
	public static void write(NbtCompound nbt, Path path) throws IOException {
		try (
			OutputStream outputStream = Files.newOutputStream(path, OPEN_OPTIONS);
			OutputStream buffered = new BufferedOutputStream(outputStream);
			DataOutputStream dataOutputStream = new DataOutputStream(buffered)
		) {
			writeCompound(nbt, dataOutputStream);
		}
	}

	/**
	 * Читает NBT-компаунд из файла в несжатом формате.
	 * Возвращает {@code null}, если файл не существует.
	 *
	 * @param path путь к файлу
	 * @return прочитанный {@link NbtCompound} или {@code null}
	 * @throws IOException при ошибке чтения
	 */
	public static @Nullable NbtCompound read(Path path) throws IOException {
		if (!Files.exists(path)) {
			return null;
		}

		try (
			InputStream inputStream = Files.newInputStream(path);
			DataInputStream dataInputStream = new DataInputStream(inputStream)
		) {
			return readCompound(dataInputStream, NbtSizeTracker.ofUnlimitedBytes());
		}
	}

	/**
	 * Читает NBT-компаунд из потока без ограничений по размеру.
	 *
	 * @param input входной поток данных
	 * @return прочитанный {@link NbtCompound}
	 * @throws IOException при ошибке чтения или если корневой тег не является компаундом
	 */
	public static NbtCompound readCompound(DataInput input) throws IOException {
		return readCompound(input, NbtSizeTracker.ofUnlimitedBytes());
	}

	/**
	 * Читает NBT-компаунд из потока с проверкой ограничений по размеру.
	 *
	 * @param input   входной поток данных
	 * @param tracker трекер размера
	 * @return прочитанный {@link NbtCompound}
	 * @throws IOException при ошибке чтения или если корневой тег не является компаундом
	 */
	public static NbtCompound readCompound(DataInput input, NbtSizeTracker tracker) throws IOException {
		NbtElement element = readElement(input, tracker);
		if (element instanceof NbtCompound compound) {
			return compound;
		}

		throw new IOException("Root tag must be a named compound tag");
	}

	/**
	 * Записывает NBT-компаунд в поток данных.
	 *
	 * @param nbt    компаунд для записи
	 * @param output выходной поток данных
	 * @throws IOException при ошибке записи
	 */
	public static void writeCompound(NbtCompound nbt, DataOutput output) throws IOException {
		write(nbt, output);
	}

	/**
	 * Сканирует NBT-данные из потока через {@link NbtScanner} без полной десериализации.
	 * Позволяет выборочно читать поля без создания промежуточных объектов.
	 *
	 * @param input   входной поток данных
	 * @param scanner сканер для обхода структуры
	 * @param tracker трекер размера
	 * @throws IOException при ошибке чтения
	 */
	public static void scan(DataInput input, NbtScanner scanner, NbtSizeTracker tracker) throws IOException {
		NbtType<?> nbtType = NbtTypes.byId(input.readByte());

		if (nbtType == NbtEnd.TYPE) {
			if (scanner.start(NbtEnd.TYPE) == NbtScanner.Result.CONTINUE) {
				scanner.visitEnd();
			}
			return;
		}

		switch (scanner.start(nbtType)) {
			case HALT:
			default:
				break;
			case BREAK:
				NbtString.skip(input);
				nbtType.skip(input, tracker);
				break;
			case CONTINUE:
				NbtString.skip(input);
				nbtType.doAccept(input, scanner, tracker);
		}
	}

	/**
	 * Читает один NBT-элемент из потока (с заголовком типа и именем).
	 *
	 * @param input   входной поток данных
	 * @param tracker трекер размера
	 * @return прочитанный элемент
	 * @throws IOException при ошибке чтения
	 */
	public static NbtElement read(DataInput input, NbtSizeTracker tracker) throws IOException {
		byte typeId = input.readByte();
		return typeId == NbtElement.END_TYPE ? NbtEnd.INSTANCE : readElement(input, tracker, typeId);
	}

	/**
	 * Записывает NBT-элемент в поток с заголовком типа и пустым именем.
	 * Используется для записи в сетевые пакеты.
	 *
	 * @param nbt    элемент для записи
	 * @param output выходной поток данных
	 * @throws IOException при ошибке записи
	 */
	public static void writeForPacket(NbtElement nbt, DataOutput output) throws IOException {
		output.writeByte(nbt.getType());
		if (nbt.getType() != NbtElement.END_TYPE) {
			nbt.write(output);
		}
	}

	/**
	 * Записывает NBT-элемент в поток с заголовком типа и пустой строкой имени.
	 * Используется для небезопасной записи (без валидации UTF).
	 *
	 * @param nbt    элемент для записи
	 * @param output выходной поток данных
	 * @throws IOException при ошибке записи
	 */
	public static void writeUnsafe(NbtElement nbt, DataOutput output) throws IOException {
		output.writeByte(nbt.getType());
		if (nbt.getType() != NbtElement.END_TYPE) {
			output.writeUTF("");
			nbt.write(output);
		}
	}

	/**
	 * Записывает NBT-элемент через {@link InvalidUtfSkippingDataOutput},
	 * который молча заменяет некорректные UTF-строки на пустые.
	 *
	 * @param nbt    элемент для записи
	 * @param output выходной поток данных
	 * @throws IOException при ошибке записи
	 */
	public static void write(NbtElement nbt, DataOutput output) throws IOException {
		writeUnsafe(nbt, new NbtIo.InvalidUtfSkippingDataOutput(output));
	}

	/**
	 * Читает NBT-элемент из потока, пропуская заголовок имени.
	 * Если тип равен {@code TAG_End}, возвращает {@link NbtEnd#INSTANCE}.
	 *
	 * @param input   входной поток данных
	 * @param tracker трекер размера
	 * @return прочитанный элемент
	 * @throws IOException при ошибке чтения
	 */
	@VisibleForTesting
	public static NbtElement readElement(DataInput input, NbtSizeTracker tracker) throws IOException {
		byte typeId = input.readByte();
		if (typeId == NbtElement.END_TYPE) {
			return NbtEnd.INSTANCE;
		}

		NbtString.skip(input);
		return readElement(input, tracker, typeId);
	}

	private static NbtElement readElement(DataInput input, NbtSizeTracker tracker, byte typeId) {
		try {
			return NbtTypes.byId(typeId).read(input, tracker);
		}
		catch (IOException exception) {
			CrashReport crashReport = CrashReport.create(exception, "Loading NBT data");
			CrashReportSection section = crashReport.addElement("NBT Tag");
			section.add("Tag type", typeId);
			throw new NbtCrashException(crashReport);
		}
	}

	/**
	 * Обёртка над {@link DataOutput}, которая при ошибке записи некорректной UTF-строки
	 * логирует проблему и записывает пустую строку вместо исходной.
	 * Предотвращает падение сервера из-за повреждённых строковых данных в NBT.
	 */
	public static class InvalidUtfSkippingDataOutput extends DelegatingDataOutput {

		public InvalidUtfSkippingDataOutput(DataOutput dataOutput) {
			super(dataOutput);
		}

		@Override
		public void writeUTF(String string) throws IOException {
			try {
				super.writeUTF(string);
			}
			catch (UTFDataFormatException exception) {
				Util.logErrorOrPause("Failed to write NBT String", exception);
				super.writeUTF("");
			}
		}
	}
}

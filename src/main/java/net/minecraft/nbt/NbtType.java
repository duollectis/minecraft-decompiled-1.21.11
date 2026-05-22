package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;

import java.io.DataInput;
import java.io.IOException;

/**
 * Дескриптор типа NBT-тега: умеет читать, пропускать и сканировать теги из бинарного потока.
 * <p>
 * Каждый конкретный тип (NbtByte, NbtCompound и т.д.) объявляет статическую константу {@code TYPE},
 * реализующую этот интерфейс. Реестр всех типов хранится в {@link NbtTypes}.
 *
 * @param <T> конкретный класс NBT-элемента
 */
public interface NbtType<T extends NbtElement> {

	/**
	 * Читает тег из бинарного потока, учитывая ограничения по размеру.
	 *
	 * @param input   источник данных
	 * @param tracker трекер размера для защиты от переполнения
	 * @return прочитанный элемент
	 * @throws IOException при ошибке чтения
	 */
	T read(DataInput input, NbtSizeTracker tracker) throws IOException;

	/**
	 * Сканирует тег через {@link NbtScanner} без полной десериализации в объект.
	 * Позволяет выборочно читать поля без создания промежуточных объектов.
	 *
	 * @param input   источник данных
	 * @param visitor сканер, получающий уведомления о прочитанных значениях
	 * @param tracker трекер размера
	 * @return результат сканирования
	 * @throws IOException при ошибке чтения
	 */
	NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException;

	/**
	 * Запускает полный цикл сканирования: сначала вызывает {@link NbtScanner#start},
	 * затем в зависимости от результата либо читает данные, либо пропускает их.
	 *
	 * @param input   источник данных
	 * @param visitor сканер
	 * @param tracker трекер размера
	 * @throws IOException при ошибке чтения
	 */
	default void accept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
		switch (visitor.start(this)) {
			case CONTINUE:
				doAccept(input, visitor, tracker);
			case HALT:
			default:
				break;
			case BREAK:
				skip(input, tracker);
		}
	}

	/**
	 * Пропускает {@code count} последовательных тегов данного типа в потоке.
	 *
	 * @param input   источник данных
	 * @param count   количество тегов для пропуска
	 * @param tracker трекер размера
	 * @throws IOException при ошибке чтения
	 */
	void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException;

	/**
	 * Пропускает один тег данного типа в потоке.
	 *
	 * @param input   источник данных
	 * @param tracker трекер размера
	 * @throws IOException при ошибке чтения
	 */
	void skip(DataInput input, NbtSizeTracker tracker) throws IOException;

	String getCrashReportName();

	String getCommandFeedbackName();

	/**
	 * Создаёт «невалидный» тип-заглушку для неизвестных идентификаторов тегов.
	 * Любая операция с ним бросает {@link IOException} с описанием проблемного id.
	 *
	 * @param type неизвестный числовой идентификатор типа
	 * @return тип-заглушка, бросающий исключение при любом обращении
	 */
	static NbtType<NbtEnd> createInvalid(int type) {
		return new NbtType<NbtEnd>() {
			private IOException createException() {
				return new IOException("Invalid tag id: " + type);
			}

			@Override
			public NbtEnd read(DataInput input, NbtSizeTracker tracker) throws IOException {
				throw createException();
			}

			@Override
			public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
			throws IOException {
				throw createException();
			}

			@Override
			public void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
				throw createException();
			}

			@Override
			public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
				throw createException();
			}

			@Override
			public String getCrashReportName() {
				return "INVALID[" + type + "]";
			}

			@Override
			public String getCommandFeedbackName() {
				return "UNKNOWN_" + type;
			}
		};
	}

	/**
	 * Специализация для тегов фиксированного размера (byte, short, int, long, float, double).
	 * Реализует {@code skip} через {@link DataInput#skipBytes} на основе известного размера.
	 *
	 * @param <T> конкретный класс NBT-элемента
	 */
	interface OfFixedSize<T extends NbtElement> extends NbtType<T> {

		int getSizeInBytes();

		@Override
		default void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(getSizeInBytes());
		}

		@Override
		default void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(getSizeInBytes() * count);
		}
	}

	/**
	 * Специализация для тегов переменного размера (строки, массивы, списки, компаунды).
	 * Реализует {@code skip(count)} через последовательный вызов {@code skip(1)} для каждого элемента.
	 *
	 * @param <T> конкретный класс NBT-элемента
	 */
	interface OfVariableSize<T extends NbtElement> extends NbtType<T> {

		@Override
		default void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
			for (int index = 0; index < count; index++) {
				skip(input, tracker);
			}
		}
	}
}

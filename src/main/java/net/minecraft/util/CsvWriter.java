package net.minecraft.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Утилита для записи данных в формате CSV.
 * <p>
 * Создаётся через {@link Header#startBody(Writer)} после объявления заголовков столбцов.
 * Все значения автоматически экранируются по правилам CSV.
 */
public class CsvWriter {

	private static final String CRLF = "\r\n";
	private static final String COMMA = ",";

	private final Writer writer;
	private final int columnCount;

	CsvWriter(Writer writer, List<String> columns) throws IOException {
		this.writer = writer;
		this.columnCount = columns.size();
		printRow(columns.stream());
	}

	/** @return построитель заголовка CSV-файла */
	public static CsvWriter.Header makeHeader() {
		return new CsvWriter.Header();
	}

	/**
	 * Записывает строку данных в CSV-файл.
	 *
	 * @param columns значения столбцов; количество должно совпадать с числом объявленных заголовков
	 * @throws IllegalArgumentException если количество значений не совпадает с числом столбцов
	 */
	public void printRow(@Nullable Object... columns) throws IOException {
		if (columns.length != columnCount) {
			throw new IllegalArgumentException(
				"Invalid number of columns, expected " + columnCount + ", but got " + columns.length
			);
		}

		printRow(Stream.of(columns));
	}

	private void printRow(Stream<? extends @Nullable Object> columns) throws IOException {
		writer.write(columns.map(CsvWriter::escape).collect(Collectors.joining(COMMA)) + CRLF);
	}

	private static String escape(@Nullable Object value) {
		return StringEscapeUtils.escapeCsv(value != null ? value.toString() : "[null]");
	}

	/**
	 * Построитель заголовка CSV-файла.
	 * Позволяет объявить столбцы перед началом записи данных.
	 */
	public static class Header {

		private final List<String> columns = Lists.newArrayList();

		public CsvWriter.Header addColumn(String name) {
			columns.add(name);
			return this;
		}

		/**
		 * Записывает строку заголовков и возвращает {@link CsvWriter} для записи данных.
		 *
		 * @param writer целевой поток записи
		 * @return готовый к использованию {@link CsvWriter}
		 */
		public CsvWriter startBody(Writer writer) throws IOException {
			return new CsvWriter(writer, columns);
		}
	}
}

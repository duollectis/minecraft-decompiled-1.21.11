package net.minecraft.client.realms.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Утилита для разбора текста с встроенными ссылками-плейсхолдерами {@code %link}.
 * <p>
 * Текст разбивается на строки по символу {@code \n}, затем каждая строка
 * разбивается по плейсхолдеру {@code %link}, который заменяется на переданные
 * объекты {@link LineSegment} в порядке их следования.
 */
@Environment(EnvType.CLIENT)
public class TextRenderingUtils {

	private TextRenderingUtils() {
	}

	@VisibleForTesting
	protected static List<String> lineBreak(String text) {
		return Arrays.asList(text.split("\\n"));
	}

	/**
	 * Разбирает текст с плейсхолдерами {@code %link} и заменяет их на переданные сегменты.
	 *
	 * @param text  исходный текст с плейсхолдерами {@code %link} и переносами строк {@code \n}
	 * @param links сегменты-ссылки в порядке замены плейсхолдеров
	 * @return список строк, каждая из которых содержит список сегментов
	 */
	public static List<TextRenderingUtils.Line> decompose(String text, TextRenderingUtils.LineSegment... links) {
		return decompose(text, Arrays.asList(links));
	}

	private static List<TextRenderingUtils.Line> decompose(String text, List<TextRenderingUtils.LineSegment> links) {
		return insertLinks(lineBreak(text), links);
	}

	private static List<TextRenderingUtils.Line> insertLinks(
		List<String> lines,
		List<TextRenderingUtils.LineSegment> links
	) {
		int linkIndex = 0;
		List<TextRenderingUtils.Line> result = Lists.newArrayList();

		for (String line : lines) {
			List<TextRenderingUtils.LineSegment> segments = Lists.newArrayList();

			for (String part : split(line, "%link")) {
				if ("%link".equals(part)) {
					segments.add(links.get(linkIndex++));
				} else {
					segments.add(TextRenderingUtils.LineSegment.text(part));
				}
			}

			result.add(new TextRenderingUtils.Line(segments));
		}

		return result;
	}

	/**
	 * Разбивает строку по разделителю, включая сам разделитель как отдельный элемент.
	 * Пустые части между разделителями не добавляются.
	 *
	 * @param line      исходная строка
	 * @param delimiter разделитель (не может быть пустой строкой)
	 * @return список частей строки, включая вхождения разделителя
	 */
	public static List<String> split(String line, String delimiter) {
		if (delimiter.isEmpty()) {
			throw new IllegalArgumentException("Delimiter cannot be the empty string");
		}

		List<String> parts = Lists.newArrayList();
		int searchFrom = 0;

		int matchIndex;
		while ((matchIndex = line.indexOf(delimiter, searchFrom)) != -1) {
			if (matchIndex > searchFrom) {
				parts.add(line.substring(searchFrom, matchIndex));
			}

			parts.add(delimiter);
			searchFrom = matchIndex + delimiter.length();
		}

		if (searchFrom < line.length()) {
			parts.add(line.substring(searchFrom));
		}

		return parts;
	}

	/**
	 * Одна строка текста, состоящая из последовательности сегментов.
	 * Сегменты могут быть обычным текстом или кликабельными ссылками.
	 */
	@Environment(EnvType.CLIENT)
	public static class Line {

		public final List<TextRenderingUtils.LineSegment> segments;

		Line(TextRenderingUtils.LineSegment... segments) {
			this(Arrays.asList(segments));
		}

		Line(List<TextRenderingUtils.LineSegment> segments) {
			this.segments = segments;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TextRenderingUtils.Line line = (TextRenderingUtils.Line) o;
			return Objects.equals(segments, line.segments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(segments);
		}

		@Override
		public String toString() {
			return "Line{segments=" + segments + "}";
		}
	}

	/**
	 * Сегмент строки — либо обычный текст, либо кликабельная ссылка с заголовком и URL.
	 * Ссылка создаётся через {@link #link}, обычный текст — через {@link #text}.
	 */
	@Environment(EnvType.CLIENT)
	public static class LineSegment {

		private final String fullText;
		private final @Nullable String linkTitle;
		private final @Nullable String linkUrl;

		private LineSegment(String fullText) {
			this.fullText = fullText;
			linkTitle = null;
			linkUrl = null;
		}

		private LineSegment(String fullText, @Nullable String linkTitle, @Nullable String linkUrl) {
			this.fullText = fullText;
			this.linkTitle = linkTitle;
			this.linkUrl = linkUrl;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TextRenderingUtils.LineSegment segment = (TextRenderingUtils.LineSegment) o;
			return Objects.equals(fullText, segment.fullText)
				&& Objects.equals(linkTitle, segment.linkTitle)
				&& Objects.equals(linkUrl, segment.linkUrl);
		}

		@Override
		public int hashCode() {
			return Objects.hash(fullText, linkTitle, linkUrl);
		}

		@Override
		public String toString() {
			return "Segment{fullText='" + fullText + "', linkTitle='" + linkTitle + "', linkUrl='" + linkUrl + "'}";
		}

		public String renderedText() {
			return isLink() ? linkTitle : fullText;
		}

		public boolean isLink() {
			return linkTitle != null;
		}

		public String getLinkUrl() {
			if (!isLink()) {
				throw new IllegalStateException("Not a link: " + this);
			}

			return linkUrl;
		}

		public static TextRenderingUtils.LineSegment link(String linkTitle, String linkUrl) {
			return new TextRenderingUtils.LineSegment(null, linkTitle, linkUrl);
		}

		@VisibleForTesting
		protected static TextRenderingUtils.LineSegment text(String fullText) {
			return new TextRenderingUtils.LineSegment(fullText);
		}
	}
}

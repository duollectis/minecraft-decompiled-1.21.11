package net.minecraft.nbt.visitor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.nbt.*;
import net.minecraft.util.Util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Форматирует дерево NBT в читаемую многострочную SNBT-строку с отступами и упорядоченными ключами.
 * <p>
 * В отличие от {@link StringNbtWriter}, этот форматтер:
 * <ul>
 *   <li>Добавляет отступы и переносы строк для вложенных структур.</li>
 *   <li>Сортирует ключи {@link NbtCompound} в алфавитном порядке, с возможностью
 *       переопределения порядка через {@link #ENTRY_ORDER_OVERRIDES} для известных структур.</li>
 *   <li>Пропускает форматирование для путей из {@link #IGNORED_PATHS}, выводя их компактно.</li>
 * </ul>
 * Используется для отладочного вывода и инструментов разработчика.
 */
public class NbtOrderedStringFormatter implements NbtElementVisitor {

	private static final Map<String, List<String>> ENTRY_ORDER_OVERRIDES = Util.make(
		Maps.newHashMap(), map -> {
			map.put(
				"{}",
				Lists.newArrayList(
					"DataVersion",
					"author",
					"size",
					"data",
					"entities",
					"palette",
					"palettes"
				)
			);
			map.put("{}.data.[].{}", Lists.newArrayList("pos", "state", "nbt"));
			map.put("{}.entities.[].{}", Lists.newArrayList("blockPos", "pos"));
		}
	);
	private static final Set<String> IGNORED_PATHS = Sets.newHashSet(
		"{}.size.[]",
		"{}.data.[].{}",
		"{}.palette.[].{}",
		"{}.entities.[].{}"
	);
	private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z0-9._+-]+");
	private static final String KEY_VALUE_SEPARATOR = String.valueOf(':');
	private static final String ENTRY_SEPARATOR = String.valueOf(',');

	private final String prefix;
	private final int indentationLevel;
	private final List<String> pathParts;
	private String result = "";

	public NbtOrderedStringFormatter() {
		this("    ", 0, Lists.newArrayList());
	}

	public NbtOrderedStringFormatter(String prefix, int indentationLevel, List<String> pathParts) {
		this.prefix = prefix;
		this.indentationLevel = indentationLevel;
		this.pathParts = pathParts;
	}

	/**
	 * Применяет форматтер к NBT-элементу и возвращает отформатированную строку.
	 *
	 * @param element корневой элемент для форматирования
	 * @return отформатированная SNBT-строка с отступами
	 */
	public String apply(NbtElement element) {
		element.accept(this);
		return result;
	}

	@Override
	public void visitString(NbtString element) {
		result = NbtString.escape(element.value());
	}

	@Override
	public void visitByte(NbtByte element) {
		result = element.value() + "b";
	}

	@Override
	public void visitShort(NbtShort element) {
		result = element.value() + "s";
	}

	@Override
	public void visitInt(NbtInt element) {
		result = String.valueOf(element.value());
	}

	@Override
	public void visitLong(NbtLong element) {
		result = element.value() + "L";
	}

	@Override
	public void visitFloat(NbtFloat element) {
		result = element.value() + "f";
	}

	@Override
	public void visitDouble(NbtDouble element) {
		result = element.value() + "d";
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		StringBuilder builder = new StringBuilder("[B;");
		byte[] bytes = element.getByteArray();

		for (int index = 0; index < bytes.length; index++) {
			builder.append(' ').append(bytes[index]).append('B');
			if (index != bytes.length - 1) {
				builder.append(ENTRY_SEPARATOR);
			}
		}

		builder.append(']');
		result = builder.toString();
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		StringBuilder builder = new StringBuilder("[I;");
		int[] ints = element.getIntArray();

		for (int index = 0; index < ints.length; index++) {
			builder.append(' ').append(ints[index]);
			if (index != ints.length - 1) {
				builder.append(ENTRY_SEPARATOR);
			}
		}

		builder.append(']');
		result = builder.toString();
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		StringBuilder builder = new StringBuilder("[L;");
		long[] longs = element.getLongArray();

		for (int index = 0; index < longs.length; index++) {
			builder.append(' ').append(longs[index]).append('L');
			if (index != longs.length - 1) {
				builder.append(ENTRY_SEPARATOR);
			}
		}

		builder.append(']');
		result = builder.toString();
	}

	@Override
	public void visitList(NbtList element) {
		if (element.isEmpty()) {
			result = "[]";
			return;
		}

		StringBuilder builder = new StringBuilder("[");
		pushPathPart("[]");
		String indent = IGNORED_PATHS.contains(joinPath()) ? "" : prefix;

		if (!indent.isEmpty()) {
			builder.append('\n');
		}

		for (int index = 0; index < element.size(); index++) {
			builder.append(Strings.repeat(indent, indentationLevel + 1));
			builder.append(
				new NbtOrderedStringFormatter(indent, indentationLevel + 1, pathParts)
					.apply(element.get(index))
			);

			if (index != element.size() - 1) {
				builder.append(ENTRY_SEPARATOR).append(indent.isEmpty() ? " " : "\n");
			}
		}

		if (!indent.isEmpty()) {
			builder.append('\n').append(Strings.repeat(indent, indentationLevel));
		}

		builder.append(']');
		result = builder.toString();
		popPathPart();
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		if (compound.isEmpty()) {
			result = "{}";
			return;
		}

		StringBuilder builder = new StringBuilder("{");
		pushPathPart("{}");
		String indent = IGNORED_PATHS.contains(joinPath()) ? "" : prefix;

		if (!indent.isEmpty()) {
			builder.append('\n');
		}

		Collection<String> sortedNames = getSortedNames(compound);
		Iterator<String> iterator = sortedNames.iterator();

		while (iterator.hasNext()) {
			String key = iterator.next();
			NbtElement value = compound.get(key);
			pushPathPart(key);
			builder.append(Strings.repeat(indent, indentationLevel + 1))
			       .append(escapeName(key))
			       .append(KEY_VALUE_SEPARATOR)
			       .append(' ')
			       .append(new NbtOrderedStringFormatter(indent, indentationLevel + 1, pathParts).apply(value));
			popPathPart();

			if (iterator.hasNext()) {
				builder.append(ENTRY_SEPARATOR).append(indent.isEmpty() ? " " : "\n");
			}
		}

		if (!indent.isEmpty()) {
			builder.append('\n').append(Strings.repeat(indent, indentationLevel));
		}

		builder.append('}');
		result = builder.toString();
		popPathPart();
	}

	private void popPathPart() {
		pathParts.remove(pathParts.size() - 1);
	}

	private void pushPathPart(String part) {
		pathParts.add(part);
	}

	/**
	 * Возвращает список ключей {@link NbtCompound} в нужном порядке.
	 * <p>
	 * Если для текущего пути задан порядок в {@link #ENTRY_ORDER_OVERRIDES},
	 * приоритетные ключи идут первыми, остальные — в алфавитном порядке.
	 * Иначе все ключи сортируются алфавитно.
	 *
	 * @param compound исходный compound-тег
	 * @return упорядоченный список ключей
	 */
	protected List<String> getSortedNames(NbtCompound compound) {
		Set<String> remaining = Sets.newHashSet(compound.getKeys());
		List<String> ordered = Lists.newArrayList();
		List<String> priorityOrder = ENTRY_ORDER_OVERRIDES.get(joinPath());

		if (priorityOrder != null) {
			for (String key : priorityOrder) {
				if (remaining.remove(key)) {
					ordered.add(key);
				}
			}

			if (!remaining.isEmpty()) {
				remaining.stream().sorted().forEach(ordered::add);
			}
		}
		else {
			ordered.addAll(remaining);
			Collections.sort(ordered);
		}

		return ordered;
	}

	/**
	 * Возвращает текущий путь в дереве NBT в виде строки, разделённой точками.
	 * Используется для поиска в {@link #ENTRY_ORDER_OVERRIDES} и {@link #IGNORED_PATHS}.
	 *
	 * @return строковое представление текущего пути, например {@code "{}.data.[].{}"}
	 */
	public String joinPath() {
		return String.join(".", pathParts);
	}

	/**
	 * Экранирует имя ключа NBT, если оно содержит спецсимволы.
	 * Простые алфавитно-цифровые имена возвращаются без изменений.
	 *
	 * @param name имя ключа
	 * @return экранированное или исходное имя
	 */
	protected static String escapeName(String name) {
		return SIMPLE_NAME.matcher(name).matches() ? name : NbtString.escape(name);
	}

	@Override
	public void visitEnd(NbtEnd element) {
	}
}

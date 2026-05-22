package net.minecraft.nbt.visitor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Форматирует дерево NBT-элементов в цветной {@link Text} для отображения в интерфейсе игры.
 * <p>
 * Каждый тип данных окрашивается в свой цвет: числа — золотым, строки — зелёным,
 * суффиксы типов — красным, имена ключей — голубым. Глубоко вложенные структуры
 * и длинные массивы обрезаются с заменой на {@code <...>} для читаемости.
 */
public class NbtTextFormatter implements NbtElementVisitor {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_LIST_INLINE_SIZE = 8;
	private static final int MAX_ARRAY_DISPLAY_SIZE = 128;
	private static final int MAX_DEPTH_BEFORE_ELLIPSIS = 64;
	private static final Formatting NAME_COLOR = Formatting.AQUA;
	private static final Formatting STRING_COLOR = Formatting.GREEN;
	private static final Formatting NUMBER_COLOR = Formatting.GOLD;
	private static final Formatting TYPE_SUFFIX_COLOR = Formatting.RED;
	private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z0-9._+-]+");
	private static final Text ELLIPSIS = Text.literal("<...>").formatted(Formatting.GRAY);
	private static final Text BYTE_TYPE_SUFFIX = Text.literal("b").formatted(TYPE_SUFFIX_COLOR);
	private static final Text SHORT_TYPE_SUFFIX = Text.literal("s").formatted(TYPE_SUFFIX_COLOR);
	private static final Text INT_TYPE_SUFFIX = Text.literal("I").formatted(TYPE_SUFFIX_COLOR);
	private static final Text LONG_TYPE_SUFFIX = Text.literal("L").formatted(TYPE_SUFFIX_COLOR);
	private static final Text FLOAT_TYPE_SUFFIX = Text.literal("f").formatted(TYPE_SUFFIX_COLOR);
	private static final Text DOUBLE_TYPE_SUFFIX = Text.literal("d").formatted(TYPE_SUFFIX_COLOR);
	private static final Text ARRAY_BYTE_TYPE_SUFFIX = Text.literal("B").formatted(TYPE_SUFFIX_COLOR);
	private static final String ENTRY_SEPARATOR = String.valueOf(',');
	private static final String ENTRY_SEPARATOR_WITH_NEW_LINE = ENTRY_SEPARATOR + "\n";
	private static final String ENTRY_SEPARATOR_WITH_SPACE = ENTRY_SEPARATOR + " ";

	private final String prefix;
	private int indentationLevel;
	private int depth;
	private final MutableText result = Text.empty();

	public NbtTextFormatter(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Применяет форматтер к NBT-элементу и возвращает цветной текст.
	 *
	 * @param element корневой элемент для форматирования
	 * @return цветной {@link Text} с форматированным содержимым
	 */
	public Text apply(NbtElement element) {
		element.accept(this);
		return result;
	}

	@Override
	public void visitString(NbtString element) {
		String escaped = NbtString.escape(element.value());
		String quote = escaped.substring(0, 1);
		Text content = Text.literal(escaped.substring(1, escaped.length() - 1)).formatted(STRING_COLOR);
		result.append(quote).append(content).append(quote);
	}

	@Override
	public void visitByte(NbtByte element) {
		result
			.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
			.append(BYTE_TYPE_SUFFIX);
	}

	@Override
	public void visitShort(NbtShort element) {
		result
			.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
			.append(SHORT_TYPE_SUFFIX);
	}

	@Override
	public void visitInt(NbtInt element) {
		result.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR));
	}

	@Override
	public void visitLong(NbtLong element) {
		result
			.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
			.append(LONG_TYPE_SUFFIX);
	}

	@Override
	public void visitFloat(NbtFloat element) {
		result
			.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
			.append(FLOAT_TYPE_SUFFIX);
	}

	@Override
	public void visitDouble(NbtDouble element) {
		result
			.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
			.append(DOUBLE_TYPE_SUFFIX);
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		result.append("[").append(ARRAY_BYTE_TYPE_SUFFIX).append(";");
		byte[] bytes = element.getByteArray();

		for (int index = 0; index < bytes.length && index < MAX_ARRAY_DISPLAY_SIZE; index++) {
			MutableText number = Text.literal(String.valueOf(bytes[index])).formatted(NUMBER_COLOR);
			result.append(" ").append(number).append(ARRAY_BYTE_TYPE_SUFFIX);
			if (index != bytes.length - 1) {
				result.append(ENTRY_SEPARATOR);
			}
		}

		if (bytes.length > MAX_ARRAY_DISPLAY_SIZE) {
			result.append(ELLIPSIS);
		}

		result.append("]");
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		result.append("[").append(INT_TYPE_SUFFIX).append(";");
		int[] ints = element.getIntArray();

		for (int index = 0; index < ints.length && index < MAX_ARRAY_DISPLAY_SIZE; index++) {
			result.append(" ").append(Text.literal(String.valueOf(ints[index])).formatted(NUMBER_COLOR));
			if (index != ints.length - 1) {
				result.append(ENTRY_SEPARATOR);
			}
		}

		if (ints.length > MAX_ARRAY_DISPLAY_SIZE) {
			result.append(ELLIPSIS);
		}

		result.append("]");
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		result.append("[").append(LONG_TYPE_SUFFIX).append(";");
		long[] longs = element.getLongArray();

		for (int index = 0; index < longs.length && index < MAX_ARRAY_DISPLAY_SIZE; index++) {
			Text number = Text.literal(String.valueOf(longs[index])).formatted(NUMBER_COLOR);
			result.append(" ").append(number).append(LONG_TYPE_SUFFIX);
			if (index != longs.length - 1) {
				result.append(ENTRY_SEPARATOR);
			}
		}

		if (longs.length > MAX_ARRAY_DISPLAY_SIZE) {
			result.append(ELLIPSIS);
		}

		result.append("]");
	}

	/**
	 * Определяет, нужно ли форматировать список с отступами.
	 * Списки с числовыми элементами или большие списки выводятся компактно.
	 */
	private static boolean shouldIndent(NbtList list) {
		if (list.size() >= MAX_LIST_INLINE_SIZE) {
			return false;
		}

		for (NbtElement element : list) {
			if (element instanceof AbstractNbtNumber) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void visitList(NbtList element) {
		if (element.isEmpty()) {
			result.append("[]");
			return;
		}

		if (depth >= MAX_DEPTH_BEFORE_ELLIPSIS) {
			result.append("[").append(ELLIPSIS).append("]");
			return;
		}

		if (!shouldIndent(element)) {
			result.append("[");

			for (int index = 0; index < element.size(); index++) {
				if (index != 0) {
					result.append(ENTRY_SEPARATOR_WITH_SPACE);
				}

				formatSubElement(element.get(index), false);
			}

			result.append("]");
			return;
		}

		result.append("[");
		if (!prefix.isEmpty()) {
			result.append("\n");
		}

		String indent = Strings.repeat(prefix, indentationLevel + 1);

		for (int index = 0; index < element.size() && index < MAX_ARRAY_DISPLAY_SIZE; index++) {
			result.append(indent);
			formatSubElement(element.get(index), true);
			if (index != element.size() - 1) {
				result.append(prefix.isEmpty() ? ENTRY_SEPARATOR_WITH_SPACE : ENTRY_SEPARATOR_WITH_NEW_LINE);
			}
		}

		if (element.size() > MAX_ARRAY_DISPLAY_SIZE) {
			result.append(indent).append(ELLIPSIS);
		}

		if (!prefix.isEmpty()) {
			result.append("\n").append(Strings.repeat(prefix, indentationLevel));
		}

		result.append("]");
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		if (compound.isEmpty()) {
			result.append("{}");
			return;
		}

		if (depth >= MAX_DEPTH_BEFORE_ELLIPSIS) {
			result.append("{").append(ELLIPSIS).append("}");
			return;
		}

		result.append("{");
		Collection<String> keys = compound.getKeys();

		if (LOGGER.isDebugEnabled()) {
			List<String> sortedKeys = Lists.newArrayList(compound.getKeys());
			Collections.sort(sortedKeys);
			keys = sortedKeys;
		}

		if (!prefix.isEmpty()) {
			result.append("\n");
		}

		String indent = Strings.repeat(prefix, indentationLevel + 1);
		Iterator<String> iterator = keys.iterator();

		while (iterator.hasNext()) {
			String key = iterator.next();
			result.append(indent).append(escapeName(key)).append(": ");
			formatSubElement(compound.get(key), true);
			if (iterator.hasNext()) {
				result.append(prefix.isEmpty() ? ENTRY_SEPARATOR_WITH_SPACE : ENTRY_SEPARATOR_WITH_NEW_LINE);
			}
		}

		if (!prefix.isEmpty()) {
			result.append("\n").append(Strings.repeat(prefix, indentationLevel));
		}

		result.append("}");
	}

	private void formatSubElement(NbtElement element, boolean indent) {
		if (indent) {
			indentationLevel++;
		}

		depth++;

		try {
			element.accept(this);
		}
		finally {
			if (indent) {
				indentationLevel--;
			}

			depth--;
		}
	}

	/**
	 * Экранирует имя ключа NBT для цветного отображения.
	 * Простые имена окрашиваются напрямую, сложные — с кавычками.
	 *
	 * @param name имя ключа
	 * @return цветной {@link Text} с именем ключа
	 */
	protected static Text escapeName(String name) {
		if (SIMPLE_NAME.matcher(name).matches()) {
			return Text.literal(name).formatted(NAME_COLOR);
		}

		String escaped = NbtString.escape(name);
		String quote = escaped.substring(0, 1);
		Text content = Text.literal(escaped.substring(1, escaped.length() - 1)).formatted(NAME_COLOR);
		return Text.literal(quote).append(content).append(quote);
	}

	@Override
	public void visitEnd(NbtEnd element) {
	}
}

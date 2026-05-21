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
 * {@code NbtTextFormatter}.
 */
public class NbtTextFormatter implements NbtElementVisitor {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_LIST_INLINE_SIZE = 8;
	private static final int MAX_ARRAY_DISPLAY_SIZE_SMALL = 64;
	private static final int MAX_ARRAY_DISPLAY_SIZE = 128;
	private static final Formatting NAME_COLOR = Formatting.AQUA;
	private static final Formatting STRING_COLOR = Formatting.GREEN;
	private static final Formatting NUMBER_COLOR = Formatting.GOLD;
	private static final Formatting TYPE_SUFFIX_COLOR = Formatting.RED;
	private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z0-9._+-]+");
	private static final String SQUARE_OPEN_BRACKET = "[";
	private static final String SQUARE_CLOSE_BRACKET = "]";
	private static final String SEMICOLON = ";";
	private static final String SPACE = " ";
	private static final String CURLY_OPEN_BRACKET = "{";
	private static final String CURLY_CLOSE_BRACKET = "}";
	private static final String NEW_LINE = "\n";
	private static final String COLON_WITH_SPACE = ": ";
	private static final String ENTRY_SEPARATOR = String.valueOf(',');
	private static final String ENTRY_SEPARATOR_WITH_NEW_LINE = ENTRY_SEPARATOR + "\n";
	private static final String ENTRY_SEPARATOR_WITH_SPACE = ENTRY_SEPARATOR + " ";
	private static final Text ELLIPSIS = Text.literal("<...>").formatted(Formatting.GRAY);
	private static final Text BYTE_TYPE_SUFFIX = Text.literal("b").formatted(TYPE_SUFFIX_COLOR);
	private static final Text SHORT_TYPE_SUFFIX = Text.literal("s").formatted(TYPE_SUFFIX_COLOR);
	private static final Text INT_TYPE_SUFFIX = Text.literal("I").formatted(TYPE_SUFFIX_COLOR);
	private static final Text LONG_TYPE_SUFFIX = Text.literal("L").formatted(TYPE_SUFFIX_COLOR);
	private static final Text FLOAT_TYPE_SUFFIX = Text.literal("f").formatted(TYPE_SUFFIX_COLOR);
	private static final Text DOUBLE_TYPE_SUFFIX = Text.literal("d").formatted(TYPE_SUFFIX_COLOR);
	private static final Text ARRAY_BYTE_TYPE_SUFFIX = Text.literal("B").formatted(TYPE_SUFFIX_COLOR);
	private final String prefix;
	private int indentationLevel;
	private int depth;
	private final MutableText result = Text.empty();

	public NbtTextFormatter(String prefix) {
		this.prefix = prefix;
	}

	public Text apply(NbtElement element) {
		element.accept(this);
		return this.result;
	}

	@Override
	public void visitString(NbtString element) {
		String string = NbtString.escape(element.value());
		String string2 = string.substring(0, 1);
		Text text = Text.literal(string.substring(1, string.length() - 1)).formatted(STRING_COLOR);
		this.result.append(string2).append(text).append(string2);
	}

	@Override
	public void visitByte(NbtByte element) {
		this.result
				.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
				.append(BYTE_TYPE_SUFFIX);
	}

	@Override
	public void visitShort(NbtShort element) {
		this.result
				.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
				.append(SHORT_TYPE_SUFFIX);
	}

	@Override
	public void visitInt(NbtInt element) {
		this.result.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR));
	}

	@Override
	public void visitLong(NbtLong element) {
		this.result
				.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
				.append(LONG_TYPE_SUFFIX);
	}

	@Override
	public void visitFloat(NbtFloat element) {
		this.result
				.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
				.append(FLOAT_TYPE_SUFFIX);
	}

	@Override
	public void visitDouble(NbtDouble element) {
		this.result
				.append(Text.literal(String.valueOf(element.value())).formatted(NUMBER_COLOR))
				.append(DOUBLE_TYPE_SUFFIX);
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		this.result.append("[").append(ARRAY_BYTE_TYPE_SUFFIX).append(";");
		byte[] bs = element.getByteArray();

		for (int i = 0; i < bs.length && i < MAX_ARRAY_DISPLAY_SIZE; i++) {
			MutableText mutableText = Text.literal(String.valueOf(bs[i])).formatted(NUMBER_COLOR);
			this.result.append(" ").append(mutableText).append(ARRAY_BYTE_TYPE_SUFFIX);
			if (i != bs.length - 1) {
				this.result.append(ENTRY_SEPARATOR);
			}
		}

		if (bs.length > MAX_ARRAY_DISPLAY_SIZE) {
			this.result.append(ELLIPSIS);
		}

		this.result.append("]");
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		this.result.append("[").append(INT_TYPE_SUFFIX).append(";");
		int[] is = element.getIntArray();

		for (int i = 0; i < is.length && i < MAX_ARRAY_DISPLAY_SIZE; i++) {
			this.result.append(" ").append(Text.literal(String.valueOf(is[i])).formatted(NUMBER_COLOR));
			if (i != is.length - 1) {
				this.result.append(ENTRY_SEPARATOR);
			}
		}

		if (is.length > MAX_ARRAY_DISPLAY_SIZE) {
			this.result.append(ELLIPSIS);
		}

		this.result.append("]");
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		this.result.append("[").append(LONG_TYPE_SUFFIX).append(";");
		long[] ls = element.getLongArray();

		for (int i = 0; i < ls.length && i < MAX_ARRAY_DISPLAY_SIZE; i++) {
			Text text = Text.literal(String.valueOf(ls[i])).formatted(NUMBER_COLOR);
			this.result.append(" ").append(text).append(LONG_TYPE_SUFFIX);
			if (i != ls.length - 1) {
				this.result.append(ENTRY_SEPARATOR);
			}
		}

		if (ls.length > MAX_ARRAY_DISPLAY_SIZE) {
			this.result.append(ELLIPSIS);
		}

		this.result.append("]");
	}

	private static boolean shouldIndent(NbtList list) {
		if (list.size() >= MAX_LIST_INLINE_SIZE) {
			return false;
		}
		else {
			for (NbtElement nbtElement : list) {
				if (!(nbtElement instanceof AbstractNbtNumber)) {
					return true;
				}
			}

			return false;
		}
	}

	@Override
	public void visitList(NbtList element) {
		if (element.isEmpty()) {
			this.result.append("[]");
		}
		else if (this.depth >= 64) {
			this.result.append("[").append(ELLIPSIS).append("]");
		}
		else if (!shouldIndent(element)) {
			this.result.append("[");

			for (int i = 0; i < element.size(); i++) {
				if (i != 0) {
					this.result.append(ENTRY_SEPARATOR_WITH_SPACE);
				}

				this.formatSubElement(element.get(i), false);
			}

			this.result.append("]");
		}
		else {
			this.result.append("[");
			if (!this.prefix.isEmpty()) {
				this.result.append("\n");
			}

			String string = Strings.repeat(this.prefix, this.indentationLevel + 1);

			for (int j = 0; j < element.size() && j < MAX_ARRAY_DISPLAY_SIZE; j++) {
				this.result.append(string);
				this.formatSubElement(element.get(j), true);
				if (j != element.size() - 1) {
					this.result.append(
							this.prefix.isEmpty() ? ENTRY_SEPARATOR_WITH_SPACE : ENTRY_SEPARATOR_WITH_NEW_LINE);
				}
			}

			if (element.size() > MAX_ARRAY_DISPLAY_SIZE) {
				this.result.append(string).append(ELLIPSIS);
			}

			if (!this.prefix.isEmpty()) {
				this.result.append("\n" + Strings.repeat(this.prefix, this.indentationLevel));
			}

			this.result.append("]");
		}
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		if (compound.isEmpty()) {
			this.result.append("{}");
		}
		else if (this.depth >= 64) {
			this.result.append("{").append(ELLIPSIS).append("}");
		}
		else {
			this.result.append("{");
			Collection<String> collection = compound.getKeys();
			if (LOGGER.isDebugEnabled()) {
				List<String> list = Lists.newArrayList(compound.getKeys());
				Collections.sort(list);
				collection = list;
			}

			if (!this.prefix.isEmpty()) {
				this.result.append("\n");
			}

			String string = Strings.repeat(this.prefix, this.indentationLevel + 1);
			Iterator<String> iterator = collection.iterator();

			while (iterator.hasNext()) {
				String string2 = iterator.next();
				this.result.append(string).append(escapeName(string2)).append(": ");
				this.formatSubElement(compound.get(string2), true);
				if (iterator.hasNext()) {
					this.result.append(
							this.prefix.isEmpty() ? ENTRY_SEPARATOR_WITH_SPACE : ENTRY_SEPARATOR_WITH_NEW_LINE);
				}
			}

			if (!this.prefix.isEmpty()) {
				this.result.append("\n" + Strings.repeat(this.prefix, this.indentationLevel));
			}

			this.result.append("}");
		}
	}

	private void formatSubElement(NbtElement element, boolean indent) {
		if (indent) {
			this.indentationLevel++;
		}

		this.depth++;

		try {
			element.accept(this);
		}
		finally {
			if (indent) {
				this.indentationLevel--;
			}

			this.depth--;
		}
	}

	protected static Text escapeName(String name) {
		if (SIMPLE_NAME.matcher(name).matches()) {
			return Text.literal(name).formatted(NAME_COLOR);
		}
		else {
			String string = NbtString.escape(name);
			String string2 = string.substring(0, 1);
			Text text = Text.literal(string.substring(1, string.length() - 1)).formatted(NAME_COLOR);
			return Text.literal(string2).append(text).append(string2);
		}
	}

	@Override
	public void visitEnd(NbtEnd element) {
	}
}

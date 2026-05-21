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
 * {@code NbtOrderedStringFormatter}.
 */
public class NbtOrderedStringFormatter implements NbtElementVisitor {

	private static final Map<String, List<String>> ENTRY_ORDER_OVERRIDES = Util.make(
			Maps.newHashMap(), map -> {
				map.put(
						"{}",
						Lists.newArrayList(new String[]{
								"DataVersion",
								"author",
								"size",
								"data",
								"entities",
								"palette",
								"palettes"
						})
				);
				map.put("{}.data.[].{}", Lists.newArrayList(new String[]{"pos", "state", "nbt"}));
				map.put("{}.entities.[].{}", Lists.newArrayList(new String[]{"blockPos", "pos"}));
			}
	);
	private static final Set<String>
			IGNORED_PATHS =
			Sets.newHashSet(new String[]{"{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"});
	private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z0-9._+-]+");
	private static final String KEY_VALUE_SEPARATOR = String.valueOf(':');
	private static final String ENTRY_SEPARATOR = String.valueOf(',');
	private static final String SQUARE_OPEN_BRACKET = "[";
	private static final String SQUARE_CLOSE_BRACKET = "]";
	private static final String SEMICOLON = ";";
	private static final String SPACE = " ";
	private static final String CURLY_OPEN_BRACKET = "{";
	private static final String CURLY_CLOSE_BRACKET = "}";
	private static final String NEW_LINE = "\n";
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
	 * Apply.
	 *
	 * @param element element
	 *
	 * @return String — результат операции
	 */
	public String apply(NbtElement element) {
		element.accept(this);
		return this.result;
	}

	@Override
	public void visitString(NbtString element) {
		this.result = NbtString.escape(element.value());
	}

	@Override
	public void visitByte(NbtByte element) {
		this.result = element.value() + "b";
	}

	@Override
	public void visitShort(NbtShort element) {
		this.result = element.value() + "s";
	}

	@Override
	public void visitInt(NbtInt element) {
		this.result = String.valueOf(element.value());
	}

	@Override
	public void visitLong(NbtLong element) {
		this.result = element.value() + "L";
	}

	@Override
	public void visitFloat(NbtFloat element) {
		this.result = element.value() + "f";
	}

	@Override
	public void visitDouble(NbtDouble element) {
		this.result = element.value() + "d";
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		StringBuilder stringBuilder = new StringBuilder("[").append("B").append(";");
		byte[] bs = element.getByteArray();

		for (int i = 0; i < bs.length; i++) {
			stringBuilder.append(" ").append(bs[i]).append("B");
			if (i != bs.length - 1) {
				stringBuilder.append(ENTRY_SEPARATOR);
			}
		}

		stringBuilder.append("]");
		this.result = stringBuilder.toString();
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		StringBuilder stringBuilder = new StringBuilder("[").append("I").append(";");
		int[] is = element.getIntArray();

		for (int i = 0; i < is.length; i++) {
			stringBuilder.append(" ").append(is[i]);
			if (i != is.length - 1) {
				stringBuilder.append(ENTRY_SEPARATOR);
			}
		}

		stringBuilder.append("]");
		this.result = stringBuilder.toString();
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		String string = "L";
		StringBuilder stringBuilder = new StringBuilder("[").append("L").append(";");
		long[] ls = element.getLongArray();

		for (int i = 0; i < ls.length; i++) {
			stringBuilder.append(" ").append(ls[i]).append("L");
			if (i != ls.length - 1) {
				stringBuilder.append(ENTRY_SEPARATOR);
			}
		}

		stringBuilder.append("]");
		this.result = stringBuilder.toString();
	}

	@Override
	public void visitList(NbtList element) {
		if (element.isEmpty()) {
			this.result = "[]";
		}
		else {
			StringBuilder stringBuilder = new StringBuilder("[");
			this.pushPathPart("[]");
			String string = IGNORED_PATHS.contains(this.joinPath()) ? "" : this.prefix;
			if (!string.isEmpty()) {
				stringBuilder.append("\n");
			}

			for (int i = 0; i < element.size(); i++) {
				stringBuilder.append(Strings.repeat(string, this.indentationLevel + 1));
				stringBuilder.append(new NbtOrderedStringFormatter(
						string,
						this.indentationLevel + 1,
						this.pathParts
				).apply(element.get(i)));
				if (i != element.size() - 1) {
					stringBuilder.append(ENTRY_SEPARATOR).append(string.isEmpty() ? " " : "\n");
				}
			}

			if (!string.isEmpty()) {
				stringBuilder.append("\n").append(Strings.repeat(string, this.indentationLevel));
			}

			stringBuilder.append("]");
			this.result = stringBuilder.toString();
			this.popPathPart();
		}
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		if (compound.isEmpty()) {
			this.result = "{}";
		}
		else {
			StringBuilder stringBuilder = new StringBuilder("{");
			this.pushPathPart("{}");
			String string = IGNORED_PATHS.contains(this.joinPath()) ? "" : this.prefix;
			if (!string.isEmpty()) {
				stringBuilder.append("\n");
			}

			Collection<String> collection = this.getSortedNames(compound);
			Iterator<String> iterator = collection.iterator();

			while (iterator.hasNext()) {
				String string2 = iterator.next();
				NbtElement nbtElement = compound.get(string2);
				this.pushPathPart(string2);
				stringBuilder.append(Strings.repeat(string, this.indentationLevel + 1))
				             .append(escapeName(string2))
				             .append(KEY_VALUE_SEPARATOR)
				             .append(" ")
				             .append(new NbtOrderedStringFormatter(
						             string,
						             this.indentationLevel + 1,
						             this.pathParts
				             ).apply(nbtElement));
				this.popPathPart();
				if (iterator.hasNext()) {
					stringBuilder.append(ENTRY_SEPARATOR).append(string.isEmpty() ? " " : "\n");
				}
			}

			if (!string.isEmpty()) {
				stringBuilder.append("\n").append(Strings.repeat(string, this.indentationLevel));
			}

			stringBuilder.append("}");
			this.result = stringBuilder.toString();
			this.popPathPart();
		}
	}

	private void popPathPart() {
		this.pathParts.remove(this.pathParts.size() - 1);
	}

	private void pushPathPart(String part) {
		this.pathParts.add(part);
	}

	protected List<String> getSortedNames(NbtCompound compound) {
		Set<String> set = Sets.newHashSet(compound.getKeys());
		List<String> list = Lists.newArrayList();
		List<String> list2 = ENTRY_ORDER_OVERRIDES.get(this.joinPath());
		if (list2 != null) {
			for (String string : list2) {
				if (set.remove(string)) {
					list.add(string);
				}
			}

			if (!set.isEmpty()) {
				set.stream().sorted().forEach(list::add);
			}
		}
		else {
			list.addAll(set);
			Collections.sort(list);
		}

		return list;
	}

	/**
	 * Join path.
	 *
	 * @return String — результат операции
	 */
	public String joinPath() {
		return String.join(".", this.pathParts);
	}

	/**
	 * Escape name.
	 *
	 * @param name name
	 *
	 * @return String — результат операции
	 */
	protected static String escapeName(String name) {
		return SIMPLE_NAME.matcher(name).matches() ? name : NbtString.escape(name);
	}

	@Override
	public void visitEnd(NbtEnd element) {
	}
}

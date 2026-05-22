package net.minecraft.command.argument;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Тип аргумента команды для разбора пути в NBT-структуре.
 *
 * <p>Поддерживаемые узлы пути:
 * <ul>
 *   <li>{@code foo} — именованный дочерний элемент компаунда</li>
 *   <li>{@code foo.bar} — вложенный путь</li>
 *   <li>{@code foo[0]} — элемент списка по индексу</li>
 *   <li>{@code foo[]} — все элементы списка</li>
 *   <li>{@code foo[{key=val}]} — элементы списка, соответствующие фильтру</li>
 *   <li>{@code {key=val}} — корневой фильтр (только в начале пути)</li>
 * </ul>
 */
public class NbtPathArgumentType implements ArgumentType<NbtPathArgumentType.NbtPath> {

	private static final Collection<String> EXAMPLES =
			Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");

	public static final SimpleCommandExceptionType INVALID_PATH_NODE_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("arguments.nbtpath.node.invalid"));

	public static final SimpleCommandExceptionType TOO_DEEP_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("arguments.nbtpath.too_deep"));

	public static final DynamicCommandExceptionType NOTHING_FOUND_EXCEPTION = new DynamicCommandExceptionType(
			path -> Text.stringifiedTranslatable("arguments.nbtpath.nothing_found", path)
	);

	static final DynamicCommandExceptionType EXPECTED_LIST_EXCEPTION = new DynamicCommandExceptionType(
			nbt -> Text.stringifiedTranslatable("commands.data.modify.expected_list", nbt)
	);

	static final DynamicCommandExceptionType INVALID_INDEX_EXCEPTION = new DynamicCommandExceptionType(
			index -> Text.stringifiedTranslatable("commands.data.modify.invalid_index", index)
	);

	private static final char LEFT_SQUARE_BRACKET = '[';
	private static final char RIGHT_SQUARE_BRACKET = ']';
	private static final char LEFT_CURLY_BRACKET = '{';
	private static final char RIGHT_CURLY_BRACKET = '}';
	private static final char DOUBLE_QUOTE = '"';
	private static final char SINGLE_QUOTE = '\'';

	public static NbtPathArgumentType nbtPath() {
		return new NbtPathArgumentType();
	}

	public static NbtPathArgumentType.NbtPath getNbtPath(CommandContext<ServerCommandSource> context, String name) {
		return (NbtPathArgumentType.NbtPath) context.getArgument(name, NbtPathArgumentType.NbtPath.class);
	}

	@Override
	public NbtPathArgumentType.NbtPath parse(StringReader reader) throws CommandSyntaxException {
		List<NbtPathArgumentType.PathNode> nodes = Lists.newArrayList();
		int start = reader.getCursor();
		Object2IntMap<NbtPathArgumentType.PathNode> nodeEndIndices = new Object2IntOpenHashMap<>();
		boolean isRoot = true;

		while (reader.canRead() && reader.peek() != ' ') {
			NbtPathArgumentType.PathNode node = parseNode(reader, isRoot);
			nodes.add(node);
			nodeEndIndices.put(node, reader.getCursor() - start);
			isRoot = false;

			if (reader.canRead()) {
				char next = reader.peek();

				if (next != ' ' && next != LEFT_SQUARE_BRACKET && next != LEFT_CURLY_BRACKET) {
					reader.expect('.');
				}

			}

		}

		return new NbtPathArgumentType.NbtPath(
				reader.getString().substring(start, reader.getCursor()),
				nodes.toArray(new NbtPathArgumentType.PathNode[0]),
				nodeEndIndices
		);
	}

	private static NbtPathArgumentType.PathNode parseNode(StringReader reader, boolean root)
	throws CommandSyntaxException {
		return switch (reader.peek()) {
			case DOUBLE_QUOTE, SINGLE_QUOTE -> readCompoundChildNode(reader, reader.readString());
			case LEFT_SQUARE_BRACKET -> {
				reader.skip();
				int next = reader.peek();

				if (next == LEFT_CURLY_BRACKET) {
					NbtCompound filter = StringNbtReader.readCompoundAsArgument(reader);
					reader.expect(RIGHT_SQUARE_BRACKET);
					yield new NbtPathArgumentType.FilteredListElementNode(filter);
				}

				if (next == RIGHT_SQUARE_BRACKET) {
					reader.skip();
					yield NbtPathArgumentType.AllListElementNode.INSTANCE;
				}

				int index = reader.readInt();
				reader.expect(RIGHT_SQUARE_BRACKET);
				yield new NbtPathArgumentType.IndexedListElementNode(index);
			}
			case LEFT_CURLY_BRACKET -> {
				if (!root) {
					throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
				}

				NbtCompound filter = StringNbtReader.readCompoundAsArgument(reader);
				yield new NbtPathArgumentType.FilteredRootNode(filter);
			}
			default -> readCompoundChildNode(reader, readName(reader));
		};
	}

	private static NbtPathArgumentType.PathNode readCompoundChildNode(StringReader reader, String name)
	throws CommandSyntaxException {
		if (name.isEmpty()) {
			throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
		}

		if (reader.canRead() && reader.peek() == LEFT_CURLY_BRACKET) {
			NbtCompound filter = StringNbtReader.readCompoundAsArgument(reader);
			return new NbtPathArgumentType.FilteredNamedNode(name, filter);
		}

		return new NbtPathArgumentType.NamedNode(name);
	}

	private static String readName(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();

		while (reader.canRead() && isNameCharacter(reader.peek())) {
			reader.skip();
		}

		if (reader.getCursor() == start) {
			throw INVALID_PATH_NODE_EXCEPTION.createWithContext(reader);
		}

		return reader.getString().substring(start, reader.getCursor());
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static boolean isNameCharacter(char c) {
		return c != ' '
				&& c != DOUBLE_QUOTE
				&& c != SINGLE_QUOTE
				&& c != LEFT_SQUARE_BRACKET
				&& c != RIGHT_SQUARE_BRACKET
				&& c != '.'
				&& c != LEFT_CURLY_BRACKET
				&& c != RIGHT_CURLY_BRACKET;
	}

	static Predicate<NbtElement> getPredicate(NbtCompound filter) {
		return nbt -> NbtHelper.matches(filter, nbt, true);
	}

	// -------------------------------------------------------------------------
	// Реализации узлов пути
	// -------------------------------------------------------------------------

	static class AllListElementNode implements NbtPathArgumentType.PathNode {

		public static final NbtPathArgumentType.AllListElementNode INSTANCE = new NbtPathArgumentType.AllListElementNode();

		private AllListElementNode() {
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof AbstractNbtList list) {
				Iterables.addAll(results, list);
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (!(current instanceof AbstractNbtList list)) {
				return;
			}

			if (list.isEmpty()) {
				NbtElement element = source.get();

				if (list.addElement(0, element)) {
					results.add(element);
				}

			}
			else {
				Iterables.addAll(results, list);
			}

		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof AbstractNbtList list)) {
				return 0;
			}

			int size = list.size();

			if (size == 0) {
				list.addElement(0, source.get());
				return 1;
			}

			NbtElement newElement = source.get();
			int changed = size - (int) list.stream().filter(newElement::equals).count();

			if (changed == 0) {
				return 0;
			}

			list.clear();

			if (!list.addElement(0, newElement)) {
				return 0;
			}

			for (int i = 1; i < size; i++) {
				list.addElement(i, source.get());
			}

			return changed;
		}

		@Override
		public int clear(NbtElement current) {
			if (!(current instanceof AbstractNbtList list)) {
				return 0;
			}

			int size = list.size();

			if (size == 0) {
				return 0;
			}

			list.clear();
			return size;
		}

	}

	static class FilteredListElementNode implements NbtPathArgumentType.PathNode {

		private final NbtCompound filter;
		private final Predicate<NbtElement> predicate;

		public FilteredListElementNode(NbtCompound filter) {
			this.filter = filter;
			predicate = NbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtList list) {
				list.stream().filter(predicate).forEach(results::add);
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (!(current instanceof NbtList list)) {
				return;
			}

			MutableBoolean found = new MutableBoolean();
			list.stream().filter(predicate).forEach(nbt -> {
				results.add(nbt);
				found.setTrue();
			});

			if (found.isFalse()) {
				NbtCompound copy = filter.copy();
				list.add(copy);
				results.add(copy);
			}

		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof NbtList list)) {
				return 0;
			}

			int count = 0;
			int size = list.size();

			if (size == 0) {
				list.add(source.get());
				return 1;
			}

			for (int i = 0; i < size; i++) {
				NbtElement existing = list.get(i);

				if (predicate.test(existing)) {
					NbtElement replacement = source.get();

					if (!replacement.equals(existing) && list.setElement(i, replacement)) {
						count++;
					}

				}

			}

			return count;
		}

		@Override
		public int clear(NbtElement current) {
			if (!(current instanceof NbtList list)) {
				return 0;
			}

			int count = 0;

			for (int i = list.size() - 1; i >= 0; i--) {
				if (predicate.test(list.get(i))) {
					list.remove(i);
					count++;
				}

			}

			return count;
		}

	}

	static class FilteredNamedNode implements NbtPathArgumentType.PathNode {

		private final String name;
		private final NbtCompound filter;
		private final Predicate<NbtElement> predicate;

		public FilteredNamedNode(String name, NbtCompound filter) {
			this.name = name;
			this.filter = filter;
			predicate = NbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (!(current instanceof NbtCompound compound)) {
				return;
			}

			NbtElement element = compound.get(name);

			if (predicate.test(element)) {
				results.add(element);
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (!(current instanceof NbtCompound compound)) {
				return;
			}

			NbtElement element = compound.get(name);

			if (element == null) {
				NbtElement copy = filter.copy();
				compound.put(name, copy);
				results.add(copy);
			}
			else if (predicate.test(element)) {
				results.add(element);
			}

		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof NbtCompound compound)) {
				return 0;
			}

			NbtElement existing = compound.get(name);

			if (predicate.test(existing)) {
				NbtElement replacement = source.get();

				if (!replacement.equals(existing)) {
					compound.put(name, replacement);
					return 1;
				}

			}

			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			if (!(current instanceof NbtCompound compound)) {
				return 0;
			}

			NbtElement element = compound.get(name);

			if (predicate.test(element)) {
				compound.remove(name);
				return 1;
			}

			return 0;
		}

	}

	static class FilteredRootNode implements NbtPathArgumentType.PathNode {

		private final Predicate<NbtElement> matcher;

		public FilteredRootNode(NbtCompound filter) {
			matcher = NbtPathArgumentType.getPredicate(filter);
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (current instanceof NbtCompound && matcher.test(current)) {
				results.add(current);
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			get(current, results);
		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			return 0;
		}

	}

	static class IndexedListElementNode implements NbtPathArgumentType.PathNode {

		private final int index;

		public IndexedListElementNode(int index) {
			this.index = index;
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (!(current instanceof AbstractNbtList list)) {
				return;
			}

			int size = list.size();
			int resolved = index < 0 ? size + index : index;

			if (0 <= resolved && resolved < size) {
				results.add(list.get(resolved));
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			get(current, results);
		}

		@Override
		public NbtElement init() {
			return new NbtList();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof AbstractNbtList list)) {
				return 0;
			}

			int size = list.size();
			int resolved = index < 0 ? size + index : index;

			if (0 <= resolved && resolved < size) {
				NbtElement existing = list.get(resolved);
				NbtElement replacement = source.get();

				if (!replacement.equals(existing) && list.setElement(resolved, replacement)) {
					return 1;
				}

			}

			return 0;
		}

		@Override
		public int clear(NbtElement current) {
			if (!(current instanceof AbstractNbtList list)) {
				return 0;
			}

			int size = list.size();
			int resolved = index < 0 ? size + index : index;

			if (0 <= resolved && resolved < size) {
				list.remove(resolved);
				return 1;
			}

			return 0;
		}

	}

	static class NamedNode implements NbtPathArgumentType.PathNode {

		private final String name;

		public NamedNode(String name) {
			this.name = name;
		}

		@Override
		public void get(NbtElement current, List<NbtElement> results) {
			if (!(current instanceof NbtCompound compound)) {
				return;
			}

			NbtElement element = compound.get(name);

			if (element != null) {
				results.add(element);
			}

		}

		@Override
		public void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results) {
			if (!(current instanceof NbtCompound compound)) {
				return;
			}

			NbtElement element;

			if (compound.contains(name)) {
				element = compound.get(name);
			}
			else {
				element = source.get();
				compound.put(name, element);
			}

			results.add(element);
		}

		@Override
		public NbtElement init() {
			return new NbtCompound();
		}

		@Override
		public int set(NbtElement current, Supplier<NbtElement> source) {
			if (!(current instanceof NbtCompound compound)) {
				return 0;
			}

			NbtElement newElement = source.get();
			NbtElement previous = compound.put(name, newElement);
			return newElement.equals(previous) ? 0 : 1;
		}

		@Override
		public int clear(NbtElement current) {
			if (current instanceof NbtCompound compound && compound.contains(name)) {
				compound.remove(name);
				return 1;
			}

			return 0;
		}

	}

	/**
	 * Разобранный путь в NBT-структуре, состоящий из последовательности узлов.
	 * Поддерживает операции чтения, записи, вставки и удаления элементов.
	 */
	public static class NbtPath {

		private final String string;
		private final Object2IntMap<NbtPathArgumentType.PathNode> nodeEndIndices;
		private final NbtPathArgumentType.PathNode[] nodes;

		public static final Codec<NbtPathArgumentType.NbtPath> CODEC = Codec.STRING.comapFlatMap(
				path -> {
					try {
						NbtPathArgumentType.NbtPath parsed = new NbtPathArgumentType().parse(new StringReader(path));
						return DataResult.success(parsed);
					}
					catch (CommandSyntaxException ex) {
						return DataResult.error(() -> "Failed to parse path " + path + ": " + ex.getMessage());
					}
				},
				NbtPathArgumentType.NbtPath::getString
		);

		public static NbtPathArgumentType.NbtPath parse(String path) throws CommandSyntaxException {
			return new NbtPathArgumentType().parse(new StringReader(path));
		}

		public NbtPath(
				String string,
				NbtPathArgumentType.PathNode[] nodes,
				Object2IntMap<NbtPathArgumentType.PathNode> nodeEndIndices
		) {
			this.string = string;
			this.nodes = nodes;
			this.nodeEndIndices = nodeEndIndices;
		}

		public List<NbtElement> get(NbtElement element) throws CommandSyntaxException {
			List<NbtElement> results = Collections.singletonList(element);

			for (NbtPathArgumentType.PathNode node : nodes) {
				results = node.get(results);

				if (results.isEmpty()) {
					throw createNothingFoundException(node);
				}

			}

			return results;
		}

		public int count(NbtElement element) {
			List<NbtElement> results = Collections.singletonList(element);

			for (NbtPathArgumentType.PathNode node : nodes) {
				results = node.get(results);

				if (results.isEmpty()) {
					return 0;
				}

			}

			return results.size();
		}

		private List<NbtElement> getTerminals(NbtElement start) throws CommandSyntaxException {
			List<NbtElement> results = Collections.singletonList(start);

			for (int i = 0; i < nodes.length - 1; i++) {
				NbtPathArgumentType.PathNode node = nodes[i];
				results = node.getOrInit(results, nodes[i + 1]::init);

				if (results.isEmpty()) {
					throw createNothingFoundException(node);
				}

			}

			return results;
		}

		public List<NbtElement> getOrInit(NbtElement element, Supplier<NbtElement> source)
		throws CommandSyntaxException {
			List<NbtElement> terminals = getTerminals(element);
			NbtPathArgumentType.PathNode lastNode = nodes[nodes.length - 1];
			return lastNode.getOrInit(terminals, source);
		}

		private static int forEach(List<NbtElement> elements, Function<NbtElement, Integer> operation) {
			return elements.stream().map(operation).reduce(0, Integer::sum);
		}

		/**
		 * Проверяет, превышает ли глубина вложенности NBT-элемента допустимый предел (512).
		 * Используется перед записью, чтобы предотвратить переполнение стека при обходе.
		 */
		public static boolean isTooDeep(NbtElement element, int depth) {
			if (depth >= 512) {
				return true;
			}

			if (element instanceof NbtCompound compound) {
				for (NbtElement child : compound.values()) {
					if (isTooDeep(child, depth + 1)) {
						return true;
					}

				}

			}
			else if (element instanceof NbtList list) {
				for (NbtElement child : list) {
					if (isTooDeep(child, depth + 1)) {
						return true;
					}

				}

			}

			return false;
		}

		public int put(NbtElement element, NbtElement source) throws CommandSyntaxException {
			if (isTooDeep(source, getDepth())) {
				throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
			}

			NbtElement copy = source.copy();
			List<NbtElement> terminals = getTerminals(element);

			if (terminals.isEmpty()) {
				return 0;
			}

			NbtPathArgumentType.PathNode lastNode = nodes[nodes.length - 1];
			MutableBoolean firstUsed = new MutableBoolean(false);

			return forEach(
					terminals, nbt -> lastNode.set(
							nbt, () -> {
								if (firstUsed.isFalse()) {
									firstUsed.setTrue();
									return copy;
								}

								return copy.copy();
							}
					)
			);
		}

		private int getDepth() {
			return nodes.length;
		}

		public int insert(int index, NbtCompound compound, List<NbtElement> elements)
		throws CommandSyntaxException {
			List<NbtElement> copies = new ArrayList<>(elements.size());

			for (NbtElement element : elements) {
				NbtElement copy = element.copy();
				copies.add(copy);

				if (isTooDeep(copy, getDepth())) {
					throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
				}

			}

			Collection<NbtElement> targets = getOrInit(compound, NbtList::new);
			int insertCount = 0;
			boolean needCopy = false;

			for (NbtElement target : targets) {
				if (!(target instanceof AbstractNbtList list)) {
					throw NbtPathArgumentType.EXPECTED_LIST_EXCEPTION.create(target);
				}

				boolean inserted = false;
				int insertPos = index < 0 ? list.size() + index + 1 : index;

				for (NbtElement copy : copies) {
					try {
						if (list.addElement(insertPos, needCopy ? copy.copy() : copy)) {
							insertPos++;
							inserted = true;
						}

					}
					catch (IndexOutOfBoundsException ex) {
						throw NbtPathArgumentType.INVALID_INDEX_EXCEPTION.create(insertPos);
					}

				}

				needCopy = true;
				insertCount += inserted ? 1 : 0;
			}

			return insertCount;
		}

		public int remove(NbtElement element) {
			List<NbtElement> results = Collections.singletonList(element);

			for (int i = 0; i < nodes.length - 1; i++) {
				results = nodes[i].get(results);
			}

			NbtPathArgumentType.PathNode lastNode = nodes[nodes.length - 1];
			return forEach(results, lastNode::clear);
		}

		private CommandSyntaxException createNothingFoundException(NbtPathArgumentType.PathNode node) {
			int endIndex = nodeEndIndices.getInt(node);
			return NbtPathArgumentType.NOTHING_FOUND_EXCEPTION.create(string.substring(0, endIndex));
		}

		@Override
		public String toString() {
			return string;
		}

		public String getString() {
			return string;
		}

	}

	/**
	 * Узел пути NBT — атомарная операция навигации по NBT-структуре.
	 */
	interface PathNode {

		void get(NbtElement current, List<NbtElement> results);

		void getOrInit(NbtElement current, Supplier<NbtElement> source, List<NbtElement> results);

		NbtElement init();

		int set(NbtElement current, Supplier<NbtElement> source);

		int clear(NbtElement current);

		default List<NbtElement> get(List<NbtElement> elements) {
			return process(elements, this::get);
		}

		default List<NbtElement> getOrInit(List<NbtElement> elements, Supplier<NbtElement> supplier) {
			return process(elements, (current, results) -> getOrInit(current, supplier, results));
		}

		default List<NbtElement> process(List<NbtElement> elements, BiConsumer<NbtElement, List<NbtElement>> action) {
			List<NbtElement> results = Lists.newArrayList();

			for (NbtElement element : elements) {
				action.accept(element, results);
			}

			return results;
		}

	}

}

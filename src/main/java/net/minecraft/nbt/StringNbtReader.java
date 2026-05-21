package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.text.Text;
import net.minecraft.util.packrat.PackratParser;

/**
 * {@code StringNbtReader}.
 */
public class StringNbtReader<T> {

	public static final SimpleCommandExceptionType
			TRAILING =
			new SimpleCommandExceptionType(Text.translatable("argument.nbt.trailing"));
	public static final SimpleCommandExceptionType
			EXPECTED_COMPOUND =
			new SimpleCommandExceptionType(Text.translatable("argument.nbt.expected.compound"));
	public static final char COMMA = ',';
	public static final char COLON = ':';
	private static final StringNbtReader<NbtElement> DEFAULT_READER = fromOps(NbtOps.INSTANCE);
	public static final Codec<NbtCompound> STRINGIFIED_CODEC = Codec.STRING
			.comapFlatMap(
					snbt -> {
						try {
							NbtElement nbtElement = DEFAULT_READER.read(snbt);
							return nbtElement instanceof NbtCompound nbtCompound
							       ? DataResult.success(nbtCompound, Lifecycle.stable())
							       : DataResult.error(() -> "Expected compound tag, got " + nbtElement);
						}
						catch (CommandSyntaxException var3) {
							return DataResult.error(var3::getMessage);
						}
					},
					NbtCompound::toString
			);
	public static final Codec<NbtCompound>
			NBT_COMPOUND_CODEC =
			Codec.withAlternative(STRINGIFIED_CODEC, NbtCompound.CODEC);
	private final DynamicOps<T> ops;
	private final PackratParser<T> parser;

	private StringNbtReader(DynamicOps<T> ops, PackratParser<T> parser) {
		this.ops = ops;
		this.parser = parser;
	}

	public DynamicOps<T> getOps() {
		return this.ops;
	}

	/**
	 * From ops.
	 *
	 * @param ops ops
	 *
	 * @return StringNbtReader — результат операции
	 */
	public static <T> StringNbtReader<T> fromOps(DynamicOps<T> ops) {
		return new StringNbtReader<>(ops, SnbtParsing.createParser(ops));
	}

	private static NbtCompound expectCompound(StringReader reader, NbtElement nbtElement)
	throws CommandSyntaxException {
		if (nbtElement instanceof NbtCompound nbtCompound) {
			return nbtCompound;
		}
		else {
			throw EXPECTED_COMPOUND.createWithContext(reader);
		}
	}

	/**
	 * Читает compound.
	 *
	 * @param snbt snbt
	 *
	 * @return NbtCompound — результат операции
	 */
	public static NbtCompound readCompound(String snbt) throws CommandSyntaxException {
		StringReader stringReader = new StringReader(snbt);
		return expectCompound(stringReader, DEFAULT_READER.read(stringReader));
	}

	/**
	 * Read.
	 *
	 * @param snbt snbt
	 *
	 * @return T — результат операции
	 */
	public T read(String snbt) throws CommandSyntaxException {
		return this.read(new StringReader(snbt));
	}

	/**
	 * Read.
	 *
	 * @param reader reader
	 *
	 * @return T — результат операции
	 */
	public T read(StringReader reader) throws CommandSyntaxException {
		T object = this.parser.parse(reader);
		reader.skipWhitespace();
		if (reader.canRead()) {
			throw TRAILING.createWithContext(reader);
		}
		else {
			return object;
		}
	}

	/**
	 * Читает as argument.
	 *
	 * @param reader reader
	 *
	 * @return T — результат операции
	 */
	public T readAsArgument(StringReader reader) throws CommandSyntaxException {
		return this.parser.parse(reader);
	}

	/**
	 * Читает compound as argument.
	 *
	 * @param reader reader
	 *
	 * @return NbtCompound — результат операции
	 */
	public static NbtCompound readCompoundAsArgument(StringReader reader) throws CommandSyntaxException {
		NbtElement nbtElement = DEFAULT_READER.readAsArgument(reader);
		return expectCompound(reader, nbtElement);
	}
}

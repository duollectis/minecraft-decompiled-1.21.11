package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Содержимое текстового компонента, читающего данные из NBT-источника по пути.
 * Поддерживает интерпретацию NBT-значений как текстовых компонентов ({@code interpret=true})
 * и объединение нескольких значений через разделитель.
 */
public class NbtTextContent implements TextContent {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<NbtTextContent> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.STRING.fieldOf("nbt").forGetter(NbtTextContent::getPath),
			Codec.BOOL.lenientOptionalFieldOf("interpret", false).forGetter(NbtTextContent::shouldInterpret),
			TextCodecs.CODEC.lenientOptionalFieldOf("separator").forGetter(NbtTextContent::getSeparator),
			NbtDataSourceTypes.CODEC.forGetter(NbtTextContent::getDataSource)
		).apply(instance, NbtTextContent::new)
	);

	private final boolean interpret;
	private final Optional<Text> separator;
	private final String rawPath;
	private final NbtDataSource dataSource;
	protected final NbtPathArgumentType.@Nullable NbtPath path;

	public NbtTextContent(String rawPath, boolean interpret, Optional<Text> separator, NbtDataSource dataSource) {
		this(rawPath, parsePath(rawPath), interpret, separator, dataSource);
	}

	private NbtTextContent(
		String rawPath,
		NbtPathArgumentType.@Nullable NbtPath path,
		boolean interpret,
		Optional<Text> separator,
		NbtDataSource dataSource
	) {
		this.rawPath = rawPath;
		this.path = path;
		this.interpret = interpret;
		this.separator = separator;
		this.dataSource = dataSource;
	}

	/**
	 * Разбирает строку пути NBT. Возвращает {@code null} при синтаксической ошибке —
	 * в этом случае компонент будет возвращать пустой текст при рендеринге.
	 */
	private static NbtPathArgumentType.@Nullable NbtPath parsePath(String rawPath) {
		try {
			return new NbtPathArgumentType().parse(new StringReader(rawPath));
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	public String getPath() {
		return rawPath;
	}

	public boolean shouldInterpret() {
		return interpret;
	}

	public Optional<Text> getSeparator() {
		return separator;
	}

	public NbtDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof NbtTextContent other
			&& dataSource.equals(other.dataSource)
			&& separator.equals(other.separator)
			&& interpret == other.interpret
			&& rawPath.equals(other.rawPath);
	}

	@Override
	public int hashCode() {
		int hash = interpret ? 1 : 0;
		hash = 31 * hash + separator.hashCode();
		hash = 31 * hash + rawPath.hashCode();
		return 31 * hash + dataSource.hashCode();
	}

	@Override
	public String toString() {
		return "nbt{" + dataSource + ", interpreting=" + interpret + ", separator=" + separator + "}";
	}

	/**
	 * Разбирает NBT-данные из источника и формирует текстовый компонент.
	 * Если {@code interpret=true} — каждое NBT-значение декодируется как {@link Text}.
	 * Иначе — значения преобразуются в строки и объединяются разделителем.
	 */
	@Override
	public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		if (source == null || path == null) {
			return Text.empty();
		}

		Stream<NbtElement> nbtStream = dataSource.get(source).flatMap(nbt -> {
			try {
				return path.get(nbt).stream();
			} catch (CommandSyntaxException e) {
				return Stream.empty();
			}
		});

		if (interpret) {
			RegistryOps<NbtElement> registryOps = source.getRegistryManager().getOps(NbtOps.INSTANCE);
			Text separatorText = (Text) DataFixUtils.orElse(
				Texts.parse(source, separator, sender, depth),
				Texts.DEFAULT_SEPARATOR_TEXT
			);

			return nbtStream.flatMap(nbt -> {
				try {
					Text parsed = (Text) TextCodecs.CODEC.parse(registryOps, nbt).getOrThrow();
					return Stream.of(Texts.parse(source, parsed, sender, depth));
				} catch (Exception e) {
					LOGGER.warn("Failed to parse component: {}", nbt, e);
					return Stream.of();
				}
			}).reduce((acc, current) -> acc.append(separatorText).append(current)).orElseGet(Text::empty);
		}

		Stream<String> stringStream = nbtStream.map(NbtTextContent::nbtToString);

		return Texts.parse(source, separator, sender, depth)
			.map(sep -> stringStream
				.map(Text::literal)
				.reduce((acc, current) -> acc.append(sep).append(current))
				.orElseGet(Text::empty))
			.orElseGet(() -> Text.literal(stringStream.collect(Collectors.joining(", "))));
	}

	private static String nbtToString(NbtElement nbt) {
		return nbt instanceof NbtString(String value) ? value : nbt.toString();
	}

	@Override
	public MapCodec<NbtTextContent> getCodec() {
		return CODEC;
	}
}

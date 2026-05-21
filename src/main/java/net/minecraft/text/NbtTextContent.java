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
 * {@code NbtTextContent}.
 */
public class NbtTextContent implements TextContent {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<NbtTextContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codec.STRING.fieldOf("nbt").forGetter(NbtTextContent::getPath),
					                    Codec.BOOL.lenientOptionalFieldOf("interpret", false).forGetter(NbtTextContent::shouldInterpret),
					                    TextCodecs.CODEC.lenientOptionalFieldOf("separator").forGetter(NbtTextContent::getSeparator),
					                    NbtDataSourceTypes.CODEC.forGetter(NbtTextContent::getDataSource)
			                    )
			                    .apply(instance, NbtTextContent::new)
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

	private static NbtPathArgumentType.@Nullable NbtPath parsePath(String rawPath) {
		try {
			return new NbtPathArgumentType().parse(new StringReader(rawPath));
		}
		catch (CommandSyntaxException var2) {
			return null;
		}
	}

	public String getPath() {
		return this.rawPath;
	}

	public boolean shouldInterpret() {
		return this.interpret;
	}

	public Optional<Text> getSeparator() {
		return this.separator;
	}

	public NbtDataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	public boolean equals(Object o) {
		return this == o
		       ? true
		       : o instanceof NbtTextContent nbtTextContent
		         && this.dataSource.equals(nbtTextContent.dataSource)
		         && this.separator.equals(nbtTextContent.separator)
		         && this.interpret == nbtTextContent.interpret
		         && this.rawPath.equals(nbtTextContent.rawPath);
	}

	@Override
	public int hashCode() {
		int i = this.interpret ? 1 : 0;
		i = 31 * i + this.separator.hashCode();
		i = 31 * i + this.rawPath.hashCode();
		return 31 * i + this.dataSource.hashCode();
	}

	@Override
	public String toString() {
		return "nbt{" + this.dataSource + ", interpreting=" + this.interpret + ", separator=" + this.separator + "}";
	}

	@Override
	public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		if (source != null && this.path != null) {
			Stream<NbtElement> stream = this.dataSource.get(source).flatMap(nbt -> {
				try {
					return this.path.get(nbt).stream();
				}
				catch (CommandSyntaxException var3) {
					return Stream.empty();
				}
			});
			if (this.interpret) {
				RegistryOps<NbtElement> registryOps = source.getRegistryManager().getOps(NbtOps.INSTANCE);
				Text
						text =
						(Text) DataFixUtils.orElse(
								Texts.parse(source, this.separator, sender, depth),
								Texts.DEFAULT_SEPARATOR_TEXT
						);
				return stream.flatMap(nbt -> {
					try {
						Text textx = (Text) TextCodecs.CODEC.parse(registryOps, nbt).getOrThrow();
						return Stream.of(Texts.parse(source, textx, sender, depth));
					}
					catch (Exception var6x) {
						LOGGER.warn("Failed to parse component: {}", nbt, var6x);
						return Stream.of();
					}
				}).reduce((accumulator, current) -> accumulator.append(text).append(current)).orElseGet(Text::empty);
			}
			else {
				Stream<String> stream2 = stream.map(NbtTextContent::asString);
				return Texts.parse(source, this.separator, sender, depth)
				            .map(textx -> stream2
						            .map(Text::literal)
						            .reduce((accumulator, current) -> accumulator.append(textx).append(current))
						            .orElseGet(Text::empty))
				            .orElseGet(() -> Text.literal(stream2.collect(Collectors.joining(", "))));
			}
		}
		else {
			return Text.empty();
		}
	}

	private static String asString(NbtElement nbt) {
		return nbt instanceof NbtString(String var5) ? var5 : nbt.toString();
	}

	@Override
	public MapCodec<NbtTextContent> getCodec() {
		return CODEC;
	}
}

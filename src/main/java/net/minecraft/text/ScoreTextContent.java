package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Содержимое текстового компонента, отображающее значение счёта из таблицы результатов.
 *
 * <p>Поле {@code name} может быть либо {@link ParsedSelector} (для динамического выбора сущности),
 * либо строкой (для статического имени держателя счёта, например {@code "*"} для wildcard).
 * Поле {@code objective} задаёт имя цели в таблице результатов.</p>
 */
public record ScoreTextContent(Either<ParsedSelector, String> name, String objective) implements TextContent {

	public static final MapCodec<ScoreTextContent> INNER_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.either(ParsedSelector.CODEC, Codec.STRING).fieldOf("name").forGetter(ScoreTextContent::name),
					Codec.STRING.fieldOf("objective").forGetter(ScoreTextContent::objective)
			)
			.apply(instance, ScoreTextContent::new)
	);
	public static final MapCodec<ScoreTextContent> CODEC = INNER_CODEC.fieldOf("score");

	@Override
	public MapCodec<ScoreTextContent> getCodec() {
		return CODEC;
	}

	/**
	 * Определяет держателя счёта на основе источника команды.
	 *
	 * <p>Если имя задано через селектор — выполняет его и возвращает единственную сущность.
	 * Если сущностей несколько — бросает исключение. Если список пуст — возвращает
	 * статический держатель по строке селектора. Если имя задано строкой — использует её напрямую.</p>
	 */
	private ScoreHolder getScoreHolder(ServerCommandSource source) throws CommandSyntaxException {
		Optional<ParsedSelector> parsedSelector = name.left();

		if (parsedSelector.isPresent()) {
			List<? extends Entity> entities = parsedSelector.get().selector().getEntities(source);

			if (entities.isEmpty()) {
				return ScoreHolder.fromName(parsedSelector.get().raw());
			}

			if (entities.size() != 1) {
				throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
			}

			return entities.getFirst();
		}

		return ScoreHolder.fromName(name.right().orElseThrow());
	}

	private MutableText getScore(ScoreHolder scoreHolder, ServerCommandSource source) {
		MinecraftServer server = source.getServer();

		if (server == null) {
			return Text.empty();
		}

		Scoreboard scoreboard = server.getScoreboard();
		ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(objective);

		if (scoreboardObjective == null) {
			return Text.empty();
		}

		ReadableScoreboardScore score = scoreboard.getScore(scoreHolder, scoreboardObjective);

		return score != null
				? score.getFormattedScore(scoreboardObjective.getNumberFormatOr(StyledNumberFormat.EMPTY))
				: Text.empty();
	}

	@Override
	public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		if (source == null) {
			return Text.empty();
		}

		ScoreHolder scoreHolder = getScoreHolder(source);
		ScoreHolder resolved = sender != null && scoreHolder.equals(ScoreHolder.WILDCARD)
				? sender
				: scoreHolder;

		return getScore(resolved, source);
	}

	@Override
	public String toString() {
		return "score{name='" + name + "', objective='" + objective + "'}";
	}
}

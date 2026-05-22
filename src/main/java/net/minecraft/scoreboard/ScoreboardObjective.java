package net.minecraft.scoreboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.NumberFormatTypes;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Цель скорборда — именованный счётчик с критерием обновления и настройками отображения.
 * <p>
 * Каждая цель привязана к {@link Scoreboard} и уведомляет его об изменениях
 * через {@link Scoreboard#updateExistingObjective(ScoreboardObjective)}.
 */
public class ScoreboardObjective {

	private final Scoreboard scoreboard;
	private final String name;
	private final ScoreboardCriterion criterion;
	private Text displayName;
	private Text bracketedDisplayName;
	private ScoreboardCriterion.RenderType renderType;
	private boolean displayAutoUpdate;
	private @Nullable NumberFormat numberFormat;

	public ScoreboardObjective(
			Scoreboard scoreboard,
			String name,
			ScoreboardCriterion criterion,
			Text displayName,
			ScoreboardCriterion.RenderType renderType,
			boolean displayAutoUpdate,
			@Nullable NumberFormat numberFormat
	) {
		this.scoreboard = scoreboard;
		this.name = name;
		this.criterion = criterion;
		this.displayName = displayName;
		this.bracketedDisplayName = generateBracketedDisplayName();
		this.renderType = renderType;
		this.displayAutoUpdate = displayAutoUpdate;
		this.numberFormat = numberFormat;
	}

	public Packed pack() {
		return new Packed(
				name,
				criterion,
				displayName,
				renderType,
				displayAutoUpdate,
				Optional.ofNullable(numberFormat)
		);
	}

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	public String getName() {
		return name;
	}

	public ScoreboardCriterion getCriterion() {
		return criterion;
	}

	public Text getDisplayName() {
		return displayName;
	}

	public boolean shouldDisplayAutoUpdate() {
		return displayAutoUpdate;
	}

	public @Nullable NumberFormat getNumberFormat() {
		return numberFormat;
	}

	public NumberFormat getNumberFormatOr(NumberFormat fallback) {
		return Objects.requireNonNullElse(numberFormat, fallback);
	}

	/**
	 * Генерирует отображаемое имя в квадратных скобках с hover-подсказкой,
	 * показывающей внутреннее имя цели.
	 */
	private Text generateBracketedDisplayName() {
		return Texts.bracketed(
				displayName.copy()
						.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(name))))
		);
	}

	/**
	 * Возвращает отображаемое имя в квадратных скобках с hover-подсказкой.
	 * Используется в командах и интерфейсе для идентификации цели.
	 */
	public Text toHoverableText() {
		return bracketedDisplayName;
	}

	public void setDisplayName(Text displayName) {
		this.displayName = displayName;
		this.bracketedDisplayName = generateBracketedDisplayName();
		scoreboard.updateExistingObjective(this);
	}

	public ScoreboardCriterion.RenderType getRenderType() {
		return renderType;
	}

	public void setRenderType(ScoreboardCriterion.RenderType renderType) {
		this.renderType = renderType;
		scoreboard.updateExistingObjective(this);
	}

	public void setDisplayAutoUpdate(boolean displayAutoUpdate) {
		this.displayAutoUpdate = displayAutoUpdate;
		scoreboard.updateExistingObjective(this);
	}

	public void setNumberFormat(@Nullable NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		scoreboard.updateExistingObjective(this);
	}

	/**
	 * Упакованное представление цели для сериализации в NBT/JSON и сетевой передачи.
	 */
	public record Packed(
			String name,
			ScoreboardCriterion criteria,
			Text displayName,
			ScoreboardCriterion.RenderType renderType,
			boolean displayAutoUpdate,
			Optional<NumberFormat> numberFormat
	) {

		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.STRING.fieldOf("Name").forGetter(Packed::name),
						ScoreboardCriterion.CODEC
								.optionalFieldOf("CriteriaName", ScoreboardCriterion.DUMMY)
								.forGetter(Packed::criteria),
						TextCodecs.CODEC.fieldOf("DisplayName").forGetter(Packed::displayName),
						ScoreboardCriterion.RenderType.CODEC
								.optionalFieldOf("RenderType", ScoreboardCriterion.RenderType.INTEGER)
								.forGetter(Packed::renderType),
						Codec.BOOL
								.optionalFieldOf("display_auto_update", false)
								.forGetter(Packed::displayAutoUpdate),
						NumberFormatTypes.CODEC
								.optionalFieldOf("format")
								.forGetter(Packed::numberFormat)
				).apply(instance, Packed::new)
		);
	}
}

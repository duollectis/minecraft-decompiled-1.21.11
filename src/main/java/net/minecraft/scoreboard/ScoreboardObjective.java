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
 * {@code ScoreboardObjective}.
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
		this.bracketedDisplayName = this.generateBracketedDisplayName();
		this.renderType = renderType;
		this.displayAutoUpdate = displayAutoUpdate;
		this.numberFormat = numberFormat;
	}

	public ScoreboardObjective.Packed pack() {
		return new ScoreboardObjective.Packed(
				this.name,
				this.criterion,
				this.displayName,
				this.renderType,
				this.displayAutoUpdate,
				Optional.ofNullable(this.numberFormat)
		);
	}

	public Scoreboard getScoreboard() {
		return this.scoreboard;
	}

	public String getName() {
		return this.name;
	}

	public ScoreboardCriterion getCriterion() {
		return this.criterion;
	}

	public Text getDisplayName() {
		return this.displayName;
	}

	public boolean shouldDisplayAutoUpdate() {
		return this.displayAutoUpdate;
	}

	public @Nullable NumberFormat getNumberFormat() {
		return this.numberFormat;
	}

	public NumberFormat getNumberFormatOr(NumberFormat format) {
		return Objects.requireNonNullElse(this.numberFormat, format);
	}

	private Text generateBracketedDisplayName() {
		return Texts.bracketed(this.displayName
				.copy()
				.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(this.name)))));
	}

	public Text toHoverableText() {
		return this.bracketedDisplayName;
	}

	public void setDisplayName(Text name) {
		this.displayName = name;
		this.bracketedDisplayName = this.generateBracketedDisplayName();
		this.scoreboard.updateExistingObjective(this);
	}

	public ScoreboardCriterion.RenderType getRenderType() {
		return this.renderType;
	}

	public void setRenderType(ScoreboardCriterion.RenderType renderType) {
		this.renderType = renderType;
		this.scoreboard.updateExistingObjective(this);
	}

	public void setDisplayAutoUpdate(boolean displayAutoUpdate) {
		this.displayAutoUpdate = displayAutoUpdate;
		this.scoreboard.updateExistingObjective(this);
	}

	public void setNumberFormat(@Nullable NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		this.scoreboard.updateExistingObjective(this);
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(
			String name,
			ScoreboardCriterion criteria,
			Text displayName,
			ScoreboardCriterion.RenderType renderType,
			boolean displayAutoUpdate,
			Optional<NumberFormat> numberFormat
	) {

		public static final Codec<ScoreboardObjective.Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("Name").forGetter(ScoreboardObjective.Packed::name),
						                    ScoreboardCriterion.CODEC
								                    .optionalFieldOf("CriteriaName", ScoreboardCriterion.DUMMY)
								                    .forGetter(ScoreboardObjective.Packed::criteria),
						                    TextCodecs.CODEC.fieldOf("DisplayName").forGetter(ScoreboardObjective.Packed::displayName),
						                    ScoreboardCriterion.RenderType.CODEC
								                    .optionalFieldOf("RenderType", ScoreboardCriterion.RenderType.INTEGER)
								                    .forGetter(ScoreboardObjective.Packed::renderType),
						                    Codec.BOOL
								                    .optionalFieldOf("display_auto_update", false)
								                    .forGetter(ScoreboardObjective.Packed::displayAutoUpdate),
						                    NumberFormatTypes.CODEC
								                    .optionalFieldOf("format")
								                    .forGetter(ScoreboardObjective.Packed::numberFormat)
				                    )
				                    .apply(instance, ScoreboardObjective.Packed::new)
		);
	}
}

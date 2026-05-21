package net.minecraft.scoreboard;

import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * {@code ScoreboardEntry}.
 */
public record ScoreboardEntry(
		String owner,
		int value,
		@Nullable Text display,
		@Nullable NumberFormat numberFormatOverride
) {

	public boolean hidden() {
		return this.owner.startsWith("#");
	}

	public Text name() {
		return (Text) (this.display != null ? this.display : Text.literal(this.owner()));
	}

	public MutableText formatted(NumberFormat format) {
		return Objects.requireNonNullElse(this.numberFormatOverride, format).format(this.value);
	}
}

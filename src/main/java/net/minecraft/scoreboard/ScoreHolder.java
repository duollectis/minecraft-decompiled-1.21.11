package net.minecraft.scoreboard;

import com.mojang.authlib.GameProfile;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * {@code ScoreHolder}.
 */
public interface ScoreHolder {

	String WILDCARD_NAME = "*";

	ScoreHolder WILDCARD = new ScoreHolder() {
		@Override
		public String getNameForScoreboard() {
			return "*";
		}
	};

	String getNameForScoreboard();

	default @Nullable Text getDisplayName() {
		return null;
	}

	default Text getStyledDisplayName() {
		Text text = this.getDisplayName();
		return text != null
		       ? text
		         .copy()
		         .styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(this.getNameForScoreboard()))))
		       : Text.literal(this.getNameForScoreboard());
	}

	static ScoreHolder fromName(String name) {
		if (name.equals("*")) {
			return WILDCARD;
		}
		else {
			final Text text = Text.literal(name);
			return new ScoreHolder() {
				@Override
				public String getNameForScoreboard() {
					return name;
				}

				@Override
				public Text getStyledDisplayName() {
					return text;
				}
			};
		}
	}

	static ScoreHolder fromProfile(GameProfile gameProfile) {
		final String string = gameProfile.name();
		return new ScoreHolder() {
			@Override
			public String getNameForScoreboard() {
				return string;
			}
		};
	}
}

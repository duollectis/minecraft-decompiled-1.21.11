package net.minecraft.scoreboard;

import com.mojang.authlib.GameProfile;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Держатель очков скорборда — любой объект, который может иметь очки в системе скорборда.
 * <p>
 * Реализуется сущностями, игроками и псевдо-держателями (командные счётчики с префиксом {@code #}).
 * Специальный экземпляр {@link #WILDCARD} с именем {@code *} используется в командах
 * для обозначения «всех держателей».
 */
public interface ScoreHolder {

	String WILDCARD_NAME = "*";

	ScoreHolder WILDCARD = new ScoreHolder() {
		@Override
		public String getNameForScoreboard() {
			return WILDCARD_NAME;
		}
	};

	String getNameForScoreboard();

	default @Nullable Text getDisplayName() {
		return null;
	}

	/**
	 * Возвращает отображаемое имя с hover-подсказкой, показывающей внутреннее имя скорборда.
	 * Если отображаемое имя не задано — возвращает простой текстовый литерал.
	 */
	default Text getStyledDisplayName() {
		Text displayName = getDisplayName();
		return displayName != null
				? displayName.copy().styled(style -> style.withHoverEvent(
						new HoverEvent.ShowText(Text.literal(getNameForScoreboard()))
				))
				: Text.literal(getNameForScoreboard());
	}

	/**
	 * Создаёт держателя очков из строкового имени.
	 * Если имя равно {@code *} — возвращает {@link #WILDCARD}.
	 */
	static ScoreHolder fromName(String name) {
		if (name.equals(WILDCARD_NAME)) {
			return WILDCARD;
		}

		final Text nameText = Text.literal(name);
		return new ScoreHolder() {
			@Override
			public String getNameForScoreboard() {
				return name;
			}

			@Override
			public Text getStyledDisplayName() {
				return nameText;
			}
		};
	}

	/**
	 * Создаёт держателя очков из профиля игрока.
	 */
	static ScoreHolder fromProfile(GameProfile gameProfile) {
		final String profileName = gameProfile.name();
		return new ScoreHolder() {
			@Override
			public String getNameForScoreboard() {
				return profileName;
			}
		};
	}
}

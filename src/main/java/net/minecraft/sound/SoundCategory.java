package net.minecraft.sound;

/**
 * Категория звука, определяющая, к какой группе настроек громкости он относится.
 *
 * <p>Игрок может независимо регулировать громкость каждой категории
 * через настройки звука в меню игры.
 */
public enum SoundCategory {

	MASTER("master"),
	MUSIC("music"),
	RECORDS("record"),
	WEATHER("weather"),
	BLOCKS("block"),
	HOSTILE("hostile"),
	NEUTRAL("neutral"),
	PLAYERS("player"),
	AMBIENT("ambient"),
	VOICE("voice"),
	UI("ui");

	private final String name;

	SoundCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}

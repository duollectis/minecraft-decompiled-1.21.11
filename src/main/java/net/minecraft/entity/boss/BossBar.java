package net.minecraft.entity.boss;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

import java.util.UUID;

/**
 * Базовый класс полосы здоровья босса. Хранит визуальные параметры (цвет, стиль, флаги эффектов)
 * и процент заполнения. Конкретные реализации ({@link ServerBossBar}) добавляют сетевую синхронизацию.
 */
public abstract class BossBar {

	private final UUID uuid;
	protected Text name;
	protected float percent;
	protected Color color;
	protected Style style;
	protected boolean darkenSky;
	protected boolean dragonMusic;
	protected boolean thickenFog;

	public BossBar(UUID uuid, Text name, Color color, Style style) {
		this.uuid = uuid;
		this.name = name;
		this.color = color;
		this.style = style;
		this.percent = 1.0F;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Text getName() {
		return name;
	}

	public void setName(Text name) {
		this.name = name;
	}

	public float getPercent() {
		return percent;
	}

	public void setPercent(float percent) {
		this.percent = percent;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public boolean shouldDarkenSky() {
		return darkenSky;
	}

	public BossBar setDarkenSky(boolean darkenSky) {
		this.darkenSky = darkenSky;
		return this;
	}

	public boolean hasDragonMusic() {
		return dragonMusic;
	}

	public BossBar setDragonMusic(boolean dragonMusic) {
		this.dragonMusic = dragonMusic;
		return this;
	}

	public BossBar setThickenFog(boolean thickenFog) {
		this.thickenFog = thickenFog;
		return this;
	}

	public boolean shouldThickenFog() {
		return thickenFog;
	}

	/**
	 * Цвет полосы здоровья босса. Определяет форматирование текста в интерфейсе.
	 */
	public enum Color implements StringIdentifiable {
		PINK("pink", Formatting.RED),
		BLUE("blue", Formatting.BLUE),
		RED("red", Formatting.DARK_RED),
		GREEN("green", Formatting.GREEN),
		YELLOW("yellow", Formatting.YELLOW),
		PURPLE("purple", Formatting.DARK_BLUE),
		WHITE("white", Formatting.WHITE);

		public static final Codec<Color> CODEC = StringIdentifiable.createCodec(Color::values);

		private final String name;
		private final Formatting format;

		Color(String name, Formatting format) {
			this.name = name;
			this.format = format;
		}

		public Formatting getTextFormat() {
			return format;
		}

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Стиль (разметка) полосы здоровья босса: сплошная или с насечками.
	 */
	public enum Style implements StringIdentifiable {
		PROGRESS("progress"),
		NOTCHED_6("notched_6"),
		NOTCHED_10("notched_10"),
		NOTCHED_12("notched_12"),
		NOTCHED_20("notched_20");

		public static final Codec<Style> CODEC = StringIdentifiable.createCodec(Style::values);

		private final String name;

		Style(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}

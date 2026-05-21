package net.minecraft.world.rule;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code GameRuleCategory}.
 */
public record GameRuleCategory(Identifier id) {

	private static final List<GameRuleCategory> CATEGORIES = new ArrayList<>();
	public static final GameRuleCategory PLAYER = register("player");
	public static final GameRuleCategory MOBS = register("mobs");
	public static final GameRuleCategory SPAWNING = register("spawning");
	public static final GameRuleCategory DROPS = register("drops");
	public static final GameRuleCategory UPDATES = register("updates");
	public static final GameRuleCategory CHAT = register("chat");
	public static final GameRuleCategory MISC = register("misc");

	public Identifier getCategory() {
		return this.id;
	}

	private static GameRuleCategory register(String name) {
		return register(Identifier.ofVanilla(name));
	}

	/**
	 * Register.
	 *
	 * @param id id
	 *
	 * @return GameRuleCategory — результат операции
	 */
	public static GameRuleCategory register(Identifier id) {
		GameRuleCategory gameRuleCategory = new GameRuleCategory(id);
		if (CATEGORIES.contains(gameRuleCategory)) {
			throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", id));
		}
		else {
			CATEGORIES.add(gameRuleCategory);
			return gameRuleCategory;
		}
	}

	public MutableText getText() {
		return Text.translatable(this.id.toTranslationKey("gamerule.category"));
	}
}

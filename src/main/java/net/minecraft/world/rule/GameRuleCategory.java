package net.minecraft.world.rule;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Категория правила игры, используемая для группировки правил в интерфейсе и командах.
 * Все зарегистрированные категории хранятся в статическом списке {@code CATEGORIES}.
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
		return id;
	}

	private static GameRuleCategory register(String name) {
		return register(Identifier.ofVanilla(name));
	}

	/**
	 * Регистрирует новую категорию правил игры.
	 * Бросает исключение, если категория с таким идентификатором уже зарегистрирована.
	 *
	 * @param id идентификатор категории
	 * @return зарегистрированная категория
	 * @throws IllegalArgumentException если категория уже существует
	 */
	public static GameRuleCategory register(Identifier id) {
		GameRuleCategory category = new GameRuleCategory(id);

		if (CATEGORIES.contains(category)) {
			throw new IllegalArgumentException(
				String.format(Locale.ROOT, "Category '%s' is already registered.", id)
			);
		}

		CATEGORIES.add(category);
		return category;
	}

	public MutableText getText() {
		return Text.translatable(id.toTranslationKey("gamerule.category"));
	}
}

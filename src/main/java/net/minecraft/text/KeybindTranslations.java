package net.minecraft.text;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Реестр фабрики переводов клавиш управления.
 *
 * <p>По умолчанию возвращает литеральный текст с именем клавиши.
 * Клиентская часть игры заменяет фабрику через {@link #setFactory} на реализацию,
 * которая возвращает локализованное название из настроек управления.</p>
 */
public class KeybindTranslations {

	static Function<String, Supplier<Text>> factory = key -> () -> Text.literal(key);

	/**
	 * Устанавливает фабрику, преобразующую идентификатор клавиши в поставщик текста.
	 *
	 * <p>Вызывается клиентом при инициализации системы управления, чтобы подключить
	 * реальные локализованные названия клавиш вместо заглушки по умолчанию.</p>
	 *
	 * @param factory функция, принимающая идентификатор клавиши и возвращающая поставщик текста
	 */
	public static void setFactory(Function<String, Supplier<Text>> factory) {
		KeybindTranslations.factory = factory;
	}
}

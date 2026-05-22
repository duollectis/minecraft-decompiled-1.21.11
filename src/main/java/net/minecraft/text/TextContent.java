package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Базовый интерфейс содержимого текстового компонента.
 *
 * <p>Определяет три ключевых операции над содержимым:
 * <ul>
 *   <li>{@link #visit(StringVisitable.StyledVisitor, Style)} — обход с учётом стиля;</li>
 *   <li>{@link #visit(StringVisitable.Visitor)} — обход без стиля;</li>
 *   <li>{@link #parse(ServerCommandSource, Entity, int)} — разрешение динамического содержимого
 *       (селекторы, счёт, NBT) в контексте источника команды.</li>
 * </ul>
 * Все методы имеют реализации по умолчанию, возвращающие пустые значения,
 * что позволяет реализациям переопределять только нужные операции.</p>
 */
public interface TextContent {

	/**
	 * Обходит содержимое с учётом стиля.
	 *
	 * @param visitor посетитель, принимающий стиль и строку
	 * @param style текущий стиль, применяемый к содержимому
	 * @param <T> тип результата посетителя
	 * @return {@link Optional} с результатом, если посетитель прервал обход, иначе пустой
	 */
	default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return Optional.empty();
	}

	/**
	 * Обходит содержимое без учёта стиля.
	 *
	 * @param visitor посетитель, принимающий только строку
	 * @param <T> тип результата посетителя
	 * @return {@link Optional} с результатом, если посетитель прервал обход, иначе пустой
	 */
	default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		return Optional.empty();
	}

	/**
	 * Разрешает динамическое содержимое в контексте источника команды.
	 *
	 * <p>Используется для содержимого, зависящего от игрового состояния:
	 * селекторов сущностей, значений счёта, NBT-данных. Статическое содержимое
	 * (литералы, переводы) возвращает {@code MutableText.of(this)} без изменений.</p>
	 *
	 * @param source источник команды для разрешения контекстно-зависимых данных
	 * @param sender сущность-отправитель (может быть {@code null})
	 * @param depth текущая глубина рекурсии для защиты от бесконечных циклов
	 * @return разрешённый изменяемый текстовый компонент
	 * @throws CommandSyntaxException если разрешение селектора или NBT завершилось ошибкой
	 */
	default MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		return MutableText.of(this);
	}

	/** @return codec для сериализации конкретной реализации содержимого */
	MapCodec<? extends TextContent> getCodec();
}

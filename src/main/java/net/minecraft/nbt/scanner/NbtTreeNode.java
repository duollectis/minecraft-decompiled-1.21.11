package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

import java.util.HashMap;
import java.util.Map;

/**
 * Узел дерева запросов для {@link SelectiveNbtCollector} и {@link ExclusiveNbtCollector}.
 * <p>
 * Хранит два вида информации о текущем уровне вложенности:
 * <ul>
 *   <li>{@code selectedFields} — поля, которые нужно прочитать на этом уровне</li>
 *   <li>{@code fieldsToRecurse} — вложенные компаунды, в которые нужно войти для поиска</li>
 * </ul>
 *
 * @param depth           глубина этого узла в дереве (корень = 1)
 * @param selectedFields  поля для чтения: ключ → ожидаемый тип
 * @param fieldsToRecurse вложенные компаунды для рекурсивного обхода: ключ → дочерний узел
 */
public record NbtTreeNode(int depth, Map<String, NbtType<?>> selectedFields, Map<String, NbtTreeNode> fieldsToRecurse) {

	private NbtTreeNode(int depth) {
		this(depth, new HashMap<>(), new HashMap<>());
	}

	/**
	 * Создаёт корневой узел дерева запросов с глубиной 1.
	 *
	 * @return новый корневой узел
	 */
	public static NbtTreeNode createRoot() {
		return new NbtTreeNode(1);
	}

	/**
	 * Добавляет запрос в дерево.
	 * Если путь запроса ещё не исчерпан на текущей глубине — рекурсивно создаёт дочерний узел.
	 * Если путь исчерпан — регистрирует поле в {@code selectedFields}.
	 *
	 * @param query запрос для добавления
	 */
	public void add(NbtScanQuery query) {
		if (depth <= query.path().size()) {
			fieldsToRecurse
				.computeIfAbsent(query.path().get(depth - 1), ignored -> new NbtTreeNode(depth + 1))
				.add(query);
		}
		else {
			selectedFields.put(query.key(), query.type());
		}
	}

	/**
	 * Проверяет, совпадает ли тип поля с зарегистрированным в {@code selectedFields}.
	 *
	 * @param type тип для проверки
	 * @param key  ключ поля
	 * @return {@code true}, если тип совпадает
	 */
	public boolean isTypeEqual(NbtType<?> type, String key) {
		return type.equals(selectedFields().get(key));
	}
}

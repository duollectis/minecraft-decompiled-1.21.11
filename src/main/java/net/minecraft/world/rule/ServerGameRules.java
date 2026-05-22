package net.minecraft.world.rule;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.registry.Registries;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Низкоуровневое хранилище значений правил игры на сервере.
 * Использует {@link Reference2ObjectOpenHashMap} для эффективного хранения
 * пар {@code GameRule → значение} без лишних аллокаций.
 */
public final class ServerGameRules {

	@SuppressWarnings("unchecked")
	public static final Codec<ServerGameRules> CODEC = ((Codec<Map<GameRule<?>, Object>>) (Codec<?>) Codec.dispatchedMap(
		Registries.GAME_RULE.getCodec(), GameRule::getCodec
	)).xmap(ServerGameRules::of, ServerGameRules::getRuleValues);

	private final Reference2ObjectMap<GameRule<?>, Object> ruleValues;

	ServerGameRules(Reference2ObjectMap<GameRule<?>, Object> ruleValues) {
		this.ruleValues = ruleValues;
	}

	private static ServerGameRules of(Map<GameRule<?>, Object> ruleValues) {
		return new ServerGameRules(new Reference2ObjectOpenHashMap<>(ruleValues));
	}

	public static ServerGameRules of() {
		return new ServerGameRules(new Reference2ObjectOpenHashMap<>());
	}

	/**
	 * Создаёт {@link ServerGameRules} с дефолтными значениями для всех переданных правил.
	 *
	 * @param rules поток правил для инициализации
	 * @return новый экземпляр с дефолтными значениями
	 */
	public static ServerGameRules ofDefault(Stream<GameRule<?>> rules) {
		Reference2ObjectOpenHashMap<GameRule<?>, Object> map = new Reference2ObjectOpenHashMap<>();
		rules.forEach(rule -> map.put(rule, rule.getDefaultValue()));
		return new ServerGameRules(map);
	}

	/**
	 * Создаёт глубокую копию переданного экземпляра.
	 *
	 * @param rules исходный экземпляр
	 * @return независимая копия
	 */
	public static ServerGameRules copyOf(ServerGameRules rules) {
		return new ServerGameRules(new Reference2ObjectOpenHashMap<>(rules.ruleValues));
	}

	public boolean contains(GameRule<?> rule) {
		return ruleValues.containsKey(rule);
	}

	public <T> @Nullable T get(GameRule<T> rule) {
		return (T) ruleValues.get(rule);
	}

	public <T> void put(GameRule<T> rule, T value) {
		ruleValues.put(rule, value);
	}

	public <T> @Nullable T remove(GameRule<T> rule) {
		return (T) ruleValues.remove(rule);
	}

	public Set<GameRule<?>> keySet() {
		return ruleValues.keySet();
	}

	public int size() {
		return ruleValues.size();
	}

	@Override
	public String toString() {
		return ruleValues.toString();
	}

	/**
	 * Возвращает новый экземпляр, в котором значения из {@code override} перекрывают текущие.
	 *
	 * @param override правила-переопределения
	 * @return новый экземпляр с применёнными переопределениями
	 */
	public ServerGameRules withOverride(ServerGameRules override) {
		ServerGameRules copy = copyOf(this);
		copy.copyFrom(override, rule -> true);
		return copy;
	}

	/**
	 * Копирует значения из {@code rules} в текущий экземпляр, применяя фильтр предикатом.
	 *
	 * @param rules     источник значений
	 * @param predicate фильтр правил для копирования
	 */
	public void copyFrom(ServerGameRules rules, Predicate<GameRule<?>> predicate) {
		for (GameRule<?> rule : rules.keySet()) {
			if (predicate.test(rule)) {
				setFrom(rules, rule, this);
			}
		}
	}

	private static <T> void setFrom(ServerGameRules source, GameRule<T> rule, ServerGameRules target) {
		target.put(rule, Objects.requireNonNull(source.get(rule)));
	}

	private Map<GameRule<?>, Object> getRuleValues() {
		return Map.copyOf(ruleValues);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o == null || o.getClass() != getClass()) {
			return false;
		}

		ServerGameRules other = (ServerGameRules) o;
		return Objects.equals(ruleValues, other.ruleValues);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ruleValues);
	}

	/**
	 * Строитель для пошагового создания {@link ServerGameRules}.
	 */
	public static class Builder {

		final Reference2ObjectMap<GameRule<?>, Object> ruleValues = new Reference2ObjectOpenHashMap<>();

		public <T> Builder put(GameRule<T> rule, T value) {
			ruleValues.put(rule, value);
			return this;
		}

		public ServerGameRules build() {
			return new ServerGameRules(ruleValues);
		}
	}
}

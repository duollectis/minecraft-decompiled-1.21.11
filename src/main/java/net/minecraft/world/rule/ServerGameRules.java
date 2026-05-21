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
 * {@code ServerGameRules}.
 */
public final class ServerGameRules {

	@SuppressWarnings("unchecked")
	public static final Codec<ServerGameRules>
			CODEC =
			((Codec<Map<GameRule<?>, Object>>) (Codec<?>) Codec.dispatchedMap(
					Registries.GAME_RULE.getCodec(), GameRule::getCodec
			)
			).xmap(ServerGameRules::of, ServerGameRules::getRuleValues);
	private final Reference2ObjectMap<GameRule<?>, Object> ruleValues;

	ServerGameRules(Reference2ObjectMap<GameRule<?>, Object> ruleValues) {
		this.ruleValues = ruleValues;
	}

	private static ServerGameRules of(Map<GameRule<?>, Object> ruleValues) {
		return new ServerGameRules(new Reference2ObjectOpenHashMap(ruleValues));
	}

	public static ServerGameRules of() {
		return new ServerGameRules(new Reference2ObjectOpenHashMap());
	}

	public static ServerGameRules ofDefault(Stream<GameRule<?>> rules) {
		Reference2ObjectOpenHashMap<GameRule<?>, Object>
				reference2ObjectOpenHashMap =
				new Reference2ObjectOpenHashMap();
		rules.forEach(gameRule -> reference2ObjectOpenHashMap.put(gameRule, gameRule.getDefaultValue()));
		return new ServerGameRules(reference2ObjectOpenHashMap);
	}

	public static ServerGameRules copyOf(ServerGameRules rules) {
		return new ServerGameRules(new Reference2ObjectOpenHashMap(rules.ruleValues));
	}

	public boolean contains(GameRule<?> rule) {
		return this.ruleValues.containsKey(rule);
	}

	public <T> @Nullable T get(GameRule<T> rule) {
		return (T) this.ruleValues.get(rule);
	}

	public <T> void put(GameRule<T> rule, T value) {
		this.ruleValues.put(rule, value);
	}

	public <T> @Nullable T remove(GameRule<T> rule) {
		return (T) this.ruleValues.remove(rule);
	}

	public Set<GameRule<?>> keySet() {
		return this.ruleValues.keySet();
	}

	public int size() {
		return this.ruleValues.size();
	}

	@Override
	public String toString() {
		return this.ruleValues.toString();
	}

	public ServerGameRules withOverride(ServerGameRules override) {
		ServerGameRules serverGameRules = copyOf(this);
		serverGameRules.copyFrom(override, rule -> true);
		return serverGameRules;
	}

	public void copyFrom(ServerGameRules rules, Predicate<GameRule<?>> predicate) {
		for (GameRule<?> gameRule : rules.keySet()) {
			if (predicate.test(gameRule)) {
				setFrom(rules, gameRule, this);
			}
		}
	}

	private static <T> void setFrom(ServerGameRules oldRules, GameRule<T> rule, ServerGameRules newRules) {
		newRules.put(rule, Objects.requireNonNull(oldRules.get(rule)));
	}

	private Map<GameRule<?>, Object> getRuleValues() {
		return Map.copyOf(this.ruleValues);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else if (o != null && o.getClass() == this.getClass()) {
			ServerGameRules serverGameRules = (ServerGameRules) o;
			return Objects.equals(this.ruleValues, serverGameRules.ruleValues);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.ruleValues);
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		final Reference2ObjectMap<GameRule<?>, Object> ruleValues = new Reference2ObjectOpenHashMap();

		public <T> ServerGameRules.Builder put(GameRule<T> rule, T value) {
			this.ruleValues.put(rule, value);
			return this;
		}

		public ServerGameRules build() {
			return new ServerGameRules(this.ruleValues);
		}
	}
}

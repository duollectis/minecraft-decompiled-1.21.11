package net.minecraft.util.context;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Типобезопасная карта параметров контекста. Хранит значения, привязанные
 * к типизированным ключам {@link ContextParameter}, и проверяет соответствие
 * набора параметров заданному {@link ContextType} при построении.
 */
public class ContextParameterMap {

	private final Map<ContextParameter<?>, Object> map;

	ContextParameterMap(Map<ContextParameter<?>, Object> map) {
		this.map = map;
	}

	public boolean contains(ContextParameter<?> parameter) {
		return this.map.containsKey(parameter);
	}

	public <T> T getOrThrow(ContextParameter<T> parameter) {
		T object = (T) this.map.get(parameter);
		if (object == null) {
			throw new NoSuchElementException(parameter.getId().toString());
		}

		return object;
	}

	public <T> @Nullable T getNullable(ContextParameter<T> parameter) {
		return (T) this.map.get(parameter);
	}

	@Contract("_,!null->!null; _,_->_")
	public <T> @Nullable T getOrDefault(ContextParameter<T> parameter, @Nullable T defaultValue) {
		return (T) this.map.getOrDefault(parameter, defaultValue);
	}

	public static class Builder {

		private final Map<ContextParameter<?>, Object> map = new IdentityHashMap<>();

		public <T> ContextParameterMap.Builder add(ContextParameter<T> parameter, T value) {
			this.map.put(parameter, value);
			return this;
		}

		public <T> ContextParameterMap.Builder addNullable(ContextParameter<T> parameter, @Nullable T value) {
			if (value == null) {
				this.map.remove(parameter);
			}
			else {
				this.map.put(parameter, value);
			}

			return this;
		}

		public <T> T getOrThrow(ContextParameter<T> parameter) {
			T object = (T) this.map.get(parameter);
			if (object == null) {
				throw new NoSuchElementException(parameter.getId().toString());
			}

			return object;
		}

		public <T> @Nullable T getNullable(ContextParameter<T> parameter) {
			return (T) this.map.get(parameter);
		}

		public ContextParameterMap build(ContextType type) {
			Set<ContextParameter<?>> disallowed = Sets.difference(this.map.keySet(), type.getAllowed());
			if (!disallowed.isEmpty()) {
				throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + disallowed);
			}

			Set<ContextParameter<?>> missing = Sets.difference(type.getRequired(), this.map.keySet());
			if (!missing.isEmpty()) {
				throw new IllegalArgumentException("Missing required parameters: " + missing);
			}

			return new ContextParameterMap(this.map);
		}
	}
}

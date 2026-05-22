package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Описывает один атрибут окружения: его тип, значение по умолчанию,
 * правила валидации и флаги поведения (синхронизация, позиционность, интерполяция).
 * <p>
 * Атрибуты регистрируются в {@link net.minecraft.registry.Registries#ENVIRONMENTAL_ATTRIBUTE}
 * и используются для управления визуальными и игровыми параметрами биомов/измерений.
 *
 * @param <Value> тип значения атрибута
 */
public class EnvironmentAttribute<Value> {

	private final EnvironmentAttributeType<Value> type;
	private final Value defaultValue;
	private final AttributeValidator<Value> validator;
	private final boolean synced;
	private final boolean positional;
	private final boolean interpolated;

	EnvironmentAttribute(
		EnvironmentAttributeType<Value> type,
		Value defaultValue,
		AttributeValidator<Value> validator,
		boolean synced,
		boolean positional,
		boolean interpolated
	) {
		this.type = type;
		this.defaultValue = defaultValue;
		this.validator = validator;
		this.synced = synced;
		this.positional = positional;
		this.interpolated = interpolated;
	}

	/** Создаёт билдер для атрибута заданного типа. */
	public static <Value> Builder<Value> builder(EnvironmentAttributeType<Value> type) {
		return new Builder<>(type);
	}

	public EnvironmentAttributeType<Value> getType() {
		return type;
	}

	public Value getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Возвращает codec для значений этого атрибута с встроенной валидацией.
	 * Codec отклонит значения, не прошедшие {@link AttributeValidator#validate}.
	 */
	public Codec<Value> getCodec() {
		return type.valueCodec().validate(validator::validate);
	}

	/**
	 * Зажимает значение в допустимый диапазон согласно валидатору атрибута.
	 *
	 * @param value исходное значение
	 * @return значение в допустимом диапазоне
	 */
	public Value clamp(Value value) {
		return validator.clamp(value);
	}

	/** Синхронизируется ли атрибут с клиентом по сети. */
	public boolean isSynced() {
		return synced;
	}

	/**
	 * Является ли атрибут позиционным — зависящим от координат в мире.
	 * Позиционные атрибуты требуют передачи позиции при запросе значения.
	 */
	public boolean isPositional() {
		return positional;
	}

	/** Интерполируется ли атрибут между биомами при смешивании. */
	public boolean isInterpolated() {
		return interpolated;
	}

	@Override
	public String toString() {
		return Util.registryValueToString(Registries.ENVIRONMENTAL_ATTRIBUTE, this);
	}

	/**
	 * Билдер для создания {@link EnvironmentAttribute}.
	 * По умолчанию: без синхронизации, позиционный, без интерполяции.
	 *
	 * @param <Value> тип значения атрибута
	 */
	public static class Builder<Value> {

		private final EnvironmentAttributeType<Value> type;
		private @Nullable Value defaultValue;
		private AttributeValidator<Value> validator = AttributeValidator.all();
		private boolean synced = false;
		private boolean positional = true;
		private boolean interpolated = false;

		public Builder(EnvironmentAttributeType<Value> type) {
			this.type = type;
		}

		public Builder<Value> defaultValue(Value defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public Builder<Value> validator(AttributeValidator<Value> validator) {
			this.validator = validator;
			return this;
		}

		/** Помечает атрибут как синхронизируемый с клиентом. */
		public Builder<Value> synced() {
			this.synced = true;
			return this;
		}

		/** Помечает атрибут как глобальный (не зависящий от позиции). */
		public Builder<Value> global() {
			this.positional = false;
			return this;
		}

		/** Включает интерполяцию значения между соседними биомами. */
		public Builder<Value> interpolated() {
			this.interpolated = true;
			return this;
		}

		/**
		 * Собирает атрибут. Бросает {@link NullPointerException} если
		 * значение по умолчанию не было задано через {@link #defaultValue}.
		 */
		public EnvironmentAttribute<Value> build() {
			return new EnvironmentAttribute<>(
				type,
				Objects.requireNonNull(defaultValue, "Missing default value"),
				validator,
				synced,
				positional,
				interpolated
			);
		}
	}
}

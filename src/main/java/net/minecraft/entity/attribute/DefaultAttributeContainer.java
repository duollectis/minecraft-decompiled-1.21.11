package net.minecraft.entity.attribute;

import com.google.common.collect.ImmutableMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Неизменяемый контейнер атрибутов по умолчанию для конкретного типа сущности.
 * Создаётся один раз через {@link Builder} и используется как эталон при создании
 * переопределяемых экземпляров в {@link AttributeContainer}.
 */
public class DefaultAttributeContainer {

	private final Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> instances;

	DefaultAttributeContainer(Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> instances) {
		this.instances = instances;
	}

	/**
	 * Возвращает экземпляр атрибута или бросает исключение, если атрибут не зарегистрирован.
	 */
	private EntityAttributeInstance require(RegistryEntry<EntityAttribute> attribute) {
		EntityAttributeInstance instance = instances.get(attribute);
		if (instance == null) {
			throw new IllegalArgumentException("Can't find attribute " + attribute.getIdAsString());
		}

		return instance;
	}

	public double getValue(RegistryEntry<EntityAttribute> attribute) {
		return require(attribute).getValue();
	}

	public double getBaseValue(RegistryEntry<EntityAttribute> attribute) {
		return require(attribute).getBaseValue();
	}

	/**
	 * Возвращает значение конкретного модификатора атрибута или бросает исключение, если модификатор не найден.
	 */
	public double getModifierValue(RegistryEntry<EntityAttribute> attribute, Identifier id) {
		EntityAttributeModifier modifier = require(attribute).getModifier(id);
		if (modifier == null) {
			throw new IllegalArgumentException(
					"Can't find modifier " + id + " on attribute " + attribute.getIdAsString()
			);
		}

		return modifier.value();
	}

	/**
	 * Создаёт изменяемую копию экземпляра атрибута для конкретной сущности.
	 * Возвращает {@code null}, если атрибут не зарегистрирован в этом контейнере.
	 */
	public @Nullable EntityAttributeInstance createOverride(
			Consumer<EntityAttributeInstance> updateCallback,
			RegistryEntry<EntityAttribute> attribute
	) {
		EntityAttributeInstance base = instances.get(attribute);
		if (base == null) {
			return null;
		}

		EntityAttributeInstance override = new EntityAttributeInstance(attribute, updateCallback);
		override.setFrom(base);
		return override;
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean has(RegistryEntry<EntityAttribute> attribute) {
		return instances.containsKey(attribute);
	}

	public boolean hasModifier(RegistryEntry<EntityAttribute> attribute, Identifier id) {
		EntityAttributeInstance instance = instances.get(attribute);
		return instance != null && instance.getModifier(id) != null;
	}

	/**
	 * Строитель {@link DefaultAttributeContainer}. После вызова {@link #build()} контейнер
	 * становится неизменяемым — любые попытки изменить значения атрибутов бросят исключение.
	 */
	public static class Builder {

		private final ImmutableMap.Builder<RegistryEntry<EntityAttribute>, EntityAttributeInstance> instances =
				ImmutableMap.builder();
		private boolean unmodifiable;

		/**
		 * Создаёт экземпляр атрибута и добавляет его в строитель.
		 * Callback блокирует изменения после финализации контейнера.
		 */
		private EntityAttributeInstance checkedAdd(RegistryEntry<EntityAttribute> attribute) {
			EntityAttributeInstance instance = new EntityAttributeInstance(attribute, inst -> {
				if (unmodifiable) {
					throw new UnsupportedOperationException(
							"Tried to change value for default attribute instance: " + attribute.getIdAsString()
					);
				}
			});
			instances.put(attribute, instance);
			return instance;
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute) {
			checkedAdd(attribute);
			return this;
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, double baseValue) {
			checkedAdd(attribute).setBaseValue(baseValue);
			return this;
		}

		public DefaultAttributeContainer build() {
			unmodifiable = true;
			return new DefaultAttributeContainer(instances.buildKeepingLast());
		}
	}
}

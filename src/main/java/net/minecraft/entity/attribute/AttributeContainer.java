package net.minecraft.entity.attribute;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Контейнер атрибутов конкретной сущности. Хранит переопределённые (custom) экземпляры атрибутов
 * поверх базовых значений из {@link DefaultAttributeContainer}.
 * <p>
 * Отслеживает изменённые атрибуты для последующей синхронизации с клиентом.
 */
public class AttributeContainer {

	private final Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> custom = new Object2ObjectOpenHashMap<>();
	private final Set<EntityAttributeInstance> tracked = new ObjectOpenHashSet<>();
	private final Set<EntityAttributeInstance> pendingUpdate = new ObjectOpenHashSet<>();
	private final DefaultAttributeContainer defaultAttributes;

	public AttributeContainer(DefaultAttributeContainer defaultAttributes) {
		this.defaultAttributes = defaultAttributes;
	}

	private void updateTrackedStatus(EntityAttributeInstance instance) {
		pendingUpdate.add(instance);
		if (instance.getAttribute().value().isTracked()) {
			tracked.add(instance);
		}
	}

	public Set<EntityAttributeInstance> getTracked() {
		return tracked;
	}

	public Set<EntityAttributeInstance> getPendingUpdate() {
		return pendingUpdate;
	}

	public Collection<EntityAttributeInstance> getAttributesToSend() {
		return custom.values()
				.stream()
				.filter(instance -> instance.getAttribute().value().isTracked())
				.collect(Collectors.toList());
	}

	public @Nullable EntityAttributeInstance getCustomInstance(RegistryEntry<EntityAttribute> attribute) {
		return custom.computeIfAbsent(
				attribute,
				attr -> defaultAttributes.createOverride(this::updateTrackedStatus, attr)
		);
	}

	public boolean hasAttribute(RegistryEntry<EntityAttribute> attribute) {
		return custom.containsKey(attribute) || defaultAttributes.has(attribute);
	}

	public boolean hasModifierForAttribute(RegistryEntry<EntityAttribute> attribute, Identifier id) {
		EntityAttributeInstance instance = custom.get(attribute);
		return instance != null
				? instance.getModifier(id) != null
				: defaultAttributes.hasModifier(attribute, id);
	}

	public double getValue(RegistryEntry<EntityAttribute> attribute) {
		EntityAttributeInstance instance = custom.get(attribute);
		return instance != null ? instance.getValue() : defaultAttributes.getValue(attribute);
	}

	public double getBaseValue(RegistryEntry<EntityAttribute> attribute) {
		EntityAttributeInstance instance = custom.get(attribute);
		return instance != null ? instance.getBaseValue() : defaultAttributes.getBaseValue(attribute);
	}

	public double getModifierValue(RegistryEntry<EntityAttribute> attribute, Identifier id) {
		EntityAttributeInstance instance = custom.get(attribute);
		return instance != null
				? instance.getModifier(id).value()
				: defaultAttributes.getModifierValue(attribute, id);
	}

	/**
	 * Добавляет временные модификаторы из мультимапы. Перед добавлением удаляет старый модификатор
	 * с тем же id, чтобы избежать дублирования.
	 */
	public void addTemporaryModifiers(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiersMap) {
		modifiersMap.forEach((attribute, modifier) -> {
			EntityAttributeInstance instance = getCustomInstance(attribute);
			if (instance == null) {
				return;
			}

			instance.removeModifier(modifier.id());
			instance.addTemporaryModifier(modifier);
		});
	}

	public void removeModifiers(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiersMap) {
		modifiersMap.asMap().forEach((attribute, modifiers) -> {
			EntityAttributeInstance instance = custom.get(attribute);
			if (instance == null) {
				return;
			}

			modifiers.forEach(modifier -> instance.removeModifier(modifier.id()));
		});
	}

	public void setFrom(AttributeContainer other) {
		other.custom.values().forEach(otherInstance -> {
			EntityAttributeInstance instance = getCustomInstance(otherInstance.getAttribute());
			if (instance != null) {
				instance.setFrom(otherInstance);
			}
		});
	}

	public void setBaseFrom(AttributeContainer other) {
		other.custom.values().forEach(otherInstance -> {
			EntityAttributeInstance instance = getCustomInstance(otherInstance.getAttribute());
			if (instance != null) {
				instance.setBaseValue(otherInstance.getBaseValue());
			}
		});
	}

	public void addPersistentModifiersFrom(AttributeContainer other) {
		other.custom.values().forEach(otherInstance -> {
			EntityAttributeInstance instance = getCustomInstance(otherInstance.getAttribute());
			if (instance != null) {
				instance.addPersistentModifiers(otherInstance.getPersistentModifiers());
			}
		});
	}

	/**
	 * Сбрасывает базовое значение атрибута к значению по умолчанию из {@link DefaultAttributeContainer}.
	 *
	 * @return {@code false}, если атрибут не зарегистрирован в базовом контейнере
	 */
	public boolean resetToBaseValue(RegistryEntry<EntityAttribute> attribute) {
		if (!defaultAttributes.has(attribute)) {
			return false;
		}

		EntityAttributeInstance instance = custom.get(attribute);
		if (instance != null) {
			instance.setBaseValue(defaultAttributes.getBaseValue(attribute));
		}

		return true;
	}

	public List<EntityAttributeInstance.Packed> pack() {
		List<EntityAttributeInstance.Packed> result = new ArrayList<>(custom.values().size());
		for (EntityAttributeInstance instance : custom.values()) {
			result.add(instance.pack());
		}

		return result;
	}

	public void unpack(List<EntityAttributeInstance.Packed> packedList) {
		for (EntityAttributeInstance.Packed packed : packedList) {
			EntityAttributeInstance instance = getCustomInstance(packed.attribute());
			if (instance != null) {
				instance.unpack(packed);
			}
		}
	}
}

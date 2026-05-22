package net.minecraft.entity.attribute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Экземпляр атрибута сущности, хранящий базовое значение и набор активных модификаторов.
 * <p>
 * Итоговое значение вычисляется лениво (при первом обращении после изменения) по трёхэтапной формуле:
 * <ol>
 *   <li>Суммируются все {@link EntityAttributeModifier.Operation#ADD_VALUE} модификаторы к базе.</li>
 *   <li>К результату прибавляются {@link EntityAttributeModifier.Operation#ADD_MULTIPLIED_BASE} × база.</li>
 *   <li>Результат умножается на {@code (1 + value)} для каждого {@link EntityAttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}.</li>
 * </ol>
 */
public class EntityAttributeInstance {

	private final RegistryEntry<EntityAttribute> type;
	private final Map<EntityAttributeModifier.Operation, Map<Identifier, EntityAttributeModifier>> operationToModifiers =
			Maps.newEnumMap(EntityAttributeModifier.Operation.class);
	private final Map<Identifier, EntityAttributeModifier> idToModifiers = new Object2ObjectArrayMap<>();
	private final Map<Identifier, EntityAttributeModifier> persistentModifiers = new Object2ObjectArrayMap<>();
	private final Consumer<EntityAttributeInstance> updateCallback;
	private double baseValue;
	private boolean dirty = true;
	private double value;

	public EntityAttributeInstance(RegistryEntry<EntityAttribute> type, Consumer<EntityAttributeInstance> updateCallback) {
		this.type = type;
		this.updateCallback = updateCallback;
		this.baseValue = type.value().getDefaultValue();
	}

	public RegistryEntry<EntityAttribute> getAttribute() {
		return type;
	}

	public double getBaseValue() {
		return baseValue;
	}

	public void setBaseValue(double baseValue) {
		if (baseValue == this.baseValue) {
			return;
		}

		this.baseValue = baseValue;
		onUpdate();
	}

	@VisibleForTesting
	Map<Identifier, EntityAttributeModifier> getModifiers(EntityAttributeModifier.Operation operation) {
		return operationToModifiers.computeIfAbsent(operation, op -> new Object2ObjectOpenHashMap<>());
	}

	public Set<EntityAttributeModifier> getModifiers() {
		return ImmutableSet.copyOf(idToModifiers.values());
	}

	public Set<EntityAttributeModifier> getPersistentModifiers() {
		return ImmutableSet.copyOf(persistentModifiers.values());
	}

	public @Nullable EntityAttributeModifier getModifier(Identifier id) {
		return idToModifiers.get(id);
	}

	public boolean hasModifier(Identifier id) {
		return idToModifiers.containsKey(id);
	}

	private void addModifier(EntityAttributeModifier modifier) {
		EntityAttributeModifier existing = idToModifiers.putIfAbsent(modifier.id(), modifier);
		if (existing != null) {
			throw new IllegalArgumentException("Modifier is already applied on this attribute!");
		}

		getModifiers(modifier.operation()).put(modifier.id(), modifier);
		onUpdate();
	}

	/**
	 * Обновляет существующий модификатор по идентификатору. Если модификатор не изменился — пропускает обновление.
	 */
	public void updateModifier(EntityAttributeModifier modifier) {
		EntityAttributeModifier previous = idToModifiers.put(modifier.id(), modifier);
		if (modifier == previous) {
			return;
		}

		getModifiers(modifier.operation()).put(modifier.id(), modifier);
		onUpdate();
	}

	public void addTemporaryModifier(EntityAttributeModifier modifier) {
		addModifier(modifier);
	}

	/**
	 * Перезаписывает постоянный модификатор: удаляет старый и добавляет новый с тем же id.
	 */
	public void overwritePersistentModifier(EntityAttributeModifier modifier) {
		removeModifier(modifier.id());
		addModifier(modifier);
		persistentModifiers.put(modifier.id(), modifier);
	}

	public void addPersistentModifier(EntityAttributeModifier modifier) {
		addModifier(modifier);
		persistentModifiers.put(modifier.id(), modifier);
	}

	public void addPersistentModifiers(Collection<EntityAttributeModifier> modifiers) {
		for (EntityAttributeModifier modifier : modifiers) {
			addPersistentModifier(modifier);
		}
	}

	protected void onUpdate() {
		dirty = true;
		updateCallback.accept(this);
	}

	public void removeModifier(EntityAttributeModifier modifier) {
		removeModifier(modifier.id());
	}

	public boolean removeModifier(Identifier id) {
		EntityAttributeModifier removed = idToModifiers.remove(id);
		if (removed == null) {
			return false;
		}

		getModifiers(removed.operation()).remove(id);
		persistentModifiers.remove(id);
		onUpdate();
		return true;
	}

	public void clearModifiers() {
		for (EntityAttributeModifier modifier : getModifiers()) {
			removeModifier(modifier);
		}
	}

	public double getValue() {
		if (dirty) {
			value = computeValue();
			dirty = false;
		}

		return value;
	}

	/**
	 * Вычисляет итоговое значение атрибута по трёхэтапной формуле модификаторов.
	 */
	private double computeValue() {
		double base = getBaseValue();

		for (EntityAttributeModifier modifier : getModifiersByOperation(EntityAttributeModifier.Operation.ADD_VALUE)) {
			base += modifier.value();
		}

		double total = base;

		for (EntityAttributeModifier modifier : getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
			total += base * modifier.value();
		}

		for (EntityAttributeModifier modifier : getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
			total *= 1.0 + modifier.value();
		}

		return type.value().clamp(total);
	}

	private Collection<EntityAttributeModifier> getModifiersByOperation(EntityAttributeModifier.Operation operation) {
		return operationToModifiers.getOrDefault(operation, Map.of()).values();
	}

	public void setFrom(EntityAttributeInstance other) {
		baseValue = other.baseValue;
		idToModifiers.clear();
		idToModifiers.putAll(other.idToModifiers);
		persistentModifiers.clear();
		persistentModifiers.putAll(other.persistentModifiers);
		operationToModifiers.clear();
		other.operationToModifiers.forEach((operation, modifiers) -> getModifiers(operation).putAll(modifiers));
		onUpdate();
	}

	public Packed pack() {
		return new Packed(type, baseValue, List.copyOf(persistentModifiers.values()));
	}

	/**
	 * Восстанавливает состояние экземпляра из упакованного представления (используется при синхронизации по сети).
	 */
	public void unpack(Packed packed) {
		baseValue = packed.baseValue;

		for (EntityAttributeModifier modifier : packed.modifiers) {
			idToModifiers.put(modifier.id(), modifier);
			getModifiers(modifier.operation()).put(modifier.id(), modifier);
			persistentModifiers.put(modifier.id(), modifier);
		}

		onUpdate();
	}

	/**
	 * Компактное сетевое/NBT-представление экземпляра атрибута: только базовое значение и постоянные модификаторы.
	 */
	public record Packed(
			RegistryEntry<EntityAttribute> attribute,
			double baseValue,
			List<EntityAttributeModifier> modifiers
	) {

		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Registries.ATTRIBUTE.getEntryCodec().fieldOf("id").forGetter(Packed::attribute),
						Codec.DOUBLE.fieldOf("base").orElse(0.0).forGetter(Packed::baseValue),
						EntityAttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Packed::modifiers)
				).apply(instance, Packed::new)
		);
		public static final Codec<List<Packed>> LIST_CODEC = CODEC.listOf();
	}
}

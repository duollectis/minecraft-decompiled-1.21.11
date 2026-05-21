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
 * {@code EntityAttributeInstance}.
 */
public class EntityAttributeInstance {

	private final RegistryEntry<EntityAttribute> type;
	private final Map<EntityAttributeModifier.Operation, Map<Identifier, EntityAttributeModifier>>
			operationToModifiers =
			Maps.newEnumMap(
					EntityAttributeModifier.Operation.class
			);
	private final Map<Identifier, EntityAttributeModifier> idToModifiers = new Object2ObjectArrayMap();
	private final Map<Identifier, EntityAttributeModifier> persistentModifiers = new Object2ObjectArrayMap();
	private double baseValue;
	private boolean dirty = true;
	private double value;
	private final Consumer<EntityAttributeInstance> updateCallback;

	public EntityAttributeInstance(
			RegistryEntry<EntityAttribute> type,
			Consumer<EntityAttributeInstance> updateCallback
	) {
		this.type = type;
		this.updateCallback = updateCallback;
		this.baseValue = type.value().getDefaultValue();
	}

	public RegistryEntry<EntityAttribute> getAttribute() {
		return this.type;
	}

	public double getBaseValue() {
		return this.baseValue;
	}

	public void setBaseValue(double baseValue) {
		if (baseValue != this.baseValue) {
			this.baseValue = baseValue;
			this.onUpdate();
		}
	}

	@VisibleForTesting
	Map<Identifier, EntityAttributeModifier> getModifiers(EntityAttributeModifier.Operation operation) {
		return this.operationToModifiers.computeIfAbsent(operation, operationx -> new Object2ObjectOpenHashMap());
	}

	public Set<EntityAttributeModifier> getModifiers() {
		return ImmutableSet.copyOf(this.idToModifiers.values());
	}

	public Set<EntityAttributeModifier> getPersistentModifiers() {
		return ImmutableSet.copyOf(this.persistentModifiers.values());
	}

	public @Nullable EntityAttributeModifier getModifier(Identifier id) {
		return this.idToModifiers.get(id);
	}

	public boolean hasModifier(Identifier id) {
		return this.idToModifiers.get(id) != null;
	}

	private void addModifier(EntityAttributeModifier modifier) {
		EntityAttributeModifier entityAttributeModifier = this.idToModifiers.putIfAbsent(modifier.id(), modifier);
		if (entityAttributeModifier != null) {
			throw new IllegalArgumentException("Modifier is already applied on this attribute!");
		}
		else {
			this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
			this.onUpdate();
		}
	}

	public void updateModifier(EntityAttributeModifier modifier) {
		EntityAttributeModifier entityAttributeModifier = this.idToModifiers.put(modifier.id(), modifier);
		if (modifier != entityAttributeModifier) {
			this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
			this.onUpdate();
		}
	}

	public void addTemporaryModifier(EntityAttributeModifier modifier) {
		this.addModifier(modifier);
	}

	public void overwritePersistentModifier(EntityAttributeModifier modifier) {
		this.removeModifier(modifier.id());
		this.addModifier(modifier);
		this.persistentModifiers.put(modifier.id(), modifier);
	}

	public void addPersistentModifier(EntityAttributeModifier modifier) {
		this.addModifier(modifier);
		this.persistentModifiers.put(modifier.id(), modifier);
	}

	public void addPersistentModifiers(Collection<EntityAttributeModifier> modifiers) {
		for (EntityAttributeModifier entityAttributeModifier : modifiers) {
			this.addPersistentModifier(entityAttributeModifier);
		}
	}

	protected void onUpdate() {
		this.dirty = true;
		this.updateCallback.accept(this);
	}

	public void removeModifier(EntityAttributeModifier modifier) {
		this.removeModifier(modifier.id());
	}

	public boolean removeModifier(Identifier id) {
		EntityAttributeModifier entityAttributeModifier = this.idToModifiers.remove(id);
		if (entityAttributeModifier == null) {
			return false;
		}
		else {
			this.getModifiers(entityAttributeModifier.operation()).remove(id);
			this.persistentModifiers.remove(id);
			this.onUpdate();
			return true;
		}
	}

	public void clearModifiers() {
		for (EntityAttributeModifier entityAttributeModifier : this.getModifiers()) {
			this.removeModifier(entityAttributeModifier);
		}
	}

	public double getValue() {
		if (this.dirty) {
			this.value = this.computeValue();
			this.dirty = false;
		}

		return this.value;
	}

	private double computeValue() {
		double d = this.getBaseValue();

		for (EntityAttributeModifier entityAttributeModifier : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_VALUE)) {
			d += entityAttributeModifier.value();
		}

		double e = d;

		for (EntityAttributeModifier entityAttributeModifier2 : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
			e += d * entityAttributeModifier2.value();
		}

		for (EntityAttributeModifier entityAttributeModifier2 : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
			e *= 1.0 + entityAttributeModifier2.value();
		}

		return this.type.value().clamp(e);
	}

	private Collection<EntityAttributeModifier> getModifiersByOperation(EntityAttributeModifier.Operation operation) {
		return this.operationToModifiers.getOrDefault(operation, Map.of()).values();
	}

	public void setFrom(EntityAttributeInstance other) {
		this.baseValue = other.baseValue;
		this.idToModifiers.clear();
		this.idToModifiers.putAll(other.idToModifiers);
		this.persistentModifiers.clear();
		this.persistentModifiers.putAll(other.persistentModifiers);
		this.operationToModifiers.clear();
		other.operationToModifiers
				.forEach((operation, modifiers) -> this
						.getModifiers(operation)
						.putAll((Map<? extends Identifier, ? extends EntityAttributeModifier>) modifiers));
		this.onUpdate();
	}

	public EntityAttributeInstance.Packed pack() {
		return new EntityAttributeInstance.Packed(
				this.type,
				this.baseValue,
				List.copyOf(this.persistentModifiers.values())
		);
	}

	public void unpack(EntityAttributeInstance.Packed packed) {
		this.baseValue = packed.baseValue;

		for (EntityAttributeModifier entityAttributeModifier : packed.modifiers) {
			this.idToModifiers.put(entityAttributeModifier.id(), entityAttributeModifier);
			this
					.getModifiers(entityAttributeModifier.operation())
					.put(entityAttributeModifier.id(), entityAttributeModifier);
			this.persistentModifiers.put(entityAttributeModifier.id(), entityAttributeModifier);
		}

		this.onUpdate();
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(
			RegistryEntry<EntityAttribute> attribute,
			double baseValue,
			List<EntityAttributeModifier> modifiers
	) {

		public static final Codec<EntityAttributeInstance.Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Registries.ATTRIBUTE
								                    .getEntryCodec()
								                    .fieldOf("id")
								                    .forGetter(EntityAttributeInstance.Packed::attribute),
						                    Codec.DOUBLE.fieldOf("base").orElse(0.0).forGetter(EntityAttributeInstance.Packed::baseValue),
						                    EntityAttributeModifier.CODEC
								                    .listOf()
								                    .optionalFieldOf("modifiers", List.of())
								                    .forGetter(EntityAttributeInstance.Packed::modifiers)
				                    )
				                    .apply(instance, EntityAttributeInstance.Packed::new)
		);
		public static final Codec<List<EntityAttributeInstance.Packed>> LIST_CODEC = CODEC.listOf();
	}
}

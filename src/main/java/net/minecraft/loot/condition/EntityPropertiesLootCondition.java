package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Set;

/**
 * {@code EntityPropertiesLootCondition}.
 */
public record EntityPropertiesLootCondition(
		Optional<EntityPredicate> predicate,
		LootContext.EntityReference entity
) implements LootCondition {

	public static final MapCodec<EntityPropertiesLootCondition> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    EntityPredicate.CODEC
							                    .optionalFieldOf("predicate")
							                    .forGetter(EntityPropertiesLootCondition::predicate),
					                    LootContext.EntityReference.CODEC.fieldOf("entity").forGetter(EntityPropertiesLootCondition::entity)
			                    )
			                    .apply(instance, EntityPropertiesLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.ENTITY_PROPERTIES;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.ORIGIN, this.entity.contextParam());
	}

	/**
	 * Test.
	 *
	 * @param lootContext loot context
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(LootContext lootContext) {
		Entity entity = lootContext.get(this.entity.contextParam());
		Vec3d vec3d = lootContext.get(LootContextParameters.ORIGIN);
		return this.predicate.isEmpty() || this.predicate.get().test(lootContext.getWorld(), vec3d, entity);
	}

	public static LootCondition.Builder create(LootContext.EntityReference entity) {
		return builder(entity, EntityPredicate.Builder.create());
	}

	public static LootCondition.Builder builder(
			LootContext.EntityReference entity,
			EntityPredicate.Builder predicateBuilder
	) {
		return () -> new EntityPropertiesLootCondition(Optional.of(predicateBuilder.build()), entity);
	}

	public static LootCondition.Builder builder(LootContext.EntityReference entity, EntityPredicate predicate) {
		return () -> new EntityPropertiesLootCondition(Optional.of(predicate), entity);
	}
}

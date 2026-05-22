package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Составной предикат сущности. Объединяет проверки типа, расстояния, движения,
 * позиции, эффектов, NBT, флагов, снаряжения, специфики типа, транспорта,
 * пассажиров, цели, команды, слотов и компонентов.
 */
public record EntityPredicate(
		Optional<EntityTypePredicate> type,
		Optional<DistancePredicate> distance,
		Optional<MovementPredicate> movement,
		EntityPredicate.PositionalPredicates location,
		Optional<EntityEffectPredicate> effects,
		Optional<NbtPredicate> nbt,
		Optional<EntityFlagsPredicate> flags,
		Optional<EntityEquipmentPredicate> equipment,
		Optional<EntitySubPredicate> typeSpecific,
		Optional<Integer> periodicTick,
		Optional<EntityPredicate> vehicle,
		Optional<EntityPredicate> passenger,
		Optional<EntityPredicate> targetedEntity,
		Optional<String> team,
		Optional<SlotsPredicate> slots,
		ComponentsPredicate components
) {

	public static final Codec<EntityPredicate> CODEC = Codec.recursive(
			"EntityPredicate",
			entityPredicateCodec -> RecordCodecBuilder.create(
					instance -> instance.group(
							EntityTypePredicate.CODEC.optionalFieldOf("type").forGetter(EntityPredicate::type),
							DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(EntityPredicate::distance),
							MovementPredicate.CODEC.optionalFieldOf("movement").forGetter(EntityPredicate::movement),
							EntityPredicate.PositionalPredicates.CODEC.forGetter(EntityPredicate::location),
							EntityEffectPredicate.CODEC.optionalFieldOf("effects").forGetter(EntityPredicate::effects),
							NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(EntityPredicate::nbt),
							EntityFlagsPredicate.CODEC.optionalFieldOf("flags").forGetter(EntityPredicate::flags),
							EntityEquipmentPredicate.CODEC
									.optionalFieldOf("equipment")
									.forGetter(EntityPredicate::equipment),
							EntitySubPredicate.CODEC
									.optionalFieldOf("type_specific")
									.forGetter(EntityPredicate::typeSpecific),
							Codecs.POSITIVE_INT.optionalFieldOf("periodic_tick").forGetter(EntityPredicate::periodicTick),
							entityPredicateCodec.optionalFieldOf("vehicle").forGetter(EntityPredicate::vehicle),
							entityPredicateCodec.optionalFieldOf("passenger").forGetter(EntityPredicate::passenger),
							entityPredicateCodec
									.optionalFieldOf("targeted_entity")
									.forGetter(EntityPredicate::targetedEntity),
							Codec.STRING.optionalFieldOf("team").forGetter(EntityPredicate::team),
							SlotsPredicate.CODEC.optionalFieldOf("slots").forGetter(EntityPredicate::slots),
							ComponentsPredicate.CODEC.forGetter(EntityPredicate::components)
					).apply(instance, EntityPredicate::new)
			)
	);

	public static final Codec<LootContextPredicate> LOOT_CONTEXT_PREDICATE_CODEC = Codec.withAlternative(
			LootContextPredicate.CODEC, CODEC, EntityPredicate::asLootContextPredicate
	);

	public static LootContextPredicate contextPredicateFromEntityPredicate(EntityPredicate.Builder builder) {
		return asLootContextPredicate(builder.build());
	}

	public static Optional<LootContextPredicate> contextPredicateFromEntityPredicate(
			Optional<EntityPredicate> entityPredicate
	) {
		return entityPredicate.map(EntityPredicate::asLootContextPredicate);
	}

	public static List<LootContextPredicate> contextPredicateFromEntityPredicates(
			EntityPredicate.Builder... builders
	) {
		return Stream.of(builders).map(EntityPredicate::contextPredicateFromEntityPredicate).toList();
	}

	public static LootContextPredicate asLootContextPredicate(EntityPredicate predicate) {
		LootCondition lootCondition =
				EntityPropertiesLootCondition.builder(LootContext.EntityReference.THIS, predicate).build();
		return new LootContextPredicate(List.of(lootCondition));
	}

	public boolean test(ServerPlayerEntity player, @Nullable Entity entity) {
		return test(player.getEntityWorld(), player.getEntityPos(), entity);
	}

	/**
	 * Проверяет сущность в контексте мира и позиции наблюдателя.
	 * Все условия проверяются последовательно с ранним возвратом.
	 */
	public boolean test(ServerWorld world, @Nullable Vec3d pos, @Nullable Entity entity) {
		if (entity == null) {
			return false;
		}

		if (type.isPresent() && !type.get().matches(entity.getType())) {
			return false;
		}

		if (pos == null) {
			if (distance.isPresent()) {
				return false;
			}
		} else if (distance.isPresent()
				&& !distance.get().test(pos.x, pos.y, pos.z, entity.getX(), entity.getY(), entity.getZ())
		) {
			return false;
		}

		if (movement.isPresent()) {
			Vec3d velocity = entity.getMovement().multiply(20.0);

			if (!movement.get().test(velocity.x, velocity.y, velocity.z, entity.fallDistance)) {
				return false;
			}
		}

		if (location.located.isPresent()
				&& !location.located.get().test(world, entity.getX(), entity.getY(), entity.getZ())
		) {
			return false;
		}

		if (location.steppingOn.isPresent()) {
			Vec3d steppingPos = Vec3d.ofCenter(entity.getSteppingPos());

			if (!entity.isOnGround()
					|| !location.steppingOn.get().test(world, steppingPos.getX(), steppingPos.getY(), steppingPos.getZ())
			) {
				return false;
			}
		}

		if (location.affectsMovement.isPresent()) {
			Vec3d movementPos = Vec3d.ofCenter(entity.getVelocityAffectingPos());

			if (!location.affectsMovement.get().test(world, movementPos.getX(), movementPos.getY(), movementPos.getZ())) {
				return false;
			}
		}

		if (effects.isPresent() && !effects.get().test(entity)) {
			return false;
		}

		if (flags.isPresent() && !flags.get().test(entity)) {
			return false;
		}

		if (equipment.isPresent() && !equipment.get().test(entity)) {
			return false;
		}

		if (typeSpecific.isPresent() && !typeSpecific.get().test(entity, world, pos)) {
			return false;
		}

		if (vehicle.isPresent() && !vehicle.get().test(world, pos, entity.getVehicle())) {
			return false;
		}

		if (passenger.isPresent()
				&& entity.getPassengerList().stream().noneMatch(p -> passenger.get().test(world, pos, p))
		) {
			return false;
		}

		if (targetedEntity.isPresent()) {
			Entity target = entity instanceof MobEntity mob ? mob.getTarget() : null;

			if (!targetedEntity.get().test(world, pos, target)) {
				return false;
			}
		}

		if (periodicTick.isPresent() && entity.age % periodicTick.get() != 0) {
			return false;
		}

		if (team.isPresent()) {
			AbstractTeam entityTeam = entity.getScoreboardTeam();

			if (entityTeam == null || !team.get().equals(entityTeam.getName())) {
				return false;
			}
		}

		if (slots.isPresent() && !slots.get().matches(entity)) {
			return false;
		}

		if (!components.test((ComponentsAccess) entity)) {
			return false;
		}

		return nbt.isEmpty() || nbt.get().test(entity);
	}

	public static LootContext createAdvancementEntityLootContext(ServerPlayerEntity player, Entity target) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(player.getEntityWorld())
				.add(LootContextParameters.THIS_ENTITY, target)
				.add(LootContextParameters.ORIGIN, player.getEntityPos())
				.build(LootContextTypes.ADVANCEMENT_ENTITY);
		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Строитель для составления {@link EntityPredicate} с фильтрами по типу, местоположению, флагам и снаряжению сущности.
	 */
	public static class Builder {

		private Optional<EntityTypePredicate> type = Optional.empty();
		private Optional<DistancePredicate> distance = Optional.empty();
		private Optional<MovementPredicate> movement = Optional.empty();
		private Optional<LocationPredicate> location = Optional.empty();
		private Optional<LocationPredicate> steppingOn = Optional.empty();
		private Optional<LocationPredicate> movementAffectedBy = Optional.empty();
		private Optional<EntityEffectPredicate> effects = Optional.empty();
		private Optional<NbtPredicate> nbt = Optional.empty();
		private Optional<EntityFlagsPredicate> flags = Optional.empty();
		private Optional<EntityEquipmentPredicate> equipment = Optional.empty();
		private Optional<EntitySubPredicate> typeSpecific = Optional.empty();
		private Optional<Integer> periodicTick = Optional.empty();
		private Optional<EntityPredicate> vehicle = Optional.empty();
		private Optional<EntityPredicate> passenger = Optional.empty();
		private Optional<EntityPredicate> targetedEntity = Optional.empty();
		private Optional<String> team = Optional.empty();
		private Optional<SlotsPredicate> slots = Optional.empty();
		private ComponentsPredicate components = ComponentsPredicate.EMPTY;

		public static EntityPredicate.Builder create() {
			return new EntityPredicate.Builder();
		}

		public EntityPredicate.Builder type(RegistryEntryLookup<EntityType<?>> entityTypeRegistry, EntityType<?> type) {
			this.type = Optional.of(EntityTypePredicate.create(entityTypeRegistry, type));
			return this;
		}

		public EntityPredicate.Builder type(
				RegistryEntryLookup<EntityType<?>> entityTypeRegistry,
				TagKey<EntityType<?>> tag
		) {
			type = Optional.of(EntityTypePredicate.create(entityTypeRegistry, tag));
			return this;
		}

		public EntityPredicate.Builder type(EntityTypePredicate type) {
			this.type = Optional.of(type);
			return this;
		}

		public EntityPredicate.Builder distance(DistancePredicate distance) {
			this.distance = Optional.of(distance);
			return this;
		}

		public EntityPredicate.Builder movement(MovementPredicate movement) {
			this.movement = Optional.of(movement);
			return this;
		}

		public EntityPredicate.Builder location(LocationPredicate.Builder location) {
			this.location = Optional.of(location.build());
			return this;
		}

		public EntityPredicate.Builder steppingOn(LocationPredicate.Builder steppingOn) {
			this.steppingOn = Optional.of(steppingOn.build());
			return this;
		}

		public EntityPredicate.Builder movementAffectedBy(LocationPredicate.Builder movementAffectedBy) {
			this.movementAffectedBy = Optional.of(movementAffectedBy.build());
			return this;
		}

		public EntityPredicate.Builder effects(EntityEffectPredicate.Builder effects) {
			this.effects = effects.build();
			return this;
		}

		public EntityPredicate.Builder nbt(NbtPredicate nbt) {
			this.nbt = Optional.of(nbt);
			return this;
		}

		public EntityPredicate.Builder flags(EntityFlagsPredicate.Builder flags) {
			this.flags = Optional.of(flags.build());
			return this;
		}

		public EntityPredicate.Builder equipment(EntityEquipmentPredicate.Builder equipment) {
			this.equipment = Optional.of(equipment.build());
			return this;
		}

		public EntityPredicate.Builder equipment(EntityEquipmentPredicate equipment) {
			this.equipment = Optional.of(equipment);
			return this;
		}

		public EntityPredicate.Builder typeSpecific(EntitySubPredicate typeSpecific) {
			this.typeSpecific = Optional.of(typeSpecific);
			return this;
		}

		public EntityPredicate.Builder periodicTick(int periodicTick) {
			this.periodicTick = Optional.of(periodicTick);
			return this;
		}

		public EntityPredicate.Builder vehicle(EntityPredicate.Builder vehicle) {
			this.vehicle = Optional.of(vehicle.build());
			return this;
		}

		public EntityPredicate.Builder passenger(EntityPredicate.Builder passenger) {
			this.passenger = Optional.of(passenger.build());
			return this;
		}

		public EntityPredicate.Builder targetedEntity(EntityPredicate.Builder targetedEntity) {
			this.targetedEntity = Optional.of(targetedEntity.build());
			return this;
		}

		public EntityPredicate.Builder team(String team) {
			this.team = Optional.of(team);
			return this;
		}

		public EntityPredicate.Builder slots(SlotsPredicate slots) {
			this.slots = Optional.of(slots);
			return this;
		}

		public EntityPredicate.Builder components(ComponentsPredicate components) {
			this.components = components;
			return this;
		}

		public EntityPredicate build() {
			return new EntityPredicate(
					type,
					distance,
					movement,
					new EntityPredicate.PositionalPredicates(location, steppingOn, movementAffectedBy),
					effects,
					nbt,
					flags,
					equipment,
					typeSpecific,
					periodicTick,
					vehicle,
					passenger,
					targetedEntity,
					team,
					slots,
					components
			);
		}
	}

	/**
	 * Группа позиционных предикатов: текущая позиция, позиция под ногами и позиция,
	 * влияющая на движение (например, блок под ногами).
	 */
	public record PositionalPredicates(
			Optional<LocationPredicate> located,
			Optional<LocationPredicate> steppingOn,
			Optional<LocationPredicate> affectsMovement
	) {

		public static final MapCodec<EntityPredicate.PositionalPredicates> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						LocationPredicate.CODEC
								.optionalFieldOf("location")
								.forGetter(EntityPredicate.PositionalPredicates::located),
						LocationPredicate.CODEC
								.optionalFieldOf("stepping_on")
								.forGetter(EntityPredicate.PositionalPredicates::steppingOn),
						LocationPredicate.CODEC
								.optionalFieldOf("movement_affected_by")
								.forGetter(EntityPredicate.PositionalPredicates::affectsMovement)
				).apply(instance, EntityPredicate.PositionalPredicates::new)
		);
	}
}

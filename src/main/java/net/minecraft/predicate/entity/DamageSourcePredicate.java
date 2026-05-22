package net.minecraft.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.predicate.TagPredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/**
 * Предикат источника урона. Проверяет теги типа урона, прямую и косвенную сущности-источники,
 * а также признак прямого урона.
 */
public record DamageSourcePredicate(
		List<TagPredicate<DamageType>> tags,
		Optional<EntityPredicate> directEntity,
		Optional<EntityPredicate> sourceEntity,
		Optional<Boolean> isDirect
) {

	public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					TagPredicate.createCodec(RegistryKeys.DAMAGE_TYPE)
							.listOf()
							.optionalFieldOf("tags", List.of())
							.forGetter(DamageSourcePredicate::tags),
					EntityPredicate.CODEC
							.optionalFieldOf("direct_entity")
							.forGetter(DamageSourcePredicate::directEntity),
					EntityPredicate.CODEC
							.optionalFieldOf("source_entity")
							.forGetter(DamageSourcePredicate::sourceEntity),
					Codec.BOOL.optionalFieldOf("is_direct").forGetter(DamageSourcePredicate::isDirect)
			).apply(instance, DamageSourcePredicate::new)
	);

	public boolean test(ServerPlayerEntity player, DamageSource damageSource) {
		return test(player.getEntityWorld(), player.getEntityPos(), damageSource);
	}

	public boolean test(ServerWorld world, Vec3d pos, DamageSource damageSource) {
		for (TagPredicate<DamageType> tagPredicate : tags) {
			if (!tagPredicate.test(damageSource.getTypeRegistryEntry())) {
				return false;
			}
		}

		if (directEntity.isPresent() && !directEntity.get().test(world, pos, damageSource.getSource())) {
			return false;
		}

		if (sourceEntity.isPresent() && !sourceEntity.get().test(world, pos, damageSource.getAttacker())) {
			return false;
		}

		return isDirect.isEmpty() || isDirect.get() == damageSource.isDirect();
	}

	/**
	 * Строитель для составления {@link DamageSourcePredicate} с фильтрами по тегам, прямой и косвенной сущности.
	 */
	public static class Builder {

		private final ImmutableList.Builder<TagPredicate<DamageType>> tagPredicates = ImmutableList.builder();
		private Optional<EntityPredicate> directEntity = Optional.empty();
		private Optional<EntityPredicate> sourceEntity = Optional.empty();
		private Optional<Boolean> isDirect = Optional.empty();

		public static DamageSourcePredicate.Builder create() {
			return new DamageSourcePredicate.Builder();
		}

		public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> tagPredicate) {
			tagPredicates.add(tagPredicate);
			return this;
		}

		public DamageSourcePredicate.Builder directEntity(EntityPredicate.Builder entity) {
			directEntity = Optional.of(entity.build());
			return this;
		}

		public DamageSourcePredicate.Builder sourceEntity(EntityPredicate.Builder entity) {
			sourceEntity = Optional.of(entity.build());
			return this;
		}

		public DamageSourcePredicate.Builder isDirect(boolean direct) {
			isDirect = Optional.of(direct);
			return this;
		}

		public DamageSourcePredicate build() {
			return new DamageSourcePredicate(tagPredicates.build(), directEntity, sourceEntity, isDirect);
		}
	}
}

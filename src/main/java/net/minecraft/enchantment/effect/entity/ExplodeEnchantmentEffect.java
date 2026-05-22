package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.particle.BlockParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Эффект зачарования, создающий взрыв в позиции применения.
 * Поддерживает настройку: атрибуцию урона, тип урона, множитель отдачи,
 * иммунные блоки, смещение, радиус, создание огня, тип взаимодействия с блоками,
 * частицы и звук взрыва.
 */
public record ExplodeEnchantmentEffect(
		boolean attributeToUser,
		Optional<RegistryEntry<DamageType>> damageType,
		Optional<EnchantmentLevelBasedValue> knockbackMultiplier,
		Optional<RegistryEntryList<Block>> immuneBlocks,
		Vec3d offset,
		EnchantmentLevelBasedValue radius,
		boolean createFire,
		World.ExplosionSourceType blockInteraction,
		ParticleEffect smallParticle,
		ParticleEffect largeParticle,
		Pool<BlockParticleEffect> blockParticles,
		RegistryEntry<SoundEvent> sound
) implements EnchantmentEntityEffect {

	public static final MapCodec<ExplodeEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.BOOL
							.optionalFieldOf("attribute_to_user", false)
							.forGetter(ExplodeEnchantmentEffect::attributeToUser),
					DamageType.ENTRY_CODEC
							.optionalFieldOf("damage_type")
							.forGetter(ExplodeEnchantmentEffect::damageType),
					EnchantmentLevelBasedValue.CODEC
							.optionalFieldOf("knockback_multiplier")
							.forGetter(ExplodeEnchantmentEffect::knockbackMultiplier),
					RegistryCodecs
							.entryList(RegistryKeys.BLOCK)
							.optionalFieldOf("immune_blocks")
							.forGetter(ExplodeEnchantmentEffect::immuneBlocks),
					Vec3d.CODEC
							.optionalFieldOf("offset", Vec3d.ZERO)
							.forGetter(ExplodeEnchantmentEffect::offset),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("radius")
							.forGetter(ExplodeEnchantmentEffect::radius),
					Codec.BOOL
							.optionalFieldOf("create_fire", false)
							.forGetter(ExplodeEnchantmentEffect::createFire),
					World.ExplosionSourceType.CODEC
							.fieldOf("block_interaction")
							.forGetter(ExplodeEnchantmentEffect::blockInteraction),
					ParticleTypes.TYPE_CODEC
							.fieldOf("small_particle")
							.forGetter(ExplodeEnchantmentEffect::smallParticle),
					ParticleTypes.TYPE_CODEC
							.fieldOf("large_particle")
							.forGetter(ExplodeEnchantmentEffect::largeParticle),
					Pool
							.createCodec(BlockParticleEffect.CODEC)
							.optionalFieldOf("block_particles", Pool.empty())
							.forGetter(ExplodeEnchantmentEffect::blockParticles),
					SoundEvent.ENTRY_CODEC
							.fieldOf("sound")
							.forGetter(ExplodeEnchantmentEffect::sound)
			).apply(instance, ExplodeEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		Vec3d explosionPos = pos.add(offset);

		world.createExplosion(
				attributeToUser ? user : null,
				buildDamageSource(user, explosionPos),
				new AdvancedExplosionBehavior(
						blockInteraction != World.ExplosionSourceType.NONE,
						damageType.isPresent(),
						knockbackMultiplier.map(multiplier -> multiplier.getValue(level)),
						immuneBlocks
				),
				explosionPos.getX(),
				explosionPos.getY(),
				explosionPos.getZ(),
				Math.max(radius.getValue(level), 0.0F),
				createFire,
				blockInteraction,
				smallParticle,
				largeParticle,
				blockParticles,
				sound
		);
	}

	@Override
	public MapCodec<ExplodeEnchantmentEffect> getCodec() {
		return CODEC;
	}

	private @Nullable DamageSource buildDamageSource(Entity user, Vec3d pos) {
		if (damageType.isEmpty()) {
			return null;
		}

		return attributeToUser
			? new DamageSource(damageType.get(), user)
			: new DamageSource(damageType.get(), pos);
	}
}

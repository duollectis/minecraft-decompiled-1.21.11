package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;

/**
 * {@code EnchantmentEntityEffect}.
 */
public interface EnchantmentEntityEffect extends EnchantmentLocationBasedEffect {

	Codec<EnchantmentEntityEffect>
			CODEC =
			Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE
					.getCodec()
					.dispatch(EnchantmentEntityEffect::getCodec, Function.identity());

	static MapCodec<? extends EnchantmentEntityEffect> registerAndGetDefault(Registry<MapCodec<? extends EnchantmentEntityEffect>> registry) {
		Registry.register(registry, "all_of", AllOfEnchantmentEffects.EntityEffects.CODEC);
		Registry.register(registry, "apply_mob_effect", ApplyMobEffectEnchantmentEffect.CODEC);
		Registry.register(registry, "change_item_damage", ChangeItemDamageEnchantmentEffect.CODEC);
		Registry.register(registry, "damage_entity", DamageEntityEnchantmentEffect.CODEC);
		Registry.register(registry, "explode", ExplodeEnchantmentEffect.CODEC);
		Registry.register(registry, "ignite", IgniteEnchantmentEffect.CODEC);
		Registry.register(registry, "apply_impulse", ApplyImpulseEnchantmentEffect.CODEC);
		Registry.register(registry, "apply_exhaustion", ApplyExhaustionEnchantmentEffect.CODEC);
		Registry.register(registry, "play_sound", PlaySoundEnchantmentEffect.CODEC);
		Registry.register(registry, "replace_block", ReplaceBlockEnchantmentEffect.CODEC);
		Registry.register(registry, "replace_disk", ReplaceDiskEnchantmentEffect.CODEC);
		Registry.register(registry, "run_function", RunFunctionEnchantmentEffect.CODEC);
		Registry.register(registry, "set_block_properties", SetBlockPropertiesEnchantmentEffect.CODEC);
		Registry.register(registry, "spawn_particles", SpawnParticlesEnchantmentEffect.CODEC);
		return Registry.register(registry, "summon_entity", SummonEntityEnchantmentEffect.CODEC);
	}

	void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos);

	@Override
	default void apply(
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			Entity user,
			Vec3d pos,
			boolean newlyApplied
	) {
		this.apply(world, level, context, user, pos);
	}

	@Override
	MapCodec<? extends EnchantmentEntityEffect> getCodec();
}

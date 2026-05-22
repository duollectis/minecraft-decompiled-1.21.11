package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Эффект зачарования, воспроизводящий звук в позиции сущности.
 * Звук выбирается из списка по индексу {@code level - 1} (зажатому в допустимый диапазон),
 * что позволяет использовать разные звуки для разных уровней зачарования.
 * Не воспроизводится для беззвучных сущностей.
 */
public record PlaySoundEnchantmentEffect(
		List<RegistryEntry<SoundEvent>> soundEvents,
		FloatProvider volume,
		FloatProvider pitch
) implements EnchantmentEntityEffect {

	public static final MapCodec<PlaySoundEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codecs.listOrSingle(SoundEvent.ENTRY_CODEC, SoundEvent.ENTRY_CODEC.sizeLimitedListOf(255))
							.fieldOf("sound")
							.forGetter(PlaySoundEnchantmentEffect::soundEvents),
					FloatProvider
							.createValidatedCodec(1.0E-5F, 10.0F)
							.fieldOf("volume")
							.forGetter(PlaySoundEnchantmentEffect::volume),
					FloatProvider
							.createValidatedCodec(1.0E-5F, 2.0F)
							.fieldOf("pitch")
							.forGetter(PlaySoundEnchantmentEffect::pitch)
			).apply(instance, PlaySoundEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		if (user.isSilent()) {
			return;
		}

		Random random = user.getRandom();
		int soundIndex = MathHelper.clamp(level - 1, 0, soundEvents.size() - 1);

		world.playSound(
				null,
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				soundEvents.get(soundIndex),
				user.getSoundCategory(),
				volume.get(random),
				pitch.get(random)
		);
	}

	@Override
	public MapCodec<PlaySoundEnchantmentEffect> getCodec() {
		return CODEC;
	}
}

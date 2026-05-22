package net.minecraft.entity.damage;

import com.mojang.serialization.Codec;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.StringIdentifiable;

/**
 * Визуальный и звуковой эффект, воспроизводимый при получении урона определённого типа.
 */
public enum DamageEffects implements StringIdentifiable {
	HURT("hurt", SoundEvents.ENTITY_PLAYER_HURT),
	THORNS("thorns", SoundEvents.ENCHANT_THORNS_HIT),
	DROWNING("drowning", SoundEvents.ENTITY_PLAYER_HURT_DROWN),
	BURNING("burning", SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE),
	POKING("poking", SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH),
	FREEZING("freezing", SoundEvents.ENTITY_PLAYER_HURT_FREEZE);

	public static final Codec<DamageEffects> CODEC = StringIdentifiable.createCodec(DamageEffects::values);

	private final String id;
	private final SoundEvent sound;

	DamageEffects(String id, SoundEvent sound) {
		this.id = id;
		this.sound = sound;
	}

	@Override
	public String asString() {
		return id;
	}

	public SoundEvent getSound() {
		return sound;
	}
}

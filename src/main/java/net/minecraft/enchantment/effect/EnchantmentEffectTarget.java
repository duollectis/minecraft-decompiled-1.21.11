package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Определяет роль сущности в контексте применения эффекта зачарования при атаке.
 * Используется в {@link TargetedEnchantmentEffect} для указания, кто является
 * носителем зачарования и кто получает эффект.
 */
public enum EnchantmentEffectTarget implements StringIdentifiable {
	ATTACKER("attacker"),
	DAMAGING_ENTITY("damaging_entity"),
	VICTIM("victim");

	public static final Codec<EnchantmentEffectTarget> CODEC = StringIdentifiable.createCodec(EnchantmentEffectTarget::values);

	private final String id;

	EnchantmentEffectTarget(String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}
}

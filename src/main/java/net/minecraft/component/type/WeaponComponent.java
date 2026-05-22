package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;

/**
	 * Компонент оружия. Определяет урон предмету за каждую атаку и время,
	 * на которое блокирование противника отключается после удара.
	 */
public record WeaponComponent(int itemDamagePerAttack, float disableBlockingForSeconds) {

	public static final float AXE_DISABLE_BLOCKING_FOR_SECONDS = 5.0F;
	public static final Codec<WeaponComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.NON_NEGATIVE_INT
												.optionalFieldOf("item_damage_per_attack", 1)
												.forGetter(WeaponComponent::itemDamagePerAttack),
										Codecs.NON_NEGATIVE_FLOAT
												.optionalFieldOf("disable_blocking_for_seconds", 0.0F)
												.forGetter(WeaponComponent::disableBlockingForSeconds)
								)
								.apply(instance, WeaponComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, WeaponComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			WeaponComponent::itemDamagePerAttack,
			PacketCodecs.FLOAT,
			WeaponComponent::disableBlockingForSeconds,
			WeaponComponent::new
	);

	public WeaponComponent(int itemDamagePerAttack) {
		this(itemDamagePerAttack, 0.0F);
	}
}

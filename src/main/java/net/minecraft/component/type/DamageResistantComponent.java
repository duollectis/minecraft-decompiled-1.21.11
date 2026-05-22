package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
	 * Компонент устойчивости к урону. Предмет с этим компонентом не уничтожается
	 * при получении урона типов, входящих в тег {@code types}.
	 */
public record DamageResistantComponent(TagKey<DamageType> types) {

	public static final Codec<DamageResistantComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(TagKey
							.codec(RegistryKeys.DAMAGE_TYPE)
							.fieldOf("types")
							.forGetter(DamageResistantComponent::types))
					.apply(instance, DamageResistantComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, DamageResistantComponent> PACKET_CODEC = PacketCodec.tuple(
			TagKey.packetCodec(RegistryKeys.DAMAGE_TYPE), DamageResistantComponent::types, DamageResistantComponent::new
	);

	/**
		 * Проверяет, устойчив ли предмет к данному источнику урона.
		 *
		 * @param damageSource источник урона
		 * @return {@code true} если тип урона входит в тег {@code types}
		 */
	public boolean resists(DamageSource damageSource) {
		return damageSource.isIn(this.types);
	}
}

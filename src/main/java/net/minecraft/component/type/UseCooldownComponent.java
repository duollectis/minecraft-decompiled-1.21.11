package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
	 * Компонент кулдауна использования предмета. Задаёт время перезарядки в секундах
	 * и опциональную группу кулдауна (предметы одной группы делят общий кулдаун).
	 */
public record UseCooldownComponent(float seconds, Optional<Identifier> cooldownGroup) {

	private static final float TICKS_PER_SECOND = 20.0F;

	public static final Codec<UseCooldownComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.POSITIVE_FLOAT.fieldOf("seconds").forGetter(UseCooldownComponent::seconds),
										Identifier.CODEC.optionalFieldOf("cooldown_group").forGetter(UseCooldownComponent::cooldownGroup)
								)
								.apply(instance, UseCooldownComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, UseCooldownComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			UseCooldownComponent::seconds,
			Identifier.PACKET_CODEC.collect(PacketCodecs::optional),
			UseCooldownComponent::cooldownGroup,
			UseCooldownComponent::new
	);

	public UseCooldownComponent(float seconds) {
		this(seconds, Optional.empty());
	}

	public int getCooldownTicks() {
		return (int) (seconds * TICKS_PER_SECOND);
	}

	/**
		 * Устанавливает кулдаун для данного стека предмета игроку.
		 * Для не-игровых сущностей кулдаун не применяется.
		 *
		 * @param stack стек предмета, для которого устанавливается кулдаун
		 * @param user  сущность, использующая предмет
		 */
	public void set(ItemStack stack, LivingEntity user) {
		if (user instanceof PlayerEntity playerEntity) {
			playerEntity.getItemCooldownManager().set(stack, getCooldownTicks());
		}
	}
}

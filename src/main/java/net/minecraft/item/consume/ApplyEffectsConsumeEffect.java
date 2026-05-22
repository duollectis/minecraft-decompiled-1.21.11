package net.minecraft.item.consume;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.world.World;

import java.util.List;

/**
 * Эффект потребления, накладывающий список статус-эффектов на сущность с заданной вероятностью.
 * Если {@code probability} меньше 1.0, эффекты применяются не всегда.
 */
public record ApplyEffectsConsumeEffect(
		List<StatusEffectInstance> effects,
		float probability
) implements ConsumeEffect {

	public static final MapCodec<ApplyEffectsConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    StatusEffectInstance.CODEC
							                    .listOf()
							                    .fieldOf("effects")
							                    .forGetter(ApplyEffectsConsumeEffect::effects),
					                    Codec
							                    .floatRange(0.0F, 1.0F)
							                    .optionalFieldOf("probability", 1.0F)
							                    .forGetter(ApplyEffectsConsumeEffect::probability)
			                    )
			                    .apply(instance, ApplyEffectsConsumeEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, ApplyEffectsConsumeEffect> PACKET_CODEC = PacketCodec.tuple(
			StatusEffectInstance.PACKET_CODEC.collect(PacketCodecs.toList()),
			ApplyEffectsConsumeEffect::effects,
			PacketCodecs.FLOAT,
			ApplyEffectsConsumeEffect::probability,
			ApplyEffectsConsumeEffect::new
	);

	public ApplyEffectsConsumeEffect(StatusEffectInstance effect, float probability) {
		this(List.of(effect), probability);
	}

	public ApplyEffectsConsumeEffect(List<StatusEffectInstance> effects) {
		this(effects, 1.0F);
	}

	public ApplyEffectsConsumeEffect(StatusEffectInstance effect) {
		this(effect, 1.0F);
	}

	@Override
	public ConsumeEffect.Type<ApplyEffectsConsumeEffect> getType() {
		return ConsumeEffect.Type.APPLY_EFFECTS;
	}

	@Override
	public boolean onConsume(World world, ItemStack stack, LivingEntity user) {
		if (user.getRandom().nextFloat() >= probability) {
			return false;
		}

		boolean anyApplied = false;

		for (StatusEffectInstance effect : effects) {
			if (user.addStatusEffect(new StatusEffectInstance(effect))) {
				anyApplied = true;
			}
		}

		return anyApplied;
	}
}

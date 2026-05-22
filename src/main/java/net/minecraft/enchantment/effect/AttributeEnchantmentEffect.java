package net.minecraft.enchantment.effect;

import com.google.common.collect.HashMultimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Vec3d;

/**
 * Эффект зачарования, добавляющий модификатор атрибута сущности на время действия зачарования.
 * Применяется при надевании предмета ({@code newlyApplied = true}) и снимается при снятии.
 * Идентификатор модификатора формируется как {@code id/slotName} для уникальности по слоту.
 */
public record AttributeEnchantmentEffect(
		Identifier id,
		RegistryEntry<EntityAttribute> attribute,
		EnchantmentLevelBasedValue amount,
		EntityAttributeModifier.Operation operation
) implements EnchantmentLocationBasedEffect {

	public static final MapCodec<AttributeEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Identifier.CODEC.fieldOf("id").forGetter(AttributeEnchantmentEffect::id),
					EntityAttribute.CODEC.fieldOf("attribute").forGetter(AttributeEnchantmentEffect::attribute),
					EnchantmentLevelBasedValue.CODEC.fieldOf("amount").forGetter(AttributeEnchantmentEffect::amount),
					EntityAttributeModifier.Operation.CODEC
							.fieldOf("operation")
							.forGetter(AttributeEnchantmentEffect::operation)
			).apply(instance, AttributeEnchantmentEffect::new)
	);

	/**
	 * Создаёт модификатор атрибута для указанного уровня зачарования и суффикса (обычно — слота экипировки).
	 * Суффикс добавляется к идентификатору, чтобы один и тот же эффект мог применяться из разных слотов.
	 */
	public EntityAttributeModifier createAttributeModifier(int level, StringIdentifiable suffix) {
		return new EntityAttributeModifier(
				id.withSuffixedPath("/" + suffix.asString()),
				amount.getValue(level),
				operation
		);
	}

	@Override
	public void apply(
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			Entity user,
			Vec3d pos,
			boolean newlyApplied
	) {
		if (newlyApplied && user instanceof LivingEntity livingEntity) {
			livingEntity.getAttributes().addTemporaryModifiers(buildModifiers(level, context.slot()));
		}
	}

	@Override
	public void remove(EnchantmentEffectContext context, Entity user, Vec3d pos, int level) {
		if (user instanceof LivingEntity livingEntity) {
			livingEntity.getAttributes().removeModifiers(buildModifiers(level, context.slot()));
		}
	}

	@Override
	public MapCodec<AttributeEnchantmentEffect> getCodec() {
		return CODEC;
	}

	private HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> buildModifiers(
			int level,
			EquipmentSlot slot
	) {
		HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiers = HashMultimap.create();
		modifiers.put(attribute, createAttributeModifier(level, slot));

		return modifiers;
	}
}

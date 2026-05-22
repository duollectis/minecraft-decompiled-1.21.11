package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Эффект зачарования, изменяющий прочность предмета в контексте зачарования.
 * Применяется только к предметам, имеющим компоненты {@code MAX_DAMAGE} и {@code DAMAGE}.
 * Если владелец является серверным игроком — передаётся для корректной обработки поломки.
 */
public record ChangeItemDamageEnchantmentEffect(EnchantmentLevelBasedValue amount) implements EnchantmentEntityEffect {

	public static final MapCodec<ChangeItemDamageEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("amount")
							.forGetter(ChangeItemDamageEnchantmentEffect::amount)
			).apply(instance, ChangeItemDamageEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		ItemStack stack = context.stack();

		if (!stack.contains(DataComponentTypes.MAX_DAMAGE) || !stack.contains(DataComponentTypes.DAMAGE)) {
			return;
		}

		ServerPlayerEntity player = context.owner() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
		int damage = (int) amount.getValue(level);

		stack.damage(damage, world, player, context.breakCallback());
	}

	@Override
	public MapCodec<ChangeItemDamageEnchantmentEffect> getCodec() {
		return CODEC;
	}
}

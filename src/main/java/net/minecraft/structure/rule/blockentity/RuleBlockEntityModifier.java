package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс модификатора NBT блок-сущности, применяемого в правилах процессора структур.
 * Позволяет изменять, очищать или дополнять NBT-данные блок-сущности при размещении структуры.
 */
public interface RuleBlockEntityModifier {

	Codec<RuleBlockEntityModifier> TYPE_CODEC = Registries.RULE_BLOCK_ENTITY_MODIFIER
		.getCodec()
		.dispatch(RuleBlockEntityModifier::getType, RuleBlockEntityModifierType::codec);

	/**
	 * Применяет модификацию к NBT блок-сущности.
	 *
	 * @param random генератор случайных чисел
	 * @param nbt    исходный NBT блок-сущности, может быть {@code null}
	 * @return изменённый NBT, либо {@code null} для удаления данных
	 */
	@Nullable NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt);

	RuleBlockEntityModifierType<?> getType();
}

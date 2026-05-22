package net.minecraft.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;

/**
 * Неизменяемая пара «слот + стак», используемая при сериализации инвентарей в NBT.
 * Поле {@code slot} кодируется как беззнаковый байт (0–255), что соответствует
 * максимальному размеру стандартных контейнеров Minecraft.
 */
public record StackWithSlot(int slot, ItemStack stack) {

	public static final Codec<StackWithSlot> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codecs.UNSIGNED_BYTE.fieldOf("Slot").orElse(0).forGetter(StackWithSlot::slot),
			ItemStack.MAP_CODEC.forGetter(StackWithSlot::stack)
		).apply(instance, StackWithSlot::new)
	);

	/**
	 * Проверяет, что {@link #slot} находится в допустимом диапазоне для инвентаря
	 * заданного размера. Защищает от выхода за границы при десериализации.
	 *
	 * @param inventorySize размер целевого инвентаря
	 * @return {@code true}, если слот валиден
	 */
	public boolean isValidSlot(int inventorySize) {
		return slot >= 0 && slot < inventorySize;
	}
}

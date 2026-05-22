package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.text.Text;

/**
 * Предмет-воздух. Используется как нулевой объект для пустого слота инвентаря.
 * <p>Переопределяет {@link #getName(ItemStack)} чтобы вернуть имя предмета
 * без учёта стека (воздух не имеет стека).</p>
 */
public class AirBlockItem extends Item {

	public AirBlockItem(Block block, Item.Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		return getName();
	}
}

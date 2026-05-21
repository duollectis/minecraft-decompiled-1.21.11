package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.text.Text;

/**
 * {@code AirBlockItem}.
 */
public class AirBlockItem extends Item {

	public AirBlockItem(Block block, Item.Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		return this.getName();
	}
}

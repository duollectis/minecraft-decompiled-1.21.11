package net.minecraft.item;

import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.Validate;

/**
 * Предмет баннера, который можно размещать вертикально или на стене.
 * Делегирует логику цвета к соответствующему {@link AbstractBannerBlock}.
 */
public class BannerItem extends VerticallyAttachableBlockItem {

	public BannerItem(Block bannerBlock, Block wallBannerBlock, Item.Settings settings) {
		super(bannerBlock, wallBannerBlock, Direction.DOWN, settings);
		Validate.isInstanceOf(AbstractBannerBlock.class, bannerBlock);
		Validate.isInstanceOf(AbstractBannerBlock.class, wallBannerBlock);
	}

	public DyeColor getColor() {
		return ((AbstractBannerBlock) getBlock()).getColor();
	}
}

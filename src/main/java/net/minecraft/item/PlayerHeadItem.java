package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

/**
 * Предмет «Голова игрока». Если в компоненте {@link DataComponentTypes#PROFILE}
 * задано имя профиля, отображает его в названии предмета через ключ перевода
 * {@code <translationKey>.named}.
 */
public class PlayerHeadItem extends VerticallyAttachableBlockItem {

	public PlayerHeadItem(Block block, Block wallBlock, Item.Settings settings) {
		super(block, wallBlock, Direction.DOWN, settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);

		return profile != null && profile.getName().isPresent()
			? Text.translatable(translationKey + ".named", profile.getName().get())
			: super.getName(stack);
	}
}

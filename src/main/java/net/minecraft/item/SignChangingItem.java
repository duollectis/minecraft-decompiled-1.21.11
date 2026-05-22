package net.minecraft.item;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Интерфейс для предметов, способных изменять текст на вывеске.
 * Реализуется предметами красителей, чернил и светящихся чернил.
 */
public interface SignChangingItem {

	boolean useOnSign(World world, SignBlockEntity signBlockEntity, boolean front, PlayerEntity player);

	default boolean canUseOnSignText(SignText signText, PlayerEntity player) {
		return signText.hasText(player);
	}
}

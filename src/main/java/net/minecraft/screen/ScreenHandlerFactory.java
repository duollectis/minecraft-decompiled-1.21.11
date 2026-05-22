package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.jspecify.annotations.Nullable;

/**
 * Фабрика для создания экземпляров {@link ScreenHandler}.
 * <p>
 * Вызывается сервером при открытии игроком интерфейса блока или сущности.
 * Может вернуть {@code null}, если открытие экрана в данный момент невозможно
 * (например, блок уже занят другим игроком).
 */
@FunctionalInterface
public interface ScreenHandlerFactory {

	@Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player);
}

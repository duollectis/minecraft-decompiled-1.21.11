package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Простая неизменяемая реализация {@link NamedScreenHandlerFactory}.
 * <p>
 * Оборачивает произвольную {@link ScreenHandlerFactory} и фиксированный заголовок,
 * позволяя открывать именованные экраны без создания отдельного класса.
 */
public final class SimpleNamedScreenHandlerFactory implements NamedScreenHandlerFactory {

	private final Text name;
	private final ScreenHandlerFactory baseFactory;

	public SimpleNamedScreenHandlerFactory(ScreenHandlerFactory baseFactory, Text name) {
		this.baseFactory = baseFactory;
		this.name = name;
	}

	@Override
	public Text getDisplayName() {
		return name;
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return baseFactory.createMenu(syncId, playerInventory, player);
	}
}

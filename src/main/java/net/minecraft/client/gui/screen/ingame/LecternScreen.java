package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

/**
 * Экран кафедры. Отображает книгу, лежащую на кафедре, с возможностью
 * перелистывания страниц через сервер и взятия книги (для операторов).
 */
@Environment(EnvType.CLIENT)
public class LecternScreen extends BookScreen implements ScreenHandlerProvider<LecternScreenHandler> {

	private static final int BUTTON_WIDTH = 98;
	private static final int PAGE_PREV_BUTTON_ID = 1;
	private static final int PAGE_NEXT_BUTTON_ID = 2;
	private static final int TAKE_BOOK_BUTTON_ID = 3;
	private static final int PAGE_JUMP_OFFSET = 100;
	private static final Text TAKE_BOOK_TEXT = Text.translatable("lectern.take_book");

	private final LecternScreenHandler handler;
	private final ScreenHandlerListener listener = new ScreenHandlerListener() {
		@Override
		public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
			LecternScreen.this.updatePageProvider();
		}

		@Override
		public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
			if (property == 0) {
				LecternScreen.this.updatePage();
			}
		}
	};

	public LecternScreen(LecternScreenHandler handler, PlayerInventory inventory, Text title) {
		this.handler = handler;
	}

	@Override
	public LecternScreenHandler getScreenHandler() {
		return handler;
	}

	@Override
	protected void init() {
		super.init();
		handler.addListener(listener);
	}

	@Override
	public void close() {
		client.player.closeHandledScreen();
		super.close();
	}

	@Override
	public void removed() {
		super.removed();
		handler.removeListener(listener);
	}

	@Override
	protected void addCloseButton() {
		if (client.player.canModifyBlocks()) {
			int buttonY = getCloseButtonY();
			int centerX = width / 2;

			addDrawableChild(
				ButtonWidget.builder(ScreenTexts.DONE, button -> close())
					.position(centerX - BUTTON_WIDTH - 2, buttonY)
					.width(BUTTON_WIDTH)
					.build()
			);
			addDrawableChild(
				ButtonWidget.builder(TAKE_BOOK_TEXT, button -> sendButtonPressPacket(TAKE_BOOK_BUTTON_ID))
					.position(centerX + 2, buttonY)
					.width(BUTTON_WIDTH)
					.build()
			);
		} else {
			super.addCloseButton();
		}
	}

	@Override
	protected void goToPreviousPage() {
		sendButtonPressPacket(PAGE_PREV_BUTTON_ID);
	}

	@Override
	protected void goToNextPage() {
		sendButtonPressPacket(PAGE_NEXT_BUTTON_ID);
	}

	@Override
	protected boolean jumpToPage(int page) {
		if (page == handler.getPage()) {
			return false;
		}

		sendButtonPressPacket(PAGE_JUMP_OFFSET + page);
		return true;
	}

	private void sendButtonPressPacket(int id) {
		client.interactionManager.clickButton(handler.syncId, id);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	void updatePageProvider() {
		ItemStack bookItem = handler.getBookItem();
		setPageProvider(Objects.requireNonNullElse(
			BookScreen.Contents.create(bookItem),
			BookScreen.EMPTY_PROVIDER
		));
	}

	void updatePage() {
		setPage(handler.getPage());
	}

	@Override
	protected void closeScreen() {
		client.player.closeHandledScreen();
	}
}

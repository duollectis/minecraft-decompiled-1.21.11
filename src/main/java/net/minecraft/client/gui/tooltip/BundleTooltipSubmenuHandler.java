package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Scroller;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.joml.Vector2i;

/**
 * Обработчик подменю тултипа для предметов типа «сумка» (bundle).
 * Перехватывает прокрутку колёсика мыши над слотом с сумкой и
 * циклически переключает выбранный предмет внутри неё,
 * отправляя пакет {@link BundleItemSelectedC2SPacket} на сервер.
 */
@Environment(EnvType.CLIENT)
public class BundleTooltipSubmenuHandler implements TooltipSubmenuHandler {

	/** Индекс, означающий «ничего не выбрано» — сбрасывает выделение. */
	private static final int NO_SELECTION = -1;

	private final MinecraftClient client;
	private final Scroller scroller;

	public BundleTooltipSubmenuHandler(MinecraftClient client) {
		this.client = client;
		scroller = new Scroller();
	}

	@Override
	public boolean isApplicableTo(Slot slot) {
		return slot.getStack().isIn(ItemTags.BUNDLES);
	}

	/**
	 * Обрабатывает прокрутку: вычисляет направление (вертикаль приоритетнее горизонтали),
	 * циклически сдвигает индекс выбранного предмета и отправляет пакет на сервер.
	 */
	@Override
	public boolean onScroll(double horizontal, double vertical, int slotId, ItemStack item) {
		int stackCount = BundleItem.getNumberOfStacksShown(item);
		if (stackCount == 0) {
			return false;
		}

		Vector2i scrollDelta = scroller.update(horizontal, vertical);
		int delta = scrollDelta.y == 0 ? -scrollDelta.x : scrollDelta.y;

		if (delta != 0) {
			int currentIndex = BundleItem.getSelectedStackIndex(item);
			int newIndex = Scroller.scrollCycling(delta, currentIndex, stackCount);

			if (currentIndex != newIndex) {
				sendPacket(item, slotId, newIndex);
			}
		}

		return true;
	}

	@Override
	public void reset(Slot slot) {
		reset(slot.getStack(), slot.id);
	}

	@Override
	public void onMouseClick(Slot slot, SlotActionType actionType) {
		if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP) {
			reset(slot.getStack(), slot.id);
		}
	}

	/**
	 * Сбрасывает выделение предмета в сумке, отправляя индекс {@value #NO_SELECTION}.
	 *
	 * @param item   стек предмета-сумки
	 * @param slotId идентификатор слота в контейнере
	 */
	public void reset(ItemStack item, int slotId) {
		sendPacket(item, slotId, NO_SELECTION);
	}

	private void sendPacket(ItemStack item, int slotId, int selectedItemIndex) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler == null || selectedItemIndex >= BundleItem.getNumberOfStacksShown(item)) {
			return;
		}

		BundleItem.setSelectedStackIndex(item, selectedItemIndex);
		networkHandler.sendPacket(new BundleItemSelectedC2SPacket(slotId, selectedItemIndex));
	}
}

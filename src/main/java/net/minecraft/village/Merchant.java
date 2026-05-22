package net.minecraft.village;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

/**
 * Контракт торговца — сущности, способной предлагать игроку сделки.
 * <p>
 * Реализуется как жителями деревни, так и странствующими торговцами.
 * Разделяет серверную и клиентскую стороны через {@link #isClient()}.
 */
public interface Merchant {

	void setCustomer(@Nullable PlayerEntity customer);

	@Nullable PlayerEntity getCustomer();

	TradeOfferList getOffers();

	void setOffersFromServer(TradeOfferList offers);

	void trade(TradeOffer offer);

	void onSellingItem(ItemStack stack);

	int getExperience();

	void setExperienceFromServer(int experience);

	boolean isLeveledMerchant();

	SoundEvent getYesSound();

	default boolean canRefreshTrades() {
		return false;
	}

	/**
	 * Открывает экран торговли для игрока и синхронизирует список предложений.
	 *
	 * @param player        игрок, открывающий торговлю
	 * @param name          отображаемое имя торговца
	 * @param levelProgress текущий уровень торговца для отображения прогресса
	 */
	default void sendOffers(PlayerEntity player, Text name, int levelProgress) {
		OptionalInt syncId = player.openHandledScreen(
				new SimpleNamedScreenHandlerFactory(
						(id, playerInventory, playerx) -> new MerchantScreenHandler(id, playerInventory, this),
						name
				)
		);

		if (syncId.isEmpty()) {
			return;
		}

		TradeOfferList offers = getOffers();

		if (offers.isEmpty()) {
			return;
		}

		player.sendTradeOffers(
				syncId.getAsInt(),
				offers,
				levelProgress,
				getExperience(),
				isLeveledMerchant(),
				canRefreshTrades()
		);
	}

	boolean isClient();

	boolean canInteract(PlayerEntity player);
}

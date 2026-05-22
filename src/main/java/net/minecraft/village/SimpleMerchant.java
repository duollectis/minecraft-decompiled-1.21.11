package net.minecraft.village;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jspecify.annotations.Nullable;

/**
 * Простая реализация торговца, привязанная к конкретному игроку.
 * <p>
 * Используется для странствующих торговцев и других сущностей,
 * у которых нет сложной логики уровней и репутации.
 */
public class SimpleMerchant implements Merchant {

	private final PlayerEntity player;
	private TradeOfferList offers = new TradeOfferList();
	private int experience;

	public SimpleMerchant(PlayerEntity player) {
		this.player = player;
	}

	@Override
	public PlayerEntity getCustomer() {
		return player;
	}

	@Override
	public void setCustomer(@Nullable PlayerEntity customer) {
	}

	@Override
	public TradeOfferList getOffers() {
		return offers;
	}

	@Override
	public void setOffersFromServer(TradeOfferList offers) {
		this.offers = offers;
	}

	@Override
	public void trade(TradeOffer offer) {
		offer.use();
	}

	@Override
	public void onSellingItem(ItemStack stack) {
	}

	@Override
	public boolean isClient() {
		return player.getEntityWorld().isClient();
	}

	@Override
	public boolean canInteract(PlayerEntity other) {
		return player == other;
	}

	@Override
	public int getExperience() {
		return experience;
	}

	@Override
	public void setExperienceFromServer(int experience) {
		this.experience = experience;
	}

	@Override
	public boolean isLeveledMerchant() {
		return true;
	}

	@Override
	public SoundEvent getYesSound() {
		return SoundEvents.ENTITY_VILLAGER_YES;
	}
}

package net.minecraft.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * Одно торговое предложение жителя или торговца.
 * <p>
 * Содержит один или два входных предмета ({@code firstBuyItem}, {@code secondBuyItem})
 * и один выходной ({@code sellItem}). Цена первого предмета динамически корректируется
 * через {@code specialPrice} (репутация игрока) и {@code demandBonus} (спрос).
 */
public class TradeOffer {

	public static final int DEFAULT_MAX_USES = 4;
	public static final int MAX_EMERALD_PRICE = 64;

	public static final Codec<TradeOffer> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					TradedItem.CODEC.fieldOf("buy").forGetter(offer -> offer.firstBuyItem),
					TradedItem.CODEC.lenientOptionalFieldOf("buyB").forGetter(offer -> offer.secondBuyItem),
					ItemStack.CODEC.fieldOf("sell").forGetter(offer -> offer.sellItem),
					Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(offer -> offer.uses),
					Codec.INT.lenientOptionalFieldOf("maxUses", DEFAULT_MAX_USES).forGetter(offer -> offer.maxUses),
					Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(offer -> offer.rewardingPlayerExperience),
					Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(offer -> offer.specialPrice),
					Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(offer -> offer.demandBonus),
					Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", 0.0F).forGetter(offer -> offer.priceMultiplier),
					Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(offer -> offer.merchantExperience)
			).apply(instance, TradeOffer::new)
	);

	public static final PacketCodec<RegistryByteBuf, TradeOffer> PACKET_CODEC =
			PacketCodec.ofStatic(TradeOffer::write, TradeOffer::read);

	private final TradedItem firstBuyItem;
	private final Optional<TradedItem> secondBuyItem;
	private final ItemStack sellItem;
	private int uses;
	private final int maxUses;
	private final boolean rewardingPlayerExperience;
	private int specialPrice;
	private int demandBonus;
	private final float priceMultiplier;
	private final int merchantExperience;

	private TradeOffer(
			TradedItem firstBuyItem,
			Optional<TradedItem> secondBuyItem,
			ItemStack sellItem,
			int uses,
			int maxUses,
			boolean rewardingPlayerExperience,
			int specialPrice,
			int demandBonus,
			float priceMultiplier,
			int merchantExperience
	) {
		this.firstBuyItem = firstBuyItem;
		this.secondBuyItem = secondBuyItem;
		this.sellItem = sellItem;
		this.uses = uses;
		this.maxUses = maxUses;
		this.rewardingPlayerExperience = rewardingPlayerExperience;
		this.specialPrice = specialPrice;
		this.demandBonus = demandBonus;
		this.priceMultiplier = priceMultiplier;
		this.merchantExperience = merchantExperience;
	}

	public TradeOffer(
			TradedItem buyItem,
			ItemStack sellItem,
			int maxUses,
			int merchantExperience,
			float priceMultiplier
	) {
		this(buyItem, Optional.empty(), sellItem, maxUses, merchantExperience, priceMultiplier);
	}

	public TradeOffer(
			TradedItem firstBuyItem,
			Optional<TradedItem> secondBuyItem,
			ItemStack sellItem,
			int maxUses,
			int merchantExperience,
			float priceMultiplier
	) {
		this(firstBuyItem, secondBuyItem, sellItem, 0, maxUses, merchantExperience, priceMultiplier);
	}

	public TradeOffer(
			TradedItem firstBuyItem,
			Optional<TradedItem> secondBuyItem,
			ItemStack sellItem,
			int uses,
			int maxUses,
			int merchantExperience,
			float priceMultiplier
	) {
		this(firstBuyItem, secondBuyItem, sellItem, uses, maxUses, merchantExperience, priceMultiplier, 0);
	}

	public TradeOffer(
			TradedItem firstBuyItem,
			Optional<TradedItem> secondBuyItem,
			ItemStack sellItem,
			int uses,
			int maxUses,
			int merchantExperience,
			float priceMultiplier,
			int demandBonus
	) {
		this(
				firstBuyItem,
				secondBuyItem,
				sellItem,
				uses,
				maxUses,
				true,
				0,
				demandBonus,
				priceMultiplier,
				merchantExperience
		);
	}

	private TradeOffer(TradeOffer offer) {
		this(
				offer.firstBuyItem,
				offer.secondBuyItem,
				offer.sellItem.copy(),
				offer.uses,
				offer.maxUses,
				offer.rewardingPlayerExperience,
				offer.specialPrice,
				offer.demandBonus,
				offer.priceMultiplier,
				offer.merchantExperience
		);
	}

	public ItemStack getOriginalFirstBuyItem() {
		return firstBuyItem.itemStack();
	}

	public ItemStack getDisplayedFirstBuyItem() {
		return firstBuyItem.itemStack().copyWithCount(calculateFirstBuyCount(firstBuyItem));
	}

	public ItemStack getDisplayedSecondBuyItem() {
		return secondBuyItem.map(TradedItem::itemStack).orElse(ItemStack.EMPTY);
	}

	public TradedItem getFirstBuyItem() {
		return firstBuyItem;
	}

	public Optional<TradedItem> getSecondBuyItem() {
		return secondBuyItem;
	}

	public ItemStack getSellItem() {
		return sellItem;
	}

	public ItemStack copySellItem() {
		return sellItem.copy();
	}

	public int getUses() {
		return uses;
	}

	public int getMaxUses() {
		return maxUses;
	}

	public int getDemandBonus() {
		return demandBonus;
	}

	public int getSpecialPrice() {
		return specialPrice;
	}

	public float getPriceMultiplier() {
		return priceMultiplier;
	}

	public int getMerchantExperience() {
		return merchantExperience;
	}

	public boolean isDisabled() {
		return uses >= maxUses;
	}

	public boolean hasBeenUsed() {
		return uses > 0;
	}

	public boolean shouldRewardPlayerExperience() {
		return rewardingPlayerExperience;
	}

	public void use() {
		uses++;
	}

	public void resetUses() {
		uses = 0;
	}

	public void disable() {
		uses = maxUses;
	}

	public void setSpecialPrice(int specialPrice) {
		this.specialPrice = specialPrice;
	}

	public void increaseSpecialPrice(int increment) {
		specialPrice += increment;
	}

	public void clearSpecialPrice() {
		specialPrice = 0;
	}

	/**
	 * Пересчитывает бонус спроса после завершения сделки.
	 * <p>
	 * Формула: {@code demand = demand + uses - (maxUses - uses)}.
	 * Чем чаще покупают — тем выше спрос, чем больше остаток — тем ниже.
	 */
	public void updateDemandBonus() {
		demandBonus = demandBonus + uses - (maxUses - uses);
	}

	/**
	 * Проверяет, достаточно ли у игрока предметов для совершения сделки.
	 *
	 * @param firstStack  стек первого покупаемого предмета
	 * @param secondStack стек второго покупаемого предмета (может быть пустым)
	 * @return {@code true}, если оба стека удовлетворяют условиям сделки
	 */
	public boolean matchesBuyItems(ItemStack firstStack, ItemStack secondStack) {
		if (!firstBuyItem.matches(firstStack) || firstStack.getCount() < calculateFirstBuyCount(firstBuyItem)) {
			return false;
		}

		return secondBuyItem.isEmpty()
				? secondStack.isEmpty()
				: secondBuyItem.get().matches(secondStack) && secondStack.getCount() >= secondBuyItem.get().count();
	}

	/**
	 * Списывает предметы из стеков игрока при совершении сделки.
	 *
	 * @param firstBuyStack  стек первого предмета (изменяется in-place)
	 * @param secondBuyStack стек второго предмета (изменяется in-place)
	 * @return {@code true}, если списание прошло успешно
	 */
	public boolean depleteBuyItems(ItemStack firstBuyStack, ItemStack secondBuyStack) {
		if (!matchesBuyItems(firstBuyStack, secondBuyStack)) {
			return false;
		}

		firstBuyStack.decrement(getDisplayedFirstBuyItem().getCount());

		if (!getDisplayedSecondBuyItem().isEmpty()) {
			secondBuyStack.decrement(getDisplayedSecondBuyItem().getCount());
		}

		return true;
	}

	public TradeOffer copy() {
		return new TradeOffer(this);
	}

	/**
	 * Вычисляет итоговое количество первого покупаемого предмета с учётом
	 * бонуса спроса, множителя цены и специальной скидки от репутации.
	 * <p>
	 * Итоговая цена зажата в диапазоне [1, maxCount предмета].
	 */
	private int calculateFirstBuyCount(TradedItem item) {
		int baseCount = item.count();
		int demandAdjustment = Math.max(0, MathHelper.floor(baseCount * demandBonus * priceMultiplier));
		return MathHelper.clamp(baseCount + demandAdjustment + specialPrice, 1, item.itemStack().getMaxCount());
	}

	private static void write(RegistryByteBuf buf, TradeOffer offer) {
		TradedItem.PACKET_CODEC.encode(buf, offer.getFirstBuyItem());
		ItemStack.PACKET_CODEC.encode(buf, offer.getSellItem());
		TradedItem.OPTIONAL_PACKET_CODEC.encode(buf, offer.getSecondBuyItem());
		buf.writeBoolean(offer.isDisabled());
		buf.writeInt(offer.getUses());
		buf.writeInt(offer.getMaxUses());
		buf.writeInt(offer.getMerchantExperience());
		buf.writeInt(offer.getSpecialPrice());
		buf.writeFloat(offer.getPriceMultiplier());
		buf.writeInt(offer.getDemandBonus());
	}

	public static TradeOffer read(RegistryByteBuf buf) {
		TradedItem firstBuyItem = TradedItem.PACKET_CODEC.decode(buf);
		ItemStack sellItem = ItemStack.PACKET_CODEC.decode(buf);
		Optional<TradedItem> secondBuyItem = TradedItem.OPTIONAL_PACKET_CODEC.decode(buf);
		boolean disabled = buf.readBoolean();
		int uses = buf.readInt();
		int maxUses = buf.readInt();
		int merchantExperience = buf.readInt();
		int specialPrice = buf.readInt();
		float priceMultiplier = buf.readFloat();
		int demandBonus = buf.readInt();

		TradeOffer offer = new TradeOffer(firstBuyItem, secondBuyItem, sellItem, uses, maxUses, merchantExperience, priceMultiplier, demandBonus);

		if (disabled) {
			offer.disable();
		}

		offer.setSpecialPrice(specialPrice);
		return offer;
	}
}

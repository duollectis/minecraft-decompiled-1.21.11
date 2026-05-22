package net.minecraft.village;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Список торговых предложений жителя или торговца.
 * <p>
 * Расширяет {@link ArrayList} для удобной сериализации через кодеки.
 * Предоставляет метод поиска подходящего предложения по входным предметам.
 */
public class TradeOfferList extends ArrayList<TradeOffer> {

	public static final Codec<TradeOfferList> CODEC = TradeOffer.CODEC
			.listOf()
			.optionalFieldOf("Recipes", List.of())
			.xmap(TradeOfferList::new, Function.identity())
			.codec();

	public static final PacketCodec<RegistryByteBuf, TradeOfferList> PACKET_CODEC = TradeOffer.PACKET_CODEC
			.collect(PacketCodecs.toCollection(TradeOfferList::new));

	public TradeOfferList() {
	}

	private TradeOfferList(int size) {
		super(size);
	}

	private TradeOfferList(Collection<TradeOffer> tradeOffers) {
		super(tradeOffers);
	}

	/**
	 * Ищет первое подходящее предложение для заданных входных предметов.
	 * <p>
	 * Если {@code index} валиден (больше 0 и в пределах списка), сначала проверяет
	 * предложение по этому индексу — это оптимизация для повторного использования
	 * последнего выбранного предложения. Иначе перебирает весь список.
	 *
	 * @param firstBuyItem  первый покупаемый предмет
	 * @param secondBuyItem второй покупаемый предмет
	 * @param index         предпочтительный индекс предложения
	 * @return подходящее предложение или {@code null}, если ничего не найдено
	 */
	public @Nullable TradeOffer getValidOffer(ItemStack firstBuyItem, ItemStack secondBuyItem, int index) {
		if (index > 0 && index < size()) {
			TradeOffer offer = get(index);
			return offer.matchesBuyItems(firstBuyItem, secondBuyItem) ? offer : null;
		}

		for (int i = 0; i < size(); i++) {
			TradeOffer offer = get(i);

			if (offer.matchesBuyItems(firstBuyItem, secondBuyItem)) {
				return offer;
			}
		}

		return null;
	}

	public TradeOfferList copy() {
		TradeOfferList copy = new TradeOfferList(size());

		for (TradeOffer offer : this) {
			copy.add(offer.copy());
		}

		return copy;
	}
}

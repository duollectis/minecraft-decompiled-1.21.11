package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Хранит четыре черепка (sherd), украшающих стороны декоративного горшка:
 * заднюю, левую, правую и переднюю. Каждый черепок опционален —
 * отсутствующий слот кодируется как {@link Items#BRICK} при сериализации.
 */
public record Sherds(
		Optional<Item> back,
		Optional<Item> left,
		Optional<Item> right,
		Optional<Item> front
) implements TooltipAppender {

	public static final Sherds
			DEFAULT =
			new Sherds(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	public static final Codec<Sherds>
			CODEC =
			Registries.ITEM.getCodec().sizeLimitedListOf(4).xmap(Sherds::new, Sherds::toList);
	public static final PacketCodec<RegistryByteBuf, Sherds>
			PACKET_CODEC =
			PacketCodecs.registryValue(RegistryKeys.ITEM)
			            .collect(PacketCodecs.toList(4))
			            .xmap(Sherds::new, Sherds::toList);

	private Sherds(List<Item> sherds) {
		this(getSherd(sherds, 0), getSherd(sherds, 1), getSherd(sherds, 2), getSherd(sherds, 3));
	}

	public Sherds(Item back, Item left, Item right, Item front) {
		this(List.of(back, left, right, front));
	}

	private static Optional<Item> getSherd(List<Item> sherds, int index) {
		if (index >= sherds.size()) {
			return Optional.empty();
		}

		Item item = sherds.get(index);
		return item == Items.BRICK ? Optional.empty() : Optional.of(item);
	}

	public List<Item> toList() {
		return Stream.of(this.back, this.left, this.right, this.front).map(item -> item.orElse(Items.BRICK)).toList();
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (!this.equals(DEFAULT)) {
			textConsumer.accept(ScreenTexts.EMPTY);
			appendSherdTooltip(textConsumer, this.front);
			appendSherdTooltip(textConsumer, this.left);
			appendSherdTooltip(textConsumer, this.right);
			appendSherdTooltip(textConsumer, this.back);
		}
	}

	private static void appendSherdTooltip(Consumer<Text> textConsumer, Optional<Item> sherdItem) {
		textConsumer.accept(new ItemStack(sherdItem.orElse(Items.BRICK), 1)
				.getName()
				.copyContentOnly()
				.formatted(Formatting.GRAY));
	}
}

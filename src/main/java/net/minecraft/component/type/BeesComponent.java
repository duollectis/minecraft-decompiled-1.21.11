package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Consumer;

/**
	 * Компонент пчёл улья. Хранит список пчёл, находящихся внутри блока улья,
	 * и отображает их количество в тултипе.
	 */
public record BeesComponent(List<BeehiveBlockEntity.BeeData> bees) implements TooltipAppender {

	public static final Codec<BeesComponent>
			CODEC =
			BeehiveBlockEntity.BeeData.LIST_CODEC.xmap(BeesComponent::new, BeesComponent::bees);
	public static final PacketCodec<RegistryByteBuf, BeesComponent>
			PACKET_CODEC =
			BeehiveBlockEntity.BeeData.PACKET_CODEC
					.collect(PacketCodecs.toList())
					.xmap(BeesComponent::new, BeesComponent::bees);
	public static final int MAX_BEES = 3;
	public static final BeesComponent DEFAULT = new BeesComponent(List.of());

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		textConsumer.accept(Text
				.translatable("container.beehive.bees", bees.size(), MAX_BEES)
				.formatted(Formatting.GRAY));
	}
}

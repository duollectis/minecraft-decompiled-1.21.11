package net.minecraft.component.type;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
	 * Компонент слоёв узора баннера. Хранит упорядоченный список слоёв (паттерн + цвет).
	 * В тултипе отображается не более {@link #MAX_TOOLTIP_LAYERS} слоёв.
	 */
public record BannerPatternsComponent(List<BannerPatternsComponent.Layer> layers) implements TooltipAppender {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final BannerPatternsComponent DEFAULT = new BannerPatternsComponent(List.of());
	public static final Codec<BannerPatternsComponent> CODEC = BannerPatternsComponent.Layer.CODEC
			.listOf()
			.xmap(BannerPatternsComponent::new, BannerPatternsComponent::layers);
	public static final PacketCodec<RegistryByteBuf, BannerPatternsComponent>
			PACKET_CODEC =
			BannerPatternsComponent.Layer.PACKET_CODEC
					.collect(PacketCodecs.toList())
					.xmap(BannerPatternsComponent::new, BannerPatternsComponent::layers);

	private static final int MAX_TOOLTIP_LAYERS = 6;

	/**
		 * Возвращает новый компонент без последнего (верхнего) слоя узора.
		 * Используется при снятии верхнего паттерна с баннера.
		 *
		 * @return компонент без верхнего слоя
		 */
	public BannerPatternsComponent withoutTopLayer() {
		return new BannerPatternsComponent(List.copyOf(layers.subList(0, layers.size() - 1)));
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		int visibleCount = Math.min(layers().size(), MAX_TOOLTIP_LAYERS);
		for (int index = 0; index < visibleCount; index++) {
			textConsumer.accept(layers().get(index).getTooltipText().formatted(Formatting.GRAY));
		}
	}

	public static class Builder {

		private final ImmutableList.Builder<BannerPatternsComponent.Layer> entries = ImmutableList.builder();

		@Deprecated
		public BannerPatternsComponent.Builder add(
				RegistryEntryLookup<BannerPattern> patternLookup,
				RegistryKey<BannerPattern> pattern,
				DyeColor color
		) {
			Optional<RegistryEntry.Reference<BannerPattern>> found = patternLookup.getOptional(pattern);
			if (found.isEmpty()) {
				LOGGER.warn("Unable to find banner pattern with id: '{}'", pattern.getValue());
				return this;
			}

			return add(found.get(), color);
		}

		public BannerPatternsComponent.Builder add(RegistryEntry<BannerPattern> pattern, DyeColor color) {
			return add(new BannerPatternsComponent.Layer(pattern, color));
		}

		public BannerPatternsComponent.Builder add(BannerPatternsComponent.Layer layer) {
			entries.add(layer);
			return this;
		}

		public BannerPatternsComponent.Builder addAll(BannerPatternsComponent patterns) {
			entries.addAll(patterns.layers);
			return this;
		}

		public BannerPatternsComponent build() {
			return new BannerPatternsComponent(entries.build());
		}
	}

	/**
		 * Один слой узора баннера: паттерн и цвет краски.
		 */
	public record Layer(RegistryEntry<BannerPattern> pattern, DyeColor color) {

		public static final Codec<BannerPatternsComponent.Layer> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											BannerPattern.ENTRY_CODEC.fieldOf("pattern").forGetter(BannerPatternsComponent.Layer::pattern),
											DyeColor.CODEC.fieldOf("color").forGetter(BannerPatternsComponent.Layer::color)
									)
									.apply(instance, BannerPatternsComponent.Layer::new)
		);
		public static final PacketCodec<RegistryByteBuf, BannerPatternsComponent.Layer>
				PACKET_CODEC =
				PacketCodec.tuple(
						BannerPattern.ENTRY_PACKET_CODEC,
						BannerPatternsComponent.Layer::pattern,
						DyeColor.PACKET_CODEC,
						BannerPatternsComponent.Layer::color,
						BannerPatternsComponent.Layer::new
				);

		public MutableText getTooltipText() {
			String string = this.pattern.value().translationKey();
			return Text.translatable(string + "." + this.color.getId());
		}
	}
}

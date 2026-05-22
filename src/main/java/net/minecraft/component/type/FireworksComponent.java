package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.function.Consumer;

/**
	 * Компонент фейерверка предмета. Хранит длительность полёта и список взрывов.
	 * При отображении подсказки группирует одинаковые взрывы для компактности.
	 */
public record FireworksComponent(
		int flightDuration,
		List<FireworkExplosionComponent> explosions
) implements TooltipAppender {

	public static final int MAX_EXPLOSIONS = 256;
	public static final Codec<FireworksComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.UNSIGNED_BYTE
												.optionalFieldOf("flight_duration", 0)
												.forGetter(FireworksComponent::flightDuration),
										FireworkExplosionComponent.CODEC
												.sizeLimitedListOf(MAX_EXPLOSIONS)
												.optionalFieldOf("explosions", List.of())
												.forGetter(FireworksComponent::explosions)
								)
								.apply(instance, FireworksComponent::new)
	);
	public static final PacketCodec<ByteBuf, FireworksComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			FireworksComponent::flightDuration,
			FireworkExplosionComponent.PACKET_CODEC.collect(PacketCodecs.toList(MAX_EXPLOSIONS)),
			FireworksComponent::explosions,
			FireworksComponent::new
	);

	public FireworksComponent(int flightDuration, List<FireworkExplosionComponent> explosions) {
		if (explosions.size() > MAX_EXPLOSIONS) {
			throw new IllegalArgumentException(
				"Got " + explosions.size() + " explosions, but maximum is " + MAX_EXPLOSIONS
			);
		}

		this.flightDuration = flightDuration;
		this.explosions = explosions;
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (flightDuration > 0) {
			textConsumer.accept(
				Text.translatable("item.minecraft.firework_rocket.flight")
					.append(ScreenTexts.SPACE)
					.append(String.valueOf(flightDuration))
					.formatted(Formatting.GRAY)
			);
		}

		FireworkExplosionComponent current = null;
		int count = 0;

		for (FireworkExplosionComponent explosion : explosions) {
			if (current == null) {
				current = explosion;
				count = 1;
			} else if (current.equals(explosion)) {
				count++;
			} else {
				appendExplosionTooltip(textConsumer, current, count);
				current = explosion;
				count = 1;
			}
		}

		if (current != null) {
			appendExplosionTooltip(textConsumer, current, count);
		}
	}

	private static void appendExplosionTooltip(
			Consumer<Text> textConsumer,
			FireworkExplosionComponent explosionComponent,
			int stars
	) {
		Text shapeName = explosionComponent.shape().getName();
		MutableText label = stars == 1
			? Text.translatable("item.minecraft.firework_rocket.single_star", shapeName)
			: Text.translatable("item.minecraft.firework_rocket.multiple_stars", stars, shapeName);

		textConsumer.accept(label.formatted(Formatting.GRAY));

		explosionComponent.appendOptionalTooltip(tooltip -> textConsumer.accept(Text.literal("  ").append(tooltip)));
	}
}

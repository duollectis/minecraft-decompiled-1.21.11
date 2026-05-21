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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.function.Consumer;

/**
 * {@code FireworksComponent}.
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
							                    .sizeLimitedListOf(256)
							                    .optionalFieldOf("explosions", List.of())
							                    .forGetter(FireworksComponent::explosions)
			                    )
			                    .apply(instance, FireworksComponent::new)
	);
	public static final PacketCodec<ByteBuf, FireworksComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			FireworksComponent::flightDuration,
			FireworkExplosionComponent.PACKET_CODEC.collect(PacketCodecs.toList(256)),
			FireworksComponent::explosions,
			FireworksComponent::new
	);

	public FireworksComponent(int flightDuration, List<FireworkExplosionComponent> explosions) {
		if (explosions.size() > 256) {
			throw new IllegalArgumentException("Got " + explosions.size() + " explosions, but maximum is 256");
		}
		else {
			this.flightDuration = flightDuration;
			this.explosions = explosions;
		}
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (this.flightDuration > 0) {
			textConsumer.accept(
					Text.translatable("item.minecraft.firework_rocket.flight")
					    .append(ScreenTexts.SPACE)
					    .append(String.valueOf(this.flightDuration))
					    .formatted(Formatting.GRAY)
			);
		}

		FireworkExplosionComponent fireworkExplosionComponent = null;
		int i = 0;

		for (FireworkExplosionComponent fireworkExplosionComponent2 : this.explosions) {
			if (fireworkExplosionComponent == null) {
				fireworkExplosionComponent = fireworkExplosionComponent2;
				i = 1;
			}
			else if (fireworkExplosionComponent.equals(fireworkExplosionComponent2)) {
				i++;
			}
			else {
				appendExplosionTooltip(textConsumer, fireworkExplosionComponent, i);
				fireworkExplosionComponent = fireworkExplosionComponent2;
				i = 1;
			}
		}

		if (fireworkExplosionComponent != null) {
			appendExplosionTooltip(textConsumer, fireworkExplosionComponent, i);
		}
	}

	private static void appendExplosionTooltip(
			Consumer<Text> textConsumer,
			FireworkExplosionComponent explosionComponent,
			int stars
	) {
		Text text = explosionComponent.shape().getName();
		if (stars == 1) {
			textConsumer.accept(Text
					.translatable("item.minecraft.firework_rocket.single_star", text)
					.formatted(Formatting.GRAY));
		}
		else {
			textConsumer.accept(Text
					.translatable("item.minecraft.firework_rocket.multiple_stars", stars, text)
					.formatted(Formatting.GRAY));
		}

		explosionComponent.appendOptionalTooltip(tooltip -> textConsumer.accept(Text.literal("  ").append(tooltip)));
	}
}

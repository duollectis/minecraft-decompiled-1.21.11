package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.component.ComponentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;
import java.util.SequencedSet;

/**
 * {@code TooltipDisplayComponent}.
 */
public record TooltipDisplayComponent(boolean hideTooltip, SequencedSet<ComponentType<?>> hiddenComponents) {

	private static final Codec<SequencedSet<ComponentType<?>>> HIDDEN_COMPONENTS_CODEC = ComponentType.CODEC
			.listOf()
			.xmap(ReferenceLinkedOpenHashSet::new, List::copyOf);
	public static final Codec<TooltipDisplayComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TooltipDisplayComponent::hideTooltip),
					                    HIDDEN_COMPONENTS_CODEC
							                    .optionalFieldOf("hidden_components", ReferenceSortedSets.emptySet())
							                    .forGetter(TooltipDisplayComponent::hiddenComponents)
			                    )
			                    .apply(instance, TooltipDisplayComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, TooltipDisplayComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOLEAN,
			TooltipDisplayComponent::hideTooltip,
			ComponentType.PACKET_CODEC.collect(PacketCodecs.toCollection(ReferenceLinkedOpenHashSet::new)),
			TooltipDisplayComponent::hiddenComponents,
			TooltipDisplayComponent::new
	);
	public static final TooltipDisplayComponent
			DEFAULT =
			new TooltipDisplayComponent(false, ReferenceSortedSets.emptySet());

	public TooltipDisplayComponent with(ComponentType<?> component, boolean hidden) {
		if (this.hiddenComponents.contains(component) == hidden) {
			return this;
		}
		else {
			SequencedSet<ComponentType<?>> sequencedSet = new ReferenceLinkedOpenHashSet(this.hiddenComponents);
			if (hidden) {
				sequencedSet.add(component);
			}
			else {
				sequencedSet.remove(component);
			}

			return new TooltipDisplayComponent(this.hideTooltip, sequencedSet);
		}
	}

	public boolean shouldDisplay(ComponentType<?> component) {
		return !this.hideTooltip && !this.hiddenComponents.contains(component);
	}
}

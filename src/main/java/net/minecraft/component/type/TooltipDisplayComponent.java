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
	 * Компонент управления отображением тултипа. Позволяет полностью скрыть тултип
	 * предмета или скрыть отдельные компоненты из него.
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

	/**
		 * Возвращает новый компонент с изменённым состоянием видимости указанного компонента.
		 * Если состояние уже совпадает — возвращает {@code this} без создания нового объекта.
		 *
		 * @param component компонент, видимость которого нужно изменить
		 * @param hidden    {@code true} — скрыть компонент, {@code false} — показать
		 * @return обновлённый {@code TooltipDisplayComponent}
		 */
	public TooltipDisplayComponent with(ComponentType<?> component, boolean hidden) {
		if (hiddenComponents.contains(component) == hidden) {
			return this;
		}

		SequencedSet<ComponentType<?>> updated = new ReferenceLinkedOpenHashSet(hiddenComponents);

		if (hidden) {
			updated.add(component);
		} else {
			updated.remove(component);
		}

		return new TooltipDisplayComponent(hideTooltip, updated);
	}

	/**
		 * Проверяет, должен ли указанный компонент отображаться в тултипе.
		 *
		 * @param component проверяемый компонент
		 * @return {@code true} если тултип не скрыт и компонент не в списке скрытых
		 */
	public boolean shouldDisplay(ComponentType<?> component) {
		if (hideTooltip) {
			return false;
		}

		return !hiddenComponents.contains(component);
	}
}

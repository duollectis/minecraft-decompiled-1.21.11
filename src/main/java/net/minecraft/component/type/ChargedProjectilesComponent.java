package net.minecraft.component.type;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
	 * Компонент заряженных снарядов (арбалет). Хранит список снарядов в виде иммутабельных копий.
	 */
public final class ChargedProjectilesComponent implements TooltipAppender {

	public static final ChargedProjectilesComponent DEFAULT = new ChargedProjectilesComponent(List.of());
	public static final Codec<ChargedProjectilesComponent> CODEC = ItemStack.CODEC
			.listOf()
			.xmap(
					ChargedProjectilesComponent::new,
					chargedProjectilesComponent -> chargedProjectilesComponent.projectiles
			);
	public static final PacketCodec<RegistryByteBuf, ChargedProjectilesComponent> PACKET_CODEC = ItemStack.PACKET_CODEC
			.collect(PacketCodecs.toList())
			.xmap(ChargedProjectilesComponent::new, component -> component.projectiles);
	private final List<ItemStack> projectiles;

	private ChargedProjectilesComponent(List<ItemStack> projectiles) {
		this.projectiles = projectiles;
	}

	/**
		 * Создаёт компонент с одним снарядом (копия стека).
		 *
		 * @param projectile стек снаряда
		 * @return компонент с одним снарядом
		 */
	public static ChargedProjectilesComponent of(ItemStack projectile) {
		return new ChargedProjectilesComponent(List.of(projectile.copy()));
	}

	/**
		 * Создаёт компонент из списка снарядов (каждый стек копируется).
		 *
		 * @param projectiles список стеков снарядов
		 * @return компонент с иммутабельным списком копий снарядов
		 */
	public static ChargedProjectilesComponent of(List<ItemStack> projectiles) {
		return new ChargedProjectilesComponent(List.copyOf(Lists.transform(projectiles, ItemStack::copy)));
	}

	/**
		 * Проверяет, содержит ли компонент хотя бы один снаряд указанного типа предмета.
		 *
		 * @param item тип предмета для поиска
		 * @return {@code true} если снаряд данного типа присутствует
		 */
	public boolean contains(Item item) {
		for (ItemStack itemStack : this.projectiles) {
			if (itemStack.isOf(item)) {
				return true;
			}
		}

		return false;
	}

	public List<ItemStack> getProjectiles() {
		return Lists.transform(this.projectiles, ItemStack::copy);
	}

	public boolean isEmpty() {
		return this.projectiles.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		return this == o
				? true
				: o instanceof ChargedProjectilesComponent chargedProjectilesComponent
					&& ItemStack.stacksEqual(this.projectiles, chargedProjectilesComponent.projectiles);
	}

	@Override
	public int hashCode() {
		return ItemStack.listHashCode(this.projectiles);
	}

	@Override
	public String toString() {
		return "ChargedProjectiles[items=" + this.projectiles + "]";
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		ItemStack current = null;
		int count = 0;

		for (ItemStack projectile : projectiles) {
			if (current == null) {
				current = projectile;
				count = 1;
			}
			else if (ItemStack.areEqual(current, projectile)) {
				count++;
			}
			else {
				appendProjectileTooltip(context, textConsumer, current, count);
				current = projectile;
				count = 1;
			}
		}

		if (current != null) {
			appendProjectileTooltip(context, textConsumer, current, count);
		}
	}

	private static void appendProjectileTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			ItemStack projectile,
			int count
	) {
		if (count == 1) {
			textConsumer.accept(Text.translatable(
					"item.minecraft.crossbow.projectile.single",
					projectile.toHoverableText()
			));
		}
		else {
			textConsumer.accept(Text.translatable(
					"item.minecraft.crossbow.projectile.multiple",
					count,
					projectile.toHoverableText()
			));
		}

		TooltipDisplayComponent
				tooltipDisplayComponent =
				projectile.getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);
		projectile.appendTooltip(
				context,
				tooltipDisplayComponent,
				null,
				TooltipType.BASIC,
				tooltip -> textConsumer.accept(Text.literal("  ").append(tooltip).formatted(Formatting.GRAY))
		);
	}
}

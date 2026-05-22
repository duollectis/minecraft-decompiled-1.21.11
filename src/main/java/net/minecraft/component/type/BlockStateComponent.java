package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
	 * Компонент состояния блока предмета. Хранит свойства блока в виде строковых пар
	 * ключ-значение и применяет их к {@link BlockState} при размещении блока.
	 */
public record BlockStateComponent(Map<String, String> properties) implements TooltipAppender {

	public static final BlockStateComponent DEFAULT = new BlockStateComponent(Map.of());
	public static final Codec<BlockStateComponent> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
																.xmap(
																		BlockStateComponent::new,
																		BlockStateComponent::properties
																);
	private static final PacketCodec<ByteBuf, Map<String, String>> MAP_PACKET_CODEC = PacketCodecs.map(
			Object2ObjectOpenHashMap::new, PacketCodecs.STRING, PacketCodecs.STRING
	);
	public static final PacketCodec<ByteBuf, BlockStateComponent>
			PACKET_CODEC =
			MAP_PACKET_CODEC.xmap(BlockStateComponent::new, BlockStateComponent::properties);

	/**
		 * Возвращает новый компонент с добавленным или обновлённым свойством блока.
		 *
		 * @param property свойство блока
		 * @param value    новое значение свойства
		 * @return новый компонент с обновлённым свойством
		 */
	public <T extends Comparable<T>> BlockStateComponent with(Property<T> property, T value) {
		return new BlockStateComponent(Util.mapWith(this.properties, property.getName(), property.name(value)));
	}

	/**
		 * Возвращает новый компонент, скопировав значение свойства из переданного {@link BlockState}.
		 *
		 * @param property  свойство блока
		 * @param fromState состояние блока, из которого берётся значение
		 * @return новый компонент с обновлённым свойством
		 */
	public <T extends Comparable<T>> BlockStateComponent with(Property<T> property, BlockState fromState) {
		return this.with(property, fromState.get(property));
	}

	public <T extends Comparable<T>> @Nullable T getValue(Property<T> property) {
		String string = this.properties.get(property.getName());
		return string == null ? null : property.parse(string).orElse(null);
	}

	/**
		 * Применяет все сохранённые свойства к переданному {@link BlockState}.
		 * Свойства, отсутствующие у блока или имеющие некорректные значения, игнорируются.
		 *
		 * @param state исходное состояние блока
		 * @return состояние блока с применёнными свойствами
		 */
	public BlockState applyToState(BlockState state) {
		StateManager<Block, BlockState> stateManager = state.getBlock().getStateManager();

		for (Entry<String, String> entry : this.properties.entrySet()) {
			Property<?> property = stateManager.getProperty(entry.getKey());
			if (property != null) {
				state = applyToState(state, property, entry.getValue());
			}
		}

		return state;
	}

	private static <T extends Comparable<T>> BlockState applyToState(
			BlockState state,
			Property<T> property,
			String value
	) {
		return property.parse(value).map(parsed -> state.with(property, parsed)).orElse(state);
	}

	public boolean isEmpty() {
		return this.properties.isEmpty();
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		Integer integer = this.getValue(BeehiveBlock.HONEY_LEVEL);
		if (integer != null) {
			textConsumer.accept(Text.translatable("container.beehive.honey", integer, 5).formatted(Formatting.GRAY));
		}
	}
}

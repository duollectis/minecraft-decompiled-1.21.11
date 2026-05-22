package net.minecraft.world.gen.stateprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Поставщик состояний блоков, случайно изменяющий целочисленное свойство блока.
 * Делегирует выбор базового блока {@code source}, затем применяет случайное значение
 * из диапазона {@code values} к свойству с именем {@code propertyName}.
 */
public class RandomizedIntBlockStateProvider extends BlockStateProvider {

	public static final MapCodec<RandomizedIntBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BlockStateProvider.TYPE_CODEC
							.fieldOf("source")
							.forGetter(provider -> provider.source),
					Codec.STRING
							.fieldOf("property")
							.forGetter(provider -> provider.propertyName),
					IntProvider.VALUE_CODEC
							.fieldOf("values")
							.forGetter(provider -> provider.values)
			)
			.apply(instance, RandomizedIntBlockStateProvider::new)
	);
	private final BlockStateProvider source;
	private final String propertyName;
	private @Nullable IntProperty property;
	private final IntProvider values;

	public RandomizedIntBlockStateProvider(BlockStateProvider source, IntProperty property, IntProvider values) {
		this.source = source;
		this.property = property;
		this.propertyName = property.getName();
		this.values = values;
		Collection<Integer> validValues = property.getValues();

		for (int value = values.getMin(); value <= values.getMax(); value++) {
			if (!validValues.contains(value)) {
				throw new IllegalArgumentException("Property value out of range: " + property.getName() + ": " + value);
			}
		}
	}

	public RandomizedIntBlockStateProvider(BlockStateProvider source, String propertyName, IntProvider values) {
		this.source = source;
		this.propertyName = propertyName;
		this.values = values;
	}

	@Override
	protected BlockStateProviderType<?> getType() {
		return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
	}

	@Override
	public BlockState get(Random random, BlockPos pos) {
		BlockState blockState = source.get(random, pos);

		if (property == null || !blockState.contains(property)) {
			IntProperty resolved = getIntPropertyByName(blockState, propertyName);

			if (resolved == null) {
				return blockState;
			}

			property = resolved;
		}

		return blockState.with(property, values.get(random));
	}

	/**
	 * Ищет свойство типа {@link IntProperty} по имени среди свойств данного состояния блока.
	 * Используется для ленивой инициализации поля {@code property} при первом вызове {@code get}.
	 */
	private static @Nullable IntProperty getIntPropertyByName(BlockState state, String propertyName) {
		return state.getProperties()
				.stream()
				.filter(property -> property.getName().equals(propertyName))
				.filter(property -> property instanceof IntProperty)
				.map(property -> (IntProperty) property)
				.findAny()
				.orElse(null);
	}
}

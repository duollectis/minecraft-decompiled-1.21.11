package net.minecraft.world.gen.heightprovider;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import org.slf4j.Logger;

/**
 * Провайдер высоты с равномерным распределением в диапазоне [min, max].
 * Логирует предупреждение при пустом диапазоне (не более одного раза на пару min/max).
 */
public class UniformHeightProvider extends HeightProvider {

	public static final MapCodec<UniformHeightProvider> UNIFORM_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    YOffset.OFFSET_CODEC.fieldOf("min_inclusive").forGetter(provider -> provider.minOffset),
					                    YOffset.OFFSET_CODEC.fieldOf("max_inclusive").forGetter(provider -> provider.maxOffset)
			                    )
			                    .apply(instance, UniformHeightProvider::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final YOffset minOffset;
	private final YOffset maxOffset;
	private final LongSet warnedEmptyHeightRanges = new LongOpenHashSet();

	private UniformHeightProvider(YOffset minOffset, YOffset maxOffset) {
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
	}

	public static UniformHeightProvider create(YOffset minOffset, YOffset maxOffset) {
		return new UniformHeightProvider(minOffset, maxOffset);
	}

	@Override
	public int get(Random random, HeightContext context) {
		int minY = minOffset.getY(context);
		int maxY = maxOffset.getY(context);

		if (minY > maxY) {
			if (warnedEmptyHeightRanges.add((long) minY << 32 | maxY)) {
				LOGGER.warn("Empty height range: {}", this);
			}

			return minY;
		}

		return MathHelper.nextBetween(random, minY, maxY);
	}

	@Override
	public HeightProviderType<?> getType() {
		return HeightProviderType.UNIFORM;
	}

	@Override
	public String toString() {
		return "[" + minOffset + "-" + maxOffset + "]";
	}
}

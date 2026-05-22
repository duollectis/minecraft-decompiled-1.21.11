package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Фонарь из меди с поддержкой окисления. Деградирует со временем через случайные тики,
 * пока не достигнет максимального уровня окисления.
 */
public class OxidizableLanternBlock extends LanternBlock implements Oxidizable {

	public static final MapCodec<OxidizableLanternBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Oxidizable.OxidationLevel.CODEC
							                    .fieldOf("weathering_state")
							                    .forGetter(OxidizableLanternBlock::getDegradationLevel),
					                    createSettingsCodec()
			                    )
			                    .apply(instance, OxidizableLanternBlock::new)
	);
	private final Oxidizable.OxidationLevel oxidationLevel;

	@Override
	public MapCodec<OxidizableLanternBlock> getCodec() {
		return CODEC;
	}

	public OxidizableLanternBlock(Oxidizable.OxidationLevel oxidationLevel, AbstractBlock.Settings settings) {
		super(settings);
		this.oxidationLevel = oxidationLevel;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		tickDegradation(state, world, pos, random);
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return Oxidizable.getIncreasedOxidationBlock(state.getBlock()).isPresent();
	}

	public Oxidizable.OxidationLevel getDegradationLevel() {
		return oxidationLevel;
	}
}

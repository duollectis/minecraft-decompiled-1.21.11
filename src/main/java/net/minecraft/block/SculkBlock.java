package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.fluid.Fluids;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

/**
 * Блок скалка — распространяется по миру через механизм {@link SculkSpreadManager}.
 * При распространении может размещать сенсоры и крикуны поверх себя.
 * Не конвертируется в распространяемый блок (в отличие от скалк-вены).
 */
public class SculkBlock extends ExperienceDroppingBlock implements SculkSpreadable {

	public static final MapCodec<SculkBlock> CODEC = createCodec(SculkBlock::new);

	@Override
	public MapCodec<SculkBlock> getCodec() {
		return CODEC;
	}

	public SculkBlock(AbstractBlock.Settings settings) {
		super(ConstantIntProvider.create(1), settings);
	}

	@Override
	public int spread(
			SculkSpreadManager.Cursor cursor,
			WorldAccess world,
			BlockPos catalystPos,
			Random random,
			SculkSpreadManager spreadManager,
			boolean shouldConvertToBlock
	) {
		int charge = cursor.getCharge();

		if (charge == 0 || random.nextInt(spreadManager.getSpreadChance()) != 0) {
			return charge;
		}

		BlockPos cursorPos = cursor.getPos();
		boolean withinDistance = cursorPos.isWithinDistance(catalystPos, spreadManager.getMaxDistance());

		if (withinDistance || !shouldNotDecay(world, cursorPos)) {
			return random.nextInt(spreadManager.getDecayChance()) != 0
					? charge
					: charge - (withinDistance ? 1 : getDecay(spreadManager, cursorPos, catalystPos, charge));
		}

		int extraChance = spreadManager.getExtraBlockChance();

		if (random.nextInt(extraChance) < charge) {
			BlockPos abovePos = cursorPos.up();
			BlockState extraState = getExtraBlockState(world, abovePos, random, spreadManager.isWorldGen());

			world.setBlockState(abovePos, extraState, 3);
			world.playSound(null, cursorPos, extraState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		return Math.max(0, charge - extraChance);
	}

	private static int getDecay(
			SculkSpreadManager spreadManager,
			BlockPos cursorPos,
			BlockPos catalystPos,
			int charge
	) {
		int maxDistance = spreadManager.getMaxDistance();
		float distanceDelta = MathHelper.square((float) Math.sqrt(cursorPos.getSquaredDistance(catalystPos)) - maxDistance);
		int maxDelta = MathHelper.square(24 - maxDistance);
		float ratio = Math.min(1.0F, distanceDelta / maxDelta);

		return Math.max(1, (int) (charge * ratio * 0.5F));
	}

	private BlockState getExtraBlockState(WorldAccess world, BlockPos pos, Random random, boolean allowShrieker) {
		// Шанс 1/11 на крикун, иначе — сенсор
		BlockState extraState = random.nextInt(11) == 0
				? Blocks.SCULK_SHRIEKER.getDefaultState().with(SculkShriekerBlock.CAN_SUMMON, allowShrieker)
				: Blocks.SCULK_SENSOR.getDefaultState();

		return extraState.contains(Properties.WATERLOGGED) && !world.getFluidState(pos).isEmpty()
				? extraState.with(Properties.WATERLOGGED, true)
				: extraState;
	}

	private static boolean shouldNotDecay(WorldAccess world, BlockPos pos) {
		BlockState aboveState = world.getBlockState(pos.up());

		if (!aboveState.isAir() && !(aboveState.isOf(Blocks.WATER) && aboveState.getFluidState().isOf(Fluids.WATER))) {
			return false;
		}

		int nearbyCount = 0;

		for (BlockPos nearbyPos : BlockPos.iterate(pos.add(-4, 0, -4), pos.add(4, 2, 4))) {
			BlockState nearbyState = world.getBlockState(nearbyPos);

			if (nearbyState.isOf(Blocks.SCULK_SENSOR) || nearbyState.isOf(Blocks.SCULK_SHRIEKER)) {
				nearbyCount++;
			}

			if (nearbyCount > 2) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean shouldConvertToSpreadable() {
		return false;
	}
}

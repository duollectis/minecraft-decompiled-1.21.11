package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Блок доменной печи. Плавит только руды и металлические предметы, но вдвое быстрее
 * обычной печи. Визуально отображает дым и звук потрескивания огня при активном горении.
 */
public class BlastFurnaceBlock extends AbstractFurnaceBlock {

	public static final MapCodec<BlastFurnaceBlock> CODEC = createCodec(BlastFurnaceBlock::new);
	private static final double SMOKE_OFFSET = 0.52;
	private static final double SMOKE_SOUND_CHANCE = 0.1;

	@Override
	public MapCodec<BlastFurnaceBlock> getCodec() {
		return CODEC;
	}

	public BlastFurnaceBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlastFurnaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return validateTicker(world, type, BlockEntityType.BLAST_FURNACE);
	}

	@Override
	protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
		if (world.getBlockEntity(pos) instanceof BlastFurnaceBlockEntity furnace) {
			player.openHandledScreen(furnace);
			player.incrementStat(Stats.INTERACT_WITH_BLAST_FURNACE);
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(LIT) == false) {
			return;
		}

		double centerX = pos.getX() + 0.5;
		double baseY = pos.getY();
		double centerZ = pos.getZ() + 0.5;

		if (random.nextDouble() < SMOKE_SOUND_CHANCE) {
			world.playSoundClient(
					centerX,
					baseY,
					centerZ,
					SoundEvents.BLOCK_BLASTFURNACE_FIRE_CRACKLE,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F,
					false
			);
		}

		Direction facing = state.get(FACING);
		Direction.Axis axis = facing.getAxis();
		double spread = random.nextDouble() * 0.6 - 0.3;
		double offsetX = axis == Direction.Axis.X ? facing.getOffsetX() * SMOKE_OFFSET : spread;
		double offsetY = random.nextDouble() * 9.0 / 16.0;
		double offsetZ = axis == Direction.Axis.Z ? facing.getOffsetZ() * SMOKE_OFFSET : spread;

		world.addParticleClient(ParticleTypes.SMOKE, centerX + offsetX, baseY + offsetY, centerZ + offsetZ, 0.0, 0.0, 0.0);
	}
}

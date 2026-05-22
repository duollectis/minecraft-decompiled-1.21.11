package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок с зарытым лутом, который можно раскопать кисточкой.
 * При падении ведёт себя как {@link FallingBlock}: разрушается при приземлении,
 * превращаясь в {@link #baseBlock}. Степень раскопки хранится в свойстве {@link #DUSTED}.
 */
public class BrushableBlock extends BlockWithEntity implements Falling {

	public static final MapCodec<BrushableBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Registries.BLOCK.getCodec().fieldOf("turns_into").forGetter(BrushableBlock::getBaseBlock),
			Registries.SOUND_EVENT.getCodec().fieldOf("brush_sound").forGetter(BrushableBlock::getBrushingSound),
			Registries.SOUND_EVENT
				.getCodec()
				.fieldOf("brush_completed_sound")
				.forGetter(BrushableBlock::getBrushingCompleteSound),
			createSettingsCodec()
		).apply(instance, BrushableBlock::new)
	);
	private static final IntProperty DUSTED = Properties.DUSTED;
	private static final int PARTICLE_SPAWN_CHANCE = 16;
	public static final int TICK_DELAY = 2;
	private final Block baseBlock;
	private final SoundEvent brushingSound;
	private final SoundEvent brushingCompleteSound;

	@Override
	public MapCodec<BrushableBlock> getCodec() {
		return CODEC;
	}

	public BrushableBlock(
			Block baseBlock,
			SoundEvent brushingSound,
			SoundEvent brushingCompleteSound,
			AbstractBlock.Settings settings
	) {
		super(settings);
		this.baseBlock = baseBlock;
		this.brushingSound = brushingSound;
		this.brushingCompleteSound = brushingCompleteSound;
		this.setDefaultState(this.stateManager.getDefaultState().with(DUSTED, 0));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(DUSTED);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		world.scheduleBlockTick(pos, this, TICK_DELAY);
	}

	@Override
	public BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		tickView.scheduleBlockTick(pos, this, TICK_DELAY);
		return super.getStateForNeighborUpdate(
				state,
				world,
				tickView,
				pos,
				direction,
				neighborPos,
				neighborState,
				random
		);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof BrushableBlockEntity brushableBlockEntity) {
			brushableBlockEntity.scheduledTick(world);
		}

		if (FallingBlock.canFallThrough(world.getBlockState(pos.down())) && pos.getY() >= world.getBottomY()) {
			FallingBlockEntity fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, pos, state);
			fallingBlockEntity.setDestroyedOnLanding();
		}
	}

	@Override
	public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
		Vec3d center = fallingBlockEntity.getBoundingBox().getCenter();
		world.syncWorldEvent(
			2001,
			BlockPos.ofFloored(center),
			Block.getRawIdFromState(fallingBlockEntity.getBlockState())
		);
		world.emitGameEvent(fallingBlockEntity, GameEvent.BLOCK_DESTROY, center);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(PARTICLE_SPAWN_CHANCE) == 0
			&& FallingBlock.canFallThrough(world.getBlockState(pos.down()))
		) {
			double dustX = pos.getX() + random.nextDouble();
			double dustY = pos.getY() - 0.05;
			double dustZ = pos.getZ() + random.nextDouble();
			world.addParticleClient(
				new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state),
				dustX,
				dustY,
				dustZ,
				0.0,
				0.0,
				0.0
			);
		}
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BrushableBlockEntity(pos, state);
	}

	public Block getBaseBlock() {
		return baseBlock;
	}

	public SoundEvent getBrushingSound() {
		return brushingSound;
	}

	public SoundEvent getBrushingCompleteSound() {
		return brushingCompleteSound;
	}
}

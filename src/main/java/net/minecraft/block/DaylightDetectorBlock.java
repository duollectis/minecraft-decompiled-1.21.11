package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Датчик дневного света — блок, выдающий сигнал редстоуна пропорционально уровню
 * освещения неба. В инвертированном режиме работает как датчик ночи.
 * Переключение режима — правым кликом игрока.
 */
public class DaylightDetectorBlock extends BlockWithEntity {

	public static final MapCodec<DaylightDetectorBlock> CODEC = createCodec(DaylightDetectorBlock::new);
	public static final IntProperty POWER = Properties.POWER;
	public static final BooleanProperty INVERTED = Properties.INVERTED;
	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 6.0);

	@Override
	public MapCodec<DaylightDetectorBlock> getCodec() {
		return CODEC;
	}

	public DaylightDetectorBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(POWER, 0).with(INVERTED, false));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWER);
	}

	/**
	 * Пересчитывает мощность сигнала на основе текущего уровня освещения неба.
	 * В обычном режиме учитывает угол солнца для плавного изменения сигнала на рассвете/закате.
	 */
	private static void updateState(BlockState state, World world, BlockPos pos) {
		int skyLight = world.getLightLevel(LightType.SKY, pos) - world.getAmbientDarkness();
		float sunAngle = world.getEnvironmentAttributes()
			.getAttributeValue(EnvironmentAttributes.SUN_ANGLE_VISUAL, pos)
			* (float) (Math.PI / 180.0);
		boolean inverted = state.get(INVERTED);

		if (inverted) {
			skyLight = 15 - skyLight;
		} else if (skyLight > 0) {
			float target = sunAngle < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
			sunAngle += (target - sunAngle) * 0.2F;
			skyLight = Math.round(skyLight * MathHelper.cos(sunAngle));
		}

		int power = MathHelper.clamp(skyLight, 0, 15);

		if (state.get(POWER) != power) {
			world.setBlockState(pos, state.with(POWER, power), Block.NOTIFY_ALL);
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.canModifyBlocks() == false) {
			return super.onUse(state, world, pos, player, hit);
		}

		if (world.isClient() == false) {
			BlockState toggled = state.cycle(INVERTED);
			world.setBlockState(pos, toggled, Block.NOTIFY_LISTENERS);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, toggled));
			updateState(toggled, world, pos);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DaylightDetectorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return !world.isClient() && world.getDimension().hasSkyLight()
		       ? validateTicker(type, BlockEntityType.DAYLIGHT_DETECTOR, DaylightDetectorBlock::tick)
		       : null;
	}

	private static void tick(World world, BlockPos pos, BlockState state, DaylightDetectorBlockEntity blockEntity) {
		if (world.getTime() % 20L == 0L) {
			updateState(state, world, pos);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(POWER, INVERTED);
	}
}

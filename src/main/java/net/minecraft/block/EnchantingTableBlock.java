package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code EnchantingTableBlock}.
 */
public class EnchantingTableBlock extends BlockWithEntity {

	public static final MapCodec<EnchantingTableBlock> CODEC = createCodec(EnchantingTableBlock::new);
	public static final List<BlockPos> POWER_PROVIDER_OFFSETS = BlockPos.stream(-2, 0, -2, 2, 1, 2)
	                                                                    .filter(pos -> Math.abs(pos.getX()) == 2
			                                                                    || Math.abs(pos.getZ()) == 2)
	                                                                    .map(BlockPos::toImmutable)
	                                                                    .toList();
	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 12.0);

	@Override
	public MapCodec<EnchantingTableBlock> getCodec() {
		return CODEC;
	}

	public EnchantingTableBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	/**
	 * Проверяет возможность access power provider.
	 *
	 * @param world world
	 * @param tablePos table pos
	 * @param providerOffset provider offset
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canAccessPowerProvider(World world, BlockPos tablePos, BlockPos providerOffset) {
		return world.getBlockState(tablePos.add(providerOffset)).isIn(BlockTags.ENCHANTMENT_POWER_PROVIDER)
				&& world
				.getBlockState(tablePos.add(
						providerOffset.getX() / 2,
						providerOffset.getY(),
						providerOffset.getZ() / 2
				))
				.isIn(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);

		for (BlockPos blockPos : POWER_PROVIDER_OFFSETS) {
			if (random.nextInt(16) == 0 && canAccessPowerProvider(world, pos, blockPos)) {
				world.addParticleClient(
						ParticleTypes.ENCHANT,
						pos.getX() + 0.5,
						pos.getY() + 2.0,
						pos.getZ() + 0.5,
						blockPos.getX() + random.nextFloat() - 0.5,
						blockPos.getY() - random.nextFloat() - 1.0F,
						blockPos.getZ() + random.nextFloat() - 0.5
				);
			}
		}
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new EnchantingTableBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return world.isClient() ? validateTicker(
				type,
				BlockEntityType.ENCHANTING_TABLE,
				EnchantingTableBlockEntity::tick
		) : null;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient()) {
			player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(
			BlockState state,
			World world,
			BlockPos pos
	) {
		if (world.getBlockEntity(pos) instanceof EnchantingTableBlockEntity enchantingTableBlockEntity) {
			Text text = enchantingTableBlockEntity.getDisplayName();
			return new SimpleNamedScreenHandlerFactory(
					(syncId, inventory, player) -> new EnchantmentScreenHandler(
							syncId,
							inventory,
							ScreenHandlerContext.create(world, pos)
					), text
			);
		}
		else {
			return null;
		}
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}

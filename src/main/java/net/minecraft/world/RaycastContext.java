package net.minecraft.world;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.rule.GameRules;

import java.util.function.Predicate;

/**
 * Контекст для выполнения рейкаста (трассировки луча) в мире.
 * Определяет начальную и конечную точки луча, тип проверяемых форм блоков
 * и режим обработки жидкостей.
 */
public class RaycastContext {

	private final Vec3d start;
	private final Vec3d end;
	private final ShapeType shapeType;
	private final FluidHandling fluid;
	private final ShapeContext shapeContext;

	public RaycastContext(
		Vec3d start,
		Vec3d end,
		ShapeType shapeType,
		FluidHandling fluidHandling,
		ShapeContext shapeContext
	) {
		this.start = start;
		this.end = end;
		this.shapeType = shapeType;
		this.fluid = fluidHandling;
		this.shapeContext = shapeContext;
	}

	public RaycastContext(
		Vec3d start,
		Vec3d end,
		ShapeType shapeType,
		FluidHandling fluidHandling,
		Entity entity
	) {
		this(start, end, shapeType, fluidHandling, ShapeContext.of(entity));
	}

	public Vec3d getStart() {
		return start;
	}

	public Vec3d getEnd() {
		return end;
	}

	public VoxelShape getBlockShape(BlockState state, BlockView world, BlockPos pos) {
		return shapeType.get(state, world, pos, shapeContext);
	}

	public VoxelShape getFluidShape(FluidState state, BlockView world, BlockPos pos) {
		return fluid.handled(state) ? state.getShape(world, pos) : VoxelShapes.empty();
	}

	/**
	 * Определяет, какие жидкости считаются препятствием при рейкасте.
	 */
	public enum FluidHandling {
		NONE(state -> false),
		SOURCE_ONLY(FluidState::isStill),
		ANY(state -> !state.isEmpty()),
		WATER(state -> state.isIn(FluidTags.WATER));

		private final Predicate<FluidState> predicate;

		FluidHandling(final Predicate<FluidState> predicate) {
			this.predicate = predicate;
		}

		public boolean handled(FluidState state) {
			return predicate.test(state);
		}
	}

	/**
	 * Поставщик формы столкновения для блока в заданном контексте.
	 */
	public interface ShapeProvider {

		VoxelShape get(BlockState state, BlockView world, BlockPos pos, ShapeContext context);
	}

	/**
	 * Тип формы блока, используемой при рейкасте.
	 * {@link #FALLDAMAGE_RESETTING} — специальный тип для определения сброса урона от падения:
	 * возвращает полный куб для блоков, сбрасывающих урон, порталов и нетер-порталов
	 * с нулевой задержкой (только для игроков).
	 */
	public enum ShapeType implements ShapeProvider {
		COLLIDER(AbstractBlock.AbstractBlockState::getCollisionShape),
		OUTLINE(AbstractBlock.AbstractBlockState::getOutlineShape),
		VISUAL(AbstractBlock.AbstractBlockState::getCameraCollisionShape),
		FALLDAMAGE_RESETTING(
			(state, world, pos, context) -> {
				if (state.isIn(BlockTags.FALL_DAMAGE_RESETTING)) {
					return VoxelShapes.fullCube();
				}

				if (context instanceof EntityShapeContext entityShapeContext
					&& entityShapeContext.getEntity() != null
					&& entityShapeContext.getEntity().getType() == EntityType.PLAYER
				) {
					if (state.isOf(Blocks.END_GATEWAY) || state.isOf(Blocks.END_PORTAL)) {
						return VoxelShapes.fullCube();
					}

					if (world instanceof ServerWorld serverWorld
						&& state.isOf(Blocks.NETHER_PORTAL)
						&& serverWorld.getGameRules().getValue(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY) == 0
					) {
						return VoxelShapes.fullCube();
					}
				}

				return VoxelShapes.empty();
			}
		);

		private final ShapeProvider provider;

		ShapeType(final ShapeProvider provider) {
			this.provider = provider;
		}

		@Override
		public VoxelShape get(BlockState blockState, BlockView blockView, BlockPos blockPos, ShapeContext shapeContext) {
			return provider.get(blockState, blockView, blockPos, shapeContext);
		}
	}
}

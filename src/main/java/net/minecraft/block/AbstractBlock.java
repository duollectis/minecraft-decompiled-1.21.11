package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeyedValue;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.State;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Базовый класс для всех блоков игры.
 * <p>Содержит общую логику поведения блоков: физические свойства (твёрдость, сопротивление),
 * звуки, лут-таблицы, взаимодействие с игроком и окружением.
 * Конкретные блоки наследуют {@link Block}, который расширяет этот класс.</p>
 * <p>Настройки блока задаются через {@link AbstractBlock.Settings} и передаются в конструктор.</p>
 */
public abstract class AbstractBlock implements ToggleableFeature {

	protected static final Direction[] DIRECTIONS = new Direction[]{
			Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP
	};
	protected final boolean collidable;
	protected final float resistance;
	protected final boolean randomTicks;
	protected final BlockSoundGroup soundGroup;
	protected final float slipperiness;
	protected final float velocityMultiplier;
	protected final float jumpVelocityMultiplier;
	protected final boolean dynamicBounds;
	protected final FeatureSet requiredFeatures;
	protected final AbstractBlock.Settings settings;
	protected final Optional<RegistryKey<LootTable>> lootTableKey;
	protected final String translationKey;

	public AbstractBlock(AbstractBlock.Settings settings) {
		this.collidable = settings.collidable;
		this.lootTableKey = settings.getLootTableKey();
		this.translationKey = settings.getTranslationKey();
		this.resistance = settings.resistance;
		this.randomTicks = settings.randomTicks;
		this.soundGroup = settings.soundGroup;
		this.slipperiness = settings.slipperiness;
		this.velocityMultiplier = settings.velocityMultiplier;
		this.jumpVelocityMultiplier = settings.jumpVelocityMultiplier;
		this.dynamicBounds = settings.dynamicBounds;
		this.requiredFeatures = settings.requiredFeatures;
		this.settings = settings;
	}

	public AbstractBlock.Settings getSettings() {
		return this.settings;
	}

	protected abstract MapCodec<? extends Block> getCodec();

	protected static <B extends Block> RecordCodecBuilder<B, AbstractBlock.Settings> createSettingsCodec() {
		return AbstractBlock.Settings.CODEC.fieldOf("properties").forGetter(AbstractBlock::getSettings);
	}

	public static <B extends Block> MapCodec<B> createCodec(Function<AbstractBlock.Settings, B> blockFromSettings) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(createSettingsCodec())
				.apply(instance, blockFromSettings));
	}

	protected void prepare(
			BlockState state,
			WorldAccess world,
			BlockPos pos,
			@Block.SetBlockStateFlag int flags,
			int maxUpdateDepth
	) {
	}

	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		switch (type) {
			case LAND:
				return !state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
			case WATER:
				return state.getFluidState().isIn(FluidTags.WATER);
			case AIR:
				return !state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
			default:
				return false;
		}
	}

	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		return state;
	}

	protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return false;
	}

	protected void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable WireOrientation wireOrientation,
			boolean notify
	) {
	}

	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
	}

	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
	}

	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (!state.isAir() && explosion.getDestructionType() != Explosion.DestructionType.TRIGGER_BLOCK) {
			Block block = state.getBlock();
			boolean bl = explosion.getCausingEntity() instanceof PlayerEntity;
			if (block.shouldDropItemsOnExplosion(explosion)) {
				BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
				LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
						.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
						.add(LootContextParameters.TOOL, ItemStack.EMPTY)
						.addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity)
						.addOptional(LootContextParameters.THIS_ENTITY, explosion.getEntity());
				if (explosion.getDestructionType() == Explosion.DestructionType.DESTROY_WITH_DECAY) {
					builder.add(LootContextParameters.EXPLOSION_RADIUS, explosion.getPower());
				}

				state.onStacksDropped(world, pos, ItemStack.EMPTY, bl);
				state.getDroppedStacks(builder).forEach(stack -> stackMerger.accept(stack, pos));
			}

			world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
			block.onDestroyedByExplosion(world, pos, explosion);
		}
	}

	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return ActionResult.PASS;
	}

	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}

	protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		return false;
	}

	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	protected boolean hasSidedTransparency(BlockState state) {
		return false;
	}

	protected boolean emitsRedstonePower(BlockState state) {
		return false;
	}

	protected FluidState getFluidState(BlockState state) {
		return Fluids.EMPTY.getDefaultState();
	}

	protected boolean hasComparatorOutput(BlockState state) {
		return false;
	}

	protected float getMaxHorizontalModelOffset() {
		return 0.25F;
	}

	protected float getVerticalModelOffsetMultiplier() {
		return 0.2F;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return this.requiredFeatures;
	}

	protected boolean keepBlockEntityWhenReplacedWith(BlockState state) {
		return false;
	}

	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state;
	}

	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state;
	}

	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		return state.isReplaceable() && (context.getStack().isEmpty() || !context.getStack().isOf(this.asItem()));
	}

	protected boolean canBucketPlace(BlockState state, Fluid fluid) {
		return state.isReplaceable() || !state.isSolid();
	}

	protected List<ItemStack> getDroppedStacks(BlockState state, LootWorldContext.Builder builder) {
		if (this.lootTableKey.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			LootWorldContext
					lootWorldContext =
					builder.add(LootContextParameters.BLOCK_STATE, state).build(LootContextTypes.BLOCK);
			ServerWorld serverWorld = lootWorldContext.getWorld();
			LootTable
					resolvedLootTable =
					serverWorld.getServer().getReloadableRegistries().getLootTable(this.lootTableKey.get());
			return resolvedLootTable.generateLoot(lootWorldContext);
		}
	}

	protected long getRenderingSeed(BlockState state, BlockPos pos) {
		return MathHelper.hashCode(pos);
	}

	protected VoxelShape getCullingShape(BlockState state) {
		return state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
	}

	protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
		return this.getCollisionShape(state, world, pos, ShapeContext.absent());
	}

	protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return VoxelShapes.empty();
	}

	protected int getOpacity(BlockState state) {
		if (state.isOpaqueFullCube()) {
			return 15;
		}
		else {
			return state.isTransparent() ? 0 : 1;
		}
	}

	protected @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(
			BlockState state,
			World world,
			BlockPos pos
	) {
		return null;
	}

	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return true;
	}

	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return state.isFullCube(world, pos) ? 0.2F : 1.0F;
	}

	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return 0;
	}

	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.fullCube();
	}

	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.collidable ? state.getOutlineShape(world, pos) : VoxelShapes.empty();
	}

	protected VoxelShape getInsideCollisionShape(BlockState state, BlockView world, BlockPos pos, Entity entity) {
		return VoxelShapes.fullCube();
	}

	protected boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
		return Block.isShapeFullCube(state.getCollisionShape(world, pos));
	}

	protected VoxelShape getCameraCollisionShape(
			BlockState state,
			BlockView world,
			BlockPos pos,
			ShapeContext context
	) {
		return this.getCollisionShape(state, world, pos, context);
	}

	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
	}

	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
	}

	protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
		float f = state.getHardness(world, pos);
		if (f == -1.0F) {
			return 0.0F;
		}
		else {
			int i = player.canHarvest(state) ? 30 : 100;
			return player.getBlockBreakingSpeed(state) / f / i;
		}
	}

	protected void onStacksDropped(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			ItemStack tool,
			boolean dropExperience
	) {
	}

	protected void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
	}

	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return 0;
	}

	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
	}

	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return 0;
	}

	public final Optional<RegistryKey<LootTable>> getLootTableKey() {
		return this.lootTableKey;
	}

	public final String getTranslationKey() {
		return this.translationKey;
	}

	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
	}

	protected boolean isTransparent(BlockState state) {
		return !Block.isShapeFullCube(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) && state
				.getFluidState()
				.isEmpty();
	}

	protected boolean hasRandomTicks(BlockState state) {
		return this.randomTicks;
	}

	protected BlockSoundGroup getSoundGroup(BlockState state) {
		return this.soundGroup;
	}

	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.asItem());
	}

	public abstract Item asItem();

	protected abstract Block asBlock();

	public MapColor getDefaultMapColor() {
		return this.settings.mapColorProvider.apply(this.asBlock().getDefaultState());
	}

	public float getHardness() {
		return this.settings.hardness;
	}

	/**
	 * Базовое состояние блока, хранящее кешированные физические и визуальные свойства.
	 * <p>Каждый уникальный набор значений свойств блока порождает отдельный экземпляр
	 * {@link BlockState}. Кеш форм ({@code ShapeCache}) инициализируется лениво
	 * при первом обращении к геометрии.</p>
	 */
	public abstract static class AbstractBlockState extends State<Block, BlockState> {

		private static final Direction[] DIRECTIONS = Direction.values();
		private static final VoxelShape[] EMPTY_CULLING_FACES = Util.make(
				new VoxelShape[DIRECTIONS.length], direction -> Arrays.fill(direction, VoxelShapes.empty())
		);
		private static final VoxelShape[] FULL_CULLING_FACES = Util.make(
				new VoxelShape[DIRECTIONS.length], direction -> Arrays.fill(direction, VoxelShapes.fullCube())
		);
		private final int luminance;
		private final boolean hasSidedTransparency;
		private final boolean isAir;
		private final boolean burnable;
		@Deprecated
		private final boolean liquid;
		@Deprecated
		private boolean solid;
		private final PistonBehavior pistonBehavior;
		private final MapColor mapColor;
		private final float hardness;
		private final boolean toolRequired;
		private final boolean opaque;
		private final AbstractBlock.ContextPredicate solidBlockPredicate;
		private final AbstractBlock.ContextPredicate suffocationPredicate;
		private final AbstractBlock.ContextPredicate blockVisionPredicate;
		private final AbstractBlock.ContextPredicate postProcessPredicate;
		private final AbstractBlock.ContextPredicate emissiveLightingPredicate;
		private final AbstractBlock.@Nullable Offsetter offsetter;
		private final boolean blockBreakParticles;
		private final NoteBlockInstrument instrument;
		private final boolean replaceable;
		private AbstractBlock.AbstractBlockState.@Nullable ShapeCache shapeCache;
		private FluidState fluidState = Fluids.EMPTY.getDefaultState();
		private boolean ticksRandomly;
		private boolean opaqueFullCube;
		private VoxelShape cullingShape;
		private VoxelShape[] cullingFaces;
		private boolean transparent;
		private int opacity;

		protected AbstractBlockState(
				Block block,
				Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
				MapCodec<BlockState> codec
		) {
			super(block, propertyMap, codec);
			AbstractBlock.Settings settings = block.settings;
			this.luminance = settings.luminance.applyAsInt(this.asBlockState());
			this.hasSidedTransparency = block.hasSidedTransparency(this.asBlockState());
			this.isAir = settings.isAir;
			this.burnable = settings.burnable;
			this.liquid = settings.liquid;
			this.pistonBehavior = settings.pistonBehavior;
			this.mapColor = settings.mapColorProvider.apply(this.asBlockState());
			this.hardness = settings.hardness;
			this.toolRequired = settings.toolRequired;
			this.opaque = settings.opaque;
			this.solidBlockPredicate = settings.solidBlockPredicate;
			this.suffocationPredicate = settings.suffocationPredicate;
			this.blockVisionPredicate = settings.blockVisionPredicate;
			this.postProcessPredicate = settings.postProcessPredicate;
			this.emissiveLightingPredicate = settings.emissiveLightingPredicate;
			this.offsetter = settings.offsetter;
			this.blockBreakParticles = settings.blockBreakParticles;
			this.instrument = settings.instrument;
			this.replaceable = settings.replaceable;
		}

		private boolean shouldBeSolid() {
			if (this.owner.settings.forceSolid) {
				return true;
			}
			else if (this.owner.settings.forceNotSolid) {
				return false;
			}
			else if (this.shapeCache == null) {
				return false;
			}
			else {
				VoxelShape voxelShape = this.shapeCache.collisionShape;
				if (voxelShape.isEmpty()) {
					return false;
				}
				else {
					Box box = voxelShape.getBoundingBox();
					return box.getAverageSideLength() >= 0.7291666666666666 ? true : box.getLengthY() >= 1.0;
				}
			}
		}

		public void initShapeCache() {
			this.fluidState = this.owner.getFluidState(this.asBlockState());
			this.ticksRandomly = this.owner.hasRandomTicks(this.asBlockState());
			if (!this.getBlock().hasDynamicBounds()) {
				this.shapeCache = new AbstractBlock.AbstractBlockState.ShapeCache(this.asBlockState());
			}

			this.solid = this.shouldBeSolid();
			this.cullingShape = this.opaque ? this.owner.getCullingShape(this.asBlockState()) : VoxelShapes.empty();
			this.opaqueFullCube = Block.isShapeFullCube(this.cullingShape);
			if (this.cullingShape.isEmpty()) {
				this.cullingFaces = EMPTY_CULLING_FACES;
			}
			else if (this.opaqueFullCube) {
				this.cullingFaces = FULL_CULLING_FACES;
			}
			else {
				this.cullingFaces = new VoxelShape[DIRECTIONS.length];

				for (Direction direction : DIRECTIONS) {
					this.cullingFaces[direction.ordinal()] = this.cullingShape.getFace(direction);
				}
			}

			this.transparent = this.owner.isTransparent(this.asBlockState());
			this.opacity = this.owner.getOpacity(this.asBlockState());
		}

		public Block getBlock() {
			return this.owner;
		}

		public RegistryEntry<Block> getRegistryEntry() {
			return this.owner.getRegistryEntry();
		}

		@Deprecated
		public boolean blocksMovement() {
			Block block = this.getBlock();
			return block != Blocks.COBWEB && block != Blocks.BAMBOO_SAPLING && this.isSolid();
		}

		@Deprecated
		public boolean isSolid() {
			return this.solid;
		}

		public boolean allowsSpawning(BlockView world, BlockPos pos, EntityType<?> type) {
			return this.getBlock().settings.allowsSpawningPredicate.test(this.asBlockState(), world, pos, type);
		}

		public boolean isTransparent() {
			return this.transparent;
		}

		public int getOpacity() {
			return this.opacity;
		}

		public VoxelShape getCullingFace(Direction direction) {
			return this.cullingFaces[direction.ordinal()];
		}

		public VoxelShape getCullingShape() {
			return this.cullingShape;
		}

		public boolean exceedsCube() {
			return this.shapeCache == null || this.shapeCache.exceedsCube;
		}

		public boolean hasSidedTransparency() {
			return this.hasSidedTransparency;
		}

		public int getLuminance() {
			return this.luminance;
		}

		public boolean isAir() {
			return this.isAir;
		}

		public boolean isBurnable() {
			return this.burnable;
		}

		@Deprecated
		public boolean isLiquid() {
			return this.liquid;
		}

		public MapColor getMapColor(BlockView world, BlockPos pos) {
			return this.mapColor;
		}

		public BlockState rotate(BlockRotation rotation) {
			return this.getBlock().rotate(this.asBlockState(), rotation);
		}

		public BlockState mirror(BlockMirror mirror) {
			return this.getBlock().mirror(this.asBlockState(), mirror);
		}

		public BlockRenderType getRenderType() {
			return this.getBlock().getRenderType(this.asBlockState());
		}

		public boolean hasEmissiveLighting(BlockView world, BlockPos pos) {
			return this.emissiveLightingPredicate.test(this.asBlockState(), world, pos);
		}

		public float getAmbientOcclusionLightLevel(BlockView world, BlockPos pos) {
			return this.getBlock().getAmbientOcclusionLightLevel(this.asBlockState(), world, pos);
		}

		public boolean isSolidBlock(BlockView world, BlockPos pos) {
			return this.solidBlockPredicate.test(this.asBlockState(), world, pos);
		}

		public boolean emitsRedstonePower() {
			return this.getBlock().emitsRedstonePower(this.asBlockState());
		}

		public int getWeakRedstonePower(BlockView world, BlockPos pos, Direction direction) {
			return this.getBlock().getWeakRedstonePower(this.asBlockState(), world, pos, direction);
		}

		public boolean hasComparatorOutput() {
			return this.getBlock().hasComparatorOutput(this.asBlockState());
		}

		public int getComparatorOutput(World world, BlockPos pos, Direction direction) {
			return this.getBlock().getComparatorOutput(this.asBlockState(), world, pos, direction);
		}

		public float getHardness(BlockView world, BlockPos pos) {
			return this.hardness;
		}

		public float calcBlockBreakingDelta(PlayerEntity player, BlockView world, BlockPos pos) {
			return this.getBlock().calcBlockBreakingDelta(this.asBlockState(), player, world, pos);
		}

		public int getStrongRedstonePower(BlockView world, BlockPos pos, Direction direction) {
			return this.getBlock().getStrongRedstonePower(this.asBlockState(), world, pos, direction);
		}

		public PistonBehavior getPistonBehavior() {
			return this.pistonBehavior;
		}

		public boolean isOpaqueFullCube() {
			return this.opaqueFullCube;
		}

		public boolean isOpaque() {
			return this.opaque;
		}

		public boolean isSideInvisible(BlockState state, Direction direction) {
			return this.getBlock().isSideInvisible(this.asBlockState(), state, direction);
		}

		public VoxelShape getOutlineShape(BlockView world, BlockPos pos) {
			return this.getOutlineShape(world, pos, ShapeContext.absent());
		}

		public VoxelShape getOutlineShape(BlockView world, BlockPos pos, ShapeContext context) {
			return this.getBlock().getOutlineShape(this.asBlockState(), world, pos, context);
		}

		public VoxelShape getCollisionShape(BlockView world, BlockPos pos) {
			return this.shapeCache != null ? this.shapeCache.collisionShape
			                               : this.getCollisionShape(world, pos, ShapeContext.absent());
		}

		public VoxelShape getCollisionShape(BlockView world, BlockPos pos, ShapeContext context) {
			return this.getBlock().getCollisionShape(this.asBlockState(), world, pos, context);
		}

		public VoxelShape getInsideCollisionShape(BlockView blockView, BlockPos pos, Entity entity) {
			return this.getBlock().getInsideCollisionShape(this.asBlockState(), blockView, pos, entity);
		}

		public VoxelShape getSidesShape(BlockView world, BlockPos pos) {
			return this.getBlock().getSidesShape(this.asBlockState(), world, pos);
		}

		public VoxelShape getCameraCollisionShape(BlockView world, BlockPos pos, ShapeContext context) {
			return this.getBlock().getCameraCollisionShape(this.asBlockState(), world, pos, context);
		}

		public VoxelShape getRaycastShape(BlockView world, BlockPos pos) {
			return this.getBlock().getRaycastShape(this.asBlockState(), world, pos);
		}

		public final boolean hasSolidTopSurface(BlockView world, BlockPos pos, Entity entity) {
			return this.isSolidSurface(world, pos, entity, Direction.UP);
		}

		public final boolean isSolidSurface(BlockView world, BlockPos pos, Entity entity, Direction direction) {
			return Block.isFaceFullSquare(this.getCollisionShape(world, pos, ShapeContext.of(entity)), direction);
		}

		public Vec3d getModelOffset(BlockPos pos) {
			AbstractBlock.Offsetter blockOffsetter = this.offsetter;
			return blockOffsetter != null ? blockOffsetter.evaluate(this.asBlockState(), pos) : Vec3d.ZERO;
		}

		public boolean hasModelOffset() {
			return this.offsetter != null;
		}

		public boolean onSyncedBlockEvent(World world, BlockPos pos, int type, int data) {
			return this.getBlock().onSyncedBlockEvent(this.asBlockState(), world, pos, type, data);
		}

		public void neighborUpdate(
				World world,
				BlockPos pos,
				Block sourceBlock,
				@Nullable WireOrientation wireOrientation,
				boolean notify
		) {
			this.getBlock().neighborUpdate(this.asBlockState(), world, pos, sourceBlock, wireOrientation, notify);
		}

		public final void updateNeighbors(WorldAccess world, BlockPos pos, @Block.SetBlockStateFlag int flags) {
			this.updateNeighbors(world, pos, flags, 512);
		}

		public final void updateNeighbors(
				WorldAccess world,
				BlockPos pos,
				@Block.SetBlockStateFlag int flags,
				int maxUpdateDepth
		) {
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (Direction direction : AbstractBlock.DIRECTIONS) {
				mutable.set(pos, direction);
				world.replaceWithStateForNeighborUpdate(
						direction.getOpposite(),
						mutable,
						pos,
						this.asBlockState(),
						flags,
						maxUpdateDepth
				);
			}
		}

		public final void prepare(WorldAccess world, BlockPos pos, @Block.SetBlockStateFlag int flags) {
			this.prepare(world, pos, flags, 512);
		}

		public void prepare(WorldAccess world, BlockPos pos, @Block.SetBlockStateFlag int flags, int maxUpdateDepth) {
			this.getBlock().prepare(this.asBlockState(), world, pos, flags, maxUpdateDepth);
		}

		public void onBlockAdded(World world, BlockPos pos, BlockState state, boolean notify) {
			this.getBlock().onBlockAdded(this.asBlockState(), world, pos, state, notify);
		}

		public void onStateReplaced(ServerWorld world, BlockPos pos, boolean moved) {
			this.getBlock().onStateReplaced(this.asBlockState(), world, pos, moved);
		}

		public void onExploded(
				ServerWorld world,
				BlockPos pos,
				Explosion explosion,
				BiConsumer<ItemStack, BlockPos> stackMerger
		) {
			this.getBlock().onExploded(this.asBlockState(), world, pos, explosion, stackMerger);
		}

		public void scheduledTick(ServerWorld world, BlockPos pos, Random random) {
			this.getBlock().scheduledTick(this.asBlockState(), world, pos, random);
		}

		public void randomTick(ServerWorld world, BlockPos pos, Random random) {
			this.getBlock().randomTick(this.asBlockState(), world, pos, random);
		}

		public void onEntityCollision(
				World world,
				BlockPos pos,
				Entity entity,
				EntityCollisionHandler entityCollisionHandler,
				boolean bl
		) {
			this.getBlock().onEntityCollision(this.asBlockState(), world, pos, entity, entityCollisionHandler, bl);
		}

		public void onStacksDropped(ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
			this.getBlock().onStacksDropped(this.asBlockState(), world, pos, tool, dropExperience);
		}

		public List<ItemStack> getDroppedStacks(LootWorldContext.Builder builder) {
			return this.getBlock().getDroppedStacks(this.asBlockState(), builder);
		}

		public ActionResult onUseWithItem(
				ItemStack stack,
				World world,
				PlayerEntity player,
				Hand hand,
				BlockHitResult hit
		) {
			return this
					.getBlock()
					.onUseWithItem(stack, this.asBlockState(), world, hit.getBlockPos(), player, hand, hit);
		}

		public ActionResult onUse(World world, PlayerEntity player, BlockHitResult hit) {
			return this.getBlock().onUse(this.asBlockState(), world, hit.getBlockPos(), player, hit);
		}

		public void onBlockBreakStart(World world, BlockPos pos, PlayerEntity player) {
			this.getBlock().onBlockBreakStart(this.asBlockState(), world, pos, player);
		}

		public boolean shouldSuffocate(BlockView world, BlockPos pos) {
			return this.suffocationPredicate.test(this.asBlockState(), world, pos);
		}

		public boolean shouldBlockVision(BlockView world, BlockPos pos) {
			return this.blockVisionPredicate.test(this.asBlockState(), world, pos);
		}

		public BlockState getStateForNeighborUpdate(
				WorldView world,
				ScheduledTickView tickView,
				BlockPos pos,
				Direction direction,
				BlockPos neighborPos,
				BlockState neighborState,
				Random random
		) {
			return this
					.getBlock()
					.getStateForNeighborUpdate(
							this.asBlockState(),
							world,
							tickView,
							pos,
							direction,
							neighborPos,
							neighborState,
							random
					);
		}

		public boolean canPathfindThrough(NavigationType type) {
			return this.getBlock().canPathfindThrough(this.asBlockState(), type);
		}

		public boolean canReplace(ItemPlacementContext context) {
			return this.getBlock().canReplace(this.asBlockState(), context);
		}

		public boolean canBucketPlace(Fluid fluid) {
			return this.getBlock().canBucketPlace(this.asBlockState(), fluid);
		}

		public boolean isReplaceable() {
			return this.replaceable;
		}

		public boolean canPlaceAt(WorldView world, BlockPos pos) {
			return this.getBlock().canPlaceAt(this.asBlockState(), world, pos);
		}

		public boolean shouldPostProcess(BlockView world, BlockPos pos) {
			return this.postProcessPredicate.test(this.asBlockState(), world, pos);
		}

		public @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(World world, BlockPos pos) {
			return this.getBlock().createScreenHandlerFactory(this.asBlockState(), world, pos);
		}

		public boolean isIn(TagKey<Block> tag) {
			return this.getBlock().getRegistryEntry().isIn(tag);
		}

		public boolean isIn(TagKey<Block> tag, Predicate<AbstractBlock.AbstractBlockState> predicate) {
			return this.isIn(tag) && predicate.test(this);
		}

		public boolean isIn(RegistryEntryList<Block> blocks) {
			return blocks.contains(this.getBlock().getRegistryEntry());
		}

		public boolean isOf(RegistryEntry<Block> blockEntry) {
			return this.isOf(blockEntry.value());
		}

		public Stream<TagKey<Block>> streamTags() {
			return this.getBlock().getRegistryEntry().streamTags();
		}

		public boolean hasBlockEntity() {
			return this.getBlock() instanceof BlockEntityProvider;
		}

		public boolean keepBlockEntityWhenReplacedWith(BlockState state) {
			return this.getBlock().keepBlockEntityWhenReplacedWith(state);
		}

		public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getBlockEntityTicker(
				World world,
				BlockEntityType<T> blockEntityType
		) {
			return this.getBlock() instanceof BlockEntityProvider
			       ? ((BlockEntityProvider) this.getBlock()).getTicker(world, this.asBlockState(), blockEntityType)
			       : null;
		}

		public boolean isOf(Block block) {
			return this.getBlock() == block;
		}

		public boolean matchesKey(RegistryKey<Block> key) {
			return this.getBlock().getRegistryEntry().matchesKey(key);
		}

		public FluidState getFluidState() {
			return this.fluidState;
		}

		public boolean hasRandomTicks() {
			return this.ticksRandomly;
		}

		public long getRenderingSeed(BlockPos pos) {
			return this.getBlock().getRenderingSeed(this.asBlockState(), pos);
		}

		public BlockSoundGroup getSoundGroup() {
			return this.getBlock().getSoundGroup(this.asBlockState());
		}

		public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
			this.getBlock().onProjectileHit(world, state, hit, projectile);
		}

		public boolean isSideSolidFullSquare(BlockView world, BlockPos pos, Direction direction) {
			return this.isSideSolid(world, pos, direction, SideShapeType.FULL);
		}

		public boolean isSideSolid(BlockView world, BlockPos pos, Direction direction, SideShapeType shapeType) {
			return this.shapeCache != null ? this.shapeCache.isSideSolid(direction, shapeType)
			                               : shapeType.matches(this.asBlockState(), world, pos, direction);
		}

		public boolean isFullCube(BlockView world, BlockPos pos) {
			return this.shapeCache != null ? this.shapeCache.isFullCube
			                               : this.getBlock().isShapeFullCube(this.asBlockState(), world, pos);
		}

		public ItemStack getPickStack(WorldView world, BlockPos pos, boolean includeData) {
			return this.getBlock().getPickStack(world, pos, this.asBlockState(), includeData);
		}

		protected abstract BlockState asBlockState();

		public boolean isToolRequired() {
			return this.toolRequired;
		}

		public boolean hasBlockBreakParticles() {
			return this.blockBreakParticles;
		}

		public NoteBlockInstrument getInstrument() {
			return this.instrument;
		}

		/**
		 * {@code ShapeCache}.
		 */
		static final class ShapeCache {

			private static final Direction[] DIRECTIONS = Direction.values();
			private static final int SHAPE_TYPE_LENGTH = SideShapeType.values().length;
			protected final VoxelShape collisionShape;
			protected final boolean exceedsCube;
			private final boolean[] solidSides;
			protected final boolean isFullCube;

			ShapeCache(BlockState state) {
				Block block = state.getBlock();
				this.collisionShape =
						block.getCollisionShape(state, EmptyBlockView.INSTANCE, BlockPos.ORIGIN, ShapeContext.absent());
				if (!this.collisionShape.isEmpty() && state.hasModelOffset()) {
					throw new IllegalStateException(
							String.format(
									Locale.ROOT,
									"%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.",
									Registries.BLOCK.getId(block)
							)
					);
				}
				else {
					this.exceedsCube = Arrays.stream(Direction.Axis.values())
					                         .anyMatch(axis -> this.collisionShape.getMin(axis) < 0.0
							                         || this.collisionShape.getMax(axis) > 1.0);
					this.solidSides = new boolean[DIRECTIONS.length * SHAPE_TYPE_LENGTH];

					for (Direction direction : DIRECTIONS) {
						for (SideShapeType sideShapeType : SideShapeType.values()) {
							this.solidSides[indexSolidSide(direction, sideShapeType)] = sideShapeType.matches(
									state, EmptyBlockView.INSTANCE, BlockPos.ORIGIN, direction
							);
						}
					}

					this.isFullCube =
							Block.isShapeFullCube(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
				}
			}

			public boolean isSideSolid(Direction direction, SideShapeType shapeType) {
				return this.solidSides[indexSolidSide(direction, shapeType)];
			}

			private static int indexSolidSide(Direction direction, SideShapeType shapeType) {
				return direction.ordinal() * SHAPE_TYPE_LENGTH + shapeType.ordinal();
			}
		}
	}

	/**
	 * Предикат, проверяющий условие для блока в заданном мире и позиции.
	 */
	@FunctionalInterface
	public interface ContextPredicate {

		boolean test(BlockState state, BlockView world, BlockPos pos);
	}

	/**
	 * Тип смещения модели блока относительно центра ячейки.
	 */
	public static enum OffsetType {
		NONE,
		XZ,
		XYZ;
	}

	/**
	 * Функциональный интерфейс для вычисления смещения модели блока в зависимости
	 * от состояния и позиции (используется для случайного смещения растений и т.п.).
	 */
	@FunctionalInterface
	public interface Offsetter {

		Vec3d evaluate(BlockState state, BlockPos pos);
	}

	/**
	 * Строитель настроек блока.
	 * <p>Используется при регистрации блока для задания всех физических,
	 * визуальных и поведенческих параметров: твёрдость, звуки, лут-таблица,
	 * поведение поршня, цвет карты и т.д.</p>
	 * <p>Создаётся через {@link AbstractBlock.Settings#create()} или
	 * {@link AbstractBlock.Settings#copy(AbstractBlock)}.</p>
	 */
	public static class Settings {

		public static final Codec<AbstractBlock.Settings> CODEC = MapCodec.unitCodec(() -> create());
		Function<BlockState, MapColor> mapColorProvider = state -> MapColor.CLEAR;
		boolean collidable = true;
		BlockSoundGroup soundGroup = BlockSoundGroup.STONE;
		ToIntFunction<BlockState> luminance = state -> 0;
		float resistance;
		float hardness;
		boolean toolRequired;
		boolean randomTicks;
		float slipperiness = 0.6F;
		float velocityMultiplier = 1.0F;
		float jumpVelocityMultiplier = 1.0F;
		private @Nullable RegistryKey<Block> registryKey;
		private RegistryKeyedValue<Block, Optional<RegistryKey<LootTable>>> lootTable = registryKey -> Optional.of(
				RegistryKey.of(RegistryKeys.LOOT_TABLE, registryKey.getValue().withPrefixedPath("blocks/"))
		);
		private RegistryKeyedValue<Block, String>
				translationKey =
				registryKey -> Util.createTranslationKey("block", registryKey.getValue());
		boolean opaque = true;
		boolean isAir;
		boolean burnable;
		@Deprecated
		boolean liquid;
		@Deprecated
		boolean forceNotSolid;
		boolean forceSolid;
		PistonBehavior pistonBehavior = PistonBehavior.NORMAL;
		boolean blockBreakParticles = true;
		NoteBlockInstrument instrument = NoteBlockInstrument.HARP;
		boolean replaceable;
		AbstractBlock.TypedContextPredicate<EntityType<?>>
				allowsSpawningPredicate =
				(state, world, pos, type) -> state.isSideSolidFullSquare(
						world, pos, Direction.UP
				)
						&& state.getLuminance() < 14;
		AbstractBlock.ContextPredicate solidBlockPredicate = (state, world, pos) -> state.isFullCube(world, pos);
		AbstractBlock.ContextPredicate
				suffocationPredicate =
				(state, world, pos) -> state.blocksMovement() && state.isFullCube(world, pos);
		AbstractBlock.ContextPredicate blockVisionPredicate = this.suffocationPredicate;
		AbstractBlock.ContextPredicate postProcessPredicate = (state, world, pos) -> false;
		AbstractBlock.ContextPredicate emissiveLightingPredicate = (state, world, pos) -> false;
		boolean dynamicBounds;
		FeatureSet requiredFeatures = FeatureFlags.VANILLA_FEATURES;
		AbstractBlock.@Nullable Offsetter offsetter;

		private Settings() {
		}

		public static AbstractBlock.Settings create() {
			return new AbstractBlock.Settings();
		}

		public static AbstractBlock.Settings copy(AbstractBlock block) {
			AbstractBlock.Settings copied = copyShallow(block);
			AbstractBlock.Settings source = block.settings;
			copied.jumpVelocityMultiplier = source.jumpVelocityMultiplier;
			copied.solidBlockPredicate = source.solidBlockPredicate;
			copied.allowsSpawningPredicate = source.allowsSpawningPredicate;
			copied.postProcessPredicate = source.postProcessPredicate;
			copied.suffocationPredicate = source.suffocationPredicate;
			copied.blockVisionPredicate = source.blockVisionPredicate;
			copied.lootTable = source.lootTable;
			copied.translationKey = source.translationKey;
			return copied;
		}

		@Deprecated
		public static AbstractBlock.Settings copyShallow(AbstractBlock block) {
			AbstractBlock.Settings copied = new AbstractBlock.Settings();
			AbstractBlock.Settings source = block.settings;
			copied.hardness = source.hardness;
			copied.resistance = source.resistance;
			copied.collidable = source.collidable;
			copied.randomTicks = source.randomTicks;
			copied.luminance = source.luminance;
			copied.mapColorProvider = source.mapColorProvider;
			copied.soundGroup = source.soundGroup;
			copied.slipperiness = source.slipperiness;
			copied.velocityMultiplier = source.velocityMultiplier;
			copied.dynamicBounds = source.dynamicBounds;
			copied.opaque = source.opaque;
			copied.isAir = source.isAir;
			copied.burnable = source.burnable;
			copied.liquid = source.liquid;
			copied.forceNotSolid = source.forceNotSolid;
			copied.forceSolid = source.forceSolid;
			copied.pistonBehavior = source.pistonBehavior;
			copied.toolRequired = source.toolRequired;
			copied.offsetter = source.offsetter;
			copied.blockBreakParticles = source.blockBreakParticles;
			copied.requiredFeatures = source.requiredFeatures;
			copied.emissiveLightingPredicate = source.emissiveLightingPredicate;
			copied.instrument = source.instrument;
			copied.replaceable = source.replaceable;
			return copied;
		}

		public AbstractBlock.Settings mapColor(DyeColor color) {
			this.mapColorProvider = state -> color.getMapColor();
			return this;
		}

		public AbstractBlock.Settings mapColor(MapColor color) {
			this.mapColorProvider = state -> color;
			return this;
		}

		public AbstractBlock.Settings mapColor(Function<BlockState, MapColor> mapColorProvider) {
			this.mapColorProvider = mapColorProvider;
			return this;
		}

		public AbstractBlock.Settings noCollision() {
			this.collidable = false;
			this.opaque = false;
			return this;
		}

		public AbstractBlock.Settings nonOpaque() {
			this.opaque = false;
			return this;
		}

		public AbstractBlock.Settings slipperiness(float slipperiness) {
			this.slipperiness = slipperiness;
			return this;
		}

		public AbstractBlock.Settings velocityMultiplier(float velocityMultiplier) {
			this.velocityMultiplier = velocityMultiplier;
			return this;
		}

		public AbstractBlock.Settings jumpVelocityMultiplier(float jumpVelocityMultiplier) {
			this.jumpVelocityMultiplier = jumpVelocityMultiplier;
			return this;
		}

		public AbstractBlock.Settings sounds(BlockSoundGroup soundGroup) {
			this.soundGroup = soundGroup;
			return this;
		}

		public AbstractBlock.Settings luminance(ToIntFunction<BlockState> luminance) {
			this.luminance = luminance;
			return this;
		}

		public AbstractBlock.Settings strength(float hardness, float resistance) {
			return this.hardness(hardness).resistance(resistance);
		}

		public AbstractBlock.Settings breakInstantly() {
			return this.strength(0.0F);
		}

		public AbstractBlock.Settings strength(float strength) {
			this.strength(strength, strength);
			return this;
		}

		public AbstractBlock.Settings ticksRandomly() {
			this.randomTicks = true;
			return this;
		}

		public AbstractBlock.Settings dynamicBounds() {
			this.dynamicBounds = true;
			return this;
		}

		public AbstractBlock.Settings dropsNothing() {
			this.lootTable = RegistryKeyedValue.fixed(Optional.empty());
			return this;
		}

		public AbstractBlock.Settings lootTable(Optional<RegistryKey<LootTable>> lootTableKey) {
			this.lootTable = RegistryKeyedValue.fixed(lootTableKey);
			return this;
		}

		protected Optional<RegistryKey<LootTable>> getLootTableKey() {
			return this.lootTable.get(Objects.requireNonNull(this.registryKey, "Block id not set"));
		}

		public AbstractBlock.Settings burnable() {
			this.burnable = true;
			return this;
		}

		public AbstractBlock.Settings liquid() {
			this.liquid = true;
			return this;
		}

		public AbstractBlock.Settings solid() {
			this.forceSolid = true;
			return this;
		}

		@Deprecated
		public AbstractBlock.Settings notSolid() {
			this.forceNotSolid = true;
			return this;
		}

		public AbstractBlock.Settings pistonBehavior(PistonBehavior pistonBehavior) {
			this.pistonBehavior = pistonBehavior;
			return this;
		}

		public AbstractBlock.Settings air() {
			this.isAir = true;
			return this;
		}

		public AbstractBlock.Settings allowsSpawning(AbstractBlock.TypedContextPredicate<EntityType<?>> predicate) {
			this.allowsSpawningPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings solidBlock(AbstractBlock.ContextPredicate predicate) {
			this.solidBlockPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings suffocates(AbstractBlock.ContextPredicate predicate) {
			this.suffocationPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings blockVision(AbstractBlock.ContextPredicate predicate) {
			this.blockVisionPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings postProcess(AbstractBlock.ContextPredicate predicate) {
			this.postProcessPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings emissiveLighting(AbstractBlock.ContextPredicate predicate) {
			this.emissiveLightingPredicate = predicate;
			return this;
		}

		public AbstractBlock.Settings requiresTool() {
			this.toolRequired = true;
			return this;
		}

		public AbstractBlock.Settings hardness(float hardness) {
			this.hardness = hardness;
			return this;
		}

		public AbstractBlock.Settings resistance(float resistance) {
			this.resistance = Math.max(0.0F, resistance);
			return this;
		}

		public AbstractBlock.Settings offset(AbstractBlock.OffsetType offsetType) {
			this.offsetter = switch (offsetType) {
				case NONE -> null;
				case XZ -> (state, pos) -> {
					Block block = state.getBlock();
					long l = MathHelper.hashCode(pos.getX(), 0, pos.getZ());
					float f = block.getMaxHorizontalModelOffset();
					double d = MathHelper.clamp(((float) (l & 15L) / 15.0F - 0.5) * 0.5, (double) (-f), (double) f);
					double
							e =
							MathHelper.clamp(((float) (l >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double) (-f), (double) f);
					return new Vec3d(d, 0.0, e);
				};
				case XYZ -> (state, pos) -> {
					Block block = state.getBlock();
					long l = MathHelper.hashCode(pos.getX(), 0, pos.getZ());
					double d = ((float) (l >> 4 & 15L) / 15.0F - 1.0) * block.getVerticalModelOffsetMultiplier();
					float f = block.getMaxHorizontalModelOffset();
					double e = MathHelper.clamp(((float) (l & 15L) / 15.0F - 0.5) * 0.5, (double) (-f), (double) f);
					double
							g =
							MathHelper.clamp(((float) (l >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double) (-f), (double) f);
					return new Vec3d(e, d, g);
				};
			};
			return this;
		}

		public AbstractBlock.Settings noBlockBreakParticles() {
			this.blockBreakParticles = false;
			return this;
		}

		public AbstractBlock.Settings requires(FeatureFlag... features) {
			this.requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(features);
			return this;
		}

		public AbstractBlock.Settings instrument(NoteBlockInstrument instrument) {
			this.instrument = instrument;
			return this;
		}

		public AbstractBlock.Settings replaceable() {
			this.replaceable = true;
			return this;
		}

		public AbstractBlock.Settings registryKey(RegistryKey<Block> registryKey) {
			this.registryKey = registryKey;
			return this;
		}

		public AbstractBlock.Settings overrideTranslationKey(String translationKey) {
			this.translationKey = RegistryKeyedValue.fixed(translationKey);
			return this;
		}

		protected String getTranslationKey() {
			return this.translationKey.get(Objects.requireNonNull(this.registryKey, "Block id not set"));
		}
	}

	/**
	 * Предикат контекста блока с дополнительным типизированным параметром.
	 *
	 * @param <A> тип дополнительного аргумента (например, {@link EntityType})
	 */
	@FunctionalInterface
	public interface TypedContextPredicate<A> {

		boolean test(BlockState state, BlockView world, BlockPos pos, A type);
	}
}

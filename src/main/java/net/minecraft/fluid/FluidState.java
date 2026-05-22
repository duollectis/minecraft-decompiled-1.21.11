package net.minecraft.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.State;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Неизменяемое состояние жидкости, аналог {@code BlockState} для блоков.
 *
 * <p>Хранит набор свойств (уровень, флаг падения) и делегирует всю логику
 * своему владельцу — объекту {@link Fluid}. Все экземпляры регистрируются
 * в {@link Fluid#STATE_IDS} при инициализации {@link Fluids}.</p>
 */
public final class FluidState extends State<Fluid, FluidState> {

	public static final Codec<FluidState> CODEC =
		createCodec(Registries.FLUID.getCodec(), Fluid::getDefaultState).stable();

	/** Максимальное количество «единиц» жидкости в блоке (уровень источника + 1). */
	public static final int MAX_AMOUNT = 9;

	/** Максимальный числовой уровень текущей жидкости. */
	public static final int MAX_FLUID_LEVEL = 8;

	public FluidState(
		Fluid fluid,
		Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
		MapCodec<FluidState> codec
	) {
		super(fluid, propertyMap, codec);
	}

	public Fluid getFluid() {
		return owner;
	}

	public boolean isStill() {
		return getFluid().isStill(this);
	}

	/**
	 * Проверяет, является ли данное состояние стоячей жидкостью конкретного типа.
	 * Используется для определения источников при генерации новых источников.
	 */
	public boolean isEqualAndStill(Fluid fluid) {
		return owner == fluid && owner.isStill(this);
	}

	public boolean isEmpty() {
		return getFluid().isEmpty();
	}

	public float getHeight(BlockView world, BlockPos pos) {
		return getFluid().getHeight(this, world, pos);
	}

	public float getHeight() {
		return getFluid().getHeight(this);
	}

	public int getLevel() {
		return getFluid().getLevel(this);
	}

	/**
	 * Проверяет, может ли жидкость вытечь из данной позиции хотя бы в одном горизонтальном направлении.
	 * Используется для оптимизации: если вытекать некуда, тик не нужен.
	 */
	public boolean canFlowTo(BlockView world, BlockPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos neighbor = pos.add(dx, 0, dz);
				FluidState neighborFluid = world.getFluidState(neighbor);

				if (neighborFluid.getFluid().matchesType(getFluid())) {
					continue;
				}

				if (!world.getBlockState(neighbor).isOpaqueFullCube()) {
					return true;
				}
			}
		}

		return false;
	}

	public void onScheduledTick(ServerWorld world, BlockPos pos, BlockState state) {
		getFluid().onScheduledTick(world, pos, state, this);
	}

	public void randomDisplayTick(World world, BlockPos pos, Random random) {
		getFluid().randomDisplayTick(world, pos, this, random);
	}

	public boolean hasRandomTicks() {
		return getFluid().hasRandomTicks();
	}

	public void onRandomTick(ServerWorld world, BlockPos pos, Random random) {
		getFluid().onRandomTick(world, pos, this, random);
	}

	public Vec3d getVelocity(BlockView world, BlockPos pos) {
		return getFluid().getVelocity(world, pos, this);
	}

	public BlockState getBlockState() {
		return getFluid().toBlockState(this);
	}

	public @Nullable ParticleEffect getParticle() {
		return getFluid().getParticle();
	}

	public boolean isIn(TagKey<Fluid> tag) {
		return getFluid().getRegistryEntry().isIn(tag);
	}

	public boolean isIn(RegistryEntryList<Fluid> fluids) {
		return fluids.contains(getFluid().getRegistryEntry());
	}

	public boolean isOf(Fluid fluid) {
		return getFluid() == fluid;
	}

	public float getBlastResistance() {
		return getFluid().getBlastResistance();
	}

	public boolean canBeReplacedWith(BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
		return getFluid().canBeReplacedWith(this, world, pos, fluid, direction);
	}

	public VoxelShape getShape(BlockView world, BlockPos pos) {
		return getFluid().getShape(this, world, pos);
	}

	public @Nullable Box getCollisionBox(BlockView world, BlockPos pos) {
		return getFluid().getCollisionBox(this, world, pos);
	}

	public RegistryEntry<Fluid> getRegistryEntry() {
		return owner.getRegistryEntry();
	}

	public Stream<TagKey<Fluid>> streamTags() {
		return owner.getRegistryEntry().streamTags();
	}

	public void onEntityCollision(World world, BlockPos pos, Entity entity, EntityCollisionHandler handler) {
		getFluid().onEntityCollision(world, pos, entity, handler);
	}
}

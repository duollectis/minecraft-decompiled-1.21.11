package net.minecraft.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Базовый абстрактный класс для всех жидкостей в игре.
 *
 * <p>Каждая жидкость управляет своими состояниями ({@link FluidState}) через {@link StateManager},
 * аналогично тому, как блоки управляют {@code BlockState}. Жидкость регистрируется в реестре
 * {@code Registries.FLUID} и имеет уникальную запись реестра.</p>
 */
public abstract class Fluid {

	/** Глобальный список всех состояний жидкостей, индексированный по числовому ID. */
	public static final IdList<FluidState> STATE_IDS = new IdList<>();

	protected final StateManager<Fluid, FluidState> stateManager;
	private FluidState defaultState;
	private final RegistryEntry.Reference<Fluid> registryEntry = Registries.FLUID.createEntry(this);

	protected Fluid() {
		StateManager.Builder<Fluid, FluidState> builder = new StateManager.Builder<>(this);
		appendProperties(builder);
		stateManager = builder.build(Fluid::getDefaultState, FluidState::new);
		setDefaultState(stateManager.getDefaultState());
	}

	/**
	 * Регистрирует свойства состояния жидкости в билдере.
	 * Переопределяется подклассами для добавления специфичных свойств (например, уровня или флага падения).
	 */
	protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
	}

	public StateManager<Fluid, FluidState> getStateManager() {
		return stateManager;
	}

	protected final void setDefaultState(FluidState state) {
		defaultState = state;
	}

	public final FluidState getDefaultState() {
		return defaultState;
	}

	/** @return предмет-ведро, соответствующий данной жидкости. */
	public abstract Item getBucketItem();

	/** Вызывается клиентом для визуальных эффектов (частицы, звуки) при случайном тике. */
	protected void randomDisplayTick(World world, BlockPos pos, FluidState state, Random random) {
	}

	/** Вызывается при запланированном тике жидкости на сервере. */
	protected void onScheduledTick(ServerWorld world, BlockPos pos, BlockState blockState, FluidState fluidState) {
	}

	/** Вызывается при случайном тике жидкости на сервере (например, для распространения огня лавой). */
	protected void onRandomTick(ServerWorld world, BlockPos pos, FluidState state, Random random) {
	}

	/** Вызывается при столкновении сущности с жидкостью. */
	protected void onEntityCollision(World world, BlockPos pos, Entity entity, EntityCollisionHandler handler) {
	}

	/** @return частицу, капающую с нижней стороны блока с данной жидкостью, или {@code null}. */
	protected @Nullable ParticleEffect getParticle() {
		return null;
	}

	/**
	 * Определяет, может ли данная жидкость быть вытеснена другой жидкостью {@code fluid},
	 * текущей в направлении {@code direction}.
	 */
	protected abstract boolean canBeReplacedWith(
		FluidState state,
		BlockView world,
		BlockPos pos,
		Fluid fluid,
		Direction direction
	);

	/**
	 * Вычисляет вектор скорости потока жидкости в данной позиции на основе разницы высот
	 * соседних блоков жидкости.
	 */
	protected abstract Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state);

	/** @return интервал тика жидкости в игровых тиках. */
	public abstract int getTickRate(WorldView world);

	/** @return {@code true}, если жидкость поддерживает случайные тики. */
	protected boolean hasRandomTicks() {
		return false;
	}

	/** @return {@code true}, если жидкость является «пустой» (воздух, отсутствие жидкости). */
	protected boolean isEmpty() {
		return false;
	}

	/** @return взрывостойкость жидкости. */
	protected abstract float getBlastResistance();

	/**
	 * Возвращает высоту поверхности жидкости с учётом соседних блоков.
	 * Используется для рендеринга и физики.
	 */
	public abstract float getHeight(FluidState state, BlockView world, BlockPos pos);

	/**
	 * Возвращает высоту поверхности жидкости только на основе её уровня,
	 * без учёта соседних блоков.
	 */
	public abstract float getHeight(FluidState state);

	/** @return состояние блока, соответствующее данному состоянию жидкости. */
	protected abstract BlockState toBlockState(FluidState state);

	/** @return {@code true}, если данное состояние жидкости является стоячим (источником). */
	public abstract boolean isStill(FluidState state);

	/** @return числовой уровень жидкости (1–8 для текущей, 8 для стоячей). */
	public abstract int getLevel(FluidState state);

	/**
	 * Проверяет, является ли переданная жидкость тем же типом, что и данная.
	 * Переопределяется в подклассах для объединения текущей и стоячей форм одной жидкости.
	 */
	public boolean matchesType(Fluid fluid) {
		return fluid == this;
	}

	/**
	 * @deprecated Используй {@link FluidState#isIn(TagKey)} напрямую.
	 */
	@Deprecated
	public boolean isIn(TagKey<Fluid> tag) {
		return registryEntry.isIn(tag);
	}

	/** @return форма (VoxelShape) жидкости для данного состояния и позиции. */
	public abstract VoxelShape getShape(FluidState state, BlockView world, BlockPos pos);

	/**
	 * Возвращает AABB-коллизию жидкости для взаимодействия с сущностями.
	 * Для пустой жидкости возвращает {@code null}.
	 */
	public @Nullable Box getCollisionBox(FluidState state, BlockView world, BlockPos pos) {
		if (isEmpty()) {
			return null;
		}

		float height = state.getHeight(world, pos);
		return new Box(
			pos.getX(), pos.getY(), pos.getZ(),
			pos.getX() + 1.0, pos.getY() + height, pos.getZ() + 1.0
		);
	}

	/** @return звук наполнения ведра данной жидкостью, если он есть. */
	public Optional<SoundEvent> getBucketFillSound() {
		return Optional.empty();
	}

	/**
	 * @deprecated Используй через реестр напрямую.
	 */
	@Deprecated
	public RegistryEntry.Reference<Fluid> getRegistryEntry() {
		return registryEntry;
	}
}

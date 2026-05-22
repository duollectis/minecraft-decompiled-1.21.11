package net.minecraft.entity.decoration;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Базовый класс для display-сущностей (блок, предмет, текст).
 * Управляет интерполяцией трансформаций, billboard-режимом, яркостью и видимостью через DataTracker.
 * Не получает урона, игнорирует поршни и ловушки.
 */
public abstract class DisplayEntity extends Entity {

	static final Logger LOGGER = LogUtils.getLogger();

	public static final int NO_GLOW_COLOR_OVERRIDE = -1;

	private static final TrackedData<Integer> START_INTERPOLATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> INTERPOLATION_DURATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> TELEPORT_DURATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Vector3fc> TRANSLATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR_3F);
	private static final TrackedData<Vector3fc> SCALE =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR_3F);
	private static final TrackedData<Quaternionfc> LEFT_ROTATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNION_F);
	private static final TrackedData<Quaternionfc> RIGHT_ROTATION =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNION_F);
	private static final TrackedData<Byte> BILLBOARD =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Integer> BRIGHTNESS =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> VIEW_RANGE =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SHADOW_RADIUS =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SHADOW_STRENGTH =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> WIDTH =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> HEIGHT =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> GLOW_COLOR_OVERRIDE =
			DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** Набор ID трекеров, изменение которых требует обновления рендер-состояния. */
	private static final IntSet RENDERING_DATA_IDS = IntSet.of(
			new int[]{
					TRANSLATION.id(),
					SCALE.id(),
					LEFT_ROTATION.id(),
					RIGHT_ROTATION.id(),
					BILLBOARD.id(),
					BRIGHTNESS.id(),
					SHADOW_RADIUS.id(),
					SHADOW_STRENGTH.id()
			}
	);

	private static final int DEFAULT_INTERPOLATION_DURATION = 0;
	private static final int DEFAULT_START_INTERPOLATION = 0;
	private static final int DEFAULT_TELEPORT_DURATION = 0;
	private static final float DEFAULT_SHADOW_RADIUS = 0.0F;
	private static final float DEFAULT_SHADOW_STRENGTH = 1.0F;
	private static final float DEFAULT_VIEW_RANGE = 1.0F;
	private static final float DEFAULT_DISPLAY_WIDTH = 0.0F;
	private static final float DEFAULT_DISPLAY_HEIGHT = 0.0F;
	private static final int DEFAULT_BRIGHTNESS = -1;

	/** Максимально допустимая длительность телепортации (в тиках). */
	private static final int MAX_TELEPORT_DURATION = 59;

	public static final String TELEPORT_DURATION_KEY = "teleport_duration";
	public static final String INTERPOLATION_DURATION_KEY = "interpolation_duration";
	public static final String START_INTERPOLATION_KEY = "start_interpolation";
	public static final String TRANSFORMATION_NBT_KEY = "transformation";
	public static final String BILLBOARD_NBT_KEY = "billboard";
	public static final String BRIGHTNESS_NBT_KEY = "brightness";
	public static final String VIEW_RANGE_NBT_KEY = "view_range";
	public static final String SHADOW_RADIUS_NBT_KEY = "shadow_radius";
	public static final String SHADOW_STRENGTH_NBT_KEY = "shadow_strength";
	public static final String WIDTH_NBT_KEY = "width";
	public static final String HEIGHT_NBT_KEY = "height";
	public static final String GLOW_COLOR_OVERRIDE_NBT_KEY = "glow_color_override";

	private long interpolationStart = Long.MIN_VALUE;
	private int interpolationDuration;
	private float lerpProgress;
	private Box visibilityBoundingBox;
	private boolean tooSmallToRender = true;
	protected boolean renderingDataSet;
	private boolean startInterpolationSet;
	private boolean interpolationDurationSet;
	private @Nullable RenderState renderProperties;
	private final PositionInterpolator interpolator = new PositionInterpolator(this, 0);

	public DisplayEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
		noClip = true;
		visibilityBoundingBox = getBoundingBox();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (HEIGHT.equals(data) || WIDTH.equals(data)) {
			updateVisibilityBoundingBox();
		}

		if (START_INTERPOLATION.equals(data)) {
			startInterpolationSet = true;
		}

		if (TELEPORT_DURATION.equals(data)) {
			interpolator.setLerpDuration(getTeleportDuration());
		}

		if (INTERPOLATION_DURATION.equals(data)) {
			interpolationDurationSet = true;
		}

		if (RENDERING_DATA_IDS.contains(data.id())) {
			renderingDataSet = true;
		}
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	private static AffineTransformation getTransformation(DataTracker dataTracker) {
		Vector3fc translation = dataTracker.get(TRANSLATION);
		Quaternionfc leftRotation = dataTracker.get(LEFT_ROTATION);
		Vector3fc scale = dataTracker.get(SCALE);
		Quaternionfc rightRotation = dataTracker.get(RIGHT_ROTATION);
		return new AffineTransformation(translation, leftRotation, scale, rightRotation);
	}

	/**
	 * На клиенте обрабатывает интерполяцию трансформаций и позиции.
	 * Обновляет рендер-состояние при изменении данных трекера.
	 */
	@Override
	public void tick() {
		Entity vehicle = getVehicle();
		if (vehicle != null && vehicle.isRemoved()) {
			stopRiding();
		}

		if (getEntityWorld().isClient()) {
			if (startInterpolationSet) {
				startInterpolationSet = false;
				interpolationStart = age + getStartInterpolation();
			}

			if (interpolationDurationSet) {
				interpolationDurationSet = false;
				interpolationDuration = getInterpolationDuration();
			}

			if (renderingDataSet) {
				renderingDataSet = false;
				boolean shouldLerp = interpolationDuration != 0;
				if (shouldLerp && renderProperties != null) {
					renderProperties = getLerpedRenderState(renderProperties, lerpProgress);
				} else {
					renderProperties = copyRenderState();
				}

				refreshData(shouldLerp, lerpProgress);
			}

			interpolator.tick();
		}
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return interpolator;
	}

	/**
	 * Обновляет специфичные для подкласса данные рендеринга.
	 * Вызывается при изменении трекеров рендеринга на клиенте.
	 *
	 * @param shouldLerp    нужна ли интерполяция от предыдущего состояния
	 * @param lerpProgress  текущий прогресс интерполяции [0, 1]
	 */
	protected abstract void refreshData(boolean shouldLerp, float lerpProgress);

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(TELEPORT_DURATION, DEFAULT_TELEPORT_DURATION);
		builder.add(START_INTERPOLATION, DEFAULT_START_INTERPOLATION);
		builder.add(INTERPOLATION_DURATION, DEFAULT_INTERPOLATION_DURATION);
		builder.add(TRANSLATION, new Vector3f());
		builder.add(SCALE, new Vector3f(1.0F, 1.0F, 1.0F));
		builder.add(RIGHT_ROTATION, new Quaternionf());
		builder.add(LEFT_ROTATION, new Quaternionf());
		builder.add(BILLBOARD, BillboardMode.FIXED.getIndex());
		builder.add(BRIGHTNESS, DEFAULT_BRIGHTNESS);
		builder.add(VIEW_RANGE, DEFAULT_VIEW_RANGE);
		builder.add(SHADOW_RADIUS, DEFAULT_SHADOW_RADIUS);
		builder.add(SHADOW_STRENGTH, DEFAULT_SHADOW_STRENGTH);
		builder.add(WIDTH, DEFAULT_DISPLAY_WIDTH);
		builder.add(HEIGHT, DEFAULT_DISPLAY_HEIGHT);
		builder.add(GLOW_COLOR_OVERRIDE, NO_GLOW_COLOR_OVERRIDE);
	}

	@Override
	protected void readCustomData(ReadView view) {
		setTransformation(view.<AffineTransformation>read("transformation", AffineTransformation.ANY_CODEC)
				.orElse(AffineTransformation.identity()));
		setInterpolationDuration(view.getInt("interpolation_duration", DEFAULT_INTERPOLATION_DURATION));
		setStartInterpolation(view.getInt("start_interpolation", DEFAULT_START_INTERPOLATION));
		setTeleportDuration(MathHelper.clamp(
				view.getInt("teleport_duration", DEFAULT_TELEPORT_DURATION),
				0,
				MAX_TELEPORT_DURATION
		));
		setBillboardMode(view.<BillboardMode>read("billboard", BillboardMode.CODEC)
				.orElse(BillboardMode.FIXED));
		setViewRange(view.getFloat("view_range", DEFAULT_VIEW_RANGE));
		setShadowRadius(view.getFloat("shadow_radius", DEFAULT_SHADOW_RADIUS));
		setShadowStrength(view.getFloat("shadow_strength", DEFAULT_SHADOW_STRENGTH));
		setDisplayWidth(view.getFloat("width", DEFAULT_DISPLAY_WIDTH));
		setDisplayHeight(view.getFloat("height", DEFAULT_DISPLAY_HEIGHT));
		setGlowColorOverride(view.getInt("glow_color_override", NO_GLOW_COLOR_OVERRIDE));
		setBrightness(view.<Brightness>read("brightness", Brightness.CODEC).orElse(null));
	}

	public final void setTransformation(AffineTransformation transformation) {
		dataTracker.set(TRANSLATION, transformation.getTranslation());
		dataTracker.set(LEFT_ROTATION, transformation.getLeftRotation());
		dataTracker.set(SCALE, transformation.getScale());
		dataTracker.set(RIGHT_ROTATION, transformation.getRightRotation());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("transformation", AffineTransformation.ANY_CODEC, getTransformation(dataTracker));
		view.put("billboard", BillboardMode.CODEC, getBillboardMode());
		view.putInt("interpolation_duration", getInterpolationDuration());
		view.putInt("teleport_duration", getTeleportDuration());
		view.putFloat("view_range", getViewRange());
		view.putFloat("shadow_radius", getShadowRadius());
		view.putFloat("shadow_strength", getShadowStrength());
		view.putFloat("width", getDisplayWidth());
		view.putFloat("height", getDisplayHeight());
		view.putInt("glow_color_override", getGlowColorOverride());
		view.putNullable("brightness", Brightness.CODEC, getBrightnessUnpacked());
	}

	public Box getVisibilityBoundingBox() {
		return visibilityBoundingBox;
	}

	public boolean shouldRender() {
		return !tooSmallToRender;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	public @Nullable RenderState getRenderState() {
		return renderProperties;
	}

	public final void setInterpolationDuration(int interpolationDuration) {
		dataTracker.set(INTERPOLATION_DURATION, interpolationDuration);
	}

	public final int getInterpolationDuration() {
		return dataTracker.get(INTERPOLATION_DURATION);
	}

	public final void setStartInterpolation(int startInterpolation) {
		dataTracker.set(START_INTERPOLATION, startInterpolation, true);
	}

	public final int getStartInterpolation() {
		return dataTracker.get(START_INTERPOLATION);
	}

	public final void setTeleportDuration(int teleportDuration) {
		dataTracker.set(TELEPORT_DURATION, teleportDuration);
	}

	public final int getTeleportDuration() {
		return dataTracker.get(TELEPORT_DURATION);
	}

	public final void setBillboardMode(BillboardMode billboardMode) {
		dataTracker.set(BILLBOARD, billboardMode.getIndex());
	}

	public final BillboardMode getBillboardMode() {
		return BillboardMode.FROM_INDEX.apply(dataTracker.get(BILLBOARD));
	}

	public final void setBrightness(@Nullable Brightness brightness) {
		dataTracker.set(BRIGHTNESS, brightness != null ? brightness.pack() : DEFAULT_BRIGHTNESS);
	}

	public final @Nullable Brightness getBrightnessUnpacked() {
		int packed = dataTracker.get(BRIGHTNESS);
		return packed != DEFAULT_BRIGHTNESS ? Brightness.unpack(packed) : null;
	}

	public final int getBrightness() {
		return dataTracker.get(BRIGHTNESS);
	}

	public final void setViewRange(float viewRange) {
		dataTracker.set(VIEW_RANGE, viewRange);
	}

	public final float getViewRange() {
		return dataTracker.get(VIEW_RANGE);
	}

	public final void setShadowRadius(float shadowRadius) {
		dataTracker.set(SHADOW_RADIUS, shadowRadius);
	}

	public final float getShadowRadius() {
		return dataTracker.get(SHADOW_RADIUS);
	}

	public final void setShadowStrength(float shadowStrength) {
		dataTracker.set(SHADOW_STRENGTH, shadowStrength);
	}

	public final float getShadowStrength() {
		return dataTracker.get(SHADOW_STRENGTH);
	}

	public final void setDisplayWidth(float width) {
		dataTracker.set(WIDTH, width);
	}

	public final float getDisplayWidth() {
		return dataTracker.get(WIDTH);
	}

	public final void setDisplayHeight(float height) {
		dataTracker.set(HEIGHT, height);
	}

	public final float getDisplayHeight() {
		return dataTracker.get(HEIGHT);
	}

	public final int getGlowColorOverride() {
		return dataTracker.get(GLOW_COLOR_OVERRIDE);
	}

	public final void setGlowColorOverride(int glowColorOverride) {
		dataTracker.set(GLOW_COLOR_OVERRIDE, glowColorOverride);
	}

	/**
	 * Вычисляет прогресс интерполяции трансформации на основе текущего тика и tickProgress.
	 * Сохраняет результат в {@code lerpProgress} для последующего использования.
	 */
	public float getLerpProgress(float tickProgress) {
		if (interpolationDuration <= 0) {
			return 1.0F;
		}

		float elapsed = (float) (age - interpolationStart);
		float progress = MathHelper.clamp(
				MathHelper.getLerpProgress(elapsed + tickProgress, 0.0F, (float) interpolationDuration),
				0.0F,
				1.0F
		);
		lerpProgress = progress;
		return progress;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		updateVisibilityBoundingBox();
	}

	private void updateVisibilityBoundingBox() {
		float width = getDisplayWidth();
		float height = getDisplayHeight();
		tooSmallToRender = width == 0.0F || height == 0.0F;
		float halfWidth = width / 2.0F;
		double x = getX();
		double y = getY();
		double z = getZ();
		visibilityBoundingBox = new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < MathHelper.square(getViewRange() * 64.0 * getRenderDistanceMultiplier());
	}

	@Override
	public int getTeamColorValue() {
		int glowColor = getGlowColorOverride();
		return glowColor != NO_GLOW_COLOR_OVERRIDE ? glowColor : super.getTeamColorValue();
	}

	private RenderState copyRenderState() {
		return new RenderState(
				AbstractInterpolator.constant(getTransformation(dataTracker)),
				getBillboardMode(),
				getBrightness(),
				FloatLerper.constant(getShadowRadius()),
				FloatLerper.constant(getShadowStrength()),
				getGlowColorOverride()
		);
	}

	private RenderState getLerpedRenderState(RenderState state, float progress) {
		AffineTransformation prevTransform = state.transformation.interpolate(progress);
		float prevShadowRadius = state.shadowRadius.lerp(progress);
		float prevShadowStrength = state.shadowStrength.lerp(progress);
		return new RenderState(
				new AffineTransformationInterpolator(prevTransform, getTransformation(dataTracker)),
				getBillboardMode(),
				getBrightness(),
				new FloatLerperImpl(prevShadowRadius, getShadowRadius()),
				new FloatLerperImpl(prevShadowStrength, getShadowStrength()),
				getGlowColorOverride()
		);
	}

	// ─── Вложенные типы ───────────────────────────────────────────────────────

	/**
	 * Интерполятор произвольного значения по прогрессу [0, 1].
	 */
	@FunctionalInterface
	public interface AbstractInterpolator<T> {

		static <T> AbstractInterpolator<T> constant(T value) {
			return delta -> value;
		}

		T interpolate(float delta);
	}

	/**
	 * Интерполятор аффинных трансформаций между двумя состояниями.
	 */
	record AffineTransformationInterpolator(AffineTransformation previous, AffineTransformation current)
			implements AbstractInterpolator<AffineTransformation> {

		@Override
		public AffineTransformation interpolate(float delta) {
			return delta >= 1.0 ? current : previous.interpolate(current, delta);
		}
	}

	/**
	 * Интерполятор ARGB-цвета через {@link ColorHelper#lerp}.
	 */
	record ArgbLerper(int previous, int current) implements IntLerper {

		@Override
		public int lerp(float delta) {
			return ColorHelper.lerp(delta, previous, current);
		}
	}

	/**
	 * Режим billboard-ориентации display-сущности относительно камеры.
	 */
	public enum BillboardMode implements StringIdentifiable {
		FIXED((byte) 0, "fixed"),
		VERTICAL((byte) 1, "vertical"),
		HORIZONTAL((byte) 2, "horizontal"),
		CENTER((byte) 3, "center");

		public static final Codec<BillboardMode> CODEC =
				StringIdentifiable.createCodec(BillboardMode::values);
		public static final IntFunction<BillboardMode> FROM_INDEX = ValueLists.createIndexToValueFunction(
				BillboardMode::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
		);

		private final byte index;
		private final String name;

		BillboardMode(final byte index, final String name) {
			this.name = name;
			this.index = index;
		}

		@Override
		public String asString() {
			return name;
		}

		byte getIndex() {
			return index;
		}
	}

	/**
	 * Display-сущность для отображения блока.
	 */
	public static class BlockDisplayEntity extends DisplayEntity {

		public static final String BLOCK_STATE_NBT_KEY = "block_state";
		private static final TrackedData<BlockState> BLOCK_STATE = DataTracker.registerData(
				BlockDisplayEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE
		);
		private BlockDisplayEntity.@Nullable Data data;

		public BlockDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker(DataTracker.Builder builder) {
			super.initDataTracker(builder);
			builder.add(BLOCK_STATE, Blocks.AIR.getDefaultState());
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (data.equals(BLOCK_STATE)) {
				renderingDataSet = true;
			}
		}

		public final BlockState getBlockState() {
			return dataTracker.get(BLOCK_STATE);
		}

		public final void setBlockState(BlockState state) {
			dataTracker.set(BLOCK_STATE, state);
		}

		@Override
		protected void readCustomData(ReadView view) {
			super.readCustomData(view);
			setBlockState(view.<BlockState>read("block_state", BlockState.CODEC)
					.orElse(Blocks.AIR.getDefaultState()));
		}

		@Override
		protected void writeCustomData(WriteView view) {
			super.writeCustomData(view);
			view.put("block_state", BlockState.CODEC, getBlockState());
		}

		public BlockDisplayEntity.@Nullable Data getData() {
			return data;
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			data = new BlockDisplayEntity.Data(getBlockState());
		}

		/** Снимок данных блока для рендеринга. */
		public record Data(BlockState blockState) {
		}
	}

	/**
	 * Интерполятор float-значения по прогрессу [0, 1].
	 */
	@FunctionalInterface
	public interface FloatLerper {

		static FloatLerper constant(float value) {
			return delta -> value;
		}

		float lerp(float delta);
	}

	/**
	 * Линейный интерполятор float между двумя значениями.
	 */
	record FloatLerperImpl(float previous, float current) implements FloatLerper {

		@Override
		public float lerp(float delta) {
			return MathHelper.lerp(delta, previous, current);
		}
	}

	/**
	 * Интерполятор int-значения по прогрессу [0, 1].
	 */
	@FunctionalInterface
	public interface IntLerper {

		static IntLerper constant(int value) {
			return delta -> value;
		}

		int lerp(float delta);
	}

	/**
	 * Линейный интерполятор int между двумя значениями.
	 */
	record IntLerperImpl(int previous, int current) implements IntLerper {

		@Override
		public int lerp(float delta) {
			return MathHelper.lerp(delta, previous, current);
		}
	}

	/**
	 * Display-сущность для отображения предмета.
	 */
	public static class ItemDisplayEntity extends DisplayEntity {

		private static final String ITEM_NBT_KEY = "item";
		private static final String ITEM_DISPLAY_NBT_KEY = "item_display";
		private static final TrackedData<ItemStack> ITEM =
				DataTracker.registerData(ItemDisplayEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
		private static final TrackedData<Byte> ITEM_DISPLAY =
				DataTracker.registerData(ItemDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);

		private final StackReference stackReference = StackReference.of(this::getItemStack, this::setItemStack);
		private ItemDisplayEntity.@Nullable Data data;

		public ItemDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker(DataTracker.Builder builder) {
			super.initDataTracker(builder);
			builder.add(ITEM, ItemStack.EMPTY);
			builder.add(ITEM_DISPLAY, ItemDisplayContext.NONE.getIndex());
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (ITEM.equals(data) || ITEM_DISPLAY.equals(data)) {
				renderingDataSet = true;
			}
		}

		public final ItemStack getItemStack() {
			return dataTracker.get(ITEM);
		}

		public final void setItemStack(ItemStack stack) {
			dataTracker.set(ITEM, stack);
		}

		public final void setItemDisplayContext(ItemDisplayContext context) {
			dataTracker.set(ITEM_DISPLAY, context.getIndex());
		}

		public final ItemDisplayContext getItemDisplayContext() {
			return ItemDisplayContext.FROM_INDEX.apply(dataTracker.get(ITEM_DISPLAY));
		}

		@Override
		protected void readCustomData(ReadView view) {
			super.readCustomData(view);
			setItemStack(view.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
			setItemDisplayContext(view.<ItemDisplayContext>read("item_display", ItemDisplayContext.CODEC)
					.orElse(ItemDisplayContext.NONE));
		}

		@Override
		protected void writeCustomData(WriteView view) {
			super.writeCustomData(view);
			ItemStack itemStack = getItemStack();
			if (!itemStack.isEmpty()) {
				view.put("item", ItemStack.CODEC, itemStack);
			}

			view.put("item_display", ItemDisplayContext.CODEC, getItemDisplayContext());
		}

		@Override
		public @Nullable StackReference getStackReference(int slot) {
			return slot == 0 ? stackReference : null;
		}

		public ItemDisplayEntity.@Nullable Data getData() {
			return data;
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			ItemStack itemStack = getItemStack();
			itemStack.setHolder(this);
			data = new ItemDisplayEntity.Data(itemStack, getItemDisplayContext());
		}

		/** Снимок данных предмета для рендеринга. */
		public record Data(ItemStack itemStack, ItemDisplayContext itemTransform) {
		}
	}

	/**
	 * Снимок рендер-состояния display-сущности для интерполяции на клиенте.
	 */
	public record RenderState(
			AbstractInterpolator<AffineTransformation> transformation,
			BillboardMode billboardConstraints,
			int brightnessOverride,
			FloatLerper shadowRadius,
			FloatLerper shadowStrength,
			int glowColorOverride
	) {
	}

	/**
	 * Display-сущность для отображения текста с поддержкой переноса строк,
	 * прозрачности, фона и выравнивания.
	 */
	public static class TextDisplayEntity extends DisplayEntity {

		public static final String TEXT_NBT_KEY = "text";
		private static final String LINE_WIDTH_NBT_KEY = "line_width";
		private static final String TEXT_OPACITY_NBT_KEY = "text_opacity";
		private static final String BACKGROUND_NBT_KEY = "background";
		private static final String SHADOW_NBT_KEY = "shadow";
		private static final String SEE_THROUGH_NBT_KEY = "see_through";
		private static final String DEFAULT_BACKGROUND_NBT_KEY = "default_background";
		private static final String ALIGNMENT_NBT_KEY = "alignment";

		public static final byte SHADOW_FLAG = 1;
		public static final byte SEE_THROUGH_FLAG = 2;
		public static final byte DEFAULT_BACKGROUND_FLAG = 4;
		public static final byte LEFT_ALIGNMENT_FLAG = 8;
		public static final byte RIGHT_ALIGNMENT_FLAG = 16;

		private static final byte INITIAL_TEXT_OPACITY = -1;
		public static final int INITIAL_BACKGROUND = 1073741824;
		private static final int DEFAULT_LINE_WIDTH = 200;

		private static final TrackedData<Text> TEXT = DataTracker.registerData(
				TextDisplayEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT
		);
		private static final TrackedData<Integer> LINE_WIDTH =
				DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
		private static final TrackedData<Integer> BACKGROUND =
				DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
		private static final TrackedData<Byte> TEXT_OPACITY =
				DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
		private static final TrackedData<Byte> TEXT_DISPLAY_FLAGS = DataTracker.registerData(
				TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE
		);
		private static final IntSet TEXT_RENDERING_DATA_IDS = IntSet.of(
				new int[]{TEXT.id(), LINE_WIDTH.id(), BACKGROUND.id(), TEXT_OPACITY.id(), TEXT_DISPLAY_FLAGS.id()}
		);

		private @Nullable TextLines textLines;
		private @Nullable Data data;

		public TextDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker(DataTracker.Builder builder) {
			super.initDataTracker(builder);
			builder.add(TEXT, Text.empty());
			builder.add(LINE_WIDTH, DEFAULT_LINE_WIDTH);
			builder.add(BACKGROUND, INITIAL_BACKGROUND);
			builder.add(TEXT_OPACITY, INITIAL_TEXT_OPACITY);
			builder.add(TEXT_DISPLAY_FLAGS, (byte) 0);
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (TEXT_RENDERING_DATA_IDS.contains(data.id())) {
				renderingDataSet = true;
			}
		}

		public final Text getText() {
			return dataTracker.get(TEXT);
		}

		public final void setText(Text text) {
			dataTracker.set(TEXT, text);
		}

		public final int getLineWidth() {
			return dataTracker.get(LINE_WIDTH);
		}

		public final void setLineWidth(int lineWidth) {
			dataTracker.set(LINE_WIDTH, lineWidth);
		}

		public final byte getTextOpacity() {
			return dataTracker.get(TEXT_OPACITY);
		}

		public final void setTextOpacity(byte textOpacity) {
			dataTracker.set(TEXT_OPACITY, textOpacity);
		}

		public final int getBackground() {
			return dataTracker.get(BACKGROUND);
		}

		public final void setBackground(int background) {
			dataTracker.set(BACKGROUND, background);
		}

		public final byte getDisplayFlags() {
			return dataTracker.get(TEXT_DISPLAY_FLAGS);
		}

		public final void setDisplayFlags(byte flags) {
			dataTracker.set(TEXT_DISPLAY_FLAGS, flags);
		}

		private static byte readFlag(byte flags, ReadView view, String nbtKey, byte flag) {
			return view.getBoolean(nbtKey, false) ? (byte) (flags | flag) : flags;
		}

		@Override
		protected void readCustomData(ReadView view) {
			super.readCustomData(view);
			setLineWidth(view.getInt("line_width", DEFAULT_LINE_WIDTH));
			setTextOpacity(view.getByte("text_opacity", INITIAL_TEXT_OPACITY));
			setBackground(view.getInt("background", INITIAL_BACKGROUND));
			byte flags = readFlag((byte) 0, view, "shadow", SHADOW_FLAG);
			flags = readFlag(flags, view, "see_through", SEE_THROUGH_FLAG);
			flags = readFlag(flags, view, "default_background", DEFAULT_BACKGROUND_FLAG);
			Optional<TextAlignment> alignment = view.read("alignment", TextAlignment.CODEC);
			if (alignment.isPresent()) {
				flags = switch (alignment.get()) {
					case CENTER -> flags;
					case LEFT -> (byte) (flags | LEFT_ALIGNMENT_FLAG);
					case RIGHT -> (byte) (flags | RIGHT_ALIGNMENT_FLAG);
				};
			}

			setDisplayFlags(flags);
			Optional<Text> rawText = view.read("text", TextCodecs.CODEC);
			if (rawText.isPresent()) {
				try {
					if (getEntityWorld() instanceof ServerWorld serverWorld) {
						ServerCommandSource source = getCommandSource(serverWorld)
								.withPermissions(LeveledPermissionPredicate.GAMEMASTERS);
						setText(Texts.parse(source, rawText.get(), this, 0));
					} else {
						setText(Text.empty());
					}
				} catch (Exception exception) {
					LOGGER.warn("Failed to parse display entity text {}", rawText, exception);
				}
			}
		}

		private static void writeFlag(byte flags, WriteView view, String nbtKey, byte flag) {
			view.putBoolean(nbtKey, (flags & flag) != 0);
		}

		@Override
		protected void writeCustomData(WriteView view) {
			super.writeCustomData(view);
			view.put("text", TextCodecs.CODEC, getText());
			view.putInt("line_width", getLineWidth());
			view.putInt("background", getBackground());
			view.putByte("text_opacity", getTextOpacity());
			byte flags = getDisplayFlags();
			writeFlag(flags, view, "shadow", SHADOW_FLAG);
			writeFlag(flags, view, "see_through", SEE_THROUGH_FLAG);
			writeFlag(flags, view, "default_background", DEFAULT_BACKGROUND_FLAG);
			view.put("alignment", TextAlignment.CODEC, getAlignment(flags));
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			if (shouldLerp && data != null) {
				data = getLerpedRenderState(data, lerpProgress);
			} else {
				data = copyData();
			}

			textLines = null;
		}

		public @Nullable Data getData() {
			return data;
		}

		private Data copyData() {
			return new Data(
					getText(),
					getLineWidth(),
					IntLerper.constant(getTextOpacity()),
					IntLerper.constant(getBackground()),
					getDisplayFlags()
			);
		}

		private Data getLerpedRenderState(Data prevData, float progress) {
			int prevBackground = prevData.backgroundColor.lerp(progress);
			int prevOpacity = prevData.textOpacity.lerp(progress);
			return new Data(
					getText(),
					getLineWidth(),
					new IntLerperImpl(prevOpacity, getTextOpacity()),
					new ArgbLerper(prevBackground, getBackground()),
					getDisplayFlags()
			);
		}

		/**
		 * Разбивает текст на строки с кешированием результата.
		 * Кеш сбрасывается при каждом обновлении данных рендеринга.
		 */
		public TextLines splitLines(LineSplitter splitter) {
			if (textLines == null) {
				textLines = data != null
						? splitter.split(data.text(), data.lineWidth())
						: new TextLines(List.of(), 0);
			}

			return textLines;
		}

		/** Определяет выравнивание текста по битовым флагам. */
		public static TextAlignment getAlignment(byte flags) {
			if ((flags & LEFT_ALIGNMENT_FLAG) != 0) {
				return TextAlignment.LEFT;
			}

			return (flags & RIGHT_ALIGNMENT_FLAG) != 0 ? TextAlignment.RIGHT : TextAlignment.CENTER;
		}

		/** Снимок данных текста для рендеринга с интерполируемыми цветами. */
		public record Data(
				Text text,
				int lineWidth,
				IntLerper textOpacity,
				IntLerper backgroundColor,
				byte flags
		) {
		}

		/** Функция разбивки текста на строки заданной ширины. */
		@FunctionalInterface
		public interface LineSplitter {

			TextLines split(Text text, int lineWidth);
		}

		/** Выравнивание текста в display-сущности. */
		public enum TextAlignment implements StringIdentifiable {
			CENTER("center"),
			LEFT("left"),
			RIGHT("right");

			public static final Codec<TextAlignment> CODEC =
					StringIdentifiable.createCodec(TextAlignment::values);

			private final String name;

			TextAlignment(final String name) {
				this.name = name;
			}

			@Override
			public String asString() {
				return name;
			}
		}

		/** Одна строка текста с её шириной в пикселях. */
		public record TextLine(OrderedText contents, int width) {
		}

		/** Результат разбивки текста: список строк и общая ширина. */
		public record TextLines(List<TextLine> lines, int width) {
		}
	}
}

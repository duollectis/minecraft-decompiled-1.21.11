package net.minecraft.entity.decoration;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
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

public abstract class DisplayEntity extends Entity {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final int field_42384 = -1;
   private static final TrackedData<Integer> START_INTERPOLATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Integer> INTERPOLATION_DURATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Integer> TELEPORT_DURATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Vector3fc> TRANSLATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR_3F);
   private static final TrackedData<Vector3fc> SCALE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR_3F);
   private static final TrackedData<Quaternionfc> LEFT_ROTATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNION_F);
   private static final TrackedData<Quaternionfc> RIGHT_ROTATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNION_F);
   private static final TrackedData<Byte> BILLBOARD = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
   private static final TrackedData<Integer> BRIGHTNESS = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Float> VIEW_RANGE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Float> SHADOW_RADIUS = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Float> SHADOW_STRENGTH = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Float> WIDTH = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Float> HEIGHT = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Integer> GLOW_COLOR_OVERRIDE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final IntSet RENDERING_DATA_IDS = IntSet.of(
      new int[]{
         TRANSLATION.id(), SCALE.id(), LEFT_ROTATION.id(), RIGHT_ROTATION.id(), BILLBOARD.id(), BRIGHTNESS.id(), SHADOW_RADIUS.id(), SHADOW_STRENGTH.id()
      }
   );
   private static final int DEFAULT_INTERPOLATION_DURATION = 0;
   private static final int DEFAULT_START_INTERPOLATION = 0;
   private static final int field_56423 = 0;
   private static final float field_42376 = 0.0F;
   private static final float field_42377 = 1.0F;
   private static final float field_57575 = 1.0F;
   private static final float field_57576 = 0.0F;
   private static final float field_57577 = 0.0F;
   private static final int field_42378 = -1;
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
   private long interpolationStart = -2147483648L;
   private int interpolationDuration;
   private float lerpProgress;
   private Box visibilityBoundingBox;
   private boolean tooSmallToRender = true;
   protected boolean renderingDataSet;
   private boolean startInterpolationSet;
   private boolean interpolationDurationSet;
   private DisplayEntity.@Nullable RenderState renderProperties;
   private final PositionInterpolator interpolator = new PositionInterpolator(this, 0);

   public DisplayEntity(EntityType<?> entityType, World world) {
      super(entityType, world);
      this.noClip = true;
      this.visibilityBoundingBox = this.getBoundingBox();
   }

   @Override
   public void onTrackedDataSet(TrackedData<?> data) {
      super.onTrackedDataSet(data);
      if (HEIGHT.equals(data) || WIDTH.equals(data)) {
         this.updateVisibilityBoundingBox();
      }

      if (START_INTERPOLATION.equals(data)) {
         this.startInterpolationSet = true;
      }

      if (TELEPORT_DURATION.equals(data)) {
         this.interpolator.setLerpDuration(this.getTeleportDuration());
      }

      if (INTERPOLATION_DURATION.equals(data)) {
         this.interpolationDurationSet = true;
      }

      if (RENDERING_DATA_IDS.contains(data.id())) {
         this.renderingDataSet = true;
      }
   }

   @Override
   public final boolean damage(ServerWorld world, DamageSource source, float amount) {
      return false;
   }

   private static AffineTransformation getTransformation(DataTracker dataTracker) {
      Vector3fc vector3fc = dataTracker.get(TRANSLATION);
      Quaternionfc quaternionfc = dataTracker.get(LEFT_ROTATION);
      Vector3fc vector3fc2 = dataTracker.get(SCALE);
      Quaternionfc quaternionfc2 = dataTracker.get(RIGHT_ROTATION);
      return new AffineTransformation(vector3fc, quaternionfc, vector3fc2, quaternionfc2);
   }

   @Override
   public void tick() {
      Entity entity = this.getVehicle();
      if (entity != null && entity.isRemoved()) {
         this.stopRiding();
      }

      if (this.getEntityWorld().isClient()) {
         if (this.startInterpolationSet) {
            this.startInterpolationSet = false;
            int i = this.getStartInterpolation();
            this.interpolationStart = this.age + i;
         }

         if (this.interpolationDurationSet) {
            this.interpolationDurationSet = false;
            this.interpolationDuration = this.getInterpolationDuration();
         }

         if (this.renderingDataSet) {
            this.renderingDataSet = false;
            boolean bl = this.interpolationDuration != 0;
            if (bl && this.renderProperties != null) {
               this.renderProperties = this.getLerpedRenderState(this.renderProperties, this.lerpProgress);
            } else {
               this.renderProperties = this.copyRenderState();
            }

            this.refreshData(bl, this.lerpProgress);
         }

         this.interpolator.tick();
      }
   }

   @Override
   public PositionInterpolator getInterpolator() {
      return this.interpolator;
   }

   protected abstract void refreshData(boolean shouldLerp, float lerpProgress);

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      builder.add(TELEPORT_DURATION, 0);
      builder.add(START_INTERPOLATION, 0);
      builder.add(INTERPOLATION_DURATION, 0);
      builder.add(TRANSLATION, new Vector3f());
      builder.add(SCALE, new Vector3f(1.0F, 1.0F, 1.0F));
      builder.add(RIGHT_ROTATION, new Quaternionf());
      builder.add(LEFT_ROTATION, new Quaternionf());
      builder.add(BILLBOARD, DisplayEntity.BillboardMode.FIXED.getIndex());
      builder.add(BRIGHTNESS, -1);
      builder.add(VIEW_RANGE, 1.0F);
      builder.add(SHADOW_RADIUS, 0.0F);
      builder.add(SHADOW_STRENGTH, 1.0F);
      builder.add(WIDTH, 0.0F);
      builder.add(HEIGHT, 0.0F);
      builder.add(GLOW_COLOR_OVERRIDE, -1);
   }

   @Override
   protected void readCustomData(ReadView view) {
      this.setTransformation(view.<AffineTransformation>read("transformation", AffineTransformation.ANY_CODEC).orElse(AffineTransformation.identity()));
      this.setInterpolationDuration(view.getInt("interpolation_duration", 0));
      this.setStartInterpolation(view.getInt("start_interpolation", 0));
      int i = view.getInt("teleport_duration", 0);
      this.setTeleportDuration(MathHelper.clamp(i, 0, 59));
      this.setBillboardMode(view.<DisplayEntity.BillboardMode>read("billboard", DisplayEntity.BillboardMode.CODEC).orElse(DisplayEntity.BillboardMode.FIXED));
      this.setViewRange(view.getFloat("view_range", 1.0F));
      this.setShadowRadius(view.getFloat("shadow_radius", 0.0F));
      this.setShadowStrength(view.getFloat("shadow_strength", 1.0F));
      this.setDisplayWidth(view.getFloat("width", 0.0F));
      this.setDisplayHeight(view.getFloat("height", 0.0F));
      this.setGlowColorOverride(view.getInt("glow_color_override", -1));
      this.setBrightness(view.<Brightness>read("brightness", Brightness.CODEC).orElse(null));
   }

   public final void setTransformation(AffineTransformation transformation) {
      this.dataTracker.set(TRANSLATION, transformation.getTranslation());
      this.dataTracker.set(LEFT_ROTATION, transformation.getLeftRotation());
      this.dataTracker.set(SCALE, transformation.getScale());
      this.dataTracker.set(RIGHT_ROTATION, transformation.getRightRotation());
   }

   @Override
   protected void writeCustomData(WriteView view) {
      view.put("transformation", AffineTransformation.ANY_CODEC, getTransformation(this.dataTracker));
      view.put("billboard", DisplayEntity.BillboardMode.CODEC, this.getBillboardMode());
      view.putInt("interpolation_duration", this.getInterpolationDuration());
      view.putInt("teleport_duration", this.getTeleportDuration());
      view.putFloat("view_range", this.getViewRange());
      view.putFloat("shadow_radius", this.getShadowRadius());
      view.putFloat("shadow_strength", this.getShadowStrength());
      view.putFloat("width", this.getDisplayWidth());
      view.putFloat("height", this.getDisplayHeight());
      view.putInt("glow_color_override", this.getGlowColorOverride());
      view.putNullable("brightness", Brightness.CODEC, this.getBrightnessUnpacked());
   }

   public Box getVisibilityBoundingBox() {
      return this.visibilityBoundingBox;
   }

   public boolean shouldRender() {
      return !this.tooSmallToRender;
   }

   @Override
   public PistonBehavior getPistonBehavior() {
      return PistonBehavior.IGNORE;
   }

   @Override
   public boolean canAvoidTraps() {
      return true;
   }

   public DisplayEntity.@Nullable RenderState getRenderState() {
      return this.renderProperties;
   }

   public final void setInterpolationDuration(int interpolationDuration) {
      this.dataTracker.set(INTERPOLATION_DURATION, interpolationDuration);
   }

   public final int getInterpolationDuration() {
      return this.dataTracker.get(INTERPOLATION_DURATION);
   }

   public final void setStartInterpolation(int startInterpolation) {
      this.dataTracker.set(START_INTERPOLATION, startInterpolation, true);
   }

   public final int getStartInterpolation() {
      return this.dataTracker.get(START_INTERPOLATION);
   }

   public final void setTeleportDuration(int teleportDuration) {
      this.dataTracker.set(TELEPORT_DURATION, teleportDuration);
   }

   public final int getTeleportDuration() {
      return this.dataTracker.get(TELEPORT_DURATION);
   }

   public final void setBillboardMode(DisplayEntity.BillboardMode billboardMode) {
      this.dataTracker.set(BILLBOARD, billboardMode.getIndex());
   }

   public final DisplayEntity.BillboardMode getBillboardMode() {
      return DisplayEntity.BillboardMode.FROM_INDEX.apply(this.dataTracker.get(BILLBOARD));
   }

   public final void setBrightness(@Nullable Brightness brightness) {
      this.dataTracker.set(BRIGHTNESS, brightness != null ? brightness.pack() : -1);
   }

   public final @Nullable Brightness getBrightnessUnpacked() {
      int i = this.dataTracker.get(BRIGHTNESS);
      return i != -1 ? Brightness.unpack(i) : null;
   }

   public final int getBrightness() {
      return this.dataTracker.get(BRIGHTNESS);
   }

   public final void setViewRange(float viewRange) {
      this.dataTracker.set(VIEW_RANGE, viewRange);
   }

   public final float getViewRange() {
      return this.dataTracker.get(VIEW_RANGE);
   }

   public final void setShadowRadius(float shadowRadius) {
      this.dataTracker.set(SHADOW_RADIUS, shadowRadius);
   }

   public final float getShadowRadius() {
      return this.dataTracker.get(SHADOW_RADIUS);
   }

   public final void setShadowStrength(float shadowStrength) {
      this.dataTracker.set(SHADOW_STRENGTH, shadowStrength);
   }

   public final float getShadowStrength() {
      return this.dataTracker.get(SHADOW_STRENGTH);
   }

   public final void setDisplayWidth(float width) {
      this.dataTracker.set(WIDTH, width);
   }

   public final float getDisplayWidth() {
      return this.dataTracker.get(WIDTH);
   }

   public final void setDisplayHeight(float height) {
      this.dataTracker.set(HEIGHT, height);
   }

   public final int getGlowColorOverride() {
      return this.dataTracker.get(GLOW_COLOR_OVERRIDE);
   }

   public final void setGlowColorOverride(int glowColorOverride) {
      this.dataTracker.set(GLOW_COLOR_OVERRIDE, glowColorOverride);
   }

   public float getLerpProgress(float tickProgress) {
      int i = this.interpolationDuration;
      if (i <= 0) {
         return 1.0F;
      } else {
         float f = (float)(this.age - this.interpolationStart);
         float g = f + tickProgress;
         float h = MathHelper.clamp(MathHelper.getLerpProgress(g, 0.0F, (float)i), 0.0F, 1.0F);
         this.lerpProgress = h;
         return h;
      }
   }

   public final float getDisplayHeight() {
      return this.dataTracker.get(HEIGHT);
   }

   @Override
   public void setPosition(double x, double y, double z) {
      super.setPosition(x, y, z);
      this.updateVisibilityBoundingBox();
   }

   private void updateVisibilityBoundingBox() {
      float f = this.getDisplayWidth();
      float g = this.getDisplayHeight();
      this.tooSmallToRender = f == 0.0F || g == 0.0F;
      float h = f / 2.0F;
      double d = this.getX();
      double e = this.getY();
      double i = this.getZ();
      this.visibilityBoundingBox = new Box(d - h, e, i - h, d + h, e + g, i + h);
   }

   @Override
   public boolean shouldRender(double distance) {
      return distance < MathHelper.square(this.getViewRange() * 64.0 * getRenderDistanceMultiplier());
   }

   @Override
   public int getTeamColorValue() {
      int i = this.getGlowColorOverride();
      return i != -1 ? i : super.getTeamColorValue();
   }

   private DisplayEntity.RenderState copyRenderState() {
      return new DisplayEntity.RenderState(
         DisplayEntity.AbstractInterpolator.constant(getTransformation(this.dataTracker)),
         this.getBillboardMode(),
         this.getBrightness(),
         DisplayEntity.FloatLerper.constant(this.getShadowRadius()),
         DisplayEntity.FloatLerper.constant(this.getShadowStrength()),
         this.getGlowColorOverride()
      );
   }

   private DisplayEntity.RenderState getLerpedRenderState(DisplayEntity.RenderState state, float lerpProgress) {
      AffineTransformation affineTransformation = state.transformation.interpolate(lerpProgress);
      float f = state.shadowRadius.lerp(lerpProgress);
      float g = state.shadowStrength.lerp(lerpProgress);
      return new DisplayEntity.RenderState(
         new DisplayEntity.AffineTransformationInterpolator(affineTransformation, getTransformation(this.dataTracker)),
         this.getBillboardMode(),
         this.getBrightness(),
         new DisplayEntity.FloatLerperImpl(f, this.getShadowRadius()),
         new DisplayEntity.FloatLerperImpl(g, this.getShadowStrength()),
         this.getGlowColorOverride()
      );
   }

   @FunctionalInterface
   public interface AbstractInterpolator<T> {
      static <T> DisplayEntity.AbstractInterpolator<T> constant(T value) {
         return delta -> value;
      }

      T interpolate(float delta);
   }

   record AffineTransformationInterpolator(AffineTransformation previous, AffineTransformation current)
      implements DisplayEntity.AbstractInterpolator<AffineTransformation> {
      public AffineTransformation interpolate(float f) {
         return f >= 1.0 ? this.current : this.previous.interpolate(this.current, f);
      }
   }

   record ArgbLerper(int previous, int current) implements DisplayEntity.IntLerper {
      @Override
      public int lerp(float delta) {
         return ColorHelper.lerp(delta, this.previous, this.current);
      }
   }

   public static enum BillboardMode implements StringIdentifiable {
      FIXED((byte)0, "fixed"),
      VERTICAL((byte)1, "vertical"),
      HORIZONTAL((byte)2, "horizontal"),
      CENTER((byte)3, "center");

      public static final Codec<DisplayEntity.BillboardMode> CODEC = StringIdentifiable.createCodec(DisplayEntity.BillboardMode::values);
      public static final IntFunction<DisplayEntity.BillboardMode> FROM_INDEX = ValueLists.createIndexToValueFunction(
         DisplayEntity.BillboardMode::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
      );
      private final byte index;
      private final String name;

      private BillboardMode(final byte index, final String name) {
         this.name = name;
         this.index = index;
      }

      @Override
      public String asString() {
         return this.name;
      }

      byte getIndex() {
         return this.index;
      }
   }

   public static class BlockDisplayEntity extends DisplayEntity {
      public static final String BLOCK_STATE_NBT_KEY = "block_state";
      private static final TrackedData<BlockState> BLOCK_STATE = DataTracker.registerData(
         DisplayEntity.BlockDisplayEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE
      );
      private DisplayEntity.BlockDisplayEntity.@Nullable Data data;

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
            this.renderingDataSet = true;
         }
      }

      public final BlockState getBlockState() {
         return this.dataTracker.get(BLOCK_STATE);
      }

      public final void setBlockState(BlockState state) {
         this.dataTracker.set(BLOCK_STATE, state);
      }

      @Override
      protected void readCustomData(ReadView view) {
         super.readCustomData(view);
         this.setBlockState(view.<BlockState>read("block_state", BlockState.CODEC).orElse(Blocks.AIR.getDefaultState()));
      }

      @Override
      protected void writeCustomData(WriteView view) {
         super.writeCustomData(view);
         view.put("block_state", BlockState.CODEC, this.getBlockState());
      }

      public DisplayEntity.BlockDisplayEntity.@Nullable Data getData() {
         return this.data;
      }

      @Override
      protected void refreshData(boolean shouldLerp, float lerpProgress) {
         this.data = new DisplayEntity.BlockDisplayEntity.Data(this.getBlockState());
      }

      public record Data(BlockState blockState) {
      }
   }

   @FunctionalInterface
   public interface FloatLerper {
      static DisplayEntity.FloatLerper constant(float value) {
         return delta -> value;
      }

      float lerp(float delta);
   }

   record FloatLerperImpl(float previous, float current) implements DisplayEntity.FloatLerper {
      @Override
      public float lerp(float delta) {
         return MathHelper.lerp(delta, this.previous, this.current);
      }
   }

   @FunctionalInterface
   public interface IntLerper {
      static DisplayEntity.IntLerper constant(int value) {
         return delta -> value;
      }

      int lerp(float delta);
   }

   record IntLerperImpl(int previous, int current) implements DisplayEntity.IntLerper {
      @Override
      public int lerp(float delta) {
         return MathHelper.lerp(delta, this.previous, this.current);
      }
   }

   public static class ItemDisplayEntity extends DisplayEntity {
      private static final String ITEM_NBT_KEY = "item";
      private static final String ITEM_DISPLAY_NBT_KEY = "item_display";
      private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(DisplayEntity.ItemDisplayEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
      private static final TrackedData<Byte> ITEM_DISPLAY = DataTracker.registerData(DisplayEntity.ItemDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
      private final StackReference stackReference = StackReference.of(this::getItemStack, this::setItemStack);
      private DisplayEntity.ItemDisplayEntity.@Nullable Data data;

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
            this.renderingDataSet = true;
         }
      }

      public final ItemStack getItemStack() {
         return this.dataTracker.get(ITEM);
      }

      public final void setItemStack(ItemStack stack) {
         this.dataTracker.set(ITEM, stack);
      }

      public final void setItemDisplayContext(ItemDisplayContext context) {
         this.dataTracker.set(ITEM_DISPLAY, context.getIndex());
      }

      public final ItemDisplayContext getItemDisplayContext() {
         return ItemDisplayContext.FROM_INDEX.apply(this.dataTracker.get(ITEM_DISPLAY));
      }

      @Override
      protected void readCustomData(ReadView view) {
         super.readCustomData(view);
         this.setItemStack(view.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
         this.setItemDisplayContext(view.<ItemDisplayContext>read("item_display", ItemDisplayContext.CODEC).orElse(ItemDisplayContext.NONE));
      }

      @Override
      protected void writeCustomData(WriteView view) {
         super.writeCustomData(view);
         ItemStack itemStack = this.getItemStack();
         if (!itemStack.isEmpty()) {
            view.put("item", ItemStack.CODEC, itemStack);
         }

         view.put("item_display", ItemDisplayContext.CODEC, this.getItemDisplayContext());
      }

      @Override
      public @Nullable StackReference getStackReference(int slot) {
         return slot == 0 ? this.stackReference : null;
      }

      public DisplayEntity.ItemDisplayEntity.@Nullable Data getData() {
         return this.data;
      }

      @Override
      protected void refreshData(boolean shouldLerp, float lerpProgress) {
         ItemStack itemStack = this.getItemStack();
         itemStack.setHolder(this);
         this.data = new DisplayEntity.ItemDisplayEntity.Data(itemStack, this.getItemDisplayContext());
      }

      public record Data(ItemStack itemStack, ItemDisplayContext itemTransform) {
      }
   }

   public record RenderState(
      DisplayEntity.AbstractInterpolator<AffineTransformation> transformation,
      DisplayEntity.BillboardMode billboardConstraints,
      int brightnessOverride,
      DisplayEntity.FloatLerper shadowRadius,
      DisplayEntity.FloatLerper shadowStrength,
      int glowColorOverride
   ) {
   }

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
      private static final TrackedData<Text> TEXT = DataTracker.registerData(DisplayEntity.TextDisplayEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT);
      private static final TrackedData<Integer> LINE_WIDTH = DataTracker.registerData(DisplayEntity.TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
      private static final TrackedData<Integer> BACKGROUND = DataTracker.registerData(DisplayEntity.TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
      private static final TrackedData<Byte> TEXT_OPACITY = DataTracker.registerData(DisplayEntity.TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
      private static final TrackedData<Byte> TEXT_DISPLAY_FLAGS = DataTracker.registerData(
         DisplayEntity.TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE
      );
      private static final IntSet TEXT_RENDERING_DATA_IDS = IntSet.of(
         new int[]{TEXT.id(), LINE_WIDTH.id(), BACKGROUND.id(), TEXT_OPACITY.id(), TEXT_DISPLAY_FLAGS.id()}
      );
      private DisplayEntity.TextDisplayEntity.@Nullable TextLines textLines;
      private DisplayEntity.TextDisplayEntity.@Nullable Data data;

      public TextDisplayEntity(EntityType<?> entityType, World world) {
         super(entityType, world);
      }

      @Override
      protected void initDataTracker(DataTracker.Builder builder) {
         super.initDataTracker(builder);
         builder.add(TEXT, Text.empty());
         builder.add(LINE_WIDTH, 200);
         builder.add(BACKGROUND, 1073741824);
         builder.add(TEXT_OPACITY, (byte)-1);
         builder.add(TEXT_DISPLAY_FLAGS, (byte)0);
      }

      @Override
      public void onTrackedDataSet(TrackedData<?> data) {
         super.onTrackedDataSet(data);
         if (TEXT_RENDERING_DATA_IDS.contains(data.id())) {
            this.renderingDataSet = true;
         }
      }

      public final Text getText() {
         return this.dataTracker.get(TEXT);
      }

      public final void setText(Text text) {
         this.dataTracker.set(TEXT, text);
      }

      public final int getLineWidth() {
         return this.dataTracker.get(LINE_WIDTH);
      }

      public final void setLineWidth(int lineWidth) {
         this.dataTracker.set(LINE_WIDTH, lineWidth);
      }

      public final byte getTextOpacity() {
         return this.dataTracker.get(TEXT_OPACITY);
      }

      public final void setTextOpacity(byte textOpacity) {
         this.dataTracker.set(TEXT_OPACITY, textOpacity);
      }

      public final int getBackground() {
         return this.dataTracker.get(BACKGROUND);
      }

      public final void setBackground(int background) {
         this.dataTracker.set(BACKGROUND, background);
      }

      public final byte getDisplayFlags() {
         return this.dataTracker.get(TEXT_DISPLAY_FLAGS);
      }

      public final void setDisplayFlags(byte flags) {
         this.dataTracker.set(TEXT_DISPLAY_FLAGS, flags);
      }

      private static byte readFlag(byte flags, ReadView view, String nbtKey, byte flag) {
         return view.getBoolean(nbtKey, false) ? (byte)(flags | flag) : flags;
      }

      @Override
      protected void readCustomData(ReadView view) {
         super.readCustomData(view);
         this.setLineWidth(view.getInt("line_width", 200));
         this.setTextOpacity(view.getByte("text_opacity", (byte)-1));
         this.setBackground(view.getInt("background", 1073741824));
         byte b = readFlag((byte)0, view, "shadow", (byte)1);
         b = readFlag(b, view, "see_through", (byte)2);
         b = readFlag(b, view, "default_background", (byte)4);
         Optional<DisplayEntity.TextDisplayEntity.TextAlignment> optional = view.read("alignment", DisplayEntity.TextDisplayEntity.TextAlignment.CODEC);
         if (optional.isPresent()) {
            b = switch ((DisplayEntity.TextDisplayEntity.TextAlignment)optional.get()) {
               case CENTER -> b;
               case LEFT -> (byte)(b | 8);
               case RIGHT -> (byte)(b | 16);
            };
         }

         this.setDisplayFlags(b);
         Optional<Text> optional2 = view.read("text", TextCodecs.CODEC);
         if (optional2.isPresent()) {
            try {
               if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
                  ServerCommandSource serverCommandSource = this.getCommandSource(serverWorld).withPermissions(LeveledPermissionPredicate.GAMEMASTERS);
                  Text text = Texts.parse(serverCommandSource, optional2.get(), this, 0);
                  this.setText(text);
               } else {
                  this.setText(Text.empty());
               }
            } catch (Exception var8) {
               DisplayEntity.LOGGER.warn("Failed to parse display entity text {}", optional2, var8);
            }
         }
      }

      private static void writeFlag(byte flags, WriteView view, String nbtKey, byte flag) {
         view.putBoolean(nbtKey, (flags & flag) != 0);
      }

      @Override
      protected void writeCustomData(WriteView view) {
         super.writeCustomData(view);
         view.put("text", TextCodecs.CODEC, this.getText());
         view.putInt("line_width", this.getLineWidth());
         view.putInt("background", this.getBackground());
         view.putByte("text_opacity", this.getTextOpacity());
         byte b = this.getDisplayFlags();
         writeFlag(b, view, "shadow", (byte)1);
         writeFlag(b, view, "see_through", (byte)2);
         writeFlag(b, view, "default_background", (byte)4);
         view.put("alignment", DisplayEntity.TextDisplayEntity.TextAlignment.CODEC, getAlignment(b));
      }

      @Override
      protected void refreshData(boolean shouldLerp, float lerpProgress) {
         if (shouldLerp && this.data != null) {
            this.data = this.getLerpedRenderState(this.data, lerpProgress);
         } else {
            this.data = this.copyData();
         }

         this.textLines = null;
      }

      public DisplayEntity.TextDisplayEntity.@Nullable Data getData() {
         return this.data;
      }

      private DisplayEntity.TextDisplayEntity.Data copyData() {
         return new DisplayEntity.TextDisplayEntity.Data(
            this.getText(),
            this.getLineWidth(),
            DisplayEntity.IntLerper.constant(this.getTextOpacity()),
            DisplayEntity.IntLerper.constant(this.getBackground()),
            this.getDisplayFlags()
         );
      }

      private DisplayEntity.TextDisplayEntity.Data getLerpedRenderState(DisplayEntity.TextDisplayEntity.Data data, float lerpProgress) {
         int i = data.backgroundColor.lerp(lerpProgress);
         int j = data.textOpacity.lerp(lerpProgress);
         return new DisplayEntity.TextDisplayEntity.Data(
            this.getText(),
            this.getLineWidth(),
            new DisplayEntity.IntLerperImpl(j, this.getTextOpacity()),
            new DisplayEntity.ArgbLerper(i, this.getBackground()),
            this.getDisplayFlags()
         );
      }

      public DisplayEntity.TextDisplayEntity.TextLines splitLines(DisplayEntity.TextDisplayEntity.LineSplitter splitter) {
         if (this.textLines == null) {
            if (this.data != null) {
               this.textLines = splitter.split(this.data.text(), this.data.lineWidth());
            } else {
               this.textLines = new DisplayEntity.TextDisplayEntity.TextLines(List.of(), 0);
            }
         }

         return this.textLines;
      }

      public static DisplayEntity.TextDisplayEntity.TextAlignment getAlignment(byte flags) {
         if ((flags & 8) != 0) {
            return DisplayEntity.TextDisplayEntity.TextAlignment.LEFT;
         } else {
            return (flags & 16) != 0 ? DisplayEntity.TextDisplayEntity.TextAlignment.RIGHT : DisplayEntity.TextDisplayEntity.TextAlignment.CENTER;
         }
      }

      public record Data(Text text, int lineWidth, DisplayEntity.IntLerper textOpacity, DisplayEntity.IntLerper backgroundColor, byte flags) {
      }

      @FunctionalInterface
      public interface LineSplitter {
         DisplayEntity.TextDisplayEntity.TextLines split(Text text, int lineWidth);
      }

      public static enum TextAlignment implements StringIdentifiable {
         CENTER("center"),
         LEFT("left"),
         RIGHT("right");

         public static final Codec<DisplayEntity.TextDisplayEntity.TextAlignment> CODEC = StringIdentifiable.createCodec(
            DisplayEntity.TextDisplayEntity.TextAlignment::values
         );
         private final String name;

         private TextAlignment(final String name) {
            this.name = name;
         }

         @Override
         public String asString() {
            return this.name;
         }
      }

      public record TextLine(OrderedText contents, int width) {
      }

      public record TextLines(List<DisplayEntity.TextDisplayEntity.TextLine> lines, int width) {
      }
   }
}

package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

public class TropicalFishEntity extends SchoolingFishEntity {
   public static final TropicalFishEntity.Variant DEFAULT_VARIANT = new TropicalFishEntity.Variant(
      TropicalFishEntity.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE
   );
   private static final TrackedData<Integer> VARIANT = DataTracker.registerData(TropicalFishEntity.class, TrackedDataHandlerRegistry.INTEGER);
   public static final List<TropicalFishEntity.Variant> COMMON_VARIANTS = List.of(
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.KOB, DyeColor.RED, DyeColor.WHITE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW),
      new TropicalFishEntity.Variant(TropicalFishEntity.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)
   );
   private boolean commonSpawn = true;

   public TropicalFishEntity(EntityType<? extends TropicalFishEntity> entityType, World world) {
      super(entityType, world);
   }

   public static String getToolTipForVariant(int variant) {
      return "entity.minecraft.tropical_fish.predefined." + variant;
   }

   static int getVariantId(TropicalFishEntity.Pattern variety, DyeColor baseColor, DyeColor patternColor) {
      return variety.getIndex() & 65535 | (baseColor.getIndex() & 0xFF) << 16 | (patternColor.getIndex() & 0xFF) << 24;
   }

   public static DyeColor getBaseColor(int variant) {
      return DyeColor.byIndex(variant >> 16 & 0xFF);
   }

   public static DyeColor getPatternColor(int variant) {
      return DyeColor.byIndex(variant >> 24 & 0xFF);
   }

   public static TropicalFishEntity.Pattern getVariety(int variant) {
      return TropicalFishEntity.Pattern.byIndex(variant & 65535);
   }

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(VARIANT, DEFAULT_VARIANT.getId());
   }

   @Override
   protected void writeCustomData(WriteView view) {
      super.writeCustomData(view);
      view.put("Variant", TropicalFishEntity.Variant.CODEC, new TropicalFishEntity.Variant(this.getTropicalFishVariant()));
   }

   @Override
   protected void readCustomData(ReadView view) {
      super.readCustomData(view);
      TropicalFishEntity.Variant variant = view.<TropicalFishEntity.Variant>read("Variant", TropicalFishEntity.Variant.CODEC).orElse(DEFAULT_VARIANT);
      this.setTropicalFishVariant(variant.getId());
   }

   private void setTropicalFishVariant(int variant) {
      this.dataTracker.set(VARIANT, variant);
   }

   @Override
   public boolean spawnsTooManyForEachTry(int count) {
      return !this.commonSpawn;
   }

   private int getTropicalFishVariant() {
      return this.dataTracker.get(VARIANT);
   }

   public DyeColor getBaseColor() {
      return getBaseColor(this.getTropicalFishVariant());
   }

   public DyeColor getPatternColor() {
      return getPatternColor(this.getTropicalFishVariant());
   }

   public TropicalFishEntity.Pattern getVariety() {
      return getVariety(this.getTropicalFishVariant());
   }

   private void setVariety(TropicalFishEntity.Pattern variety) {
      int i = this.getTropicalFishVariant();
      DyeColor dyeColor = getBaseColor(i);
      DyeColor dyeColor2 = getPatternColor(i);
      this.setTropicalFishVariant(getVariantId(variety, dyeColor, dyeColor2));
   }

   private void setBaseColor(DyeColor baseColor) {
      int i = this.getTropicalFishVariant();
      TropicalFishEntity.Pattern pattern = getVariety(i);
      DyeColor dyeColor = getPatternColor(i);
      this.setTropicalFishVariant(getVariantId(pattern, baseColor, dyeColor));
   }

   private void setPatternColor(DyeColor patternColor) {
      int i = this.getTropicalFishVariant();
      TropicalFishEntity.Pattern pattern = getVariety(i);
      DyeColor dyeColor = getBaseColor(i);
      this.setTropicalFishVariant(getVariantId(pattern, dyeColor, patternColor));
   }

   @Override
   public <T> @Nullable T get(ComponentType<? extends T> type) {
      if (type == DataComponentTypes.TROPICAL_FISH_PATTERN) {
         return castComponentValue((ComponentType<T>)type, this.getVariety());
      } else if (type == DataComponentTypes.TROPICAL_FISH_BASE_COLOR) {
         return castComponentValue((ComponentType<T>)type, this.getBaseColor());
      } else {
         return type == DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR ? castComponentValue((ComponentType<T>)type, this.getPatternColor()) : super.get(type);
      }
   }

   @Override
   protected void copyComponentsFrom(ComponentsAccess from) {
      this.copyComponentFrom(from, DataComponentTypes.TROPICAL_FISH_PATTERN);
      this.copyComponentFrom(from, DataComponentTypes.TROPICAL_FISH_BASE_COLOR);
      this.copyComponentFrom(from, DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR);
      super.copyComponentsFrom(from);
   }

   @Override
   protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
      if (type == DataComponentTypes.TROPICAL_FISH_PATTERN) {
         this.setVariety(castComponentValue(DataComponentTypes.TROPICAL_FISH_PATTERN, value));
         return true;
      } else if (type == DataComponentTypes.TROPICAL_FISH_BASE_COLOR) {
         this.setBaseColor(castComponentValue(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, value));
         return true;
      } else if (type == DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR) {
         this.setPatternColor(castComponentValue(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, value));
         return true;
      } else {
         return super.setApplicableComponent(type, value);
      }
   }

   @Override
   public void copyDataToStack(ItemStack stack) {
      super.copyDataToStack(stack);
      stack.copy(DataComponentTypes.TROPICAL_FISH_PATTERN, this);
      stack.copy(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, this);
      stack.copy(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, this);
   }

   @Override
   public ItemStack getBucketItem() {
      return new ItemStack(Items.TROPICAL_FISH_BUCKET);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_TROPICAL_FISH_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_TROPICAL_FISH_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_TROPICAL_FISH_HURT;
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.ENTITY_TROPICAL_FISH_FLOP;
   }

   @Override
   public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
      entityData = super.initialize(world, difficulty, spawnReason, entityData);
      Random random = world.getRandom();
      TropicalFishEntity.Variant variant;
      if (entityData instanceof TropicalFishEntity.TropicalFishData tropicalFishData) {
         variant = tropicalFishData.variant;
      } else if (random.nextFloat() < 0.9) {
         variant = Util.getRandom(COMMON_VARIANTS, random);
         entityData = new TropicalFishEntity.TropicalFishData(this, variant);
      } else {
         this.commonSpawn = false;
         TropicalFishEntity.Pattern[] patterns = TropicalFishEntity.Pattern.values();
         DyeColor[] dyeColors = DyeColor.values();
         TropicalFishEntity.Pattern pattern = Util.getRandom(patterns, random);
         DyeColor dyeColor = Util.getRandom(dyeColors, random);
         DyeColor dyeColor2 = Util.getRandom(dyeColors, random);
         variant = new TropicalFishEntity.Variant(pattern, dyeColor, dyeColor2);
      }

      this.setTropicalFishVariant(variant.getId());
      return entityData;
   }

   public static boolean canTropicalFishSpawn(EntityType<TropicalFishEntity> type, WorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
      return world.getFluidState(pos.down()).isIn(FluidTags.WATER)
         && world.getBlockState(pos.up()).isOf(Blocks.WATER)
         && (world.getBiome(pos).isIn(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT) || WaterCreatureEntity.canSpawn(type, world, reason, pos, random));
   }

   public static enum Pattern implements StringIdentifiable, TooltipAppender {
      KOB("kob", TropicalFishEntity.Size.SMALL, 0),
      SUNSTREAK("sunstreak", TropicalFishEntity.Size.SMALL, 1),
      SNOOPER("snooper", TropicalFishEntity.Size.SMALL, 2),
      DASHER("dasher", TropicalFishEntity.Size.SMALL, 3),
      BRINELY("brinely", TropicalFishEntity.Size.SMALL, 4),
      SPOTTY("spotty", TropicalFishEntity.Size.SMALL, 5),
      FLOPPER("flopper", TropicalFishEntity.Size.LARGE, 0),
      STRIPEY("stripey", TropicalFishEntity.Size.LARGE, 1),
      GLITTER("glitter", TropicalFishEntity.Size.LARGE, 2),
      BLOCKFISH("blockfish", TropicalFishEntity.Size.LARGE, 3),
      BETTY("betty", TropicalFishEntity.Size.LARGE, 4),
      CLAYFISH("clayfish", TropicalFishEntity.Size.LARGE, 5);

      public static final Codec<TropicalFishEntity.Pattern> CODEC = StringIdentifiable.createCodec(TropicalFishEntity.Pattern::values);
      private static final IntFunction<TropicalFishEntity.Pattern> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
         TropicalFishEntity.Pattern::getIndex, values(), KOB
      );
      public static final PacketCodec<ByteBuf, TropicalFishEntity.Pattern> PACKET_CODEC = PacketCodecs.indexed(
         INDEX_MAPPER, TropicalFishEntity.Pattern::getIndex
      );
      private final String id;
      private final Text text;
      private final TropicalFishEntity.Size size;
      private final int index;

      private Pattern(final String id, final TropicalFishEntity.Size size, final int index) {
         this.id = id;
         this.size = size;
         this.index = size.index | index << 8;
         this.text = Text.translatable("entity.minecraft.tropical_fish.type." + this.id);
      }

      public static TropicalFishEntity.Pattern byIndex(int index) {
         return INDEX_MAPPER.apply(index);
      }

      public TropicalFishEntity.Size getSize() {
         return this.size;
      }

      public int getIndex() {
         return this.index;
      }

      @Override
      public String asString() {
         return this.id;
      }

      public Text getText() {
         return this.text;
      }

      @Override
      public void appendTooltip(Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components) {
         DyeColor dyeColor = components.getOrDefault(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, TropicalFishEntity.DEFAULT_VARIANT.baseColor());
         DyeColor dyeColor2 = components.getOrDefault(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, TropicalFishEntity.DEFAULT_VARIANT.patternColor());
         Formatting[] formattings = new Formatting[]{Formatting.ITALIC, Formatting.GRAY};
         int i = TropicalFishEntity.COMMON_VARIANTS.indexOf(new TropicalFishEntity.Variant(this, dyeColor, dyeColor2));
         if (i != -1) {
            textConsumer.accept(Text.translatable(TropicalFishEntity.getToolTipForVariant(i)).formatted(formattings));
         } else {
            textConsumer.accept(this.text.copyContentOnly().formatted(formattings));
            MutableText mutableText = Text.translatable("color.minecraft." + dyeColor.getId());
            if (dyeColor != dyeColor2) {
               mutableText.append(", ").append(Text.translatable("color.minecraft." + dyeColor2.getId()));
            }

            mutableText.formatted(formattings);
            textConsumer.accept(mutableText);
         }
      }
   }

   public static enum Size {
      SMALL(0),
      LARGE(1);

      final int index;

      private Size(final int index) {
         this.index = index;
      }
   }

   static class TropicalFishData extends SchoolingFishEntity.FishData {
      final TropicalFishEntity.Variant variant;

      TropicalFishData(TropicalFishEntity leader, TropicalFishEntity.Variant variant) {
         super(leader);
         this.variant = variant;
      }
   }

   public record Variant(TropicalFishEntity.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
      public static final Codec<TropicalFishEntity.Variant> CODEC = Codec.INT.xmap(TropicalFishEntity.Variant::new, TropicalFishEntity.Variant::getId);

      public Variant(int id) {
         this(TropicalFishEntity.getVariety(id), TropicalFishEntity.getBaseColor(id), TropicalFishEntity.getPatternColor(id));
      }

      public int getId() {
         return TropicalFishEntity.getVariantId(this.pattern, this.baseColor, this.patternColor);
      }
   }
}

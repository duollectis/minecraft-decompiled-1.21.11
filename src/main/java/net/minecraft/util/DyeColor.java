package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.minecraft.block.MapColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum DyeColor implements StringIdentifiable {
   WHITE(0, "white", 16383998, MapColor.WHITE, 15790320, 16777215),
   ORANGE(1, "orange", 16351261, MapColor.ORANGE, 15435844, 16738335),
   MAGENTA(2, "magenta", 13061821, MapColor.MAGENTA, 12801229, 16711935),
   LIGHT_BLUE(3, "light_blue", 3847130, MapColor.LIGHT_BLUE, 6719955, 10141901),
   YELLOW(4, "yellow", 16701501, MapColor.YELLOW, 14602026, 16776960),
   LIME(5, "lime", 8439583, MapColor.LIME, 4312372, 12582656),
   PINK(6, "pink", 15961002, MapColor.PINK, 14188952, 16738740),
   GRAY(7, "gray", 4673362, MapColor.GRAY, 4408131, 8421504),
   LIGHT_GRAY(8, "light_gray", 10329495, MapColor.LIGHT_GRAY, 11250603, 13882323),
   CYAN(9, "cyan", 1481884, MapColor.CYAN, 2651799, 65535),
   PURPLE(10, "purple", 8991416, MapColor.PURPLE, 8073150, 10494192),
   BLUE(11, "blue", 3949738, MapColor.BLUE, 2437522, 255),
   BROWN(12, "brown", 8606770, MapColor.BROWN, 5320730, 9127187),
   GREEN(13, "green", 6192150, MapColor.GREEN, 3887386, 65280),
   RED(14, "red", 11546150, MapColor.RED, 11743532, 16711680),
   BLACK(15, "black", 1908001, MapColor.BLACK, 1973019, 0);

   private static final IntFunction<DyeColor> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
      DyeColor::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
   );
   private static final Int2ObjectOpenHashMap<DyeColor> BY_FIREWORK_COLOR = new Int2ObjectOpenHashMap(
      Arrays.stream(values()).collect(Collectors.toMap(color -> color.fireworkColor, color -> (DyeColor)color))
   );
   public static final StringIdentifiable.EnumCodec<DyeColor> CODEC = StringIdentifiable.createCodec(DyeColor::values);
   public static final PacketCodec<ByteBuf, DyeColor> PACKET_CODEC = PacketCodecs.indexed(INDEX_MAPPER, DyeColor::getIndex);
   @Deprecated
   public static final Codec<DyeColor> INDEX_CODEC = Codec.BYTE.xmap(DyeColor::byIndex, color -> (byte)color.index);
   private final int index;
   private final String id;
   private final MapColor mapColor;
   private final int entityColor;
   private final int fireworkColor;
   private final int signColor;

   private DyeColor(final int index, final String id, final int entityColor, final MapColor mapColor, final int fireworkColor, final int signColor) {
      this.index = index;
      this.id = id;
      this.mapColor = mapColor;
      this.signColor = ColorHelper.fullAlpha(signColor);
      this.entityColor = ColorHelper.fullAlpha(entityColor);
      this.fireworkColor = fireworkColor;
   }

   public int getIndex() {
      return this.index;
   }

   public String getId() {
      return this.id;
   }

   public int getEntityColor() {
      return this.entityColor;
   }

   public MapColor getMapColor() {
      return this.mapColor;
   }

   public int getFireworkColor() {
      return this.fireworkColor;
   }

   public int getSignColor() {
      return this.signColor;
   }

   public static DyeColor byIndex(int index) {
      return INDEX_MAPPER.apply(index);
   }

   @Contract("_,!null->!null;_,null->_")
   public static @Nullable DyeColor byId(String id, @Nullable DyeColor fallback) {
      DyeColor dyeColor = CODEC.byId(id);
      return dyeColor != null ? dyeColor : fallback;
   }

   public static @Nullable DyeColor byFireworkColor(int color) {
      return (DyeColor)BY_FIREWORK_COLOR.get(color);
   }

   @Override
   public String toString() {
      return this.id;
   }

   @Override
   public String asString() {
      return this.id;
   }

   public static DyeColor mixColors(ServerWorld world, DyeColor first, DyeColor second) {
      CraftingRecipeInput craftingRecipeInput = createColorMixingRecipeInput(first, second);
      return world.getRecipeManager()
         .getFirstMatch(RecipeType.CRAFTING, craftingRecipeInput, world)
         .map(recipe -> recipe.value().craft(craftingRecipeInput, world.getRegistryManager()))
         .map(ItemStack::getItem)
         .filter(DyeItem.class::isInstance)
         .map(DyeItem.class::cast)
         .map(DyeItem::getColor)
         .orElseGet(() -> world.random.nextBoolean() ? first : second);
   }

   private static CraftingRecipeInput createColorMixingRecipeInput(DyeColor first, DyeColor second) {
      return CraftingRecipeInput.create(2, 1, List.of(new ItemStack(DyeItem.byColor(first)), new ItemStack(DyeItem.byColor(second))));
   }
}

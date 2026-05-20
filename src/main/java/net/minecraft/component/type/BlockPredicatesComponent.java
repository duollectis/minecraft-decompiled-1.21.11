package net.minecraft.component.type;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BlockPredicatesComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<BlockPredicatesComponent> CODEC = Codecs.listOrSingle(BlockPredicate.CODEC, Codecs.nonEmptyList(BlockPredicate.CODEC.listOf()))
      .xmap(BlockPredicatesComponent::new, checker -> checker.predicates);
   public static final PacketCodec<RegistryByteBuf, BlockPredicatesComponent> PACKET_CODEC = PacketCodec.tuple(
      BlockPredicate.PACKET_CODEC.collect(PacketCodecs.toList()), blockPredicatesChecker -> blockPredicatesChecker.predicates, BlockPredicatesComponent::new
   );
   public static final Text CAN_BREAK_TEXT = Text.translatable("item.canBreak").formatted(Formatting.GRAY);
   public static final Text CAN_PLACE_TEXT = Text.translatable("item.canPlace").formatted(Formatting.GRAY);
   private static final Text CAN_USE_UNKNOWN_TEXT = Text.translatable("item.canUse.unknown").formatted(Formatting.GRAY);
   private final List<BlockPredicate> predicates;
   private @Nullable List<Text> tooltipText;
   private @Nullable CachedBlockPosition cachedPos;
   private boolean lastResult;
   private boolean nbtAware;

   public BlockPredicatesComponent(List<BlockPredicate> predicates) {
      this.predicates = predicates;
   }

   private static boolean canUseCache(CachedBlockPosition pos, @Nullable CachedBlockPosition cachedPos, boolean nbtAware) {
      if (cachedPos == null || pos.getBlockState() != cachedPos.getBlockState()) {
         return false;
      } else if (!nbtAware) {
         return true;
      } else if (pos.getBlockEntity() == null && cachedPos.getBlockEntity() == null) {
         return true;
      } else if (pos.getBlockEntity() != null && cachedPos.getBlockEntity() != null) {
         boolean var7;
         try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
            DynamicRegistryManager dynamicRegistryManager = pos.getWorld().getRegistryManager();
            NbtCompound nbtCompound = getNbt(pos.getBlockEntity(), dynamicRegistryManager, logging);
            NbtCompound nbtCompound2 = getNbt(cachedPos.getBlockEntity(), dynamicRegistryManager, logging);
            var7 = Objects.equals(nbtCompound, nbtCompound2);
         }

         return var7;
      } else {
         return false;
      }
   }

   private static NbtCompound getNbt(BlockEntity blockEntity, DynamicRegistryManager registries, ErrorReporter errorReporter) {
      NbtWriteView nbtWriteView = NbtWriteView.create(errorReporter.makeChild(blockEntity.getReporterContext()), registries);
      blockEntity.writeDataWithId(nbtWriteView);
      return nbtWriteView.getNbt();
   }

   public boolean check(CachedBlockPosition cachedPos) {
      if (canUseCache(cachedPos, this.cachedPos, this.nbtAware)) {
         return this.lastResult;
      } else {
         this.cachedPos = cachedPos;
         this.nbtAware = false;

         for (BlockPredicate blockPredicate : this.predicates) {
            if (blockPredicate.test(cachedPos)) {
               this.nbtAware = this.nbtAware | blockPredicate.hasNbt();
               this.lastResult = true;
               return true;
            }
         }

         this.lastResult = false;
         return false;
      }
   }

   private List<Text> getOrCreateTooltipText() {
      if (this.tooltipText == null) {
         this.tooltipText = createTooltipText(this.predicates);
      }

      return this.tooltipText;
   }

   public void addTooltips(Consumer<Text> adder) {
      this.getOrCreateTooltipText().forEach(adder);
   }

   private static List<Text> createTooltipText(List<BlockPredicate> blockPredicates) {
      for (BlockPredicate blockPredicate : blockPredicates) {
         if (blockPredicate.blocks().isEmpty()) {
            return List.of(CAN_USE_UNKNOWN_TEXT);
         }
      }

      return blockPredicates.stream()
         .flatMap(predicate -> predicate.blocks().orElseThrow().stream())
         .distinct()
         .map(block -> (Text) block.value().getName().formatted(Formatting.DARK_GRAY))
         .toList();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof BlockPredicatesComponent blockPredicatesComponent ? this.predicates.equals(blockPredicatesComponent.predicates) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.predicates.hashCode();
   }

   @Override
   public String toString() {
      return "AdventureModePredicate{predicates=" + this.predicates + "}";
   }
}

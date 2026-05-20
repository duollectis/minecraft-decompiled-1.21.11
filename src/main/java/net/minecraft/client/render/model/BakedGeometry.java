package net.minecraft.client.render.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BakedGeometry {
   public static final BakedGeometry EMPTY = new BakedGeometry(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
   private final List<BakedQuad> allQuads;
   private final List<BakedQuad> sidelessQuads;
   private final List<BakedQuad> northQuads;
   private final List<BakedQuad> southQuads;
   private final List<BakedQuad> eastQuads;
   private final List<BakedQuad> westQuads;
   private final List<BakedQuad> upQuads;
   private final List<BakedQuad> downQuads;

   BakedGeometry(
      List<BakedQuad> allQuads,
      List<BakedQuad> sidelessQuads,
      List<BakedQuad> northQuads,
      List<BakedQuad> southQuads,
      List<BakedQuad> eastQuads,
      List<BakedQuad> westQuads,
      List<BakedQuad> upQuads,
      List<BakedQuad> downQuads
   ) {
      this.allQuads = allQuads;
      this.sidelessQuads = sidelessQuads;
      this.northQuads = northQuads;
      this.southQuads = southQuads;
      this.eastQuads = eastQuads;
      this.westQuads = westQuads;
      this.upQuads = upQuads;
      this.downQuads = downQuads;
   }

   public List<BakedQuad> getQuads(@Nullable Direction side) {
      return switch (side) {
         case null -> this.sidelessQuads;
         case NORTH -> this.northQuads;
         case SOUTH -> this.southQuads;
         case EAST -> this.eastQuads;
         case WEST -> this.westQuads;
         case UP -> this.upQuads;
         case DOWN -> this.downQuads;
      };
   }

   public List<BakedQuad> getAllQuads() {
      return this.allQuads;
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<BakedQuad> sidelessQuads = ImmutableList.builder();
      private final Multimap<Direction, BakedQuad> sidedQuads = ArrayListMultimap.create();

      public BakedGeometry.Builder add(Direction side, BakedQuad quad) {
         this.sidedQuads.put(side, quad);
         return this;
      }

      public BakedGeometry.Builder add(BakedQuad quad) {
         this.sidelessQuads.add(quad);
         return this;
      }

      private static BakedGeometry buildFromList(
         List<BakedQuad> quads, int sidelessCount, int northCount, int southCount, int eastCount, int westCount, int upCount, int downCount
      ) {
         int i = 0;
         int var16;
         List<BakedQuad> list = quads.subList(i, var16 = i + sidelessCount);
         List<BakedQuad> list2 = quads.subList(var16, i = var16 + northCount);
         int var18;
         List<BakedQuad> list3 = quads.subList(i, var18 = i + southCount);
         List<BakedQuad> list4 = quads.subList(var18, i = var18 + eastCount);
         int var20;
         List<BakedQuad> list5 = quads.subList(i, var20 = i + westCount);
         List<BakedQuad> list6 = quads.subList(var20, i = var20 + upCount);
         List<BakedQuad> list7 = quads.subList(i, i + downCount);
         return new BakedGeometry(quads, list, list2, list3, list4, list5, list6, list7);
      }

      public BakedGeometry build() {
         ImmutableList<BakedQuad> immutableList = this.sidelessQuads.build();
         if (this.sidedQuads.isEmpty()) {
            return immutableList.isEmpty()
               ? BakedGeometry.EMPTY
               : new BakedGeometry(immutableList, immutableList, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
         } else {
            com.google.common.collect.ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            builder.addAll(immutableList);
            Collection<BakedQuad> collection = this.sidedQuads.get(Direction.NORTH);
            builder.addAll(collection);
            Collection<BakedQuad> collection2 = this.sidedQuads.get(Direction.SOUTH);
            builder.addAll(collection2);
            Collection<BakedQuad> collection3 = this.sidedQuads.get(Direction.EAST);
            builder.addAll(collection3);
            Collection<BakedQuad> collection4 = this.sidedQuads.get(Direction.WEST);
            builder.addAll(collection4);
            Collection<BakedQuad> collection5 = this.sidedQuads.get(Direction.UP);
            builder.addAll(collection5);
            Collection<BakedQuad> collection6 = this.sidedQuads.get(Direction.DOWN);
            builder.addAll(collection6);
            return buildFromList(
               builder.build(),
               immutableList.size(),
               collection.size(),
               collection2.size(),
               collection3.size(),
               collection4.size(),
               collection5.size(),
               collection6.size()
            );
         }
      }
   }
}

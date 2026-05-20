package net.minecraft.client.render.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MultipartBlockStateModel implements BlockStateModel {
   private final MultipartBlockStateModel.MultipartBakedModel bakedModels;
   private final BlockState state;
   private @Nullable List<BlockStateModel> models;

   MultipartBlockStateModel(MultipartBlockStateModel.MultipartBakedModel bakedModels, BlockState state) {
      this.bakedModels = bakedModels;
      this.state = state;
   }

   @Override
   public Sprite particleSprite() {
      return this.bakedModels.particleSprite;
   }

   @Override
   public void addParts(Random random, List<BlockModelPart> parts) {
      if (this.models == null) {
         this.models = this.bakedModels.build(this.state);
      }

      long l = random.nextLong();

      for (BlockStateModel blockStateModel : this.models) {
         random.setSeed(l);
         blockStateModel.addParts(random, parts);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class MultipartBakedModel {
      private final List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors;
      final Sprite particleSprite;
      private final Map<BitSet, List<BlockStateModel>> map = new ConcurrentHashMap<>();

      private static BlockStateModel getFirst(List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors) {
         if (selectors.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one selector");
         } else {
            return selectors.getFirst().model();
         }
      }

      public MultipartBakedModel(List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors) {
         this.selectors = selectors;
         BlockStateModel blockStateModel = getFirst(selectors);
         this.particleSprite = blockStateModel.particleSprite();
      }

      public List<BlockStateModel> build(BlockState state) {
         BitSet bitSet = new BitSet();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(state)) {
               bitSet.set(i);
            }
         }

         return this.map.computeIfAbsent(bitSet, bitSetx -> {
            Builder<BlockStateModel> builder = ImmutableList.builder();

            for (int ix = 0; ix < this.selectors.size(); ix++) {
               if (bitSetx.get(ix)) {
                  builder.add(this.selectors.get(ix).model);
               }
            }

            return builder.build();
         });
      }
   }

   @Environment(EnvType.CLIENT)
   public static class MultipartUnbaked implements BlockStateModel.UnbakedGrouped {
      final List<MultipartBlockStateModel.Selector<BlockStateModel.Unbaked>> selectors;
      private final Baker.ResolvableCacheKey<MultipartBlockStateModel.MultipartBakedModel> bakerCache = new Baker.ResolvableCacheKey<MultipartBlockStateModel.MultipartBakedModel>(
         
      ) {
         public MultipartBlockStateModel.MultipartBakedModel compute(Baker baker) {
            Builder<MultipartBlockStateModel.Selector<BlockStateModel>> builder = ImmutableList.builderWithExpectedSize(MultipartUnbaked.this.selectors.size());

            for (MultipartBlockStateModel.Selector<BlockStateModel.Unbaked> selector : MultipartUnbaked.this.selectors) {
               builder.add(selector.build(selector.model.bake(baker)));
            }

            return new MultipartBlockStateModel.MultipartBakedModel(builder.build());
         }
      };

      public MultipartUnbaked(List<MultipartBlockStateModel.Selector<BlockStateModel.Unbaked>> selectors) {
         this.selectors = selectors;
      }

      @Override
      public Object getEqualityGroup(BlockState state) {
         IntList intList = new IntArrayList();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(state)) {
               intList.add(i);
            }
         }

         @Environment(EnvType.CLIENT)
         record EqualityGroup(MultipartBlockStateModel.MultipartUnbaked model, IntList selectors) {
         }

         return new EqualityGroup(this, intList);
      }

      @Override
      public void resolve(ResolvableModel.Resolver resolver) {
         this.selectors.forEach(selector -> selector.model.resolve(resolver));
      }

      @Override
      public BlockStateModel bake(BlockState state, Baker baker) {
         MultipartBlockStateModel.MultipartBakedModel multipartBakedModel = baker.compute(this.bakerCache);
         return new MultipartBlockStateModel(multipartBakedModel, state);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Selector<T>(Predicate<BlockState> condition, T model) {

      public <S> MultipartBlockStateModel.Selector<S> build(S object) {
         return new MultipartBlockStateModel.Selector<>(this.condition, object);
      }
   }
}

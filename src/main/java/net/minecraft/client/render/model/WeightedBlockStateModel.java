package net.minecraft.client.render.model;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class WeightedBlockStateModel implements BlockStateModel {
   private final Pool<BlockStateModel> models;
   private final Sprite particleSprite;

   public WeightedBlockStateModel(Pool<BlockStateModel> models) {
      this.models = models;
      BlockStateModel blockStateModel = models.getEntries().getFirst().value();
      this.particleSprite = blockStateModel.particleSprite();
   }

   @Override
   public Sprite particleSprite() {
      return this.particleSprite;
   }

   @Override
   public void addParts(Random random, List<BlockModelPart> parts) {
      this.models.get(random).addParts(random, parts);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(Pool<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked {
      @Override
      public BlockStateModel bake(Baker baker) {
         return new WeightedBlockStateModel(this.entries.transform(model -> model.bake(baker)));
      }

      @Override
      public void resolve(ResolvableModel.Resolver resolver) {
         this.entries.getEntries().forEach(entry -> entry.value().resolve(resolver));
      }
   }
}

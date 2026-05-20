package net.minecraft.client.render.model.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.MultipartBlockStateModel;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.util.dynamic.Codecs;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record BlockModelDefinition(Optional<BlockModelDefinition.Variants> simpleModels, Optional<BlockModelDefinition.Multipart> multipartModel) {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<BlockModelDefinition> CODEC = RecordCodecBuilder.<BlockModelDefinition>create(
         instance -> instance.group(
               BlockModelDefinition.Variants.CODEC.optionalFieldOf("variants").forGetter(BlockModelDefinition::simpleModels),
               BlockModelDefinition.Multipart.CODEC.optionalFieldOf("multipart").forGetter(BlockModelDefinition::multipartModel)
            )
            .apply(instance, BlockModelDefinition::new)
      )
      .validate(
         modelDefinition -> modelDefinition.simpleModels().isEmpty() && modelDefinition.multipartModel().isEmpty()
            ? DataResult.error(() -> "Neither 'variants' nor 'multipart' found")
            : DataResult.success(modelDefinition)
      );

   public Map<BlockState, BlockStateModel.UnbakedGrouped> load(StateManager<Block, BlockState> stateManager, Supplier<String> idSupplier) {
      Map<BlockState, BlockStateModel.UnbakedGrouped> map = new IdentityHashMap<>();
      this.simpleModels.ifPresent(simpleModels -> simpleModels.load(stateManager, idSupplier, (state, model) -> {
         BlockStateModel.UnbakedGrouped unbakedGrouped = map.put(state, model);
         if (unbakedGrouped != null) {
            throw new IllegalArgumentException("Overlapping definition on state: " + state);
         }
      }));
      this.multipartModel.ifPresent(multipartModel -> {
         List<BlockState> list = stateManager.getStates();
         BlockStateModel.UnbakedGrouped unbakedGrouped = multipartModel.toModel(stateManager);

         for (BlockState blockState : list) {
            map.putIfAbsent(blockState, unbakedGrouped);
         }
      });
      return map;
   }

   @Environment(EnvType.CLIENT)
   public record Multipart(List<MultipartModelComponent> selectors) {
      public static final Codec<BlockModelDefinition.Multipart> CODEC = Codecs.nonEmptyList(MultipartModelComponent.CODEC.listOf())
         .xmap(BlockModelDefinition.Multipart::new, BlockModelDefinition.Multipart::selectors);

      public MultipartBlockStateModel.MultipartUnbaked toModel(StateManager<Block, BlockState> stateManager) {
         Builder<MultipartBlockStateModel.Selector<BlockStateModel.Unbaked>> builder = ImmutableList.builderWithExpectedSize(this.selectors.size());

         for (MultipartModelComponent multipartModelComponent : this.selectors) {
            builder.add(new MultipartBlockStateModel.Selector<>(multipartModelComponent.init(stateManager), multipartModelComponent.model()));
         }

         return new MultipartBlockStateModel.MultipartUnbaked(builder.build());
      }
   }

   @Environment(EnvType.CLIENT)
   public record Variants(Map<String, BlockStateModel.Unbaked> models) {
      public static final Codec<BlockModelDefinition.Variants> CODEC = Codecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC))
         .xmap(BlockModelDefinition.Variants::new, BlockModelDefinition.Variants::models);

      public void load(
         StateManager<Block, BlockState> stateManager, Supplier<String> idSupplier, BiConsumer<BlockState, BlockStateModel.UnbakedGrouped> callback
      ) {
         this.models
            .forEach(
               (predicate, model) -> {
                  try {
                     Predicate<State<Block, BlockState>> predicate2 = BlockPropertiesPredicate.parse(stateManager, predicate);
                     BlockStateModel.UnbakedGrouped unbakedGrouped = model.cached();
                     UnmodifiableIterator var7 = stateManager.getStates().iterator();

                     while (var7.hasNext()) {
                        BlockState blockState = (BlockState)var7.next();
                        if (predicate2.test(blockState)) {
                           callback.accept(blockState, unbakedGrouped);
                        }
                     }
                  } catch (Exception var9) {
                     BlockModelDefinition.LOGGER
                        .warn(
                           "Exception loading blockstate definition: '{}' for variant: '{}': {}", new Object[]{idSupplier.get(), predicate, var9.getMessage()}
                        );
                  }
               }
            );
      }
   }
}

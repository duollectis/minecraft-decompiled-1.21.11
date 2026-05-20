package net.minecraft.client.render.model.json;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.util.StringIdentifiable;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface MultipartModelCondition {
   Codec<MultipartModelCondition> CODEC = Codec.recursive(
      "condition",
      group -> {
         Codec<MultipartModelCombinedCondition> codec = Codec.simpleMap(
               MultipartModelCombinedCondition.LogicalOperator.CODEC,
               group.listOf(),
               StringIdentifiable.toKeyable(MultipartModelCombinedCondition.LogicalOperator.values())
            )
            .codec()
            .comapFlatMap(
               map -> {
                  if (map.size() != 1) {
                     return DataResult.error(() -> "Invalid map size for combiner condition, expected exactly one element");
                  } else {
                     Entry<MultipartModelCombinedCondition.LogicalOperator, List<MultipartModelCondition>> entry = (Entry<MultipartModelCombinedCondition.LogicalOperator, List<MultipartModelCondition>>)map.entrySet()
                        .iterator()
                        .next();
                     return DataResult.success(new MultipartModelCombinedCondition(entry.getKey(), entry.getValue()));
                  }
               },
               multipartModelCombinedCondition -> Map.of(multipartModelCombinedCondition.operation(), multipartModelCombinedCondition.terms())
            );
         return Codec.either(codec, SimpleMultipartModelSelector.CODEC)
            .flatComapMap(
               either -> (MultipartModelCondition)either.map(
                  multipartModelCombinedCondition -> multipartModelCombinedCondition, simpleMultipartModelSelector -> simpleMultipartModelSelector
               ),
               multipartModelCondition -> {
                  return switch (multipartModelCondition) {
                     case MultipartModelCombinedCondition multipartModelCombinedCondition -> DataResult.success(Either.left(multipartModelCombinedCondition));
                     case SimpleMultipartModelSelector simpleMultipartModelSelector -> DataResult.success(Either.right(simpleMultipartModelSelector));
                     default -> DataResult.error(() -> "Unrecognized condition");
                  };
               }
            );
      }
   );

   <O, S extends State<O, S>> Predicate<S> instantiate(StateManager<O, S> value);
}

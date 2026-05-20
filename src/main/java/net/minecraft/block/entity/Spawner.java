package net.minecraft.block.entity;

import java.util.function.Consumer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

public interface Spawner {
   void setEntityType(EntityType<?> type, Random random);

   static void appendSpawnDataToTooltip(@Nullable TypedEntityData<BlockEntityType<?>> nbtComponent, Consumer<Text> textConsumer, String spawnDataKey) {
      Text text = getSpawnedEntityText(nbtComponent, spawnDataKey);
      if (text != null) {
         textConsumer.accept(text);
      } else {
         textConsumer.accept(ScreenTexts.EMPTY);
         textConsumer.accept(Text.translatable("block.minecraft.spawner.desc1").formatted(Formatting.GRAY));
         textConsumer.accept(ScreenTexts.space().append(Text.translatable("block.minecraft.spawner.desc2").formatted(Formatting.BLUE)));
      }
   }

   static @Nullable Text getSpawnedEntityText(@Nullable TypedEntityData<BlockEntityType<?>> nbtComponent, String spawnDataKey) {
      return nbtComponent == null
         ? null
         : nbtComponent.getNbtWithoutId()
            .getCompound(spawnDataKey)
            .flatMap(spawnDataNbt -> spawnDataNbt.getCompound("entity"))
            .flatMap(entityNbt -> entityNbt.get("id", EntityType.CODEC))
            .map(entityType -> Text.translatable(entityType.getTranslationKey()).formatted(Formatting.GRAY))
            .orElse(null);
   }
}

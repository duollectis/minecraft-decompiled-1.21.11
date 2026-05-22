package net.fabricmc.fabric.api.tag;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public interface FabricTagKey {
   default String getTranslationKey() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("tag.");
      TagKey<?> tagKey = (TagKey<?>)this;
      Identifier registryIdentifier = tagKey.registryRef().getValue();
      Identifier tagIdentifier = tagKey.id();
      if (!registryIdentifier.getNamespace().equals("minecraft")) {
         stringBuilder.append(registryIdentifier.getNamespace()).append(".");
      }

      stringBuilder.append(registryIdentifier.getPath().replace("/", "."))
         .append(".")
         .append(tagIdentifier.getNamespace())
         .append(".")
         .append(tagIdentifier.getPath().replace("/", ".").replace(":", "."));
      return stringBuilder.toString();
   }

   default Text getName() {
      return Text.translatableWithFallback(this.getTranslationKey(), "#" + ((TagKey)this).id().toString());
   }
}

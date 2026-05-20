package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.metadata.PackFeatureSetMetadata;
import net.minecraft.resource.metadata.PackOverlaysMetadata;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ResourcePackProfile {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourcePackInfo info;
   private final ResourcePackProfile.PackFactory packFactory;
   private final ResourcePackProfile.Metadata metaData;
   private final ResourcePackPosition position;

   public static @Nullable ResourcePackProfile create(
      ResourcePackInfo info, ResourcePackProfile.PackFactory packFactory, ResourceType type, ResourcePackPosition position
   ) {
      PackVersion packVersion = SharedConstants.getGameVersion().packVersion(type);
      ResourcePackProfile.Metadata metadata = loadMetadata(info, packFactory, packVersion, type);
      return metadata != null ? new ResourcePackProfile(info, packFactory, metadata, position) : null;
   }

   public ResourcePackProfile(
      ResourcePackInfo info, ResourcePackProfile.PackFactory packFactory, ResourcePackProfile.Metadata metaData, ResourcePackPosition position
   ) {
      this.info = info;
      this.packFactory = packFactory;
      this.metaData = metaData;
      this.position = position;
   }

   public static ResourcePackProfile.@Nullable Metadata loadMetadata(
      ResourcePackInfo info, ResourcePackProfile.PackFactory packFactory, PackVersion version, ResourceType type
   ) {
      try {
         ResourcePackProfile.Metadata var11;
         try (ResourcePack resourcePack = packFactory.open(info)) {
            PackResourceMetadata packResourceMetadata = resourcePack.parseMetadata(PackResourceMetadata.getSerializerFor(type));
            if (packResourceMetadata == null) {
               packResourceMetadata = resourcePack.parseMetadata(PackResourceMetadata.DESCRIPTION_SERIALIZER);
            }

            if (packResourceMetadata == null) {
               LOGGER.warn("Missing metadata in pack {}", info.id());
               return null;
            }

            PackFeatureSetMetadata packFeatureSetMetadata = resourcePack.parseMetadata(PackFeatureSetMetadata.SERIALIZER);
            FeatureSet featureSet = packFeatureSetMetadata != null ? packFeatureSetMetadata.flags() : FeatureSet.empty();
            ResourcePackCompatibility resourcePackCompatibility = ResourcePackCompatibility.from(packResourceMetadata.supportedFormats(), version);
            PackOverlaysMetadata packOverlaysMetadata = resourcePack.parseMetadata(PackOverlaysMetadata.getSerializerFor(type));
            List<String> list = packOverlaysMetadata != null ? packOverlaysMetadata.getAppliedOverlays(version) : List.of();
            var11 = new ResourcePackProfile.Metadata(packResourceMetadata.description(), resourcePackCompatibility, featureSet, list);
         }

         return var11;
      } catch (Exception var14) {
         LOGGER.warn("Failed to read pack {} metadata", info.id(), var14);
         return null;
      }
   }

   public ResourcePackInfo getInfo() {
      return this.info;
   }

   public Text getDisplayName() {
      return this.info.title();
   }

   public Text getDescription() {
      return this.metaData.description();
   }

   public Text getInformationText(boolean enabled) {
      return this.info.getInformationText(enabled, this.metaData.description);
   }

   public ResourcePackCompatibility getCompatibility() {
      return this.metaData.compatibility();
   }

   public FeatureSet getRequestedFeatures() {
      return this.metaData.requestedFeatures();
   }

   public ResourcePack createResourcePack() {
      return this.packFactory.openWithOverlays(this.info, this.metaData);
   }

   public String getId() {
      return this.info.id();
   }

   public ResourcePackPosition getPosition() {
      return this.position;
   }

   public boolean isRequired() {
      return this.position.required();
   }

   public boolean isPinned() {
      return this.position.fixedPosition();
   }

   public ResourcePackProfile.InsertionPosition getInitialPosition() {
      return this.position.defaultPosition();
   }

   public ResourcePackSource getSource() {
      return this.info.source();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof ResourcePackProfile resourcePackProfile) ? false : this.info.equals(resourcePackProfile.info);
      }
   }

   @Override
   public int hashCode() {
      return this.info.hashCode();
   }

   public static enum InsertionPosition {
      TOP,
      BOTTOM;

      public <T> int insert(List<T> items, T item, Function<T, ResourcePackPosition> profileGetter, boolean listInverted) {
         ResourcePackProfile.InsertionPosition insertionPosition = listInverted ? this.inverse() : this;
         if (insertionPosition == BOTTOM) {
            int i;
            for (i = 0; i < items.size(); i++) {
               ResourcePackPosition resourcePackPosition = profileGetter.apply(items.get(i));
               if (!resourcePackPosition.fixedPosition() || resourcePackPosition.defaultPosition() != this) {
                  break;
               }
            }

            items.add(i, item);
            return i;
         } else {
            int i;
            for (i = items.size() - 1; i >= 0; i--) {
               ResourcePackPosition resourcePackPosition = profileGetter.apply(items.get(i));
               if (!resourcePackPosition.fixedPosition() || resourcePackPosition.defaultPosition() != this) {
                  break;
               }
            }

            items.add(i + 1, item);
            return i + 1;
         }
      }

      public ResourcePackProfile.InsertionPosition inverse() {
         return this == TOP ? BOTTOM : TOP;
      }
   }

   public record Metadata(Text description, ResourcePackCompatibility compatibility, FeatureSet requestedFeatures, List<String> overlays) {
   }

   public interface PackFactory {
      ResourcePack open(ResourcePackInfo info);

      ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata);
   }
}

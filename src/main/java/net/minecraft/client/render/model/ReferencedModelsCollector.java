package net.minecraft.client.render.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ReferencedModelsCollector {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<Identifier, ReferencedModelsCollector.Holder> modelCache = new Object2ObjectOpenHashMap();
   private final ReferencedModelsCollector.Holder missingModel;
   private final Object2ObjectFunction<Identifier, ReferencedModelsCollector.Holder> holder;
   private final ResolvableModel.Resolver resolver;
   private final Queue<ReferencedModelsCollector.Holder> queue = new ArrayDeque<>();

   public ReferencedModelsCollector(Map<Identifier, UnbakedModel> unbakedModels, UnbakedModel missingModel) {
      this.missingModel = new ReferencedModelsCollector.Holder(MissingModel.ID, missingModel, true);
      this.modelCache.put(MissingModel.ID, this.missingModel);
      this.holder = id -> {
         Identifier identifier = (Identifier)id;
         UnbakedModel unbakedModel = unbakedModels.get(identifier);
         if (unbakedModel == null) {
            LOGGER.warn("Missing block model: {}", identifier);
            return this.missingModel;
         } else {
            return this.schedule(identifier, unbakedModel);
         }
      };
      this.resolver = this::resolve;
   }

   private static boolean isRootModel(UnbakedModel model) {
      return model.parent() == null;
   }

   private ReferencedModelsCollector.Holder resolve(Identifier id) {
      return (ReferencedModelsCollector.Holder)this.modelCache.computeIfAbsent(id, this.holder);
   }

   private ReferencedModelsCollector.Holder schedule(Identifier id, UnbakedModel model) {
      boolean bl = isRootModel(model);
      ReferencedModelsCollector.Holder holder = new ReferencedModelsCollector.Holder(id, model, bl);
      if (!bl) {
         this.queue.add(holder);
      }

      return holder;
   }

   public void resolve(ResolvableModel model) {
      model.resolve(this.resolver);
   }

   public void addSpecialModel(Identifier id, UnbakedModel model) {
      if (!isRootModel(model)) {
         LOGGER.warn("Trying to add non-root special model {}, ignoring", id);
      } else {
         ReferencedModelsCollector.Holder holder = (ReferencedModelsCollector.Holder)this.modelCache.put(id, this.schedule(id, model));
         if (holder != null) {
            LOGGER.warn("Duplicate special model {}", id);
         }
      }
   }

   public BakedSimpleModel getMissingModel() {
      return this.missingModel;
   }

   public Map<Identifier, BakedSimpleModel> collectModels() {
      List<ReferencedModelsCollector.Holder> list = new ArrayList<>();
      this.resolveAll(list);
      checkIfValid(list);
      Builder<Identifier, BakedSimpleModel> builder = ImmutableMap.builder();
      this.modelCache.forEach((id, model) -> {
         if (model.valid) {
            builder.put(id, model);
         } else {
            LOGGER.warn("Model {} ignored due to cyclic dependency", id);
         }
      });
      return builder.build();
   }

   private void resolveAll(List<ReferencedModelsCollector.Holder> models) {
      ReferencedModelsCollector.Holder holder;
      while ((holder = this.queue.poll()) != null) {
         Identifier identifier = Objects.requireNonNull(holder.model.parent());
         ReferencedModelsCollector.Holder holder2 = this.resolve(identifier);
         holder.parent = holder2;
         if (holder2.valid) {
            holder.valid = true;
         } else {
            models.add(holder);
         }
      }
   }

   private static void checkIfValid(List<ReferencedModelsCollector.Holder> models) {
      boolean bl = true;

      while (bl) {
         bl = false;
         Iterator<ReferencedModelsCollector.Holder> iterator = models.iterator();

         while (iterator.hasNext()) {
            ReferencedModelsCollector.Holder holder = iterator.next();
            if (Objects.requireNonNull(holder.parent).valid) {
               holder.valid = true;
               iterator.remove();
               bl = true;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class Holder implements BakedSimpleModel {
      private static final ReferencedModelsCollector.Property<Boolean> AMBIENT_OCCLUSION_PROPERTY = createProperty(0);
      private static final ReferencedModelsCollector.Property<UnbakedModel.GuiLight> GUI_LIGHT_PROPERTY = createProperty(1);
      private static final ReferencedModelsCollector.Property<Geometry> GEOMETRY_PROPERTY = createProperty(2);
      private static final ReferencedModelsCollector.Property<ModelTransformation> TRANSFORMATIONS_PROPERTY = createProperty(3);
      private static final ReferencedModelsCollector.Property<ModelTextures> TEXTURE_PROPERTY = createProperty(4);
      private static final ReferencedModelsCollector.Property<Sprite> PARTICLE_TEXTURE_PROPERTY = createProperty(5);
      private static final ReferencedModelsCollector.Property<BakedGeometry> BAKED_GEOMETRY_PROPERTY = createProperty(6);
      private static final int PROPERTY_COUNT = 7;
      private final Identifier id;
      boolean valid;
      ReferencedModelsCollector.@Nullable Holder parent;
      final UnbakedModel model;
      private final AtomicReferenceArray<@Nullable Object> properties = new AtomicReferenceArray<>(7);
      private final Map<ModelBakeSettings, BakedGeometry> bakeCache = new ConcurrentHashMap<>();

      private static <T> ReferencedModelsCollector.Property<T> createProperty(int i) {
         Objects.checkIndex(i, 7);
         return new ReferencedModelsCollector.Property<>(i);
      }

      Holder(Identifier id, UnbakedModel model, boolean valid) {
         this.id = id;
         this.model = model;
         this.valid = valid;
      }

      @Override
      public UnbakedModel getModel() {
         return this.model;
      }

      @Override
      public @Nullable BakedSimpleModel getParent() {
         return this.parent;
      }

      @Override
      public String name() {
         return this.id.toString();
      }

      private <T> @Nullable T getProperty(ReferencedModelsCollector.Property<T> property) {
         return (T)this.properties.get(property.index);
      }

      private <T> T setProperty(ReferencedModelsCollector.Property<T> property, T value) {
         T object = (T)this.properties.compareAndExchange(property.index, null, value);
         return object == null ? value : object;
      }

      private <T> T getProperty(ReferencedModelsCollector.Property<T> property, Function<BakedSimpleModel, T> fallback) {
         T object = this.getProperty(property);
         return object != null ? object : this.setProperty(property, fallback.apply(this));
      }

      @Override
      public boolean getAmbientOcclusion() {
         return this.<Boolean>getProperty(AMBIENT_OCCLUSION_PROPERTY, model -> BakedSimpleModel.getAmbientOcclusion(model));
      }

      @Override
      public UnbakedModel.GuiLight getGuiLight() {
         return this.getProperty(GUI_LIGHT_PROPERTY, model -> BakedSimpleModel.getGuiLight(model));
      }

      @Override
      public ModelTransformation getTransformations() {
         return this.getProperty(TRANSFORMATIONS_PROPERTY, BakedSimpleModel::copyTransformations);
      }

      @Override
      public Geometry getGeometry() {
         return this.getProperty(GEOMETRY_PROPERTY, model -> BakedSimpleModel.getGeometry(model));
      }

      @Override
      public ModelTextures getTextures() {
         return this.getProperty(TEXTURE_PROPERTY, model -> BakedSimpleModel.getTextures(model));
      }

      @Override
      public Sprite getParticleTexture(ModelTextures textures, Baker baker) {
         Sprite sprite = this.getProperty(PARTICLE_TEXTURE_PROPERTY);
         return sprite != null ? sprite : this.setProperty(PARTICLE_TEXTURE_PROPERTY, BakedSimpleModel.getParticleTexture(textures, baker, this));
      }

      private BakedGeometry getBakedGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings) {
         BakedGeometry bakedGeometry = this.getProperty(BAKED_GEOMETRY_PROPERTY);
         return bakedGeometry != null ? bakedGeometry : this.setProperty(BAKED_GEOMETRY_PROPERTY, this.getGeometry().bake(textures, baker, settings, this));
      }

      @Override
      public BakedGeometry bakeGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings) {
         return settings == ModelRotation.IDENTITY ? this.getBakedGeometry(textures, baker, settings) : this.bakeCache.computeIfAbsent(settings, settings1 -> {
            Geometry geometry = this.getGeometry();
            return geometry.bake(textures, baker, settings1, this);
         });
      }
   }

   @Environment(EnvType.CLIENT)
   record Property<T>(int index) {
   }
}

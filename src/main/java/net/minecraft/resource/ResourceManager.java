package net.minecraft.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;

public interface ResourceManager extends ResourceFactory {
   Set<String> getAllNamespaces();

   List<Resource> getAllResources(Identifier id);

   Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate);

   Map<Identifier, List<Resource>> findAllResources(String startingPath, Predicate<Identifier> allowedPathPredicate);

   Stream<ResourcePack> streamResourcePacks();

   public static enum Empty implements ResourceManager {
      INSTANCE;

      @Override
      public Set<String> getAllNamespaces() {
         return Set.of();
      }

      @Override
      public Optional<Resource> getResource(Identifier identifier) {
         return Optional.empty();
      }

      @Override
      public List<Resource> getAllResources(Identifier id) {
         return List.of();
      }

      @Override
      public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
         return Map.of();
      }

      @Override
      public Map<Identifier, List<Resource>> findAllResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
         return Map.of();
      }

      @Override
      public Stream<ResourcePack> streamResourcePacks() {
         return Stream.of();
      }
   }
}

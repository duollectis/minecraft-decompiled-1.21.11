package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

public record EntityNbtDataSource(String rawSelector, @Nullable EntitySelector selector) implements NbtDataSource {
   public static final MapCodec<EntityNbtDataSource> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.STRING.fieldOf("entity").forGetter(EntityNbtDataSource::rawSelector)).apply(instance, EntityNbtDataSource::new)
   );

   public EntityNbtDataSource(String rawPath) {
      this(rawPath, parseSelector(rawPath));
   }

   private static @Nullable EntitySelector parseSelector(String rawSelector) {
      try {
         EntitySelectorReader entitySelectorReader = new EntitySelectorReader(new StringReader(rawSelector), true);
         return entitySelectorReader.read();
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   @Override
   public Stream<NbtCompound> get(ServerCommandSource source) throws CommandSyntaxException {
      if (this.selector != null) {
         List<? extends Entity> list = this.selector.getEntities(source);
         return list.stream().map(NbtPredicate::entityToNbt);
      } else {
         return Stream.empty();
      }
   }

   @Override
   public MapCodec<EntityNbtDataSource> getCodec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return "entity=" + this.rawSelector;
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof EntityNbtDataSource entityNbtDataSource && this.rawSelector.equals(entityNbtDataSource.rawSelector);
   }

   @Override
   public int hashCode() {
      return this.rawSelector.hashCode();
   }
}

package net.minecraft.command.permission;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public interface Permission {
   Codec<Permission> UNABBREVIATED_CODEC = Registries.PERMISSION_TYPE.getCodec().dispatch(Permission::getCodec, codec -> codec);
   Codec<Permission> CODEC = Codec.either(UNABBREVIATED_CODEC, Identifier.CODEC)
      .xmap(
         either -> (Permission)either.map(perm -> perm, Permission.Atom::of),
         perm -> perm instanceof Permission.Atom atom ? Either.right(atom.id()) : Either.left(perm)
      );

   MapCodec<? extends Permission> getCodec();

   public record Atom(Identifier id) implements Permission {
      public static final MapCodec<Permission.Atom> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(Identifier.CODEC.fieldOf("id").forGetter(Permission.Atom::id)).apply(instance, Permission.Atom::new)
      );

      @Override
      public MapCodec<Permission.Atom> getCodec() {
         return CODEC;
      }

      public static Permission.Atom ofVanilla(String path) {
         return of(Identifier.ofVanilla(path));
      }

      public static Permission.Atom of(Identifier id) {
         return new Permission.Atom(id);
      }
   }

   public record Level(PermissionLevel level) implements Permission {
      public static final MapCodec<Permission.Level> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(PermissionLevel.CODEC.fieldOf("level").forGetter(Permission.Level::level)).apply(instance, Permission.Level::new)
      );

      @Override
      public MapCodec<Permission.Level> getCodec() {
         return CODEC;
      }
   }
}

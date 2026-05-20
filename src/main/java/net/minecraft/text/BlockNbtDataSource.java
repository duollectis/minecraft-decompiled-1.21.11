package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public record BlockNbtDataSource(String rawPos, @Nullable PosArgument pos) implements NbtDataSource {
   public static final MapCodec<BlockNbtDataSource> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.STRING.fieldOf("block").forGetter(BlockNbtDataSource::rawPos)).apply(instance, BlockNbtDataSource::new)
   );

   public BlockNbtDataSource(String rawPath) {
      this(rawPath, parsePos(rawPath));
   }

   private static @Nullable PosArgument parsePos(String string) {
      try {
         return BlockPosArgumentType.blockPos().parse(new StringReader(string));
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   @Override
   public Stream<NbtCompound> get(ServerCommandSource source) {
      if (this.pos != null) {
         ServerWorld serverWorld = source.getWorld();
         BlockPos blockPos = this.pos.toAbsoluteBlockPos(source);
         if (serverWorld.isPosLoaded(blockPos)) {
            BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos);
            if (blockEntity != null) {
               return Stream.of(blockEntity.createNbtWithIdentifyingData(source.getRegistryManager()));
            }
         }
      }

      return Stream.empty();
   }

   @Override
   public MapCodec<BlockNbtDataSource> getCodec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return "block=" + this.rawPos;
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof BlockNbtDataSource blockNbtDataSource && this.rawPos.equals(blockNbtDataSource.rawPos);
   }

   @Override
   public int hashCode() {
      return this.rawPos.hashCode();
   }
}

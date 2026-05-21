package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;

import java.util.stream.Stream;

/**
 * {@code NbtDataSource}.
 */
public interface NbtDataSource {

	Stream<NbtCompound> get(ServerCommandSource source) throws CommandSyntaxException;

	MapCodec<? extends NbtDataSource> getCodec();
}

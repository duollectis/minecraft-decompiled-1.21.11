package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Источник NBT-данных, читающий данные из блочной сущности по заданным координатам.
 *
 * <p>Позиция парсится из строки при создании объекта. Если строка невалидна,
 * поле {@code pos} будет {@code null} и {@link #get} вернёт пустой поток.
 * Сравнение экземпляров выполняется только по строке {@code rawPos},
 * так как {@code pos} является производным полем.</p>
 */
public record BlockNbtDataSource(String rawPos, @Nullable PosArgument pos) implements NbtDataSource {

	public static final MapCodec<BlockNbtDataSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.STRING.fieldOf("block").forGetter(BlockNbtDataSource::rawPos))
					.apply(instance, BlockNbtDataSource::new)
	);

	public BlockNbtDataSource(String rawPos) {
		this(rawPos, parsePos(rawPos));
	}

	private static @Nullable PosArgument parsePos(String rawPos) {
		try {
			return BlockPosArgumentType.blockPos().parse(new StringReader(rawPos));
		}
		catch (CommandSyntaxException e) {
			return null;
		}
	}

	@Override
	public Stream<NbtCompound> get(ServerCommandSource source) {
		if (pos == null) {
			return Stream.empty();
		}

		ServerWorld world = source.getWorld();
		BlockPos blockPos = pos.toAbsoluteBlockPos(source);

		if (!world.isPosLoaded(blockPos)) {
			return Stream.empty();
		}

		BlockEntity blockEntity = world.getBlockEntity(blockPos);

		return blockEntity != null
				? Stream.of(blockEntity.createNbtWithIdentifyingData(source.getRegistryManager()))
				: Stream.empty();
	}

	@Override
	public MapCodec<BlockNbtDataSource> getCodec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "block=" + rawPos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof BlockNbtDataSource other && rawPos.equals(other.rawPos);
	}

	@Override
	public int hashCode() {
		return rawPos.hashCode();
	}
}

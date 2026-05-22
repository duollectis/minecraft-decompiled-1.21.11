package net.minecraft.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.storage.NbtReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.function.Function;

/**
 * Реализация {@link DataCommandObject} для блоков с {@link BlockEntity}.
 * Позволяет читать и записывать NBT-данные блочной сущности через команду {@code /data}.
 */
public class BlockDataObject implements DataCommandObject {

	private static final Logger LOGGER = LogUtils.getLogger();

	static final SimpleCommandExceptionType INVALID_BLOCK_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.data.block.invalid"));

	public static final Function<String, DataCommand.ObjectType> TYPE_FACTORY =
			argumentName -> new DataCommand.ObjectType() {
				@Override
				public DataCommandObject getObject(CommandContext<ServerCommandSource> context)
				throws CommandSyntaxException {
					BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, argumentName + "Pos");
					BlockEntity blockEntity = context.getSource().getWorld().getBlockEntity(pos);
					if (blockEntity == null) {
						throw INVALID_BLOCK_EXCEPTION.create();
					}

					return new BlockDataObject(blockEntity, pos);
				}

				@Override
				public ArgumentBuilder<ServerCommandSource, ?> addArgumentsToBuilder(
						ArgumentBuilder<ServerCommandSource, ?> argument,
						Function<ArgumentBuilder<ServerCommandSource, ?>, ArgumentBuilder<ServerCommandSource, ?>> argumentAdder
				) {
					return argument.then(
							CommandManager
									.literal("block")
									.then(argumentAdder.apply(CommandManager.argument(
											argumentName + "Pos",
											BlockPosArgumentType.blockPos()
									)))
					);
				}
			};

	private final BlockEntity blockEntity;
	private final BlockPos pos;

	public BlockDataObject(BlockEntity blockEntity, BlockPos pos) {
		this.blockEntity = blockEntity;
		this.pos = pos;
	}

	@Override
	public void setNbt(NbtCompound nbt) {
		BlockState blockState = blockEntity.getWorld().getBlockState(pos);

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
			blockEntity.read(NbtReadView.create(logging, blockEntity.getWorld().getRegistryManager(), nbt));
			blockEntity.markDirty();
			blockEntity.getWorld().updateListeners(pos, blockState, blockState, 3);
		}
	}

	@Override
	public NbtCompound getNbt() {
		return blockEntity.createNbtWithIdentifyingData(blockEntity.getWorld().getRegistryManager());
	}

	@Override
	public Text feedbackModify() {
		return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public Text feedbackQuery(NbtElement element) {
		return Text.translatable(
				"commands.data.block.query",
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				NbtHelper.toPrettyPrintedText(element)
		);
	}

	@Override
	public Text feedbackGet(NbtPathArgumentType.NbtPath path, double scale, int result) {
		return Text.translatable(
				"commands.data.block.get",
				path.getString(),
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				String.format(Locale.ROOT, "%.2f", scale),
				result
		);
	}
}

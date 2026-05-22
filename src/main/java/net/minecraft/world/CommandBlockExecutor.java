package net.minecraft.world;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.StringHelper;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Базовый исполнитель команд командного блока.
 * Хранит команду, счётчик успехов, последний вывод и управляет
 * логикой выполнения команды на сервере.
 */
public abstract class CommandBlockExecutor {

	private static final Text DEFAULT_NAME = Text.literal("@");
	private static final long NO_LAST_EXECUTION = -1L;

	private long lastExecution = NO_LAST_EXECUTION;
	private boolean updateLastExecution = true;
	private int successCount;
	private boolean trackOutput = true;
	@Nullable Text lastOutput;
	private String command = "";
	private @Nullable Text customName;

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public Text getLastOutput() {
		return lastOutput == null ? ScreenTexts.EMPTY : lastOutput;
	}

	public void writeData(WriteView view) {
		view.putString("Command", command);
		view.putInt("SuccessCount", successCount);
		view.putNullable("CustomName", TextCodecs.CODEC, customName);
		view.putBoolean("TrackOutput", trackOutput);

		if (trackOutput) {
			view.putNullable("LastOutput", TextCodecs.CODEC, lastOutput);
		}

		view.putBoolean("UpdateLastExecution", updateLastExecution);

		if (updateLastExecution && lastExecution != NO_LAST_EXECUTION) {
			view.putLong("LastExecution", lastExecution);
		}
	}

	public void readData(ReadView view) {
		command = view.getString("Command", "");
		successCount = view.getInt("SuccessCount", 0);
		setCustomName(BlockEntity.tryParseCustomName(view, "CustomName"));
		trackOutput = view.getBoolean("TrackOutput", true);
		lastOutput = trackOutput ? BlockEntity.tryParseCustomName(view, "LastOutput") : null;
		updateLastExecution = view.getBoolean("UpdateLastExecution", true);
		lastExecution = updateLastExecution ? view.getLong("LastExecution", NO_LAST_EXECUTION) : NO_LAST_EXECUTION;
	}

	public void setCommand(String command) {
		this.command = command;
		successCount = 0;
	}

	public String getCommand() {
		return command;
	}

	/**
	 * Выполняет команду в заданном мире.
	 * Пропускает выполнение, если команда уже была выполнена в этом тике.
	 * Пасхалка: команда "Searge" всегда возвращает успех с особым выводом.
	 *
	 * @param world серверный мир для выполнения команды
	 * @return {@code true} если команда была выполнена (или пасхалка сработала)
	 */
	public boolean execute(ServerWorld world) {
		if (world.getTime() == lastExecution) {
			return false;
		}

		if ("Searge".equalsIgnoreCase(command)) {
			lastOutput = Text.literal("#itzlipofutzli");
			successCount = 1;
			return true;
		}

		successCount = 0;

		if (world.areCommandBlocksEnabled() && !StringHelper.isEmpty(command)) {
			try {
				lastOutput = null;

				try (CommandBlockOutput output = createOutput(world)) {
					CommandOutput commandOutput = Objects.requireNonNullElse(output, CommandOutput.DUMMY);
					ServerCommandSource source = getSource(world, commandOutput)
						.withReturnValueConsumer((successful, returnValue) -> {
							if (successful) {
								successCount++;
							}
						});
					world.getServer().getCommandManager().parseAndExecute(source, command);
				}
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "Executing command block");
				CrashReportSection section = crashReport.addElement("Command to be executed");
				section.add("Command", this::getCommand);
				section.add("Name", () -> getName().getString());
				throw new CrashException(crashReport);
			}
		}

		lastExecution = updateLastExecution ? world.getTime() : NO_LAST_EXECUTION;

		return true;
	}

	private @Nullable CommandBlockOutput createOutput(ServerWorld serverWorld) {
		return trackOutput ? new CommandBlockOutput(serverWorld) : null;
	}

	public Text getName() {
		return customName != null ? customName : DEFAULT_NAME;
	}

	public @Nullable Text getCustomName() {
		return customName;
	}

	public void setCustomName(@Nullable Text customName) {
		this.customName = customName;
	}

	public abstract void markDirty(ServerWorld world);

	public void setLastOutput(@Nullable Text lastOutput) {
		this.lastOutput = lastOutput;
	}

	public void setTrackOutput(boolean trackOutput) {
		this.trackOutput = trackOutput;
	}

	public boolean isTrackingOutput() {
		return trackOutput;
	}

	public abstract ServerCommandSource getSource(ServerWorld world, CommandOutput output);

	public abstract boolean isEditable();

	/**
	 * Внутренний получатель вывода команды, записывающий результат в поле {@code lastOutput}
	 * с временной меткой и уведомляющий мир об изменении.
	 */
	protected class CommandBlockOutput implements CommandOutput, AutoCloseable {

		private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

		private final ServerWorld world;
		private boolean closed;

		protected CommandBlockOutput(ServerWorld world) {
			this.world = world;
		}

		@Override
		public boolean shouldReceiveFeedback() {
			return !closed && world.getGameRules().getValue(GameRules.SEND_COMMAND_FEEDBACK);
		}

		@Override
		public boolean shouldTrackOutput() {
			return !closed;
		}

		@Override
		public boolean shouldBroadcastConsoleToOps() {
			return !closed && world.getGameRules().getValue(GameRules.COMMAND_BLOCK_OUTPUT);
		}

		@Override
		public void sendMessage(Text message) {
			if (closed) {
				return;
			}

			CommandBlockExecutor.this.lastOutput =
				Text.literal("[" + TIME_FORMATTER.format(ZonedDateTime.now()) + "] ").append(message);
			CommandBlockExecutor.this.markDirty(world);
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}

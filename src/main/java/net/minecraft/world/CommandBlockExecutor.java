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
 * {@code CommandBlockExecutor}.
 */
public abstract class CommandBlockExecutor {

	private static final Text DEFAULT_NAME = Text.literal("@");
	private static final int DEFAULT_LAST_EXECUTION = -1;
	private long lastExecution = -1L;
	private boolean updateLastExecution = true;
	private int successCount;
	private boolean trackOutput = true;
	@Nullable Text lastOutput;
	private String command = "";
	private @Nullable Text customName;

	public int getSuccessCount() {
		return this.successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public Text getLastOutput() {
		return this.lastOutput == null ? ScreenTexts.EMPTY : this.lastOutput;
	}

	public void writeData(WriteView view) {
		view.putString("Command", this.command);
		view.putInt("SuccessCount", this.successCount);
		view.putNullable("CustomName", TextCodecs.CODEC, this.customName);
		view.putBoolean("TrackOutput", this.trackOutput);
		if (this.trackOutput) {
			view.putNullable("LastOutput", TextCodecs.CODEC, this.lastOutput);
		}

		view.putBoolean("UpdateLastExecution", this.updateLastExecution);
		if (this.updateLastExecution && this.lastExecution != -1L) {
			view.putLong("LastExecution", this.lastExecution);
		}
	}

	public void readData(ReadView view) {
		this.command = view.getString("Command", "");
		this.successCount = view.getInt("SuccessCount", 0);
		this.setCustomName(BlockEntity.tryParseCustomName(view, "CustomName"));
		this.trackOutput = view.getBoolean("TrackOutput", true);
		if (this.trackOutput) {
			this.lastOutput = BlockEntity.tryParseCustomName(view, "LastOutput");
		}
		else {
			this.lastOutput = null;
		}

		this.updateLastExecution = view.getBoolean("UpdateLastExecution", true);
		if (this.updateLastExecution) {
			this.lastExecution = view.getLong("LastExecution", -1L);
		}
		else {
			this.lastExecution = -1L;
		}
	}

	public void setCommand(String command) {
		this.command = command;
		this.successCount = 0;
	}

	public String getCommand() {
		return this.command;
	}

	public boolean execute(ServerWorld world) {
		if (world.getTime() == this.lastExecution) {
			return false;
		}
		else if ("Searge".equalsIgnoreCase(this.command)) {
			this.lastOutput = Text.literal("#itzlipofutzli");
			this.successCount = 1;
			return true;
		}
		else {
			this.successCount = 0;
			if (world.areCommandBlocksEnabled() && !StringHelper.isEmpty(this.command)) {
				try {
					this.lastOutput = null;

					try (CommandBlockExecutor.CommandBlockOutput commandBlockOutput = this.createOutput(world)) {
						CommandOutput
								commandOutput =
								Objects.requireNonNullElse(commandBlockOutput, CommandOutput.DUMMY);
						ServerCommandSource
								serverCommandSource =
								this
										.getSource(world, commandOutput)
										.withReturnValueConsumer((successful, returnValue) -> {
											if (successful) {
												this.successCount++;
											}
										});
						world.getServer().getCommandManager().parseAndExecute(serverCommandSource, this.command);
					}
				}
				catch (Throwable var7) {
					CrashReport crashReport = CrashReport.create(var7, "Executing command block");
					CrashReportSection crashReportSection = crashReport.addElement("Command to be executed");
					crashReportSection.add("Command", this::getCommand);
					crashReportSection.add("Name", () -> this.getName().getString());
					throw new CrashException(crashReport);
				}
			}

			if (this.updateLastExecution) {
				this.lastExecution = world.getTime();
			}
			else {
				this.lastExecution = -1L;
			}

			return true;
		}
	}

	private CommandBlockExecutor.@Nullable CommandBlockOutput createOutput(ServerWorld serverWorld) {
		return this.trackOutput ? new CommandBlockExecutor.CommandBlockOutput(serverWorld) : null;
	}

	public Text getName() {
		return this.customName != null ? this.customName : DEFAULT_NAME;
	}

	public @Nullable Text getCustomName() {
		return this.customName;
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
		return this.trackOutput;
	}

	public abstract ServerCommandSource getSource(ServerWorld world, CommandOutput output);

	public abstract boolean isEditable();

	/**
	 * {@code CommandBlockOutput}.
	 */
	protected class CommandBlockOutput implements CommandOutput, AutoCloseable {

		private final ServerWorld world;
		private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
		private boolean closed;

		protected CommandBlockOutput(final ServerWorld world) {
			this.world = world;
		}

		@Override
		public boolean shouldReceiveFeedback() {
			return !this.closed && this.world.getGameRules().getValue(GameRules.SEND_COMMAND_FEEDBACK);
		}

		@Override
		public boolean shouldTrackOutput() {
			return !this.closed;
		}

		@Override
		public boolean shouldBroadcastConsoleToOps() {
			return !this.closed && this.world.getGameRules().getValue(GameRules.COMMAND_BLOCK_OUTPUT);
		}

		@Override
		public void sendMessage(Text message) {
			if (!this.closed) {
				CommandBlockExecutor.this.lastOutput =
						Text.literal("[" + TIME_FORMATTER.format(ZonedDateTime.now()) + "] ").append(message);
				CommandBlockExecutor.this.markDirty(this.world);
			}
		}

		@Override
		public void close() throws Exception {
			this.closed = true;
		}
	}
}

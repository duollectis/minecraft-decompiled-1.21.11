package net.minecraft.command;

/**
 * Битовые флаги, управляющие поведением выполнения команды.
 * Хранятся в одном байте для минимального расхода памяти.
 */
public record ExecutionFlags(byte flags) {

	public static final ExecutionFlags NONE = new ExecutionFlags((byte) 0);
	private static final byte SILENT = 1;
	private static final byte INSIDE_RETURN_RUN = 2;

	private ExecutionFlags set(byte flag) {
		int merged = flags | flag;
		return merged != flags ? new ExecutionFlags((byte) merged) : this;
	}

	public boolean isSilent() {
		return (flags & SILENT) != 0;
	}

	public ExecutionFlags setSilent() {
		return set(SILENT);
	}

	public boolean isInsideReturnRun() {
		return (flags & INSIDE_RETURN_RUN) != 0;
	}

	public ExecutionFlags setInsideReturnRun() {
		return set(INSIDE_RETURN_RUN);
	}
}

package net.minecraft.world;

/**
 * {@code PersistentState}.
 */
public abstract class PersistentState {

	private boolean dirty;

	/**
	 * Mark dirty.
	 */
	public void markDirty() {
		this.setDirty(true);
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isDirty() {
		return this.dirty;
	}
}

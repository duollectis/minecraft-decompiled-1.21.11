package net.minecraft.screen.sync;

import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@code TrackedSlot}.
 */
public interface TrackedSlot {

	TrackedSlot ALWAYS_IN_SYNC = new TrackedSlot() {
		@Override
		public void setReceivedHash(ItemStackHash receivedHash) {
		}

		@Override
		public void setReceivedStack(ItemStack receivedStack) {
		}

		@Override
		public boolean isInSync(ItemStack actualStack) {
			return true;
		}
	};

	void setReceivedStack(ItemStack receivedStack);

	void setReceivedHash(ItemStackHash receivedHash);

	boolean isInSync(ItemStack actualStack);

	/**
	 * {@code Impl}.
	 */
	public static class Impl implements TrackedSlot {

		private final ComponentChangesHash.ComponentHasher hasher;
		private @Nullable ItemStack receivedStack = null;
		private @Nullable ItemStackHash receivedHash = null;

		public Impl(ComponentChangesHash.ComponentHasher hasher) {
			this.hasher = hasher;
		}

		@Override
		public void setReceivedStack(ItemStack receivedStack) {
			this.receivedStack = receivedStack.copy();
			this.receivedHash = null;
		}

		@Override
		public void setReceivedHash(ItemStackHash receivedHash) {
			this.receivedStack = null;
			this.receivedHash = receivedHash;
		}

		@Override
		public boolean isInSync(ItemStack actualStack) {
			if (this.receivedStack != null) {
				return ItemStack.areEqual(this.receivedStack, actualStack);
			}
			else if (this.receivedHash != null && this.receivedHash.hashEquals(actualStack, this.hasher)) {
				this.receivedStack = actualStack.copy();
				return true;
			}
			else {
				return false;
			}
		}

		public void copyFrom(TrackedSlot.Impl slot) {
			this.receivedStack = slot.receivedStack;
			this.receivedHash = slot.receivedHash;
		}
	}
}

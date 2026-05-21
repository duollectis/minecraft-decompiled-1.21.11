package net.minecraft.resource.fs;

import java.nio.file.Path;
import java.util.Map;

/**
 * {@code ResourceFile}.
 */
interface ResourceFile {

	ResourceFile EMPTY = new ResourceFile() {
		@Override
		public String toString() {
			return "empty";
		}
	};

	ResourceFile RELATIVE = new ResourceFile() {
		@Override
		public String toString() {
			return "relative";
		}
	};

	/**
	 * {@code Directory}.
	 */
	public record Directory(Map<String, ResourcePath> children) implements ResourceFile {
	}

	/**
	 * {@code File}.
	 */
	public record File(Path contents) implements ResourceFile {
	}
}

package net.minecraft.util.path;

import java.nio.file.Path;

/**
 * {@code SymlinkEntry}.
 */
public record SymlinkEntry(Path link, Path target) {
}

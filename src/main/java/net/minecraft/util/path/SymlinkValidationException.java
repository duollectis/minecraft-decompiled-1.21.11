package net.minecraft.util.path;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Исключение, выбрасываемое при обнаружении запрещённых символьных ссылок
 * в директории ресурс-пака. Содержит список всех нарушений для диагностики.
 */
public class SymlinkValidationException extends Exception {

	private final Path path;
	private final List<SymlinkEntry> symlinks;

	public SymlinkValidationException(Path path, List<SymlinkEntry> symlinks) {
		this.path = path;
		this.symlinks = symlinks;
	}

	@Override
	public String getMessage() {
		return getMessage(path, symlinks);
	}

	public static String getMessage(Path path, List<SymlinkEntry> symlinks) {
		return "Failed to validate '"
				+ path
				+ "'. Found forbidden symlinks: "
				+ symlinks.stream()
						.map(symlink -> symlink.link() + "->" + symlink.target())
						.collect(Collectors.joining(", "));
	}
}

package net.minecraft.data;

import com.google.common.hash.HashCode;
import net.minecraft.util.path.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code DataWriter}.
 */
public interface DataWriter {

	DataWriter UNCACHED = (path, data, hashCode) -> {
		PathUtil.createDirectories(path.getParent());
		Files.write(path, data);
	};

	void write(Path path, byte[] data, HashCode hashCode) throws IOException;
}

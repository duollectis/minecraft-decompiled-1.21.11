package net.minecraft.util.path;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.slf4j.Logger;

public class PathResolving {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Path toPath(URI uri) throws IOException {
      try {
         return Paths.get(uri);
      } catch (FileSystemNotFoundException var3) {
      } catch (Throwable var4) {
         LOGGER.warn("Unable to get path for: {}", uri, var4);
      }

      try {
         FileSystems.newFileSystem(uri, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException var2) {
      }

      return Paths.get(uri);
   }
}

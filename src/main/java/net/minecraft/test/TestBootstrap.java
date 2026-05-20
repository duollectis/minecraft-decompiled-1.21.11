package net.minecraft.test;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.Bootstrap;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class TestBootstrap {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_UNIVERSE = "gametestserver";
   private static final String DEFAULT_WORLD = "gametestworld";
   private static final OptionParser PARSER = new OptionParser();
   private static final OptionSpec<String> UNIVERSE = PARSER.accepts(
         "universe", "The path to where the test server world will be created. Any existing folder will be replaced."
      )
      .withRequiredArg()
      .defaultsTo("gametestserver", new String[0]);
   private static final OptionSpec<File> REPORT = PARSER.accepts("report", "Exports results in a junit-like XML report at the given path.")
      .withRequiredArg()
      .ofType(File.class);
   private static final OptionSpec<String> TESTS = PARSER.accepts(
         "tests", "Which test(s) to run (namespaced ID selector using wildcards). Empty means run all."
      )
      .withRequiredArg();
   private static final OptionSpec<Boolean> VERIFY = PARSER.accepts(
         "verify", "Runs the tests specified with `test` or `testNamespace` 100 times for each 90 degree rotation step"
      )
      .withRequiredArg()
      .ofType(Boolean.class)
      .defaultsTo(false, new Boolean[0]);
   private static final OptionSpec<String> PACKS = PARSER.accepts("packs", "A folder of datapacks to include in the world").withRequiredArg();
   private static final OptionSpec<Void> HELP = PARSER.accepts("help").forHelp();

   @SuppressLinter(reason = "Using System.err due to no bootstrap")
   public static void run(String[] args, Consumer<String> universeCallback) throws Exception {
      PARSER.allowsUnrecognizedOptions();
      OptionSet optionSet = PARSER.parse(args);
      if (optionSet.has(HELP)) {
         PARSER.printHelpOn(System.err);
      } else {
         if ((Boolean)optionSet.valueOf(VERIFY) && !optionSet.has(TESTS)) {
            LOGGER.error("Please specify a test selection to run the verify option. For example: --verify --tests example:test_something_*");
            System.exit(-1);
         }

         LOGGER.info("Running GameTestMain with cwd '{}', universe path '{}'", System.getProperty("user.dir"), optionSet.valueOf(UNIVERSE));
         if (optionSet.has(REPORT)) {
            TestFailureLogger.setCompletionListener(new XmlReportingTestCompletionListener((File)REPORT.value(optionSet)));
         }

         Bootstrap.initialize();
         Util.startTimerHack();
         String string = (String)optionSet.valueOf(UNIVERSE);
         empty(string);
         universeCallback.accept(string);
         if (optionSet.has(PACKS)) {
            String string2 = (String)optionSet.valueOf(PACKS);
            copyPacks(string, string2);
         }

         LevelStorage.Session session = LevelStorage.create(Paths.get(string)).createSessionWithoutSymlinkCheck("gametestworld");
         ResourcePackManager resourcePackManager = VanillaDataPackProvider.createManager(session);
         MinecraftServer.startServer(thread -> TestServer.create(thread, session, resourcePackManager, get(optionSet, TESTS), optionSet.has(VERIFY)));
      }
   }

   private static Optional<String> get(OptionSet options, OptionSpec<String> option) {
      return options.has(option) ? Optional.of((String)options.valueOf(option)) : Optional.empty();
   }

   private static void empty(String path) throws IOException {
      Path path2 = Paths.get(path);
      if (Files.exists(path2)) {
         FileUtils.deleteDirectory(path2.toFile());
      }

      Files.createDirectories(path2);
   }

   private static void copyPacks(String universe, String packDir) throws IOException {
      Path path = Paths.get(universe).resolve("gametestworld").resolve("datapacks");
      if (!Files.exists(path)) {
         Files.createDirectories(path);
      }

      Path path2 = Paths.get(packDir);
      if (Files.exists(path2)) {
         try (Stream<Path> stream = Files.list(path2)) {
            for (Path path3 : stream.toList()) {
               Path path4 = path.resolve(path3.getFileName());
               if (Files.isDirectory(path3)) {
                  if (Files.isRegularFile(path3.resolve("pack.mcmeta"))) {
                     FileUtils.copyDirectory(path3.toFile(), path4.toFile());
                     LOGGER.info("Included folder pack {}", path3.getFileName());
                  }
               } else if (path3.toString().endsWith(".zip")) {
                  Files.copy(path3, path4);
                  LOGGER.info("Included zip pack {}", path3.getFileName());
               }
            }
         }
      }
   }
}

package net.minecraft.client.main;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.util.UndashedUuid;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.telemetry.GameLoadTimeEvent;
import net.minecraft.client.session.telemetry.TelemetryEventProperty;
import net.minecraft.client.util.GlException;
import net.minecraft.client.util.tracy.TracyLoader;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Nullables;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.WinNativeModuleUtil;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.util.profiling.jfr.InstanceType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code Main}.
 */
public class Main {

	@DontObfuscate
	public static void main(String[] args) {
		OptionParser optionParser = new OptionParser();
		optionParser.allowsUnrecognizedOptions();
		optionParser.accepts("demo");
		optionParser.accepts("disableMultiplayer");
		optionParser.accepts("disableChat");
		optionParser.accepts("fullscreen");
		optionParser.accepts("checkGlErrors");
		OptionSpec<Void> optionSpec = optionParser.accepts("renderDebugLabels");
		OptionSpec<Void> optionSpec2 = optionParser.accepts("jfrProfile");
		OptionSpec<Void> optionSpec3 = optionParser.accepts("tracy");
		OptionSpec<Void> optionSpec4 = optionParser.accepts("tracyNoImages");
		OptionSpec<String> optionSpec5 = optionParser.accepts("quickPlayPath").withRequiredArg();
		OptionSpec<String> optionSpec6 = optionParser.accepts("quickPlaySingleplayer").withOptionalArg();
		OptionSpec<String> optionSpec7 = optionParser.accepts("quickPlayMultiplayer").withRequiredArg();
		OptionSpec<String> optionSpec8 = optionParser.accepts("quickPlayRealms").withRequiredArg();
		OptionSpec<File>
				optionSpec9 =
				optionParser
						.accepts("gameDir")
						.withRequiredArg()
						.ofType(File.class)
						.defaultsTo(new File("."), new File[0]);
		OptionSpec<File> optionSpec10 = optionParser.accepts("assetsDir").withRequiredArg().ofType(File.class);
		OptionSpec<File> optionSpec11 = optionParser.accepts("resourcePackDir").withRequiredArg().ofType(File.class);
		OptionSpec<String> optionSpec12 = optionParser.accepts("proxyHost").withRequiredArg();
		OptionSpec<Integer>
				optionSpec13 =
				optionParser
						.accepts("proxyPort")
						.withRequiredArg()
						.defaultsTo("8080", new String[0])
						.ofType(Integer.class);
		OptionSpec<String> optionSpec14 = optionParser.accepts("proxyUser").withRequiredArg();
		OptionSpec<String> optionSpec15 = optionParser.accepts("proxyPass").withRequiredArg();
		OptionSpec<String> optionSpec16 = optionParser.accepts("username")
		                                              .withRequiredArg()
		                                              .defaultsTo("Player" + System.currentTimeMillis() % 1000L);
		OptionSpec<Void> optionSpec17 = optionParser.accepts("offlineDeveloperMode");
		OptionSpec<String> optionSpec18 = optionParser.accepts("uuid").withRequiredArg();
		OptionSpec<String> optionSpec19 = optionParser.accepts("xuid").withOptionalArg().defaultsTo("", new String[0]);
		OptionSpec<String>
				optionSpec20 =
				optionParser.accepts("clientId").withOptionalArg().defaultsTo("", new String[0]);
		OptionSpec<String> optionSpec21 = optionParser.accepts("accessToken").withRequiredArg().required();
		OptionSpec<String> optionSpec22 = optionParser.accepts("version").withRequiredArg().required();
		OptionSpec<Integer>
				optionSpec23 =
				optionParser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(854, new Integer[0]);
		OptionSpec<Integer>
				optionSpec24 =
				optionParser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(480, new Integer[0]);
		OptionSpec<Integer>
				optionSpec25 =
				optionParser.accepts("fullscreenWidth").withRequiredArg().ofType(Integer.class);
		OptionSpec<Integer>
				optionSpec26 =
				optionParser.accepts("fullscreenHeight").withRequiredArg().ofType(Integer.class);
		OptionSpec<String> optionSpec27 = optionParser.accepts("assetIndex").withRequiredArg();
		OptionSpec<String>
				optionSpec28 =
				optionParser.accepts("versionType").withRequiredArg().defaultsTo("release", new String[0]);
		OptionSpec<String> optionSpec29 = optionParser.nonOptions();
		OptionSet optionSet = optionParser.parse(args);
		File file = getOption(optionSet, optionSpec9);
		String string = getOption(optionSet, optionSpec22);
		String string2 = "Pre-bootstrap";

		Logger logger;
		RunArgs runArgs;
		try {
			if (optionSet.has(optionSpec2)) {
				FlightProfiler.INSTANCE.start(InstanceType.CLIENT);
			}

			if (optionSet.has(optionSpec3)) {
				TracyLoader.load();
			}

			Stopwatch stopwatch = Stopwatch.createStarted(Ticker.systemTicker());
			Stopwatch stopwatch2 = Stopwatch.createStarted(Ticker.systemTicker());
			GameLoadTimeEvent.INSTANCE.addTimer(TelemetryEventProperty.LOAD_TIME_TOTAL_TIME_MS, stopwatch);
			GameLoadTimeEvent.INSTANCE.addTimer(TelemetryEventProperty.LOAD_TIME_PRE_WINDOW_MS, stopwatch2);
			SharedConstants.createGameVersion();
			TracyClient.reportAppInfo("Minecraft Java Edition " + SharedConstants.getGameVersion().name());
			CompletableFuture<?> completableFuture = Schemas.optimize(DataFixTypes.REQUIRED_TYPES);
			CrashReport.initCrashReport();
			logger = LogUtils.getLogger();
			string2 = "Bootstrap";
			Bootstrap.initialize();
			ClientBootstrap.initialize();
			GameLoadTimeEvent.INSTANCE.setBootstrapTime(Bootstrap.LOAD_TIME.get());
			Bootstrap.logMissing();
			string2 = "Argument parsing";
			List<String> list = optionSet.valuesOf(optionSpec29);
			if (!list.isEmpty()) {
				logger.info("Completely ignored arguments: {}", list);
			}

			String string3 = getOption(optionSet, optionSpec12);
			Proxy proxy = Proxy.NO_PROXY;
			if (string3 != null) {
				try {
					proxy = new Proxy(Type.SOCKS, new InetSocketAddress(string3, getOption(optionSet, optionSpec13)));
				}
				catch (Exception var74) {
				}
			}

			final String string4 = getOption(optionSet, optionSpec14);
			final String string5 = getOption(optionSet, optionSpec15);
			if (!proxy.equals(Proxy.NO_PROXY) && isNotNullOrEmpty(string4) && isNotNullOrEmpty(string5)) {
				Authenticator.setDefault(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(string4, string5.toCharArray());
					}
				});
			}

			int i = getOption(optionSet, optionSpec23);
			int j = getOption(optionSet, optionSpec24);
			OptionalInt optionalInt = toOptional(getOption(optionSet, optionSpec25));
			OptionalInt optionalInt2 = toOptional(getOption(optionSet, optionSpec26));
			boolean bl = optionSet.has("fullscreen");
			boolean bl2 = optionSet.has("demo");
			boolean bl3 = optionSet.has("disableMultiplayer");
			boolean bl4 = optionSet.has("disableChat");
			boolean bl5 = !optionSet.has(optionSpec4);
			boolean bl6 = optionSet.has(optionSpec);
			String string6 = getOption(optionSet, optionSpec28);
			File file2 = optionSet.has(optionSpec10) ? getOption(optionSet, optionSpec10) : new File(file, "assets/");
			File
					file3 =
					optionSet.has(optionSpec11) ? getOption(optionSet, optionSpec11) : new File(file, "resourcepacks/");
			UUID uUID = isUuidSetAndValid(optionSpec18, optionSet, logger)
			            ? UndashedUuid.fromStringLenient((String) optionSpec18.value(optionSet))
			            : Uuids.getOfflinePlayerUuid((String) optionSpec16.value(optionSet));
			String string7 = optionSet.has(optionSpec27) ? (String) optionSpec27.value(optionSet) : null;
			String string8 = (String) optionSet.valueOf(optionSpec19);
			String string9 = (String) optionSet.valueOf(optionSpec20);
			String string10 = getOption(optionSet, optionSpec5);
			RunArgs.QuickPlayVariant
					quickPlayVariant =
					getQuickPlayVariant(optionSet, optionSpec6, optionSpec7, optionSpec8);
			Session session = new Session(
					(String) optionSpec16.value(optionSet),
					uUID,
					(String) optionSpec21.value(optionSet),
					toOptional(string8),
					toOptional(string9)
			);
			runArgs = new RunArgs(
					new RunArgs.Network(session, proxy),
					new WindowSettings(i, j, optionalInt, optionalInt2, bl),
					new RunArgs.Directories(file, file3, file2, string7),
					new RunArgs.Game(bl2, string, string6, bl3, bl4, bl5, bl6, optionSet.has(optionSpec17)),
					new RunArgs.QuickPlay(string10, quickPlayVariant)
			);
			Util.startTimerHack();
			completableFuture.join();
		}
		catch (Throwable var75) {
			CrashReport crashReport = CrashReport.create(var75, string2);
			CrashReportSection crashReportSection = crashReport.addElement("Initialization");
			WinNativeModuleUtil.addDetailTo(crashReportSection);
			MinecraftClient.addSystemDetailsToCrashReport(null, null, string, null, crashReport);
			MinecraftClient.printCrashReport(null, file, crashReport);
			return;
		}

		Thread thread = new Thread("Client Shutdown Thread") {
			@Override
			public void run() {
				MinecraftClient minecraftClient = MinecraftClient.getInstance();
				if (minecraftClient != null) {
					IntegratedServer integratedServer = minecraftClient.getServer();
					if (integratedServer != null) {
						integratedServer.stop(true);
					}
				}
			}
		};
		thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(logger));
		Runtime.getRuntime().addShutdownHook(thread);
		MinecraftClient minecraftClient = null;

		try {
			Thread.currentThread().setName("Render thread");
			RenderSystem.initRenderThread();
			minecraftClient = new MinecraftClient(runArgs);
		}
		catch (GlException var72) {
			Util.shutdownExecutors();
			logger.warn("Failed to create window: ", var72);
			return;
		}
		catch (Throwable var73) {
			CrashReport crashReport2 = CrashReport.create(var73, "Initializing game");
			CrashReportSection crashReportSection2 = crashReport2.addElement("Initialization");
			WinNativeModuleUtil.addDetailTo(crashReportSection2);
			MinecraftClient.addSystemDetailsToCrashReport(
					minecraftClient,
					null,
					runArgs.game.version,
					null,
					crashReport2
			);
			MinecraftClient.printCrashReport(minecraftClient, runArgs.directories.runDir, crashReport2);
			return;
		}

		MinecraftClient minecraftClient2 = minecraftClient;
		minecraftClient.run();

		try {
			minecraftClient2.scheduleStop();
		}
		finally {
			minecraftClient.stop();
		}
	}

	private static RunArgs.QuickPlayVariant getQuickPlayVariant(
			OptionSet optionSet,
			OptionSpec<String> worldIdOption,
			OptionSpec<String> serverAddressOption,
			OptionSpec<String> realmIdOption
	) {
		long l = Stream.of(worldIdOption, serverAddressOption, realmIdOption).filter(optionSet::has).count();
		if (l == 0L) {
			return RunArgs.QuickPlayVariant.DEFAULT;
		}
		else if (l > 1L) {
			throw new IllegalArgumentException("Only one quick play option can be specified");
		}
		else if (optionSet.has(worldIdOption)) {
			String string = unescape(getOption(optionSet, worldIdOption));
			return new RunArgs.SingleplayerQuickPlay(string);
		}
		else if (optionSet.has(serverAddressOption)) {
			String string = unescape(getOption(optionSet, serverAddressOption));
			return Nullables.mapOrElse(string, RunArgs.MultiplayerQuickPlay::new, RunArgs.QuickPlayVariant.DEFAULT);
		}
		else if (optionSet.has(realmIdOption)) {
			String string = unescape(getOption(optionSet, realmIdOption));
			return Nullables.mapOrElse(string, RunArgs.RealmsQuickPlay::new, RunArgs.QuickPlayVariant.DEFAULT);
		}
		else {
			return RunArgs.QuickPlayVariant.DEFAULT;
		}
	}

	private static @Nullable String unescape(@Nullable String string) {
		return string == null ? null : StringEscapeUtils.unescapeJava(string);
	}

	private static Optional<String> toOptional(String string) {
		return string.isEmpty() ? Optional.empty() : Optional.of(string);
	}

	private static OptionalInt toOptional(@Nullable Integer i) {
		return i != null ? OptionalInt.of(i) : OptionalInt.empty();
	}

	private static <T> @Nullable T getOption(OptionSet optionSet, OptionSpec<T> optionSpec) {
		try {
			return (T) optionSet.valueOf(optionSpec);
		}
		catch (Throwable var5) {
			if (optionSpec instanceof ArgumentAcceptingOptionSpec<T> argumentAcceptingOptionSpec) {
				List<T> list = argumentAcceptingOptionSpec.defaultValues();
				if (!list.isEmpty()) {
					return list.get(0);
				}
			}

			throw var5;
		}
	}

	private static boolean isNotNullOrEmpty(@Nullable String s) {
		return s != null && !s.isEmpty();
	}

	private static boolean isUuidSetAndValid(OptionSpec<String> uuidOption, OptionSet optionSet, Logger logger) {
		return optionSet.has(uuidOption) && isUuidValid(uuidOption, optionSet, logger);
	}

	private static boolean isUuidValid(OptionSpec<String> uuidOption, OptionSet optionSet, Logger logger) {
		try {
			UndashedUuid.fromStringLenient((String) uuidOption.value(optionSet));
			return true;
		}
		catch (IllegalArgumentException var4) {
			logger.warn("Invalid UUID: '{}", uuidOption.value(optionSet));
			return false;
		}
	}

	static {
		System.setProperty("java.awt.headless", "true");
	}
}

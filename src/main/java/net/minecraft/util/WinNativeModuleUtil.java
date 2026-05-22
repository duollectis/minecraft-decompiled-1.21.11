package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.minecraft.util.crash.CrashReportSection;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * Утилита для сбора информации о нативных модулях Windows через JNA.
 * Используется для диагностики в отчётах о сбоях.
 */
public class WinNativeModuleUtil {

	private static final Logger LOGGER = LogUtils.getLogger();

	// Коды ошибок Win32: файл ресурсов не найден / тип ресурса не найден
	private static final int ERROR_RESOURCE_DATA_NOT_FOUND = 1812;
	private static final int ERROR_RESOURCE_TYPE_NOT_FOUND = 1813;

	private static final int CODE_PAGE_MASK = 0xFFFF;
	private static final int LANGUAGE_ID_SHIFT = 16;
	private static final int EN_US_CODE_PAGE = 1033;
	// Языковой идентификатор английского (США): 0x0409 << 16 = 0x04090000
	private static final int LANGUAGE_ID = 0x04B00000;

	/**
	 * Собирает список нативных модулей текущего процесса Windows.
	 * На не-Windows платформах возвращает пустой список.
	 *
	 * @return список нативных модулей
	 */
	public static List<NativeModule> collectNativeModules() {
		if (!Platform.isWindows()) {
			return ImmutableList.of();
		}

		int processId = Kernel32.INSTANCE.GetCurrentProcessId();
		Builder<NativeModule> builder = ImmutableList.builder();

		for (MODULEENTRY32W entry : Kernel32Util.getModules(processId)) {
			String moduleName = entry.szModule();
			Optional<NativeModuleInfo> info = createNativeModuleInfo(entry.szExePath());
			builder.add(new NativeModule(moduleName, info));
		}

		return builder.build();
	}

	private static Optional<NativeModuleInfo> createNativeModuleInfo(String path) {
		try {
			IntByReference dummy = new IntByReference();
			int infoSize = Version.INSTANCE.GetFileVersionInfoSize(path, dummy);

			if (infoSize == 0) {
				int errorCode = Native.getLastError();

				if (errorCode == ERROR_RESOURCE_TYPE_NOT_FOUND || errorCode == ERROR_RESOURCE_DATA_NOT_FOUND) {
					return Optional.empty();
				}

				throw new Win32Exception(errorCode);
			}

			Pointer versionInfo = new Memory(infoSize);

			if (!Version.INSTANCE.GetFileVersionInfo(path, 0, infoSize, versionInfo)) {
				throw new Win32Exception(Native.getLastError());
			}

			IntByReference lengthRef = new IntByReference();
			Pointer translationsPointer = query(versionInfo, "\\VarFileInfo\\Translation", lengthRef);
			int[] translations = translationsPointer.getIntArray(0L, lengthRef.getValue() / 4);
			OptionalInt translationIndex = getEnglishTranslationIndex(translations);

			if (translationIndex.isEmpty()) {
				return Optional.empty();
			}

			int translation = translationIndex.getAsInt();
			int languageId = translation & CODE_PAGE_MASK;
			int codePage = (translation & ~CODE_PAGE_MASK) >> LANGUAGE_ID_SHIFT;

			String description = queryString(versionInfo, getStringFileInfoPath("FileDescription", languageId, codePage), lengthRef);
			String companyName = queryString(versionInfo, getStringFileInfoPath("CompanyName", languageId, codePage), lengthRef);
			String fileVersion = queryString(versionInfo, getStringFileInfoPath("FileVersion", languageId, codePage), lengthRef);

			return Optional.of(new NativeModuleInfo(description, fileVersion, companyName));
		}
		catch (Exception exception) {
			LOGGER.info("Failed to find module info for {}", path, exception);
			return Optional.empty();
		}
	}

	private static String getStringFileInfoPath(String key, int languageId, int codePage) {
		return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", languageId, codePage, key);
	}

	private static OptionalInt getEnglishTranslationIndex(int[] indices) {
		OptionalInt fallback = OptionalInt.empty();

		for (int index : indices) {
			if ((index & ~CODE_PAGE_MASK) == LANGUAGE_ID && (index & CODE_PAGE_MASK) == EN_US_CODE_PAGE) {
				return OptionalInt.of(index);
			}

			fallback = OptionalInt.of(index);
		}

		return fallback;
	}

	private static Pointer query(Pointer pointer, String path, IntByReference lengthPointer) {
		PointerByReference result = new PointerByReference();

		if (!Version.INSTANCE.VerQueryValue(pointer, path, result, lengthPointer)) {
			throw new UnsupportedOperationException("Can't get version value " + path);
		}

		return result.getValue();
	}

	private static String queryString(Pointer pointer, String path, IntByReference lengthPointer) {
		try {
			Pointer stringPointer = query(pointer, path, lengthPointer);
			byte[] bytes = stringPointer.getByteArray(0L, (lengthPointer.getValue() - 1) * 2);
			return new String(bytes, StandardCharsets.UTF_16LE);
		}
		catch (Exception ignored) {
			return "";
		}
	}

	/**
	 * Добавляет в секцию отчёта о сбое список нативных модулей Windows,
	 * отсортированных по имени.
	 *
	 * @param section секция отчёта о сбое
	 */
	public static void addDetailTo(CrashReportSection section) {
		section.add(
				"Modules",
				() -> collectNativeModules()
						.stream()
						.sorted(Comparator.comparing(module -> module.path))
						.map(module -> "\n\t\t" + module)
						.collect(Collectors.joining())
		);
	}

	public static class NativeModule {

		public final String path;
		public final Optional<NativeModuleInfo> info;

		public NativeModule(String path, Optional<NativeModuleInfo> info) {
			this.path = path;
			this.info = info;
		}

		@Override
		public String toString() {
			return info.<String>map(moduleInfo -> path + ":" + moduleInfo).orElse(path);
		}
	}

	public static class NativeModuleInfo {

		public final String fileDescription;
		public final String fileVersion;
		public final String companyName;

		public NativeModuleInfo(String fileDescription, String fileVersion, String companyName) {
			this.fileDescription = fileDescription;
			this.fileVersion = fileVersion;
			this.companyName = companyName;
		}

		@Override
		public String toString() {
			return fileDescription + ":" + fileVersion + ":" + companyName;
		}
	}
}

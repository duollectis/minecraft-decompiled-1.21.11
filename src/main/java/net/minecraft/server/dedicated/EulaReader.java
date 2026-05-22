package net.minecraft.server.dedicated;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.util.Urls;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Читатель и валидатор файла EULA: проверяет принятие лицензионного соглашения Minecraft.
 */
public class EulaReader {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final Path eulaFile;
	private final boolean eulaAgreedTo;

	public EulaReader(Path eulaFile) {
		this.eulaFile = eulaFile;
		this.eulaAgreedTo = SharedConstants.isDevelopment || this.checkEulaAgreement();
	}

	private boolean checkEulaAgreement() {
		try {
			boolean var3;
			try (InputStream inputStream = Files.newInputStream(this.eulaFile)) {
				Properties properties = new Properties();
				properties.load(inputStream);
				var3 = Boolean.parseBoolean(properties.getProperty("eula", "false"));
			}

			return var3;
		}
		catch (Exception var6) {
			LOGGER.warn("Failed to load {}", this.eulaFile);
			this.createEulaFile();
			return false;
		}
	}

	public boolean isEulaAgreedTo() {
		return this.eulaAgreedTo;
	}

	private void createEulaFile() {
		if (!SharedConstants.isDevelopment) {
			try (OutputStream outputStream = Files.newOutputStream(this.eulaFile)) {
				Properties properties = new Properties();
				properties.setProperty("eula", "false");
				properties.store(outputStream,
						"By changing the setting below to TRUE you are indicating your agreement to our EULA ("
								+ Urls.EULA + ")."
				);
			}
			catch (Exception var6) {
				LOGGER.warn("Failed to save {}", this.eulaFile, var6);
			}
		}
	}
}

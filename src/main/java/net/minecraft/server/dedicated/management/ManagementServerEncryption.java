package net.minecraft.server.dedicated.management;

import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * {@code ManagementServerEncryption}.
 */
public class ManagementServerEncryption {

	private static final String PASSWORD_ENVIRONMENT_VARIABLE_NAME = "MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD";
	private static final String PASSWORD_PROPERTY_NAME = "management.tls.keystore.password";
	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Создаёт context.
	 *
	 * @param keystore keystore
	 * @param password password
	 *
	 * @return SslContext — результат операции
	 */
	public static SslContext createContext(String keystore, String password) throws Exception {
		if (keystore.isEmpty()) {
			throw new IllegalArgumentException("TLS is enabled but keystore is not configured");
		}
		else {
			File file = new File(keystore);
			if (file.exists() && file.isFile()) {
				String string = getKeystorePassword(password);
				return createContext(file, string);
			}
			else {
				throw new IllegalArgumentException(
						"Supplied keystore is not a file or does not exist: '" + keystore + "'");
			}
		}
	}

	private static String getKeystorePassword(String fallback) {
		String string = System.getenv().get("MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD");
		if (string != null) {
			return string;
		}
		else {
			String string2 = System.getProperty("management.tls.keystore.password", null);
			return string2 != null ? string2 : fallback;
		}
	}

	private static SslContext createContext(File keystore, String password) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");

		try (InputStream inputStream = new FileInputStream(keystore)) {
			keyStore.load(inputStream, password.toCharArray());
		}

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, password.toCharArray());
		TrustManagerFactory
				trustManagerFactory =
				TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		return SslContextBuilder.forServer(keyManagerFactory).trustManager(trustManagerFactory).build();
	}

	/**
	 * Логирует instructions.
	 */
	public static void logInstructions() {
		LOGGER.info("To use TLS for the management server, please follow these steps:");
		LOGGER.info("1. Set the server property 'management-server-tls-enabled' to 'true' to enable TLS");
		LOGGER.info("2. Create a keystore file of type PKCS12 containing your server certificate and private key");
		LOGGER.info("3. Set the server property 'management-server-tls-keystore' to the path of your keystore file");
		LOGGER.info(
				"4. Set the keystore password via the environment variable 'MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD', or system property 'management.tls.keystore.password', or server property 'management-server-tls-keystore-password'"
		);
		LOGGER.info("5. Restart the server to apply the changes.");
	}
}

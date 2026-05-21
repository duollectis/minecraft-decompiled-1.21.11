package net.minecraft.client.realms.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code UploadResult}.
 */
public record UploadResult(int statusCode, @Nullable String errorMessage) {

	public @Nullable String getErrorMessage() {
		if (this.statusCode >= 200 && this.statusCode < 300) {
			return null;
		}
		else {
			return this.statusCode == 400 && this.errorMessage != null ? this.errorMessage
			                                                           : String.valueOf(this.statusCode);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private int statusCode = -1;
		private @Nullable String errorMessage;

		public UploadResult.Builder withStatusCode(int statusCode) {
			this.statusCode = statusCode;
			return this;
		}

		public UploadResult.Builder withErrorMessage(@Nullable String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
		}

		/**
		 * Build.
		 *
		 * @return UploadResult — результат операции
		 */
		public UploadResult build() {
			return new UploadResult(this.statusCode, this.errorMessage);
		}
	}
}

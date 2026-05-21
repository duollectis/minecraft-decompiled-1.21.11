package net.minecraft.server.dedicated.management.network;

/**
 * {@code ManagementConnectionId}.
 */
public record ManagementConnectionId(Integer connectionId) {

	/**
	 * Of.
	 *
	 * @param connectionId connection id
	 *
	 * @return ManagementConnectionId — результат операции
	 */
	public static ManagementConnectionId of(Integer connectionId) {
		return new ManagementConnectionId(connectionId);
	}
}

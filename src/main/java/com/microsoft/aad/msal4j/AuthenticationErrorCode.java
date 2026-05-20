package com.microsoft.aad.msal4j;

public class AuthenticationErrorCode {
   public static final String AUTHORIZATION_PENDING = "authorization_pending";
   public static final String CODE_EXPIRED = "code_expired";
   public static final String INVALID_GRANT = "invalid_grant";
   public static final String WSTRUST_ENDPOINT_NOT_FOUND_IN_METADATA_DOCUMENT = "wstrust_endpoint_not_found";
   public static final String WSTRUST_INVALID_RESPONSE = "wstrust_invalid_response";
   public static final String WSTRUST_SERVICE_ERROR = "wstrust_service_error";
   public static final String PASSWORD_REQUIRED_FOR_MANAGED_USER = "password_required_for_managed_user";
   public static final String USER_REALM_DISCOVERY_FAILED = "user_realm_discovery_failed";
   public static final String CACHE_MISS = "cache_miss";
   public static final String INVALID_INSTANCE_DISCOVERY_METADATA = "invalid_instance_discovery_metadata";
   public static final String UNKNOWN = "unknown";
   public static final String LOOPBACK_REDIRECT_URI = "loopback_redirect_uri";
   public static final String UNABLE_TO_START_HTTP_LISTENER = "unable_to_start_http_listener";
   public static final String INVALID_AUTHORIZATION_RESULT = "invalid_authorization_result";
   public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";
   public static final String INVALID_ENDPOINT_URI = "invalid_endpoint_uri";
   public static final String DESKTOP_BROWSER_NOT_SUPPORTED = "desktop_browser_not_supported";
   public static final String THROTTLED_REQUEST = "throttled_request";
   public static final String INVALID_JSON = "invalid_json";
   public static final String INVALID_JWT = "invalid_jwt";
   public static final String MISSING_BROKER = "missing_broker";
   public static final String HTTP_TIMEOUT = "http_timeout";
   public static final String MSALRUNTIME_INTEROP_ERROR = "interop_package_error";
   public static final String MSALJAVA_BROKERS_ERROR = "brokers_package_error";
   public static final String MANAGED_IDENTITY_REQUEST_FAILED = "managed_identity_request_failed";
   public static final String CRYPTO_ERROR = "crypto_error";
   public static final String INVALID_TIMESTAMP_FORMAT = "invalid_timestamp_format";
}

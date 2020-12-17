package kofu

class ApplicationProperties(
    val message: String,
    val item: String,
    val mail: Mail = Mail(),
    val http: Http = Http(),
    val cache: Cache = Cache(),
    val security: Security = Security()
) {
    class Mail(
        var isEnabled: Boolean = false,
        var from: String = "",
        var baseUrl: String = ""
    )

    class Http(val cache: Cache = Cache()) {
        class Cache(var timeToLiveInDays: Int = 1461)
    }

    class Cache(val ehcache: Ehcache = Ehcache()) {
        class Ehcache(
            var timeToLiveSeconds: Int = 3600,
            var maxEntries: Long = 100
        )
    }

    class Security(
        val rememberMe: RememberMe = RememberMe(),
        val authentication: Authentication = Authentication(),
        val clientAuthorization: ClientAuthorization = ClientAuthorization()
    ) {
        class RememberMe(var key: String? = null)

        class Authentication(val jwt: Jwt = Jwt()) {
            class Jwt(
                var tokenValidityInSecondsForRememberMe: Long = 2592000,
                var tokenValidityInSeconds: Long = 1800,
                var base64Secret: String? = null,
                var secret: String? = null
            )
        }

        class ClientAuthorization(
            var accessTokenUri: String? = null,
            var tokenServiceId: String? = null,
            var clientId: String? = null,
            var clientSecret: String? = null
        )
    }
}
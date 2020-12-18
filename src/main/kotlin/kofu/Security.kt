package kofu

import kofu.AppLogger.log
import java.security.Key
import org.springframework.beans.factory.InitializingBean
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import java.util.Date

import org.springframework.security.core.context.ReactiveSecurityContextHolder.getContext
import org.springframework.util.StringUtils
import org.springframework.util.StringUtils.hasLength
import org.springframework.util.StringUtils.isEmpty
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import java.nio.charset.StandardCharsets.UTF_8


const val ROLE_ADMIN: String = "ADMIN"
const val ROLE_USER: String = "USER"
const val ROLE_ANONYMOUS: String = "ANONYMOUS"
const val LOGIN_REGEX: String =
    "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$"
const val SYSTEM_ACCOUNT: String = "system"
const val ANONYMOUS_USER: String = "anonymoususer"
const val DEFAULT_LANGUAGE: String = "en"
private const val AUTHORITIES_KEY = "auth"

class TokenProvider(
    private val applicationProperties: ApplicationProperties,
    private var applicationContext: ApplicationContext
) : InitializingBean, ApplicationContextAware {
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    private var key: Key? = null

    private var tokenValidityInMilliseconds: Long = 0

    private var tokenValidityInMillisecondsForRememberMe: Long = 0

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        log.info(this.javaClass.simpleName + ".afterPropertiesSet called")
        init()
    }

    fun init() {
        val keyBytes: ByteArray
        val secret = applicationProperties.security.authentication.jwt.secret!!
        keyBytes = if (!hasLength(secret)) {
            log.warn("Warning: the JWT key used is not Base64-encoded. We recommend using the `kofu.security.authentication.jwt.base64-secret` key for optimum security.")
            secret.toByteArray(UTF_8)
        } else {
            log.debug("Using a Base64-encoded JWT secret key")
            Decoders.BASE64.decode(applicationProperties.security.authentication.jwt.base64Secret)
        }
        this.key = Keys.hmacShaKeyFor(keyBytes)
        this.tokenValidityInMilliseconds =
            1000 * applicationProperties.security.authentication.jwt.tokenValidityInSeconds
        this.tokenValidityInMillisecondsForRememberMe = 1000 * applicationProperties.security.authentication.jwt
            .tokenValidityInSecondsForRememberMe
    }


    fun createToken(authentication: Authentication, rememberMe: Boolean): String {
        val authorities = authentication.authorities.asSequence()
            .map { it.authority }
            .joinToString(separator = ",")

        val now = Date().time
        val validity = if (rememberMe) {
            Date(now + this.tokenValidityInMillisecondsForRememberMe)
        } else {
            Date(now + this.tokenValidityInMilliseconds)
        }

        return Jwts.builder()
            .setSubject(authentication.name)
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(key, SignatureAlgorithm.HS512)
            .setExpiration(validity)
            .serializeToJsonWith(JacksonSerializer())
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        val authorities = claims[AUTHORITIES_KEY].toString().splitToSequence(",")
            .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }

        val principal = User(claims.subject, "", authorities)

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }


    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken)

            return true
        } catch (e: JwtException) {
            log.info("Invalid JWT token.")
            log.trace("Invalid JWT token trace. $e")
        } catch (e: IllegalArgumentException) {
            log.info("Invalid JWT token.")
            log.trace("Invalid JWT token trace. $e")
        }

        return false
    }

}

class JWTFilter(private val tokenProvider: TokenProvider) : WebFilter {
    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val jwt = resolveToken(exchange.request)
        if (!jwt.isNullOrBlank() && this.tokenProvider.validateToken(jwt)) {
            val authentication = this.tokenProvider.getAuthentication(jwt)
            return chain.filter(exchange)
                .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication))
        }
        return chain.filter(exchange)
    }

    private fun resolveToken(request: ServerHttpRequest): String? {
        val bearerToken = request.headers.getFirst(AUTHORIZATION_HEADER)
        if (!bearerToken.isNullOrBlank() && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }
}


class UserNotActivatedException(message: String, t: Throwable? = null) :
    AuthenticationException(message, t) {
    companion object {
        private const val serialVersionUID = 1L
    }
}

fun getCurrentUserLogin(): Mono<String> =
    getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap { Mono.justOrEmpty(extractPrincipal(it)) }

fun extractPrincipal(authentication: Authentication?): String? =
    if (authentication == null) {
        null
    } else when (val principal = authentication.principal) {
        is UserDetails -> principal.username
        is String -> principal
        else -> null
    }

fun getCurrentUserJWT(): Mono<String> =
    getContext()
        .map(SecurityContext::getAuthentication)
        .filter { it.credentials is String }
        .map { it.credentials as String }

fun isAuthenticated(): Mono<Boolean> =
    getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .map {
            it.map(GrantedAuthority::getAuthority)
                .none { it == ROLE_ANONYMOUS }
        }

fun isCurrentUserInRole(authority: String): Mono<Boolean> =
    getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .map {
            it.map(GrantedAuthority::getAuthority)
                .any { it == authority }
        }


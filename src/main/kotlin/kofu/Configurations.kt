package kofu

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.io.ClassPathResource
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.webflux.security
import org.springframework.fu.kofu.r2dbc.dataR2dbc
import org.springframework.fu.kofu.r2dbc.r2dbc
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User.withUsername
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

val dataConfig = configuration {
    dataR2dbc {
        r2dbc {
            url = "r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1"
        }
    }
    beans {
        bean {
            ConnectionFactoryInitializer().apply {
                setConnectionFactory(ref())
                setDatabasePopulator(ResourceDatabasePopulator(ClassPathResource("db/tables.sql")))
            }
        }
        bean<UserRepository>()
    }

}

val webConfig = configuration {
    beans {
        bean<HelloHandler>()
        bean<ConfHandler>()
        bean<UserHandler>()
        bean(::routes)
    }
    webFlux {
        codecs {
            string()
            jackson()
        }
        security {
            org.springframework.context.support.beans {
                bean<TokenProvider>("tokenProvider")
                bean<JWTFilter>("jwtFilter")
            }
            http {
                authorizeExchange {
                    authorize("/hello", hasAuthority(ROLE_USER))
                    authorize("/conf", hasAuthority(ROLE_ADMIN))
                    authorize("/api/user/**", permitAll)
                }
                csrf { disable() }
                val tokenProvider=ref<TokenProvider>("tokenProvider")
            }
            passwordEncoder = BCryptPasswordEncoder()
            userDetailsService = MapReactiveUserDetailsService(
                withUsername("john")
                    .password("12345")
                    .authorities(ROLE_USER)
                    .build(),
                withUsername("bill")
                    .password("12345")
                    .roles(ROLE_USER, ROLE_ADMIN)
                    .build()
            )
            authenticationManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
                .apply {
                    setPasswordEncoder(passwordEncoder)
                }
        }
        port = if (profiles.contains("test")) 8181 else 8080
    }

}

object AppLogger {
    @JvmStatic
    val log: Logger by lazy { getLogger(AppLogger.javaClass) }
}
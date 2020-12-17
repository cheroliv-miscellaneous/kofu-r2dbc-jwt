package kofu

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class IntegrationTests {

    private val client = WebTestClient.bindToServer().baseUrl("http://localhost:8181").build()

    private lateinit var context: ConfigurableApplicationContext

    @BeforeAll
    fun beforeAll() {
        context = webApp.run(profiles = "test")
    }


    @Test
    fun `Check Properties`() {
        assert(
            "kofu-r2dbc-jwt@localhost" ==
                    context.environment.getProperty("kofu.mail.from")
        )
        assert(
            "http://127.0.0.1:8080" ==
                    context.environment.getProperty("kofu.mail.base-url")
        )
    }

    @Test
    fun `Request base endpoint`() {
        client.get().uri("/").exchange()
            .expectStatus().isUnauthorized
//			.expectStatus().is2xxSuccessful
//			.expectHeader().contentType("text/plain;charset=UTF-8")
    }

    @Test
    fun `Request HTTP API endpoint for listing all users`() {
        client.get().uri("/api/user").exchange()
            .expectStatus().is2xxSuccessful
            .expectHeader().contentType(APPLICATION_JSON_VALUE)
            .expectBodyList<User>()
            .hasSize(3)
    }

    @Test
    fun `Request HTTP API endpoint for getting one specified user`() {
        client.get().uri("/api/user/bclozel").exchange()
            .expectStatus().is2xxSuccessful
            .expectHeader().contentType(APPLICATION_JSON_VALUE)
            .expectBody<User>()
            .isEqualTo(User("bclozel", "Brian", "Clozel"))
    }

    @Test
    fun `Request conf endpoint`() {
        client.get().uri("/conf").exchange()
            .expectStatus().isUnauthorized
//			.expectStatus().is2xxSuccessful
//			.expectHeader().contentType("text/plain;charset=UTF-8")
    }

    @AfterAll
    fun afterAll() {
        context.close()
    }
}
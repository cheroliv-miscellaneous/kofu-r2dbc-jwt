package kofu

import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*

@Suppress("UNUSED_PARAMETER")
class HelloHandler {
    fun hello(request: ServerRequest) =
        ok()
            .contentType(APPLICATION_JSON)
            .bodyValue("hello world!")

}

@Suppress("UNUSED_PARAMETER")
class ConfHandler(private val properties: ApplicationProperties) {
    fun conf(request: ServerRequest) =
        ok()
            .bodyValue(properties.message)
}

@Suppress("UNUSED_PARAMETER")
class UserHandler(private val repository: UserRepository) {
    fun listApi(request: ServerRequest) =
        ok()
            .contentType(APPLICATION_JSON)
            .body(repository.findAll())

    fun userApi(request: ServerRequest) =
        ok()
            .contentType(APPLICATION_JSON)
            .body(repository.findOne(request.pathVariable("login")))
}
package kofu

import org.springframework.web.reactive.function.server.router

fun routes(
    helloHandler: HelloHandler,
    confHandler: ConfHandler,
    userHandler: UserHandler
) = router {
    GET("/hello",  helloHandler::hello)
    GET("/conf", confHandler::conf)
    GET("/api/user", userHandler::listApi)
    GET("/api/user/{login}", userHandler::userApi)
}
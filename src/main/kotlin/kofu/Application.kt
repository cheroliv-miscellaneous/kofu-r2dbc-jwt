package kofu

import org.springframework.fu.kofu.reactiveWebApplication

fun main() {
    webApp.run()
}

val webApp = reactiveWebApplication {
    configurationProperties<ApplicationProperties>(prefix = "kofu")
    enable(dataConfig)
    enable(webConfig)
}


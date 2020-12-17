package kofu

import org.springframework.boot.SpringApplication

interface ProfileConstants {
    companion object {
        const val SPRING_PROFILE_DEVELOPMENT = "dev"
        const val SPRING_PROFILE_TEST = "test"
        const val SPRING_PROFILE_PRODUCTION = "prod"
        const val SPRING_PROFILE_CLOUD = "cloud"
        const val SPRING_PROFILE_HEROKU = "heroku"
        const val SPRING_PROFILE_AWS_ECS = "aws-ecs"
        const val SPRING_PROFILE_AZURE = "azure"
        const val SPRING_PROFILE_SWAGGER = "swagger"
        const val SPRING_PROFILE_NO_LIQUIBASE = "no-liquibase"
        const val SPRING_PROFILE_K8S = "k8s"
    }
}


object DefaultProfileUtil {
    private const val SPRING_PROFILE_DEFAULT = "spring.profiles.default"
    fun addDefaultProfile(app: SpringApplication) {
        val defProperties: MutableMap<String, Any> = HashMap()
        defProperties[SPRING_PROFILE_DEFAULT] = ProfileConstants.SPRING_PROFILE_DEVELOPMENT
        app.setDefaultProperties(defProperties)
    }
}
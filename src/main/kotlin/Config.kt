import java.io.File
import java.util.Properties

object Config {
    private val properties: Properties by lazy {
        val props = Properties()
        val localPropertiesFile = File("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { props.load(it) }
        } else {
            println("⚠️  Warning: local.properties not found!")
            println("   Please copy local.properties.example to local.properties")
            println("   and add your OpenRouter API key.")
        }

        props
    }

    val openRouterApiKey: String
        get() = properties.getProperty("OPENROUTER_API_KEY")
            ?: throw IllegalStateException(
                """
                OpenRouter API key not found!

                Please add OPENROUTER_API_KEY to local.properties file.
                You can copy local.properties.example to local.properties and add your key.

                Get your API key from: https://openrouter.ai/
                """.trimIndent()
            )
}

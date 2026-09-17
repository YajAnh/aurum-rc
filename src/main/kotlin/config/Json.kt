package config

import kotlin.io.path.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class Config(
    @SerialName("duplicate mode")
    val duplicateMode: String,

    @SerialName("remove session after redo")
    val removeSessionAfterRedo: Boolean
)

@Serializable
data class Directories(
    val directories: String
)


fun loadConfig() {
    val json = Json.decodeFromString<Map<String, String>>(
        Path("Config.json").readText()
    )

    config.configurations.putAll(json)
}

fun saveConfig() {
    val json = Json {
        prettyPrint = true
    }

    Path("Config.json").writeText(
        json.encodeToString(config.configurations)
    )
}

fun loadDirectories(){
    val json = Json.decodeFromString<Map<String, String>>(
        Path("src/main/kotlin/json/Directories.json").readText()
    )

    config.addedDirectories.putAll(json)
}

fun saveDirectories(){
    val json = Json {
        prettyPrint = true
    }

    Path("Directories.json").writeText(
        json.encodeToString(config.addedDirectories)
    )
}
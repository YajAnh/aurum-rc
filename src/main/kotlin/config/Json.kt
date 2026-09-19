package config

import kotlin.io.path.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import services.History
import services.historyPath
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class Config(
    @SerialName("duplicate mode")
    val duplicateMode: String,

    @SerialName("remove session after redo")
    val removeSessionAfterRedo: String
)



fun loadConfig() {
    val json = Json.decodeFromString<Map<String, String>>(
        Path("")
            .resolve("src")
            .resolve("main")
            .resolve("kotlin")
            .resolve("json")
            .resolve("Config.json")
            .readText()
    )

     Config.configurations.putAll(json)

}

fun saveConfig() {
    val json = Json {
        prettyPrint = true
    }

    Path("").toAbsolutePath()
        .resolve("src")
        .resolve("main")
        .resolve("kotlin")
        .resolve("json")
        .resolve("Config.json").writeText(
        json.encodeToString(config.configurations)
    )
}

fun loadDirectories(){
    val json = Json.decodeFromString<Map<String, String>>(
        Path("").toAbsolutePath()
            .resolve("src")
            .resolve("main")
            .resolve("kotlin")
            .resolve("json")
            .resolve("Directories.json")
            .readText()
    )

    config.addedDirectories.putAll(json)
}

fun saveDirectories(){
    val json = Json {
        prettyPrint = true
    }

    Path("").toAbsolutePath()
        .resolve("src")
        .resolve("main")
        .resolve("kotlin")
        .resolve("json")
        .resolve("Directories.json")
        .writeText(
        json.encodeToString(config.addedDirectories)
    )
}

fun loadHistory(): History {
    if (!Files.exists(historyPath)) {
        return History()
    }

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    return json.decodeFromString<History>(
        Files.readString(historyPath)
    )
}

fun saveHistory() {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    Files.createDirectories(historyPath.parent)

    Files.writeString(
        historyPath,
        Json.encodeToString(services.History())
    )
}
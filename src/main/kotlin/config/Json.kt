package config

import kotlin.io.path.Path
import kotlinx.serialization.json.Json
import services.History
import services.historyPath
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun loadConfig() {
    val json = Json.decodeFromString<Map<String, String>>(
        Path("")
            .resolve("data")
            .resolve("Config.json")
            .readText()
    )
     configurations.putAll(json)

}

fun saveConfig() {
    val json = Json {
        prettyPrint = true
    }

    Path("").toAbsolutePath()
        .resolve("data")
        .resolve("Config.json").writeText(
        json.encodeToString(configurations)
    )
}

fun loadDirectories(){
    val json = Json.decodeFromString<Map<String, String>>(
        Path("").toAbsolutePath()
            .resolve("data")
            .resolve("Directories.json")
            .readText()
    )

    addedDirectories.putAll(json)
}

fun saveDirectories(){
    val json = Json {
        prettyPrint = true
    }

    Path("").toAbsolutePath()
        .resolve("data")
        .resolve("Directories.json")
        .writeText(
        json.encodeToString(addedDirectories)
    )
}

var history = loadHistory()
    private set

fun loadHistory(): History =
    if (Files.exists(historyPath))
        json.decodeFromString(Files.readString(historyPath))
    else History()

fun saveHistory() {
    Files.createDirectories(historyPath.parent)
    Files.writeString(historyPath, json.encodeToString(history))
}
package config

import kotlin.io.path.Path
import kotlinx.serialization.json.Json
import services.History
import services.HistoryStore
import java.nio.file.Files
import kotlin.io.path.writeText

private val dataDirectory = Path("").toAbsolutePath().resolve("data")
private val configPath = dataDirectory.resolve("Config.json")
private val directoriesPath = dataDirectory.resolve("Directories.json")
private val defaultConfigurations = mapOf(
    "duplicate mode" to "rename",
    "remove session after redo" to "false"
)

fun loadConfig() {
    configurations.clear()
    configurations.putAll(defaultConfigurations)
    if (Files.exists(configPath)) {
        configurations.putAll(Json.decodeFromString(Files.readString(configPath)))
    } else {
        saveConfig()
    }

}

fun saveConfig() {
    val json = Json {
        prettyPrint = true
    }

    Files.createDirectories(dataDirectory)
    configPath.writeText(json.encodeToString(configurations))
}

fun loadDirectories(){
    addedDirectories.clear()
    if (Files.exists(directoriesPath)) {
        addedDirectories.putAll(Json.decodeFromString(Files.readString(directoriesPath)))
    } else {
        saveDirectories()
    }
}

fun saveDirectories(){
    val json = Json {
        prettyPrint = true
    }

    Files.createDirectories(dataDirectory)
    directoriesPath.writeText(json.encodeToString(addedDirectories))
}

fun loadHistory(): History = HistoryStore.load()

package services

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.Files
import java.nio.file.Path


val historyPath: Path = Path.of("").toAbsolutePath()
    .resolve("src")
    .resolve("main")
    .resolve("kotlin")
    .resolve("json")
    .resolve("History.json")

@Serializable
data class History(
    val sessions: MutableList<HistoryEntry> = mutableListOf()
)

@Serializable
data class HistoryEntry(
    val timestamp: String,
    val directory: String,
    val moves: MutableList<Map<String, JsonElement>>
)

fun recordHistory(selectedDirectory: Path, moves: MutableList<Map<String, JsonElement>>){

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    var history = History()

    if (Files.exists(historyPath)) {
        val contents = Files.readString(historyPath)
        history = json.decodeFromString<History>(contents)
    }

    val entry = HistoryEntry(
        timestamp = Clock.System.now().toString(),
        directory = selectedDirectory.toString(),
        moves = moves
    )

    history.sessions.add(entry)
    Files.createDirectories(historyPath.parent)

    Files.writeString(
        historyPath,
        json.encodeToString(history)
    )
}

fun clearHistory() {
    Files.createDirectories(historyPath.parent)

    Files.writeString(
        historyPath,
        Json.encodeToString(services.History())
    )
}

fun removeHistorySession(index: Int) {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    if (!Files.exists(historyPath)) return

    val contents = Files.readString(historyPath)
    val history = json.decodeFromString<History>(contents)

    // Check if index is valid
    if (index >= 0 && index < history.sessions.size) {
        history.sessions.removeAt(index)

        Files.createDirectories(historyPath.parent)
        Files.writeString(
            historyPath,
            json.encodeToString(history)
        )
        echo("Session at index $index removed successfully")
    } else {
        echo("Invalid index: $index. History has ${history.sessions.size} sessions (0-${history.sessions.size - 1})")
    }
}








package services

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("HandleHistory")
private val sessionTimestampFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    time(LocalTime.Format {
        hour()
        char(':')
        minute()
        char(':')
        second()
    })
}

val historyPath: Path = Path.of("").toAbsolutePath()
    .resolve("data")
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

fun formatSessionTimestamp(timestamp: String): String =
    Instant.parse(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(sessionTimestampFormat)

object HistoryStore {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): History =
        if (Files.exists(historyPath)) {
            json.decodeFromString(Files.readString(historyPath))
        } else {
            History()
        }

    fun save(history: History) {
        Files.createDirectories(historyPath.parent)
        Files.writeString(historyPath, json.encodeToString(history))
    }

    fun record(entry: HistoryEntry) {
        val history = load()
        history.sessions.add(entry)
        save(history)
    }
}

fun recordHistory(selectedDirectory: Path, moves: MutableList<Map<String, JsonElement>>) {
    HistoryStore.record(
        HistoryEntry(
            timestamp = Clock.System.now().toString(),
            directory = selectedDirectory.toString(),
            moves = moves
        )
    )
}

fun clearHistory() {
    HistoryStore.save(History())
}

fun removeHistorySession(index: Int) {
    val history = HistoryStore.load()
    if (index in 1..history.sessions.size) {
        history.sessions.removeAt(index - 1)
        HistoryStore.save(history)
        logger.info("Session $index removed successfully")
    } else {
        logger.warn("Invalid index: $index. History has ${history.sessions.size} sessions (1-${history.sessions.size})")
    }
}

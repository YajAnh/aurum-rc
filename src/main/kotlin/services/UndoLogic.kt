package services

import config.loadConfig
import config.loadHistory
import config.saveHistory
import kotlinx.serialization.json.JsonPrimitive
import sorting.handleDuplicates
import java.nio.file.Path
import java.nio.file.Files
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import sorting.duplicatesDryRun

private val logger = LoggerFactory.getLogger("UndoLogic")

fun undoLogic(input: Int) {
    val history = loadHistory()
    if (input < 1 || input > history.sessions.size) {
        logger.warn("Invalid index: $input (valid: 1-${history.sessions.size})")
        return
    }

    val selectedSession = history.sessions[input - 1]
    val directory = Path.of(selectedSession.directory)

    if (!Files.exists(directory)) {
        logger.warn("$directory no longer exists — cannot undo this session")
        return
    }

    val iterator = selectedSession.moves.listIterator()
    for (move in iterator) {
        val newPath = move["new path"]?.jsonPrimitive?.contentOrNull ?: continue
        val originalPath = move["original path"]?.jsonPrimitive?.contentOrNull ?: continue

        val currentLocation = Path.of(newPath)
        val original = Path.of(originalPath)

        if (!Files.exists(currentLocation)) {
            logger.warn("Skipped: file no longer exists -> $currentLocation")
            continue
        }

        Files.createDirectories(original.parent)
        handleDuplicates(original.parent, currentLocation)
        iterator.set(move + ("undo" to JsonPrimitive(true)))
        logger.info("Undo: ${original.fileName} <- $currentLocation")
    }

    loadConfig()
    if (config.configurations["remove session after redo"]?.toBoolean() == true) {
        history.sessions.removeAt(input - 1)
    }

    saveHistory()
}

fun undoLogicDryRun(input: Int) {
    val history = loadHistory()
    if (input < 1 || input > history.sessions.size) {
        logger.warn("Invalid index: $input (valid: 1-${history.sessions.size})")
        return
    }

    val selectedSession = history.sessions[input - 1]
    val directory = Path.of(selectedSession.directory)

    if (!Files.exists(directory)) {
        logger.warn("Preview: $directory no longer exists")
        return
    }

    val iterator = selectedSession.moves.listIterator()
    for (move in iterator) {
        val newPath = move["new path"]?.jsonPrimitive?.contentOrNull ?: continue
        val originalPath = move["original path"]?.jsonPrimitive?.contentOrNull ?: continue

        val currentLocation = Path.of(newPath)
        val original = Path.of(originalPath)

        if (!Files.exists(currentLocation)) {
            logger.warn("Preview Skipped: file no longer exists -> $currentLocation")
            continue
        }

        Files.createDirectories(original.parent)
        duplicatesDryRun(original.parent, currentLocation)
        iterator.set(move + ("undo" to JsonPrimitive(true)))
        logger.info("Preview Undo: ${original.fileName} <- $currentLocation")
    }
}
package services

import config.loadConfig
import config.loadHistory
import config.saveHistory
import sorting.handleDuplicates
import java.nio.file.Path
import java.nio.file.Files
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("UndoLogic")

fun undoLogic(input: Int) {
    val history = loadHistory()
    if (input < 1 || input > history.sessions.size) {
        logger.info("Index error"); return
    }



    val selectedSession = history.sessions[input - 1]
    val directory = Path.of(selectedSession.directory)

    if (!Files.exists(directory)) {
        print("$directory does not exist!!")
        return
    }

    for (move in selectedSession.moves) {
        val source = Path.of(move["new path"].toString()).parent
        val targetParentDirectory = Path.of(move["original path"].toString()).parent
        Files.createDirectories(targetParentDirectory)

        if (!Files.exists(source)) {
            logger.warn("Skipped: file no longer exists -> $source")
            continue
        }

        handleDuplicates(targetParentDirectory, source)
        logger.info("Undo: ${source.fileName} Moved back -> $targetParentDirectory")
    }
    loadConfig()
    if (config.configurations.getValue("remove session after redo").toBoolean()) {
        history.sessions.removeAt(input - 1)
    }

    saveHistory()
}
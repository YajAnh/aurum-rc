package sorting


import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import services.recordHistory
import java.nio.file.Path
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Sorting")

fun sortingLogic(selectedDirectory: Path, sortingRules: Map<String, List<String>>) {
    println("Files in the selected folder: ${selectedDirectory.fileName} will be moved into category subfolders: ")
    for (folderName in sortingRules.keys) {
        logger.info("~ $folderName")
    }

    print("Press enter to continue: "); readln()
    val moves = mutableListOf<Map<String, JsonElement>>()

    Files.list(selectedDirectory).use { stream ->
        for (filePath in stream) {
            if (!filePath.isRegularFile()) continue

            val matchingFolder = sortingRules.entries
                .firstOrNull { (_, extensions) -> filePath.extension.lowercase() in extensions }
                ?.key

            if (matchingFolder == null) continue

            try {
                val destination = selectedDirectory.resolve(matchingFolder)
                Files.createDirectories(destination)
                val finalPath = handleDuplicates(destination, filePath) ?: continue  // skipped → not recorded

                logger.info("Moved: ${filePath.fileName} -> $finalPath")

                moves.add(
                    mapOf(
                        "original path" to JsonPrimitive(filePath.toString()),
                        "new path" to JsonPrimitive(finalPath.toString()),  // ← real path, not assumed path
                        "undo" to JsonPrimitive(false),
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to process ${filePath.fileName}: ${e.message}")
            }
        }
        recordHistory(selectedDirectory, moves)
    }
}


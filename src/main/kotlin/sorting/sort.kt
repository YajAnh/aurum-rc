package sorting


import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import services.recordHistory
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.slf4j.LoggerFactory
import kotlin.io.path.nameWithoutExtension

private val logger = LoggerFactory.getLogger("Sorting")



fun handleDuplicates(destination: Path, filePath: Path): Path? {
    val duplicateMode = config.configurations["duplicate mode"]
    val destinationFile = destination.resolve(filePath.fileName)

    return when (duplicateMode) {
        "rename" -> {
            var counter = 1
            var target = destinationFile
            while (Files.exists(target)) {
                val newName = "${filePath.nameWithoutExtension} Duplicate ($counter).${filePath.extension}"
                target = destination.resolve(newName)
                counter++
            }
            Files.move(filePath, target)
            target
        }
        "skip" -> {
            logger.info("Skipped duplicate: ${filePath.fileName}")
            null
        }
        "overwrite" -> {
            Files.move(filePath, destinationFile, StandardCopyOption.REPLACE_EXISTING)
            logger.info("Duplicate detected: replacing $destinationFile")
            destinationFile
        }
        else -> {
            logger.warn("Unknown duplicate mode: $duplicateMode — skipping ${filePath.fileName}")
            null
        }
    }
}

fun ensureFolderDestination(selectedDirectory: Path,sortingRules: Map<String, List<String>>) {
    val missingDirectories = sortingRules.keys
        .map { folderName -> selectedDirectory.resolve(folderName) }
        .filter { !Files.isDirectory(it) }

    if (missingDirectories.isNotEmpty()) {
        logger.info("Directories are missing!! (note: these are folders in which the sorted files are gonna be moved)")
        for (missingDirectory in missingDirectories) {
            logger.info("Creating... $missingDirectory")

            val result = runCatching {
                Files.createDirectories(missingDirectory)
            }
            result.onSuccess { continue }
                .onFailure { logger.error("Error: Check the folder if any file have the exact name to  ${missingDirectory.fileName}") }
        }
    }
}

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


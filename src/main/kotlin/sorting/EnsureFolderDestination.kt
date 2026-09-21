package sorting

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("Sorting")
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

fun ensureFolderDestinationDryRun(selectedDirectory: Path,sortingRules: Map<String, List<String>>) {
    val missingDirectories = sortingRules.keys
        .map { folderName -> selectedDirectory.resolve(folderName) }
        .filter { !Files.isDirectory(it) }

    if (missingDirectories.isNotEmpty()) {
        logger.info("Directories are missing!! (Currently in a dry run ~ Create directories? (Y/N))")
        val input = readlnOrNull() ?: run{
            logger.warn("Input is null"); return
        }

        when (input.uppercase()) {
            "Y" -> {
                for (missingDirectory in missingDirectories) {
                    logger.info("Creating... $missingDirectory ")

                    val result = runCatching {
                        Files.createDirectories(missingDirectory)
                    }
                    result.onSuccess { continue }
                        .onFailure { logger.error("Error: Check the folder if any file have the exact name to  ${missingDirectory.fileName} ") }
                }
            }

            "N" -> {
                for (missingDirectory in missingDirectories) {
                    logger.warn("missing... $missingDirectory ")

                }
                return
            }

            else -> {
                logger.warn("Wrong Input, must be (Y/N)")
                return
            }
        }
    }
}
package sorting


import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.collections.iterator
import kotlin.io.iterator
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.resolve
import kotlin.text.contains
import kotlin.text.iterator
import kotlin.toString


fun handleDuplicates(destination: Path, filePath: Path): Path? {
    var destinationFile = destination.resolve(filePath.fileName)
    val duplicateMode = config.configurations["duplicate-mode"]

    when (duplicateMode) {
        "rename" -> {
            var counter = 1
            while (Files.exists(destinationFile)) {
                destinationFile = Path.of("$destination/${filePath.fileName} Duplicate (${counter})${filePath.extension}")
                counter += 1
            }

            Files.move(filePath, destinationFile)
            return destinationFile
        }
        "skip" -> {
            return null
        }
        "overwrite" -> {
            Files.move(
                filePath,
                destinationFile,
                StandardCopyOption.REPLACE_EXISTING
            )

            println("Duplicate detected: replacing $destinationFile")
            return destinationFile
        }
        else -> {
            Files.move(filePath, destinationFile)
            return destinationFile
        }
    }
}

fun ensureFolderDestination(selectedDirectory: Path,sortingRules: Map<String, List<String>>) {
    val missingDirectories = sortingRules.keys
        .map { folderName -> selectedDirectory.resolve(folderName) }
        .filter { !Files.isDirectory(it) }

    if (missingDirectories.isEmpty()) {
        println("Directories are missing!! (note: these are folders in which the sorted files are gonna be moved)")
        for (missingDirectory in missingDirectories) {
            println("Creating... $missingDirectory")

            val result = runCatching {
                Files.createDirectories(missingDirectory)
            }
            result.onSuccess { continue }
                .onFailure { println("Error: Check the folder if any file have the exact name to  ${missingDirectory.fileName}") }
        }
    }
}

fun sortingLogic(selectedDirectory: Path, sortingRules: Map<String, List<String>>) {
    println("Files in the selected folder: ${selectedDirectory.fileName} will be moved into category subfolders: ")
    for (folderName in sortingRules.keys) {
        println("~ $folderName")
    }

    println("Press enter to continue: "); readln()
    val moves = mutableListOf<Map<String, Any>>()

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
                val newPath = handleDuplicates(destination, filePath)

                println("Moved: ${filePath.fileName} -> $destination\n")

                moves.add(
                    mapOf(
                        "original path" to JsonPrimitive(filePath.toString()),
                        "new path" to JsonPrimitive(destination.resolve(filePath.fileName).toString()),
                        "undo" to JsonPrimitive(false),
                    )
                )

            } catch (e: Exception) {
                println("Failed to process ${filePath.fileName}: ${e.message}")
            }
        }
    }
}


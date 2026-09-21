package sorting

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import org.slf4j.LoggerFactory
private val logger = LoggerFactory.getLogger("Sorting")
fun handleDuplicates(destination: Path, filePath: Path): Path? {
    val duplicateMode = config.configurations["duplicate mode"] ?: "rename"
    val destinationFile = destination.resolve(filePath.fileName)

    return when (duplicateMode) {
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
            if (duplicateMode != "rename") {
                logger.warn("Unknown duplicate mode: $duplicateMode — using rename")
            }
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
    }
}

fun duplicatesDryRun(destination: Path, filePath: Path): Path? {
    val duplicateMode = config.configurations["duplicate mode"] ?: "rename"
    val destinationFile = destination.resolve(filePath.fileName)

    return when (duplicateMode) {
        "skip" -> {
            logger.info("duplicate:  to be skipped ${filePath.fileName}")
            null
        }
        "overwrite" -> {
            logger.info("Duplicate: to be replaced/overwritten $destinationFile")
            destinationFile
        }
        else -> {
            if (duplicateMode != "rename") {
                logger.warn("Unknown duplicate mode: $duplicateMode — using rename")
            }
            var counter = 1
            var target = destinationFile
            while (Files.exists(target)) {
                val newName = "${filePath.nameWithoutExtension} Duplicate ($counter).${filePath.extension}"
                target = destination.resolve(newName)
                counter++
            }
            target
        }
    }
}
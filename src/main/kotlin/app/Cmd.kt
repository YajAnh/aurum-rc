package app

import main
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Cmd")

fun cmdMain() {
    logger.info("Aurum-RC Interactive Mode")
    logger.info("Type 'exit' or 'quit' to quit")
    logger.info("Type 'help' for available commands")

    while (true) {
        print("[aurum-rc:~]$> ")
        val input = readlnOrNull()?.trim() ?: break
        if (input.isBlank()) continue
        if (input.trim() == "exit" || input.trim() == "quit") break

        val args = input.trim().split(Regex("\\s+"))
        val result = runCatching {
            App().main(args)
        }
        result.onSuccess { continue }
            .onFailure { exception ->  val errorMessage = exception.message ?: "Unknown error"
            logger.warn("Error: $errorMessage")
            }
    }
}
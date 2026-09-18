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

        val args = parseArgs(input)
        val result = runCatching {
            App().main(args)
        }
        result.onSuccess { continue }
            .onFailure { exception ->  val errorMessage = exception.message ?: "Unknown error"
            logger.warn("Error: $errorMessage")
            }
    }
}

fun parseArgs(input: String): List<String> {
    val args = mutableListOf<String>()
    val current = StringBuilder()

    var inQuotes = false
    var quoteChar = '\u0000'

    for (char in input.trim()) {
        if (char == '"' || char == '\'') {
            if (!inQuotes) {
                inQuotes = true
                quoteChar = char
            } else if (char == quoteChar) {
                inQuotes = false
            } else {
                current.append(char)
            }
            continue
        }

        if (char.isWhitespace() && !inQuotes) {
            if (current.isNotEmpty()) {
                args.add(current.toString())
                current.clear()
            }
        } else {
            current.append(char)
        }
    }

    if (current.isNotEmpty()) {
        args.add(current.toString())
    }

    return args
}
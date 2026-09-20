package app

import rules.homeDirectory
import rules.sortingRules

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

import config.loadConfig
import config.loadDirectories
import config.loadHistory
import config.saveConfig
import config.saveHistory
import sorting.ensureFolderDestination
import sorting.sortingLogic
import services.clearHistory
import services.undoLogic
import services.undoLogicDryRun
import sorting.ensureFolderDestinationDryRun
import sorting.sortingDryRun
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory


class App : CliktCommand(name = "aurum-rc") {
    init {
        subcommands(
            Sort(),
            Folders(),
            Undo(),
            Dirs(),
            History(),
            Config()
        )
    }

    override fun run() {
        runCatching {
            loadHistory()
            loadDirectories()
            loadConfig()
        }.onFailure { e -> println(e.stackTraceToString()) }
    }
}

class Sort : CliktCommand(name = "sort", help = "Sort files in a directory") {
    private val folder by argument(help = "Standard folder to sort").optional()
    private val custom by option("--custom", help = "Sort files in a manually specified path")
    private val pinned by option("--pinned", help = "Sort files in a pinned directory (by name)")
    private val dryRun by option("--dryrun", help = "Shows what files get moved, without moving").flag()

    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        if (listOfNotNull(folder, custom, pinned).size != 1) {
            echo("Provide exactly one source: a standard <folder>, --custom <path>, or --pinned <name>")
            return
        }

        val selectedDirectory: Path = when {
            custom != null -> {
                val path = Path.of(custom!!)
                if (!Files.isDirectory(path)) {
                    echo("$path is not a valid directory")
                    return
                }
                path
            }

            pinned != null -> {
                val name = pinned!!.replaceFirstChar { it.titlecase() }
                val path = config.addedDirectories[name]
                if (path == null) {
                    echo("Pinned directory '$name' not found. Pinned directories:")
                    for ((index, entry) in config.addedDirectories.entries.withIndex()) {
                        echo("   ${index + 1}. ~ ${entry.key} ~ ${entry.value}")
                    }
                    return
                }
                Path.of(path)
            }

            else -> {
                val folders = rules.standardFolders(homeDirectory)
                val name = folder!!.replaceFirstChar { it.titlecase() }
                val path = folders[name]
                if (path == null) {
                    echo("Invalid folder: $folder. Available folders:")
                    for ((index, folderName) in folders.keys.withIndex()) {
                        echo("   ${index + 1}. :: $folderName")
                    }
                    return
                }
                Path.of(path)
            }
        }

if (dryRun) {
            echo("On Dry Run!!")
            runCatching {
                ensureFolderDestinationDryRun(selectedDirectory, sortingRules)
                sortingDryRun(selectedDirectory, sortingRules)
                return
            }.onFailure {
                e -> if (debug) {
                    echo(e.stackTraceToString())
                } else {
                    echo("Error: ${e.message}", err = true)
                }
            }
        }

        runCatching {
            ensureFolderDestination(selectedDirectory, sortingRules)
            sortingLogic(selectedDirectory, sortingRules)
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class Folders : CliktCommand(name = "folders", help = "Shows standard folders") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            val folders = rules.standardFolders(homeDirectory)

            for ((index, folderName) in folders.keys.withIndex()) {
                echo("${index + 1}. :: $folderName")
            }
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class Undo : CliktCommand(name = "undo", help = "Undo sort sessions") {
    private val dryRun by option(
        "--dryrun",
        help = "Shows what files get moved, without moving"
    ).flag()

    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            val history = loadHistory()

            if (history.sessions.isEmpty()) {
                echo("No sessions to undo"); return
            }

            echo("Current sessions (0 - ${history.sessions.size}):")
            for ((i, session) in history.sessions.withIndex()) {
                echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")
            }

            echo("Select the respective index: ")
            val input = readlnOrNull()?.toIntOrNull() ?: run { echo("Input is either empty or not a number"); return }

            if (input !in 1..history.sessions.size) {
                echo("IndexError: Input is out the index range. Please pick a number from 1 to ${history.sessions.size}")
                return
            }

            if (dryRun) {
                echo("On Dry Run!!")
                undoLogicDryRun(input = input)
                return
            }

            undoLogic(input = input)
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class Dirs : CliktCommand(name = "dirs", help = "Manage pinned directories") {
    init {
        subcommands(
            DirsList(),
            DirsAdd(),
            DirsRemove()
        )
    }

    override fun run() = Unit
}

class DirsList : CliktCommand(name = "list", help = "Shows user added directories") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            loadDirectories()

            if (config.addedDirectories.isNotEmpty()) {
                for ((index, entry) in config.addedDirectories.entries.withIndex()) {
                    echo("${index + 1}. ~ ${entry.key} ~ ${entry.value}")
                }
            } else {
                echo("No added directories")
            }
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class DirsAdd : CliktCommand(name = "add", help = "Adds a directory") {
    private val pathName by argument()
    private val pathDirectory by argument()
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            if (!Path.of(pathDirectory).isDirectory()) {
                echo("$pathDirectory is not a valid directory")
                return
            }

            config.addedDirectories[pathName] = pathDirectory

            if (config.addedDirectories[pathName] != pathDirectory) {
                echo("$pathName did not get added properly"); return
            }

            echo("Added ~ $pathName ~ ($pathDirectory)")
            config.saveDirectories()
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class DirsRemove : CliktCommand(name = "remove", help = "Removes a directory") {
    private val targetDirectory by argument()
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            if (targetDirectory !in config.addedDirectories) {
                echo("$targetDirectory is not in added directories...")
                return
            }
            echo("removing $targetDirectory...")
            config.addedDirectories.remove(targetDirectory)
            config.saveDirectories()
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class History : CliktCommand(name = "history", help = "Manage history") {
    init {
        subcommands(
            HistoryShow(),
            HistoryClear(),
            HistoryRemove()
        )
    }

    override fun run() = Unit
}

class HistoryShow : CliktCommand(name = "show", help = "Shows full history") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            val history = loadHistory()

            if (history.sessions.isEmpty()) {
                echo("Sessions are empty!!")
                return
            }

            echo("Current sessions (0 - ${history.sessions.size}):")
            for ((i, session) in history.sessions.withIndex()) {
                echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")
            }
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class HistoryClear : CliktCommand(name = "clear", help = "Clears history") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            clearHistory()
            saveHistory()
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class HistoryRemove : CliktCommand(name = "remove", help = "Removes a session in history") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            val history = loadHistory()

            if (history.sessions.isNotEmpty()) {
                echo("Current sessions (0 - ${history.sessions.size}):")
                for ((i, session) in history.sessions.withIndex()) {
                    echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")
                }
                echo()

                echo("Remove by Index > "); val index = readln().toInt()
                services.removeHistorySession(index)
            } else {
                echo("No history sessions to remove")
            }
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class Config : CliktCommand(name = "config", help = "Settings and configuration") {
    init {
        subcommands(
            ConfigDuplicates(),
            ConfigRemoveSessionAfterUndo()
        )
    }

    override fun run() = Unit
}

class ConfigDuplicates : CliktCommand(name = "duplicates", help = "Sets duplicate handling mode") {
    init {
        subcommands(
            DuplicateMode("rename"),
            DuplicateMode("skip"),
            DuplicateMode("overwrite")
        )
    }

    override fun run() = Unit
}

class DuplicateMode(private val mode: String) : CliktCommand(name = mode, help = "Duplicate mode: $mode") {
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            config.configurations["duplicate mode"] = mode
            echo("Duplicate Mode set to $mode")
            saveConfig()
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}

class ConfigRemoveSessionAfterUndo : CliktCommand(name = "remove-session-after-undo", help = "Removes a session after an undo") {
    private val enabled by argument().boolean()
    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        runCatching {
            config.configurations["remove session after redo"] = enabled.toString()
            echo("Remove session after undo set to $enabled")
            saveConfig()
        }.onFailure {
            e -> if (debug) {
                echo(e.stackTraceToString())
            } else {
                echo("Error: ${e.message}", err = true)
            }
        }
    }
}
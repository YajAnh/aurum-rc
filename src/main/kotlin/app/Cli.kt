package app

import rules.homeDirectory


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.boolean

import rules.sortingRules
import config.loadConfig
import config.loadDirectories
import config.loadHistory
import config.saveConfig
import config.saveDirectories
import config.saveHistory
import sorting.ensureFolderDestination
import sorting.sortingLogic
import java.nio.file.Path
import java.nio.file.Files
import kotlin.io.path.isDirectory
import services.clearHistory
import services.undoLogic



class App : CliktCommand(name = "aurum-rc") {
    init {
        subcommands(
            SortType(),
            Settings()
        )
    }

    override fun run() {
        loadHistory()
        loadDirectories()
        loadConfig()

    }
}

class SortType : CliktCommand(name = "st", help = "Sort types") {
    init {
        subcommands(
            Automatic()
        )
    }

    override fun run() = Unit
}

class Automatic : CliktCommand(name = "auto", help = "Sorts files by extensions (limited)") {
    init {
        subcommands(
            Standard(),
            Show(),
            Custom(),
        )
    }
    override fun run() = Unit
}

class Standard : CliktCommand(name = "std", help = "Sorts files on Windows's pinned folders") {
    private val folder by argument()

    override fun run() {
        val folders = rules.standardFolders(homeDirectory)

        if (folder.replaceFirstChar { it.titlecase() } !in folders){
            echo("Invalid folder: $folder. Available folders: ")

            for ((index, folderName) in folders.keys.withIndex()) {
                echo("${index + 1}. :: $folderName")
            }
            return
        }

        val selectedDirectory = Path.of(folders.getValue(folder))
        ensureFolderDestination(selectedDirectory, sortingRules)
        sortingLogic(selectedDirectory, sortingRules)
    }

}

class Show : CliktCommand(name = "show", help = "Shows Directories") {
    override fun run() {
        val folders = rules.standardFolders(homeDirectory)

        for ((index, folderName) in folders.keys.withIndex()) {
            echo("${index + 1}. :: $folderName")
        }
    }
}

class Custom : CliktCommand(name = "cus", help = "Sorts files by either manually inserted Path or a user pinned Path") {
    init {
        subcommands(
            ManualInput(),
            UsePinned()
        )
    }
    override fun run() = Unit
}

class UsePinned : CliktCommand(name = "up", help = "Sorts files using user pinned Paths") {
    private val name by argument()

    override fun run() {

        if (name !in config.addedDirectories.keys) {
            echo("No directories added...")
            echo("Available pinned directories:")

            if (config.addedDirectories.isNotEmpty()) {
                for ((index, entry) in config.addedDirectories.entries.withIndex()) {
                    val dirKey = entry.key
                    val dirVal = entry.value

                    print("${index + 1}. ~ $dirKey ~ $dirVal")
                }

                return
            }
        }
        val selectedDirectory = Path.of(
            config.addedDirectories[name] ?: throw IllegalArgumentException("Directory '$name' not found"))
        ensureFolderDestination(selectedDirectory, sortingRules)
        sortingLogic(selectedDirectory, sortingRules)
    }
}

class ManualInput : CliktCommand(name = "mi", help = "Sorts files using a manually inserted Path") {
    private val manualInput by argument()

    override fun run() {
        val selectedDirectory: Path = Path.of(manualInput)

        if (!Files.isDirectory(selectedDirectory)) {
            echo("$selectedDirectory not found and may not exist. Try again")
            return
        }
        ensureFolderDestination(selectedDirectory, sortingRules)
        sortingLogic(selectedDirectory, sortingRules)
    }

}

class Settings : CliktCommand(name = "stg", help = "app.Settings and configurations") {
    init {
        subcommands(
            Misc(),
            ModSet(),
        )
    }
    override fun run() = Unit
}

class Misc : CliktCommand(name = "misc", help = "miscellaneous") {
    init {
        subcommands(
            Undo()
        )
    }
    override fun run() = Unit
}

class Undo : CliktCommand(help = "Undo sort sessions") {
    override fun run() {
        val history = loadHistory()

        echo("Current sessions (0-${history.sessions.lastIndex}):")
        for ((i, session) in history.sessions.withIndex()) {
            echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")


            echo("Select the respective index: ")
            val input = readln().toIntOrNull()
            if (input == null) {
                echo("Input is not Int"); return
            } else {
                undoLogic(input)
            }

        }
    }
}

class ModSet : CliktCommand(name = "mod", help = "None") {
    init {
        subcommands(
            RemoveSessionAfterRedo(),
            History(),
            DuplicateHandler(),
            Configuration()
        )
    }

    override fun run() = Unit
}

class RemoveSessionAfterRedo : CliktCommand(name = "rsar", help = "Remove session after a redo session") {
    private val enabled by argument().boolean()

    override fun run() {
        config.configurations["remove session after redo"] = enabled.toString().replaceFirstChar { it.lowercase() }
        saveConfig()
    }


}

class History : CliktCommand(name = "h", help = "Manage app.History") {
    init {
        subcommands(
            Clear(),
            Remove(),
            ShowHistory()
        )
    }
    override fun run() = Unit
}

class Clear : CliktCommand(name = "c", help = "app.Clear history") {
    override fun run() {
        clearHistory()
        saveHistory()
    }
}

class Remove : CliktCommand(name = "r", help = "app.Remove a session in history") {
    override fun run() {

        echo("Remove by Index > "); val result = runCatching {
            val index = readln().toInt()
            val history = loadHistory()

            if (history.sessions.isNotEmpty()) {
                echo("Current sessions (0-${history.sessions.lastIndex}):")
                for ((i, session) in history.sessions.withIndex()) {
                    echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")
                }
                echo()

                services.removeHistorySession(index)
                saveHistory()
            } else {
                echo("No history sessions to remove")
            }
        }
        result.onSuccess { echo("Success") }
            .onFailure { echo("Failed to remove, Try again") }
    }
}

class ShowHistory : CliktCommand(name = "sh", help = "Shows full history") {
    override fun run() {
        val history = loadHistory()

        if (history.sessions.isEmpty()) {
            echo("Sessions are empty!!")
            return
        }

        echo("Current sessions (0-${history.sessions.lastIndex}):")
        for ((i, session) in history.sessions.withIndex()) {
            echo("   ${i + 1}. ${session.timestamp} - ${session.directory}")

        }
    }
}

class DuplicateHandler : CliktCommand(name = "dh", help = "Duplicate handlers"){
    init {
        subcommands(
            Rename(),
            Skip(),
            Overwrite()
        )
    }
    override fun run() = Unit
}

class Rename : CliktCommand(name = "r", help = "Renames duplicate files") {
    override fun run() {
        config.configurations["duplicate mode"] = "rename"
        echo("Duplicate Mode set to rename")
        saveConfig()
    }
}

class Skip : CliktCommand(name = "s", help = "Skips duplicate files") {
    override fun run() {
        config.configurations["duplicate mode"] = "skip"
        echo("Duplicate Mode set to skip")
        saveConfig()
    }
}

class Overwrite : CliktCommand(name = "ow", help = "overwrites duplicate files") {
    override fun run() {
        config.configurations["duplicate mode"] = "overwrite"
        echo("Duplicate Mode set to overwrite")
        saveConfig()
    }
}

class Configuration : CliktCommand(name = "config", help = "Configuration handlers") {
    init {
        subcommands(
            ShowDirs(),
            DirectoryHandlers()
        )
    }
    override fun run() = Unit
}

class ShowDirs : CliktCommand(name = "sd", help = "app.Show user added directories") {
    override fun run() {
        loadDirectories()

        if (config.addedDirectories.isNotEmpty()) {
            for ((index, entry) in config.addedDirectories.entries.withIndex()) {
                val dirKey = entry.key
                val dirVal = entry.value

                echo("${index + 1}. ~ $dirKey ~ $dirVal")
            }
        } else {
            echo("No added directories")
        }
    }
}

class DirectoryHandlers : CliktCommand(name = "dirh", help = "Handle user added directories") {
    init {
        subcommands(
            Add(),
            RemoveDir()
        )
    }
    override fun run() = Unit
}

class Add : CliktCommand("app.Add Directory") {
    private val pathName by argument()
    private val pathDirectory by argument()

    override fun run() {
        if (!Path.of(pathDirectory).isDirectory()){
            echo("$pathDirectory is not a valid directory")
            return
        }

        config.addedDirectories[pathName] = pathDirectory

        if (config.addedDirectories[pathName] != pathDirectory) {echo("$pathName did not get added properly"); return}

        echo("Added ~ $pathName ~ ($pathDirectory)")
        saveDirectories()
    }
}

class RemoveDir : CliktCommand(name = "remove", help = "app.Add Directory") {
    private val targetDirectory by argument()

    override fun run() {
        if (targetDirectory !in config.addedDirectories) {
            echo("$targetDirectory is not in added directories...")
            return
        }
        echo("removing $targetDirectory...")
        config.addedDirectories.remove(targetDirectory)
        saveDirectories()
    }
}


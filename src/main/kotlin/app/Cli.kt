package app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import config.loadConfig
import config.loadDirectories
import config.loadHistory
import config.saveConfig
import kotlinx.datetime.LocalDate
import rules.DateDataGroup
import rules.Destination
import rules.ExtensionsData
import rules.NameLengthDataGroup
import rules.PatternData
import rules.SizeDataGroup
import rules.homeDirectory
import rules.sortingRules
import services.clearHistory
import services.formatSessionTimestamp
import services.undoLogic
import services.undoLogicDryRun
import sorting.ensureFolderDestination
import sorting.ensureFolderDestinationDryRun
import sorting.sortingDryRun
import sorting.sortingLogic
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
    private val folder by argument(help = "Standard folder name or path to sort").optional()

    private val extensions by option("--extensions", help = "Add sorting rule to sort extensions")
        .convert {
            val parsedExtensions = it.split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(String::lowercase)

            require(parsedExtensions.isNotEmpty()) {
                "--extensions requires at least one extension"
            }

            ExtensionsData(parsedExtensions.joinToString(","))
        }
    // --extensions jpg,jpeg,png,gif,webm
    // --extensions mp4,mov


    private val date by option("--date", help = "Add sorting rule to sort depending on the date")
        .convert { value ->
            val dateInputParsed = value.trim().split(Regex("\\s+"))
            require(dateInputParsed.isNotEmpty() && dateInputParsed.first().isNotBlank()) {
                "--date requires: <created|modified> <date-range> or <age> <older|newer> <duration>"
            }

            val dateOption = when (dateInputParsed[0]) {
                "created" -> DateDataGroup.DateOptionEnum.CREATED
                "modified" -> DateDataGroup.DateOptionEnum.MODIFIED
                "age" -> DateDataGroup.DateOptionEnum.AGE
                else -> throw IllegalArgumentException("Unexpected date option '${dateInputParsed[0]}'")
            }

            if (dateOption == DateDataGroup.DateOptionEnum.AGE) {
                require(dateInputParsed.size == 3) {
                    "--date age requires: age <older|newer> <duration>"
                }

                val ageComparison = when (dateInputParsed[1]) {
                        "older" -> DateDataGroup.AgeEnum.OLDER
                        "newer" -> DateDataGroup.AgeEnum.NEWER
                        else -> throw IllegalArgumentException("Unexpected age comparison '${dateInputParsed[1]}'")
                    }

                val ageDateEntries = dateInputParsed[2].split(":")
                require(ageDateEntries.all(String::isNotBlank)) {
                    "--date age duration must not contain empty values"
                }

                return@convert DateDataGroup.AgeData(
                    dateOption,
                    ageComparison,
                    ageDateEntries
                )
            }

            require(dateInputParsed.size == 2) {
                "--date requires: <created|modified> <date-range>"
            }

            val dateSections = dateInputParsed[1].split(":")
            require(dateSections.all(String::isNotBlank)) {
                "--date range must not contain empty values"
            }

            DateDataGroup.DateData(
                dateOption,
                dateSections.map { LocalDate.parse(it) }
            )

        }
    // --date created YYYY-MM-DD:YYYY-MM-DD
    // --date modified YYYY-MM-DD
    // --date age older/newer 1m:10d

    private val nameLength by option("--length", help = "Add sorting rule to sort depending on the file name length")
        .convert { value ->
            val nameLengthInputParse = value.trim().split(Regex("\\s+"))

            require(nameLengthInputParse.size == 2) {
                "--length requires: <less|greater|range|exact> <value>"
            }

            val nameLengthOption = when (nameLengthInputParse[0]) {
                    "less" -> NameLengthDataGroup.NameLengthComparisonStatementsEnum.LESS
                    "greater" -> NameLengthDataGroup.NameLengthComparisonStatementsEnum.GREATER
                    "range" -> NameLengthDataGroup.NameLengthComparisonStatementsEnum.RANGE
                    "exact" -> NameLengthDataGroup.NameLengthComparisonStatementsEnum.EXACT
                    else -> throw IllegalArgumentException("Unexpected name length option '${nameLengthInputParse[0]}'")
                }

            val ranges = nameLengthInputParse[1].split('-').map { entry ->
                entry.toIntOrNull()?.also {
                    require(it >= 0) { "Name lengths must not be negative" }
                } ?: throw IllegalArgumentException("Invalid name length '$entry'")
            }

            require(
                when (nameLengthOption) {
                    NameLengthDataGroup.NameLengthComparisonStatementsEnum.RANGE -> ranges.size == 2
                    else -> ranges.size == 1
                }
            ) {
                if (nameLengthOption == NameLengthDataGroup.NameLengthComparisonStatementsEnum.RANGE) {
                    "--length range requires <minimum>-<maximum>"
                } else {
                    "--length ${nameLengthInputParse[0]} requires one value"
                }
            }

            if (nameLengthOption == NameLengthDataGroup.NameLengthComparisonStatementsEnum.RANGE) {
                require(ranges[0] <= ranges[1]) {
                    "--length range requires minimum <= maximum"
                }
            }

            NameLengthDataGroup.NameLengthData(nameLengthOption, ranges)
        }
    // --length less 10 ~ LESS THAN
    // --length greater 5 ~ GREATER THAN
    // --length range 10-5 ~ RANGE
    // --length exact 10

    private val size by option("--size", help = "Add sorting rule to sort depending on size")
        .convert { value ->
            val sizeInputParsed = value.trim().split(Regex("\\s+"))
            require(sizeInputParsed.size == 1 || sizeInputParsed.size == 2) {
                "--size requires: <minimum>-<maximum>, less <size>, or greater <size>"
            }

            if (sizeInputParsed[0] != "less" && sizeInputParsed[0] != "greater") {
                val range = sizeInputParsed[0].split("-")
                require(range.size == 2 && range.all(String::isNotBlank)) {
                    "--size range requires <minimum>-<maximum>"
                }

                SizeDataGroup.Range(range)
                return@convert
            }

            require(sizeInputParsed.size == 2 && sizeInputParsed[1].isNotBlank()) {
                "--size ${sizeInputParsed[0]} requires a value"
            }

            val comparisonStatement = when (sizeInputParsed[0]) {
                "less" -> SizeDataGroup.SizeComparisonStatementsEnum.LESS
                "greater" -> SizeDataGroup.SizeComparisonStatementsEnum.GREATER
                else -> error("Unreachable")
            }
            SizeDataGroup.CompareData(comparisonStatement, sizeInputParsed[1])
        }
    // --size 1mb-100mb ~ less 100mb ~ greater 50mb

    private val pattern by option("--pattern", help = "Add sorting rule to sort depending on prefixes/pattern that matches a file")
        .convert { value ->
            val patternInputParsed = value.trim().split(Regex("\\s+"))
            require(patternInputParsed.size == 1 && patternInputParsed[0].isNotBlank()) {
                "--pattern requires one or more comma-separated patterns"
            }

            val patterns = patternInputParsed[0].split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
            require(patterns.isNotEmpty()) {
                "--pattern requires one or more comma-separated patterns"
            }

            PatternData(patterns)
        }
    // --pattern screenshot*,IMG*

    private val destination by option("--destination", help = "Custom destination of the file at instance")
        .convert { value ->
            val destinationInputParsed = value.trim().split(Regex("\\s+"), limit = 2)
            require(destinationInputParsed.size == 2 &&
                destinationInputParsed[0].isNotBlank() &&
                destinationInputParsed[1].isNotBlank()
            ) {
                "--destination requires: <name> <path>"
            }

            Destination(destinationInputParsed[0], Path.of(destinationInputParsed[1]))
        }
    // --destination <Name> <Path>

    private val pinned by option("--pinned", help = "Sort files in a pinned directory (by name)")
    private val dryRun by option("--dryrun", help = "Shows what files get moved, without moving").flag()

    private val debug by option("--debug", help = "Shows stack traces for debugging").flag()

    override fun run() {
        if (listOfNotNull(folder, pinned).size != 1) {
            echo("Provide exactly one source: a standard folder, a directory path, or --pinned <name>")
            return
        }

        val selectedDirectory: Path = when {
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
                val path = folders[name]?.let(Path::of) ?: Path.of(folder!!)
                if (!Files.isDirectory(path)) {
                    echo("$path is not a valid directory")
                    return
                }
                path
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

            echo("Current sessions (1 - ${history.sessions.size}):")
            for ((i, session) in history.sessions.withIndex()) {
                echo("   ${i + 1}. ${formatSessionTimestamp(session.timestamp)} - ${session.directory}")
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

            echo("Current sessions (1 - ${history.sessions.size}):")
            for ((i, session) in history.sessions.withIndex()) {
                echo("   ${i + 1}. ${formatSessionTimestamp(session.timestamp)} - ${session.directory}")
                echo("      Preview...")

                val moveEntries = history.sessions[i].moves
                var counter = 0

                for ((index, preview) in moveEntries.withIndex()) {
                    counter += 1
                    if (counter > 3) {
                        echo("          ... and ${moveEntries.size} more file(s)....")
                        break
                    }
                    echo("       ${index + 1}. ${preview["original path"]} -> ${preview["new path"]}")
                }
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
                echo("Current sessions (1 - ${history.sessions.size}):")
                for ((i, session) in history.sessions.withIndex()) {
                    echo("   ${i + 1}. ${formatSessionTimestamp(session.timestamp)} - ${session.directory}")
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
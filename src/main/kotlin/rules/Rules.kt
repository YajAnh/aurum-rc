package rules

import kotlinx.datetime.LocalDate
import java.nio.file.Path

val homeDirectory: String = System.getProperty("user.home")

var sortingRules = mutableMapOf(
    "SortedImages" to listOf("jpg", "jfif", "png", "webp", "gif"),
    "SortedDocuments" to listOf("pdf", "docx", "xlsx", "txt", "csv", "html", "htm", "ppt", "pptx", "md"),
    "SortedExecutables" to listOf("exe", "iso", "so", "bat", "msi"),
    "SortedVideos" to listOf("mp4", "webm", "mov"),
    "SortedProgramming" to listOf("py", "cpp", "js", "json", "hpp", "lua", "ahk"),
    "SortedMusic" to listOf("mp3", "opus", "wav"),
    "SortedCompressedFile" to listOf("rar", "zip", "7z", "tgz", "tar.gz")
)

fun standardFolders(homeDirectory: String): Map<String, String> = mapOf(
    "Desktop" to "$homeDirectory/Desktop",
    "Downloads" to "$homeDirectory/Downloads",
    "Documents" to "$homeDirectory/Documents",
    "Music" to "$homeDirectory/Music",
    "Pictures" to "$homeDirectory/Pictures",
    "Videos" to "$homeDirectory/Videos"
)

data class ExtensionsData(
    val extensionsParsed: String
)

sealed interface DateDataGroup {
    enum class DateOptionEnum {
        CREATED,
        MODIFIED,
        AGE
    }

    enum class AgeEnum {
        OLDER,
        NEWER
    }

    data class DateData(
        val dateOption: DateOptionEnum,
        val dates: List<LocalDate>
    )

    data class AgeData(
        val dateOption: DateOptionEnum,
        val ageComparison: AgeEnum,
        val ageDateEntries: List<String>?
    )
}

sealed interface NameLengthDataGroup {
    enum class NameLengthComparisonStatementsEnum {
        LESS,
        GREATER,
        RANGE,
        EXACT
    }

    data class NameLengthData(
        val nameLengthOption: NameLengthComparisonStatementsEnum,
        val ranges: List<Int>
    )
}

sealed interface SizeDataGroup {
    enum class SizeComparisonStatementsEnum {
        GREATER,
        LESS
    }

    data class CompareData(
        val comparisonStatementsEnum: SizeComparisonStatementsEnum?,
        val size: String
    )

    data class Range(
        val size: List<String>
    )
}

data class PatternData(
    val pattern: List<String>
)

data class Destination(
    val name: String,
    val path: Path
)

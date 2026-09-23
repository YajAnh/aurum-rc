package rules

import kotlinx.datetime.LocalDate
import java.nio.file.Path

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

    data class NameLengthData (
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

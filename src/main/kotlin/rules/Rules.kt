package rules

import java.nio.file.Path

val homeDirectory: String = System.getProperty("user.home")

val sortingRules = mapOf(
    "SortedImages" to listOf("jpg", "jfif", "png", "webp", "gif", "webm"),
    "SortedDocuments" to listOf("pdf", "docx", "xlsx", "txt", "csv", "html", "htm", "ppt", "pptx", "md"),
    "SortedExecutables" to listOf("exe", "iso", "so", "bat", "msi"),
    "SortedVideos" to listOf("mp4"),
    "SortedProgramming" to listOf("py", "cpp", "js", "json", "hpp", "lua", "ahk"),
    "SortedMusic" to listOf("mp3", "opus"),
    "SortedCompressedFile" to listOf("rar", "zip", "7z", "tgz", "tag.gz")
)

fun standardFolders(homeDirectory: String): Map<String, String> = mapOf(
    "Desktop" to "$homeDirectory/Desktop",
    "Downloads" to "$homeDirectory/Downloads",
    "Documents" to "$homeDirectory/Documents",
    "Music" to "$homeDirectory/Music",
    "Pictures" to "$homeDirectory/Pictures",
    "Videos" to "$homeDirectory/Videos"
)


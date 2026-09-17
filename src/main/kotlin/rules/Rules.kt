package rules


import java.nio.file.Path

val homeDirectory: String = System.getProperty("user.home")


val configPath: Path = Path.of("").toAbsolutePath()
    .resolve("src")
    .resolve("main")
    .resolve("kotlin")
    .resolve("json")
    .resolve("Config.json")

val directoriesPath: Path = Path.of("").toAbsolutePath()
    .resolve("src")
    .resolve("main")
    .resolve("kotlin")
    .resolve("json")
    .resolve("Directories.json")


val sortingRules = mapOf(
    "SortedImages" to listOf("jpg", "jfif", "png", "webp", "gif"),
    "SortedDocuments" to listOf("pdf", "docx", "xlsx", "txt", "csv", "html", "ppt", "pptx"),
    "SortedExecutables" to listOf("exe", "iso", "bat"),
    "SortedVideos" to listOf("mp4"),
    "SortedProgramming" to listOf("py", "cpp", "js", "json"),
    "SortedMusic" to listOf("mp3", "opus"),
    "SortedCompressedFile" to listOf("rar", "zip", "7z", "tgz")
)

fun standardFolders(homeDirectory: String): Map<String, String> = mapOf(
    "Desktop" to "$homeDirectory/Desktop",
    "Downloads" to "$homeDirectory/Downloads",
    "Documents" to "$homeDirectory/Documents",
    "Music" to "$homeDirectory/Music",
    "Pictures" to "$homeDirectory/Pictures",
    "Videos" to "$homeDirectory/Videos"
)


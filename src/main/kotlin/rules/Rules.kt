package rules


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


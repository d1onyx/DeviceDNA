package com.devstdvad.devicedna.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Plugin-free i18n for Compose Multiplatform. The standard `composeResources`/`Res`
 * mechanism needs the org.jetbrains.compose Gradle plugin, which is incompatible with
 * this project's AGP 9.x. Instead, strings are generated into [StringCatalog] from the
 * Android `strings.xml` files and resolved at runtime with a language fallback.
 *
 * Usage in a shared @Composable:  Text(stringRes("settings_title"))
 *                                 Text(stringRes("battery_cycles_value", cycles))
 */
enum class AppLanguage(val tag: String) {
    En("en"), De("de"), Ru("ru");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag, ignoreCase = true) } ?: En
    }
}

class Strings internal constructor(
    private val primary: Map<String, String>,
    private val fallback: Map<String, String>,
) {
    operator fun get(key: String): String = primary[key] ?: fallback[key] ?: key
    fun format(key: String, vararg args: Any?): String = formatPattern(get(key), args)
}

fun stringsFor(language: AppLanguage): Strings {
    val primary = when (language) {
        AppLanguage.En -> StringCatalog.en
        AppLanguage.De -> StringCatalog.de
        AppLanguage.Ru -> StringCatalog.ru
    }
    return Strings(primary, StringCatalog.default)
}

val LocalStrings = staticCompositionLocalOf {
    Strings(StringCatalog.default, StringCatalog.default)
}

@Composable
@ReadOnlyComposable
fun stringRes(key: String): String = LocalStrings.current[key]

@Composable
@ReadOnlyComposable
fun stringRes(key: String, vararg args: Any?): String = LocalStrings.current.format(key, *args)

/**
 * Minimal printf-style substitution supporting `%s`, `%d`, positional `%1$s` / `%2$d`,
 * and `%%`. Width/precision flags (e.g. `%.1f`) are substituted with the raw argument.
 */
internal fun formatPattern(pattern: String, args: Array<out Any?>): String {
    if (args.isEmpty() || '%' !in pattern) return pattern
    val sb = StringBuilder()
    var i = 0
    var autoIndex = 0
    while (i < pattern.length) {
        val c = pattern[i]
        if (c == '%' && i + 1 < pattern.length) {
            if (pattern[i + 1] == '%') {
                sb.append('%'); i += 2; continue
            }
            var j = i + 1
            val digitsStart = j
            var index = -1
            while (j < pattern.length && pattern[j].isDigit()) j++
            if (j > digitsStart && j < pattern.length && pattern[j] == '$') {
                index = pattern.substring(digitsStart, j).toInt() - 1
                j++
            }
            while (j < pattern.length && !pattern[j].isLetter()) j++
            if (j < pattern.length) {
                val argIndex = if (index >= 0) index else autoIndex++
                sb.append(args.getOrNull(argIndex)?.toString() ?: "")
                i = j + 1
                continue
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

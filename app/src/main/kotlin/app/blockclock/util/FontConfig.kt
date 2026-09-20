package app.blockclock.util

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The system font configuration: the family names the platform declares for the installed
 * font files. The home screen can only build a font by its family name — a `Typeface` is a
 * process-local object and does not survive the transfer to the launcher process (see
 * `android.graphics.LeakyTypefaceStorage`) — so the widget looks up the family of the
 * picked file here and lets the host build the font itself.
 *
 * There is no API for that name: `Font` has neither a family nor a PostScript getter, and
 * `Typeface.getSystemFontFamilyName` answers only for a typeface built from a name. The
 * names live in the files the platform reads itself: the preinstalled `fonts.xml` of the
 * system partition, the `font_fallback.xml` fallbacks, the vendor `fonts_customization.xml`
 * of the other partitions — and whichever configuration a vendor adds of its own, the way
 * Nothing declares its families in `ntfonts.xml`.
 *
 * A configuration is therefore recognised by its content and not by its name: the scan of
 * the standard partition directories keeps every XML that opens with a font declaration
 * root. The declarations are merged the way the platform merges them: the system comes
 * first and the first declaration of a family name wins, unless a vendor asks for a
 * replacement ([Family.replace]), so a file of a vendor family is never taken for a file of
 * a system family of the same name.
 *
 * Only the named families are read: the unnamed ones are pure fallback lists, their files
 * have no name to pass to the host.
 */
object FontConfig {

    /** The standard configuration directories of the Android partitions. */
    private val dirs = listOf("/system/etc", "/system_ext/etc", "/product/etc", "/vendor/etc", "/odm/etc")

    /** The configurations of the device: the declarations of the platform and of the vendors. */
    private val configs: List<File> by lazy(LazyThreadSafetyMode.NONE) {
        dirs.flatMap { dir ->
            File(dir).listFiles()
                ?.filter(::declaresFamilies)
                ?.sortedBy(File::getName)
                ?: emptyList()
        }
    }

    /** The named families of the device: the family name to the file names it declares. */
    private val families: Map<String, List<String>> by lazy(LazyThreadSafetyMode.NONE) {
        merge(configs.flatMap(::read))
    }

    /** The family name of every declared file, e.g. `Roboto-Regular.ttf` to `sans-serif`. */
    private val names: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        byFile(families)
    }

    /** The family of the font [file], or null when the configuration does not name it. */
    fun family(file: File): String? = names[file.name]

    /**
     * True for a name the platform resolves to a font family: a declared family or an alias
     * of one. The names live in the same configurations the platform reads, and no API
     * answers the question before the 31st level — `Typeface.getSystemFontFamilyName`
     * appears there and is missing from 28 to 30, where calling it fails the whole lookup.
     */
    fun canResolve(name: String): Boolean = name in families || name in aliases

    /** The alias names of the device: the alternative names of the declared families. */
    private val aliases: Set<String> by lazy(LazyThreadSafetyMode.NONE) {
        configs.flatMap(::readAliases).map(Alias::name).toSet()
    }

    /** The aliases that ask for a weight: the family name to the weight and the alias name. */
    private val weighted: Map<String, Map<Int, String>> by lazy(LazyThreadSafetyMode.NONE) {
        configs.flatMap(::readAliases)
            .filter { it.weight != null && it.to.isNotEmpty() }
            .groupBy(Alias::to)
            .mapValues { (_, aliases) -> aliases.asReversed().associate { it.weight!! to it.name } }
    }

    /**
     * The alias that asks the [family] for this [weight], or null when the device declares none.
     *
     * An alias of the configuration is a name of its own that the platform resolves to the family
     * with the requested weight, e.g. `source-sans-pro-semi-bold` asks `source-sans-pro` for the
     * 600. It is the only way to ask the host for a weight: `RemoteViews` reaches the style bits
     * alone, so the faces of a family that declares several weights would look the same.
     */
    fun alias(family: String, weight: Int): String? = weighted[family]?.get(weight)

    /** The families declared in the configuration [file], in the order of the file. */
    internal fun read(file: File): List<Family> = try {
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
            .children(FAMILY)
            .mapNotNull { family ->
                val name = family.getAttribute(NAME)
                val files = family.children(FONT)
                    .map { it.textContent.trim().substringAfterLast('/') }
                    .filter(String::isNotEmpty)
                when {
                    name.isEmpty() || files.isEmpty() -> null
                    else -> Family(name, files, family.getAttribute(CUSTOMIZATION) == REPLACE)
                }
            }
    } catch (_: Exception) {
        emptyList()
    }

    /** The aliases declared in the configuration [file]: the name, the family and the weight. */
    internal fun readAliases(file: File): List<Alias> = try {
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
            .children(ALIAS)
            .map { alias ->
                Alias(
                    name = alias.getAttribute(NAME),
                    to = alias.getAttribute(TO),
                    weight = alias.getAttribute(WEIGHT).toIntOrNull(),
                )
            }
            .filter { it.name.isNotEmpty() }
    } catch (_: Exception) {
        emptyList()
    }

    /** The merged families of the [declarations]: the system ones first, the vendors after. */
    internal fun merge(declarations: List<Family>): Map<String, List<String>> {
        val families = LinkedHashMap<String, List<String>>()
        declarations.forEach { family ->
            if (family.replace || family.name !in families) {
                families[family.name] = family.files
            }
        }
        return families
    }

    /** The family name of every file of the [families]: the first family that declares it. */
    internal fun byFile(families: Map<String, List<String>>): Map<String, String> = families.entries
        .flatMap { (name, files) -> files.map { it to name } }
        .distinctBy { (font, _) -> font }
        .toMap()

    /**
     * True for a font configuration: an XML that opens with a font declaration root. The
     * name is not checked, only the content — the vendors call their configurations whatever
     * they like. The head is read generously: the `fonts.xml` of the platform carries a long
     * deprecation comment before the root.
     */
    internal fun declaresFamilies(file: File): Boolean = when {
        !file.isFile -> false
        !file.name.endsWith(EXTENSION) -> false
        else -> try {
            val head = ByteArray(HEAD)
            val size = file.inputStream().use { it.read(head) }
            ROOTS.any(head.decodeToString(endIndex = size.coerceAtLeast(0))::contains)
        } catch (_: Exception) {
            false
        }
    }

    /** The children of this element with the [tag] name. */
    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { index ->
            (nodes.item(index) as? Element)?.takeIf { it.tagName == tag }
        }
    }

    /** A named family of a configuration: the name, its files and the vendor replacement flag. */
    internal data class Family(
        val name: String,
        val files: List<String>,
        val replace: Boolean = false,
    )

    /** An alias of a configuration: the name, the family it points to and the weight it asks. */
    internal data class Alias(
        val name: String,
        val to: String,
        val weight: Int?,
    )

    private const val FAMILY = "family"
    private const val ALIAS = "alias"
    private const val FONT = "font"
    private const val NAME = "name"
    private const val TO = "to"
    private const val WEIGHT = "weight"
    private const val CUSTOMIZATION = "customizationType"
    private const val REPLACE = "replace"
    private const val EXTENSION = ".xml"

    /** The bytes read to recognise a configuration by its root element. */
    private const val HEAD = 64 * 1024

    /** The roots of the font configurations: the platform one and the vendor customization. */
    private val ROOTS = listOf("<familyset", "<fonts-modification")
}

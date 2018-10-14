/** Author: RedFantom
 * License: GNU GPLv3 as in LICENSE.md
 * Copyright (c) 2018 RedFantom
 */
import java.io.File
import kotlin.collections.MutableMap
import kotlin.collections.Map
import kotlin.collections.mutableMapOf
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.DateTimeException
import java.time.format.DateTimeFormatter


fun remove_id_number(s: String): String {
    /** Remove the ID number of an ability or effect from a string
     *
     * Example:
     *   Laser Cannon {3290928496246784} -> Laser Cannon
     */
    return s.split("{").elementAt(0).strip()
}


fun parse_file_name(f: File): LocalDateTime? {
    /** Parse the file name of the given CombatLog file
     *
     * Pattern: combat_2016-01-15_20_37_08_109597.txt
     */
    try {
        return LocalDateTime.parse(f.name.removeSurrounding("combat_", ".txt"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss_SSSSSS"))
    } catch (e: DateTimeException) {
        return null
    }
}


fun line_to_event(line: String, d: LocalDate): Map<String, Any>? {
    /** Parse an event line into a Map of string to elements
     *
     * Line Map Elements:
     *  time -> LocalDateTime
     *  source -> String
     *  target -> String
     *  ability -> String
     *  effect -> String "$type: $effect"
     *  amount -> Int
     *  crit -> Boolean
     */
    val result: MutableMap<String, Any> = mutableMapOf()
    var elems: Sequence<String> = line.splitToSequence("]")
    elems = elems.map{ it.strip().removePrefix("[") }
    if (elems.count() != 6)
        return null

    result.put("line", line)
    val t: LocalTime = LocalTime.parse(elems.elementAt(0))
    result.put("time", LocalDateTime.of(d, t))
    result.put("source", elems.elementAt(1))
    result.put("target", elems.elementAt(2))
    result.put("ability", remove_id_number(elems.elementAt(3)))
    val effect_elems = elems.elementAt(4).split(":").
            map{ remove_id_number(it) }
    result.put("effect", "${effect_elems.elementAt(0)}: ${effect_elems.elementAt(1)}")
    val damage = elems.elementAt(5)
            .removeSurrounding("(", ")")
            .split(" ").elementAt(0)
    val amount = damage.replace("*", "")
    result.put("amount", if (amount != "") amount.toInt() else 0)
    result.put("crit", "*" in damage)

    return result.toMap()
}


fun file_to_events(f: File): Sequence<Map<String, Any>>? {
    /** Read the lines from a file and parse them into elements
     *
     * The lines in the file are parsed into Map<String, Any>? by
     * line_to_event. The Date element is provided by the file name. If
     * the file name cannot be parsed, then the result is null.
     */
    val d: LocalDate? = parse_file_name(f)?.toLocalDate()
    if (d == null) return null
    val lines: List<String> = f.readLines()
    val result: List<Map<String, Any>> = lines
            .map{ line_to_event(it, d) }
            .filterNotNull()
    return result.asSequence()
}


fun is_gsf_event(l: Map<String, Any>): Boolean {
    /** Determine whether a given event Map contains a GSF event */
    val source = !(l.get("source").toString().contains("@"))
    val target = !(l.get("target").toString().contains("@"))
    return source && target
}


fun split_file(f: File): Sequence<Sequence<Map<String, Any>>>? {
    /** Split a file into separate sequences of matches */
    val lines = file_to_events(f)
    lines ?: return null
    var is_match = false
    val match: MutableList<Map<String, Any>> = mutableListOf()
    val matches: MutableList<Sequence<Map<String, Any>>> = mutableListOf()
    for (line: Map<String, Any> in lines) {
        val gsf = is_gsf_event(line)
        if (gsf) {
            is_match = true
            match.add(line)
        } else if (!gsf && is_match) {
            matches.add(match.asSequence())
            match.clear()
            is_match = false
        }
    }
    return matches.asSequence()
}


fun get_player_id_list(lines: Sequence<Map<String, Any>>): Set<String> {
    /** Determine the player ID numbers of a list of events */
    val interim = lines.filter { it.get("source") == it.get("target") }
    return interim.map{ it.get("source")?.toString() }.filterNotNull().toSet()
}


fun main(args: Array<String>) {
    val r = split_file(File("/home/RedFantom/Documents/Star Wars - The Old Republic/CombatLogs/combat_2016-01-15_20_37_08_109597.txt"))
}

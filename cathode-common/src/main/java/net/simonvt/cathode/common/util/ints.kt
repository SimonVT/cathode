package net.simonvt.cathode.common.util

class Entry(val number: Int, val group: Int)

fun List<Int>.toRanges(): String {
  val sb = StringBuilder()

  this.distinct()
    .sorted()
    .mapIndexed { index, number -> Entry(number, number - index) }
    .groupBy { it.group }
    .forEach { (group, entries) ->
      if (sb.count() > 0) {
        sb.append(", ")
      }
      if (entries.size >= 3) {
        val first = entries.first().number
        if (first < 0) {
          sb.append("(").append(first).append(")")
        } else {
          sb.append(first)
        }

        sb.append("-")

        val last = entries.last().number
        if (last < 0) {
          sb.append("(").append(last).append(")")
        } else {
          sb.append(last)
        }
      } else {
        sb.append(entries.map { it.number }.joinToString(separator = ", "))
      }
    }

  return sb.toString()
}

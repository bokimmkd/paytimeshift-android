package com.paytimeshift.pts.domain

private val jobPalette = listOf(0xFF2488FFL, 0xFFFFA000L, 0xFFA040C5L, 0xFF00857CL, 0xFFDE5353L,
    0xFF546E7AL, 0xFF795548L, 0xFF536DFEL, 0xFF00A8B5L, 0xFFE65094L)

/** Reserve archived jobs too, so restoring one cannot create an ambiguous calendar color. */
fun jobColorAvailable(jobs: List<Job>, color: Long, editingId: String? = null): Boolean =
    jobs.none { it.id != editingId && (it.color and 0xFFFFFFFFL) == (color and 0xFFFFFFFFL) }

fun jobColorChoices(jobs: List<Job>, editingId: String? = null): List<Long> {
    val choices = (jobPalette + jobs.map { it.color and 0xFFFFFFFFL }).distinct().toMutableList()
    // Preserve unlimited jobs even when every standard swatch has been used.
    if (choices.none { jobColorAvailable(jobs, it, editingId) }) {
        var candidate = 0xFF3F51B5L
        while (!jobColorAvailable(jobs, candidate, editingId))
            candidate = 0xFF000000L or ((candidate + 0x005B3729L) and 0x00FFFFFFL)
        choices.add(candidate)
    }
    return choices
}

fun newJobColor(jobs: List<Job>): Long = jobColorChoices(jobs).first { jobColorAvailable(jobs, it) }

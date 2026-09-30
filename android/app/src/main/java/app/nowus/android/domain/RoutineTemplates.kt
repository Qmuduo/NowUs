package app.nowus.android.domain

enum class RoutineTemplate(val id: String, val title: String) {
    STUDENT("学生", "学生"),
    OFFICE_WORKER("上班族", "上班族"),
}

/** Editable starting points. City groups shift meal and commute times; they are not fixed claims about an individual. */
object RoutineTemplates {
    private enum class Region { EAST_ASIA, SOUTH_ASIA, WESTERN_EUROPE, OTHER_WEST }

    fun forCity(cityId: String, template: RoutineTemplate = RoutineTemplate.STUDENT): Schedule {
        val region = when (cityId) {
            "beijing", "shanghai", "tokyo" -> Region.EAST_ASIA
            "kathmandu" -> Region.SOUTH_ASIA
            "paris" -> Region.WESTERN_EUROPE
            else -> Region.OTHER_WEST
        }
        return Schedule(
            weekday = day(region, template, rest = false),
            rest = day(region, template, rest = true),
            templateId = template.id,
        )
    }

    private fun day(region: Region, template: RoutineTemplate, rest: Boolean): Rhythm {
        val sleep = if (rest) block("sleep", "睡觉", "23:30", "08:30") else block("sleep", "睡觉", "23:00", "07:00")
        val blocks = if (rest) restDay(region, sleep) else when (template) {
            RoutineTemplate.STUDENT -> studentDay(region, sleep)
            RoutineTemplate.OFFICE_WORKER -> officeDay(region, sleep)
        }
        return rhythm(blocks, if (rest) "休息" else if (template == RoutineTemplate.STUDENT) "上课" else "上班")
    }

    private fun studentDay(region: Region, sleep: RoutineBlock): List<RoutineBlock> = when (region) {
        Region.EAST_ASIA -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:15"), block("breakfast", "早餐", "07:15", "07:45"), block("commute-morning", "上学通勤", "07:45", "08:30"),
            block("morning-class", "上午上课", "08:30", "12:00"), block("lunch", "午餐", "12:00", "12:45"),
            block("nap", "午休", "12:45", "13:30"), block("afternoon-class", "下午上课", "13:30", "17:00"),
            block("commute-evening", "返程通勤", "17:00", "17:30"), block("dinner", "晚餐", "18:00", "19:00"),
            block("evening-study", "晚间学习", "19:00", "22:00"),
        )
        Region.SOUTH_ASIA -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "上学通勤", "08:00", "09:00"),
            block("morning-class", "上午上课", "09:00", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-class", "下午上课", "14:00", "17:00"),
            block("commute-evening", "返程通勤", "17:00", "18:00"), block("dinner", "晚餐", "19:00", "20:00"),
            block("evening-study", "晚间学习", "20:00", "22:00"),
        )
        Region.WESTERN_EUROPE -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "上学通勤", "08:00", "08:30"),
            block("morning-class", "上午上课", "08:30", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-class", "下午上课", "14:00", "17:00"),
            block("commute-evening", "返程通勤", "17:00", "18:00"), block("free-before-dinner", "自由时间", "18:00", "20:00"),
            block("dinner", "晚餐", "20:00", "21:00"), block("evening-study", "晚间学习", "21:00", "22:30"),
        )
        Region.OTHER_WEST -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "上学通勤", "08:00", "09:00"),
            block("morning-class", "上午上课", "09:00", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-class", "下午上课", "14:00", "17:00"),
            block("commute-evening", "返程通勤", "17:00", "18:00"), block("dinner", "晚餐", "18:30", "19:30"),
            block("evening-study", "晚间学习", "19:30", "22:00"),
        )
    }

    private fun officeDay(region: Region, sleep: RoutineBlock): List<RoutineBlock> = when (region) {
        Region.EAST_ASIA -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:15"), block("breakfast", "早餐", "07:15", "07:45"), block("commute-morning", "通勤", "08:00", "09:00"),
            block("morning-work", "上午上班", "09:00", "12:00"), block("lunch", "午餐", "12:00", "13:00"),
            block("nap", "午休", "13:00", "13:30"), block("afternoon-work", "下午上班", "13:30", "18:00"),
            block("commute-evening", "返程通勤", "18:00", "19:00"), block("dinner", "晚餐", "19:00", "20:00"),
            block("evening-free", "晚间休息", "20:00", "22:00"),
        )
        Region.SOUTH_ASIA -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "通勤", "08:00", "09:00"),
            block("morning-work", "上午上班", "09:00", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-work", "下午上班", "14:00", "18:00"),
            block("commute-evening", "返程通勤", "18:00", "19:00"), block("dinner", "晚餐", "19:00", "20:00"),
            block("evening-free", "晚间休息", "20:00", "22:00"),
        )
        Region.WESTERN_EUROPE -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "通勤", "08:00", "09:00"),
            block("morning-work", "上午上班", "09:00", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-work", "下午上班", "14:00", "17:30"),
            block("commute-evening", "返程通勤", "17:30", "18:30"), block("free-before-dinner", "自由时间", "18:30", "20:00"),
            block("dinner", "晚餐", "20:00", "21:00"), block("evening-free", "晚间休息", "21:00", "22:30"),
        )
        Region.OTHER_WEST -> listOf(
            sleep, block("preparation", "起床准备", "07:00", "07:30"), block("breakfast", "早餐", "07:30", "08:00"), block("commute-morning", "通勤", "08:00", "09:00"),
            block("morning-work", "上午上班", "09:00", "12:30"), block("lunch", "午餐", "12:30", "13:30"),
            block("midday-rest", "午间休息", "13:30", "14:00"), block("afternoon-work", "下午上班", "14:00", "17:30"),
            block("commute-evening", "返程通勤", "17:30", "18:30"), block("dinner", "晚餐", "18:30", "19:30"),
            block("evening-free", "晚间休息", "19:30", "22:00"),
        )
    }

    private fun restDay(region: Region, sleep: RoutineBlock): List<RoutineBlock> {
        val dinner = when (region) {
            Region.EAST_ASIA -> "18:30" to "19:30"
            Region.SOUTH_ASIA -> "19:00" to "20:00"
            Region.WESTERN_EUROPE -> "20:00" to "21:00"
            Region.OTHER_WEST -> "18:30" to "19:30"
        }
        val lunch = when (region) {
            Region.EAST_ASIA -> "12:00" to "13:00"
            else -> "12:30" to "13:30"
        }
        val blocks = mutableListOf(
            sleep,
            block("preparation", "起床准备", "08:30", "08:45"),
            block("breakfast", "早餐", "08:45", "09:15"),
            block("lunch", "午餐", lunch.first, lunch.second),
            block("afternoon-free", "自由安排", "14:00", "18:00"),
        )
        if (dinner.first == "20:00") blocks += block("free-before-dinner", "自由时间", "18:00", "20:00")
        blocks += block("dinner", "晚餐", dinner.first, dinner.second)
        blocks += block("evening-free", "晚间休息", dinner.second, "22:30")
        return blocks
    }

    private fun rhythm(blocks: List<RoutineBlock>, fallbackActivity: String): Rhythm {
        val sleep = blocks.single { it.id == "sleep" }
        val main = blocks.firstOrNull { it.id.startsWith("morning-") }
        return Rhythm(
            sleepStart = sleep.start,
            sleepEnd = sleep.end,
            activity = main?.label ?: fallbackActivity,
            activityStart = main?.start ?: "10:00",
            activityEnd = main?.end ?: "12:00",
            blocks = blocks,
        )
    }

    private fun block(id: String, label: String, start: String, end: String) = RoutineBlock(id, label, start, end, categoryFor(id))

    private fun categoryFor(id: String): RoutineCategory = when (id) {
        "sleep" -> RoutineCategory.SLEEP
        "preparation" -> RoutineCategory.PREPARATION
        "breakfast", "lunch", "dinner" -> RoutineCategory.MEAL
        "commute-morning", "commute-evening" -> RoutineCategory.COMMUTE
        "morning-class", "afternoon-class", "morning-work", "afternoon-work", "evening-study" -> RoutineCategory.STUDY_WORK
        "nap", "midday-rest", "evening-free" -> RoutineCategory.REST
        else -> RoutineCategory.OTHER
    }
}

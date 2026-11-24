package org.littlegrove.learn

import android.content.Context
import android.graphics.Color
import org.json.JSONObject

data class Lesson(val prompt: String, val hint: String, val options: List<String>, val answer: String)

data class Module(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val color: Int,
    val type: String,
    val lessons: List<Lesson>
)

object LessonCatalog {
    fun load(context: Context): List<Module> {
        return try {
            val source = context.assets.open("content/lessons.json").bufferedReader().use { it.readText() }
            val rows = JSONObject(source).getJSONArray("modules")
            (0 until rows.length()).map { index ->
                val item = rows.getJSONObject(index)
                val lessonRows = item.getJSONArray("lessons")
                val lessons = (0 until lessonRows.length()).map { lessonIndex ->
                    val lesson = lessonRows.getJSONObject(lessonIndex)
                    val options = lesson.getJSONArray("options")
                    Lesson(
                        prompt = lesson.getString("prompt"),
                        hint = lesson.getString("hint"),
                        options = (0 until options.length()).map { options.getString(it) },
                        answer = lesson.getString("answer")
                    )
                }
                Module(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    subtitle = item.getString("subtitle"),
                    icon = item.getString("icon"),
                    color = Color.parseColor(item.getString("color")),
                    type = item.getString("type"),
                    lessons = lessons
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

package org.littlegrove.learn

import android.content.Context

class LocalProgress(context: Context) {
    private val preferences = context.getSharedPreferences("little_grove_progress", Context.MODE_PRIVATE)

    fun completed(moduleId: String): Int = preferences.getInt("completed_" + moduleId, 0)
    fun stars(): Int = preferences.getInt("stars", 0)

    fun finish(moduleId: String) {
        preferences.edit()
            .putInt("completed_" + moduleId, completed(moduleId) + 1)
            .putInt("stars", stars() + 1)
            .apply()
    }
}

package org.littlegrove.learn

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var progress: LocalProgress
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var modules = emptyList<Module>()
    private var activeModule: Module? = null
    private var lessonIndex = 0
    private var answerLocked = false
    private var sessionGeneration = 0
    private var feedback: TextView? = null
    private var pendingPrompt: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = GrovePalette.paper
        window.navigationBarColor = GrovePalette.paper
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        progress = LocalProgress(this)
        modules = LessonCatalog.load(this)
        speech = TextToSpeech(this, this)
        showHome()
    }

    override fun onInit(status: Int) {
        speechReady = status == TextToSpeech.SUCCESS
        if (speechReady) {
            speech?.language = Locale.US
            pendingPrompt?.let { speak(it) }
        }
    }

    override fun onDestroy() {
        speech?.stop()
        speech?.shutdown()
        super.onDestroy()
    }

    private fun showHome() {
        sessionGeneration += 1
        activeModule = null
        val page = page()
        val content = column()
        content.addView(text("Little Grove", 31, GrovePalette.ink, true))
        content.addView(text("A little learning, one happy step at a time.", 17, GrovePalette.muted, false, 8))

        val welcome = roundedPanel(Color.rgb(222, 242, 231), 24)
        welcome.orientation = LinearLayout.VERTICAL
        welcome.setPadding(dp(22), dp(20), dp(22), dp(20))
        welcome.addView(text("🌱  Your learning garden", 20, GrovePalette.ink, true))
        welcome.addView(text("You have collected " + progress.stars() + " bright " + starWord() + ". Pick an activity to begin!", 16, GrovePalette.ink, false, 8))
        content.addView(welcome, marginParams(top = 20, bottom = 18))

        content.addView(text("Choose an activity", 22, GrovePalette.ink, true))
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }
        modules.forEachIndexed { index, module ->
            val card = moduleCard(module)
            val params = LinearLayout.LayoutParams(0, dp(154), 1f)
            params.setMargins(if (index % 2 == 0) 0 else dp(7), dp(8), if (index % 2 == 0) dp(7) else 0, dp(6))
            if (index == 2) {
                content.addView(grid)
            }
            if (index == 2) {
                // The second row uses a fresh line so the cards remain easy to scan.
                val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f }
                secondRow.addView(card, params)
                content.addView(secondRow)
            } else if (index == 3) {
                val lastRow = (content.getChildAt(content.childCount - 1) as? LinearLayout)
                lastRow?.addView(card, params)
            } else {
                grid.addView(card, params)
            }
        }
        if (grid.parent == null) content.addView(grid)

        val footer = text("Made for curious hands and growing minds.", 14, GrovePalette.muted, false, 22)
        footer.gravity = Gravity.CENTER
        content.addView(footer)
        attachContent(page, content)
        setContentView(page)
    }

    private fun moduleCard(module: Module): View {
        val card = roundedPanel(module.color, 22)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(dp(17), dp(13), dp(15), dp(13))
        card.isClickable = true
        card.isFocusable = true
        card.contentDescription = module.title + ". " + module.subtitle
        card.setOnClickListener { startModule(module) }
        card.addView(text(module.icon, 30, GrovePalette.ink, false))
        card.addView(text(module.title, 19, GrovePalette.ink, true, 3))
        card.addView(text(module.subtitle, 13, GrovePalette.muted, false, 2))
        card.addView(text("Done " + progress.completed(module.id), 12, GrovePalette.muted, true, 6))
        return card
    }

    private fun startModule(module: Module) {
        sessionGeneration += 1
        activeModule = module
        lessonIndex = 0
        showLesson()
    }

    private fun showLesson() {
        val module = activeModule ?: return showHome()
        if (lessonIndex >= module.lessons.size) return showCompletion(module)
        answerLocked = false
        val lesson = module.lessons[lessonIndex]
        val page = page()
        val content = column()

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(actionButton("‹  Home", GrovePalette.white, GrovePalette.greenDark) { showHome() }, LinearLayout.LayoutParams(0, dp(60), 1f))
        val count = text((lessonIndex + 1).toString() + " of " + module.lessons.size, 15, GrovePalette.muted, true)
        count.gravity = Gravity.CENTER
        top.addView(count, LinearLayout.LayoutParams(0, dp(60), 1f))
        val replay = actionButton("🔊  Hear it", GrovePalette.white, GrovePalette.greenDark) { speak(lesson.prompt) }
        replay.contentDescription = "Read the activity prompt aloud"
        top.addView(replay, LinearLayout.LayoutParams(0, dp(60), 1f))
        content.addView(top)

        val promptPanel = roundedPanel(module.color, 24)
        promptPanel.orientation = LinearLayout.VERTICAL
        promptPanel.gravity = Gravity.CENTER
        promptPanel.setPadding(dp(20), dp(22), dp(20), dp(22))
        promptPanel.minimumHeight = dp(142)
        promptPanel.addView(text(module.icon, 38, GrovePalette.ink, false))
        val prompt = text(lesson.prompt, 25, GrovePalette.ink, true, 5)
        prompt.gravity = Gravity.CENTER
        promptPanel.addView(prompt)
        content.addView(promptPanel, marginParams(top = 22, bottom = 14))

        val hint = text(lesson.hint, 16, GrovePalette.muted, false, 4)
        hint.gravity = Gravity.CENTER
        content.addView(hint, marginParams(bottom = 12))

        feedback = text("", 17, GrovePalette.greenDark, true, 8)
        feedback?.gravity = Gravity.CENTER
        content.addView(feedback, marginParams(bottom = 4))

        if (module.type == "trace") {
            val pad = TracePad(this, lesson.answer)
            content.addView(pad, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(290)))
            content.addView(actionButton("Clear and try again", GrovePalette.white, GrovePalette.greenDark) { pad.clear() }, marginParams(top = 8))
            content.addView(actionButton("I traced it!", GrovePalette.green, GrovePalette.white) {
                if (pad.hasDrawing()) acceptCorrect("Lovely tracing!") else showFeedback("Try drawing the letter with your finger first.", false)
            }, marginParams(top = 10))
        } else {
            lesson.options.forEach { option ->
                val choice = actionButton(option, GrovePalette.white, GrovePalette.ink) { choose(lesson, option) }
                content.addView(choice, marginParams(top = 6, bottom = 6))
            }
        }
        attachContent(page, content)
        setContentView(page)
        speak(lesson.prompt)
    }

    private fun choose(lesson: Lesson, choice: String) {
        if (answerLocked) return
        if (choice == lesson.answer) {
            acceptCorrect("You found it! Great thinking.")
        } else {
            showFeedback("Nice try. " + lesson.hint, false)
        }
    }

    private fun acceptCorrect(message: String) {
        if (answerLocked) return
        answerLocked = true
        showFeedback(message, true)
        val generation = sessionGeneration
        currentFocus?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        feedback?.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        window.decorView.postDelayed({
            if (generation == sessionGeneration && activeModule != null) {
                lessonIndex += 1
                showLesson()
            }
        }, 850)
    }

    private fun showFeedback(message: String, positive: Boolean) {
        feedback?.text = message
        feedback?.setTextColor(if (positive) GrovePalette.greenDark else GrovePalette.ink)
        feedback?.contentDescription = message
    }

    private fun showCompletion(module: Module) {
        progress.finish(module.id)
        val page = page()
        val content = column()
        content.gravity = Gravity.CENTER_HORIZONTAL
        val badge = roundedPanel(Color.rgb(255, 239, 204), 32)
        badge.gravity = Gravity.CENTER
        badge.minimumHeight = dp(190)
        badge.addView(text("⭐", 64, GrovePalette.ink, false))
        content.addView(badge, marginParams(top = 30, bottom = 24))
        val title = text("Learning looks good on you!", 27, GrovePalette.ink, true)
        title.gravity = Gravity.CENTER
        content.addView(title)
        val detail = text("You finished " + module.title.lowercase(Locale.US) + ". Your progress is saved on this device.", 17, GrovePalette.muted, false, 10)
        detail.gravity = Gravity.CENTER
        content.addView(detail)
        content.addView(actionButton("Choose another activity", GrovePalette.green, GrovePalette.white) { showHome() }, marginParams(top = 26))
        content.addView(actionButton("Play " + module.title + " again", GrovePalette.white, GrovePalette.greenDark) { startModule(module) }, marginParams(top = 10))
        attachContent(page, content)
        setContentView(page)
        speak("Great job. You finished " + module.title + ".")
    }

    private fun speak(message: String) {
        if (speechReady) {
            pendingPrompt = null
            speech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "little_grove_prompt")
        } else {
            pendingPrompt = message
        }
    }

    private fun starWord(): String = if (progress.stars() == 1) "star" else "stars"

    private fun page(): FrameLayout {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(GrovePalette.paper)
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.clipToPadding = false
        scroll.setPadding(dp(16), dp(12), dp(16), dp(28))
        frame.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(8), dp(2), dp(12))
    }

    private fun attachContent(page: FrameLayout, content: View) {
        val scroll = page.getChildAt(0) as ScrollView
        val width = minOf(resources.displayMetrics.widthPixels - dp(32), dp(760))
        scroll.addView(content, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        })
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean, top: Int = 0): TextView {
        return TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
            setPadding(0, dp(top), 0, 0)
        }
    }

    private fun actionButton(label: String, backgroundColor: Int, foreground: Int, click: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 18f
            setTextColor(foreground)
            setAllCaps(false)
            isSoundEffectsEnabled = false
            minHeight = dp(64)
            minimumHeight = dp(64)
            minWidth = dp(64)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedDrawable(backgroundColor, 18)
            stateListAnimator = null
            setOnClickListener { click() }
        }
    }

    private fun roundedPanel(color: Int, radius: Int): LinearLayout = LinearLayout(this).apply {
        background = roundedDrawable(color, radius)
        clipToOutline = true
    }

    private fun roundedDrawable(color: Int, radius: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun marginParams(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

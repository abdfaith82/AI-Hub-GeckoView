package com.abd.aihubgecko

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// تطبيق أصلي بالكامل: كود جديد، وتصميم جديد، ولا ينسخ كود أي تطبيق آخر.
// الفكرة: مركز واحد يفتح المواقع الرسمية للمساعدات الستة داخل التطبيق
// (GeckoView — محرك موزيلا بدل WebView النظام)، وتسجّل الدخول أنت
// بنفسك بحسابك في كل موقع — بلا أتمتة لتسجيل الدخول وبلا سرقة جلسات
// وبلا نسخ شعارات.
// v1.1 — إصلاحات القارئ: إعادة محاولة تثبيت الإضافة + تحديث تلقائي
// مرة واحدة إن حمّلت الصفحة بلا قراءة، ومسح نص الصفحة القديم عند
// تغيّر الرابط (محادثة جديدة) مع زرّي مسح، وإخفاء زر قراءة الصفحات
// عندما يكون القائد بلا API رسمي (Meta AI / Antigravity).
// v1.0-gecko — لماذا GeckoView؟ هو الطريق الوحيد المدعوم لإضافة داخل
// التطبيق: إضافة مدمجة (assets/webext) تقرأ نص الصفحة الظاهرة وترسله
// للتطبيق عبر الرسائل الأصلية — فيرى القائد نص صفحات الويب. قراءة
// فقط: الإضافة لا تكتب شيئاً في الصفحات ولا ترسل الأوامر.
// v1.1 — مقسّم المهام: المهمة الرئيسية تُقسَّم محلياً (قوالب مهنية حسب
// نوع المهمة، بلا أي API خارجي) إلى مهام فرعية، كل واحدة تُسند إلى
// النموذج الأقوى فيها. الإرسال يدوي ومشروع: يُنسخ السؤال المجهّز
// ويُفتح تبويب النموذج، فتلصقه أنت بيدك — لا حقن جافاسكربت في الصفحات
// ولا أتمتة لتسجيل الدخول، لأن ذلك يخالف شروط تلك المواقع ويتكسر مع
// كل تحديث. ثم تلصق نتيجة كل نموذج في بطاقته، ويدمجها التطبيق في
// تقرير نهائي واحد.

data class AiService(
    val id: String,
    val name: String,
    val company: String,
    val url: String,
    val color: Color,
    val initials: String,
    val desc: String,
    val strength: String,
    val limit: String
)

fun servicesList() = listOf(
    AiService(
        "chatgpt", "ChatGPT", "OpenAI", "https://chatgpt.com",
        Color(0xFF0E7A5F), "GP",
        "الأشهر والأوسع استخداماً: كتابة وبحث وبرمجة وصور وصوت.",
        "الأفضل للاستخدام العام والمهام المتنوعة.",
        "مجاني بحد للرسائل؛ المزايا الكاملة باشتراك Plus."
    ),
    AiService(
        "grok", "Grok", "xAI", "https://grok.com",
        Color(0xFF1D1D1F), "Gk",
        "أخبار لحظية من منصة X، وتوليد صور وفيديو (Imagine) وصوت.",
        "الأفضل للأخبار العاجلة وما يدور الآن على X.",
        "مجاني بحد محدود؛ المزايا الكاملة باشتراك SuperGrok."
    ),
    AiService(
        "gemini", "Gemini", "Google", "https://gemini.google.com",
        Color(0xFF1A73E8), "Ge",
        "مساعد جوجل: بحث وملفات طويلة، ويتكامل مع Gmail وDrive.",
        "الأفضل إذا كان عملك كله في حساب جوجل.",
        "مجاني؛ المزايا المتقدمة باشتراك Google AI Pro/Ultra."
    ),
    AiService(
        "claude", "Claude", "Anthropic", "https://claude.ai",
        Color(0xFFD97757), "Cl",
        "كتابة وتحليل وبرمجة وملفات طويلة، مع مشاريع وملفات Artifacts.",
        "الأفضل للكتابة الطويلة والتحليل والبرمجة الدقيقة.",
        "مجاني بحد يومي؛ المزايا الكاملة باشتراك Pro."
    ),
    AiService(
        "metaai", "Meta AI", "Meta", "https://meta.ai",
        Color(0xFF0082FB), "Me",
        "مساعد ميتا: مدمج أيضاً في واتساب وإنستغرام وفيسبوك.",
        "الأفضل للمحادثة السريعة المجانية بلا تعقيد.",
        "مجاني بلا خطة مدفوعة معلنة."
    ),
    AiService(
        "antigravity", "Antigravity", "Google", "https://antigravity.google",
        Color(0xFF7C4DFF), "An",
        "منصة جوجل لبرمجة الوكلاء: يكتب الكود ويشغّله ويختبره بنفسه.",
        "الأفضل للبرمجة الاحترافية بوكلاء مستقلين.",
        "سطح مكتب فقط (ويندوز/ماك/لينكس) — بلا تطبيق أندرويد، وتديره من الهاتف عبر واجهة التحكم عن بعد في الموقع."
    )
)

data class SubTask(
    val role: String,
    val modelIndex: Int,
    val instruction: String,
    val prompt: String,
    val result: String = "",
    val status: String = "لم يُرسل",
    val startedAt: Long = 0,
    val doneAt: Long = 0
)

// كل نوع مهمة يُقسَّم إلى 6 أدوار مهنية، وكل دور يُسند إلى النموذج
// الأقوى فيه. التقسيم محلي بالكامل — قوالب ثابتة يملؤها نص مهمتك.
fun splitTask(mainTask: String, type: String): List<SubTask> {
    // (الدور بالعربية، رقم النموذج، التعليمات بالإنجليزية لأن النماذج
    // تجيب أدق بها في المهام التقنية، مع نص المهمة الأصلي كما كتبه المستخدم)
    val roles: List<Triple<String, Int, String>> = when (type) {
        "بحث" -> listOf(
            Triple("الخلفية والتعريفات", 2, "Give the essential background and clear definitions. Cover only this part, in depth, with headings."),
            Triple("آخر الأخبار والتطورات", 1, "Report the very latest news, updates and trends. Mention dates and what changed recently. Cover only this part."),
            Triple("التحليل العميق والمقارنة", 3, "Analyse deeply: compare viewpoints, pros and cons, and hidden assumptions. Cover only this part."),
            Triple("الأفكار والزوايا الإبداعية", 0, "Suggest creative ideas, fresh angles and unexpected approaches. Cover only this part."),
            Triple("ملخص مبسط للمبتدئين", 4, "Explain it simply for a complete beginner, with a short everyday example. Cover only this part."),
            Triple("خطة عمل من النتائج", 0, "Turn the findings into a practical step-by-step action plan with priorities. Cover only this part.")
        )
        "كتابة" -> listOf(
            Triple("الهيكل والمسودة الرئيسية", 3, "Write the main draft with a clear structure: introduction, body sections and conclusion. Cover only this part."),
            Triple("تحسين الأسلوب والصياغة", 0, "Rewrite and polish the style: make it vivid, fluent and engaging, keeping the same meaning. Cover only this part."),
            Triple("تدقيق الحقائق والأرقام", 2, "Check the facts and figures carefully, correct any error, and flag anything uncertain. Cover only this part."),
            Triple("أمثلة وقصص حديثة", 1, "Add fresh, recent real examples and short stories that support the text. Cover only this part."),
            Triple("نسخة مختصرة للنشر", 4, "Produce a short punchy version suitable for posting, keeping the key points. Cover only this part."),
            Triple("المراجعة النهائية والتدقيق", 3, "Do a final review: fix language, grammar and flow, and list what you changed. Cover only this part.")
        )
        "برمجة" -> listOf(
            Triple("تحليل المتطلبات والتصميم", 3, "Analyse the requirements and design the solution: components, data flow and key decisions. No code yet. Cover only this part."),
            Triple("كتابة الكود الأساسي", 0, "Write the main working code, clean and commented, in the most suitable language. Cover only this part."),
            Triple("المكتبات والبدائل", 2, "List the best libraries and alternative approaches, with pros and cons of each. Cover only this part."),
            Triple("الأخطاء الشائعة وأحدث الممارسات", 1, "List the common pitfalls and the latest best practices for this task. Cover only this part."),
            Triple("شرح الكود ببساطة", 4, "Explain the code simply, step by step, as if teaching a beginner. Cover only this part."),
            Triple("التنفيذ والاختبار كوكيل", 5, "Act as a coding agent: give the full implementation plan, the files to create, and the tests to run to verify it works. Cover only this part.")
        )
        else -> listOf( // تحليل عام
            Triple("جمع المعلومات الأساسية", 2, "Gather the essential facts and figures about this topic, organised with headings. Cover only this part."),
            Triple("التحليل المنطقي", 3, "Analyse logically: causes, effects, strengths, weaknesses and risks. Cover only this part."),
            Triple("الرأي العام والاتجاهات", 1, "Summarise public opinion and current trends around this topic. Cover only this part."),
            Triple("الحلول المقترحة", 0, "Propose practical solutions, ranked by impact and ease. Cover only this part."),
            Triple("الملخص التنفيذي", 4, "Write a short executive summary a busy person can read in one minute. Cover only this part."),
            Triple("خطة العمل", 0, "Turn everything into a concrete action plan with steps, order and priorities. Cover only this part.")
        )
    }
    return roles.map { (role, model, instruction) ->
        SubTask(
            role = role,
            modelIndex = model,
            instruction = instruction,
            prompt = "$instruction\n\nMain task: $mainTask\n\nYour part only: $role.\nAnswer in Arabic, with clear headings."
        )
    }
}

// ===== v1.5 — النموذج القائد =====
// القائد نموذج (عبر API الرسمي) يخطط التقسيم بنفسه بدل القوالب
// الثابتة: يستلم المهمة الرئيسية ويعيد 6 أسطر بصيغة صارمة
// "عنوان عربي قصير | ModelName | تعليمات إنجليزية مفصلة".
// التطبيق يحلل الأسطر إلى مهام فرعية، وإن تعذّر فهمها (أقل من
// سطرين صالحين) يعود إلى التقسيم المحلي splitTask.
fun conductorPlanPrompt(mainTask: String, type: String): String =
    "You are the lead coordinator of a team of AI models: ChatGPT, Grok, Gemini and Claude.\n" +
    "Main task: " + mainTask + "\nTask type: " + type + "\n" +
    "Split this task into exactly 6 subtasks, one per line, and assign each to the model best suited for it (use each model's strengths; you may reuse a model).\n" +
    "Reply with ONLY the 6 lines, no introduction, no markdown, no bold markers, no code fences.\n" +
    "Each line must have exactly this format:\n" +
    "Short Arabic role title | ModelName | Detailed English instruction for that subtask\n" +
    "Each instruction must end with: Answer in Arabic, with clear headings."

fun parseConductorPlan(text: String, mainTask: String): List<SubTask> {
    val models = mapOf("chatgpt" to 0, "gpt" to 0, "grok" to 1, "gemini" to 2, "claude" to 3)
    val out = mutableListOf<SubTask>()
    for (raw in text.lines()) {
        var line = raw.trim()
        if (line.startsWith("```")) continue
        line = line.replace(Regex("^\\s*\\d+[.)\\-]\\s*"), "")
        val parts = line.split("|").map { it.trim() }
        if (parts.size < 3) continue
        val role = parts[0].trim().trimStart('#', '*', ' ').replace("**", "").trim()
        val modelKey = parts[1].lowercase()
        val modelIndex = models.entries.firstOrNull { (k, _) -> modelKey.contains(k) }?.value ?: continue
        val instruction = parts.subList(2, parts.size).joinToString(" | ").trim()
        if (role.isBlank() || instruction.isBlank()) continue
        out.add(
            SubTask(
                role = role,
                modelIndex = modelIndex,
                instruction = instruction,
                prompt = "$instruction\n\nMain task: $mainTask\n\nYour part only: $role.\nAnswer in Arabic, with clear headings."
            )
        )
        if (out.size >= 6) break
    }
    return out
}

// ===== الطريقة الشرعية للأتمتة: الـ API الرسمي =====
// الحقن في صفحات الويب (WebView.evaluateJavascript لملء صندوق المحادثة
// والضغط على زر الإرسال) موجود تقنياً، لكنه ليس شرعياً: شروط ChatGPT
// وClaude وGrok تمنع الوصول الآلي لمواقعها إلا عبر API الرسمي، والحقن
// يتكسر مع كل تغيير في الصفحة، وتكشفه فحوصات CAPTCHA، وقد يُحظر الحساب.
// لذلك لا يوجد حقن في هذا التطبيق أبداً. الطريقة الشرعية الوحيدة
// للأتمتة الكاملة هي API الرسمي بمفتاحك أنت — وهذا ما يفعله الكود أدناه.
// المفاتيح تُحفظ في التطبيق فقط على الهاتف، ولا تُرسل إلا إلى API
// الشركة صاحبة المفتاح نفسها.
fun prefs(context: Context) = context.getSharedPreferences("aihub_keys", Context.MODE_PRIVATE)
fun getKey(context: Context, name: String): String = prefs(context).getString(name, "").orEmpty()
fun getModel(context: Context, provider: String, default: String): String {
    val v = prefs(context).getString("model_$provider", "").orEmpty()
    return v.ifBlank { default }
}
fun providerFor(modelIndex: Int): String? = when (modelIndex) {
    0 -> "openai"
    1 -> "xai"
    2 -> "gemini"
    3 -> "anthropic"
    10 -> "custom" // v1.2 — القائد المخصص: نقطة نهاية متوافقة مع OpenAI
    else -> null // Meta AI وAntigravity: بلا API محادثة رسمي — يدوي فقط
}
fun defaultModel(provider: String): String = when (provider) {
    "openai" -> "gpt-4o-mini"
    "custom" -> "gpt-4o-mini"
    "xai" -> "grok-4"
    "gemini" -> "gemini-2.5-flash"
    "anthropic" -> "claude-sonnet-4-5-20250929"
    else -> ""
}

suspend fun callApi(provider: String, apiKey: String, model: String, prompt: String): String = withContext(Dispatchers.IO) {
    try {
        val urlStr: String
        val headers: Map<String, String>
        val body: String
        when (provider) {
            "anthropic" -> {
                urlStr = "https://api.anthropic.com/v1/messages"
                headers = mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
                body = JSONObject().put("model", model).put("max_tokens", 2000)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt))).toString()
            }
            "gemini" -> {
                urlStr = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                headers = emptyMap()
                body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))).toString()
            }
            "xai" -> {
                urlStr = "https://api.x.ai/v1/chat/completions"
                headers = mapOf("Authorization" to "Bearer $apiKey")
                body = JSONObject().put("model", model).put("max_tokens", 2000)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt))).toString()
            }
            else -> { // openai
                urlStr = "https://api.openai.com/v1/chat/completions"
                headers = mapOf("Authorization" to "Bearer $apiKey")
                body = JSONObject().put("model", model).put("max_tokens", 2000)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt))).toString()
            }
        }
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            doOutput = true
            connectTimeout = 60000
            readTimeout = 120000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) return@withContext "خطأ API ($code): ${text.take(300)}"
        val json = JSONObject(text)
        when (provider) {
            "anthropic" -> json.getJSONArray("content").getJSONObject(0).getString("text")
            "gemini" -> json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            else -> json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    } catch (e: Exception) {
        "خطأ في الاتصال: ${e.message}"
    }
}

// ===== v1.2 — نقطة نهاية مخصصة متوافقة مع OpenAI =====
// خادم يفهم صيغة OpenAI لطلبات المحادثة (POST /v1/chat/completions)
// — مثل Groq المجاني، أو OpenRouter، أو خادم FreeLLMAPI إن شغّلته
// على جهاز آخر. الرابط والمفتاح واسم النموذج تُحفظ في التطبيق فقط.
suspend fun callApiCustom(baseUrl: String, apiKey: String, model: String, prompt: String): String = withContext(Dispatchers.IO) {
    try {
        val clean = baseUrl.trim().trimEnd('/')
        if (clean.isBlank()) return@withContext "خطأ: أدخل رابط الخادم المخصص أولاً"
        val urlStr = "$clean/v1/chat/completions"
        val body = JSONObject().put("model", model.ifBlank { "gpt-4o-mini" }).put("max_tokens", 2000)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt))).toString()
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
            connectTimeout = 60000
            readTimeout = 120000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) return@withContext "خطأ API ($code): ${text.take(300)}"
        JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    } catch (e: Exception) {
        "خطأ في الاتصال: ${e.message}"
    }
}

// القائد قد يكون أحد النماذج الأربعة أو المخصص (10). هذه الدالة
// ترسل للقائد أياً كان نوعه، وتُستخدم في التخطيط والدمج وقراءة الصفحات.
suspend fun callConductorApi(context: Context, provider: String, prompt: String): String {
    return if (provider == "custom") {
        callApiCustom(getKey(context, "url_custom"), getKey(context, "key_custom"), getModel(context, "custom", defaultModel("custom")), prompt)
    } else {
        callApi(provider, getKey(context, "key_$provider"), getModel(context, provider, defaultModel(provider)), prompt)
    }
}

@Composable
fun KeyField(title: String, where: String, key: String, onKey: (String) -> Unit, model: String, onModel: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("احصل على المفتاح من: $where", fontSize = 12.sp, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = key, onValueChange = onKey,
                label = { Text("المفتاح (API Key)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = model, onValueChange = onModel,
                label = { Text("اسم النموذج") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AiHubApp()
            }
        }
    }
}

@Composable
fun AiHubApp() {
    val context = LocalContext.current
    val services = remember { servicesList() }
    var selected by remember { mutableStateOf(0) }
    var screen by remember { mutableStateOf("home") } // home | split | browser | keys
    var returnToSplit by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf("") }
    // ===== GeckoView + الإضافة المدمجة =====
    // محرك Gecko بدل WebView النظام: جلسة GeckoSession واحدة لكل
    // نموذج تُنشأ مرة واحدة وتبقى حيّة — التبديل بين التبويبات لا
    // يعيد تحميلها، وتسجيل الدخول يبقى فيها.
    // والإضافة المدمجة (assets/webext) تقرأ نص الصفحة الظاهرة
    // وترسله للتطبيق عبر الرسائل الأصلية — فيرى القائد نص صفحات
    // الويب. قراءة فقط: الإضافة لا تكتب شيئاً في الصفحات.
    // حدّ صادق: هذا يثبّت الواجهة والجلسة فقط — إرسال أي مهمة
    // واستقبال إجابتها يحتاجان إنترنت دائماً.
    val geckoRuntime = remember { GeckoRuntime.create(context) }
    val sessionPool = remember { mutableMapOf<Int, GeckoSession>() }
    val loadedTabs = remember { mutableStateMapOf<Int, Boolean>() }
    val canGoBackMap = remember { mutableStateMapOf<Int, Boolean>() }
    // نصوص الصفحات كما قرأتها الإضافة (آخر نسخة لكل نموذج)
    val pageTexts = remember { mutableStateMapOf<Int, String>() }
    // v1.1 — آخر رابط لكل نموذج (لمسح النص القديم عند تغيّر المحادثة)،
    // وعلامة «حُدّثت تلقائياً مرة» لكل تبويب، وحارس تثبيت الإضافة.
    val pageUrls = remember { mutableStateMapOf<Int, String>() }
    val autoReloaded = remember { mutableStateMapOf<Int, Boolean>() }
    var installingExtension by remember { mutableStateOf(false) }
    var extensionAttempts by remember { mutableStateOf(0) }
    var extensionReady by remember { mutableStateOf(false) }
    var geckoViewRef by remember { mutableStateOf<GeckoView?>(null) }

    fun serviceIndexForUrl(url: String?): Int? {
        if (url == null) return null
        return services.indexOfFirst { service ->
            val host = Uri.parse(service.url).host ?: return@indexOfFirst false
            url.contains(host)
        }.takeIf { it >= 0 }
    }

    fun getGeckoSession(index: Int): GeckoSession =
        sessionPool.getOrPut(index) {
            GeckoSession().apply {
                progressDelegate = object : GeckoSession.ProgressDelegate {
                    override fun onPageStop(session: GeckoSession, success: Boolean) {
                        loadedTabs[index] = true
                    }
                }
                historyDelegate = object : GeckoSession.HistoryDelegate {
                    override fun onHistoryStateChange(session: GeckoSession, history: GeckoSession.HistoryDelegate.HistoryList) {
                        canGoBackMap[index] = history.currentIndex > 0
                    }
                }
                // v1.1 — تغيّر الرابط يعني محادثة جديدة (مثلاً /c/<id>
                // في ChatGPT): امسح نص المحادثة السابقة فوراً حتى لا
                // يدمجه القائد ظناً أنه الإجابة الحالية، واسمح بتحديث
                // تلقائي جديد للرابط الجديد. ملاحظة: onLocationChange
                // في GeckoView 141 ليست في ProgressDelegate بل في
                // NavigationDelegate، وبتوقيع من 4 وسائط (البناءان الأولان
                // لـ v1.1 فشلا عليها).
                navigationDelegate = object : GeckoSession.NavigationDelegate {
                    // التوقيع الحقيقي في GeckoView 141 (من رسالة المترجم):
                    // onLocationChange(session, url, perms, hasUserGesture)
                    override fun onLocationChange(
                        session: GeckoSession,
                        url: String?,
                        perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                        hasUserGesture: Boolean
                    ) {
                        if (url == null) return
                        if (pageUrls[index] != url) {
                            pageUrls[index] = url
                            pageTexts.remove(index)
                            autoReloaded.remove(index)
                        }
                    }
                }
                open(geckoRuntime)
                loadUri(services[index].url)
            }
        }

    fun ensureTab(index: Int) {
        getGeckoSession(index)
    }

    // v1.1 — تثبيت الإضافة مع إعادة محاولة: المندوب ثابت (remember)
    // فيستطيع زر «إعادة التثبيت» في المتصفح استدعاء التثبيت نفسه،
    // وحارس installingExtension يمنع تثبيتين متزامنين، وإن فشل
    // التثبيت الأول يعيد التطبيق المحاولة بعد 5 ثوانٍ. (في GeckoView
    // 141: setMessageDelegate طريقة على WebExtension نفسها بعد
    // التثبيت، وonMessage تعيد GeckoResult<Any>?.)
    val extensionDelegate = remember {
        object : WebExtension.MessageDelegate {
            override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
                val json = message as? JSONObject ?: return null
                if (json.optString("type") != "pageText") return null
                val idx = serviceIndexForUrl(json.optString("url")) ?: return null
                val text = json.optString("text")
                if (text.isNotBlank()) pageTexts[idx] = text
                return null
            }
        }
    }
    fun installExtension() {
        if (installingExtension || extensionReady) return
        installingExtension = true
        extensionAttempts += 1
        geckoRuntime.webExtensionController.installBuiltIn("resource://android/assets/webext/").accept(
            { extension ->
                extension?.setMessageDelegate(extensionDelegate, "com.abd.aihubgecko")
                extensionReady = true
                installingExtension = false
            },
            {
                extensionReady = false
                installingExtension = false
            }
        )
    }
    LaunchedEffect(Unit) {
        installExtension()
        delay(5000)
        if (!extensionReady) installExtension()
    }

    val scope = rememberCoroutineScope()
    val sending = remember { mutableStateListOf<Int>() }
    var sendingAll by remember { mutableStateOf(false) }

    // مفاتيح API (تُقرأ مرة عند فتح التطبيق، وتُحفظ من شاشة المفاتيح)
    var keyOpenAi by remember { mutableStateOf(getKey(context, "key_openai")) }
    var keyXai by remember { mutableStateOf(getKey(context, "key_xai")) }
    var keyGemini by remember { mutableStateOf(getKey(context, "key_gemini")) }
    var keyAnthropic by remember { mutableStateOf(getKey(context, "key_anthropic")) }
    var modelOpenAi by remember { mutableStateOf(getModel(context, "openai", defaultModel("openai"))) }
    var modelXai by remember { mutableStateOf(getModel(context, "xai", defaultModel("xai"))) }
    var modelGemini by remember { mutableStateOf(getModel(context, "gemini", defaultModel("gemini"))) }
    var modelAnthropic by remember { mutableStateOf(getModel(context, "anthropic", defaultModel("anthropic"))) }

    // v1.2 — نقطة نهاية مخصصة متوافقة مع OpenAI (Groq / OpenRouter / FreeLLMAPI)
    var keyCustom by remember { mutableStateOf(getKey(context, "key_custom")) }
    var modelCustom by remember { mutableStateOf(getModel(context, "custom", defaultModel("custom"))) }
    var urlCustom by remember { mutableStateOf(getKey(context, "url_custom")) }

    // حالة مقسّم المهام
    var mainTask by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf("بحث") }
    val subTasks = remember { mutableStateListOf<SubTask>() }
    var mergedReport by remember { mutableStateOf("") }

    // v1.5 — النموذج القائد: نموذج (بمفتاح API) يخطط التقسيم بنفسه
    // ثم يدمج نتائج باقي النماذج في تقرير واحد. بلا قائد يبقى
    // التقسيم بالقوالب الثابتة والدمج بلصق النصوص.
    var conductorIndex by remember { mutableStateOf<Int?>(null) }
    var conductorPlanning by remember { mutableStateOf(false) }
    var conductorMerging by remember { mutableStateOf(false) }
    var readingPages by remember { mutableStateOf(false) }

    // v1.2 — غرفة التحكم: سجل أحداث حي + زمن المهمة
    val missionLog = remember { mutableStateListOf<String>() }
    var missionStartMs by remember { mutableStateOf(0L) }
    var missionNowMs by remember { mutableStateOf(0L) }

    fun copyText(text: String, message: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("aihub", text))
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // v1.2 — غرفة التحكم: تسجيل حدث بساعة وقوعه في سجل المهمة.
    fun logEvent(text: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        missionLog.add(0, "$time — $text")
        if (missionLog.size > 40) missionLog.removeAt(missionLog.size - 1)
    }
    // v1.2 — غرفة التحكم: تحديث الزمن المنقضي كل ثانية أثناء الإرسال.
    LaunchedEffect(sendingAll) {
        while (sendingAll) {
            missionNowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    fun copyPrompt() {
        if (prompt.isBlank()) {
            Toast.makeText(context, "اكتب سؤالك أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        copyText(prompt, "تم نسخ السؤال — الصقه في صندوق المحادثة")
    }

    fun openService(index: Int, fromSplit: Boolean = false) {
        selected = index
        ensureTab(index)
        returnToSplit = fromSplit
        screen = "browser"
    }

    fun goBackFromBrowser() {
        screen = if (returnToSplit) "split" else "home"
        returnToSplit = false
    }

    fun buildReport(): String = buildString {
        append("التقرير النهائي\n")
        append("المهمة: $mainTask\n")
        append("النوع: $taskType\n\n")
        subTasks.forEach { task ->
            val model = services[task.modelIndex]
            append("## ${task.role} — ${model.name}\n")
            append(if (task.result.isBlank()) "(لا نتيجة بعد — لم تُرسل أو لم تُلصق)\n" else task.result + "\n")
            append("\n")
        }
    }

    fun conductorMergePrompt(): String = buildString {
        append("You are the lead coordinator of a team of AI models. Main task: $mainTask\nTask type: $taskType\n\n")
        append("Below are the answers from the specialist models. Write ONE final report in Arabic: merge them into a single coherent report, remove duplication, resolve contradictions (note any disagreement briefly), keep the strongest points, use clear headings. Do not invent new facts.\n\n")
        subTasks.forEach { task ->
            val model = services[task.modelIndex]
            append("### ${task.role} — ${model.name}\n")
            append(if (task.result.isBlank()) "(no result yet)\n" else task.result + "\n")
            append("\n")
        }
    }

    // v1.0-gecko — القائد يقرأ صفحات الويب: نصوص الصفحات المفتوحة
    // كما قرأتها الإضافة المدمجة (قراءة فقط) تُرسل للقائد فيدمجها.
    fun conductorPagesPrompt(): String = buildString {
        append("You are the lead coordinator of a team of AI models. Main task: $mainTask\nTask type: $taskType\n\n")
        append("Below is the live text of the web pages currently open in the app's browser, as read by the app's built-in page reader (read-only). Each section is what that model wrote on its own page. Write ONE final report in Arabic: merge them into a single coherent report, remove duplication, resolve contradictions (note any disagreement briefly), keep the strongest points, use clear headings. Do not invent new facts. Ignore pages with no text yet.\n\n")
        pageTexts.forEach { (idx, text) ->
            append("### ${services[idx].name} — page text\n")
            append(text + "\n\n")
        }
    }

    BackHandler(enabled = screen == "browser") {
        if (canGoBackMap[selected] == true) {
            sessionPool[selected]?.goBack()
        } else {
            goBackFromBrowser()
        }
    }

    BackHandler(enabled = screen == "split") {
        screen = "home"
    }

    BackHandler(enabled = screen == "keys") {
        screen = "home"
    }

    if (screen == "home") {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("AI Hub", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "مركز الذكاء الموحد — ستة مساعدات في تطبيق واحد",
                    fontSize = 15.sp,
                    color = Color(0xFF555555)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 قسّم مهمة على النماذج", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "اكتب مهمتك الرئيسية، وسيقسّمها التطبيق إلى 6 مهام فرعية — كل نموذج يأخذ مهمة منفصلة حسب قوته — ثم ترسلها من هنا وتجمع النتائج في تقرير واحد.",
                            fontSize = 13.sp,
                            color = Color(0xFF444444)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = mainTask,
                            onValueChange = { mainTask = it },
                            label = { Text("المهمة الرئيسية… مثال: ابحث عن مستقبل السيارات الكهربائية") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("بحث", "كتابة", "برمجة", "تحليل").forEach { type ->
                                OutlinedButton(
                                    onClick = { taskType = type },
                                    colors = if (taskType == type) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF7C4DFF), contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                                ) { Text(type, fontSize = 13.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("👑 النموذج القائد (اختياري):", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "القائد يخطط التقسيم بنفسه ثم يدمج النتائج — بدل القوالب الثابتة. يحتاج مفتاح API؛ وبدون قائد يبقى التقسيم المحلي كما هو.",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { conductorIndex = null },
                                colors = if (conductorIndex == null) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF7C4DFF), contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                            ) { Text("بلا قائد", fontSize = 13.sp) }
                            listOf(0 to "ChatGPT", 1 to "Grok").forEach { (idx, name) ->
                                val p = providerFor(idx)
                                val hasKey = p != null && getKey(context, "key_$p").isNotBlank()
                                OutlinedButton(
                                    onClick = { conductorIndex = idx },
                                    colors = if (conductorIndex == idx) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF6A1B9A), contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                                ) { Text(name + if (hasKey) "" else " 🔒", fontSize = 13.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2 to "Gemini", 3 to "Claude").forEach { (idx, name) ->
                                val p = providerFor(idx)
                                val hasKey = p != null && getKey(context, "key_$p").isNotBlank()
                                OutlinedButton(
                                    onClick = { conductorIndex = idx },
                                    colors = if (conductorIndex == idx) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF6A1B9A), contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                                ) { Text(name + if (hasKey) "" else " 🔒", fontSize = 13.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val hasCustomKey = getKey(context, "key_custom").isNotBlank() && getKey(context, "url_custom").isNotBlank()
                            OutlinedButton(
                                onClick = { conductorIndex = 10 },
                                colors = if (conductorIndex == 10) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF6A1B9A), contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                            ) { Text("🔧 مخصص" + if (hasCustomKey) "" else " 🔒", fontSize = 13.sp) }
                            Text("قائد من نقطة نهاية متوافقة مع OpenAI (مثل Groq المجاني) — تُضبط من شاشة المفاتيح.", fontSize = 12.sp, color = Color(0xFF666666), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (mainTask.isBlank()) {
                                    Toast.makeText(context, "اكتب المهمة الرئيسية أولاً", Toast.LENGTH_SHORT).show()
                                } else {
                                    val cIdx = conductorIndex
                                    if (cIdx == null) {
                                        subTasks.clear()
                                        subTasks.addAll(splitTask(mainTask, taskType))
                                        logEvent("تم التقسيم المحلي إلى 6 مهام فرعية")
                                        mergedReport = ""
                                        screen = "split"
                                    } else {
                                        val cProvider = providerFor(cIdx)
                                        val cKey = if (cProvider != null) getKey(context, "key_$cProvider") else ""
                                        if (cProvider == null || cKey.isBlank()) {
                                            Toast.makeText(context, "أضف مفتاح ${if (cIdx == 10) "المخصص" else services[cIdx].name} أولاً من شاشة المفاتيح", Toast.LENGTH_LONG).show()
                                        } else {
                                            conductorPlanning = true
                                            scope.launch {
                                                val plan = callConductorApi(context, cProvider, conductorPlanPrompt(mainTask, taskType))
                                                conductorPlanning = false
                                                val parsed = parseConductorPlan(plan, mainTask)
                                                subTasks.clear()
                                                if (parsed.size < 2) {
                                                    subTasks.addAll(splitTask(mainTask, taskType))
                                                    logEvent("تعذّر فهم خطة القائد — استُخدم التقسيم المحلي")
                                                    Toast.makeText(context, "تعذّر فهم خطة القائد — استُخدم التقسيم المحلي", Toast.LENGTH_LONG).show()
                                                } else {
                                                    subTasks.addAll(parsed)
                                                    logEvent("القائد قسّم المهمة إلى ${parsed.size} مهام فرعية")
                                                }
                                                mergedReport = ""
                                                screen = "split"
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !conductorPlanning
                        ) { Text(if (conductorPlanning) "⏳ القائد يخطط التقسيم…" else "✂️ قسّم المهمة على النماذج") }
                        if (subTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { screen = "split" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("↩ العودة إلى التقسيم الحالي (${subTasks.count { it.result.isNotBlank() }}/6 اكتمل)") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("أو اكتب سؤالاً سريعاً مرة واحدة هنا…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "انسخ سؤالك ثم افتح أي مساعد والصقه — هكذا تسألهم كلهم بنفس السؤال دون إعادة كتابته.",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { copyPrompt() }, modifier = Modifier.fillMaxWidth()) {
                    Text("📋 نسخ السؤال")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { screen = "keys" }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔑 مفاتيح API — للإرسال التلقائي الشرعي (بدل الحقن)")
                }
                Text(
                    "الإرسال التلقائي يعمل فقط عبر API الرسمي بمفتاحك أنت. بلا مفتاح، يبقى الإرسال اليدوي (نسخ ولصق) كما في v1.1.",
                    fontSize = 12.sp,
                    color = Color(0xFF777777)
                )
            }
            items(services) { service ->
                val index = services.indexOf(service)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp)
                                    .background(service.color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(service.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(service.company, fontSize = 13.sp, color = Color(0xFF666666))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(service.desc, fontSize = 14.sp)
                        Text("✓ ${service.strength}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(service.limit, fontSize = 13.sp, color = Color(0xFF666666))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            Button(
                                onClick = { openService(index) },
                                colors = ButtonDefaults.buttonColors(containerColor = service.color)
                            ) { Text("فتح ${service.name}") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    copyPrompt()
                                    openService(index)
                                }
                            ) { Text("إرسال السؤال إليه") }
                        }
                    }
                }
            }
            item {
                Text(
                    "ملاحظة صادقة: هذا التطبيق لا يملك هذه الخدمات ولا ينسخها — هو مركز أصلي يفتح مواقعها الرسمية داخل التطبيق، وتسجّل الدخول أنت بحسابك في كل موقع. Antigravity تحديداً بلا تطبيق موبايل أصلاً: هو برنامج سطح مكتب (ويندوز/ماك/لينكس)، وفي هذا التطبيق تفتح موقعه الرسمي وتدير جلسة سطح المكتب عن بعد من متصفح هاتفك.",
                    fontSize = 12.sp,
                    color = Color(0xFF777777)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else if (screen == "split") {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { screen = "home" }) { Text("◀ الرئيسية") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎯 تقسيم المهمة", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("المهمة: $mainTask", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("النوع: $taskType — اكتمل ${subTasks.count { it.result.isNotBlank() }} من 6", fontSize = 13.sp, color = Color(0xFF666666))
                val cIdxHeader = conductorIndex
                if (cIdxHeader != null) {
                    Text("👑 القائد: ${if (cIdxHeader == 10) "🔧 مخصص" else services[cIdxHeader].name} — يخطط التقسيم ويدمج النتائج", fontSize = 13.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
                }
                Text(
                    "الخطوات: 1) اضغط «إرسال إلى النموذج» فيُنسخ سؤاله المجهّز ويُفتح تبويبه 2) الصق السؤال في صندوق المحادثة 3) انسخ إجابة النموذج 4) ارجع هنا واضغط «لصق النتيجة» في بطاقته 5) بعد اكتمال الكل اضغط «دمج النتائج». أو اضغط «إرسال تلقائي للكل» في الأسفل فيرسل التطبيق المهام دفعة واحدة عبر API الرسمي (لمن أضفت مفتاحه) ويدمج النتائج تلقائياً.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            }
            // v1.2 — غرفة التحكم (Mission Control): تقدم حي لكل الوكلاء
            // الستة + سجل أحداث بالساعات — كما في لوحة تحكم الفيديو.
            item {
                val doneCount = subTasks.count { it.result.isNotBlank() }
                val workingCount = subTasks.count { it.startedAt > 0L && it.doneAt == 0L }
                val failedCount = subTasks.count { it.status.startsWith("فشل") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A4A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎛️ غرفة التحكم — Mission Control", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✅ اكتمل $doneCount/6  •  ⚡ يعمل الآن $workingCount  •  ❌ فشل $failedCount  •  ⏳ بانتظار ${6 - doneCount - workingCount - failedCount}", fontSize = 13.sp, color = Color(0xFFBBDEFB))
                        if (missionStartMs > 0L) {
                            val elapsedSec = ((if (sendingAll) missionNowMs else System.currentTimeMillis()) - missionStartMs) / 1000
                            Text("⏱ الزمن المنقضي: ${elapsedSec / 60} د ${elapsedSec % 60} ث", fontSize = 13.sp, color = Color(0xFFBBDEFB))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF37474F), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(doneCount / 6f).height(8.dp).background(Color(0xFF66BB6A), RoundedCornerShape(4.dp)))
                        }
                        if (missionLog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("📜 سجل الأحداث:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            missionLog.take(6).forEach { line ->
                                Text(line, fontSize = 12.sp, color = Color(0xFFE3F2FD))
                            }
                        }
                    }
                }
            }
            itemsIndexed(subTasks) { index, task ->
                val model = services[task.modelIndex]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (task.result.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(model.color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(model.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.role, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("النموذج: ${model.name} — ${task.status}", fontSize = 13.sp, color = Color(0xFF666666))
                                if (task.startedAt > 0L && task.doneAt > task.startedAt) {
                                    Text("⏱ استغرق ${(task.doneAt - task.startedAt) / 1000} ث", fontSize = 12.sp, color = Color(0xFF666666))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(task.instruction, fontSize = 13.sp, color = Color(0xFF555555))
                        if (task.result.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                task.result.take(300) + if (task.result.length > 300) "…" else "",
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            Button(
                                onClick = {
                                    copyText(task.prompt, "تم نسخ سؤال «${task.role}» — الصقه في ${model.name}")
                                    subTasks[index] = task.copy(status = "أُرسل — بانتظار النتيجة")
                                    openService(task.modelIndex, fromSplit = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = model.color),
                                modifier = Modifier.weight(1f)
                            ) { Text("📤 إرسال إلى ${model.name}", fontSize = 13.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                    if (text.isBlank()) {
                                        Toast.makeText(context, "الحافظة فارغة — انسخ إجابة النموذج أولاً", Toast.LENGTH_SHORT).show()
                                    } else {
                                        subTasks[index] = task.copy(result = text, status = "اكتمل ✓", doneAt = System.currentTimeMillis())
                                        logEvent("لُصقت نتيجة «${task.role}» يدوياً")
                                        Toast.makeText(context, "تم لصق نتيجة «${task.role}»", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("📥 لصق النتيجة", fontSize = 13.sp) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val provider = providerFor(task.modelIndex)
                        val hasKey = provider != null && getKey(context, "key_$provider").isNotBlank()
                        if (provider == null) {
                            Text("⚡ ${model.name} بلا API محادثة رسمي — الإرسال يدوي فقط.", fontSize = 12.sp, color = Color(0xFF777777))
                        } else if (!hasKey) {
                            OutlinedButton(
                                onClick = { screen = "keys" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🔑 أضف مفتاح ${model.name} لتفعيل الإرسال التلقائي", fontSize = 13.sp) }
                        } else if (index in sending) {
                            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("⏳ جارٍ إرسال «${task.role}» إلى ${model.name}…", fontSize = 13.sp) }
                        } else {
                            Button(
                                onClick = {
                                    sending.add(index)
                                    subTasks[index] = task.copy(status = "يُرسل تلقائياً عبر API…", startedAt = System.currentTimeMillis(), doneAt = 0L)
                                    logEvent("أُرسلت «${task.role}» إلى ${model.name}")
                                    scope.launch {
                                        val key = getKey(context, "key_$provider")
                                        val modelName = getModel(context, provider, defaultModel(provider))
                                        val answer = callApi(provider, key, modelName, task.prompt)
                                        sending.remove(index)
                                        val current = subTasks[index]
                                        if (answer.startsWith("خطأ")) {
                                            subTasks[index] = current.copy(status = "فشل الإرسال التلقائي — جرّب اليدوي", doneAt = System.currentTimeMillis())
                                            logEvent("فشل إرسال «${task.role}» — ${answer.take(80)}")
                                            Toast.makeText(context, answer.take(200), Toast.LENGTH_LONG).show()
                                        } else {
                                            subTasks[index] = current.copy(result = answer, status = "اكتمل ✓ (تلقائي عبر API)", doneAt = System.currentTimeMillis())
                                            logEvent("اكتملت «${task.role}» من ${model.name}")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) { Text("⚡ إرسال تلقائي إلى ${model.name} (API رسمي)", fontSize = 13.sp) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = { copyText(task.prompt, "تم نسخ سؤال «${task.role}»") },
                                modifier = Modifier.weight(1f)
                            ) { Text("نسخ السؤال فقط", fontSize = 13.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (task.result.isNotBlank()) {
                                        subTasks[index] = task.copy(result = "", status = "لم يُرسل")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("مسح النتيجة", fontSize = 13.sp) }
                        }
                    }
                }
            }
            item {
                val keyedCount = subTasks.count { task ->
                    val p = providerFor(task.modelIndex)
                    p != null && task.result.isBlank() && getKey(context, "key_$p").isNotBlank()
                }
                if (sendingAll) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("⏳ جارٍ الإرسال التلقائي إلى كل النماذج…", fontSize = 14.sp)
                    }
                } else if (keyedCount > 0) {
                    Button(
                        onClick = {
                            sendingAll = true
                            missionStartMs = System.currentTimeMillis()
                            missionNowMs = missionStartMs
                            logEvent("بدأ الإرسال التلقائي للكل ($keyedCount نماذج بمفاتيح)")
                            scope.launch {
                                val jobs = subTasks.mapIndexedNotNull { index, task ->
                                    val provider = providerFor(task.modelIndex)
                                    if (provider == null || task.result.isNotBlank()) null
                                    else {
                                        val key = getKey(context, "key_$provider")
                                        if (key.isBlank()) null else {
                                            sending.add(index)
                                            subTasks[index] = task.copy(status = "يُرسل تلقائياً عبر API…", startedAt = System.currentTimeMillis(), doneAt = 0L)
                                            async {
                                                val modelName = getModel(context, provider, defaultModel(provider))
                                                val answer = callApi(provider, key, modelName, task.prompt)
                                                index to answer
                                            }
                                        }
                                    }
                                }
                                val results = jobs.awaitAll()
                                results.forEach { (index, answer) ->
                                    sending.remove(index)
                                    val current = subTasks[index]
                                    if (answer.startsWith("خطأ")) {
                                        subTasks[index] = current.copy(status = "فشل الإرسال التلقائي — جرّب اليدوي", doneAt = System.currentTimeMillis())
                                        logEvent("فشل إرسال «${current.role}»")
                                    } else {
                                        subTasks[index] = current.copy(result = answer, status = "اكتمل ✓ (تلقائي عبر API)", doneAt = System.currentTimeMillis())
                                        logEvent("اكتملت «${current.role}»")
                                    }
                                }
                                logEvent("انتهى الإرسال للكل: ${subTasks.count { it.result.isNotBlank() }}/6 بنتائج")
                                sendingAll = false
                                val cIdxDone = conductorIndex
                                val cProviderDone = if (cIdxDone != null) providerFor(cIdxDone) else null
                                val cKeyDone = if (cProviderDone != null) getKey(context, "key_$cProviderDone") else ""
                                if (cIdxDone != null && cProviderDone != null && cKeyDone.isNotBlank() && subTasks.any { it.result.isNotBlank() }) {
                                    mergedReport = "⏳ القائد (${if (cIdxDone == 10) "🔧 مخصص" else services[cIdxDone].name}) يدمج النتائج…"
                                    val merged = callConductorApi(context, cProviderDone, conductorMergePrompt())
                                    mergedReport = if (merged.startsWith("خطأ")) buildReport() else merged
                                    logEvent("القائد دمج نتائج الكل في التقرير")
                                    Toast.makeText(context, "اكتمل الإرسال التلقائي — والقائد دمج النتائج في التقرير", Toast.LENGTH_LONG).show()
                                } else {
                                    mergedReport = buildReport()
                                    Toast.makeText(context, "اكتمل الإرسال التلقائي — تم دمج النتائج في التقرير", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("⚡ إرسال تلقائي للكل ($keyedCount نماذج بمفاتيح)", fontSize = 14.sp) }
                    Text(
                        "يرسل فقط المهام التي بلا نتيجة بعد، إلى النماذج التي أضفت مفاتيحها — بالتوازي عبر API الرسمي — ثم يدمج النتائج تلقائياً في التقرير أدناه.",
                        fontSize = 12.sp,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        "⚡ الإرسال التلقائي للكل يحتاج مفاتيح API — أضف مفتاحاً واحداً على الأقل من شاشة المفاتيح (🔑) ليظهر الزر هنا.",
                        fontSize = 13.sp,
                        color = Color(0xFF777777)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val cIdxMerge = conductorIndex
                if (cIdxMerge != null) {
                    val cProviderMerge = providerFor(cIdxMerge)
                    val cHasKeyMerge = cProviderMerge != null && getKey(context, "key_$cProviderMerge").isNotBlank()
                    val anyResultMerge = subTasks.any { it.result.isNotBlank() }
                    if (conductorMerging) {
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("⏳ القائد (${if (cIdxMerge == 10) "🔧 مخصص" else services[cIdxMerge].name}) يدمج النتائج…", fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                val p = cProviderMerge
                                if (p == null || !cHasKeyMerge) {
                                    Toast.makeText(context, "أضف مفتاح ${if (cIdxMerge == 10) "المخصص" else services[cIdxMerge].name} أولاً من شاشة المفاتيح", Toast.LENGTH_LONG).show()
                                } else if (!anyResultMerge) {
                                    Toast.makeText(context, "لا نتائج بعد — أرسل المهام أولاً", Toast.LENGTH_SHORT).show()
                                } else {
                                    conductorMerging = true
                                    scope.launch {
                                        val answer = callConductorApi(context, p, conductorMergePrompt())
                                        conductorMerging = false
                                        if (answer.startsWith("خطأ")) {
                                            mergedReport = buildReport()
                                            logEvent("فشل الدمج الذكي — استُخدم الدمج العادي")
                                            Toast.makeText(context, "فشل الدمج الذكي — استُخدم الدمج العادي", Toast.LENGTH_LONG).show()
                                        } else {
                                            mergedReport = answer
                                            logEvent("القائد دمج النتائج في تقرير واحد")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                        ) { Text("🧠 دمج ذكي بالقائد (${if (cIdxMerge == 10) "🔧 مخصص" else services[cIdxMerge].name})", fontSize = 14.sp) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // v1.0-gecko — القائد يقرأ صفحات الويب: نصوص الصفحات
                // المفتوحة كما قرأتها الإضافة المدمجة تُرسل للقائد
                // فيدمجها في تقرير واحد (قراءة فقط — بلا كتابة في الصفحات).
                val readCount = pageTexts.count { it.value.isNotBlank() }
                val cIdxRead = conductorIndex
                val cProviderRead = if (cIdxRead != null) providerFor(cIdxRead) else null
                // v1.1 — القائد بلا API (Meta AI أو Antigravity) لا يظهر
                // له زر قراءة الصفحات أصلاً، بل سطر يشرح أنه يحتاج
                // قائداً من النماذج الأربعة ذات الـ API الرسمي.
                if (cIdxRead != null && cProviderRead == null) {
                    Text(
                        "👁️ قراءة الصفحات تحتاج قائداً بـ API رسمي — القائد الحالي (${services[cIdxRead].name}) بلا API، فاختر قائداً من ChatGPT أو Grok أو Gemini أو Claude.",
                        fontSize = 13.sp,
                        color = Color(0xFF777777)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (cIdxRead != null && cProviderRead != null) {
                    val cHasKeyRead = getKey(context, "key_$cProviderRead").isNotBlank()
                    Button(
                        onClick = {
                            val p = cProviderRead
                            if (!cHasKeyRead) {
                                Toast.makeText(context, "أضف مفتاح ${if (cIdxRead == 10) "المخصص" else services[cIdxRead].name} أولاً من شاشة المفاتيح", Toast.LENGTH_LONG).show()
                            } else if (readCount == 0) {
                                Toast.makeText(context, "لا نصوص بعد — افتح تبويبات النماذج في المتصفح أولاً لتقرأها الإضافة", Toast.LENGTH_LONG).show()
                            } else {
                                readingPages = true
                                logEvent("القائد يقرأ $readCount صفحة من المتصفح…")
                                scope.launch {
                                    val answer = callConductorApi(context, p, conductorPagesPrompt())
                                    readingPages = false
                                    if (answer.startsWith("خطأ")) {
                                        logEvent("تعذّر على القائد قراءة الصفحات")
                                        Toast.makeText(context, "تعذّر على القائد قراءة الصفحات — جرّب الدمج العادي", Toast.LENGTH_LONG).show()
                                    } else {
                                        mergedReport = answer
                                        logEvent("القائد قرأ $readCount صفحة ودمجها في التقرير")
                                        Toast.makeText(context, "القائد قرأ $readCount صفحة ودمجها في التقرير", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !readingPages,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text(if (readingPages) "⏳ القائد يقرأ الصفحات…" else "👁️ القائد يقرأ الصفحات ويدمجها ($readCount/6)", fontSize = 14.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // v1.1 — مسح كل النصوص المقروءة دفعة واحدة (يمسح أيضاً
                // علامات التحديث التلقائي، فيُعاد التحديث عند فتح التبويب).
                if (readCount > 0) {
                    OutlinedButton(
                        onClick = { pageTexts.clear(); autoReloaded.clear() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🗑 مسح كل النصوص المقروءة ($readCount)", fontSize = 13.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        mergedReport = buildReport()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📄 دمج النتائج في تقرير واحد") }
                if (mergedReport.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(mergedReport, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { copyText(mergedReport, "تم نسخ التقرير الكامل") },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📋 نسخ التقرير الكامل") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else if (screen == "keys") {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { screen = "home" }) { Text("◀ الرئيسية") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔑 مفاتيح API", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "هذه هي الطريقة الشرعية الوحيدة للأتمتة الكاملة: مفتاحك الرسمي لكل شركة، يُحفظ في التطبيق فقط على هاتفك، ولا يُرسل إلا إلى API تلك الشركة نفسها. لا تلصق أي مفتاح في المحادثة أبداً — أدخله هنا فقط.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
                Text(
                    "📊 الحصص (تقريبية — راجع موقع كل شركة): Gemini له فئة مجانية من Google AI Studio. OpenAI وAnthropic وxAI مدفوعة وتُحاسَب بالاستخدام — راقب رصيدك في مواقعها. وإن تغيّر اسم نموذج مع الوقت، عدّل حقل «اسم النموذج» هنا.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
                Text(
                    "Meta AI وAntigravity بلا API محادثة رسمي، لذلك لا حقل لهما هنا ويبقى إرسالهما يدوياً.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            }
            item { KeyField("OpenAI — ChatGPT", "platform.openai.com/api-keys", keyOpenAi, { keyOpenAi = it }, modelOpenAi, { modelOpenAi = it }) }
            item { KeyField("xAI — Grok", "console.x.ai", keyXai, { keyXai = it }, modelXai, { modelXai = it }) }
            item { KeyField("Google — Gemini", "aistudio.google.com/apikey", keyGemini, { keyGemini = it }, modelGemini, { modelGemini = it }) }
            item { KeyField("Anthropic — Claude", "console.anthropic.com", keyAnthropic, { keyAnthropic = it }, modelAnthropic, { modelAnthropic = it }) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔧 نقطة نهاية مخصصة (متوافقة مع OpenAI)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("خادم يفهم صيغة OpenAI — مثل Groq (فئة مجانية: نحو 200 ألف توكن/يوم)، أو OpenRouter، أو خادم FreeLLMAPI إن شغّلته على جهاز آخر. تُستخدم كقائد إضافي من الرئيسية.", fontSize = 12.sp, color = Color(0xFF666666))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = urlCustom, onValueChange = { urlCustom = it },
                            label = { Text("رابط الخادم (مثال: https://api.groq.com)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = keyCustom, onValueChange = { keyCustom = it },
                            label = { Text("المفتاح (API Key)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = modelCustom, onValueChange = { modelCustom = it },
                            label = { Text("اسم النموذج (مثال: llama-3.3-70b-versatile)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        prefs(context).edit()
                            .putString("key_openai", keyOpenAi.trim()).putString("model_openai", modelOpenAi.trim())
                            .putString("key_xai", keyXai.trim()).putString("model_xai", modelXai.trim())
                            .putString("key_gemini", keyGemini.trim()).putString("model_gemini", modelGemini.trim())
                            .putString("key_anthropic", keyAnthropic.trim()).putString("model_anthropic", modelAnthropic.trim())
                            .putString("key_custom", keyCustom.trim()).putString("model_custom", modelCustom.trim()).putString("url_custom", urlCustom.trim())
                            .apply()
                        Toast.makeText(context, "تم حفظ المفاتيح في التطبيق فقط", Toast.LENGTH_SHORT).show()
                        screen = "home"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("💾 حفظ المفاتيح") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        keyOpenAi = ""; keyXai = ""; keyGemini = ""; keyAnthropic = ""; keyCustom = ""; modelCustom = ""; urlCustom = ""
                        prefs(context).edit().clear().apply()
                        Toast.makeText(context, "تم مسح كل المفاتيح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑 مسح كل المفاتيح") }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        val service = services[selected]
        LaunchedEffect(selected) { ensureTab(selected) }
        // v1.1 — إن حمّلت الصفحة ولم تقرأها الإضافة خلال 6 ثوانٍ،
        // حدّثها مرة واحدة تلقائياً (فالتثبيت قد يكتمل بعد أول تحميل).
        // مرة واحدة لكل رابط: يُصفَّر العداد عند تغيّر الرابط.
        LaunchedEffect(selected, extensionReady, loadedTabs[selected]) {
            if (!extensionReady) return@LaunchedEffect
            if (loadedTabs[selected] != true) return@LaunchedEffect
            if (pageTexts[selected]?.isNotBlank() == true) return@LaunchedEffect
            if (autoReloaded[selected] == true) return@LaunchedEffect
            delay(6000)
            if (pageTexts[selected]?.isNotBlank() == true) return@LaunchedEffect
            if (autoReloaded[selected] == true) return@LaunchedEffect
            autoReloaded[selected] = true
            sessionPool[selected]?.reload()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selected, edgePadding = 8.dp) {
                services.forEachIndexed { index, item ->
                    Tab(
                        selected = selected == index,
                        onClick = { ensureTab(index); selected = index },
                        text = { Text(item.name + if (loadedTabs[index] == true) " ✓" else "", fontSize = 13.sp) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🦎 GeckoView — الجلسات الست تبقى محمّلة دائماً بلا إعادة تحميل. الإضافة المدمجة " +
                        (if (extensionReady) "تعمل ✓ وتقرأ نص الصفحة الظاهرة للقائد." else "لم تُثبَّت بعد — اضغط زر إعادة التثبيت هنا."),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!extensionReady) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { installExtension() }) { Text("↻ إعادة التثبيت", fontSize = 12.sp) }
                }
            }
            val currentPageText = pageTexts[selected]
            if (currentPageText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "👁️ الإضافة قرأت نص صفحة ${service.name} (${currentPageText.length} حرف) — جاهز للقائد.",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { pageTexts.remove(selected); autoReloaded.remove(selected) }) { Text("🗑 مسح", fontSize = 12.sp) }
                }
            }
            if (service.id == "antigravity") {
                Text(
                    "تنبيه: Antigravity برنامج سطح مكتب فقط — بلا تطبيق أندرويد. من هنا تفتح موقعه الرسمي وتدير جلسة جهازك عن بعد.",
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(10.dp),
                    fontSize = 13.sp
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx -> GeckoView(ctx).also { geckoViewRef = it } },
                    update = { view ->
                        val session = getGeckoSession(selected)
                        if (view.session != session) {
                            view.releaseSession()
                            view.setSession(session)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { sessionPool[selected]?.goBack() }, modifier = Modifier.weight(1f)) { Text("◀ رجوع") }
                OutlinedButton(onClick = { sessionPool[selected]?.goForward() }, modifier = Modifier.weight(1f)) { Text("تقدم ▶") }
                OutlinedButton(onClick = { sessionPool[selected]?.reload() }, modifier = Modifier.weight(1f)) { Text("↻ تحديث") }
                OutlinedButton(onClick = { goBackFromBrowser() }, modifier = Modifier.weight(1f)) { Text(if (returnToSplit) "↩ التقسيم" else "🏠 الرئيسية") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { copyPrompt() }, modifier = Modifier.weight(1f)) { Text("📋 نسخ سؤالي") }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(services[selected].url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("🌐 فتح في المتصفح") }
            }
        }
    }
}

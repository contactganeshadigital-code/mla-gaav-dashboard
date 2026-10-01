package com.ganeshadigital.mlagaav

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ISSUE_STATUS = listOf("प्रलंबित", "प्रगतीत", "सोडवले")
val WORK_STATUS = listOf("प्रस्तावित", "सुरू", "पूर्ण")
val PRIORITY = listOf("कमी", "मध्यम", "जास्त")
val CATEGORIES = listOf("पाणी", "रस्ता", "वीज", "आरोग्य", "शिक्षण", "घरकुल", "रोजगार", "इतर")

data class Village(
    val id: Long, val name: String, val taluka: String,
    val population: Int, val households: Int,
    val sarpanch: String, val contact: String, val notes: String
)

data class Issue(
    val id: Long, val villageId: Long, val title: String, val category: String,
    val priority: Int, val status: Int, val date: String
)

data class Work(
    val id: Long, val villageId: Long, val title: String,
    val budgetLakh: Double, val status: Int, val date: String
)

fun today(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
fun newId(): Long = System.currentTimeMillis() * 1000 + (0..999).random()

class AppState(context: Context) {
    // Encrypted storage (AES-256, key in Android Keystore)
    private val prefs = EncryptedSharedPreferences.create(
        context, "mla_gaav_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---- PIN security ----
    private fun hash(pin: String, salt: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return Base64.encodeToString(d, Base64.NO_WRAP)
    }

    fun hasPin() = prefs.getString("pin_hash", null) != null

    fun setPin(pin: String) {
        val salt = Base64.encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
        prefs.edit().putString("pin_salt", salt).putString("pin_hash", hash(pin, salt))
            .putInt("fails", 0).putLong("lock_until", 0).apply()
    }

    /** seconds the user must still wait (0 = can try) */
    fun waitSeconds(): Int {
        val ms = prefs.getLong("lock_until", 0) - System.currentTimeMillis()
        return if (ms > 0) (ms / 1000).toInt() + 1 else 0
    }

    fun checkPin(pin: String): Boolean {
        if (waitSeconds() > 0) return false
        val ok = hash(pin, prefs.getString("pin_salt", "") ?: "") == prefs.getString("pin_hash", "")
        if (ok) {
            prefs.edit().putInt("fails", 0).putLong("lock_until", 0).apply()
        } else {
            val f = prefs.getInt("fails", 0) + 1
            val e = prefs.edit().putInt("fails", f)
            // 5 wrong tries -> wait grows: 30s, 60s, 120s ... (max 15 min)
            if (f >= 5) e.putLong("lock_until", System.currentTimeMillis() + minOf(900L, 30L shl (f - 5).coerceAtMost(5)) * 1000)
            e.apply()
        }
        return ok
    }

    val villages = mutableStateListOf<Village>()
    val issues = mutableStateListOf<Issue>()
    val works = mutableStateListOf<Work>()

    init {
        prefs.getString("data", null)?.let { fromJson(it) }
    }

    fun villageName(id: Long) = villages.find { it.id == id }?.name ?: "-"

    fun save() {
        prefs.edit().putString("data", toJson()).apply()
    }

    // ---- Village ----
    fun upsertVillage(v: Village) {
        val i = villages.indexOfFirst { it.id == v.id }
        if (i >= 0) villages[i] = v else villages.add(v)
        save()
    }

    fun deleteVillage(id: Long) {
        villages.removeAll { it.id == id }
        issues.removeAll { it.villageId == id }
        works.removeAll { it.villageId == id }
        save()
    }

    // ---- Issue ----
    fun addIssue(i: Issue) { issues.add(0, i); save() }
    fun cycleIssue(id: Long) {
        val i = issues.indexOfFirst { it.id == id }
        if (i >= 0) { issues[i] = issues[i].copy(status = (issues[i].status + 1) % 3); save() }
    }
    fun deleteIssue(id: Long) { issues.removeAll { it.id == id }; save() }

    // ---- Work ----
    fun addWork(w: Work) { works.add(0, w); save() }
    fun cycleWork(id: Long) {
        val i = works.indexOfFirst { it.id == id }
        if (i >= 0) { works[i] = works[i].copy(status = (works[i].status + 1) % 3); save() }
    }
    fun deleteWork(id: Long) { works.removeAll { it.id == id }; save() }

    // ---- Backup / Restore ----
    fun toJson(): String {
        val o = JSONObject()
        o.put("villages", JSONArray().apply {
            villages.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("taluka", it.taluka)
                    .put("population", it.population).put("households", it.households)
                    .put("sarpanch", it.sarpanch).put("contact", it.contact).put("notes", it.notes))
            }
        })
        o.put("issues", JSONArray().apply {
            issues.forEach {
                put(JSONObject().put("id", it.id).put("villageId", it.villageId).put("title", it.title)
                    .put("category", it.category).put("priority", it.priority)
                    .put("status", it.status).put("date", it.date))
            }
        })
        o.put("works", JSONArray().apply {
            works.forEach {
                put(JSONObject().put("id", it.id).put("villageId", it.villageId).put("title", it.title)
                    .put("budgetLakh", it.budgetLakh).put("status", it.status).put("date", it.date))
            }
        })
        return o.toString()
    }

    fun fromJson(s: String): Boolean = try {
        val o = JSONObject(s)
        val v = ArrayList<Village>(); val i = ArrayList<Issue>(); val w = ArrayList<Work>()
        o.getJSONArray("villages").let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                v.add(Village(it.getLong("id"), it.getString("name"), it.optString("taluka"),
                    it.optInt("population"), it.optInt("households"),
                    it.optString("sarpanch"), it.optString("contact"), it.optString("notes")))
            }
        }
        o.getJSONArray("issues").let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                i.add(Issue(it.getLong("id"), it.getLong("villageId"), it.getString("title"),
                    it.optString("category"), it.optInt("priority"), it.optInt("status"), it.optString("date")))
            }
        }
        o.getJSONArray("works").let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                w.add(Work(it.getLong("id"), it.getLong("villageId"), it.getString("title"),
                    it.optDouble("budgetLakh"), it.optInt("status"), it.optString("date")))
            }
        }
        villages.clear(); villages.addAll(v)
        issues.clear(); issues.addAll(i)
        works.clear(); works.addAll(w)
        save()
        true
    } catch (e: Exception) { false }

    // ---- Report (WhatsApp share) ----
    fun report(): String {
        val sb = StringBuilder()
        sb.appendLine("📊 *गाव अहवाल* – ${today()}")
        sb.appendLine("गावे: ${villages.size} | लोकसंख्या: ${villages.sumOf { it.population }} | कुटुंबे: ${villages.sumOf { it.households }}")
        sb.appendLine()
        sb.appendLine("⚠️ समस्या: प्रलंबित ${issues.count { it.status == 0 }}, प्रगतीत ${issues.count { it.status == 1 }}, सोडवल्या ${issues.count { it.status == 2 }}")
        sb.appendLine("🏗 कामे: प्रस्तावित ${works.count { it.status == 0 }}, सुरू ${works.count { it.status == 1 }}, पूर्ण ${works.count { it.status == 2 }}")
        sb.appendLine("💰 एकूण निधी: ₹ ${"%.2f".format(works.sumOf { it.budgetLakh })} लाख")
        sb.appendLine()
        villages.forEach { v ->
            val p = issues.count { it.villageId == v.id && it.status != 2 }
            val wk = works.count { it.villageId == v.id }
            sb.appendLine("• ${v.name}: प्रलंबित समस्या $p, कामे $wk")
        }
        return sb.toString()
    }
}

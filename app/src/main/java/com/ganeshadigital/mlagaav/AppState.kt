package com.ganeshadigital.mlagaav

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
    val id: Long, val state: String, val district: String, val taluka: String,
    val gp: String, val name: String, val pincode: String,
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

data class Info(
    val male: Int = 0, val female: Int = 0, val literacy: Double = 0.0,
    val eduPrimary: Double = 0.0, val eduSecondary: Double = 0.0, val eduHigher: Double = 0.0,
    val houses: Int = 0, val water: Int = 0, val elec: Int = 0,
    val schools: Int = 0, val anganwadi: Int = 0, val health: Int = 0,
    val areaHa: Double = 0.0, val agriHa: Double = 0.0, val forestHa: Double = 0.0,
    val waterBodies: Int = 0, val irrigation: Int = 0, val crops: String = "", val updated: String = "",
    val lat: Double = 0.0, val lon: Double = 0.0
)

data class VContact(val id: Long, val villageId: Long, val label: String, val name: String, val phone: String)

data class Member(val id: Long, val name: String, val relation: String, val gender: Int, val age: Int, val occupation: String)

data class Family(
    val id: Long, val villageId: Long, val head: String, val mobile: String,
    val address: String, val notes: String, val members: List<Member>
)

data class GpContact(
    val key: String, val sarpanch: String, val sarpanchPhone: String,
    val gramsevak: String, val gramsevakPhone: String
)

fun gpKey(st: String, di: String, ta: String, gp: String) =
    listOf(st, di, ta, gp).joinToString("|") { it.trim().lowercase() }

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
    val infos = mutableStateMapOf<Long, Info>()
    val families = mutableStateListOf<Family>()
    val contacts = mutableStateListOf<VContact>()
    val gps = mutableStateMapOf<String, GpContact>()

    init {
        prefs.getString("data", null)?.let { fromJson(it) }
    }

    fun villageName(id: Long) = villages.find { it.id == id }?.name ?: "-"

    fun save() {
        prefs.edit().putString("data", toJson()).apply()
    }

    // ---- PIN recovery (security question) ----
    private fun norm(a: String) = a.trim().lowercase().replace(Regex("\\s+"), " ")

    fun hasRecovery() = prefs.getString("rec_hash", null) != null
    fun recoveryQuestion(): String = prefs.getString("rec_q", "") ?: ""

    fun setRecovery(question: String, answer: String) {
        val salt = Base64.encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
        prefs.edit().putString("rec_q", question).putString("rec_salt", salt)
            .putString("rec_hash", hash(norm(answer), salt))
            .putInt("rec_fails", 0).putLong("rec_lock_until", 0).apply()
    }

    fun recoveryWaitSeconds(): Int {
        val ms = prefs.getLong("rec_lock_until", 0) - System.currentTimeMillis()
        return if (ms > 0) (ms / 1000).toInt() + 1 else 0
    }

    fun checkRecovery(answer: String): Boolean {
        if (recoveryWaitSeconds() > 0) return false
        val ok = hash(norm(answer), prefs.getString("rec_salt", "") ?: "") == prefs.getString("rec_hash", "")
        if (ok) {
            prefs.edit().putInt("rec_fails", 0).putLong("rec_lock_until", 0).apply()
        } else {
            val f = prefs.getInt("rec_fails", 0) + 1
            val e = prefs.edit().putInt("rec_fails", f)
            // 3 wrong answers -> wait grows: 60s, 120s, 240s ... (max 15 min)
            if (f >= 3) e.putLong("rec_lock_until", System.currentTimeMillis() + minOf(900L, 60L shl (f - 3).coerceAtMost(4)) * 1000)
            e.apply()
        }
        return ok
    }

    /** Erase PIN, recovery and ALL village data (used when PIN is forgotten) */
    fun resetAll() {
        prefs.edit().clear().apply()
        villages.clear(); issues.clear(); works.clear(); infos.clear(); gps.clear(); families.clear(); contacts.clear()
    }

    // ---- Village info + GP contacts ----
    fun gpOf(v: Village): GpContact? = gps[gpKey(v.state, v.district, v.taluka, v.gp)]
    fun upsertInfo(id: Long, i: Info) { infos[id] = i; save() }
    fun upsertGp(c: GpContact) { gps[c.key] = c; save() }
    /** CSV/TSV lines: state,district,taluka,gp,sarpanch,sarpanchPhone,gramsevak,gramsevakPhone */
    fun importGp(text: String): Int {
        var n = 0
        text.lines().forEach { line ->
            val p = line.split(',', '\t').map { it.trim() }
            if (p.size >= 8 && p[0].lowercase() != "state" && p[3].isNotBlank()) {
                gps[gpKey(p[0], p[1], p[2], p[3])] = GpContact(gpKey(p[0], p[1], p[2], p[3]), p[4], p[5], p[6], p[7])
                n++
            }
        }
        save(); return n
    }

    fun addContact(c: VContact) { contacts.add(c); save() }
    fun deleteContact(id: Long) { contacts.removeAll { it.id == id }; save() }

    // ---- Families ----
    fun upsertFamily(f: Family) {
        val i = families.indexOfFirst { it.id == f.id }
        if (i >= 0) families[i] = f else families.add(0, f)
        save()
    }
    fun deleteFamily(id: Long) { families.removeAll { it.id == id }; save() }
    fun addMember(familyId: Long, m: Member) {
        val i = families.indexOfFirst { it.id == familyId }
        if (i >= 0) { families[i] = families[i].copy(members = families[i].members + m); save() }
    }
    fun removeMember(familyId: Long, memberId: Long) {
        val i = families.indexOfFirst { it.id == familyId }
        if (i >= 0) { families[i] = families[i].copy(members = families[i].members.filter { it.id != memberId }); save() }
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
        infos.remove(id)
        families.removeAll { it.villageId == id }
        contacts.removeAll { it.villageId == id }
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
                put(JSONObject().put("id", it.id).put("state", it.state).put("district", it.district)
                    .put("taluka", it.taluka).put("gp", it.gp).put("name", it.name).put("pincode", it.pincode)
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
        o.put("contacts", JSONArray().apply {
            contacts.forEach {
                put(JSONObject().put("id", it.id).put("villageId", it.villageId).put("label", it.label)
                    .put("name", it.name).put("phone", it.phone))
            }
        })
        o.put("families", JSONArray().apply {
            families.forEach { f ->
                put(JSONObject().put("id", f.id).put("villageId", f.villageId).put("head", f.head)
                    .put("mobile", f.mobile).put("address", f.address).put("notes", f.notes)
                    .put("members", JSONArray().apply {
                        f.members.forEach { m ->
                            put(JSONObject().put("id", m.id).put("name", m.name).put("relation", m.relation)
                                .put("gender", m.gender).put("age", m.age).put("occupation", m.occupation))
                        }
                    }))
            }
        })
        o.put("infos", JSONArray().apply {
            infos.forEach { (id, i) ->
                put(JSONObject().put("id", id).put("male", i.male).put("female", i.female).put("literacy", i.literacy)
                    .put("eduPrimary", i.eduPrimary).put("eduSecondary", i.eduSecondary).put("eduHigher", i.eduHigher)
                    .put("houses", i.houses).put("water", i.water).put("elec", i.elec)
                    .put("schools", i.schools).put("anganwadi", i.anganwadi).put("health", i.health)
                    .put("areaHa", i.areaHa).put("agriHa", i.agriHa).put("forestHa", i.forestHa)
                    .put("waterBodies", i.waterBodies).put("irrigation", i.irrigation)
                    .put("crops", i.crops).put("updated", i.updated).put("lat", i.lat).put("lon", i.lon))
            }
        })
        o.put("gps", JSONArray().apply {
            gps.values.forEach {
                put(JSONObject().put("key", it.key).put("sarpanch", it.sarpanch).put("sarpanchPhone", it.sarpanchPhone)
                    .put("gramsevak", it.gramsevak).put("gramsevakPhone", it.gramsevakPhone))
            }
        })
        return o.toString()
    }

    fun fromJson(s: String): Boolean = try {
        val o = JSONObject(s)
        val v = ArrayList<Village>(); val i = ArrayList<Issue>(); val w = ArrayList<Work>()
        o.getJSONArray("villages").let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                v.add(Village(it.getLong("id"), it.optString("state"), it.optString("district"), it.optString("taluka"),
                    it.optString("gp"), it.getString("name"), it.optString("pincode"),
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
        val im = HashMap<Long, Info>(); val gm = HashMap<String, GpContact>()
        o.optJSONArray("infos")?.let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                im[it.getLong("id")] = Info(it.optInt("male"), it.optInt("female"), it.optDouble("literacy"),
                    it.optDouble("eduPrimary"), it.optDouble("eduSecondary"), it.optDouble("eduHigher"),
                    it.optInt("houses"), it.optInt("water"), it.optInt("elec"),
                    it.optInt("schools"), it.optInt("anganwadi"), it.optInt("health"),
                    it.optDouble("areaHa"), it.optDouble("agriHa"), it.optDouble("forestHa"),
                    it.optInt("waterBodies"), it.optInt("irrigation"), it.optString("crops"), it.optString("updated"),
                    it.optDouble("lat", 0.0), it.optDouble("lon", 0.0))
            }
        }
        o.optJSONArray("gps")?.let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                gm[it.getString("key")] = GpContact(it.getString("key"), it.optString("sarpanch"),
                    it.optString("sarpanchPhone"), it.optString("gramsevak"), it.optString("gramsevakPhone"))
            }
        }
        val cl = ArrayList<VContact>()
        o.optJSONArray("contacts")?.let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let {
                cl.add(VContact(it.getLong("id"), it.getLong("villageId"), it.optString("label"), it.optString("name"), it.optString("phone")))
            }
        }
        val fl = ArrayList<Family>()
        o.optJSONArray("families")?.let { a ->
            for (k in 0 until a.length()) a.getJSONObject(k).let { f ->
                val ms = ArrayList<Member>()
                f.optJSONArray("members")?.let { ma ->
                    for (j in 0 until ma.length()) ma.getJSONObject(j).let {
                        ms.add(Member(it.getLong("id"), it.optString("name"), it.optString("relation"),
                            it.optInt("gender"), it.optInt("age"), it.optString("occupation")))
                    }
                }
                fl.add(Family(f.getLong("id"), f.getLong("villageId"), f.optString("head"), f.optString("mobile"),
                    f.optString("address"), f.optString("notes"), ms))
            }
        }
        families.clear(); families.addAll(fl)
        contacts.clear(); contacts.addAll(cl)
        infos.clear(); infos.putAll(im); gps.clear(); gps.putAll(gm)
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

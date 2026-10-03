package com.ganeshadigital.mlagaav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------- Admin login (uses the app PIN) ----------------
@Composable
fun AdminLoginDialog(s: AppState, onDismiss: () -> Unit, onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var wait by remember { mutableIntStateOf(s.waitSeconds()) }
    LaunchedEffect(wait) { if (wait > 0) { delay(1000); wait = s.waitSeconds() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔐 ग्रामपंचायत Admin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("अॅपचा PIN टाका.", fontSize = 13.sp)
                PinField("अॅप PIN", pin) { pin = it; msg = "" }
                if (wait > 0) Text("खूप चुकीचे प्रयत्न. $wait सेकंद थांबा.", color = Red)
                else if (msg.isNotEmpty()) Text(msg, color = Red)
            }
        },
        confirmButton = {
            Button(enabled = wait == 0, onClick = {
                if (s.checkPin(pin)) { s.adminMode = true; onOk() }
                else { pin = ""; wait = s.waitSeconds(); msg = "चुकीचा PIN" }
            }) { Text("Admin Login") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द") } }
    )
}

// ---------------- Dashboard ----------------
@Composable
fun AdminDashPage(s: AppState, go: (String) -> Unit) {
    val pend = s.families.count { it.status == 0 }
    PageColumn {
        item { Text("ग्रामपंचायत Admin Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("एकूण कुटुंबे", "${s.families.size}", Brand, Modifier.weight(1f))
                StatCard("प्रलंबित नोंदणी", "$pend", Amber, Modifier.weight(1f))
                StatCard("मंजूर", "${s.families.count { it.status == 1 }}", Green, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("एकूण सदस्य", "${s.families.sumOf { it.members.size }}", Brand, Modifier.weight(1f))
                StatCard("प्रलंबित अर्ज", "${s.apps.count { it.status == 0 }}", Amber, Modifier.weight(1f))
                StatCard("दुरुस्ती विनंत्या", "${s.reqs.count { it.status == 0 }}", Red, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("योजना", "${s.schemes.size}", Green, Modifier.weight(1f))
                StatCard("सूचना", "${s.notices.size}", Brand, Modifier.weight(1f))
                StatCard("गावे", "${s.villages.size}", Green, Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = { go("a_verify") }, modifier = Modifier.fillMaxWidth()) { Text("✅ कुटुंब नोंदणी तपासणी ($pend)") }
        }
        item { OutlinedButton(onClick = { go("a_apps") }, modifier = Modifier.fillMaxWidth()) { Text("📨 ऑनलाइन अर्ज") } }
        item { OutlinedButton(onClick = { go("a_status") }, modifier = Modifier.fillMaxWidth()) { Text("📋 मंजूर / प्रलंबित नोंदी") } }
    }
}

// ---------------- Family review ----------------
@Composable
fun FamilyReviewCard(s: AppState, f: Family) {
    var members by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    val v = s.villages.find { it.id == f.villageId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.head, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(REG_STATUS[f.status.coerceIn(0, 2)], color = regColor(f.status), fontWeight = FontWeight.Medium)
            if (v != null) Text("गाव: ${v.name} • ग्रा.पं. ${v.gp}", fontSize = 12.sp)
            Text("📞 ${f.mobile}  •  सदस्य: ${f.members.size}", fontSize = 12.sp)
            if (f.address.isNotBlank()) Text("📍 ${f.address}", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (f.status != 1) Button(onClick = { s.setFamilyStatus(f.id, 1) }) { Text("✅ मंजूर") }
                if (f.status != 2) OutlinedButton(onClick = { s.setFamilyStatus(f.id, 2) }) { Text("❌") }
                TextButton(onClick = { members = true }) { Text("👥") }
                TextButton(onClick = { del = true }) { Text("🗑", color = Red) }
            }
        }
    }
    if (members && v != null) FamilyMembersDialog(s, f.id, v) { members = false }
    if (del) ConfirmDialog("${f.head} यांचे कुटुंब आणि सर्व सदस्य हटवायचे?", { s.deleteFamily(f.id); del = false }, { del = false })
}

@Composable
fun AdminVerifyPage(s: AppState) {
    val list = s.families.filter { it.status == 0 }
    PageColumn {
        item { Text("तपासणी प्रलंबित कुटुंबे: ${list.size}", fontWeight = FontWeight.Bold) }
        if (list.isEmpty()) item { Text("सर्व नोंदणी तपासल्या आहेत. 🎉") }
        items(list, key = { it.id }) { FamilyReviewCard(s, it) }
    }
}

// ---------------- सदस्य माहिती ----------------
@Composable
fun AdminMembersPage(s: AppState) {
    var q by remember { mutableStateOf("") }
    val all = s.families.flatMap { f -> f.members.map { m -> m to f } }
        .filter { (m, f) -> q.isBlank() || m.name.contains(q, true) || f.head.contains(q, true) || f.mobile.contains(q) }
    PageColumn {
        item {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, label = { Text("सदस्य शोधा (नाव / प्रमुख / मोबाईल)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        item { Text("एकूण सदस्य: ${all.size}", fontWeight = FontWeight.Bold) }
        items(all, key = { it.first.id }) { (m, f) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.name, fontWeight = FontWeight.Medium)
                    Text(
                        "${m.relation} • ${GENDERS.getOrElse(m.gender) { "" }}" +
                            (if (m.age > 0) " • ${m.age} वर्षे" else "") +
                            (if (m.occupation.isNotBlank()) " • ${m.occupation}" else ""),
                        fontSize = 12.sp
                    )
                    Text("कुटुंब: ${f.head} • ${s.villageName(f.villageId)}", fontSize = 11.sp)
                }
            }
        }
    }
}

// ---------------- Applications ----------------
@Composable
fun AppReviewCard(s: AppState, a: SchemeApp) {
    val f = s.families.find { it.id == a.familyId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(a.schemeTitle, fontWeight = FontWeight.Bold)
            Text("${f?.head ?: "-"} • ${f?.mobile ?: ""} • ${a.date}", fontSize = 12.sp)
            if (a.note.isNotBlank()) Text("टिपणी: ${a.note}", fontSize = 12.sp)
            Text(REG_STATUS[a.status.coerceIn(0, 2)], color = regColor(a.status), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (a.status != 1) Button(onClick = { s.setAppStatus(a.id, 1) }) { Text("✅ मंजूर") }
                if (a.status != 2) OutlinedButton(onClick = { s.setAppStatus(a.id, 2) }) { Text("❌") }
                TextButton(onClick = { s.deleteApp(a.id) }) { Text("🗑", color = Red) }
            }
        }
    }
}

@Composable
fun AdminAppsPage(s: AppState) {
    var filter by remember { mutableIntStateOf(-1) }
    val list = s.apps.filter { filter == -1 || it.status == filter }
    PageColumn {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == -1, onClick = { filter = -1 }, label = { Text("सर्व") })
                REG_STATUS.forEachIndexed { i, n -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(n) }) }
            }
        }
        if (list.isEmpty()) item { Text("अर्ज नाहीत.") }
        items(list, key = { it.id }) { AppReviewCard(s, it) }
    }
}

// ---------------- मंजूर / प्रलंबित नोंदी ----------------
@Composable
fun AdminStatusPage(s: AppState) {
    var filter by remember { mutableIntStateOf(0) }
    val fams = s.families.filter { it.status == filter }
    val apps = s.apps.filter { it.status == filter }
    val reqs = s.reqs.filter { (if (it.status == 0) 0 else 1) == filter }
    PageColumn {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                REG_STATUS.forEachIndexed { i, n -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(n) }) }
            }
        }
        item { Text("कुटुंब नोंदणी (${fams.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(fams, key = { it.id }) { FamilyReviewCard(s, it) }
        item { Text("अर्ज (${apps.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(apps, key = { it.id }) { AppReviewCard(s, it) }
        item { Text("दुरुस्ती विनंत्या (${reqs.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(reqs, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    val f = s.families.find { it.id == r.familyId }
                    Text(r.text)
                    Text("${f?.head ?: "-"} • ${f?.mobile ?: ""} • ${r.date}", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (r.status == 0) Button(onClick = { s.setReqStatus(r.id, 1) }) { Text("✅ पूर्ण") }
                        else OutlinedButton(onClick = { s.setReqStatus(r.id, 0) }) { Text("पुन्हा उघडा") }
                    }
                }
            }
        }
    }
}

// ---------------- योजना व्यवस्थापन ----------------
@Composable
fun AdminSchemesPage(s: AppState) {
    var add by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Scheme?>(null) }
    PageColumn {
        item { Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Text("➕ नवीन योजना") } }
        if (s.schemes.isEmpty()) item { Text("अजून योजना नाहीत.") }
        items(s.schemes, key = { it.id }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title, fontWeight = FontWeight.Bold)
                    Text(SCHEME_CAT_NAMES.getOrElse(it.category) { "" }, fontSize = 11.sp, color = Brand)
                    if (it.desc.isNotBlank()) Text(it.desc, fontSize = 13.sp)
                    if (it.eligibility.isNotBlank()) Text("पात्रता: ${it.eligibility}", fontSize = 12.sp)
                    TextButton(onClick = { del = it }) { Text("🗑 हटवा", color = Red) }
                }
            }
        }
    }
    if (add) {
        var t by remember { mutableStateOf("") }
        var d by remember { mutableStateOf("") }
        var e by remember { mutableStateOf("") }
        var c by remember { mutableIntStateOf(6) }
        FormDialog("नवीन योजना", { add = false }, onSave = {
            if (t.isNotBlank()) { s.addScheme(Scheme(newId(), t.trim(), d.trim(), e.trim(), c)); add = false }
        }) {
            Picker("योजना वर्ग", SCHEME_CAT_NAMES, c) { c = it }
            Field("योजनेचे नाव *", t, { t = it })
            Field("माहिती", d, { d = it }, lines = 3)
            Field("पात्रता", e, { e = it }, lines = 2)
        }
    }
    del?.let { x -> ConfirmDialog("'${x.title}' योजना हटवायची?", { s.deleteScheme(x.id); del = null }, { del = null }) }
}

// ---------------- सूचना व्यवस्थापन ----------------
@Composable
fun AdminNoticesPage(s: AppState) {
    var add by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Notice?>(null) }
    PageColumn {
        item { Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Text("➕ नवीन सूचना / बातमी") } }
        if (s.notices.isEmpty()) item { Text("अजून सूचना नाहीत.") }
        items(s.notices, key = { it.id }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title, fontWeight = FontWeight.Bold)
                    Text(it.date, fontSize = 11.sp)
                    if (it.body.isNotBlank()) Text(it.body, fontSize = 13.sp)
                    TextButton(onClick = { del = it }) { Text("🗑 हटवा", color = Red) }
                }
            }
        }
    }
    if (add) {
        var t by remember { mutableStateOf("") }
        var b by remember { mutableStateOf("") }
        FormDialog("नवीन सूचना", { add = false }, onSave = {
            if (t.isNotBlank()) { s.addNotice(Notice(newId(), t.trim(), b.trim(), today())); add = false }
        }) {
            Field("शीर्षक *", t, { t = it })
            Field("मजकूर", b, { b = it }, lines = 4)
        }
    }
    del?.let { x -> ConfirmDialog("ही सूचना हटवायची?", { s.deleteNotice(x.id); del = null }, { del = null }) }
}

// ---------------- अहवाल ----------------
@Composable
fun AdminReportsPage(s: AppState) {
    val ctx = LocalContext.current
    val text = buildString {
        append(s.report())
        appendLine()
        appendLine("👨‍👩‍👧 कुटुंब नोंदणी: एकूण ${s.families.size}, मंजूर ${s.families.count { it.status == 1 }}, प्रलंबित ${s.families.count { it.status == 0 }}, नामंजूर ${s.families.count { it.status == 2 }}")
        appendLine("👥 एकूण सदस्य: ${s.families.sumOf { it.members.size }}")
        appendLine("📨 अर्ज: प्रलंबित ${s.apps.count { it.status == 0 }}, मंजूर ${s.apps.count { it.status == 1 }}, नामंजूर ${s.apps.count { it.status == 2 }}")
        appendLine("🛠 दुरुस्ती विनंत्या प्रलंबित: ${s.reqs.count { it.status == 0 }}")
        s.villages.forEach { v ->
            val fs = s.families.filter { it.villageId == v.id && it.status == 1 }
            appendLine("• ${v.name}: कुटुंबे ${fs.size}, सदस्य ${fs.sumOf { it.members.size }}")
        }
    }
    PageColumn {
        item { Button(onClick = { shareText(ctx, text) }, modifier = Modifier.fillMaxWidth()) { Text("📤 अहवाल शेअर करा (WhatsApp)") } }
        item { Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(14.dp), fontSize = 13.sp) } }
    }
}

// ---------------- User Management ----------------
@Composable
fun AdminUsersPage(s: AppState) {
    var setFor by remember { mutableStateOf<Family?>(null) }
    var showPin by remember { mutableStateOf(false) }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Admin", fontWeight = FontWeight.Bold)
                    Text("Admin Login = अॅप PIN. हा PIN कोणालाही देऊ नका.", fontSize = 12.sp)
                    OutlinedButton(onClick = { showPin = true }) { Text("🔑 अॅप PIN / रिकव्हरी बदला") }
                }
            }
        }
        item { Text("कुटुंब Login (${s.families.size})", fontWeight = FontWeight.Bold) }
        items(s.families, key = { it.id }) { f ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(f.head, fontWeight = FontWeight.Medium)
                    Text("📞 ${f.mobile.ifBlank { "-" }} • ${if (f.pinHash.isNotEmpty()) "Login सुरू" else "Login नाही"}", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { setFor = f }) { Text(if (f.pinHash.isNotEmpty()) "PIN रीसेट" else "PIN सेट") }
                        if (f.pinHash.isNotEmpty()) TextButton(onClick = { s.clearFamilyPin(f.id) }) { Text("Login बंद", color = Red) }
                    }
                }
            }
        }
    }
    setFor?.let { f ->
        var p by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        FormDialog("${f.head} – नवीन PIN", { setFor = null }, onSave = {
            if (f.mobile.length != 10) msg = "आधी कुटुंबात 10 अंकी मोबाईल भरा"
            else if (p.length < 4) msg = "PIN किमान 4 अंकी हवा"
            else { s.setFamilyPin(f.id, p); setFor = null }
        }) {
            PinField("नवीन PIN", p) { p = it; msg = "" }
            if (msg.isNotEmpty()) Text(msg, color = Red)
        }
    }
    if (showPin) ChangePinDialog(s) { showPin = false }
}

// ---------------- Data Backup ----------------
@Composable
fun AdminBackupPage(s: AppState) {
    val ctx = LocalContext.current
    var showRestore by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 Data Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("सर्व डेटा (गावे, कुटुंबे, अर्ज, योजना, सूचना) शेअर करून सुरक्षित ठेवा. फोटो फक्त याच फोनवर राहतात.", fontSize = 12.sp)
                    Button(onClick = { shareText(ctx, s.toJson()) }, modifier = Modifier.fillMaxWidth()) { Text("📤 बॅकअप शेअर करा") }
                    OutlinedButton(onClick = { showRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("♻️ रिस्टोर") }
                }
            }
        }
    }
    if (showRestore) {
        FormDialog("रिस्टोर", { showRestore = false; msg = "" }, onSave = {
            if (s.fromJson(restoreText.trim())) { showRestore = false; restoreText = ""; msg = "" }
            else msg = "चुकीचा बॅकअप डेटा"
        }) {
            Text("बॅकअप मजकूर पेस्ट करा. सध्याचा डेटा बदलला जाईल.")
            Field("बॅकअप डेटा", restoreText, { restoreText = it }, lines = 4)
            if (msg.isNotEmpty()) Text(msg, color = Red)
        }
    }
}

// ---------------- Settings ----------------
@Composable
fun AdminSettingsPage(s: AppState, go: (String) -> Unit) {
    var name by remember { mutableStateOf(s.officeName) }
    var phone by remember { mutableStateOf(s.officePhone) }
    var addr by remember { mutableStateOf(s.officeAddress) }
    var mail by remember { mutableStateOf(s.officeEmail) }
    var saved by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("संपर्क पानावरील माहिती", fontWeight = FontWeight.Bold)
                    Field("कार्यालय / नाव", name, { name = it; saved = false })
                    Field("मोबाईल", phone, { if (it.length <= 10 && it.all(Char::isDigit)) { phone = it; saved = false } }, number = true)
                    Field("पत्ता", addr, { addr = it; saved = false }, lines = 2)
                    Field("ईमेल", mail, { mail = it; saved = false })
                    if (saved) Text("✅ सेव्ह झाले", color = Green)
                    Button(onClick = { s.setOffice(name.trim(), phone, addr.trim(), mail.trim()); saved = true }, modifier = Modifier.fillMaxWidth()) { Text("सेव्ह") }
                }
            }
        }
        item { OutlinedButton(onClick = { showPin = true }, modifier = Modifier.fillMaxWidth()) { Text("🔑 अॅप PIN / रिकव्हरी बदला") } }
        item { OutlinedButton(onClick = { showImport = true }, modifier = Modifier.fillMaxWidth()) { Text("👥 सरपंच/ग्रामसेवक यादी इंपोर्ट") } }
        item {
            OutlinedButton(onClick = { s.adminMode = false; go("home") }, modifier = Modifier.fillMaxWidth()) { Text("🔓 Admin मधून बाहेर पडा") }
        }
    }
    if (showPin) ChangePinDialog(s) { showPin = false }
    if (showImport) GpImportDialog(s) { showImport = false }
}

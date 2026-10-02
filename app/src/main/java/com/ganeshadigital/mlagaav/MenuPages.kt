package com.ganeshadigital.mlagaav

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun regColor(st: Int) = when (st) { 0 -> Amber; 1 -> Green; else -> Red }

@Composable
fun PageColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun VillageSelector(s: AppState, sel: Long?, onSelect: (Long) -> Unit) {
    if (s.villages.isEmpty()) {
        Text("अजून गाव जोडलेले नाही. 'मुख्यपृष्ठ → गावे' मधून गाव जोडा.")
        return
    }
    val idx = s.villages.indexOfFirst { it.id == sel }.let { if (it < 0) 0 else it }
    Picker("गाव निवडा", s.villages.map { "${it.name} (${it.gp})" }, idx) { onSelect(s.villages[it].id) }
}

fun pickVillage(s: AppState, sel: Long?): Village? = s.villages.find { it.id == sel } ?: s.villages.firstOrNull()

@Composable
fun NeedLogin(go: (String) -> Unit) {
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("हे पान पाहण्यासाठी आधी कुटुंब प्रमुख Login करा.", fontWeight = FontWeight.Medium)
                    Button(onClick = { go("login") }, modifier = Modifier.fillMaxWidth()) { Text("👤 Login") }
                    OutlinedButton(onClick = { go("register") }, modifier = Modifier.fillMaxWidth()) { Text("📝 नवीन कुटुंब नोंदणी") }
                }
            }
        }
    }
}

// ---------------- माझे गाव ----------------
@Composable
fun MyVillagePage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    var showInfo by remember { mutableStateOf(false) }
    var showGp by remember { mutableStateOf(false) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            item { VillageDashboard(s, v, { if (s.adminMode) showInfo = true }, { if (s.adminMode) showGp = true }, null) }
            item { VillageActions(s, v) }
        }
    }
    if (v != null && showInfo) InfoDialog(s, v) { showInfo = false }
    if (v != null && showGp) GpDialog(s, v) { showGp = false }
}

// ---------------- गावाची माहिती ----------------
@Composable
fun VillageInfoPage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            val gc = s.gpOf(v)
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(v.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Brand)
                        Text("राज्य: ${v.state}   जिल्हा: ${v.district}")
                        Text("तालुका: ${v.taluka}   ग्रामपंचायत: ${v.gp}")
                        Text("पिन कोड: ${v.pincode}")
                        Text("लोकसंख्या: ${v.population}   कुटुंबे: ${v.households}")
                        if (v.notes.isNotBlank()) Text("टिपणी: ${v.notes}")
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("ग्रामपंचायत संपर्क", fontWeight = FontWeight.Bold)
                        if (gc != null) {
                            ContactRow("सरपंच", gc.sarpanch, gc.sarpanchPhone)
                            ContactRow("ग्रामसेवक", gc.gramsevak, gc.gramsevakPhone)
                        } else ContactRow("सरपंच", v.sarpanch, v.contact)
                    }
                }
            }
            val vi = s.issues.filter { it.villageId == v.id }
            val vw = s.works.filter { it.villageId == v.id }
            item { Text("समस्या (${vi.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(vi, key = { it.id }) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.title, fontWeight = FontWeight.Medium)
                        Text("${it.category} • ${it.date}", fontSize = 12.sp)
                        Text(ISSUE_STATUS[it.status.coerceIn(0, 2)], color = statusColor(it.status), fontSize = 13.sp)
                    }
                }
            }
            item { Text("विकास कामे (${vw.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(vw, key = { it.id }) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.title, fontWeight = FontWeight.Medium)
                        Text("₹ ${"%.2f".format(it.budgetLakh)} लाख • ${it.date}", fontSize = 12.sp)
                        Text(WORK_STATUS[it.status.coerceIn(0, 2)], color = statusColor(it.status), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ---------------- कुटुंब नोंदणी (public list + register) ----------------
@Composable
fun FamiliesPage(s: AppState, go: (String) -> Unit) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    val v = pickVillage(s, vid)
    PageColumn {
        item {
            Button(onClick = { go("register") }, modifier = Modifier.fillMaxWidth()) { Text("📝 नवीन कुटुंब नोंदणी करा") }
        }
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            val list = s.families.filter { it.villageId == v.id && it.status == 1 }
            item { Text("नोंदणीकृत कुटुंबे: ${list.size}", fontWeight = FontWeight.Bold) }
            items(list, key = { it.id }) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.head, fontWeight = FontWeight.Medium)
                        Text("सदस्य: ${it.members.size}" + (if (it.address.isNotBlank()) " • ${it.address}" else ""), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ---------------- ग्रामपंचायत ----------------
@Composable
fun GpPage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            val gc = s.gpOf(v)
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("ग्रामपंचायत ${v.gp}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Brand)
                        Text("तालुका: ${v.taluka}   जिल्हा: ${v.district}")
                        Text("राज्य: ${v.state}   पिन: ${v.pincode}")
                        Spacer(Modifier.height(6.dp))
                        if (gc != null) {
                            ContactRow("सरपंच", gc.sarpanch, gc.sarpanchPhone)
                            ContactRow("ग्रामसेवक", gc.gramsevak, gc.gramsevakPhone)
                        } else {
                            ContactRow("सरपंच", v.sarpanch, v.contact)
                            Text("ग्रामसेवक माहिती अजून भरलेली नाही.", fontSize = 12.sp)
                        }
                    }
                }
            }
            val vf = s.families.count { it.villageId == v.id && it.status == 1 }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("नोंदणीकृत कुटुंबे", "$vf", Brand, Modifier.weight(1f))
                    StatCard("प्रलंबित समस्या", "${s.issues.count { it.villageId == v.id && it.status != 2 }}", Red, Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------- ग्रामपंचायत योजना ----------------
@Composable
fun SchemesPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    var applyTo by remember { mutableStateOf<Scheme?>(null) }
    PageColumn {
        if (s.schemes.isEmpty()) item { Text("अजून योजना जोडलेल्या नाहीत. ग्रामपंचायत Admin → योजना व्यवस्थापन मधून जोडता येतील.") }
        items(s.schemes, key = { it.id }) { sc ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(sc.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (sc.desc.isNotBlank()) Text(sc.desc, fontSize = 13.sp)
                    if (sc.eligibility.isNotBlank()) Text("पात्रता: ${sc.eligibility}", fontSize = 12.sp)
                    if (fam == null) TextButton(onClick = { go("login") }) { Text("अर्ज करण्यासाठी Login करा") }
                    else if (fam.status == 1) Button(onClick = { applyTo = sc }) { Text("📨 अर्ज करा") }
                    else Text("नोंदणी मंजूर झाल्यावर अर्ज करता येईल.", fontSize = 12.sp)
                }
            }
        }
    }
    applyTo?.let { sc ->
        if (fam != null) ApplyDialog(s, fam, sc.title) { applyTo = null }
    }
}

@Composable
fun ApplyDialog(s: AppState, fam: Family, schemeTitle: String, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf("") }
    FormDialog("अर्ज: $schemeTitle", onDismiss, onSave = {
        s.addApp(SchemeApp(newId(), fam.id, schemeTitle, note.trim(), today(), 0))
        onDismiss()
    }) {
        Text("कुटुंब: ${fam.head}")
        Field("टिपणी / अतिरिक्त माहिती", note, { note = it }, lines = 3)
        Text("अर्ज ग्रामपंचायतीकडे जमा होईल. स्थिती 'अर्जांची स्थिती' मध्ये दिसेल.", fontSize = 11.sp)
    }
}

// ---------------- ऑनलाईन अर्ज ----------------
@Composable
fun ApplyPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    if (fam == null) { NeedLogin(go); return }
    val options = s.schemes.map { it.title } + "इतर अर्ज"
    var sel by remember { mutableIntStateOf(0) }
    var other by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ऑनलाईन अर्ज – ${fam.head}", fontWeight = FontWeight.Bold)
                    if (fam.status != 1) Text("तुमची नोंदणी अजून मंजूर नाही (${REG_STATUS[fam.status.coerceIn(0, 2)]}). मंजुरीनंतर अर्ज करता येईल.", color = Red)
                    else {
                        Picker("योजना / अर्जाचा प्रकार", options, sel) { sel = it; msg = "" }
                        if (sel == options.size - 1) Field("अर्जाचा विषय *", other, { other = it })
                        Field("टिपणी", note, { note = it }, lines = 3)
                        if (msg.isNotEmpty()) Text(msg, color = Green)
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            val title = if (sel == options.size - 1) other.trim() else options[sel]
                            if (title.isBlank()) msg = "विषय टाका"
                            else {
                                s.addApp(SchemeApp(newId(), fam.id, title, note.trim(), today(), 0))
                                note = ""; other = ""; msg = "✅ अर्ज जमा झाला."
                            }
                        }) { Text("अर्ज जमा करा") }
                    }
                }
            }
        }
    }
}

// ---------------- सूचना / बातम्या ----------------
@Composable
fun NewsPage(s: AppState) {
    PageColumn {
        if (s.notices.isEmpty()) item { Text("सध्या कोणतीही सूचना नाही.") }
        items(s.notices, key = { it.id }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(it.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(it.date, fontSize = 11.sp)
                    if (it.body.isNotBlank()) Text(it.body, fontSize = 13.sp)
                }
            }
        }
    }
}

// ---------------- महत्त्वाचे संपर्क ----------------
@Composable
fun ContactsPage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            val gc = s.gpOf(v)
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("ग्रामपंचायत / गाव", fontWeight = FontWeight.Bold)
                        if (gc != null) {
                            ContactRow("सरपंच", gc.sarpanch, gc.sarpanchPhone)
                            ContactRow("ग्रामसेवक", gc.gramsevak, gc.gramsevakPhone)
                        } else ContactRow("सरपंच", v.sarpanch, v.contact)
                        s.contacts.filter { it.villageId == v.id }.forEach { ContactRow(it.label, it.name, it.phone) }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("आपत्कालीन क्रमांक", fontWeight = FontWeight.Bold)
                    ContactRow("आपत्कालीन (सर्व)", "", "112")
                    ContactRow("पोलीस", "", "100")
                    ContactRow("अग्निशमन", "", "101")
                    ContactRow("रुग्णवाहिका", "", "108")
                    ContactRow("महिला हेल्पलाईन", "", "181")
                }
            }
        }
    }
}

// ---------------- गावातील सेवा ----------------
@Composable
fun ServicesPage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) item { VillageActions(s, v) }
    }
}

// ---------------- गावाचा नकाशा ----------------
@Composable
fun MapPage(s: AppState) {
    var vid by remember { mutableStateOf(s.myFamily()?.villageId) }
    var show by remember { mutableStateOf(false) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) item {
            Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("📍 ${v.name} – नकाशा उघडा") }
        }
    }
    if (v != null && show) MapDialog(s, v) { show = false }
}

// ---------------- फोटो / गॅलरी ----------------
@Composable
fun UriImage(uri: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<android.graphics.Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val u = android.net.Uri.parse(uri)
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
                var sample = 1
                while (o.outWidth / sample > 1200) sample *= 2
                val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o2) }
            } catch (e: Exception) { null }
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) { Text("🖼") }
}

@Composable
fun GalleryPage(s: AppState) {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var del by remember { mutableStateOf<Photo?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            pending = uri.toString(); caption = ""
        }
    }
    PageColumn {
        if (s.adminMode) item {
            Button(onClick = { launcher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("➕ फोटो जोडा") }
        } else item { Text("फोटो जोडण्यासाठी ग्रामपंचायत Admin मध्ये लॉगिन करा.", fontSize = 12.sp) }
        if (s.photos.isEmpty()) item { Text("अजून फोटो नाहीत.") }
        items(s.photos, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column {
                    UriImage(p.uri, Modifier.fillMaxWidth().height(200.dp))
                    Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.caption.ifBlank { "फोटो" }, fontWeight = FontWeight.Medium)
                            Text(p.date, fontSize = 11.sp)
                        }
                        if (s.adminMode) TextButton(onClick = { del = p }) { Text("🗑", color = Red) }
                    }
                }
            }
        }
    }
    pending?.let { u ->
        FormDialog("फोटो माहिती", { pending = null }, onSave = {
            s.addPhoto(Photo(newId(), u, caption.trim(), today())); pending = null
        }) { Field("शीर्षक / वर्णन", caption, { caption = it }) }
    }
    del?.let { p -> ConfirmDialog("हा फोटो गॅलरीतून काढायचा?", { s.deletePhoto(p.id); del = null }, { del = null }) }
}

// ---------------- मदत / FAQ ----------------
private val FAQ = listOf(
    "नवीन कुटुंब नोंदणी कशी करायची?" to "मेनू → नवीन कुटुंब नोंदणी. गाव निवडा, कुटुंब प्रमुखाचे नाव, मोबाईल आणि 4–6 अंकी कुटुंब PIN टाका. ग्रामपंचायत मंजुरी देईपर्यंत स्थिती 'प्रलंबित' राहील.",
    "Login कसे करायचे?" to "मेनू → कुटुंब प्रमुख Login. नोंदणीच्या वेळी दिलेला मोबाईल नंबर आणि कुटुंब PIN वापरा.",
    "कुटुंब PIN विसरलो तर?" to "ग्रामपंचायत Admin कडे जा. Admin → User Management मधून नवीन कुटुंब PIN सेट करता येतो.",
    "कुटुंबातील सदस्य कसे जोडायचे?" to "Login केल्यावर मेनू → कुटुंबातील सदस्य मध्ये नाव, नाते, वय इत्यादी भरा.",
    "नाव किंवा मोबाईल चुकला असेल तर?" to "मेनू → दुरुस्ती विनंती मध्ये बदल लिहा. ग्रामपंचायत तपासून दुरुस्त करेल.",
    "योजनेसाठी अर्ज कसा करायचा?" to "नोंदणी मंजूर झाल्यावर मेनू → ग्रामपंचायत योजना किंवा ऑनलाईन अर्ज मधून अर्ज करा. स्थिती 'अर्जांची स्थिती' मध्ये दिसेल.",
    "माझा डेटा सुरक्षित आहे का?" to "सर्व डेटा याच फोनमध्ये एन्क्रिप्टेड स्वरूपात साठवला जातो. आधार क्रमांक अॅपमध्ये साठवला जात नाही."
)

@Composable
fun HelpPage() {
    PageColumn {
        items(FAQ) { (q, a) ->
            var open by remember { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth().clickable { open = !open }) {
                Column(Modifier.padding(14.dp)) {
                    Text((if (open) "▼ " else "▶ ") + q, fontWeight = FontWeight.Bold)
                    if (open) Text(a, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ---------------- संपर्क ----------------
@Composable
fun ContactUsPage(s: AppState) {
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.officeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand)
                    if (s.officePhone.isNotBlank()) ContactRow("मोबाईल", "", s.officePhone)
                    if (s.officeAddress.isNotBlank()) Text("📍 ${s.officeAddress}")
                    if (s.officeEmail.isNotBlank()) Text("✉️ ${s.officeEmail}")
                    if (s.officePhone.isBlank() && s.officeAddress.isBlank() && s.officeEmail.isBlank())
                        Text("संपर्क तपशील ग्रामपंचायत Admin → Settings मध्ये भरा.", fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------- Login ----------------
@Composable
fun LoginPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    var mobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var wait by remember { mutableIntStateOf(s.famWait()) }
    LaunchedEffect(wait) { if (wait > 0) { kotlinx.coroutines.delay(1000); wait = s.famWait() } }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fam != null) {
                        Text("✅ Login: ${fam.head}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("नोंदणी स्थिती: ${REG_STATUS[fam.status.coerceIn(0, 2)]}", color = regColor(fam.status))
                        Button(onClick = { go("myinfo") }, modifier = Modifier.fillMaxWidth()) { Text("माझी माहिती") }
                        OutlinedButton(onClick = { s.familyId = null; go("home") }, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
                    } else {
                        Text("कुटुंब प्रमुख Login", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Field("मोबाईल नंबर", mobile, { if (it.length <= 10 && it.all(Char::isDigit)) { mobile = it; msg = "" } }, number = true)
                        PinField("कुटुंब PIN", pin) { pin = it; msg = "" }
                        if (wait > 0) Text("खूप चुकीचे प्रयत्न. $wait सेकंद थांबा.", color = Red)
                        else if (msg.isNotEmpty()) Text(msg, color = Red)
                        Button(enabled = wait == 0, modifier = Modifier.fillMaxWidth(), onClick = {
                            when {
                                mobile.length != 10 -> msg = "मोबाईल 10 अंकी हवा"
                                pin.length < 4 -> msg = "PIN किमान 4 अंकी हवा"
                                else -> when (s.loginFamily(mobile, pin)) {
                                    0 -> { pin = ""; go("myinfo") }
                                    2 -> wait = s.famWait()
                                    else -> { pin = ""; wait = s.famWait(); msg = "मोबाईल किंवा PIN चुकीचा" }
                                }
                            }
                        }) { Text("Login") }
                        OutlinedButton(onClick = { go("register") }, modifier = Modifier.fillMaxWidth()) { Text("📝 नवीन कुटुंब नोंदणी") }
                        Text("PIN विसरल्यास ग्रामपंचायत Admin कडून रीसेट करून घ्या.", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ---------------- नवीन कुटुंब नोंदणी ----------------
@Composable
fun RegisterPage(s: AppState, go: (String) -> Unit) {
    var vid by remember { mutableStateOf<Long?>(null) }
    var head by remember { mutableStateOf("") }
    var gender by remember { mutableIntStateOf(0) }
    var age by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val v = pickVillage(s, vid)
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("नवीन कुटुंब नोंदणी", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    VillageSelector(s, vid) { vid = it }
                    Field("कुटुंब प्रमुखाचे नाव *", head, { head = it; msg = "" })
                    Picker("लिंग", GENDERS, gender) { gender = it }
                    Field("वय", age, { if (it.length <= 3 && it.all(Char::isDigit)) age = it }, number = true)
                    Field("मोबाईल नंबर * (Login साठी)", mobile, { if (it.length <= 10 && it.all(Char::isDigit)) { mobile = it; msg = "" } }, number = true)
                    Field("पत्ता / वाडी / वस्ती", address, { address = it })
                    PinField("कुटुंब PIN * (4–6 अंक)", pin) { pin = it; msg = "" }
                    PinField("PIN पुन्हा टाका *", pin2) { pin2 = it; msg = "" }
                    if (msg.isNotEmpty()) Text(msg, color = Red)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        when {
                            v == null -> msg = "आधी गाव निवडा (गावे जोडलेली नाहीत?)"
                            head.isBlank() -> msg = "कुटुंब प्रमुखाचे नाव टाका"
                            mobile.length != 10 -> msg = "मोबाईल 10 अंकी हवा"
                            s.families.any { it.mobile == mobile } -> msg = "या मोबाईलने आधीच नोंदणी आहे. Login करा."
                            pin.length < 4 -> msg = "PIN किमान 4 अंकी हवा"
                            pin != pin2 -> msg = "दोन्ही PIN जुळत नाहीत"
                            else -> {
                                val id = newId()
                                s.registerFamily(
                                    Family(id, v.id, head.trim(), mobile, address.trim(), "",
                                        listOf(Member(newId(), head.trim(), RELATIONS[0], gender, age.toIntOrNull() ?: 0, ""))),
                                    pin
                                )
                                s.familyId = id
                                go("myinfo")
                            }
                        }
                    }) { Text("नोंदणी जमा करा") }
                    Text("नोंदणी ग्रामपंचायत तपासून मंजूर करेल. आधार क्रमांक अॅपमध्ये घेतला जात नाही.", fontSize = 11.sp)
                }
            }
        }
    }
}

// ---------------- माझी माहिती ----------------
@Composable
fun MyInfoPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    if (fam == null) { NeedLogin(go); return }
    val v = s.villages.find { it.id == fam.villageId }
    var edit by remember { mutableStateOf(false) }
    var pinDlg by remember { mutableStateOf(false) }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(fam.head, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Brand)
                    Text("नोंदणी स्थिती: ${REG_STATUS[fam.status.coerceIn(0, 2)]}", color = regColor(fam.status), fontWeight = FontWeight.Medium)
                    if (v != null) Text("गाव: ${v.name} • ग्रा.पं. ${v.gp} • ता. ${v.taluka}")
                    Text("📞 ${fam.mobile}")
                    if (fam.address.isNotBlank()) Text("📍 ${fam.address}")
                    if (fam.notes.isNotBlank()) Text("टिपणी: ${fam.notes}")
                    Text("सदस्य: ${fam.members.size}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { edit = true }) { Text("✏️ पत्ता / टिपणी") }
                        OutlinedButton(onClick = { pinDlg = true }) { Text("🔑 PIN बदला") }
                    }
                }
            }
        }
        if (v != null) {
            val gc = s.gpOf(v)
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("ग्रामपंचायत संपर्क", fontWeight = FontWeight.Bold)
                        if (gc != null) {
                            ContactRow("सरपंच", gc.sarpanch, gc.sarpanchPhone)
                            ContactRow("ग्रामसेवक", gc.gramsevak, gc.gramsevakPhone)
                        } else ContactRow("सरपंच", v.sarpanch, v.contact)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { go("members") }, modifier = Modifier.weight(1f)) { Text("👥 सदस्य") }
                OutlinedButton(onClick = { go("appstatus") }, modifier = Modifier.weight(1f)) { Text("📨 अर्ज") }
                OutlinedButton(onClick = { go("request") }, modifier = Modifier.weight(1f)) { Text("🛠 दुरुस्ती") }
            }
        }
    }
    if (edit) {
        var addr by remember { mutableStateOf(fam.address) }
        var nt by remember { mutableStateOf(fam.notes) }
        FormDialog("पत्ता / टिपणी", { edit = false }, onSave = {
            s.upsertFamily(fam.copy(address = addr.trim(), notes = nt.trim())); edit = false
        }) {
            Field("पत्ता / वाडी / वस्ती", addr, { addr = it })
            Field("टिपणी", nt, { nt = it }, lines = 3)
        }
    }
    if (pinDlg) FamilyPinDialog(s, fam) { pinDlg = false }
}

@Composable
fun FamilyPinDialog(s: AppState, fam: Family, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    FormDialog("कुटुंब PIN बदला", onDismiss, onSave = {
        when {
            !s.checkFamilyPin(fam.id, old) -> msg = "जुना PIN चुकीचा"
            n1.length < 4 -> msg = "नवीन PIN किमान 4 अंकी हवा"
            n1 != n2 -> msg = "नवीन PIN जुळत नाहीत"
            else -> { s.setFamilyPin(fam.id, n1); onDismiss() }
        }
    }) {
        PinField("सध्याचा PIN", old) { old = it; msg = "" }
        PinField("नवीन PIN", n1) { n1 = it; msg = "" }
        PinField("नवीन PIN पुन्हा", n2) { n2 = it; msg = "" }
        if (msg.isNotEmpty()) Text(msg, color = Red)
    }
}

// ---------------- कुटुंबातील सदस्य ----------------
@Composable
fun MembersPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    if (fam == null) { NeedLogin(go); return }
    val v = s.villages.find { it.id == fam.villageId }
    var dlg by remember { mutableStateOf(false) }
    PageColumn {
        item {
            Text("${fam.head} यांचे कुटुंब – एकूण ${fam.members.size} सदस्य", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (v != null) Button(onClick = { dlg = true }, modifier = Modifier.fillMaxWidth()) { Text("➕ सदस्य जोडा / काढा") }
        }
        items(fam.members, key = { it.id }) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.name, fontWeight = FontWeight.Medium)
                    Text(
                        "${m.relation} • ${GENDERS.getOrElse(m.gender) { "" }}" +
                            (if (m.age > 0) " • ${m.age} वर्षे" else "") +
                            (if (m.occupation.isNotBlank()) " • ${m.occupation}" else ""),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    if (dlg && v != null) FamilyMembersDialog(s, fam.id, v) { dlg = false }
}

// ---------------- अर्जांची स्थिती ----------------
@Composable
fun AppStatusPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    if (fam == null) { NeedLogin(go); return }
    val mine = s.apps.filter { it.familyId == fam.id }
    PageColumn {
        if (mine.isEmpty()) item { Text("तुम्ही अजून कोणताही अर्ज केलेला नाही.") }
        items(mine, key = { it.id }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.schemeTitle, fontWeight = FontWeight.Bold)
                    Text(it.date, fontSize = 11.sp)
                    if (it.note.isNotBlank()) Text("टिपणी: ${it.note}", fontSize = 12.sp)
                    Text(REG_STATUS[it.status.coerceIn(0, 2)], color = regColor(it.status), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------- दुरुस्ती विनंती ----------------
@Composable
fun RequestPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    if (fam == null) { NeedLogin(go); return }
    var text by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val mine = s.reqs.filter { it.familyId == fam.id }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("दुरुस्ती विनंती", fontWeight = FontWeight.Bold)
                    Text("नाव, मोबाईल किंवा इतर माहितीत चूक असल्यास येथे लिहा.", fontSize = 12.sp)
                    Field("काय दुरुस्त करायचे?", text, { text = it; msg = "" }, lines = 3)
                    if (msg.isNotEmpty()) Text(msg, color = Green)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        if (text.isBlank()) msg = "विनंती लिहा"
                        else { s.addReq(Req(newId(), fam.id, text.trim(), today(), 0)); text = ""; msg = "✅ विनंती जमा झाली." }
                    }) { Text("विनंती पाठवा") }
                }
            }
        }
        items(mine, key = { it.id }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.text)
                    Text(it.date, fontSize = 11.sp)
                    Text(if (it.status == 0) "प्रलंबित" else "पूर्ण", color = if (it.status == 0) Amber else Green, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

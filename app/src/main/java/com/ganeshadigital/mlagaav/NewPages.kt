package com.ganeshadigital.mlagaav

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
//  Catalog: services with official links
// =====================================================================
data class Svc(
    val title: String, val desc: String = "", val url: String = "",
    val kind: String = "link", val key: String = "", val docs: List<String> = emptyList()
)

val AGRI_DOCS = listOf(
    "आधार कार्ड", "7/12 व 8-अ उतारा", "बँक पासबुक (आधार लिंक)", "पासपोर्ट साईज फोटो",
    "मोबाईल नंबर", "जात प्रमाणपत्र (लागू असल्यास)"
)
val HOUSE_DOCS = listOf(
    "आधार कार्ड", "रेशन कार्ड / जॉब कार्ड", "बँक पासबुक (आधार लिंक)",
    "जागेचा पुरावा (7/12, 8-अ किंवा ग्रामपंचायत उतारा)", "पासपोर्ट साईज फोटो", "मोबाईल नंबर"
)

val AGRI_ITEMS = listOf(
    Svc("कृषी विभागाच्या योजना व अनुदान", "कृषी विभागाच्या विविध योजना व अनुदानासाठी महाडीबीटी पोर्टलवर अर्ज करता येतो.", "https://mahadbt.maharashtra.gov.in"),
    Svc("कृषी अवजारे व यंत्रसामग्री (ट्रॅक्टर, पॉवर टिलर इ.)", "कृषी यांत्रिकीकरण योजनेत अनुदानावर अवजारे मिळतात. अर्ज महाडीबीटी पोर्टलवर.", "https://mahadbt.maharashtra.gov.in"),
    Svc("ठिबक / तुषार सिंचन योजना", "प्रधानमंत्री कृषी सिंचन योजना (प्रति थेंब अधिक पीक) अंतर्गत सूक्ष्म सिंचनासाठी अनुदान.", "https://pmksy.gov.in"),
    Svc("बियाणे व निविष्ठा", "बियाणे, खते व निविष्ठा अनुदानासाठी महाडीबीटी पोर्टल.", "https://mahadbt.maharashtra.gov.in"),
    Svc("पीक विमा व शेतकरी प्रशिक्षण", "प्रधानमंत्री पीक विमा योजना – नोंदणी, पीक विमा स्थिती व माहिती.", "https://pmfby.gov.in"),
    Svc("कृषी यंत्रे भाड्याने उपलब्ध असल्यास माहिती", "गावात उपलब्ध यंत्रे, मालक व भाडे – ग्रामपंचायतीने भरलेली माहिती.", kind = "note", key = "agri_rent"),
    Svc("अर्जांची स्थिती / आवश्यक कागदपत्रे", "", kind = "docs", docs = AGRI_DOCS)
)

val HOUSE_ITEMS = listOf(
    Svc("प्रधानमंत्री आवास योजना – ग्रामीण", "ग्रामीण भागातील पात्र कुटुंबांसाठी घरकुल योजना. अधिकृत पोर्टलवर लाभार्थी माहिती पाहता येते.", "https://pmayg.nic.in"),
    Svc("राज्य शासनाच्या आवास योजना", "महाराष्ट्र शासनाच्या ग्रामविकास विभागाच्या घरकुल योजना (रमाई, शबरी, पारधी, आदिम इ.).", "https://rdd.maharashtra.gov.in"),
    Svc("घरकुल लाभार्थी यादी", "या अॅपमध्ये मंजूर झालेले घरकुल अर्ज खाली दिसतील. अधिकृत यादी पोर्टलवर.", "https://pmayg.nic.in", kind = "list"),
    Svc("मंजूर / प्रलंबित घरकुल", "घरकुल / आवास अर्जांची सध्याची स्थिती.", kind = "counts"),
    Svc("अर्जांची माहिती व स्थिती", "तुमचे केलेले अर्ज आणि त्यांची स्थिती पाहण्यासाठी.", kind = "status"),
    Svc("आवश्यक कागदपत्रे", "", kind = "docs", docs = HOUSE_DOCS)
)

class SchemeCat(val name: String, val icon: Int?, val bg: Color, val fg: Color, val items: List<Svc>)

val SCHEME_CATS = listOf(
    SchemeCat("शेतकरी व कृषी", R.drawable.cat_agri, Color(0xFFE8F5E9), Color(0xFF1B5E20), listOf(
        Svc("पी.एम. किसान सन्मान निधी", "पात्र शेतकरी कुटुंबांना वार्षिक आर्थिक सहाय्य. नोंदणी, e-KYC व लाभार्थी स्थिती.", "https://pmkisan.gov.in"),
        Svc("प्रधानमंत्री पीक विमा योजना", "नैसर्गिक आपत्तीपासून पीक संरक्षण. नोंदणी व स्थिती.", "https://pmfby.gov.in"),
        Svc("महाडीबीटी – कृषी योजना", "कृषी विभागाच्या अनुदान योजनांसाठी एकच अर्ज पोर्टल.", "https://mahadbt.maharashtra.gov.in"),
        Svc("प्रधानमंत्री कृषी सिंचन योजना", "ठिबक / तुषार सिंचनासाठी अनुदान.", "https://pmksy.gov.in")
    )),
    SchemeCat("घरकुल व निवारा", R.drawable.cat_home, Color(0xFFFFF3E0), Color(0xFFE65100), listOf(
        Svc("प्रधानमंत्री आवास योजना – ग्रामीण", "पात्र कुटुंबांसाठी पक्के घर.", "https://pmayg.nic.in"),
        Svc("राज्य शासनाच्या आवास योजना", "ग्रामविकास विभागाच्या घरकुल योजना.", "https://rdd.maharashtra.gov.in")
    )),
    SchemeCat("पाणी व स्वच्छता", R.drawable.cat_water, Color(0xFFE3F2FD), Color(0xFF0D47A1), listOf(
        Svc("जल जीवन मिशन", "प्रत्येक घराला नळाद्वारे शुद्ध पाणी.", "https://jaljeevanmission.gov.in"),
        Svc("स्वच्छ भारत मिशन (ग्रामीण)", "शौचालय, घनकचरा व सांडपाणी व्यवस्थापन.", "https://swachhbharatmission.ddws.gov.in")
    )),
    SchemeCat("महिला व बालकल्याण", R.drawable.cat_women, Color(0xFFFCE4EC), Color(0xFFC2185B), listOf(
        Svc("महिला व बाल विकास विभाग (महाराष्ट्र)", "महिला व बालकल्याणाच्या राज्य योजना.", "https://wcd.maharashtra.gov.in"),
        Svc("मिशन शक्ती", "महिला सुरक्षा, सक्षमीकरण व सहाय्य.", "https://missionshakti.wcd.gov.in"),
        Svc("मुख्यमंत्री माझी लाडकी बहीण योजना", "पात्र महिलांसाठी आर्थिक सहाय्य योजना.", "https://ladakibahin.maharashtra.gov.in")
    )),
    SchemeCat("ज्येष्ठ नागरिक", R.drawable.cat_senior, Color(0xFFEDE7F6), Color(0xFF283593), listOf(
        Svc("संजय गांधी / श्रावणबाळ योजना", "निराधार, वृद्ध व दिव्यांगांसाठी मासिक अर्थसहाय्य (सामाजिक न्याय विभाग).", "https://sjsa.maharashtra.gov.in"),
        Svc("राष्ट्रीय सामाजिक सहाय्य कार्यक्रम (NSAP)", "वृद्धापकाळ, विधवा व दिव्यांग निवृत्तीवेतन.", "https://nsap.nic.in"),
        Svc("आयुष्मान भारत (PM-JAY)", "पात्र कुटुंबांना आरोग्य विमा संरक्षण.", "https://pmjay.gov.in")
    )),
    SchemeCat("दिव्यांग कल्याण", R.drawable.cat_disabled, Color(0xFFE0F2F1), Color(0xFF00695C), listOf(
        Svc("UDID – दिव्यांग ओळखपत्र", "स्वावलंबन कार्ड / दिव्यांग प्रमाणपत्र नोंदणी.", "https://www.swavlambancard.gov.in"),
        Svc("दिव्यांग सशक्तीकरण विभाग", "दिव्यांगांसाठी केंद्र शासनाच्या योजना.", "https://disabilityaffairs.gov.in"),
        Svc("संजय गांधी निराधार योजना (दिव्यांग)", "दिव्यांगांसाठी मासिक अर्थसहाय्य.", "https://sjsa.maharashtra.gov.in")
    )),
    SchemeCat("आणि इतर योजना", null, Color(0xFFE8EAF6), Color(0xFF1A237E), listOf(
        Svc("myScheme – सर्व योजना शोधा", "तुमच्यासाठी पात्र योजना शोधण्याचे केंद्र शासनाचे पोर्टल.", "https://www.myscheme.gov.in"),
        Svc("आपले सरकार (दाखले / सेवा)", "महाराष्ट्र शासनाच्या सेवा व दाखले.", "https://aaplesarkar.mahaonline.gov.in"),
        Svc("ई-श्रम कार्ड", "असंघटित कामगार नोंदणी.", "https://eshram.gov.in"),
        Svc("मनरेगा (रोजगार हमी)", "ग्रामीण रोजगार हमी योजना.", "https://nrega.nic.in"),
        Svc("डिजिलॉकर", "कागदपत्रे डिजिटल स्वरूपात सुरक्षित.", "https://www.digilocker.gov.in")
    ))
)
val SCHEME_CAT_NAMES = SCHEME_CATS.map { it.name }

val SUGG_CATS = listOf(
    "रस्ता / नाली / पाणीपुरवठा", "स्ट्रीट लाईट / वीज समस्या", "स्वच्छता / कचरा व्यवस्थापन",
    "सार्वजनिक ठिकाणांची समस्या", "ग्रामपंचायत सेवांबाबत सूचना"
)

// =====================================================================
//  Shared pieces
// =====================================================================
@Composable
fun PosterHeader(title: String, color: Color, photo: Int?) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (photo != null) Image(
                painterResource(photo), contentDescription = null,
                modifier = Modifier.width(120.dp).height(72.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun FooterChip(text: String, bg: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(12.dp)) {
        Text(text, Modifier.padding(14.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF263238))
    }
}

@Composable
fun SvcDialog(s: AppState, svc: Svc, go: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val fam = s.myFamily()
    var msg by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf(s.notes[svc.key] ?: "") }
    val housing = s.apps.filter { it.schemeTitle.contains("आवास") || it.schemeTitle.contains("घरकुल") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(svc.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (svc.desc.isNotBlank()) Text(svc.desc, fontSize = 13.sp)
                when (svc.kind) {
                    "docs" -> {
                        Text("आवश्यक कागदपत्रे:", fontWeight = FontWeight.Bold)
                        svc.docs.forEach { Text("✔ $it", fontSize = 13.sp) }
                        Text("योजनेनुसार कागदपत्रे बदलू शकतात.", fontSize = 11.sp)
                        OutlinedButton(onClick = { onDismiss(); go("appstatus") }, modifier = Modifier.fillMaxWidth()) { Text("📋 अर्जांची स्थिती पहा") }
                    }
                    "status" -> OutlinedButton(onClick = { onDismiss(); go("appstatus") }, modifier = Modifier.fillMaxWidth()) { Text("📋 अर्जांची स्थिती पहा") }
                    "note" -> {
                        val t = s.notes[svc.key].orEmpty()
                        if (!s.adminMode) {
                            Text(if (t.isBlank()) "ही माहिती ग्रामपंचायतीने अजून भरलेली नाही." else t)
                        } else {
                            Field("माहिती (यंत्र, मालक, भाडे, संपर्क)", noteText, { noteText = it; msg = "" }, lines = 4)
                            Button(onClick = { s.setNote(svc.key, noteText.trim()); msg = "✅ सेव्ह झाले" }, modifier = Modifier.fillMaxWidth()) { Text("सेव्ह") }
                        }
                    }
                    "counts" -> {
                        Text("एकूण अर्ज: ${housing.size}", fontWeight = FontWeight.Bold)
                        Text("प्रलंबित: ${housing.count { it.status == 0 }}", color = Amber)
                        Text("मंजूर: ${housing.count { it.status == 1 }}", color = Green)
                        Text("नामंजूर: ${housing.count { it.status == 2 }}", color = Red)
                    }
                    "list" -> {
                        val ok = housing.filter { it.status == 1 }
                        if (ok.isEmpty()) Text("अजून मंजूर घरकुल लाभार्थी नोंदवलेले नाहीत.")
                        ok.forEach { a ->
                            val f = s.families.find { it.id == a.familyId }
                            Text("✔ ${f?.head ?: "-"} • ${if (f != null) s.villageName(f.villageId) else ""}", fontSize = 13.sp)
                        }
                    }
                }
                if (svc.url.isNotBlank() && svc.kind != "docs" && svc.kind != "status" && svc.kind != "note" && svc.kind != "counts") {
                    Button(onClick = { openUrl(ctx, svc.url) }, modifier = Modifier.fillMaxWidth()) { Text("🌐 अधिकृत वेबसाईट उघडा") }
                }
                if (svc.kind == "link") {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        when {
                            fam == null -> msg = "अर्ज नोंदवण्यासाठी आधी कुटुंब प्रमुख Login करा."
                            fam.status != 1 -> msg = "नोंदणी मंजूर झाल्यावर अर्ज नोंदवता येईल."
                            else -> { s.addApp(SchemeApp(newId(), fam.id, svc.title, "", today(), 0)); msg = "✅ ग्रामपंचायतीकडे नोंद झाली." }
                        }
                    }) { Text("📨 ग्रामपंचायतीकडे अर्ज नोंदवा") }
                    Text("अधिकृत अर्ज वेबसाईटवर करा. येथे नोंद केल्यास ग्रामपंचायतीला तुमची मागणी कळते.", fontSize = 11.sp)
                }
                if (msg.isNotEmpty()) Text(msg, color = if (msg.startsWith("✅")) Green else Red)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद") } }
    )
}

@Composable
fun ServicePage(
    s: AppState, go: (String) -> Unit, title: String, color: Color, photo: Int?,
    list: List<Svc>, footer: String, footerBg: Color
) {
    var sel by remember { mutableStateOf<Svc?>(null) }
    PageColumn {
        item { PosterHeader(title, color, photo) }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    list.forEachIndexed { i, svc ->
                        Row(Modifier.fillMaxWidth().clickable { sel = svc }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("✔", color = color, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(svc.title, Modifier.weight(1f), fontSize = 14.sp)
                            Text("›", color = color, fontSize = 20.sp)
                        }
                        if (i < list.size - 1) HorizontalDivider()
                    }
                }
            }
        }
        item { FooterChip(footer, footerBg) }
    }
    sel?.let { SvcDialog(s, it, go) { sel = null } }
}

// =====================================================================
//  3-dot menu pages
// =====================================================================
@Composable
fun AgriPage(s: AppState, go: (String) -> Unit) = ServicePage(
    s, go, "शेती सेवा / कृषी यंत्रे", Color(0xFF2E7D32), R.drawable.photo_agri, AGRI_ITEMS,
    "🌿 शेती अधिक सक्षम, शेतकरी अधिक समृद्ध !", Color(0xFFFFF59D)
)

@Composable
fun HousingPage(s: AppState, go: (String) -> Unit) = ServicePage(
    s, go, "घरकुल / आवास", Color(0xFF7B1FA2), R.drawable.photo_house, HOUSE_ITEMS,
    "🏠 प्रत्येक कुटुंबाला स्वतःचे घर हेच आमचे ध्येय !", Color(0xFFE1BEE7)
)

// ---------------- गावासाठी सूचना द्या ----------------
@Composable
fun SuggestPage(s: AppState, go: (String) -> Unit) {
    val ctx = LocalContext.current
    val fam = s.myFamily()
    var vid by remember { mutableStateOf(s.activeVillage()?.id) }
    var cat by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var who by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var del by remember { mutableStateOf<Suggestion?>(null) }
    val v = pickVillage(s, vid)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            photo = uri.toString()
        }
    }
    val list = s.suggestions.filter { v == null || it.villageId == v.id }
    PageColumn {
        item { PosterHeader("गावासाठी सूचना द्या", Color(0xFFEF6C00), R.drawable.photo_notice) }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VillageSelector(s, vid) { vid = it }
                    Picker("सूचनेचा प्रकार", SUGG_CATS, cat) { cat = it }
                    Field("सूचना / समस्या *", text, { text = it; msg = "" }, lines = 4)
                    if (fam == null) Field("तुमचे नाव (ऐच्छिक)", who, { who = it })
                    else Text("सूचना देणारे: ${fam.head}", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) { Text(if (photo.isEmpty()) "📷 फोटो जोडा" else "📷 फोटो बदला") }
                        if (photo.isNotEmpty()) {
                            UriImage(photo, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                            TextButton(onClick = { photo = "" }) { Text("✖") }
                        }
                    }
                    if (msg.isNotEmpty()) Text(msg, color = if (msg.startsWith("✅")) Green else Red)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        when {
                            v == null -> msg = "आधी गाव निवडा"
                            text.isBlank() -> msg = "सूचना लिहा"
                            else -> {
                                s.addSuggestion(Suggestion(newId(), v.id, fam?.id ?: 0L, fam?.head ?: who.trim(), cat, text.trim(), photo, today(), 0))
                                text = ""; photo = ""; msg = "✅ सूचना पाठवली. स्थिती खाली दिसेल."
                            }
                        }
                    }) { Text("📤 सूचना पाठवा") }
                }
            }
        }
        item { Text("सूचनांची स्थिती (${list.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(list, key = { it.id }) { g ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(SUGG_CATS.getOrElse(g.category) { "" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF6C00))
                    Text(g.text)
                    if (g.photo.isNotEmpty()) UriImage(g.photo, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)))
                    Text("${g.by.ifBlank { "गावकरी" }} • ${g.date}", fontSize = 11.sp)
                    Text(
                        SUGG_STATUS.mapIndexed { i, n -> if (i == g.status) "● $n" else n }.joinToString("  →  "),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = if (g.status >= 3) Green else Amber
                    )
                    if (s.adminMode) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (g.status < 3) Button(onClick = { s.setSuggestionStatus(g.id, g.status + 1) }) { Text("▶ ${SUGG_STATUS[g.status + 1]}") }
                        TextButton(onClick = { del = g }) { Text("🗑", color = Red) }
                    }
                }
            }
        }
    }
    del?.let { g -> ConfirmDialog("ही सूचना हटवायची?", { s.deleteSuggestion(g.id); del = null }, { del = null }) }
}

// ---------------- गावाची आकडेवारी ----------------
@Composable
private fun StatSection(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
            content()
        }
    }
}

@Composable
fun StatsPage(s: AppState) {
    var vid by remember { mutableStateOf(s.activeVillage()?.id) }
    val v = pickVillage(s, vid)
    PageColumn {
        item { PosterHeader("गावाची आकडेवारी", Color(0xFF1565C0), R.drawable.photo_stats) }
        item { VillageSelector(s, vid) { vid = it } }
        if (v != null) {
            val fams = s.families.filter { it.villageId == v.id && it.status == 1 }
            val ms = fams.flatMap { it.members }
            val i = s.infos[v.id] ?: Info()
            val reg = fams.isNotEmpty()
            val male = if (reg) ms.count { it.gender == 0 } else i.male
            val female = if (reg) ms.count { it.gender == 1 } else i.female
            val other = if (reg) ms.count { it.gender == 2 } else 0
            val citizens = if (reg) ms.size else v.population
            val houses = if (reg) fams.size else v.households
            val farmers = ms.count { it.occupation.contains("शेत") }
            val students = ms.count { it.occupation.contains("विद्यार्थी") || it.occupation.contains("शिक्षण") }
            val ok = s.apps.filter { it.status == 1 && s.families.find { f -> f.id == it.familyId }?.villageId == v.id }
            val houseOk = ok.count { it.schemeTitle.contains("आवास") || it.schemeTitle.contains("घरकुल") }
            item {
                StatSection("👪 एकूण कुटुंबे व नागरिक", Color(0xFF1565C0)) {
                    Text("एकूण कुटुंबे: $houses")
                    Text("एकूण नागरिक: $citizens")
                    if (!reg) Text("(कुटुंब नोंदणी झाल्यावर आकडे आपोआप येतील; सध्या गावाची माहिती दाखवली आहे)", fontSize = 11.sp)
                }
            }
            item {
                StatSection("🚻 महिला / पुरुष संख्या", Color(0xFF1565C0)) {
                    Text("पुरुष: $male"); Text("महिला: $female"); if (other > 0) Text("इतर: $other")
                }
            }
            item {
                StatSection("🎂 वयोगटानुसार माहिती", Color(0xFF1565C0)) {
                    if (!reg) Text("कुटुंब नोंदणीतील सदस्यांच्या वयावरून दिसेल.", fontSize = 12.sp)
                    else {
                        Text("0 – 5 वर्षे: ${ms.count { it.age in 1..5 }}")
                        Text("6 – 17 वर्षे: ${ms.count { it.age in 6..17 }}")
                        Text("18 – 59 वर्षे: ${ms.count { it.age in 18..59 }}")
                        Text("60+ वर्षे: ${ms.count { it.age >= 60 }}")
                        Text("वय नोंद नाही: ${ms.count { it.age == 0 }}", fontSize = 12.sp)
                    }
                }
            }
            item {
                StatSection("🌾 शेतकरी संख्या व जमीन तपशील", Color(0xFF2E7D32)) {
                    Text("शेतकरी (व्यवसाय 'शेती'): $farmers")
                    Text("एकूण क्षेत्र: ${i.areaHa} हे.")
                    Text("शेती क्षेत्र: ${i.agriHa} हे.   वन क्षेत्र: ${i.forestHa} हे.")
                    if (i.crops.isNotBlank()) Text("मुख्य पिके: ${i.crops}")
                }
            }
            item {
                StatSection("🐄 पशुधन, शाळा, विद्यार्थी", Color(0xFFEF6C00)) {
                    val live = s.notes["stats_livestock_${v.id}"].orEmpty()
                    Text("पशुधन: ${live.ifBlank { "माहिती भरलेली नाही" }}")
                    Text("शाळा: ${i.schools}   अंगणवाडी: ${i.anganwadi}")
                    Text("विद्यार्थी (नोंदणीतील): $students")
                    if (s.adminMode) {
                        var t by remember(v.id) { mutableStateOf(live) }
                        Field("पशुधन माहिती (उदा. गाय 120, शेळी 200)", t, { t = it })
                        Button(onClick = { s.setNote("stats_livestock_${v.id}", t.trim()) }) { Text("सेव्ह") }
                    }
                }
            }
            item {
                StatSection("🏠 घरकुल लाभार्थी व इतर योजना लाभार्थी", Color(0xFF7B1FA2)) {
                    Text("घरकुल लाभार्थी (मंजूर अर्ज): $houseOk")
                    Text("इतर योजनांचे मंजूर अर्ज: ${ok.size - houseOk}")
                }
            }
            item {
                StatSection("🏫 गावातील उपलब्ध सुविधा", Color(0xFF00695C)) {
                    Text("नळ / पाणी सुविधा: ${i.water}   वीज: ${i.elec}")
                    Text("आरोग्य केंद्रे: ${i.health}   जलसाठे: ${i.waterBodies}")
                    Text("सिंचन: ${i.irrigation}")
                }
            }
            item { FooterChip("📊 आपल्या गावाची संपूर्ण माहिती एका ठिकाणी !", Color(0xFFBBDEFB)) }
        }
    }
}

// ---------------- सर्व योजना ----------------
@Composable
fun AllSchemesPage(s: AppState, go: (String) -> Unit) {
    val fam = s.myFamily()
    var applyTo by remember { mutableStateOf<Scheme?>(null) }
    PageColumn {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("ग्रामपंचायत नुसार सर्व योजना", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("शासकीय योजनांची माहिती व अर्ज सुविधा", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SCHEME_CATS.chunked(3).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEachIndexed { c, cat ->
                            val idx = r * 3 + c
                            Card(
                                Modifier.weight(1f).fillMaxHeight().clickable { go("cat$idx") },
                                colors = CardDefaults.cardColors(containerColor = cat.bg), shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (cat.icon != null) Image(painterResource(cat.icon), null, Modifier.size(46.dp))
                                    else Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { Text("📚", fontSize = 28.sp) }
                                    Spacer(Modifier.height(6.dp))
                                    Text(cat.name, color = cat.fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item { Text("ग्रामपंचायतीने जोडलेल्या योजना (${s.schemes.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (s.schemes.isEmpty()) item { Text("अजून योजना जोडलेल्या नाहीत. (Admin → योजना व्यवस्थापन)", fontSize = 12.sp) }
        items(s.schemes, key = { it.id }) { sc ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(sc.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(SCHEME_CAT_NAMES.getOrElse(sc.category) { "" }, fontSize = 11.sp, color = Brand)
                    if (sc.desc.isNotBlank()) Text(sc.desc, fontSize = 13.sp)
                    if (sc.eligibility.isNotBlank()) Text("पात्रता: ${sc.eligibility}", fontSize = 12.sp)
                    if (fam == null) TextButton(onClick = { go("login") }) { Text("अर्ज करण्यासाठी Login करा") }
                    else if (fam.status == 1) Button(onClick = { applyTo = sc }) { Text("📨 अर्ज करा") }
                    else Text("नोंदणी मंजूर झाल्यावर अर्ज करता येईल.", fontSize = 12.sp)
                }
            }
        }
    }
    applyTo?.let { sc -> if (fam != null) ApplyDialog(s, fam, sc.title) { applyTo = null } }
}

@Composable
fun CategoryPage(s: AppState, go: (String) -> Unit, idx: Int) {
    val cat = SCHEME_CATS.getOrNull(idx)
    if (cat == null) { Text("पान सापडले नाही"); return }
    val extra = s.schemes.filter { it.category == idx }.map {
        Svc(it.title, it.desc + (if (it.eligibility.isNotBlank()) "\nपात्रता: ${it.eligibility}" else ""))
    }
    ServicePage(s, go, cat.name, cat.fg, null, cat.items + extra, "ℹ️ अधिकृत वेबसाईटवर अर्ज करा किंवा ग्रामपंचायतीकडे नोंद करा.", cat.bg)
}

// =====================================================================
//  Home (as in the poster)
// =====================================================================
@Composable
private fun HomeTile(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.fillMaxWidth().height(112.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0D2B52), textAlign = TextAlign.Center, lineHeight = 15.sp)
        }
    }
}

@Composable
fun CitizenHome(s: AppState, go: (String) -> Unit) {
    val v = s.activeVillage()
    var pick by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF4FC), Color(0xFFF7FBFF)))).verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth().height(168.dp).clickable { if (s.villages.size > 1) pick = true }) {
            Image(painterResource(R.drawable.village_banner), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B2A18)))))
            Column(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("माझे गाव - ${v?.name ?: "गाव निवडा"}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (v != null) Text("ता. ${v.taluka}  |  जि. ${v.district}  |  पिन - ${v.pincode}", color = Color.White, fontSize = 12.sp)
                else Text("Admin मधून गाव जोडा", color = Color.White, fontSize = 12.sp)
            }
            DropdownMenu(expanded = pick, onDismissRequest = { pick = false }) {
                s.villages.forEach { x ->
                    DropdownMenuItem(text = { Text("${x.name} (${x.gp})") }, onClick = { s.setHomeVillage(x.id); pick = false })
                }
            }
        }
        Card(
            Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9FE)), shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeTile(R.drawable.h_family, "माझे कुटुंब", Modifier.weight(1f)) { go("myinfo") }
                    HomeTile(R.drawable.h_gov, "शासकीय\nयोजना", Modifier.weight(1f)) { go("schemes") }
                    HomeTile(R.drawable.h_gp, "ग्रामपंचायत", Modifier.weight(1f)) { go("gp") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeTile(R.drawable.h_map, "गावाची\nमाहिती", Modifier.weight(1f)) { go("info") }
                    HomeTile(R.drawable.h_gear, "सेवा व\nसुविधा", Modifier.weight(1f)) { go("services") }
                    HomeTile(R.drawable.h_doc, "अर्ज / स्थिती", Modifier.weight(1f)) { go("appstatus") }
                }
            }
        }
        if (s.villages.isEmpty()) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                Text(
                    "सुरुवात: खालील 'अधिक' → 🔐 ग्रामपंचायत Admin → 'गावे / समस्या / कामे' मधून पहिले गाव जोडा.",
                    Modifier.padding(14.dp), fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "सर्व माहिती, सर्व सेवा, आता तुमच्या मोबाईलवर !", Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)
        )
        Spacer(Modifier.height(20.dp))
    }
}

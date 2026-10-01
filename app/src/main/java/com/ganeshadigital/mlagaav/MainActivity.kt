package com.ganeshadigital.mlagaav

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots, screen recording and recent-apps preview
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val state = AppState(applicationContext)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Brand, secondary = Green)) {
                Surface(Modifier.fillMaxSize()) {
                    if (unlocked) App(state) else LockScreen(state) { unlocked = true }
                }
            }
        }
    }

    // Auto-lock whenever the app goes to background
    override fun onStop() {
        super.onStop()
        unlocked = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(s: AppState) {
    var tab by remember { mutableIntStateOf(0) }
    var openId by remember { mutableStateOf<Long?>(null) }
    val opened = openId?.let { id -> s.villages.find { it.id == id } }

    if (opened != null) {
        BackHandler { openId = null }
        VillageDetail(s, opened) { openId = null }
        return
    }

    val tabs = listOf("🏠" to "डॅशबोर्ड", "🏘" to "गावे", "⚠️" to "समस्या", "🏗" to "कामे")
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("My Village Data", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand, titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> Dashboard(s) { openId = it }
                1 -> VillagesScreen(s) { openId = it }
                2 -> IssuesScreen(s)
                else -> WorksScreen(s)
            }
        }
    }
}

// ---------------- Dashboard ----------------
@Composable
fun Dashboard(s: AppState, onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    var showBackup by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }
    var restoreMsg by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    if (showPin) ChangePinDialog(s) { showPin = false }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("गावे", "${s.villages.size}", Brand, Modifier.weight(1f))
                StatCard("लोकसंख्या", "${s.villages.sumOf { it.population }}", Green, Modifier.weight(1f))
                StatCard("कुटुंबे", "${s.villages.sumOf { it.households }}", Brand, Modifier.weight(1f))
            }
        }
        item {
            Text("समस्या", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ISSUE_STATUS.forEachIndexed { i, n ->
                    StatCard(n, "${s.issues.count { it.status == i }}", statusColor(i), Modifier.weight(1f))
                }
            }
        }
        item {
            Text("कामे", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WORK_STATUS.forEachIndexed { i, n ->
                    StatCard(n, "${s.works.count { it.status == i }}", statusColor(i), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            StatCard("एकूण निधी (₹ लाख)", "%.2f".format(s.works.sumOf { it.budgetLakh }), Green, Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { shareText(ctx, s.report()) }, modifier = Modifier.weight(1f)) { Text("📤 अहवाल") }
                OutlinedButton(onClick = { showBackup = true }, modifier = Modifier.weight(1f)) { Text("💾 बॅकअप") }
                OutlinedButton(onClick = { showRestore = true }, modifier = Modifier.weight(1f)) { Text("♻️ रिस्टोर") }
            }
        }
        item {
            OutlinedButton(onClick = { showPin = true }, modifier = Modifier.fillMaxWidth()) { Text("🔑 PIN / रिकव्हरी बदला") }
        }
        if (s.villages.isNotEmpty()) {
            item { Text("गावानुसार प्रलंबित समस्या", fontWeight = FontWeight.Bold) }
            items(s.villages.sortedByDescending { v -> s.issues.count { it.villageId == v.id && it.status != 2 } }) { v ->
                val p = s.issues.count { it.villageId == v.id && it.status != 2 }
                Card(Modifier.fillMaxWidth().clickable { onOpen(v.id) }) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(v.name, fontWeight = FontWeight.Medium)
                        Text("$p प्रलंबित", color = if (p > 0) Red else Green)
                    }
                }
            }
        } else {
            item { Text("सुरुवात करण्यासाठी 'गावे' टॅबमध्ये गाव जोडा.") }
        }
    }

    if (showBackup) {
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text("बॅकअप") },
            text = { Text("सर्व डेटा शेअर करा (WhatsApp / Drive) आणि सुरक्षित ठेवा. नवीन फोनवर 'रिस्टोर' मध्ये पेस्ट करा.") },
            confirmButton = { Button(onClick = { shareText(ctx, s.toJson()); showBackup = false }) { Text("शेअर करा") } },
            dismissButton = { TextButton(onClick = { showBackup = false }) { Text("बंद") } }
        )
    }
    if (showRestore) {
        FormDialog("रिस्टोर", { showRestore = false; restoreMsg = "" }, onSave = {
            if (s.fromJson(restoreText.trim())) { showRestore = false; restoreText = ""; restoreMsg = "" }
            else restoreMsg = "चुकीचा बॅकअप डेटा"
        }) {
            Text("बॅकअप मजकूर येथे पेस्ट करा. सध्याचा डेटा बदलला जाईल.")
            Field("बॅकअप डेटा", restoreText, { restoreText = it }, lines = 4)
            if (restoreMsg.isNotEmpty()) Text(restoreMsg, color = Red)
        }
    }
}

// ---------------- Villages ----------------
@Composable
fun VillagesScreen(s: AppState, onOpen: (Long) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (s.villages.isEmpty()) {
            Text("अजून गाव जोडलेले नाही. खालील + बटण दाबा.", Modifier.align(Alignment.Center).padding(24.dp))
        }
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.villages, key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(v.id) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(v.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("ग्रा.पं. ${v.gp}  •  ता. ${v.taluka}  •  जि. ${v.district}", fontSize = 13.sp)
                        Text("${v.state}  •  पिन ${v.pincode}", fontSize = 13.sp)
                        Text("लोकसंख्या ${v.population}  •  कुटुंबे ${v.households}", fontSize = 13.sp)
                        if (v.sarpanch.isNotBlank()) Text("सरपंच: ${v.sarpanch}  ${v.contact}", fontSize = 13.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Brand, contentColor = androidx.compose.ui.graphics.Color.White
        ) { Text("+", fontSize = 28.sp) }
    }
    if (showAdd) VillageDialog(null, s.villages.lastOrNull(), { showAdd = false }) { s.upsertVillage(it); showAdd = false }
}

// ---------------- Issues ----------------
@Composable
fun IssueCard(s: AppState, i: Issue, showVillage: Boolean) {
    var del by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(i.title, fontWeight = FontWeight.Medium)
            Text(
                (if (showVillage) "${s.villageName(i.villageId)}  •  " else "") +
                        "${i.category}  •  प्राधान्य: ${PRIORITY[i.priority]}  •  ${i.date}",
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                StatusChip(ISSUE_STATUS[i.status] + "  (बदला)", i.status) { s.cycleIssue(i.id) }
                TextButton(onClick = { del = true }) { Text("🗑 हटवा", color = Red) }
            }
        }
    }
    if (del) ConfirmDialog("ही समस्या हटवायची?", { s.deleteIssue(i.id); del = false }, { del = false })
}

@Composable
fun IssuesScreen(s: AppState) {
    var filter by remember { mutableIntStateOf(-1) }
    var showAdd by remember { mutableStateOf(false) }
    val list = s.issues.filter { filter == -1 || it.status == filter }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == -1, onClick = { filter = -1 }, label = { Text("सर्व") })
                ISSUE_STATUS.forEachIndexed { i, n ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(n) })
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { IssueCard(s, it, true) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Brand, contentColor = androidx.compose.ui.graphics.Color.White
        ) { Text("+", fontSize = 28.sp) }
    }
    if (showAdd) IssueDialog(s, null) { showAdd = false }
}

// ---------------- Works ----------------
@Composable
fun WorkCard(s: AppState, w: Work, showVillage: Boolean) {
    var del by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(w.title, fontWeight = FontWeight.Medium)
            Text(
                (if (showVillage) "${s.villageName(w.villageId)}  •  " else "") +
                        "₹ ${"%.2f".format(w.budgetLakh)} लाख  •  ${w.date}",
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                StatusChip(WORK_STATUS[w.status] + "  (बदला)", w.status) { s.cycleWork(w.id) }
                TextButton(onClick = { del = true }) { Text("🗑 हटवा", color = Red) }
            }
        }
    }
    if (del) ConfirmDialog("हे काम हटवायचे?", { s.deleteWork(w.id); del = false }, { del = false })
}

@Composable
fun WorksScreen(s: AppState) {
    var filter by remember { mutableIntStateOf(-1) }
    var showAdd by remember { mutableStateOf(false) }
    val list = s.works.filter { filter == -1 || it.status == filter }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == -1, onClick = { filter = -1 }, label = { Text("सर्व") })
                WORK_STATUS.forEachIndexed { i, n ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(n) })
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { WorkCard(s, it, true) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Brand, contentColor = androidx.compose.ui.graphics.Color.White
        ) { Text("+", fontSize = 28.sp) }
    }
    if (showAdd) WorkDialog(s, null) { showAdd = false }
}

// ---------------- Village detail ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageDetail(s: AppState, v: Village, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var edit by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var addIssue by remember { mutableStateOf(false) }
    var addWork by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(v.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←", fontSize = 22.sp, color = androidx.compose.ui.graphics.Color.White) } },
                actions = {
                    TextButton(onClick = { edit = true }) { Text("✏️", fontSize = 18.sp) }
                    TextButton(onClick = { delete = true }) { Text("🗑", fontSize = 18.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand, titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("राज्य: ${v.state}   जिल्हा: ${v.district}")
                        Text("तालुका: ${v.taluka}   ग्रामपंचायत: ${v.gp}")
                        Text("पिन कोड: ${v.pincode}")
                        Text("लोकसंख्या: ${v.population}   कुटुंबे: ${v.households}")
                        Text("सरपंच: ${v.sarpanch}")
                        Text("संपर्क: ${v.contact}")
                        if (v.notes.isNotBlank()) Text("टिपणी: ${v.notes}")
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = {
                            val vi = s.issues.filter { it.villageId == v.id }
                            val vw = s.works.filter { it.villageId == v.id }
                            val t = buildString {
                                appendLine("📍 *${v.name}* – ${today()}")
                                appendLine("लोकसंख्या ${v.population}, कुटुंबे ${v.households}")
                                appendLine("समस्या: प्रलंबित ${vi.count { it.status == 0 }}, प्रगतीत ${vi.count { it.status == 1 }}, सोडवल्या ${vi.count { it.status == 2 }}")
                                vi.filter { it.status != 2 }.forEach { appendLine("  • ${it.title} (${ISSUE_STATUS[it.status]})") }
                                appendLine("कामे: ${vw.size}, निधी ₹ ${"%.2f".format(vw.sumOf { it.budgetLakh })} लाख")
                                vw.forEach { appendLine("  • ${it.title} (${WORK_STATUS[it.status]})") }
                            }
                            shareText(ctx, t)
                        }) { Text("📤 या गावाचा अहवाल शेअर करा") }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("समस्या", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { addIssue = true }) { Text("+ समस्या जोडा") }
                }
            }
            items(s.issues.filter { it.villageId == v.id }, key = { it.id }) { IssueCard(s, it, false) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("कामे", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { addWork = true }) { Text("+ काम जोडा") }
                }
            }
            items(s.works.filter { it.villageId == v.id }, key = { it.id }) { WorkCard(s, it, false) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (edit) VillageDialog(v, null, { edit = false }) { s.upsertVillage(it); edit = false }
    if (delete) ConfirmDialog("${v.name} आणि त्याच्या सर्व समस्या/कामे हटवायची?", { s.deleteVillage(v.id); delete = false; onBack() }, { delete = false })
    if (addIssue) IssueDialog(s, v.id) { addIssue = false }
    if (addWork) WorkDialog(s, v.id) { addWork = false }
}

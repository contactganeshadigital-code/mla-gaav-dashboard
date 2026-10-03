package com.ganeshadigital.mlagaav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class NavEntry(val key: String, val icon: String, val label: String)

val MENU_MAIN = listOf(
    NavEntry("home", "🏠", "मुख्यपृष्ठ"),
    NavEntry("myvillage", "🏡", "माझे गाव"),
    NavEntry("info", "ℹ️", "गावाची माहिती"),
    NavEntry("families", "👨‍👩‍👧", "कुटुंब नोंदणी"),
    NavEntry("gp", "🏛", "ग्रामपंचायत"),
    NavEntry("schemes", "📜", "ग्रामपंचायत योजना"),
    NavEntry("apply", "📨", "ऑनलाईन अर्ज"),
    NavEntry("news", "📢", "सूचना / बातम्या"),
    NavEntry("contacts", "📞", "महत्त्वाचे संपर्क"),
    NavEntry("services", "🛠", "गावातील सेवा"),
    NavEntry("map", "📍", "गावाचा नकाशा"),
    NavEntry("gallery", "🖼", "गावाचे फोटो / गॅलरी"),
    NavEntry("help", "❓", "मदत / FAQ"),
    NavEntry("contactus", "☎️", "संपर्क"),
    NavEntry("agri", "🚜", "शेती सेवा / कृषी यंत्रे"),
    NavEntry("suggest", "💡", "गावासाठी सूचना द्या"),
    NavEntry("stats", "📊", "गावाची आकडेवारी"),
    NavEntry("housing", "🏠", "घरकुल / आवास")
)

val MENU_LOGIN = listOf(
    NavEntry("login", "👤", "कुटुंब प्रमुख Login"),
    NavEntry("register", "📝", "नवीन कुटुंब नोंदणी"),
    NavEntry("myinfo", "🪪", "माझी माहिती"),
    NavEntry("members", "👥", "कुटुंबातील सदस्य"),
    NavEntry("appstatus", "📋", "अर्जांची स्थिती"),
    NavEntry("request", "🛠", "दुरुस्ती विनंती"),
    NavEntry("logout", "🚪", "Logout")
)

val MENU_ADMIN = listOf(
    NavEntry("a_dash", "📊", "Dashboard"),
    NavEntry("a_legacy", "🏘", "गावे / समस्या / कामे"),
    NavEntry("a_sugg", "💡", "गावकऱ्यांच्या सूचना"),
    NavEntry("a_verify", "✅", "कुटुंब नोंदणी तपासणी"),
    NavEntry("a_members", "👥", "सदस्य माहिती"),
    NavEntry("a_status", "📋", "मंजूर / प्रलंबित नोंदी"),
    NavEntry("a_schemes", "📜", "योजना व्यवस्थापन"),
    NavEntry("a_apps", "📨", "ऑनलाइन अर्ज"),
    NavEntry("a_notices", "📢", "सूचना व्यवस्थापन"),
    NavEntry("a_reports", "📈", "अहवाल / Reports"),
    NavEntry("a_users", "🔑", "User Management"),
    NavEntry("a_backup", "💾", "Data Backup"),
    NavEntry("a_settings", "⚙️", "Settings")
)

private val DOT_MENU = listOf(
    "🚜  शेती सेवा / कृषी यंत्रे" to "agri",
    "💡  गावासाठी सूचना द्या" to "suggest",
    "📊  गावाची आकडेवारी" to "stats",
    "🏠  घरकुल / आवास" to "housing",
    "🏛  ग्रामपंचायत नुसार सर्व योजना" to "schemes"
)

@Composable
private fun LegacyAdmin(s: AppState, onOpen: (Long) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏠 डॅशबोर्ड", "🏘 गावे", "⚠️ समस्या", "🏗 कामे")
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontSize = 13.sp) }) }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> Dashboard(s) { onOpen(it) }
                1 -> VillagesScreen(s) { onOpen(it) }
                2 -> IssuesScreen(s)
                else -> WorksScreen(s)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(s: AppState) {
    var page by remember { mutableStateOf("home") }
    var openId by remember { mutableStateOf<Long?>(null) }
    var adminAsk by remember { mutableStateOf<String?>(null) }
    var dots by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val opened = openId?.let { id -> s.villages.find { it.id == id } }

    fun go(p: String) {
        when {
            p == "logout" -> { s.logoutAll(); page = "home"; openId = null }
            p.startsWith("a_") && !s.adminMode -> adminAsk = p
            else -> { page = p; openId = null }
        }
    }
    fun menuTap(p: String) { scope.launch { drawer.close() }; go(p) }

    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(!drawer.isOpen && page != "home") { page = "home" }
    BackHandler(!drawer.isOpen && page.startsWith("cat")) { page = "schemes" }
    BackHandler(!drawer.isOpen && page == "a_legacy" && opened != null) { openId = null }

    val title = when {
        page.startsWith("cat") -> SCHEME_CAT_NAMES.getOrElse(page.removePrefix("cat").toIntOrNull() ?: -1) { "योजना" }
        page == "schemes" -> "ग्रामपंचायत नुसार सर्व योजना"
        else -> (MENU_MAIN + MENU_LOGIN + MENU_ADMIN).find { it.key == page }?.label ?: "My Village Data"
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("My Village Data", fontWeight = FontWeight.Bold, color = Brand, fontSize = 17.sp)
                            val fam = s.myFamily()
                            Text(
                                when {
                                    s.adminMode -> "🔐 ग्रामपंचायत Admin"
                                    fam != null -> "👤 ${fam.head}"
                                    else -> "पाहुणा (Login नाही)"
                                }, fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider()
                    DrawerGroup("मुख्य मेनू", MENU_MAIN, page, { menuTap(it) })
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    DrawerGroup("👤 Login / Admin Menu", MENU_LOGIN, page, { menuTap(it) })
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    DrawerGroup("🔐 ग्रामपंचायत Admin", MENU_ADMIN, page, { menuTap(it) })
                }
            }
        }
    ) {
        if (page == "a_legacy" && opened != null) {
            VillageDetail(s, opened) { openId = null }
        } else {
            val isHome = page == "home"
            val barBg = if (isHome) Color.White else Brand
            val barFg = if (isHome) Color(0xFF0D2B52) else Color.White
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            TextButton(onClick = { scope.launch { drawer.open() } }) {
                                Text("☰", fontSize = 24.sp, color = barFg)
                            }
                        },
                        title = {
                            if (isHome) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(color = Color(0xFF0B4DA2))) { append("My Village ") }
                                            withStyle(SpanStyle(color = Color(0xFF2E7D32))) { append("Data") }
                                        },
                                        fontWeight = FontWeight.ExtraBold, fontSize = 22.sp
                                    )
                                }
                            } else Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        },
                        actions = {
                            Box {
                                TextButton(onClick = { dots = true }) { Text("⋮", fontSize = 28.sp, color = if (isHome) Color(0xFF0B4DA2) else Color.White, fontWeight = FontWeight.Bold) }
                                DropdownMenu(expanded = dots, onDismissRequest = { dots = false }) {
                                    DOT_MENU.forEach { (label, key) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = { dots = false; go(key) })
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = barBg, titleContentColor = barFg)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(selected = page == "home", onClick = { go("home") },
                            icon = { Text("🏠", fontSize = 20.sp) }, label = { Text("Home", fontSize = 11.sp) })
                        NavigationBarItem(selected = page == "schemes" || page.startsWith("cat"), onClick = { go("schemes") },
                            icon = { Text("📄", fontSize = 20.sp) }, label = { Text("योजना", fontSize = 11.sp) })
                        NavigationBarItem(selected = page == "news", onClick = { go("news") },
                            icon = { Text("🔔", fontSize = 20.sp) }, label = { Text("सूचना", fontSize = 11.sp) })
                        NavigationBarItem(selected = false, onClick = { scope.launch { drawer.open() } },
                            icon = { Text("☰", fontSize = 20.sp) }, label = { Text("अधिक", fontSize = 11.sp) })
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    val nav: (String) -> Unit = { go(it) }
                    when {
                        page == "home" -> CitizenHome(s, nav)
                        page == "myvillage" -> MyVillagePage(s)
                        page == "info" -> VillageInfoPage(s)
                        page == "families" -> FamiliesPage(s, nav)
                        page == "gp" -> GpPage(s)
                        page == "schemes" -> AllSchemesPage(s, nav)
                        page.startsWith("cat") -> CategoryPage(s, nav, page.removePrefix("cat").toIntOrNull() ?: -1)
                        page == "apply" -> ApplyPage(s, nav)
                        page == "news" -> NewsPage(s)
                        page == "contacts" -> ContactsPage(s)
                        page == "services" -> ServicesPage(s)
                        page == "map" -> MapPage(s)
                        page == "gallery" -> GalleryPage(s)
                        page == "help" -> HelpPage()
                        page == "contactus" -> ContactUsPage(s)
                        page == "agri" -> AgriPage(s, nav)
                        page == "suggest" -> SuggestPage(s, nav)
                        page == "stats" -> StatsPage(s)
                        page == "housing" -> HousingPage(s, nav)
                        page == "login" -> LoginPage(s, nav)
                        page == "register" -> RegisterPage(s, nav)
                        page == "myinfo" -> MyInfoPage(s, nav)
                        page == "members" -> MembersPage(s, nav)
                        page == "appstatus" -> AppStatusPage(s, nav)
                        page == "request" -> RequestPage(s, nav)
                        !s.adminMode -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("हे पान पाहण्यासाठी ग्रामपंचायत Admin Login आवश्यक आहे.")
                        }
                        page == "a_dash" -> AdminDashPage(s, nav)
                        page == "a_legacy" -> LegacyAdmin(s) { openId = it }
                        page == "a_sugg" -> SuggestPage(s, nav)
                        page == "a_verify" -> AdminVerifyPage(s)
                        page == "a_members" -> AdminMembersPage(s)
                        page == "a_status" -> AdminStatusPage(s)
                        page == "a_schemes" -> AdminSchemesPage(s)
                        page == "a_apps" -> AdminAppsPage(s)
                        page == "a_notices" -> AdminNoticesPage(s)
                        page == "a_reports" -> AdminReportsPage(s)
                        page == "a_users" -> AdminUsersPage(s)
                        page == "a_backup" -> AdminBackupPage(s)
                        page == "a_settings" -> AdminSettingsPage(s, nav)
                        else -> Text("पान सापडले नाही")
                    }
                }
            }
        }
    }

    adminAsk?.let { target ->
        AdminLoginDialog(s, onDismiss = { adminAsk = null }, onOk = { page = target; openId = null; adminAsk = null })
    }
}

@Composable
private fun DrawerGroup(title: String, items: List<NavEntry>, current: String, onTap: (String) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, color = Brand, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp))
    items.forEach { m ->
        NavigationDrawerItem(
            label = { Text(m.label) },
            icon = { Text(m.icon, fontSize = 18.sp) },
            selected = current == m.key,
            onClick = { onTap(m.key) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

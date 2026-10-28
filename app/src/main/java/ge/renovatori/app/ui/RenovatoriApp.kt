package ge.renovatori.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ge.renovatori.app.admin.ExpenseAddScreen
import ge.renovatori.app.admin.ExpenseDetailScreen
import ge.renovatori.app.admin.ExpenseEntryScreen
import ge.renovatori.app.admin.ExpenseListScreen
import ge.renovatori.app.admin.PaidAmountScreen
import ge.renovatori.app.admin.ProjectHistoryFormScreen
import ge.renovatori.app.admin.StaffOnly
import ge.renovatori.app.auth.AuthSession
import ge.renovatori.app.auth.AuthState
import ge.renovatori.app.auth.LoginScreen
import ge.renovatori.app.auth.PasswordChangeScreen
import ge.renovatori.app.auth.ProfileEditScreen
import ge.renovatori.app.auth.ProfileScreen
import ge.renovatori.app.auth.SignupScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ge.renovatori.app.blog.BlogDetailScreen
import ge.renovatori.app.blog.BlogListScreen
import ge.renovatori.app.calculator.CalculatorScreen
import ge.renovatori.app.consultation.ConsultationScreen
import ge.renovatori.app.info.AboutScreen
import ge.renovatori.app.info.ContactScreen
import ge.renovatori.app.info.PricesScreen
import ge.renovatori.app.info.StaticPageScreen
import ge.renovatori.app.info.cookiesPolicy
import ge.renovatori.app.info.privacyPolicy
import ge.renovatori.app.project.ProjectDetailScreen
import ge.renovatori.app.project.ProjectListScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ge.renovatori.app.BuildConfig
import ge.renovatori.app.R
import ge.renovatori.app.faq.FaqScreen
import ge.renovatori.app.home.HomeScreen
import kotlinx.coroutines.launch
import java.time.Year

/** Drawer destinations in the web header's menu order (`header.component.pug`). */
/** [iconRes] (a web asset) is used when no Material core icon fits, e.g. the calculator. */
private enum class Destination(val route: String, val label: String, val icon: ImageVector? = null, @param:DrawableRes val iconRes: Int? = null) {
    Home(Routes.HOME, "მთავარი", Icons.Default.Home),
    Consultation(Routes.CONSULTATION, "კონსულტაცია", Icons.Default.DateRange),
    Calculator(Routes.CALCULATOR, "კალკულატორი", iconRes = R.drawable.ic_service_calculator),
    About(Routes.ABOUT, "ჩვენ შესახებ", Icons.Default.Face),
    Prices(Routes.PRICES, "ფასები", Icons.Default.ShoppingCart),
    Blog(Routes.BLOG_LIST, "ბლოგი", Icons.Default.Edit),
    Projects(Routes.PROJECT_LIST, "პროექტები", Icons.Default.Build),
    Faq(Routes.FAQ, "ხ.დ.კ.", Icons.Default.Info),
    Contact(Routes.CONTACT, "კონტაქტი", Icons.Default.Call)
}

/** Footer-only pages, listed under the main menu. */
private val footerPages = listOf(
    Routes.COOKIES_POLICY to "ქუქის პოლიტიკა",
    Routes.PRIVACY_POLICY to "კონფიდენციალურობა"
)

/** Top bar titles of routes that are not drawer destinations. */
private val secondaryTitles = mapOf(
    Routes.BLOG_DETAIL to "ბლოგი",
    Routes.PROJECT_DETAIL to "პროექტი",
    Routes.COOKIES_POLICY to "ქუქის პოლიტიკა",
    Routes.PRIVACY_POLICY to "კონფიდენციალურობა",
    Routes.LOGIN to "ავტორიზაცია",
    Routes.SIGNUP to "რეგისტრაცია",
    Routes.PROFILE to "პროფილი",
    Routes.ADMIN_EXPENSES to "ხარჯები",
    Routes.ADMIN_EXPENSE_ADD to "პროექტის დამატება",
    Routes.ADMIN_EXPENSE_DETAIL to "ხარჯების დეტალები",
    Routes.ADMIN_EXPENSE_ENTRY_ADD to "ხარჯის დამატება",
    Routes.ADMIN_EXPENSE_ENTRY_EDIT to "ხარჯის რედაქტირება",
    Routes.ADMIN_EXPENSE_PAID_ADD to "ჩარიცხვის დამატება",
    Routes.PROJECT_HISTORY_ADD to "ისტორიის დამატება",
    Routes.PROJECT_HISTORY_EDIT to "ისტორიის რედაქტირება",
    Routes.PROFILE_EDIT to "პროფილის რედაქტირება",
    Routes.PASSWORD_CHANGE to "პაროლის შეცვლა"
)

/** Detail pages show a back arrow instead of the menu (web breadcrumb). */
private val detailRoutes = setOf(
    Routes.BLOG_DETAIL, Routes.PROJECT_DETAIL,
    Routes.ADMIN_EXPENSE_ADD, Routes.ADMIN_EXPENSE_DETAIL, Routes.ADMIN_EXPENSE_ENTRY_ADD, Routes.ADMIN_EXPENSE_ENTRY_EDIT, Routes.ADMIN_EXPENSE_PAID_ADD,
    Routes.PROJECT_HISTORY_ADD, Routes.PROJECT_HISTORY_EDIT, Routes.PROFILE_EDIT, Routes.PASSWORD_CHANGE
)

private val topLevelRoutes =
    Destination.entries.map { it.route }.toSet() + footerPages.map { it.first } + setOf(Routes.LOGIN, Routes.SIGNUP, Routes.PROFILE, Routes.ADMIN_EXPENSES)

/** App shell: navigation drawer (the web `navbar-collapse` menu) + top app bar + NavHost. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenovatoriApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = Destination.entries.firstOrNull { it.route == currentRoute }
    val authState by AuthSession.state.collectAsStateWithLifecycle()

    val navigator = remember(navController) {
        object : AppNavigator {
            override fun navigate(route: String, replaceCurrent: Boolean) {
                val currentId = navController.currentBackStackEntry?.destination?.id
                if (route in topLevelRoutes) {
                    // Menu-level pages replace the stack like switching sections; each keeps its scroll/state.
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    navController.navigate(route) {
                        launchSingleTop = true
                        // A finished form gives way to its result instead of staying in the back stack.
                        if (replaceCurrent && currentId != null) popUpTo(currentId) { inclusive = true }
                    }
                }
            }

            override fun back() {
                navController.popBackStack()
            }
        }
    }
    val uriHandler = AppUriHandler(navigator, LocalUriHandler.current)
    val openRoute: (String) -> Unit = { route ->
        scope.launch { drawerState.close() }
        navigator.navigate(route)
    }

    CompositionLocalProvider(LocalAppNavigator provides navigator, LocalUriHandler provides uriHandler) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        DrawerHeader()
                        AccountSection(authState, currentRoute, openRoute)
                        AdminSection(authState, currentRoute, openRoute)
                        Destination.entries.forEach { destination ->
                            NavigationDrawerItem(
                                label = { Text(destination.label, fontWeight = FontWeight.Bold) },
                                icon = {
                                    destination.icon?.let { Icon(it, contentDescription = null) }
                                    destination.iconRes?.let { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                },
                                selected = destination == current,
                                onClick = { openRoute(destination.route) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 28.dp))
                        CompanyContacts()
                        HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 28.dp))
                        footerPages.forEach { (route, label) ->
                            NavigationDrawerItem(
                                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                selected = route == currentRoute,
                                onClick = { openRoute(route) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        AppInfo()
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                current?.takeIf { it != Destination.Home }?.label
                                    ?: secondaryTitles[currentRoute]
                                    ?: stringResource(R.string.app_name)
                            )
                        },
                        navigationIcon = {
                            if (currentRoute in detailRoutes) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "უკან")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "მენიუ")
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Routes.HOME) { HomeScreen() }
                    composable(Routes.CONSULTATION) { ConsultationScreen() }
                    composable(Routes.CALCULATOR) { CalculatorScreen() }
                    composable(Routes.ABOUT) { AboutScreen() }
                    composable(Routes.PRICES) { PricesScreen() }
                    composable(Routes.BLOG_LIST) { BlogListScreen() }
                    composable(Routes.BLOG_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        BlogDetailScreen(entry.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.PROJECT_LIST) { ProjectListScreen() }
                    composable(Routes.PROJECT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        ProjectDetailScreen(entry.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.FAQ) { FaqScreen() }
                    composable(Routes.CONTACT) { ContactScreen() }
                    composable(Routes.COOKIES_POLICY) { StaticPageScreen(cookiesPolicy) }
                    composable(Routes.PRIVACY_POLICY) { StaticPageScreen(privacyPolicy) }
                    composable(Routes.LOGIN) {
                        // Signing in (here or elsewhere) moves on to the profile, like the web redirect.
                        if (authState is AuthState.Authenticated) {
                            LaunchedEffect(Unit) { navigator.navigate(Routes.PROFILE) }
                        } else {
                            LoginScreen()
                        }
                    }
                    composable(Routes.SIGNUP) {
                        if (authState is AuthState.Authenticated) {
                            LaunchedEffect(Unit) { navigator.navigate(Routes.PROFILE) }
                        } else {
                            SignupScreen()
                        }
                    }
                    composable(Routes.ADMIN_EXPENSES) { StaffOnly { user -> ExpenseListScreen(isAdmin = user.isAdmin) } }
                    composable(Routes.ADMIN_EXPENSE_ADD) {
                        StaffOnly { user ->
                            // Moderators can only view (web: admins-only form; server: 403).
                            if (user.isAdmin) ExpenseAddScreen() else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(Routes.ADMIN_EXPENSE_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        StaffOnly { ExpenseDetailScreen(entry.arguments?.getString("id").orEmpty()) }
                    }
                    composable(Routes.ADMIN_EXPENSE_ENTRY_ADD, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        StaffOnly { user ->
                            if (user.isAdmin) ExpenseEntryScreen(entry.arguments?.getString("id").orEmpty(), entryId = null)
                            else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(
                        Routes.ADMIN_EXPENSE_ENTRY_EDIT,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("entryId") { type = NavType.StringType })
                    ) { entry ->
                        StaffOnly { user ->
                            if (user.isAdmin) ExpenseEntryScreen(entry.arguments?.getString("id").orEmpty(), entry.arguments?.getString("entryId"))
                            else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(Routes.ADMIN_EXPENSE_PAID_ADD, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        StaffOnly { user ->
                            if (user.isAdmin) PaidAmountScreen(entry.arguments?.getString("id").orEmpty())
                            else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(Routes.PROJECT_HISTORY_ADD, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        StaffOnly { user ->
                            if (user.isAdmin) ProjectHistoryFormScreen(entry.arguments?.getString("id").orEmpty(), historyId = null)
                            else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(
                        Routes.PROJECT_HISTORY_EDIT,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("historyId") { type = NavType.StringType })
                    ) { entry ->
                        StaffOnly { user ->
                            if (user.isAdmin) ProjectHistoryFormScreen(entry.arguments?.getString("id").orEmpty(), entry.arguments?.getString("historyId"))
                            else Text("ამ მოქმედებას მხოლოდ ადმინისტრატორი ასრულებს.", Modifier.padding(24.dp))
                        }
                    }
                    composable(Routes.PROFILE_EDIT) {
                        when (val auth = authState) {
                            is AuthState.Authenticated -> ProfileEditScreen(auth.user)
                            AuthState.Unauthenticated -> LoginScreen()
                        }
                    }
                    composable(Routes.PASSWORD_CHANGE) {
                        when (authState) {
                            is AuthState.Authenticated -> PasswordChangeScreen()
                            AuthState.Unauthenticated -> LoginScreen()
                        }
                    }
                    composable(Routes.PROFILE) {
                        // Profile needs a session; without one it shows the login form (web: redirect to /login).
                        when (val auth = authState) {
                            is AuthState.Authenticated -> ProfileScreen(
                                auth.user,
                                onLoggedOut = { navigator.navigate(Routes.HOME) },
                                onEdit = { navigator.navigate(Routes.PROFILE_EDIT) },
                                onChangePassword = { navigator.navigate(Routes.PASSWORD_CHANGE) }
                            )
                            AuthState.Unauthenticated -> LoginScreen()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Web header user area: signed out shows "შესვლა"; signed in shows the greeting bar (green when
 * the e-mail is verified, red otherwise) with the profile link.
 */
@Composable
private fun AccountSection(authState: AuthState, currentRoute: String?, openRoute: (String) -> Unit) {
    when (authState) {
        AuthState.Unauthenticated -> NavigationDrawerItem(
            label = { Text("შესვლა", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            selected = currentRoute == Routes.LOGIN || currentRoute == Routes.SIGNUP,
            onClick = { openRoute(Routes.LOGIN) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        is AuthState.Authenticated -> {
            val user = authState.user
            Surface(
                color = if (user.isVerified) Color(0xFFD1E7DD) else Color(0xFFF8D7DA),
                contentColor = Color(0xFF212529),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("მოგესალმებით, ${user.firstName}", fontWeight = FontWeight.Bold)
                    // Status in words too, not only the background color.
                    if (!user.isVerified) {
                        Text("ელ-ფოსტა არ არის დადასტურებული", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            NavigationDrawerItem(
                label = { Text("პროფილი", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                selected = currentRoute == Routes.PROFILE,
                onClick = { openRoute(Routes.PROFILE) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
}

/** Web admin menu (header, admins and moderators): "💸 ხარჯები" for now. */
@Composable
private fun AdminSection(authState: AuthState, currentRoute: String?, openRoute: (String) -> Unit) {
    val user = (authState as? AuthState.Authenticated)?.user ?: return
    // Only staff see the admin menu; the server checks the role again on every request.
    if (!user.isAdmin && !user.isModerator) return
    Text(
        "ადმინისტრატორის მენიუ",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
    NavigationDrawerItem(
        label = { Text("💸 ხარჯები", fontWeight = FontWeight.Bold) },
        selected = currentRoute?.startsWith("admin/") == true,
        onClick = { openRoute(Routes.ADMIN_EXPENSES) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
}

/** Logo + name, like the web `navbar-brand`. */
@Composable
private fun DrawerHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** Phone, e-mail, working hours and social links from the web header top bar. */
@Composable
private fun CompanyContacts() {
    val uriHandler = LocalUriHandler.current

    ContactItem(R.drawable.ic_phone, CompanyInfo.PHONE_DISPLAY) { uriHandler.openUri(CompanyInfo.PHONE_URI) }
    ContactItem(R.drawable.ic_envelope, CompanyInfo.EMAIL) { uriHandler.openUri(CompanyInfo.EMAIL_URI) }
    ContactItem(R.drawable.ic_clock, "${CompanyInfo.WORKING_DAYS} ${CompanyInfo.WORKING_HOURS}", onClick = null)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SocialButton(R.drawable.ic_facebook, "ჩვენი FB გვერდი") { uriHandler.openUri(CompanyInfo.FACEBOOK_URL) }
        SocialButton(R.drawable.ic_instagram, "ჩვენი Instagram გვერდი") { uriHandler.openUri(CompanyInfo.INSTAGRAM_URL) }
        SocialButton(R.drawable.ic_tiktok, "ჩვენი tiktok გვერდი") { uriHandler.openUri(CompanyInfo.TIKTOK_URL) }
    }
}

@Composable
private fun ContactItem(@DrawableRes icon: Int, text: String, onClick: (() -> Unit)?) {
    // Phone and e-mail open the dialer / mail app; working hours are plain text.
    if (onClick != null) {
        NavigationDrawerItem(
            label = { Text(text, style = MaterialTheme.typography.bodyMedium) },
            icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp)) },
            selected = false,
            onClick = onClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    } else {
        ListItem(
            headlineContent = { Text(text, style = MaterialTheme.typography.bodyMedium) },
            leadingContent = {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun SocialButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(20.dp))
    }
}

/** Web footer lines: support credit, copyright and version. */
@Composable
private fun AppInfo() {
    val uriHandler = LocalUriHandler.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 24.dp)
    ) {
        Text(
            "პროგრამულ მხარდაჭერას უზრუნველყოფს ${CompanyInfo.DEVELOPER_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(role = Role.Button) { uriHandler.openUri(CompanyInfo.DEVELOPER_URL) }
                .padding(vertical = 4.dp)
        )
        Text("©Renovatori.ge ${Year.now().value} წელი", style = MaterialTheme.typography.bodySmall, color = secondary)
        Text("ვერსია ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = secondary)
        Spacer(Modifier.height(4.dp))
    }
}

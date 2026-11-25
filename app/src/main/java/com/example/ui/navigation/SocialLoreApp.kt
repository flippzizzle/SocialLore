package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.auth.authStateFlow
import com.example.data.model.AppLockType
import com.example.data.model.MemberStatus
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.data.repository.AccountRepository
import com.example.data.repository.LoreRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialFeedRepository
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.ErrorLoreState
import com.example.ui.components.LoadingLoreState
import com.example.ui.components.LoreThemedBackground
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.ApprovalGateScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterDetailScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.LoreBookScreen
import com.example.ui.screens.LyricsLibraryScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PhotoAlbumsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuoteBookScreen
import com.example.ui.screens.RelationshipMapScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalLoreTheme
import com.example.ui.theme.SocialLoreTheme
import com.example.ui.theme.ThemeLibrary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class NavSectionItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun SocialLoreApp() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val firebaseUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (firebaseUser == null) {
        SocialLoreTheme {
            AuthScreen(onAuthSuccess = {})
        }
        return
    }

    val uid = firebaseUser!!.uid
    val context = LocalContext.current

    key(uid) {
        val db = remember(uid) {
            FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        }
        val factory = remember(uid, db) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val accountRepo = AccountRepository(db)
                    val feedRepo = SocialFeedRepository(db, accountRepo)
                    val loreRepo = LoreRepository(db, feedRepo)
                    val msgRepo = MessagingRepository(db, accountRepo)
                    return SocialLoreViewModel(
                        accountRepo = accountRepo,
                        feedRepo = feedRepo,
                        loreRepo = loreRepo,
                        msgRepo = msgRepo,
                        currentUserId = uid
                    ) as T
                }
            }
        }
        val viewModel: SocialLoreViewModel = viewModel(key = "vm_$uid", factory = factory)

        val accountState by viewModel.accountState.collectAsStateWithLifecycle()
        val initError by viewModel.initError.collectAsStateWithLifecycle()
        val appUnlocked by viewModel.appUnlocked.collectAsStateWithLifecycle()
        val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()

        val account = (accountState as? UiState.Success)?.data
        val activeThemeSpec = remember(account?.themeId, customThemes) {
            ThemeLibrary.findTheme(account?.themeId, customThemes)
        }

        SocialLoreTheme(
            themeSpec = activeThemeSpec,
            headingFontName = account?.headingFont,
            bodyFontName = account?.bodyFont,
            fontScale = (account?.fontScale ?: 1.0).toFloat(),
            headingWeightInt = account?.headingWeight ?: 700,
            bodyWeightInt = account?.bodyWeight ?: 400
        ) {
            when (val state = accountState) {
                is UiState.Loading -> {
                    LoreThemedBackground {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingLoreState("Loading your Social Lore profile...")
                        }
                    }
                }
                is UiState.Error -> {
                    LoreThemedBackground {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            ErrorLoreState(
                                message = state.message,
                                onRetry = { viewModel.retryInitialization() }
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val acc = state.data
                    if (acc == null || acc.status != MemberStatus.APPROVED.name) {
                        ApprovalGateScreen(
                            account = acc,
                            initError = initError,
                            onRetry = { viewModel.retryInitialization() },
                            onSignOut = {}
                        )
                    } else if (acc.appLockType != AppLockType.NONE.name && !appUnlocked) {
                        AppLockScreen(
                            account = acc,
                            onUnlocked = { viewModel.markAppUnlocked() },
                            onSignOut = {}
                        )
                    } else {
                        AuthenticatedLoreShell(
                            viewModel = viewModel,
                            isAdmin = acc.role == UserRole.ADMIN.name
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedLoreShell(
    viewModel: SocialLoreViewModel,
    isAdmin: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "feed"
    val theme = LocalLoreTheme.current

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminAllUsers.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val unreadNotifs = remember(notifications) { notifications.count { !it.read } }
    val unreadMessages = remember(conversationsState, viewModel.currentUserId) {
        (conversationsState as? UiState.Success)?.data
            ?.sumOf { (it.unreadCounts[viewModel.currentUserId] ?: 0L).toInt() }
            ?: 0
    }
    val pendingApprovals = remember(adminUsers) {
        adminUsers.count { it.status == MemberStatus.PENDING.name }
    }

    var showMoreSheet by remember { mutableStateOf(false) }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateDeepRoute(targetRoute: String) {
        when {
            targetRoute.startsWith("messages/") -> {
                val convId = targetRoute.removePrefix("messages/")
                navController.navigate("messages?convId=${Uri.encode(convId)}") {
                    launchSingleTop = true
                }
            }
            targetRoute == "profile" -> navigateTop("profile/${viewModel.currentUserId}")
            targetRoute in listOf("feed", "lore_book", "relationship_map", "quotes", "lyrics", "albums", "friends", "notifications", "settings", "admin") -> {
                navigateTop(targetRoute)
            }
            targetRoute == "messages" -> navController.navigate("messages") { launchSingleTop = true }
            else -> navigateTop("feed")
        }
    }

    val allSections = remember(isAdmin, unreadNotifs, unreadMessages, pendingApprovals) {
        buildList {
            add(NavSectionItem("feed", "Feed", Icons.Default.DynamicFeed))
            add(NavSectionItem("lore_book", "Lore Book", Icons.Default.AutoStories))
            add(NavSectionItem("relationship_map", "Rel. Map", Icons.Default.AccountTree))
            add(NavSectionItem("quotes", "Quote Book", Icons.Default.FormatQuote))
            add(NavSectionItem("lyrics", "Song Lyrics", Icons.Default.MusicNote))
            add(NavSectionItem("albums", "Photo Albums", Icons.Default.PhotoLibrary))
            add(NavSectionItem("friends", "Friends", Icons.Default.Group))
            add(NavSectionItem("messages", "Messages", Icons.AutoMirrored.Filled.Chat, unreadMessages))
            add(NavSectionItem("profile/${viewModel.currentUserId}", "My Profile", Icons.Default.Person))
            add(NavSectionItem("notifications", "Notifications", Icons.Default.Notifications, unreadNotifs))
            add(NavSectionItem("settings", "Settings", Icons.Default.Settings))
            if (isAdmin) {
                add(NavSectionItem("admin", "Admin", Icons.Default.AdminPanelSettings, pendingApprovals))
            }
        }
    }

    val screenTitle = when {
        currentRoute.startsWith("feed") -> "Social Lore"
        currentRoute.startsWith("lore_book") -> "Lore Book"
        currentRoute.startsWith("character/") -> "Character Lore"
        currentRoute.startsWith("relationship_map") -> "Relationship Map"
        currentRoute.startsWith("quotes") -> "Quote Book"
        currentRoute.startsWith("lyrics") -> "Song Lyrics"
        currentRoute.startsWith("albums") -> "Photo Albums"
        currentRoute.startsWith("friends") -> "Friends"
        currentRoute.startsWith("messages") -> "Messages"
        currentRoute.startsWith("profile/") -> "Member Profile"
        currentRoute.startsWith("notifications") -> "Notifications"
        currentRoute.startsWith("settings") -> "Settings"
        currentRoute.startsWith("admin") -> "Administrator"
        else -> "Social Lore"
    }

    LoreThemedBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(
                        containerColor = theme.navBarColor.copy(alpha = 0.94f),
                        contentColor = theme.textPrimary,
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(8.dp))
                        allSections.forEach { item ->
                            val selected = currentRoute.startsWith(item.route.substringBefore("/").substringBefore("?"))
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text(item.badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = theme.accent,
                                    selectedTextColor = theme.accent,
                                    indicatorColor = theme.accent.copy(alpha = 0.18f),
                                    unselectedIconColor = theme.textSecondary,
                                    unselectedTextColor = theme.textSecondary
                                )
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    screenTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = theme.navBarColor.copy(alpha = 0.88f),
                                titleContentColor = theme.textPrimary
                            ),
                            actions = {
                                if (isAdmin) {
                                    IconButton(
                                        onClick = { navigateTop("admin") },
                                        modifier = Modifier.testTag("topbar_admin_button")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (pendingApprovals > 0) {
                                                    Badge { Text(pendingApprovals.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin dashboard")
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { navigateTop("notifications") },
                                    modifier = Modifier.testTag("topbar_notifications_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifs > 0) {
                                                Badge { Text(unreadNotifs.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isWide) {
                            NavigationBar(
                                containerColor = theme.navBarColor.copy(alpha = 0.94f),
                                contentColor = theme.textPrimary
                            ) {
                                val navColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = theme.accent,
                                    selectedTextColor = theme.accent,
                                    indicatorColor = theme.accent.copy(alpha = 0.18f),
                                    unselectedIconColor = theme.textSecondary,
                                    unselectedTextColor = theme.textSecondary
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "feed",
                                    onClick = { navigateTop("feed") },
                                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                                    label = { Text("Feed", maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "lore_book" || currentRoute.startsWith("character/"),
                                    onClick = { navigateTop("lore_book") },
                                    icon = { Icon(Icons.Default.AutoStories, contentDescription = "Lore Book") },
                                    label = { Text("Lore", maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentRoute.startsWith("messages"),
                                    onClick = { navigateTop("messages") },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (unreadMessages > 0) {
                                                    Badge { Text(unreadMessages.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                                        }
                                    },
                                    label = { Text("Chats", maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "profile/${viewModel.currentUserId}",
                                    onClick = { navigateTop("profile/${viewModel.currentUserId}") },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "My Profile") },
                                    label = { Text("Profile", maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = showMoreSheet,
                                    onClick = { showMoreSheet = true },
                                    icon = { Icon(Icons.Default.Menu, contentDescription = "More sections") },
                                    label = { Text("More", maxLines = 1) },
                                    colors = navColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "feed",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("feed") {
                            FeedScreen(
                                viewModel = viewModel,
                                onOpenProfile = { uid -> navController.navigate("profile/$uid") },
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") },
                                onNavigateSection = { sec -> navigateDeepRoute(sec) }
                            )
                        }
                        composable(
                            route = "profile/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.StringType })
                        ) { entry ->
                            val targetUid = entry.arguments?.getString("uid") ?: viewModel.currentUserId
                            ProfileScreen(
                                viewModel = viewModel,
                                targetUserId = targetUid,
                                onOpenProfile = { uid -> navController.navigate("profile/$uid") },
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") },
                                onOpenAlbums = { navigateTop("albums") },
                                onOpenFriends = { navigateTop("friends") },
                                onOpenMessageThread = { convId ->
                                    navController.navigate("messages?convId=${Uri.encode(convId)}")
                                },
                                onNavigateSection = { sec -> navigateDeepRoute(sec) }
                            )
                        }
                        composable("friends") {
                            FriendsScreen(
                                viewModel = viewModel,
                                onOpenProfile = { uid -> navController.navigate("profile/$uid") },
                                onOpenMessageThread = { convId ->
                                    navController.navigate("messages?convId=${Uri.encode(convId)}")
                                }
                            )
                        }
                        composable("lore_book") {
                            LoreBookScreen(
                                viewModel = viewModel,
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") },
                                onOpenRelationshipMap = { navigateTop("relationship_map") }
                            )
                        }
                        composable(
                            route = "character/{cid}",
                            arguments = listOf(navArgument("cid") { type = NavType.StringType })
                        ) { entry ->
                            val cid = entry.arguments?.getString("cid") ?: ""
                            CharacterDetailScreen(
                                viewModel = viewModel,
                                characterId = cid,
                                onBack = { navController.popBackStack() },
                                onOpenCharacter = { nextId -> navController.navigate("character/$nextId") },
                                onOpenRelationshipMap = { navigateTop("relationship_map") }
                            )
                        }
                        composable("relationship_map") {
                            RelationshipMapScreen(
                                viewModel = viewModel,
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") }
                            )
                        }
                        composable("quotes") {
                            QuoteBookScreen(
                                viewModel = viewModel,
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") }
                            )
                        }
                        composable("lyrics") {
                            LyricsLibraryScreen(viewModel = viewModel)
                        }
                        composable("albums") {
                            PhotoAlbumsScreen(
                                viewModel = viewModel,
                                onOpenCharacter = { cid -> navController.navigate("character/$cid") }
                            )
                        }
                        composable(
                            route = "messages?convId={convId}",
                            arguments = listOf(
                                navArgument("convId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val initialConvId = entry.arguments?.getString("convId")
                            MessagesScreen(
                                viewModel = viewModel,
                                initialConversationId = initialConvId
                            )
                        }
                        composable("notifications") {
                            NotificationsScreen(
                                viewModel = viewModel,
                                onNavigateTarget = { target -> navigateDeepRoute(target) }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onSignedOut = {}
                            )
                        }
                        composable("admin") {
                            AdminDashboardScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showMoreSheet) {
        val secondarySections = allSections.filter {
            it.route !in listOf("feed", "lore_book", "messages", "profile/${viewModel.currentUserId}")
        }
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = theme.cardColor,
            contentColor = theme.textPrimary
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Explore Social Lore", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(secondarySections) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.cardVariantColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMoreSheet = false
                                    navigateTop(item.route)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (item.badgeCount > 0) {
                                            Badge { Text(item.badgeCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = theme.accent)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

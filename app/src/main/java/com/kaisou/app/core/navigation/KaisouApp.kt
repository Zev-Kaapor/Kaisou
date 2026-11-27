package com.kaisou.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// View model
import androidx.lifecycle.viewmodel.compose.viewModel
// Navigation
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.designsystem.components.ConfirmDialog
import com.kaisou.app.core.designsystem.components.OverlaySheetHost
import com.kaisou.app.core.designsystem.components.ToastHost
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.features.auth.ui.AuthState
import com.kaisou.app.features.auth.ui.AuthViewModel
import com.kaisou.app.features.auth.ui.LoginScreen
import com.kaisou.app.features.home.ui.HomeScreen
import com.kaisou.app.features.profile.ui.ProfileSheet
import com.kaisou.app.features.settings.data.UpdateStore
import com.kaisou.app.features.settings.ui.AboutSettingsScreen
import com.kaisou.app.features.settings.ui.ContentSettingsScreen
import com.kaisou.app.features.settings.ui.HomePartsScreen
import com.kaisou.app.features.settings.ui.HomePartsSection
import com.kaisou.app.features.settings.ui.HomeSettingsScreen
import com.kaisou.app.features.settings.ui.SettingsScreen
import com.kaisou.app.features.settings.ui.SettingsSection
import com.kaisou.app.features.settings.ui.ThanksScreen
import com.kaisou.app.features.settings.ui.ThanksSection
import com.kaisou.app.features.settings.ui.UpdatesSection
import com.kaisou.app.features.settings.ui.UpdatesSettingsScreen
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.ui.ProfileMediaScreen
import com.kaisou.app.features.profile.ui.StudioScreen
import com.kaisou.app.features.profile.ui.ProfileScreen
import com.kaisou.app.features.title.ui.CastScreen
import com.kaisou.app.features.title.ui.NewsScreen
import com.kaisou.app.features.title.ui.ReviewScreen
import com.kaisou.app.features.title.ui.ReviewsScreen
import com.kaisou.app.features.title.ui.ThreadScreen
import com.kaisou.app.features.title.ui.ThreadsScreen
import com.kaisou.app.features.title.ui.TitleScreen
import com.kaisou.app.features.updates.data.UpdateState
import com.kaisou.app.features.updates.data.UpdateWork
import com.kaisou.app.features.updates.data.Updates
import com.kaisou.app.features.title.ui.TitleCardsScreen
import com.kaisou.app.features.profile.ui.ProfileVoicesScreen
import com.kaisou.app.features.title.ui.TitleCoversScreen
import com.kaisou.app.features.title.ui.TitleGalleryScreen
import com.kaisou.app.features.user.ui.UserActivityScreen
import com.kaisou.app.features.user.ui.PersonListScreen
import com.kaisou.app.features.user.ui.UserListScreen
import com.kaisou.app.features.user.ui.UserFavouritesScreen
import com.kaisou.app.features.user.ui.UserScreen

// The app only opens for a user that is signed in to AniList, the login comes first
@Composable
fun KaisouApp(authViewModel: AuthViewModel = viewModel()) {
    val auth by authViewModel.state.collectAsState()
    val context = LocalContext.current

    when (val state = auth) {
        // The login of another time is being checked, the screen shows only the brand
        AuthState.Checking -> LoginScreen(
            checking = true,
            busy = false,
            error = null,
            onLogin = {},
            onSignUp = {},
            onTokenLogin = {},
            tokenByBrowser = false,
            onTokenPage = {},
            isPinToken = { false }
        )
        is AuthState.LoggedOut -> LoginScreen(
            checking = false,
            busy = state.busy,
            error = state.error,
            onLogin = { authViewModel.startLogin(context) },
            onSignUp = { authViewModel.startSignUp(context) },
            onTokenLogin = authViewModel::signInWithToken,
            tokenByBrowser = authViewModel.tokenByBrowser,
            onTokenPage = { authViewModel.startTokenLogin(context) },
            isPinToken = authViewModel::isPinToken
        )
        is AuthState.LoggedIn -> KaisouShell(viewer = state.viewer, onSignOut = authViewModel::signOut)
    }
}

// The tabs with the bar below, or the rail on the side in landscape, and over them the menu, the sheets and the update question
@Composable
private fun KaisouShell(viewer: AniViewer, onSignOut: () -> Unit, startTab: MainTab = MainTab.Home) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var profileOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    // The bar shows the tab of the screen that is on, the start tab while there is none yet
    val selected = MainTab.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: startTab

    // Pages that open over the tabs use the whole screen, but the page of a title keeps the bar with its own sections
    val route = backStackEntry?.destination?.route
    val fullScreen = route.let { it == NewsRoute || it == ProfileRoute || it == ProfileMediaRoute || it == ProfileVoicesRoute || it == CastRoute || it == CardsRoute || it == CoversRoute || it == GalleryRoute || it == ReviewsRoute || it == ReviewRoute || it == ThreadsRoute || it == ThreadRoute || it == UserActivityRoute || it == UserFavouritesRoute || it == UserListRoute }
    val titleSections by NavSectionsHost.sections.collectAsState()
    val sections = if (route == TitleRoute || route == UserRoute) titleSections else null

    val onSelect: (MainTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            // Keep one entry per tab and restore its state, except for the start tab, which stays at the bottom of the stack
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = tab != startTab
        }
    }

    // The check for a newer version runs at the pace the user chose, and every time the app opens, unless the updates are paused
    LaunchedEffect(Unit) {
        var isFirst = true
        UpdateStore.updates.collect {
            UpdateWork.apply(context, it.paused, it.interval.hours, keepExisting = isFirst)
            isFirst = false
        }
    }
    LaunchedEffect(Unit) {
        if (!UpdateStore.updates.value.paused) Updates.check(context, isAsked = false)
    }

    // The links of the texts of AniList to a title or to a person open their pages here, and not in the browser
    val aniLinks: (String) -> Boolean = { address -> aniLinkRoute(address)?.let { navController.navigate(it) } != null }

    val openUser: (String) -> Unit = { name -> navController.navigate(userRoute(name)) }

    CompositionLocalProvider(LocalAniLinks provides aniLinks, LocalOpenUser provides openUser) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = !fullScreen,
                    enter = slideInHorizontally(tween(KaisouMotion.ExpandMillis)) { -it },
                    exit = slideOutHorizontally(tween(KaisouMotion.ExpandMillis)) { -it }
                ) { KaisouNavRail(selected = selected, onSelect = onSelect, sections = sections) }

                // The rail clears the cutout on its side, so the content keeps away from the other, or from both without the rail
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (fullScreen) Modifier else Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)))
                        .windowInsetsPadding(WindowInsets.displayCutout.only(if (fullScreen) WindowInsetsSides.Horizontal else WindowInsetsSides.End))
                ) {
                    MainNavHost(
                        navController = navController,
                        startTab = startTab,
                        viewer = viewer,
                        onProfile = { profileOpen = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    navController = navController,
                    startTab = startTab,
                    viewer = viewer,
                    onProfile = { profileOpen = true },
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(
                    visible = !fullScreen,
                    enter = slideInVertically(tween(KaisouMotion.ExpandMillis)) { it },
                    exit = slideOutVertically(tween(KaisouMotion.ExpandMillis)) { it }
                ) { KaisouNavBar(selected = selected, onSelect = onSelect, sections = sections) }
            }
        }

        // The menu of the account, over everything including the bar
        ProfileSheet(
            visible = profileOpen,
            viewer = viewer,
            onDismiss = { profileOpen = false },
            onSignOut = {
                profileOpen = false
                onSignOut()
            },
            onProfile = {
                profileOpen = false
                navController.navigate(userRoute(viewer.name))
            },
            onSettings = {
                profileOpen = false
                navController.navigate(SettingsRoute)
            }
        )

        // The sheets that the screens ask for, which rise over the bar
        OverlaySheetHost()

        // The notices of the app, as an island at the top, over everything
        ToastHost(modifier = Modifier.align(Alignment.TopCenter))

        // The question when there is a newer version, which goes to the updates screen to show the download
        val updatePrompt by Updates.prompt.collectAsState()
        updatePrompt?.let { info ->
            ConfirmDialog(
                title = stringResource(R.string.updates_prompt_title),
                text = stringResource(R.string.updates_prompt_text, info.version),
                confirmLabel = stringResource(R.string.updates_prompt_confirm),
                cancelLabel = stringResource(R.string.updates_prompt_later),
                onConfirm = {
                    Updates.clearPrompt()
                    if (Updates.state.value is UpdateState.Available) Updates.download(context)
                    navController.navigate(settingsSectionRoute(UpdatesSection))
                },
                onDismiss = { Updates.postpone(context) }
            )
        }
    }
    }
}

// The four tabs and the settings screens, sliding and fading as you switch, with the back gesture following the finger
@Composable
private fun MainNavHost(
    navController: NavHostController,
    startTab: MainTab,
    viewer: AniViewer,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startTab.route,
        modifier = modifier,
        enterTransition = { comesIn(forward = targetIndex() == NotATab || targetIndex() > initialIndex()) },
        exitTransition = { goesOut(forward = targetIndex() == NotATab || targetIndex() > initialIndex()) },
        popEnterTransition = { comesIn(forward = initialIndex() != NotATab && initialIndex() < targetIndex()) },
        popExitTransition = { goesOut(forward = initialIndex() != NotATab && initialIndex() < targetIndex()) },
        predictivePopEnterTransition = { comesIn(forward = initialIndex() != NotATab && initialIndex() < targetIndex()) },
        predictivePopExitTransition = { goesOut(forward = initialIndex() != NotATab && initialIndex() < targetIndex()) }
    ) {
        composable(MainTab.Home.route) {
            HomeScreen(
                viewer = viewer,
                onProfile = onProfile,
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenList = { manga, tab -> navController.navigate(userListRoute(viewer.name, manga, tab)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // The page of a title opens over the tabs, with no bar, and back returns to where it was opened
        composable(
            route = TitleRoute,
            arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType })
        ) {
            TitleScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenNews = { id, malId, manga -> navController.navigate(newsRoute(id, malId, manga)) },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                onOpenCast = { id, staff -> navController.navigate(castRoute(id, staff)) },
                onOpenCards = { id, order, manga -> navController.navigate(cardsRoute(id, order, manga)) },
                onOpenCovers = { navController.navigate(coversRoute(it)) },
                onOpenGallery = { id, kind -> navController.navigate(galleryRoute(id, kind.path)) },
                onOpenReviews = { navController.navigate(reviewsRoute(it)) },
                onOpenThreads = { navController.navigate(threadsRoute(it)) },
                onOpenThread = { navController.navigate(threadRoute(it)) },
                onOpenReview = { navController.navigate(reviewRoute(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // A character, a person of the staff or a studio open over the title, with no bar
        composable(
            route = ProfileRoute,
            arguments = listOf(navArgument(ProfileKindArg) { type = NavType.StringType }, navArgument(TitleIdArg) { type = NavType.IntType })
        ) { entry ->
            // A studio is its productions by year, the others have a page of their own
            if (entry.arguments?.getString(ProfileKindArg) == ProfileKind.Studio.path) {
                StudioScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTitle = { navController.navigate(titleRoute(it)) },
                    onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                    modifier = Modifier.fillMaxSize()
                )
                return@composable
            }
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                onOpenMedia = { ref, tab -> navController.navigate(profileMediaRoute(ref.kind.path, ref.id, tab.path)) },
                onOpenVoices = { ref -> navController.navigate(profileVoicesRoute(ref.kind.path, ref.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // The page of a person of AniList opens over the page it came from, with no bar
        composable(route = UserRoute, arguments = listOf(navArgument(UserNameArg) { type = NavType.StringType })) {
            UserScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                onOpenActivity = { name, id -> navController.navigate(userActivityRoute(name, id)) },
                onOpenFavourites = { name, kind -> navController.navigate(userFavouritesRoute(name, kind.path)) },
                onOpenList = { name, manga, tab -> navController.navigate(userListRoute(name, manga, tab)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // The list of anime or of manga of a person opens over their page with no bar, on the tab of the state that was touched
        composable(
            route = UserListRoute,
            arguments = listOf(
                navArgument(UserNameArg) { type = NavType.StringType },
                navArgument(UserListKindArg) { type = NavType.StringType },
                navArgument(UserListTabArg) { type = NavType.StringType }
            )
        ) {
            PersonListScreen(onBack = { navController.popBackStack() }, onOpenTitle = { navController.navigate(titleRoute(it)) }, modifier = Modifier.fillMaxSize())
        }

        // The whole list of one kind of favourites of a person opens over their page, with no bar
        composable(
            route = UserFavouritesRoute,
            arguments = listOf(navArgument(UserNameArg) { type = NavType.StringType }, navArgument(UserFavouritesKindArg) { type = NavType.StringType })
        ) {
            UserFavouritesScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Everything a person did lately opens over their page, with no bar
        composable(
            route = UserActivityRoute,
            arguments = listOf(navArgument(UserNameArg) { type = NavType.StringType }, navArgument(UserIdArg) { type = NavType.IntType })
        ) {
            UserActivityScreen(onBack = { navController.popBackStack() }, onOpenTitle = { navController.navigate(titleRoute(it)) }, modifier = Modifier.fillMaxSize())
        }

        // All the voices of a character, in every language
        composable(
            route = ProfileVoicesRoute,
            arguments = listOf(navArgument(ProfileKindArg) { type = NavType.StringType }, navArgument(TitleIdArg) { type = NavType.IntType })
        ) {
            ProfileVoicesScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // All the titles of a character, a person of the staff or a studio
        composable(
            route = ProfileMediaRoute,
            arguments = listOf(
                navArgument(ProfileKindArg) { type = NavType.StringType },
                navArgument(TitleIdArg) { type = NavType.IntType },
                navArgument(ProfileTabArg) { type = NavType.StringType; defaultValue = "anime" }
            )
        ) {
            ProfileMediaScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // All the characters or all the staff of a title
        composable(
            route = CastRoute,
            arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType }, navArgument(CastKindArg) { type = NavType.StringType })
        ) {
            CastScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(profileRoute(it.kind.path, it.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // All the reviews of a title, and one of them read whole, open over it with no bar
        composable(route = ReviewsRoute, arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType })) {
            ReviewsScreen(
                onBack = { navController.popBackStack() },
                onOpenReview = { navController.navigate(reviewRoute(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(route = ReviewRoute, arguments = listOf(navArgument(ReviewIdArg) { type = NavType.IntType })) {
            ReviewScreen(onBack = { navController.popBackStack() }, modifier = Modifier.fillMaxSize())
        }

        // All the threads of a title, and one of them read whole, open over it with no bar
        composable(route = ThreadsRoute, arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType })) {
            ThreadsScreen(
                onBack = { navController.popBackStack() },
                onOpenThread = { navController.navigate(threadRoute(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(route = ThreadRoute, arguments = listOf(navArgument(ThreadIdArg) { type = NavType.IntType })) {
            ThreadScreen(onBack = { navController.popBackStack() }, onOpenTitle = { navController.navigate(titleRoute(it)) }, modifier = Modifier.fillMaxSize())
        }

        // All the covers of the volumes of a manga, open over it with no bar
        composable(route = CoversRoute, arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType })) {
            TitleCoversScreen(onBack = { navController.popBackStack() }, modifier = Modifier.fillMaxSize())
        }

        // The posters, backdrops and logos of an anime, open over it with no bar
        composable(
            route = GalleryRoute,
            arguments = listOf(navArgument(TitleIdArg) { type = NavType.IntType }, navArgument(GalleryKindArg) { type = NavType.StringType })
        ) {
            TitleGalleryScreen(onBack = { navController.popBackStack() }, modifier = Modifier.fillMaxSize())
        }

        // All the titles tied to a title, or its whole series in order, open over it with no bar
        composable(
            route = CardsRoute,
            arguments = listOf(
                navArgument(TitleIdArg) { type = NavType.IntType },
                navArgument(CardsKindArg) { type = NavType.StringType },
                navArgument(CardsMangaArg) { type = NavType.BoolType; defaultValue = false }
            )
        ) {
            TitleCardsScreen(
                onBack = { navController.popBackStack() },
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // All the news of a title open over it, with no bar
        composable(
            route = NewsRoute,
            arguments = listOf(
                navArgument(TitleIdArg) { type = NavType.IntType },
                navArgument(NewsMalArg) { type = NavType.IntType; defaultValue = -1 },
                navArgument(NewsMangaArg) { type = NavType.BoolType; defaultValue = false }
            )
        ) {
            NewsScreen(onBack = { navController.popBackStack() }, modifier = Modifier.fillMaxSize())
        }

        // The other tabs have no screen yet
        composable(MainTab.Explore.route) { Box(modifier = Modifier.fillMaxSize()) }
        composable(MainTab.Calendar.route) { Box(modifier = Modifier.fillMaxSize()) }

        // The Library is the whole list of the user, in the tab itself, with no screen to open from it
        composable(MainTab.Library.route) {
            UserListScreen(
                onOpenTitle = { navController.navigate(titleRoute(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(SettingsRoute) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSectionClick = { section -> navController.navigate(settingsSectionRoute(section.name)) }
            )
        }
        composable(
            route = SettingsSectionRoute,
            arguments = listOf(navArgument(SettingsSectionArg) { type = NavType.StringType })
        ) { entry ->
            when (entry.arguments?.getString(SettingsSectionArg)) {
                SettingsSection.About.name -> AboutSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onUpdates = { navController.navigate(settingsSectionRoute(UpdatesSection)) },
                    onThanks = { navController.navigate(settingsSectionRoute(ThanksSection)) }
                )
                SettingsSection.Home.name -> HomeSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onParts = { navController.navigate(settingsSectionRoute(HomePartsSection)) }
                )
                HomePartsSection -> HomePartsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Content.name -> ContentSettingsScreen(onBack = { navController.popBackStack() })
                ThanksSection -> ThanksScreen(onBack = { navController.popBackStack() })
                UpdatesSection -> UpdatesSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// The place in the bar of the tab that is going away and of the one that is coming, which tells to which side the screens move
private fun AnimatedContentTransitionScope<NavBackStackEntry>.initialIndex(): Int = indexOf(initialState)

// Gives the index of the screen being gone to
private fun AnimatedContentTransitionScope<NavBackStackEntry>.targetIndex(): Int = indexOf(targetState)

// What the place of a screen is when it is not one of the tabs
private const val NotATab = -1

// Gives the place of a screen among the tabs
private fun indexOf(entry: NavBackStackEntry): Int =
    MainTab.entries.indexOfFirst { it.route == entry.destination.route }

// A tab after the current one comes from the right, one before it from the left
private fun comesIn(forward: Boolean): EnterTransition {
    val direction = if (forward) 1 else -1
    return slideInHorizontally { direction * it / KaisouMotion.PageSlideDivisor } + fadeIn(tween(KaisouMotion.TabSwitchMillis))
}

// The tab that leaves goes to the side it came from
private fun goesOut(forward: Boolean): ExitTransition {
    val direction = if (forward) 1 else -1
    return slideOutHorizontally { -direction * it / KaisouMotion.PageSlideDivisor } + fadeOut(tween(KaisouMotion.TabSwitchMillis))
}

package com.kaisou.app.core.navigation

// Compose animation and layouts
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
import com.kaisou.app.features.settings.ui.SettingsScreen
import com.kaisou.app.features.settings.ui.SettingsSection
import com.kaisou.app.features.settings.ui.ThanksScreen
import com.kaisou.app.features.settings.ui.ThanksSection
import com.kaisou.app.features.settings.ui.UpdatesSection
import com.kaisou.app.features.settings.ui.UpdatesSettingsScreen
import com.kaisou.app.features.title.ui.NewsScreen
import com.kaisou.app.features.title.ui.TitleScreen
import com.kaisou.app.features.updates.data.UpdateState
import com.kaisou.app.features.updates.data.UpdateWork
import com.kaisou.app.features.updates.data.Updates

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

// The tabs with the navigation bar below them, or the rail on the side in landscape. The app opens on the start tab, which
// is the Home until the user can choose it, and back from any other tab returns to it before the app closes. The menu of the
// account, the sheets that the screens ask for and the question of a newer version are drawn over everything
@Composable
private fun KaisouShell(viewer: AniViewer, onSignOut: () -> Unit, startTab: MainTab = MainTab.Home) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var profileOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    // The bar shows the tab of the screen that is on, the start tab while there is none yet
    val selected = MainTab.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: startTab
    // The page of a title has the whole screen, with no bar and no rail
    val onTitle = backStackEntry?.destination?.route.let { it == TitleRoute || it == NewsRoute }

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (!onTitle) KaisouNavRail(selected = selected, onSelect = onSelect)

                // The rail already makes room for the camera cutout on its side, the content keeps away from it on the other. Without
                // the rail it keeps away from both
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (onTitle) Modifier else Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)))
                        .windowInsetsPadding(WindowInsets.displayCutout.only(if (onTitle) WindowInsetsSides.Horizontal else WindowInsetsSides.End))
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
                if (!onTitle) KaisouNavBar(selected = selected, onSelect = onSelect)
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
            onProfile = { profileOpen = false },
            onSettings = {
                profileOpen = false
                navController.navigate(SettingsRoute)
            }
        )

        // The sheets that the screens ask for, which rise over the bar
        OverlaySheetHost()

        // The notices of the app, as an island at the top, over everything
        ToastHost(modifier = Modifier.align(Alignment.TopCenter))

        // The question when the app opens and there is a newer version, it goes to the screen of the updates to show how the download goes
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

// The four tabs and the screens of the settings. Switching slides the screens a little to the side the destination is on and fades
// them, a screen that is not a tab counts as the one after, and the back gesture plays the same movement following the finger
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
        composable(MainTab.Library.route) { Box(modifier = Modifier.fillMaxSize()) }

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
                ThanksSection -> ThanksScreen(onBack = { navController.popBackStack() })
                UpdatesSection -> UpdatesSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// The place in the bar of the tab that is going away and of the one that is coming, which tells to which side the screens move
private fun AnimatedContentTransitionScope<NavBackStackEntry>.initialIndex(): Int = indexOf(initialState)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.targetIndex(): Int = indexOf(targetState)

// What the place of a screen is when it is not one of the tabs
private const val NotATab = -1

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

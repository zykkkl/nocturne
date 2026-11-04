package com.nocturne.learn.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nocturne.learn.AppViewModel
import com.nocturne.learn.AskViewModel
import com.nocturne.learn.SessionViewModel
import com.nocturne.learn.data.ProviderTemplates
import com.nocturne.learn.ui.components.NocturneBottomBar
import com.nocturne.learn.ui.components.TabDestination
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.screens.HomeScreen
import com.nocturne.learn.ui.screens.AskScreen
import com.nocturne.learn.ui.screens.MapScreen
import com.nocturne.learn.ui.screens.OnboardingScreen
import com.nocturne.learn.ui.screens.ProfileScreen
import com.nocturne.learn.ui.screens.ProviderEditorScreen
import com.nocturne.learn.ui.screens.ProviderListScreen
import com.nocturne.learn.ui.screens.ReviewScreen
import com.nocturne.learn.ui.screens.SessionScreen
import com.nocturne.learn.ui.theme.NocturneColors

private val tabs = listOf(
    TabDestination(Routes.HOME, "今日", Icons.Filled.Home),
    TabDestination(Routes.MAP, "星图", Icons.Filled.Star),
    TabDestination(Routes.REVIEW, "复习", Icons.Filled.Refresh),
    TabDestination(Routes.PROFILE, "我的", Icons.Filled.Person),
)

@Composable
fun NocturneNav(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val learning by viewModel.learningState.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val activeSubjectId by viewModel.activeSubjectId.collectAsState()
    val current = settings

    if (current == null) {
        LoadingScreen()
        return
    }

    val navController = rememberNavController()
    val startDestination = remember {
        if (current.onboardingDone) Routes.HOME else Routes.ONBOARDING
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    initialGoal = current.goal,
                    initialMinutes = current.dailyMinutes,
                ) { goal, minutes ->
                    viewModel.completeOnboarding(goal, minutes)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.HOME) {
                HomeScreen(
                    settings = current,
                    learning = learning,
                    subjects = subjects,
                    activeSubjectId = activeSubjectId,
                    onConfigureModel = { navController.navigate(Routes.API_PROVIDERS) },
                    onOpenMap = { navController.navigateTab(Routes.MAP) },
                    onStartSession = {
                        if (current.activeProvider != null) {
                            navController.navigate(Routes.SESSION)
                        } else {
                            navController.navigate(Routes.API_PROVIDERS)
                        }
                    },
                    onAsk = { navController.navigate(Routes.ASK) },
                    onSelectSubject = viewModel::switchSubject,
                    onCreateSubject = viewModel::createSubject,
                )
            }

            composable(Routes.ASK) {
                val askViewModel: AskViewModel = viewModel()
                AskScreen(
                    viewModel = askViewModel,
                    providerReady = current.activeProvider != null,
                    onBack = { navController.popBackStack() },
                    onOpenProviders = { navController.navigate(Routes.API_PROVIDERS) },
                )
            }

            composable(Routes.SESSION) {
                val sessionViewModel: SessionViewModel = viewModel()
                SessionScreen(
                    viewModel = sessionViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MAP) {
                MapScreen(
                    learning = learning,
                    onBackHome = { navController.navigateTab(Routes.HOME) },
                )
            }

            composable(Routes.REVIEW) {
                ReviewScreen(
                    learning = learning,
                    onStartSession = {
                        if (current.activeProvider != null) {
                            navController.navigate(Routes.SESSION)
                        } else {
                            navController.navigate(Routes.API_PROVIDERS)
                        }
                    },
                    onBackHome = { navController.navigateTab(Routes.HOME) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    settings = current,
                    learning = learning,
                    subjects = subjects,
                    activeSubjectId = activeSubjectId,
                    onOpenProviders = { navController.navigate(Routes.API_PROVIDERS) },
                    onUpdatePlan = viewModel::updatePlan,
                    onSelectSubject = viewModel::switchSubject,
                    onCreateSubject = viewModel::createSubject,
                )
            }

            composable(Routes.API_PROVIDERS) {
                ProviderListScreen(
                    providers = current.providers,
                    activeProviderId = current.activeProvider?.id,
                    onBack = { navController.popBackStack() },
                    onAdd = { templateId -> navController.navigate(Routes.apiNew(templateId)) },
                    onEdit = { providerId -> navController.navigate(Routes.apiEdit(providerId)) },
                    onActivate = viewModel::setActiveProvider,
                )
            }

            composable(
                route = Routes.API_NEW,
                arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
            ) { entry ->
                val template = ProviderTemplates.byId(entry.arguments?.getString("templateId"))
                ProviderEditorScreen(
                    template = template,
                    provider = null,
                    savedKey = null,
                    isActive = false,
                    onBack = { navController.popBackStack() },
                    onTest = viewModel::testConnection,
                    onSave = { provider, key, makeActive ->
                        viewModel.saveProvider(provider, key, makeActive)
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = Routes.API_EDIT,
                arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
            ) { entry ->
                val provider = current.providers.firstOrNull {
                    it.id == entry.arguments?.getString("providerId")
                }
                if (provider == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    ProviderEditorScreen(
                        template = ProviderTemplates.byId(provider.templateId),
                        provider = provider,
                        savedKey = viewModel.savedKey(provider.id),
                        isActive = provider.id == current.activeProvider?.id,
                        onBack = { navController.popBackStack() },
                        onTest = viewModel::testConnection,
                        onSave = { updated, key, makeActive ->
                            viewModel.saveProvider(updated, key, makeActive)
                            navController.popBackStack()
                        },
                        onDelete = {
                            viewModel.removeProvider(provider.id)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }

        if (currentRoute in Routes.TABS) {
            NocturneBottomBar(
                items = tabs,
                currentRoute = currentRoute,
                onSelect = { route -> navController.navigateTab(route) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisOrb(diameter = 56.dp, active = false)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "夜航正在校准…",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

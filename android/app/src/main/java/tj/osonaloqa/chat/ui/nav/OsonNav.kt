package tj.osonaloqa.chat.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.ui.screens.auth.OtpScreen
import tj.osonaloqa.chat.ui.screens.auth.PhoneScreen
import tj.osonaloqa.chat.ui.screens.auth.ProfileSetupScreen
import tj.osonaloqa.chat.ui.screens.auth.WelcomeScreen
import tj.osonaloqa.chat.ui.screens.chat.ChatScreen
import tj.osonaloqa.chat.ui.screens.main.MainScreen
import tj.osonaloqa.chat.ui.screens.settings.SettingsScreen

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val PHONE = "phone"
    const val OTP = "otp/{phone}"
    const val PROFILE_SETUP = "profileSetup"
    const val MAIN = "main"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"

    fun otp(phone: String) = "otp/$phone"
    fun chat(chatId: String) = "chat/$chatId"
}

@Composable
fun OsonApp() {
    val nav = rememberNavController()
    val container = Dependencies.get()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            val session by container.sessionManager.session.collectAsState(initial = null)
            LaunchedEffect(session) {
                val s = session ?: return@LaunchedEffect
                kotlinx.coroutines.delay(700)
                if (s.isAuthed) nav.navigate(Routes.MAIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                else nav.navigate(Routes.WELCOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
            tj.osonaloqa.chat.ui.screens.splash.SplashScreen()
        }
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onContinue = { nav.navigate(Routes.PHONE) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PHONE) {
            PhoneScreen(
                onBack = { nav.popBackStack() },
                onCodeSent = { phone -> nav.navigate(Routes.otp(phone)) },
            )
        }
        composable(Routes.OTP, arguments = listOf(navArgument("phone") { defaultValue = "" })) { entry ->
            val phone = entry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onBack = { nav.popBackStack() },
                onVerified = { isNew ->
                    if (isNew) nav.navigate(Routes.PROFILE_SETUP) { popUpTo(Routes.PHONE) { inclusive = true } }
                    else nav.navigate(Routes.MAIN) { popUpTo(0) }
                },
            )
        }
        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onDone = { nav.navigate(Routes.MAIN) { popUpTo(0) } },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onOpenChat = { chatId -> nav.navigate(Routes.chat(chatId)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("chatId") { defaultValue = "" })) { entry ->
            val chatId = entry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

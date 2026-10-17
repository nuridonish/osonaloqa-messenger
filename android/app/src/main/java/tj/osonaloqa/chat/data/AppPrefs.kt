package tj.osonaloqa.chat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefsStore by preferencesDataStore(name = "oson_prefs")

data class AppSettings(
    val theme: String = "system",          // system | light | dark
    val fontScale: Float = 1f,
    val highContrast: Boolean = false,
    val animateMedia: Boolean = true,
    val enterToSend: Boolean = false,
    val mediaVisibility: Boolean = true,
    val autoDownloadMobile: Boolean = true,
    val autoDownloadWifi: Boolean = true,
    val autoDownloadRoaming: Boolean = false,
    val mediaQuality: String = "auto",     // auto | high | economy
    val notifyMessages: Boolean = true,
    val notifyGroups: Boolean = true,
    val notifyCalls: Boolean = true,
    val notifyStatus: Boolean = true,
    val notifSound: Boolean = true,
    val notifVibration: Boolean = true,
    val notifLight: Boolean = true,
    val notifPriority: Boolean = true,
    val notifReactions: Boolean = true,
    val notifBadge: Boolean = true,
    val privacyLastSeen: Boolean = true,
    val privacyReadReceipts: Boolean = true,
    val privacyTyping: Boolean = true,
    val archivedVisible: Boolean = false,
)

class AppPrefs(private val context: Context) {
    private val k = { name: String -> stringPreferencesKey(name) }
    private val themeKey = k("theme")
    private val fontKey = floatPreferencesKey("font_scale")
    private val contrastKey = booleanPreferencesKey("contrast")
    private val animKey = booleanPreferencesKey("anim")
    private val enterKey = booleanPreferencesKey("enter_send")
    private val mediaVisKey = booleanPreferencesKey("media_vis")
    private val adMobile = booleanPreferencesKey("ad_mobile")
    private val adWifi = booleanPreferencesKey("ad_wifi")
    private val adRoaming = booleanPreferencesKey("ad_roaming")
    private val mediaQuality = k("media_quality")
    private val nMsg = booleanPreferencesKey("n_msg")
    private val nGroup = booleanPreferencesKey("n_group")
    private val nCall = booleanPreferencesKey("n_call")
    private val nStatus = booleanPreferencesKey("n_status")
    private val nSound = booleanPreferencesKey("n_sound")
    private val nVibro = booleanPreferencesKey("n_vibro")
    private val nLight = booleanPreferencesKey("n_light")
    private val nPriority = booleanPreferencesKey("n_priority")
    private val nReactions = booleanPreferencesKey("n_reactions")
    private val nBadge = booleanPreferencesKey("n_badge")
    private val pLastSeen = booleanPreferencesKey("p_last_seen")
    private val pRead = booleanPreferencesKey("p_read")
    private val pTyping = booleanPreferencesKey("p_typing")
    private val archivedKey = booleanPreferencesKey("archived_visible")

    val settings: Flow<AppSettings> = context.prefsStore.data.map { p ->
        AppSettings(
            theme = p[themeKey] ?: "system",
            fontScale = p[fontKey] ?: 1f,
            highContrast = p[contrastKey] ?: false,
            animateMedia = p[animKey] ?: true,
            enterToSend = p[enterKey] ?: false,
            mediaVisibility = p[mediaVisKey] ?: true,
            autoDownloadMobile = p[adMobile] ?: true,
            autoDownloadWifi = p[adWifi] ?: true,
            autoDownloadRoaming = p[adRoaming] ?: false,
            mediaQuality = p[mediaQuality] ?: "auto",
            notifyMessages = p[nMsg] ?: true,
            notifyGroups = p[nGroup] ?: true,
            notifyCalls = p[nCall] ?: true,
            notifyStatus = p[nStatus] ?: true,
            notifSound = p[nSound] ?: true,
            notifVibration = p[nVibro] ?: true,
            notifLight = p[nLight] ?: true,
            notifPriority = p[nPriority] ?: true,
            notifReactions = p[nReactions] ?: true,
            notifBadge = p[nBadge] ?: true,
            privacyLastSeen = p[pLastSeen] ?: true,
            privacyReadReceipts = p[pRead] ?: true,
            privacyTyping = p[pTyping] ?: true,
            archivedVisible = p[archivedKey] ?: false,
        )
    }

    suspend fun setString(key: String, value: String) = context.prefsStore.edit {
        when (key) {
            "theme" -> it[themeKey] = value
            "media_quality" -> it[mediaQuality] = value
        }
    }

    suspend fun setFloat(key: String, value: Float) = context.prefsStore.edit {
        if (key == "font_scale") it[fontKey] = value
    }

    suspend fun setBool(key: String, value: Boolean) = context.prefsStore.edit {
        when (key) {
            "contrast" -> it[contrastKey] = value
            "anim" -> it[animKey] = value
            "enter_send" -> it[enterKey] = value
            "media_vis" -> it[mediaVisKey] = value
            "ad_mobile" -> it[adMobile] = value
            "ad_wifi" -> it[adWifi] = value
            "ad_roaming" -> it[adRoaming] = value
            "n_msg" -> it[nMsg] = value
            "n_group" -> it[nGroup] = value
            "n_call" -> it[nCall] = value
            "n_status" -> it[nStatus] = value
            "n_sound" -> it[nSound] = value
            "n_vibro" -> it[nVibro] = value
            "n_light" -> it[nLight] = value
            "n_priority" -> it[nPriority] = value
            "n_reactions" -> it[nReactions] = value
            "n_badge" -> it[nBadge] = value
            "p_last_seen" -> it[pLastSeen] = value
            "p_read" -> it[pRead] = value
            "p_typing" -> it[pTyping] = value
            "archived_visible" -> it[archivedKey] = value
        }
    }
}

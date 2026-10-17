package tj.osonaloqa.chat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "oson_session")

data class Session(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val phone: String = "",
    val name: String = "",
    val username: String = "",
    val about: String = "",
    val issuedAt: Long = 0,
)

val Session.isAuthed: Boolean get() = accessToken.isNotEmpty()

class SessionManager(private val context: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")
    private val userIdKey = stringPreferencesKey("user_id")
    private val phoneKey = stringPreferencesKey("phone")
    private val nameKey = stringPreferencesKey("name")
    private val usernameKey = stringPreferencesKey("username")
    private val aboutKey = stringPreferencesKey("about")
    private val issuedKey = stringPreferencesKey("issued_at")

    val session: Flow<Session> = context.dataStore.data.map { p ->
        Session(
            accessToken = p[accessKey] ?: "",
            refreshToken = p[refreshKey] ?: "",
            userId = p[userIdKey] ?: "",
            phone = p[phoneKey] ?: "",
            name = p[nameKey] ?: "",
            username = p[usernameKey] ?: "",
            about = p[aboutKey] ?: "",
            issuedAt = p[issuedKey]?.toLongOrNull() ?: 0,
        )
    }

    suspend fun current(): Session = session.first()

    suspend fun save(s: Session) {
        context.dataStore.edit { p ->
            p[accessKey] = s.accessToken
            p[refreshKey] = s.refreshToken
            p[userIdKey] = s.userId
            p[phoneKey] = s.phone
            p[nameKey] = s.name
            p[usernameKey] = s.username
            p[aboutKey] = s.about
            p[issuedKey] = s.issuedAt.toString()
        }
    }

    suspend fun updateProfile(name: String, username: String, about: String) {
        context.dataStore.edit { p ->
            p[nameKey] = name
            p[usernameKey] = username
            p[aboutKey] = about
        }
    }

    suspend fun setTokens(access: String, refresh: String) {
        context.dataStore.edit { p ->
            p[accessKey] = access
            p[refreshKey] = refresh
            p[issuedKey] = System.currentTimeMillis().toString()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

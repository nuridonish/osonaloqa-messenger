package tj.osonaloqa.chat

import android.app.Application
import android.content.Context
import tj.osonaloqa.chat.data.SessionManager
import tj.osonaloqa.chat.data.local.AppDatabase
import tj.osonaloqa.chat.data.remote.ApiClient
import tj.osonaloqa.chat.data.remote.OsonApi
import tj.osonaloqa.chat.data.remote.WsClient
import tj.osonaloqa.chat.data.repo.AuthRepository
import tj.osonaloqa.chat.data.repo.ChatRepository

class AppContainer(app: Application) {
    val sessionManager = SessionManager(app)
    val okHttp = ApiClient.okHttp()
    val wsClient = WsClient(okHttp)

    @Volatile var baseUrl: String = BuildConfig.BASE_URL

    val api: OsonApi by lazy { ApiClient.create(baseUrl, okHttp) }

    val db: AppDatabase = AppDatabase.get(app)

    val authRepository = AuthRepository(api, sessionManager)

    val chatRepository = ChatRepository(api, wsClient, db, sessionManager, authRepository)
}

object Dependencies {
    @Volatile private var container: AppContainer? = null

    fun init(app: Application): AppContainer =
        container ?: synchronized(this) {
            container ?: AppContainer(app).also { container = it }
        }

    fun get(): AppContainer = container ?: error("Dependencies.init() davat nakardaast")
}

fun Context.appContainer(): AppContainer = Dependencies.get()

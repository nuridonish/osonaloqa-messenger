package tj.osonaloqa.chat.data.repo

import tj.osonaloqa.chat.data.Session
import tj.osonaloqa.chat.data.SessionManager
import tj.osonaloqa.chat.data.remote.OsonApi
import tj.osonaloqa.chat.data.remote.RefreshReq
import tj.osonaloqa.chat.data.remote.RequestOtpReq
import tj.osonaloqa.chat.data.remote.VerifyReq

sealed class AuthResult {
    data object Ok : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: OsonApi,
    private val sessions: SessionManager,
) {
    suspend fun requestOtp(e164: String): Result<Unit> = runCatching {
        val res = api.requestOtp(RequestOtpReq(phone = e164))
        if (!res.ok && res.error != null) error(res.error)
    }

    suspend fun verify(e164: String, code: String): Result<Boolean> = runCatching {
        val res = api.verify(VerifyReq(phone = e164, code = code))
        val token = res.accessToken
        if (token.isEmpty()) error(res.error ?: "invalid_code")
        val user = res.user
        sessions.save(
            Session(
                accessToken = token,
                refreshToken = res.refreshToken,
                userId = user?.id ?: "",
                phone = user?.phone ?: e164,
                name = user?.name ?: "",
                username = user?.username ?: "",
                about = user?.about ?: "",
                issuedAt = System.currentTimeMillis(),
            )
        )
        res.isNew
    }

    /** Агар access-токен ба поёнрасидан наздик бошад, аз refresh мекунад */
    suspend fun ensureFreshToken(): Boolean {
        val s = sessions.current()
        if (!s.isAuthed) return false
        val age = System.currentTimeMillis() - s.issuedAt
        return if (age > 12 * 60 * 1000L) refreshIfNeeded() else true
    }

    suspend fun refreshIfNeeded(): Boolean {
        val s = sessions.current()
        if (s.refreshToken.isEmpty()) return false
        return runCatching {
            val res = api.refresh(RefreshReq(s.refreshToken))
            sessions.setTokens(res.accessToken, res.refreshToken)
            true
        }.getOrDefault(false)
    }

    suspend fun updateProfile(name: String, about: String, username: String?): Result<Unit> = runCatching {
        api.updateProfile(
            auth = bearer(),
            body = tj.osonaloqa.chat.data.remote.UpdateProfileReq(name = name, about = about, username = username),
        )
        sessions.updateProfile(name, username ?: sessions.current().username, about)
    }

    suspend fun bearer(): String = "Bearer ${sessions.current().accessToken}"
}

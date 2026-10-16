package tj.osonaloqa.chat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class OsonAloqaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
    }

    companion object {
        fun setLanguage(tag: String) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
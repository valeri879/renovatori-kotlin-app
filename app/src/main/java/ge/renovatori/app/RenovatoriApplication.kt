package ge.renovatori.app

import android.app.Application
import ge.renovatori.app.auth.AuthSession

class RenovatoriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restore the signed-in session before any screen asks for it.
        AuthSession.init(this)
    }
}

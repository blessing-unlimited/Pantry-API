package za.ac.vc.emgppt.pantry

import android.app.Application
import android.util.Log
import za.ac.vc.emgppt.pantry.data.ServiceLocator

class PantryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("Pantry", "Application onCreate — process starting")
        ServiceLocator.init(this)
    }
}

package za.ac.vc.emgppt.pantry.data

import android.content.Context
import android.util.Log
import za.ac.vc.emgppt.pantry.BuildConfig
import za.ac.vc.emgppt.pantry.data.api.PantryApi
import za.ac.vc.emgppt.pantry.data.local.PantryDatabase
import za.ac.vc.emgppt.pantry.data.sync.NetworkMonitor
import za.ac.vc.emgppt.pantry.data.sync.PantryRepository
import za.ac.vc.emgppt.pantry.messaging.PantryNotifications
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Tiny service locator so screens share one Retrofit client without Hilt.
 * Authorization is attached from [SessionStore] on every request (Square, 2024).
 */
object ServiceLocator {
    const val TAG = "Pantry"

    lateinit var session: SessionStore
        private set

    lateinit var api: PantryApi
        private set

    lateinit var appContext: Context
        private set

    lateinit var network: NetworkMonitor
        private set

    lateinit var repository: PantryRepository
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        session = SessionStore(appContext)
        val authInterceptor = Interceptor { chain ->
            val token = session.token
            val builder = chain.request().newBuilder()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply { level = HttpLoggingInterceptor.Level.BASIC }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PantryApi::class.java)

        network = NetworkMonitor(appContext)
        val database = PantryDatabase.create(appContext)
        repository = PantryRepository(appContext, database.pantryDao(), api, session, network)
        repository.start()
        PantryNotifications.ensureChannel(appContext)

        Log.i(TAG, "Retrofit initialised against ${BuildConfig.API_BASE_URL}")
    }
}

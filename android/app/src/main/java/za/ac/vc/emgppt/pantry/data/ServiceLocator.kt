package za.ac.vc.emgppt.pantry.data

import android.content.Context
import android.util.Log
import za.ac.vc.emgppt.pantry.BuildConfig
import za.ac.vc.emgppt.pantry.data.api.PantryApi
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

    fun init(context: Context) {
        session = SessionStore(context.applicationContext)
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
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PantryApi::class.java)

        Log.i(TAG, "Retrofit initialised against ${BuildConfig.API_BASE_URL}")
    }
}

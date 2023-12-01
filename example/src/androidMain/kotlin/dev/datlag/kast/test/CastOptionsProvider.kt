package dev.datlag.kast.test

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class CastOptionsProvider : OptionsProvider {
    override fun getAdditionalSessionProviders(p0: Context): MutableList<SessionProvider>? {
        return null
    }

    override fun getCastOptions(p0: Context): CastOptions {
        return CastOptions.Builder()
            .setResumeSavedSession(false)
            .setStopReceiverApplicationWhenEndingSession(true)
            .setEnableReconnectionService(true)
            .setRemoteToLocalEnabled(true)
            .setReceiverApplicationId("A12D4273")
            .build()
    }
}
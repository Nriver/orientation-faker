/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.util

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.State
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mm2d.android.orientationfaker.BuildConfig
import java.net.URL
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object AdMob {
    private const val UNIT_ID_SETTINGS = "ca-app-pub-3057634395460859/5509364941"
    private const val UNIT_ID_DETAILED = "ca-app-pub-3057634395460859/9578179809"
    private const val PUBLISHER_ID = "pub-3057634395460859"
    private const val PRIVACY_POLICY_URL =
        "https://github.com/ohmae/orientation-faker/blob/develop/PRIVACY-POLICY.md"
    private var checked: Boolean = false
    private var isInEeaOrUnknown: Boolean = false
    private var confirmed: Boolean = false
    private var consentStatus: ConsentStatus? = null
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        if (BuildConfig.DEBUG) {
            ConsentInformation.getInstance(context)
                .debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                confirmed = false
            }
        })
    }

    fun makeSettingsAdView(context: Context): AdView = AdView(context).apply {
        adSize = AdSize.MEDIUM_RECTANGLE
        adUnitId = UNIT_ID_SETTINGS
    }

    fun makeDetailedAdView(context: Context, width: Int): AdView = AdView(context).apply {
        val density = context.resources.displayMetrics.density
        adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (width / density).toInt())
        adUnitId = UNIT_ID_DETAILED
    }

    fun loadAd(activity: ComponentActivity, adView: AdView) {
        scope.launch {
            loadAd(adView, loadAndConfirmConsentState(activity))
        }
    }

    fun isInEeaOrUnknown(): Boolean = checked && isInEeaOrUnknown

    fun updateConsent(activity: ComponentActivity) {
        showConsentForm(activity)
    }

    private fun loadAd(adView: AdView, consent: ConsentStatus?) {
        val request = when (consent) {
            ConsentStatus.PERSONALIZED -> {
                AdRequest.Builder().build()
            }
            else -> {
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
                    .build()
            }
        }
        adView.loadAd(request)
    }

    private suspend fun loadAndConfirmConsentState(activity: ComponentActivity): ConsentStatus =
        suspendCoroutine { continuation ->
            if (notifyOrConfirm(activity, continuation)) {
                return@suspendCoroutine
            }
            val consentInformation = ConsentInformation.getInstance(activity)
            consentInformation.requestConsentInfoUpdate(
                arrayOf(PUBLISHER_ID),
                object : ConsentInfoUpdateListener {
                    override fun onConsentInfoUpdated(status: ConsentStatus?) {
                        checked = true
                        consentStatus = status
                        isInEeaOrUnknown = consentInformation.isRequestLocationInEeaOrUnknown
                        notifyOrConfirm(activity, continuation)
                    }

                    override fun onFailedToUpdateConsentInfo(reason: String?) {
                        continuation.resume(ConsentStatus.UNKNOWN)
                    }
                })
        }

    private fun notifyOrConfirm(
        activity: ComponentActivity,
        continuation: Continuation<ConsentStatus>
    ): Boolean {
        if (!checked) {
            return false
        }
        if (!isInEeaOrUnknown) {
            continuation.resume(ConsentStatus.PERSONALIZED)
            return true
        }
        if (confirmed) {
            continuation.resume(consentStatus ?: ConsentStatus.UNKNOWN)
            return true
        }
        confirmed = true
        when (val status = consentStatus) {
            ConsentStatus.NON_PERSONALIZED,
            ConsentStatus.PERSONALIZED -> {
                continuation.resume(status)
            }
            ConsentStatus.UNKNOWN,
            null -> {
                try {
                    showConsentForm(activity, continuation)
                } catch (e: Throwable) {
                    continuation.resumeWithException(e)
                }
            }
        }
        return true
    }

    fun ComponentActivity.isActive(): Boolean =
        !isFinishing && lifecycle.currentState.isAtLeast(State.STARTED)

    private fun showConsentForm(
        activity: ComponentActivity,
        continuation: Continuation<ConsentStatus>? = null
    ) {
        var form: ConsentForm? = null
        val listener = object : ConsentFormListener() {
            override fun onConsentFormLoaded() {
                if (activity.isActive()) {
                    form?.show()
                } else {
                    continuation?.resume(ConsentStatus.UNKNOWN)
                }
            }

            override fun onConsentFormOpened() = Unit

            override fun onConsentFormClosed(status: ConsentStatus?, userPrefersAdFree: Boolean?) {
                consentStatus = status
                continuation?.resume(status ?: ConsentStatus.UNKNOWN)
            }

            override fun onConsentFormError(errorDescription: String?) {
                continuation?.resume(ConsentStatus.UNKNOWN)
            }
        }
        form = ConsentForm.Builder(activity, URL(PRIVACY_POLICY_URL))
            .withListener(listener)
            .withPersonalizedAdsOption()
            .withNonPersonalizedAdsOption()
            .build()
        form?.load()
    }
}

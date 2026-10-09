package com.example.documentscanner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

class AppOpenAdManager(
    private val context: Context
) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0

    // Test Ad Unit ID - Replace with your actual ad unit ID
    private val adUnit = "ca-app-pub-8738300695122947/9736593056"

    /** Load an app open ad */
    fun loadAd() {
        // Do not load ad if already loading or showing
        if (isLoadingAd || isShowingAd) {
            return
        }

        isLoadingAd = true

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adUnit,
            request,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(TAG, "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.d(TAG, "Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /** Check if ad is available */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /** Show the ad if available - Only for cold starts */
    fun showAdIfAvailable(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        // Show ad only if available and not already showing
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing")
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "Ad not available")
            onAdFailed()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false
                appOpenAd = null
                isShowingAd = false
                Log.d(TAG, "Ad dismissed")

                // Call the callback to navigate to main content
                onAdDismissed()

                // Load a new ad for next app start
                Handler(Looper.getMainLooper()).postDelayed({
                    loadAd()
                }, 1000)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.d(TAG, "Ad failed to show: ${adError.message}")

                // Call the callback to navigate to main content
                onAdFailed()

                // Load a new ad for next app start
                Handler(Looper.getMainLooper()).postDelayed({
                    loadAd()
                }, 1000)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Ad showed successfully")
            }
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    /** Helper method to check if ad was loaded recently */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    companion object {
        private const val TAG = "AppOpenAdManager"
    }
}

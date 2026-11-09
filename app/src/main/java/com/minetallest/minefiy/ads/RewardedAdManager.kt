package com.minetallest.minefiy.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val activity: Activity) {

    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-4116414893100221/8897822908"

    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    fun showRewardedAd(onRewardEarned: () -> Unit) {
        rewardedAd?.let {
            it.show(activity, OnUserEarnedRewardListener {
                onRewardEarned()
            })
            rewardedAd = null // Ad can only be shown once
            loadRewardedAd() // Pre-load the next ad
        } ?: run {
            loadRewardedAd()
        }
    }
}
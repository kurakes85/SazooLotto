package com.example.sazoolotto

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.sazoolotto.ui.SazooLottoApp
import com.example.sazoolotto.ui.theme.SazooLottoTheme

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : ComponentActivity() {

    private var rewardedAd: RewardedAd? = null
    private final val TAG = "SazooAds"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 광고 초기화
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            Log.e(TAG, "AdMob Init Error: ${e.message}")
        }

        // 2. 광고 미리 로드
        loadRewardedAd()

        setContent {
            SazooLottoTheme {
                SazooLottoApp(
                    onShowAd = { onReward ->
                        showRewardedAd(onReward)
                    }
                )
            }
        }
    }

    private fun loadRewardedAd() {
        val adUnitId = "ca-app-pub-3940256099942544/5224354917"
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(this, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Ad failed to load: ${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd(onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                }
            }

            rewardedAd?.show(this, OnUserEarnedRewardListener {
                onRewardEarned()
            })
        } else {
            Toast.makeText(this, "광고를 불러오는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }
}
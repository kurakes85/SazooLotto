package com.example.sazoolotto

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null

    // Google 공식 테스트 ID
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    init {
        MobileAds.initialize(context) { }
        loadRewardedAd()
    }

    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("AdManager", "광고 로드 실패: ${adError.message}")
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("AdManager", "광고 로드 성공")
                    rewardedAd = ad
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AdManager", "광고 표시 실패: ${adError.message}")
                    onAdDismissed()
                }
            }

            ad.show(activity) {
                // 유저가 보상을 받을 조건을 충족했을 때 호출
                onRewardEarned()
            }
        } else {
            // 광고가 아직 로드되지 않은 경우: UX를 위해 그냥 보상 지급
            onRewardEarned()
        }
    }
}

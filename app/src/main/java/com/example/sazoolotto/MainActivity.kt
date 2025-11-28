package com.example.sazoolotto

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.sazoolotto.ui.theme.SazooLottoTheme // 붉은 줄 뜨면 본인 테마명으로 수정
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

        // 1. 애드몹 초기화
        MobileAds.initialize(this) {}

        // 2. 광고 미리 로드
        loadRewardedAd()

        setContent {
            // 프로젝트 테마 이름 확인 필요 (예: SazooLottoTheme)
            SazooLottoTheme {
                // UI 실행 시, 광고 보여주는 함수(showRewardedAd)를 전달
                SazooLottoApp(
                    onShowAd = { onReward ->
                        showRewardedAd(onReward)
                    }
                )
            }
        }
    }

    // 광고 로드 함수
    private fun loadRewardedAd() {
        // 테스트용 광고 ID (배포 시 실제 ID로 교체 필수)
        val adUnitId = "ca-app-pub-3940256099942544/5224354917"

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    // 광고 출력 함수
    private fun showRewardedAd(onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.")
                    rewardedAd = null
                    loadRewardedAd() // 닫으면 다음 광고 로드
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    rewardedAd = null
                }
            }

            rewardedAd?.show(this, OnUserEarnedRewardListener {
                Log.d(TAG, "User earned the reward.")
                // 보상 지급 콜백 실행
                onRewardEarned()
            })
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            Toast.makeText(this, "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            // 테스트 편의를 위해 광고 로드 실패 시에도 보상을 주려면 아래 주석 해제
            // onRewardEarned()
        }
    }
}
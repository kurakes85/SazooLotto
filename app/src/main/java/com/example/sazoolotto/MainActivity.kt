package com.example.sazoolotto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sazoolotto.logic.LottoResult
import com.example.sazoolotto.logic.SazooEngine
import com.example.sazoolotto.ui.SazooLottoScreen

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager
    private val engine = SazooEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adManager = AdManager(this)

        setContent {
            var currentResult by remember { mutableStateOf<LottoResult?>(null) }

            SazooLottoScreen(
                currentResult = currentResult,
                onUpdateResult = { newResult ->
                    currentResult = newResult
                },
                onAdRequest = { step, result ->
                    adManager.showRewardedAd(
                        activity = this@MainActivity,
                        onRewardEarned = {
                            val nextResult = engine.applyStep(result, step + 1)
                            currentResult = nextResult
                            Toast.makeText(
                                this,
                                "운세 정제 완료!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onAdDismissed = {
                            // 필요하면 여기에서 "광고를 끝까지 보셔야 정제가 완료됩니다" 같은 안내 가능
                        }
                    )
                }
            )
        }
    }
}

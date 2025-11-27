package com.example.sazoolotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.sazoolotto.ui.theme.SazooLottoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SazooLottoTheme {
                SazooLottoApp()
            }
        }
    }
}

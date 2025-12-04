package com.example.sazoolotto.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sazoolotto.logic.LottoResult
import com.example.sazoolotto.logic.SajuInfo
import com.example.sazoolotto.logic.SazooEngine
import com.example.sazoolotto.ui.components.BigAdStepButton
import com.example.sazoolotto.ui.components.BigResultDisplayCard
import com.example.sazoolotto.ui.components.BigSajuInputCard
import com.example.sazoolotto.ui.components.LastLottoResultCard
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SazooLottoScreen(
    currentResult: LottoResult?,
    onUpdateResult: (LottoResult?) -> Unit,
    onAdRequest: (Int, LottoResult) -> Unit
) {
    val context = LocalContext.current
    val engine = remember { SazooEngine() }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "사주로또",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF3F51B5)
                ),
                actions = {
                    TextButton(onClick = { onUpdateResult(null) }) {
                        Text(
                            "처음으로",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            LastLottoResultCard()

            if (currentResult == null) {
                BigSajuInputCard { y, m, d, gender, time ->
                    try {
                        val date = LocalDate.of(y, m, d)
                        val info = SajuInfo(
                            birthDate = date,
                            birthTimeSection = time,
                            gender = gender
                        )
                        val result = engine.generateInitialResult(info)
                        onUpdateResult(result)
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("생년월일을 다시 한 번 확인해 주세요.")
                        }
                    }
                }
            } else {
                BigResultDisplayCard(
                    result = currentResult,
                    onShareClick = { text ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                "공유하기"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                BigAdStepButton(
                    currentStep = currentResult.step,
                    onAdWatchClick = { step ->
                        onAdRequest(step, currentResult)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "※ 본 서비스는 오락용 참고 자료입니다.\n실제 복권 구입과 투자는 본인의 책임입니다.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

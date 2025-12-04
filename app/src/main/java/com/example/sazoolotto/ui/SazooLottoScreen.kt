package com.example.sazoolotto.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sazoolotto.logic.*
import com.example.sazoolotto.ui.components.*
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
    val realLottoManager = remember { RealLottoManager() } // [추가] 네트워크 매니저
    val scrollState = rememberScrollState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // [추가] 실제 로또 번호 상태 (초기엔 null)
    var realLottoData by remember { mutableStateOf<LottoApiResponse?>(null) }

    // [추가] 앱 켜지면 서버에서 로또 번호 가져오기
    LaunchedEffect(Unit) {
        val data = realLottoManager.fetchLatestLotto()
        if (data != null) {
            realLottoData = data
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("사주로또", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF3F51B5)),
                actions = { TextButton(onClick = { onUpdateResult(null) }) { Text("처음으로", fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(scrollState)) {
            Spacer(modifier = Modifier.height(8.dp))

            // [수정] 서버에서 가져온 데이터가 있으면 보여주고, 없으면 로딩 중 표시 or 빈칸
            if (realLottoData != null) {
                val data = realLottoData!!
                LastLottoResultCard(
                    round = data.round,
                    date = data.date ?: "????-??-??",
                    numbers = listOf(data.no1, data.no2, data.no3, data.no4, data.no5, data.no6),
                    bonus = data.bonus
                )
            } else {
                // 로딩 중일 때 임시 카드 (혹은 비워둬도 됨)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("최신 당첨 정보를 불러오는 중...", color = Color.Gray)
                    }
                }
            }

            if (currentResult == null) {
                BigSajuInputCard(onCalculateClick = { y, m, d, gender, time ->
                    try {
                        val date = LocalDate.of(y, m, d)
                        val info = SajuInfo(date, time, gender)
                        onUpdateResult(engine.generateInitialResult(info))
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("생년월일을 다시 한 번 확인해 주세요.")
                        }
                    }
                })
            } else {
                BigResultDisplayCard(result = currentResult, onShareClick = { text ->
                    val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, text); type = "text/plain" }
                    context.startActivity(Intent.createChooser(sendIntent, "공유하기"))
                })
                Spacer(modifier = Modifier.height(16.dp))
                BigAdStepButton(currentStep = currentResult.step, onAdWatchClick = { step -> onAdRequest(step, currentResult) })
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("※ 본 서비스는 오락용 참고 자료입니다.\n실제 투자는 본인의 책임입니다.", modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
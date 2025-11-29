package com.example.sazoolotto.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// 🔥 [중요] 충돌을 막기 위해 내부 패키지 import는 모두 제거하고,
// AlarmReceiver와 외부 라이브러리만 남겼습니다.
import com.example.sazoolotto.AlarmReceiver

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.random.Random

// ---------------------- 데이터 저장소 ----------------------
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_saju_data")

// ---------------------- 색상 정의 ----------------------
val FireAccent = Color(0xFFFF5722)
val PencilDark = Color(0xFF37474F)
val PencilLight = Color(0xFF78909C)
val PencilLine = Color(0xFFCFD8DC)
val SoftGold = Color(0xFFFFECB3)
val DisabledGrey = Color(0xFFE0E0E0)
val KakaoYellow = Color(0xFFFEE500)
val BadLuckRed = Color(0xFFE57373)
val OfficialBlue = Color(0xFF1976D2)

val BallYellow = Color(0xFFFBC400)
val BallBlue = Color(0xFF69C8F2)
val BallRed = Color(0xFFFF7272)
val BallGray = Color(0xFFAAAAAA)
val BallGreen = Color(0xFFB0D840)

data class UserPreferences(
    val version: Int,
    val birthDate: String?,
    val birthTime: String?,
    val gender: String?,
    val lastDate: String,
    val drawCount: Int
)

// ---------------------- 앱 UI 진입점 -----------------------------
@Composable
fun SazooLottoApp(onShowAd: (() -> Unit) -> Unit) {
    val elementColor: Color = FireAccent
    Scaffold(
        topBar = { SazooTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            SazooLottoScreen(elementColor = elementColor, onShowAd = onShowAd)
        }
    }
}

@Composable
private fun SazooTopBar() {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "SazooLotto", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PencilDark)
            Text(text = "정통 사주 & 액운 타파 로또", style = MaterialTheme.typography.bodySmall, color = PencilLight)
        }
    }
}

// ---------------------- 메인 화면 -----------------------------
sealed class LottoLoadState {
    object Loading : LottoLoadState()
    data class Success(val result: RealLottoEngine.LottoResult) : LottoLoadState()
    object Error : LottoLoadState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SazooLottoScreen(elementColor: Color, onShowAd: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    // 알림 권한
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = { if (it) scheduleDailyAlarm(context) })
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scheduleDailyAlarm(context)
    }

    // 광고 관리
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }
    LaunchedEffect(Unit) { loadAd() }

    fun showAd(onReward: () -> Unit) {
        if (rewardedAd != null && activity != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { rewardedAd = null; loadAd() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { rewardedAd = null }
            }
            rewardedAd?.show(activity, OnUserEarnedRewardListener { onReward() })
        } else {
            Toast.makeText(context, "광고 준비 중입니다. 잠시 후 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadAd()
        }
    }

    // 실제 로또
    var lottoState by remember { mutableStateOf<LottoLoadState>(LottoLoadState.Loading) }
    fun fetchLottoData() {
        scope.launch {
            lottoState = LottoLoadState.Loading
            if (!RealLottoEngine.isNetworkAvailable(context)) {
                lottoState = LottoLoadState.Error
            } else {
                val result = RealLottoEngine.fetchLatestLotto()
                lottoState = if (result != null) LottoLoadState.Success(result) else LottoLoadState.Error
            }
        }
    }
    LaunchedEffect(Unit) { fetchLottoData() }

    // 새로고침
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    fun onRefresh() {
        isRefreshing = true
        scope.launch {
            fetchLottoData()
            loadAd()
            delay(1000); isRefreshing = false
            Toast.makeText(context, "정보 갱신 완료", Toast.LENGTH_SHORT).show()
        }
    }

    // 사용자 정보
    var birthDateText by remember { mutableStateOf("생년월일 선택") }
    var birthTimeText by remember { mutableStateOf("태어난 시간 선택") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var birthTime by remember { mutableStateOf<LocalTime?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }

    var zodiacText by remember { mutableStateOf<String?>(null) }
    var dayGanjiText by remember { mutableStateOf<String?>(null) }

    // 상태 관리
    var showResult by remember { mutableStateOf(false) }
    var canDrawToday by remember { mutableStateOf(true) }
    var drawCount by remember { mutableStateOf(0) }
    var isInfoConfirmed by remember { mutableStateOf(false) }

    // 결과 데이터
    var fortuneTitle by remember { mutableStateOf("") }
    var fortuneBody by remember { mutableStateOf("") }
    var lottoNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var lottoHistory by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

    var singleBadNumber by remember { mutableStateOf<Int?>(null) }
    var threeBadNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var specialFortune by remember { mutableStateOf<Pair<String, String>?>(null) }

    var myElement by remember { mutableStateOf(FiveElement.FIRE) }
    val activeElementColor = myElement.color
    val activeElementEmoji = myElement.emoji

    val isInputValid = birthDate != null && birthTime != null && gender != null

    // 데이터 로드
    LaunchedEffect(Unit) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt() else packageInfo.versionCode

        context.dataStore.data.map { prefs ->
            val savedVersion = prefs[intPreferencesKey("app_version_code")] ?: -1
            val dStr = prefs[stringPreferencesKey("birth_date")]
            val tStr = prefs[stringPreferencesKey("birth_time")]
            val gStr = prefs[stringPreferencesKey("gender")]
            val lastDate = prefs[stringPreferencesKey("last_draw_date")] ?: ""
            val savedCount = prefs[intPreferencesKey("saved_draw_count")] ?: 0
            UserPreferences(savedVersion, dStr, tStr, gStr, lastDate, savedCount)
        }.collect { data ->
            if (data.version != currentVersionCode) {
                context.dataStore.edit { it.clear(); it[intPreferencesKey("app_version_code")] = currentVersionCode }
            } else {
                if (data.birthDate != null && data.birthTime != null && data.gender != null) {
                    val d = LocalDate.parse(data.birthDate); val t = LocalTime.parse(data.birthTime)
                    birthDate = d; birthTime = t; gender = data.gender
                    birthDateText = d.toString()
                    birthTimeText = String.format("%02d:%02d 태생", t.hour, t.minute)

                    val sajuInfo = SazooEngine.calculateSaju(d, t)
                    zodiacText = "${sajuInfo.yearGanji} (${sajuInfo.zodiac})"
                    dayGanjiText = sajuInfo.dayGanji
                    isInfoConfirmed = true
                }

                if (data.lastDate == LocalDate.now().toString()) {
                    drawCount = data.drawCount
                    if (drawCount > 0) { showResult = true; canDrawToday = false }
                } else {
                    drawCount = 0; canDrawToday = true; showResult = false
                }
            }
        }
    }

    fun saveUserData(newCount: Int) {
        if (isInputValid) {
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey("birth_date")] = birthDate.toString()
                    prefs[stringPreferencesKey("birth_time")] = birthTime.toString()
                    prefs[stringPreferencesKey("gender")] = gender!!
                    prefs[stringPreferencesKey("last_draw_date")] = LocalDate.now().toString()
                    prefs[intPreferencesKey("saved_draw_count")] = newCount
                }
            }
        }
    }

    fun resetUserData() {
        birthDate = null; birthTime = null; gender = null
        birthDateText = "생년월일 선택"; birthTimeText = "태어난 시간 선택"
        zodiacText = null; dayGanjiText = null
        isInfoConfirmed = false; showResult = false; drawCount = 0
        lottoHistory = emptyList(); lottoNumbers = emptyList()
        scope.launch { context.dataStore.edit { it.clear() } }
        Toast.makeText(context, "정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    fun processDraw() {
        if (!isInputValid) return
        if (lottoNumbers.isNotEmpty()) lottoHistory = (listOf(lottoNumbers) + lottoHistory).take(10)

        val nextCount = if(drawCount == 0) 1 else drawCount + 1
        drawCount = nextCount
        saveUserData(drawCount)

        val saju = SazooEngine.calculateSaju(birthDate!!, birthTime!!)
        myElement = saju.dayElement
        zodiacText = "${saju.yearGanji} (${saju.zodiac})"
        dayGanjiText = saju.dayGanji

        val excludeList = mutableListOf<Int>()
        if (drawCount >= 2) {
            if (singleBadNumber == null) singleBadNumber = SazooEngine.getBadLuckNumber(birthDate, emptyList())
            excludeList.add(singleBadNumber!!)
        }
        if (drawCount >= 3) {
            if (threeBadNumbers.isEmpty()) threeBadNumbers = SazooEngine.getThreeBadNumbers(birthDate, listOf(singleBadNumber ?: 0))
            excludeList.addAll(threeBadNumbers)
        }
        if (drawCount >= 4) {
            specialFortune = SazooEngine.getSpecialFortune(myElement, birthDate!!)
        }

        val fortunePair = SazooEngine.generateFortune(birthDate, birthTime, gender, drawCount)
        fortuneTitle = fortunePair.first
        fortuneBody = fortunePair.second
        lottoNumbers = SazooEngine.generateLottoNumbers(birthDate, gender, drawCount, excludeList)

        showResult = true
        canDrawToday = false
    }

    fun confirmAndDraw() {
        if (isInputValid) {
            isInfoConfirmed = true
            if (drawCount == 0) processDraw()
            else {
                saveUserData(drawCount)
                val saju = SazooEngine.calculateSaju(birthDate!!, birthTime!!)
                zodiacText = "${saju.yearGanji} (${saju.zodiac})"
                dayGanjiText = saju.dayGanji
                showResult = true
                Toast.makeText(context, "정보 확인 완료!", Toast.LENGTH_SHORT).show()
            }
        } else Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
    }

    fun shareResult() {
        val sb = StringBuilder()
        sb.append("🔮 [SazooLotto] 오늘의 운세 & 로또\n📅 ${LocalDate.now()}\n\n👑 추천 번호: ${lottoNumbers.joinToString(", ")}\n")
        if (drawCount >= 2 && singleBadNumber != null) sb.append("🚫 액운 번호: $singleBadNumber\n")
        if (drawCount >= 3 && threeBadNumbers.isNotEmpty()) sb.append("💀 악재 숫자: ${threeBadNumbers.joinToString(", ")}\n")
        if (specialFortune != null) sb.append("\n🎁 [프리미엄 운세]\n${specialFortune!!.second}\n")
        else sb.append("\n📜 [운세]\n$fortuneBody\n")
        sb.append("\n⬇️ 앱 다운로드\nhttps://play.google.com/store/apps/details?id=com.example.sazoolotto\n")
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, sb.toString()) }
        context.startActivity(Intent.createChooser(intent, "공유하기"))
    }

    // --- UI ---
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { onRefresh() }, state = pullRefreshState, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. 실제 당첨 번호
            when (lottoState) {
                is LottoLoadState.Success -> RealLottoCard(result = (lottoState as LottoLoadState.Success).result)
                is LottoLoadState.Error -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), border = BorderStroke(1.dp, Color.Red)) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("⚠️ 인터넷 연결 필요", style = MaterialTheme.typography.bodySmall, color = Color.Red) } }
                is LottoLoadState.Loading -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), border = BorderStroke(1.dp, Color.LightGray)) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("🔄 최신 당첨번호 확인 중...", style = MaterialTheme.typography.bodySmall, color = Color.Gray) } }
            }

            // 2. 입력 카드
            UserInputCard(birthDateText, birthTimeText,
                {
                    val cal = Calendar.getInstance()
                    val dY = birthDate?.year ?: cal.get(Calendar.YEAR)
                    val dM = (birthDate?.monthValue ?: (cal.get(Calendar.MONTH) + 1)) - 1
                    val dD = birthDate?.dayOfMonth ?: cal.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(context, { _, y, m, d ->
                        val selectedDate = LocalDate.of(y, m + 1, d); birthDate = selectedDate; birthDateText = selectedDate.toString()
                    }, dY, dM, dD).show()
                },
                {
                    val cal = Calendar.getInstance()
                    val dH = birthTime?.hour ?: cal.get(Calendar.HOUR_OF_DAY)
                    val dMin = birthTime?.minute ?: cal.get(Calendar.MINUTE)
                    TimePickerDialog(context, { _, h, min ->
                        val selectedTime = LocalTime.of(h, min)
                        birthTime = selectedTime
                        birthTimeText = String.format("%02d:%02d 태생", h, min)
                    }, dH, dMin, false).show()
                },
                gender, { gender = it }, activeElementColor, zodiacText, dayGanjiText,
                isInputValid, { resetUserData() }, { confirmAndDraw() }
            )

            // 3. 결과 화면
            if (showResult || drawCount > 0) {
                if (specialFortune != null) SpecialFortuneCard(title = specialFortune!!.first, body = specialFortune!!.second, color = Color(0xFFE1BEE7))

                if (drawCount >= 3 && threeBadNumbers.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), border = BorderStroke(1.dp, BadLuckRed)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💀 오늘의 악재 숫자 (3개)", fontWeight = FontWeight.Bold, color = BadLuckRed)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { threeBadNumbers.forEach { num -> RealLottoBall(number = num) } }
                            Text("※ 위 숫자는 제외되었습니다.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                if (drawCount >= 2 && singleBadNumber != null) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9)), border = BorderStroke(1.dp, Color.Gray)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 24.sp); Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("액운 번호 발견!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PencilDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("번호: ", style = MaterialTheme.typography.bodySmall); RealLottoBall(number = singleBadNumber!!); Text(" (제외됨)", style = MaterialTheme.typography.bodySmall, color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (fortuneTitle.isNotEmpty()) FortuneCard(activeElementColor, activeElementEmoji, fortuneTitle, fortuneBody, myElement)

                val lottoTitle = when {
                    drawCount <= 1 -> "오늘의 추천 번호 (기본)"
                    drawCount == 2 -> "추천 번호 (액운 제외됨)"
                    drawCount == 3 -> "추천 번호 (악재 제외됨)"
                    else -> "추천 번호 (프리미엄)"
                }

                if (lottoNumbers.isNotEmpty()) LottoCard(activeElementColor, lottoTitle, lottoNumbers)

                if (lottoHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = PencilLine); Text(" 이전 기록 ", style = MaterialTheme.typography.bodySmall, color = PencilLight); HorizontalDivider(modifier = Modifier.weight(1f), color = PencilLine)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    lottoHistory.forEachIndexed { index, numbers -> LottoCard(elementColor = Color.LightGray, title = "이전 결과", numbers = numbers); Spacer(modifier = Modifier.height(8.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (drawCount < 4) {
                    OutlinedButton(
                        onClick = { onShowAd { processDraw() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = activeElementColor)
                    ) {
                        val btnText = when(drawCount) {
                            1 -> "📺 광고 보고 액운번호 제외하기"
                            2 -> "📺 광고(2/3) 보고 악재 숫자 확인하기"
                            3 -> "📺 광고(3/3) 보고 프리미엄 운세 확인"
                            else -> ""
                        }
                        Text(btnText, fontWeight = FontWeight.Bold)
                    }
                }

                if (drawCount >= 4) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { shareResult() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = KakaoYellow, contentColor = Color.Black)) {
                        Text("📤 카카오톡/문자로 전체 결과 공유하기", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ---------------------- UI 컴포넌트 ----------------------
@Composable
fun UserInputCard(birthDateText: String, birthTimeText: String, onBirthDateClick: () -> Unit, onBirthTimeClick: () -> Unit, gender: String?, onGenderChange: (String?) -> Unit, elementColor: Color, zodiacText: String?, dayGanjiText: String?, isInputValid: Boolean, onReset: () -> Unit, onConfirm: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, PencilLine)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✏️ 내 사주 정보 (필수 입력)", style = MaterialTheme.typography.titleMedium, color = PencilDark)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("생년월일 및 태어난 시간", style = MaterialTheme.typography.bodySmall, color = PencilLight)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBirthDateClick, modifier = Modifier.weight(1f)) { Text(birthDateText, color = PencilDark, fontSize = 12.sp, maxLines = 1) }
                    OutlinedButton(onClick = onBirthTimeClick, modifier = Modifier.weight(1f)) { Text(birthTimeText, color = PencilDark, fontSize = 12.sp, maxLines = 1) }
                }
                if (zodiacText != null && dayGanjiText != null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("띠: $zodiacText", style = MaterialTheme.typography.bodySmall, color = PencilDark)
                        Text("일주: $dayGanjiText", style = MaterialTheme.typography.bodySmall, color = elementColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column {
                Text("성별 (필수 선택)", style = MaterialTheme.typography.bodySmall, color = PencilLight)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = gender == "male", onClick = { onGenderChange("male") }, colors = RadioButtonDefaults.colors(selectedColor = elementColor)); Text("남", color = PencilDark) }
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = gender == "female", onClick = { onGenderChange("female") }, colors = RadioButtonDefaults.colors(selectedColor = elementColor)); Text("여", color = PencilDark) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("초기화") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(isInputValid) elementColor else DisabledGrey, contentColor = if(isInputValid) Color.White else Color.Gray),
                    enabled = isInputValid
                ) { Text("사주로또 확인하기") }
            }
        }
    }
}

@Composable
fun FortuneCard(elementColor: Color, elementEmoji: String, title: String, body: String, myElement: FiveElement) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, PencilLine)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(elementEmoji, fontSize = 20.sp); Spacer(Modifier.width(8.dp))
                Column { Text("오늘의 기운", style = MaterialTheme.typography.labelSmall, color = PencilLight); Text(myElement.koreanName, style = MaterialTheme.typography.labelLarge, color = PencilDark, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f)); Box(modifier = Modifier.height(2.dp).width(40.dp).background(elementColor))
            }
            HorizontalDivider(color = PencilLine.copy(alpha = 0.5f))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PencilDark)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = PencilLight)
        }
    }
}

@Composable
fun SpecialFortuneCard(title: String, body: String, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), border = BorderStroke(2.dp, color)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔮", fontSize = 24.sp); Spacer(Modifier.width(8.dp))
                Column { Text("HIDDEN 프리미엄 운세", style = MaterialTheme.typography.labelSmall, color = Color.Magenta, fontWeight = FontWeight.Bold); Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PencilDark) }
            }
            HorizontalDivider(color = color.copy(alpha = 0.5f))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = PencilDark, lineHeight = 22.sp)
        }
    }
}

@Composable
fun RealLottoBall(number: Int, isBonus: Boolean = false) {
    val color = when (number) { in 1..10 -> BallYellow; in 11..20 -> BallBlue; in 21..30 -> BallRed; in 31..40 -> BallGray; else -> BallGreen }
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Text("$number", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) } }
}

@Composable
fun RealLottoCard(result: RealLottoEngine.LottoResult) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, OfficialBlue), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 지난주 당첨 번호 (${result.drwNo}회)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OfficialBlue)
            Text("추첨일: ${result.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                result.numbers.forEach { n -> RealLottoBall(number = n); Spacer(modifier = Modifier.width(4.dp)) }
                Spacer(modifier = Modifier.width(8.dp)); Text("+", modifier = Modifier.align(Alignment.CenterVertically), fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp))
                RealLottoBall(number = result.bonus, isBonus = true)
            }
        }
    }
}

@Composable
fun LottoCard(elementColor: Color, title: String, numbers: List<Int>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, PencilLine)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PencilDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                numbers.forEach { n -> RealLottoBall(number = n) }
            }
        }
    }
}

// ---------------------- 알림 설정 ----------------------
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("daily_saju_channel", "SazooLotto Daily", NotificationManager.IMPORTANCE_DEFAULT)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
fun scheduleDailyAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
    }
    try { alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent) } catch (e: SecurityException) { Log.e("Alarm", "Perm Error") }
}

object RealLottoEngine {
    data class LottoResult(val drwNo: Int, val date: String, val numbers: List<Int>, val bonus: Int)
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val actNw = cm.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    suspend fun fetchLatestLotto(): LottoResult? = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.of(2002, 12, 7)
            val now = LocalDateTime.now()
            val diffDays = ChronoUnit.DAYS.between(startDate, now.toLocalDate())
            var round = (diffDays / 7).toInt() + 1
            if (now.dayOfWeek == DayOfWeek.SATURDAY && now.hour < 21) round -= 1
            getLottoFromApi(round) ?: getLottoFromApi(round - 1)
        } catch (e: Exception) { e.printStackTrace(); null }
    }
    private fun getLottoFromApi(round: Int): LottoResult? {
        return try {
            val url = URL("https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=$round")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                if (json.getString("returnValue") == "success") {
                    val nums = listOf(json.getInt("drwtNo1"), json.getInt("drwtNo2"), json.getInt("drwtNo3"), json.getInt("drwtNo4"), json.getInt("drwtNo5"), json.getInt("drwtNo6"))
                    LottoResult(round, json.getString("drwNoDate"), nums, json.getInt("bnusNo"))
                } else null
            } else null
        } catch (e: Exception) { null }
    }
}

// ---------------------- SazooEngine (대용량 DB 포함) ----------------------
enum class FiveElement(val koreanName: String, val emoji: String, val color: Color, val luckyNumbers: List<Int>) {
    WOOD("목(나무)", "🌿", Color(0xFF66A86E), listOf(3, 8)),
    FIRE("화(불)", "🔥", FireAccent, listOf(2, 7)),
    EARTH("토(흙)", "⛰️", Color(0xFFB59473), listOf(5, 0)),
    METAL("금(쇠)", "💎", Color(0xFF90A4AE), listOf(4, 9)),
    WATER("수(물)", "🌊", Color(0xFF4FC3F7), listOf(1, 6))
}

data class SajuInfo(val yearGanji: String, val zodiac: String, val dayGanji: String, val dayElement: FiveElement)

object SazooEngine {
    // 🔥 [주의] 여기에 이전에 드린 SazooEngine 전체 코드를 꼭 붙여넣으세요! (이전 코드 복사)
    // (calculateSaju, getBadLuckNumber, getThreeBadNumbers, getSpecialFortune, generateFortune, generateLottoNumbers 등 모든 함수 포함)
    // (분량상 생략되었지만 이전 답변의 코드를 그대로 쓰시면 됩니다)

    // (임시 함수 - 반드시 이전 코드로 교체하세요!)
    private val CHEONGAN = listOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")
    private val JIJI = listOf("자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해")
    private val ZODIAC_ANIMALS = listOf("쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지")
    private fun getElementFromCheongan(idx: Int): FiveElement { return when (idx) { 0,1->FiveElement.WOOD; 2,3->FiveElement.FIRE; 4,5->FiveElement.EARTH; 6,7->FiveElement.METAL; else->FiveElement.WATER } }
    fun calculateSaju(date: LocalDate, time: LocalTime): SajuInfo {
        val y = date.year
        val yearStem = (y - 4 + 10) % 10
        val yearBranch = (y - 4 + 12) % 12
        val yearGanji = "${CHEONGAN[yearStem]}${JIJI[yearBranch]}년"
        val zodiac = "${ZODIAC_ANIMALS[yearBranch]}띠"
        val refDate = LocalDate.of(2000, 1, 1)
        val days = ChronoUnit.DAYS.between(refDate, date)
        var dayIdx = (54L + days) % 60L
        if (dayIdx < 0) dayIdx += 60
        val dStem = (dayIdx % 10).toInt()
        val element = getElementFromCheongan(dStem)
        val dBranch = (dayIdx % 12).toInt()
        val dayGanji = "${CHEONGAN[dStem]}${JIJI[dBranch]}일"
        return SajuInfo(yearGanji, zodiac, dayGanji, element)
    }
    fun getBadLuckNumber(date: LocalDate?, excludeList: List<Int>): Int { return Random.nextInt(45) + 1 }
    fun getThreeBadNumbers(date: LocalDate?, existingBad: List<Int>): List<Int> { return listOf(1, 2, 3) }
    fun getSpecialFortune(element: FiveElement, birthDate: LocalDate): Pair<String, String> { return "프리미엄" to "내용" }
    fun generateFortune(date: LocalDate?, time: LocalTime?, gender: String?, count: Int): Pair<String, String> { return "운세" to "내용" }
    fun generateLottoNumbers(date: LocalDate?, gender: String?, count: Int, excludeList: List<Int>): List<Int> {
        val random = Random(System.nanoTime())
        val allNumbers = (1..45).filter { !excludeList.contains(it) }.toMutableList()
        allNumbers.shuffle(random)
        return allNumbers.take(6).sorted()
    }
}
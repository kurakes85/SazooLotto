package com.example.sazoolotto

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
import android.os.Build
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sazoolotto.ui.theme.* import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
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

// ---------------------- 앱 UI 진입점 -----------------------------

@Composable
fun SazooLottoApp(
    onShowAd: (() -> Unit) -> Unit
) {
    val elementColor: Color = FireAccent

    Scaffold(
        topBar = { SazooTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            SazooLottoScreen(elementColor = elementColor, onShowAd = onShowAd)
        }
    }
}

@Composable
private fun SazooTopBar() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SazooLotto",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PencilDark
            )
            Text(
                text = "정통 사주 & 액운 타파 로또",
                style = MaterialTheme.typography.bodySmall,
                color = PencilLight
            )
        }
    }
}

// ---------------------- 메인 화면 -----------------------------

@Composable
fun SazooLottoScreen(
    elementColor: Color,
    onShowAd: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    // --- [알림 권한] ---
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> if (isGranted) scheduleDailyAlarm(context) }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- [광고 관리] ---
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
            Toast.makeText(context, "광고 로드 중... (테스트 모드)", Toast.LENGTH_SHORT).show()
            loadAd()
        }
    }

    // --- 사용자 정보 ---
    var birthDateText by remember { mutableStateOf("생년월일 선택") }
    var birthTimeText by remember { mutableStateOf("태어난 시간 선택") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var birthTime by remember { mutableStateOf<LocalTime?>(null) }
    var zodiacText by remember { mutableStateOf<String?>(null) }
    var dayGanjiText by remember { mutableStateOf<String?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }

    // --- 상태 관리 ---
    var showResult by remember { mutableStateOf(false) }
    var canDrawToday by remember { mutableStateOf(true) }
    var drawCount by remember { mutableStateOf(0) }

    // --- 결과 데이터 ---
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

    // --- 데이터 로드 ---
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

            object {
                val version = savedVersion
                val d = dStr; val t = tStr; val g = gStr
                val lDate = lastDate; val sCount = savedCount
            }
        }.collect { data ->
            if (data.version != currentVersionCode) {
                context.dataStore.edit { it.clear(); it[intPreferencesKey("app_version_code")] = currentVersionCode }
            } else {
                if (data.d != null && data.t != null && data.g != null) {
                    val d = LocalDate.parse(data.d); val t = LocalTime.parse(data.t)
                    birthDate = d; birthTime = t; gender = data.g
                    birthDateText = d.toString()
                    birthTimeText = String.format("%02d:%02d 태생", t.hour, t.minute)
                    val sajuInfo = SazooEngine.calculateSaju(d, t)
                    zodiacText = "${sajuInfo.yearGanji} (${sajuInfo.zodiac})"
                    dayGanjiText = sajuInfo.dayGanji
                }

                // 🔓 [테스트 모드] 날짜가 같아도 횟수 제한 없이 진행 가능하게 함
                // 기존에는 if(count>0) showResult=false 등으로 막았지만, 지금은 그냥 둠.
                // 다만 앱 껐다 켰을 때 연속성을 위해 카운트는 불러옴
                if (data.lDate == LocalDate.now().toString()) {
                    drawCount = data.sCount
                    // 테스트 중에는 이미 뽑았어도 계속 뽑을 수 있게 UI 처리함
                } else {
                    drawCount = 0
                    canDrawToday = true
                }
            }
        }
    }

    fun saveUserData(newCount: Int) {
        if (birthDate != null && birthTime != null && gender != null) {
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

    // --- 로또 추첨 ---
    fun processDraw() {
        val nextCount = drawCount + 1
        drawCount = nextCount
        saveUserData(drawCount)

        if (birthDate != null && birthTime != null) {
            val saju = SazooEngine.calculateSaju(birthDate!!, birthTime!!)
            myElement = saju.dayElement
        }

        if (lottoNumbers.isNotEmpty()) lottoHistory = (listOf(lottoNumbers) + lottoHistory).take(10)

        val excludeList = mutableListOf<Int>()
        if (drawCount >= 2) {
            if (singleBadNumber == null) singleBadNumber = SazooEngine.getBadLuckNumber(birthDate, emptyList())
            excludeList.add(singleBadNumber!!)
        }
        if (drawCount >= 3) {
            if (threeBadNumbers.isEmpty()) threeBadNumbers = SazooEngine.getThreeBadNumbers(birthDate, listOf(singleBadNumber ?: 0))
            excludeList.addAll(threeBadNumbers)
        }

        // 🔓 [테스트 모드] 4회 이상이어도 계속 프리미엄 운세 생성
        if (drawCount >= 4 && birthDate != null) {
            specialFortune = SazooEngine.getSpecialFortune(myElement, birthDate!!)
        }

        val fortunePair = SazooEngine.generateFortune(birthDate, birthTime, gender, drawCount)
        fortuneTitle = fortunePair.first
        fortuneBody = fortunePair.second
        lottoNumbers = SazooEngine.generateLottoNumbers(birthDate, gender, drawCount, excludeList)

        showResult = true
        canDrawToday = false
    }

    fun shareResult() {
        val sb = StringBuilder()
        sb.append("🔮 [SazooLotto] 오늘의 운세 & 로또\n📅 ${LocalDate.now()}\n\n👑 추천 번호: ${lottoNumbers.joinToString(", ")}\n")
        if (drawCount >= 2 && singleBadNumber != null) sb.append("🚫 액운 번호: $singleBadNumber\n")
        if (drawCount >= 3 && threeBadNumbers.isNotEmpty()) sb.append("💀 악재 숫자: ${threeBadNumbers.joinToString(", ")}\n")

        if (specialFortune != null) sb.append("\n🎁 [프리미엄 운세]\n${specialFortune!!.second}\n")
        else sb.append("\n📜 [운세]\n$fortuneBody\n")

        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, sb.toString()) }
        context.startActivity(Intent.createChooser(intent, "공유하기"))
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        UserInputCard(birthDateText, birthTimeText,
            {
                val cal = Calendar.getInstance()
                val dY = birthDate?.year ?: cal.get(Calendar.YEAR)
                val dM = (birthDate?.monthValue ?: (cal.get(Calendar.MONTH) + 1)) - 1
                val dD = birthDate?.dayOfMonth ?: cal.get(Calendar.DAY_OF_MONTH)
                DatePickerDialog(context, { _, y, m, d ->
                    val selectedDate = LocalDate.of(y, m + 1, d)
                    birthDate = selectedDate
                    birthDateText = selectedDate.toString()
                    val tempTime = birthTime ?: LocalTime.of(0, 0)
                    val sajuInfo = SazooEngine.calculateSaju(selectedDate, tempTime)
                    zodiacText = "${sajuInfo.yearGanji} (${sajuInfo.zodiac})"
                    dayGanjiText = sajuInfo.dayGanji
                    myElement = sajuInfo.dayElement
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
            gender, { gender = it }, activeElementColor, zodiacText, dayGanjiText
        )

        // 초기 시작 버튼
        if (drawCount == 0) {
            Button(
                onClick = { processDraw() },
                enabled = isInputValid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftGold, contentColor = PencilDark, disabledContainerColor = DisabledGrey)
            ) {
                Text(if (!isInputValid) "사주 정보 입력 필요" else "🔮 오늘의 사주 & 로또 번호 뽑기 (무료)")
            }
        }

        if (showResult || drawCount > 0) {
            // 프리미엄/액운/악재 카드
            if (specialFortune != null) SpecialFortuneCard(title = specialFortune!!.first, body = specialFortune!!.second, color = Color(0xFFE1BEE7))

            if (drawCount >= 3 && threeBadNumbers.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), border = BorderStroke(1.dp, BadLuckRed)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💀 오늘의 악재 숫자 (3개)", fontWeight = FontWeight.Bold, color = BadLuckRed)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { threeBadNumbers.forEach { num -> Surface(shape = MaterialTheme.shapes.extraSmall, color = BadLuckRed, modifier = Modifier.size(32.dp)) { Box(contentAlignment = Alignment.Center) { Text("$num", color = Color.White, fontWeight = FontWeight.Bold) } } } }
                        Text("※ 위 숫자는 이번 추천 번호에서 제외되었습니다.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            if (drawCount >= 2 && singleBadNumber != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9)), border = BorderStroke(1.dp, Color.Gray)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 24.sp); Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("액운 번호 발견!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PencilDark)
                            Text("액운 [ $singleBadNumber ]번을 제외하고 다시 뽑았습니다.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FortuneCard(activeElementColor, activeElementEmoji, fortuneTitle, fortuneBody, myElement)

            val lottoTitle = when {
                drawCount == 1 -> "오늘의 추천 번호 (기본)"
                drawCount == 2 -> "추천 번호 (액운 1개 제외됨)"
                drawCount == 3 -> "추천 번호 (악재 3개 제외됨)"
                else -> "추천 번호 (프리미엄 무제한)"
            }
            LottoCard(activeElementColor, lottoTitle, lottoNumbers)

            if (lottoHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = PencilLine)
                    Text(" 이전 기록 (누적) ", style = MaterialTheme.typography.bodySmall, color = PencilLight)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = PencilLine)
                }
                Spacer(modifier = Modifier.height(8.dp))
                lottoHistory.forEachIndexed { index, numbers ->
                    LottoCard(elementColor = Color.LightGray, title = "이전 결과 (${lottoHistory.size - index}회 전)", numbers = numbers)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔓 [테스트 모드] 광고 버튼 무제한 활성화 (drawCount < 4 조건 제거)
            OutlinedButton(
                onClick = {
                    onShowAd {
                        processDraw()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = activeElementColor)
            ) {
                val btnText = when(drawCount) {
                    1 -> "📺 광고(1/3) 보고 액운 번호 1개 알아내기"
                    2 -> "📺 광고(2/3) 보고 악재 숫자 3개 알아내기"
                    3 -> "📺 광고(3/3) 보고 프리미엄 운세 확인"
                    else -> "📺 [테스트] 광고 보고 계속 뽑기 (무제한)"
                }
                Text(btnText, fontWeight = FontWeight.Bold)
            }

            // 4. 공유 버튼 (4회 이상부터 노출)
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

// ---------------------- UI 컴포넌트 ----------------------
@Composable
fun UserInputCard(birthDateText: String, birthTimeText: String, onBirthDateClick: () -> Unit, onBirthTimeClick: () -> Unit, gender: String?, onGenderChange: (String?) -> Unit, elementColor: Color, zodiacText: String?, dayGanjiText: String?) {
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
fun LottoCard(elementColor: Color, title: String, numbers: List<Int>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, PencilLine)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PencilDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                numbers.forEach { n -> Surface(shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, elementColor), color = MaterialTheme.colorScheme.surface) { Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) { Text(n.toString().padStart(2, '0'), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = PencilDark) } } }
            }
        }
    }
}

// =========================================================================================
// 👑 SazooEngine
// =========================================================================================

enum class FiveElement(val koreanName: String, val emoji: String, val color: Color, val luckyNumbers: List<Int>) {
    WOOD("목(나무)", "🌿", Color(0xFF66A86E), listOf(3, 8)),
    FIRE("화(불)", "🔥", FireAccent, listOf(2, 7)),
    EARTH("토(흙)", "⛰️", Color(0xFFB59473), listOf(5, 0)),
    METAL("금(쇠)", "💎", Color(0xFF90A4AE), listOf(4, 9)),
    WATER("수(물)", "🌊", Color(0xFF4FC3F7), listOf(1, 6))
}

data class SajuInfo(val yearGanji: String, val zodiac: String, val dayGanji: String, val dayElement: FiveElement)

// ... (위쪽 SazooInfo 등 데이터 클래스는 유지) ...

object SazooEngine {
    // ... (CHEONGAN, JIJI, calculateSaju 등 기본 계산 함수는 기존과 동일하게 유지) ...
    private val CHEONGAN = listOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")
    private val JIJI = listOf("자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해")
    private val ZODIAC_ANIMALS = listOf("쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지")

    private fun getElementFromCheongan(idx: Int): FiveElement {
        return when (idx) { 0,1->FiveElement.WOOD; 2,3->FiveElement.FIRE; 4,5->FiveElement.EARTH; 6,7->FiveElement.METAL; else->FiveElement.WATER }
    }

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
        val dBranch = (dayIdx % 12).toInt()
        val dayGanji = "${CHEONGAN[dStem]}${JIJI[dBranch]}일"

        val element = getElementFromCheongan(dStem)
        return SajuInfo(yearGanji, zodiac, dayGanji, element)
    }

    fun getBadLuckNumber(date: LocalDate?, excludeList: List<Int>): Int {
        val today = LocalDate.now().toString()
        val seed = "$date|$today|SINGLE_BAD".hashCode()
        val random = Random(seed)
        var badNum: Int
        do { badNum = random.nextInt(45) + 1 } while (excludeList.contains(badNum))
        return badNum
    }

    fun getThreeBadNumbers(date: LocalDate?, existingBad: List<Int>): List<Int> {
        val today = LocalDate.now().toString()
        val seed = "$date|$today|THREE_BAD".hashCode()
        val random = Random(seed)
        val badList = mutableSetOf<Int>()
        while (badList.size < 3) {
            val num = random.nextInt(45) + 1
            if (!existingBad.contains(num)) badList.add(num)
        }
        return badList.toList().sorted()
    }

    // =========================================================================
    // 🔥 [업데이트] 방대한 운세 데이터베이스 (각 20개 이상)
    // =========================================================================

    // 1. 오늘의 기본 운세 (Basic Fortune) - 오행별 20개
    private fun getBasicFortuneList(element: FiveElement): List<String> {
        return when (element) {
            FiveElement.WOOD -> listOf(
                "새로운 시작의 기운이 강합니다. 미뤄왔던 일을 시작해보세요.",
                "동쪽에서 반가운 소식이 들려올 수 있습니다. 귀를 기울이세요.",
                "유연한 사고가 필요한 날입니다. 고집을 꺾으면 이득이 생깁니다.",
                "대인관계가 원만해지는 날입니다. 먼저 연락해보는 건 어떨까요?",
                "성장을 위한 통증이 있을 수 있습니다. 긍정적으로 받아들이세요.",
                "초록색 소품을 지니면 행운이 따릅니다.",
                "오전 시간에 집중력이 가장 좋습니다. 중요한 일은 오전에 처리하세요.",
                "창의적인 아이디어가 샘솟는 날입니다. 메모하는 습관을 가지세요.",
                "경쟁보다는 협력이 좋은 결과를 가져옵니다.",
                "나무가 물을 만나듯, 주변의 도움이 당신을 성장시킵니다.",
                "조급해하지 마세요. 뿌리 깊은 나무는 바람에 흔들리지 않습니다.",
                "책이나 문서를 가까이하면 길한 기운을 얻습니다.",
                "가벼운 산책이나 등산이 당신의 기운을 북돋아 줍니다.",
                "새로운 인연이 찾아올 수 있습니다. 마음을 열어두세요.",
                "자신감을 가지세요. 당신은 뻗어나갈 준비가 되어 있습니다.",
                "작은 실수는 거름이 됩니다. 너무 자책하지 마세요.",
                "주변 사람들에게 칭찬을 아끼지 마세요. 배가 되어 돌아옵니다.",
                "계획을 세우기에 아주 좋은 날입니다. 다이어리를 펼치세요.",
                "곧은 성품이 빛을 발하는 날입니다. 원칙을 지키세요.",
                "기다리던 결과가 긍정적으로 나타날 것입니다."
            )
            FiveElement.FIRE -> listOf(
                "열정이 넘치는 하루입니다. 당신의 에너지를 마음껏 발산하세요.",
                "화려한 언변으로 사람들을 사로잡을 수 있는 날입니다.",
                "작은 불씨가 크게 번질 수 있으니, 말실수를 조심해야 합니다.",
                "남쪽으로 이동하면 좋은 기운을 만날 수 있습니다.",
                "직관력이 뛰어난 날입니다. 첫 번째 느낌을 믿으세요.",
                "당신의 솔직함이 매력으로 다가가는 날입니다.",
                "붉은색 계열의 옷이나 장신구가 행운을 부릅니다.",
                "성급한 결정은 금물입니다. 한 번만 더 생각하고 움직이세요.",
                "주목받는 일이 생길 수 있습니다. 당당하게 행동하세요.",
                "예술적인 감각이 깨어나는 날입니다. 문화생활을 즐겨보세요.",
                "사랑의 기운이 강합니다. 고백하기 좋은 타이밍일지도 모릅니다.",
                "다혈질적인 모습을 보이지 않도록 감정 조절이 필요합니다.",
                "오후 시간에 활동성이 좋아집니다. 약속은 오후로 잡으세요.",
                "밝은 미소가 최고의 무기입니다. 많이 웃으세요.",
                "새로운 것에 대한 호기심이 당신을 발전시킵니다.",
                "숨기기보다는 드러내는 것이 유리한 날입니다.",
                "명예운이 상승하고 있습니다. 맡은 일에 최선을 다하세요.",
                "주변을 환하게 비추는 태양 같은 존재가 되어보세요.",
                "적극적인 태도가 행운을 가져옵니다.",
                "불필요한 논쟁은 피하는 것이 상책입니다."
            )
            FiveElement.EARTH -> listOf(
                "산처럼 듬직한 모습이 신뢰를 얻는 날입니다.",
                "변동보다는 안정을 추구하는 것이 유리합니다.",
                "재물운이 차곡차곡 쌓이는 형국입니다. 저축하기 좋은 날입니다.",
                "약속을 잘 지키는 것이 오늘의 핵심 포인트입니다.",
                "중재자의 역할을 하게 될 수 있습니다. 공정함을 잃지 마세요.",
                "노란색이나 갈색 계열이 당신을 편안하게 해줍니다.",
                "옛 친구나 지인에게 연락이 올 수 있습니다.",
                "성실함이 최고의 전략입니다. 요행을 바라지 마세요.",
                "위장 건강에 유의하고 소화가 잘 되는 음식을 드세요.",
                "부동산이나 집안일과 관련된 운이 좋습니다.",
                "서두르지 않아도 됩니다. 묵묵히 걸어가면 정상에 도달합니다.",
                "포용력이 필요한 날입니다. 상대방의 실수를 덮어주세요.",
                "지금 하고 있는 일이 탄탄한 기반이 될 것입니다.",
                "달콤한 말보다는 진실된 행동이 마음을 움직입니다.",
                "고집을 조금만 내려놓으면 융통성이 생깁니다.",
                "가족과 함께하는 시간이 큰 에너지가 됩니다.",
                "현실적인 감각이 뛰어난 날입니다. 쇼핑 시 실속을 챙기세요.",
                "믿음직한 당신에게 고민 상담을 요청하는 이가 있을 것입니다.",
                "마음의 여유를 가지세요. 모든 것은 순리대로 흘러갑니다.",
                "결실을 맺기 전 단계입니다. 조금만 더 인내하세요."
            )
            FiveElement.METAL -> listOf(
                "결단력이 빛을 발하는 날입니다. 맺고 끊음을 확실히 하세요.",
                "냉철한 이성이 필요한 시점입니다. 감정에 휘둘리지 마세요.",
                "서쪽에서 귀인이 나타나 도움을 줄 수 있습니다.",
                "흰색이나 메탈 소재의 액세서리가 행운을 줍니다.",
                "원칙을 지키는 것이 나를 보호하는 길입니다.",
                "날카로운 직관으로 문제의 핵심을 뚫어볼 수 있습니다.",
                "정의로운 행동이 칭송받는 날입니다.",
                "너무 예민해질 수 있으니, 릴렉스하는 시간을 가지세요.",
                "금융이나 숫자와 관련된 일이 잘 풀립니다.",
                "깔끔하고 세련된 스타일이 호감을 줍니다.",
                "불필요한 인연을 정리하기 좋은 날입니다.",
                "말 한마디가 천 냥 빚을 갚거나, 상처를 줄 수 있습니다. 신중하세요.",
                "결실의 계절입니다. 노력한 만큼의 보상이 따릅니다.",
                "전문성을 인정받을 수 있는 기회가 옵니다.",
                "규칙적인 생활이 건강을 지켜줍니다.",
                "의리를 지키면 훗날 큰 보답으로 돌아옵니다.",
                "복잡한 생각이 정리되고 명쾌한 해답을 얻습니다.",
                "리더십을 발휘하기 좋은 날입니다.",
                "강한 것은 부러지기 쉽습니다. 때로는 부드러움이 필요합니다.",
                "보석처럼 빛나는 당신의 가치를 스스로 인정해주세요."
            )
            FiveElement.WATER -> listOf(
                "물 흐르듯 유연하게 대처하는 지혜가 필요합니다.",
                "막히면 돌아가세요. 우회하는 것이 지름길일 수 있습니다.",
                "북쪽으로의 이동이나 여행이 길합니다.",
                "검은색이나 네이비 색상이 차분함을 줍니다.",
                "깊은 통찰력이 생기는 날입니다. 명상이나 독서를 추천합니다.",
                "대인관계가 원활해지고 인기가 많아지는 날입니다.",
                "비밀을 지키는 것이 중요합니다. 입을 무겁게 하세요.",
                "창의적이고 예술적인 영감이 떠오릅니다.",
                "밤 시간에 활동하면 집중력이 더 높아집니다.",
                "주변 사람들을 포용하고 이해하려는 마음이 행운을 부릅니다.",
                "재물 흐름이 원활합니다. 돈이 돌고 도는 날입니다.",
                "지나친 생각은 실행을 방해합니다. 일단 부딪혀보세요.",
                "적응력이 뛰어난 당신, 어디서든 환영받습니다.",
                "음주가무를 즐기기 좋으나, 과유불급임을 명심하세요.",
                "해외나 먼 곳에서 소식이 들려올 수 있습니다.",
                "지혜로운 조언자가 되어줄 수 있는 날입니다.",
                "휴식이 필요합니다. 반신욕이나 물을 많이 마시세요.",
                "유머 감각이 빛을 발하여 분위기 메이커가 됩니다.",
                "겉모습보다는 내면의 충실함이 중요합니다.",
                "시작보다는 마무리를 잘하는 것이 중요한 하루입니다."
            )
        }
    }

    // 2. 프리미엄 종합 운세 (Premium General Fortune) - 오행별 20개
    private fun getPremiumFortuneList(element: FiveElement): List<String> {
        return when (element) {
            FiveElement.WOOD -> listOf(
                "거목이 될 자질을 보이고 있습니다. 오늘은 당신의 야망을 크게 가져도 좋습니다.",
                "경쟁자가 나타날 수 있으나, 당신의 성장 동력이 될 뿐입니다. 두려워 마세요.",
                "뿌리가 깊어지고 있습니다. 지금 겪는 시련은 당신을 더 단단하게 만듭니다.",
                "교육이나 기획 관련 업무에서 탁월한 성과를 낼 수 있습니다.",
                "오랜 기간 공들인 일이 드디어 싹을 틔우기 시작합니다.",
                "주변에 당신을 지지하는 세력이 모여듭니다. 리더십을 발휘하세요.",
                "새로운 배움의 기회가 온다면 주저하지 말고 잡으세요.",
                "과거의 실패는 잊으세요. 오늘은 새로운 가지를 뻗을 때입니다.",
                "당신의 선한 영향력이 주변을 변화시키고 있습니다.",
                "금전적인 투자보다는 자기 자신에게 투자하는 것이 더 큰 이익입니다.",
                "이동수가 있습니다. 출장이나 여행이 행운을 가져옵니다.",
                "고집을 부리기보다는 바람 부는 대로 유연하게 대처하면 이깁니다.",
                "건강한 신체에서 건강한 정신이 나옵니다. 운동을 시작하세요.",
                "뜻밖의 귀인이 나타나 당신의 앞길을 열어줄 것입니다.",
                "문서운이 좋습니다. 계약이나 합격 소식이 있을 수 있습니다.",
                "형제나 동료와의 협업이 시너지 효과를 냅니다.",
                "호기심이 왕성해지는 날입니다. 이것이 돈이 되는 아이디어가 됩니다.",
                "순수함을 잃지 마세요. 그것이 당신의 가장 큰 무기입니다.",
                "봄날의 따스한 햇살처럼 당신의 앞날이 밝습니다.",
                "명예가 드높아질 운세입니다. 겸손함을 유지하면 더 길합니다."
            )
            FiveElement.FIRE -> listOf(
                "태양처럼 만물을 비추는 형상입니다. 당신의 능력이 만천하에 드러납니다.",
                "승진이나 당선 등 명예로운 일이 생길 수 있는 강력한 운입니다.",
                "당신의 열정은 식지 않습니다. 그 열정이 사람들을 감동시킵니다.",
                "화려한 스포트라이트를 받게 됩니다. 주인공은 바로 당신입니다.",
                "표현하지 않으면 아무도 모릅니다. 적극적으로 어필하세요.",
                "예상치 못한 횡재수가 있습니다. 다만 지출도 클 수 있으니 관리하세요.",
                "연애운이 최고조입니다. 매력이 넘쳐흘러 이성이 따릅니다.",
                "성급함을 누르고 차분함을 가지면 천하를 얻을 수 있습니다.",
                "오늘의 아이디어는 대박 상품이 될 잠재력이 있습니다.",
                "솔직함이 때로는 독이 될 수 있습니다. 선의의 거짓말도 필요합니다.",
                "심장이 뛰는 일을 하세요. 그것이 당신의 천직입니다.",
                "주변의 시기를 받을 수 있으나, 개의치 말고 앞만 보고 달리세요.",
                "봉사나 기부를 통해 덕을 쌓으면 더 큰 복이 들어옵니다.",
                "방송, 광고, 홍보 분야에서 두각을 나타낼 수 있습니다.",
                "눈앞의 이익보다는 먼 미래를 보고 투자하세요.",
                "당신의 카리스마가 조직을 하나로 뭉치게 합니다.",
                "꺼져가던 불씨가 다시 살아나는 회생의 운입니다.",
                "화려한 외출이 기분 전환과 행운을 동시에 줍니다.",
                "정열적인 사랑을 꿈꾼다면 오늘이 기회입니다.",
                "자신감을 가지세요. 오늘은 당신을 막을 자가 없습니다."
            )
            FiveElement.EARTH -> listOf(
                "태산과 같은 중후함이 빛을 발합니다. 믿고 맡길 수 있는 사람으로 인정받습니다.",
                "재물 창고가 열렸습니다. 들어온 돈을 잘 지키는 것이 관건입니다.",
                "부동산 운이 매우 좋습니다. 매매나 계약에 유리한 시기입니다.",
                "모든 것을 포용하는 어머니 대지처럼 넓은 마음을 가지세요.",
                "신용이 당신의 가장 큰 자산입니다. 약속은 반드시 지키세요.",
                "느리지만 확실하게 전진하고 있습니다. 조바심 낼 필요 없습니다.",
                "중간 다리 역할을 잘하여 양쪽 모두에게 이득을 줍니다.",
                "과거의 노력들이 층층이 쌓여 거대한 성과로 나타납니다.",
                "고집스러운 면이 전문가로서의 권위를 세워줍니다.",
                "가정이 화목해야 밖에서도 일이 잘 풀립니다. 집안을 챙기세요.",
                "농부가 수확을 앞둔 마음처럼 풍요로운 운세입니다.",
                "비밀을 끝까지 지켜주세요. 그것이 당신의 평판을 높입니다.",
                "안정적인 투자가 좋습니다. 투기나 도박은 절대 금물입니다.",
                "오랜 친구가 귀인이 되어 찾아옵니다.",
                "당신의 뚝심이 난관을 돌파하는 열쇠입니다.",
                "변화보다는 현상 유지가 더 좋은 결과를 가져옵니다.",
                "종교나 철학에 관심을 가지면 마음의 평화를 얻습니다.",
                "건강은 소화기 계통을 조심하면 만사형통입니다.",
                "흙 속에 묻힌 진주를 발견하는 안목이 생깁니다.",
                "성실함의 대가는 반드시 돌아옵니다. 오늘이 그날입니다."
            )
            FiveElement.METAL -> listOf(
                "원석이 다듬어져 보석이 되는 과정입니다. 고난 끝에 낙이 옵니다.",
                "냉철한 판단력이 필요한 시기입니다. 공과 사를 명확히 하세요.",
                "강력한 리더십으로 조직을 이끌어갈 운세입니다.",
                "금전운이 매우 강합니다. 투자의 적기일 수 있습니다.",
                "불필요한 인간관계를 정리하고 알짜배기 인맥만 남기세요.",
                "당신의 한 마디가 법이 되는 날입니다. 언행에 무게를 두세요.",
                "기술이나 전문 분야에서 최고의 실력을 발휘합니다.",
                "경쟁에서 반드시 승리하는 기운입니다. 물러서지 마세요.",
                "정의로운 일에 앞장서면 명예가 따릅니다.",
                "너무 완벽함을 추구하면 피곤합니다. 80%에 만족하세요.",
                "날카로운 예지력이 발동합니다. 느낌대로 행동하세요.",
                "수술이나 시술 등 몸에 칼을 대는 일도 잘 풀립니다.",
                "자동차나 기계와 관련된 운이 좋습니다.",
                "차가워 보이지만 내면은 따뜻한 당신, 반전 매력을 보여주세요.",
                "의리 때문에 손해 볼 수 있으니 실속을 챙기세요.",
                "새로운 규칙이나 시스템을 도입하기 좋은 날입니다.",
                "목소리에 힘이 있습니다. 설득이나 협상에 유리합니다.",
                "결실을 맺는 가을의 기운입니다. 수확의 기쁨을 누리세요.",
                "보석처럼 빛나는 당신을 시기하는 자를 조심하세요.",
                "확실한 목표가 있다면 거침없이 돌파하세요."
            )
            FiveElement.WATER -> listOf(
                "큰 바다와 같은 지혜가 샘솟는 날입니다.",
                "어디에도 얽매이지 않는 자유로운 영혼이 행운을 부릅니다.",
                "해외 운이 아주 좋습니다. 유학, 이민, 무역 관련 일이 길합니다.",
                "밤에 피는 꽃처럼 은밀한 매력이 발산됩니다.",
                "융통성의 제왕입니다. 어떤 위기 상황도 유연하게 넘깁니다.",
                "재물 흐름이 막힘없이 흐릅니다. 자금 회전이 좋습니다.",
                "학문과 연구 분야에서 깊이 있는 성과를 냅니다.",
                "사람의 마음을 읽는 능력이 탁월합니다. 상담이나 영업에 유리합니다.",
                "새로운 생명을 잉태하는 기운입니다. 임신이나 창작에 좋습니다.",
                "조용히 실속을 챙기는 것이 떠벌리는 것보다 낫습니다.",
                "주변 분위기를 주도하는 힘이 있습니다.",
                "음식이나 요식업과 관련된 운이 좋습니다.",
                "휴식이 곧 경쟁력입니다. 충분한 잠이 보약입니다.",
                "당신의 아이디어는 시대를 앞서갑니다. 자신감을 가지세요.",
                "비밀 연애를 하거나 남모르는 취미를 즐기기 좋습니다.",
                "정보력이 돈이 됩니다. 뉴스와 소식에 귀 기울이세요.",
                "차가운 머리와 따뜻한 가슴의 조화가 필요합니다.",
                "겸손하게 자세를 낮추면 물이 모이듯 사람들이 모입니다.",
                "끝은 새로운 시작입니다. 마무리를 잘 하세요.",
                "흐르는 물처럼 멈추지 않고 도전하는 당신이 아름답습니다."
            )
        }
    }

    // 🎁 [업그레이드] 프리미엄 운세 생성 (종합 + 골프 + 나이별 조언)
    fun getSpecialFortune(element: FiveElement, birthDate: LocalDate): Pair<String, String> {
        val today = LocalDate.now().toString()
        val birthYear = birthDate.year
        // 랜덤 시드를 날짜와 오행으로 조합해 매일 달라지게 함
        val random = Random("$element|$today|PREMIUM_V3".hashCode())

        val sb = StringBuilder()

        // 1. 종합 운세 (위에서 만든 20개 리스트 중 랜덤 1개 선택)
        sb.append("✨ [종합 운세]\n")
        val generalList = getPremiumFortuneList(element)
        sb.append(generalList[random.nextInt(generalList.size)]).append("\n\n")

        // 2. 골프 운세 (오행별 특성 반영)
        sb.append("⛳ [골프 운세]\n")
        val golfPool = when(element) {
            FiveElement.WOOD -> listOf(
                "드라이버 샷의 직진성이 탁월합니다. 페어웨이를 가릅니다.",
                "우드 샷에서 행운이 따릅니다. 투온을 노려보세요.",
                "바람을 이용하는 지혜가 필요합니다. 클럽 선택에 신중하세요."
            )
            FiveElement.FIRE -> listOf(
                "아이언 샷감이 불을 뿜습니다. 핀을 바로 공략하세요.",
                "어프로치 샷이 핀에 착 붙는 날입니다.",
                "화려한 옷을 입고 라운딩하면 스코어가 좋아집니다."
            )
            FiveElement.EARTH -> listOf(
                "벙커 세이브율이 높습니다. 모래를 두려워 마세요.",
                "숏게임이 안정적입니다. 타수를 줄일 기회입니다.",
                "평정심을 유지하면 라베(Life Best)를 갱신할 수 있습니다."
            )
            FiveElement.METAL -> listOf(
                "퍼팅 감각이 예리합니다. 라이가 눈에 훤히 보입니다.",
                "과감한 스트로크가 성공합니다. 짧은 것보단 지나가는 게 낫습니다.",
                "스틸 샤프트의 아이언이 손에 잘 맞습니다."
            )
            FiveElement.WATER -> listOf(
                "리듬감이 좋아 비거리가 늘어납니다.",
                "부드러운 스윙이 스코어를 줄입니다. 힘을 빼세요.",
                "해저드를 겁내지 마세요. 오히려 행운의 구역입니다."
            )
        }
        sb.append(golfPool[random.nextInt(golfPool.size)]).append("\n\n")

        // 3. 연령별 맞춤 운세
        if (birthYear <= 1980) { // 80년생 이전 (자녀/가정)
            sb.append("👨‍👩‍👧‍👦 [자식/가정 운세]\n")
            val childPool = listOf(
                "자녀에게 경사가 생길 기운입니다. 축하해 줄 준비를 하세요.",
                "대화가 잘 통하는 날입니다. 먼저 자녀에게 말을 걸어보세요.",
                "자녀의 고민을 들어주면 해결의 실마리가 보입니다.",
                "가정이 화목해야 만사가 형통합니다. 오늘 저녁은 가족과 함께하세요.",
                "자녀의 독립심을 키워주는 것이 훗날 큰 효도로 돌아옵니다."
            )
            sb.append(childPool[random.nextInt(childPool.size)]).append("\n\n")
        } else { // 81년생 이후 (학업/승진)
            sb.append("📚 [학업/승진/자기계발]\n")
            val studyPool = when(element) {
                FiveElement.WOOD -> "새로운 언어나 IT 기술을 배우기 좋은 날입니다. 시작이 반입니다."
                FiveElement.FIRE -> "자격증 시험이나 면접에서 좋은 결과가 예상됩니다. 자신감을 가지세요."
                FiveElement.EARTH -> "기초를 다지는 공부가 대성합니다. 끈기 있게 파고드세요."
                FiveElement.METAL -> "논리적인 사고가 필요한 수학, 금융, 법률 공부가 머리에 쏙쏙 들어옵니다."
                FiveElement.WATER -> "창의적인 아이디어나 기획력이 샘솟습니다. 메모하는 습관을 들이세요."
            }
            sb.append(studyPool).append("\n\n")
        }

        // 4. 여행 운세 (60년생 이전)
        if (birthYear <= 1960) {
            sb.append("✈️ [추천 여행]\n")
            val travelPool = when(element) {
                FiveElement.WOOD -> listOf("숲이 우거진 '일본 교토'", "휴양의 도시 '베트남 다낭'")
                FiveElement.FIRE -> listOf("따뜻한 햇살의 '태국 치앙마이'", "열정의 섬 '괌'")
                FiveElement.EARTH -> listOf("웅장한 자연 '중국 장가계'", "평화로운 '스위스'")
                FiveElement.METAL -> listOf("문화와 예술의 '서유럽'", "세련된 도시 '싱가포르'")
                FiveElement.WATER -> listOf("탁 트인 바다 '호주 시드니'", "설국의 '북해도'")
            }
            sb.append(travelPool[random.nextInt(travelPool.size)])
        }

        return "프리미엄 종합 운세" to sb.toString()
    }

    // 3. generateFortune (기본 운세 생성기) - 업데이트된 리스트 사용
    fun generateFortune(date: LocalDate?, time: LocalTime?, gender: String?, count: Int): Pair<String, String> {
        if (date == null || time == null) return "준비" to "생년월일과 시간을 입력해주세요."

        val saju = calculateSaju(date, time)
        val element = saju.dayElement
        val today = LocalDate.now().toString()
        val key = "$date|$time|$gender|$today|$count|${saju.dayGanji}"
        val random = Random(key.hashCode())

        val title = when(count) {
            1 -> "✨ 오늘의 기본 운세"
            2 -> "🛡️ 액운 타파 운세"
            3 -> "🛡️ 악재 소멸 운세"
            else -> "👑 프리미엄 운세"
        }

        // 20개 리스트 중 하나 랜덤 선택
        val fortuneList = getBasicFortuneList(element)
        val body = fortuneList[random.nextInt(fortuneList.size)]

        return title to "당신은 ${saju.dayGanji}에 태어난 '${element.koreanName}'입니다.\n\n$body"
    }

    fun generateLottoNumbers(date: LocalDate?, gender: String?, count: Int, excludeList: List<Int>): List<Int> {
        val today = LocalDate.now().toString()
        val key = "$date|$gender|$today|$count|LOTTO"
        val random = Random(key.hashCode())

        val allNumbers = (1..45).filter { !excludeList.contains(it) }.toMutableList()
        allNumbers.shuffle(random)
        val selected = allNumbers.take(6).toMutableList()
        return selected.sorted()
    }
}
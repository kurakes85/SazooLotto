package com.example.sazoolotto   // MainActivity 맨 위랑 동일해야 함

import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sazoolotto.ui.theme.*
import com.sazoo.lotto.ui.theme.FireAccent
import com.sazoo.lotto.ui.theme.PencilDark
import com.sazoo.lotto.ui.theme.PencilLight
import com.sazoo.lotto.ui.theme.PencilLine
import com.sazoo.lotto.ui.theme.SoftGold
import kotlin.random.Random

// ---------------------- 앱 전체 루트 -----------------------------

@Composable
fun SazooLottoApp() {
    // 기본 오행 색은 화(火)로 시작, 실제 표시 색은 SazooLottoScreen 안에서 오행에 따라 바뀜
    val elementColor: Color = FireAccent

    Scaffold(
        topBar = { SazooTopBar() }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            SazooLottoScreen(elementColor = elementColor)
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
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SazooLotto",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PencilDark
            )
            Text(
                text = "오늘의 사주 로또",
                style = MaterialTheme.typography.bodySmall,
                color = PencilLight
            )
        }
    }
}

// ---------------------- 메인 화면 -----------------------------

@Composable
fun SazooLottoScreen(
    elementColor: Color   // 현재는 기본값 역할만 함
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var birthDateText by remember { mutableStateOf("생년월일을 선택하세요") }
    var birthYear by remember { mutableStateOf<Int?>(null) }
    var zodiacText by remember { mutableStateOf<String?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }

    var showResult by remember { mutableStateOf(false) }
    var canDrawToday by remember { mutableStateOf(true) }

    var fortuneTitle by remember { mutableStateOf("오늘의 사주를 뽑아 보세요") }
    var fortuneBody by remember { mutableStateOf("") }
    var lottoNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var previousLottoNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 현재 선택된 오행 타입
    var elementType by remember { mutableStateOf(ElementType.FIRE) }

    // 오행에 따라 색, 이모지 결정
    val activeElementColor = elementColorFromType(elementType)
    val activeElementEmoji = elementEmojiFromType(elementType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 유저 입력 카드
        UserInputCard(
            name = name,
            onNameChange = { name = it },
            birthDateText = birthDateText,
            onBirthDateClick = {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)

                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        birthDateText = "%04d-%02d-%02d".format(y, m + 1, d)
                        birthYear = y
                        zodiacText = getZodiacFromYear(y)   // 🔹 띠 계산
                    },
                    year,
                    month,
                    day
                ).show()
            },
            gender = gender,
            onGenderChange = { gender = it },
            elementColor = activeElementColor,
            zodiacText = zodiacText
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 오늘 한 번만 뽑을 수 있는 버튼
        Button(
            onClick = {
                // 이전 번호를 히스토리로 보관
                if (lottoNumbers.isNotEmpty()) {
                    previousLottoNumbers = lottoNumbers
                }

                // 입력된 생년으로 오행 타입 계산
                elementType = elementTypeFromYear(birthYear)

                // 입력값에 따라 항상 같은 패턴으로 운세/번호 생성
                val (newTitle, newBody) = generateFortune(
                    name = name,
                    birthYear = birthYear,
                    gender = gender
                )
                fortuneTitle = newTitle
                fortuneBody = newBody
                lottoNumbers = generateLottoNumbers(
                    name = name,
                    birthYear = birthYear,
                    gender = gender
                )

                showResult = true
                canDrawToday = false   // 한 번 뽑았으니 오늘은 비활성화
            },
            enabled = canDrawToday,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SoftGold,
                contentColor = PencilDark
            )
        ) {
            Text("오늘 사주 & 로또 번호 뽑기")
        }

        if (!showResult) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "버튼을 눌러 오늘 사주와 로또 번호를 확인해 보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = PencilLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))

            // 🔥 오늘의 사주 카드 (오행 이모지 포함)
            FortuneCard(
                elementColor = activeElementColor,
                elementEmoji = activeElementEmoji,
                title = fortuneTitle,
                body = fortuneBody
            )

            // 🎲 오늘 번호
            LottoCard(
                elementColor = activeElementColor,
                title = "오늘의 로또 번호",
                numbers = lottoNumbers
            )

            // 📜 이전에 뽑았던 번호 (있을 때만)
            if (previousLottoNumbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LottoCard(
                    elementColor = activeElementColor,
                    title = "이전 로또 번호",
                    numbers = previousLottoNumbers
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 광고 보고 다시 뽑기
            OutlinedButton(
                onClick = {
                    // TODO: 보상형 광고 붙이면, 광고 완료 콜백에서
                    //       아래 두 줄을 실행하면 됨.
                    canDrawToday = true
                    showResult = false
                },
                enabled = !canDrawToday,   // 한 번 뽑은 뒤에만 활성
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = activeElementColor
                )
            ) {
                Text("광고 보고 다시 뽑기")
            }
        }
    }
}

// ---------------------- 입력 카드 -----------------------------

@Composable
fun UserInputCard(
    name: String,
    onNameChange: (String) -> Unit,
    birthDateText: String,
    onBirthDateClick: () -> Unit,
    gender: String?,
    onGenderChange: (String?) -> Unit,
    elementColor: Color,
    zodiacText: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PencilLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✏️ 내 정보",
                style = MaterialTheme.typography.titleMedium,
                color = PencilDark
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("이름 (선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 생년월일 + 띠
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "생년월일",
                    style = MaterialTheme.typography.bodySmall,
                    color = PencilLight
                )
                OutlinedButton(
                    onClick = onBirthDateClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = birthDateText,
                        modifier = Modifier.weight(1f),
                        color = PencilDark
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("📅")
                }

                if (zodiacText != null) {
                    Text(
                        text = "띠: $zodiacText",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilLight
                    )
                }
            }

            // 성별 선택
            Column {
                Text(
                    text = "성별 (선택)",
                    style = MaterialTheme.typography.bodySmall,
                    color = PencilLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = gender == "male",
                            onClick = { onGenderChange("male") },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = elementColor
                            )
                        )
                        Text("남", color = PencilDark)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = gender == "female",
                            onClick = { onGenderChange("female") },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = elementColor
                            )
                        )
                        Text("여", color = PencilDark)
                    }
                }
            }
        }
    }
}

// ---------------------- 사주 카드 -----------------------------

@Composable
fun FortuneCard(
    elementColor: Color,
    elementEmoji: String,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PencilLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = elementEmoji, // 🔥/💧/🌿/🪨/💰 중 하나
                    fontSize = 20.sp
                )
                Text(
                    text = "오늘의 기운",
                    style = MaterialTheme.typography.labelLarge,
                    color = PencilDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(32.dp)
                        .background(color = elementColor)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PencilDark
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = PencilLight
            )
        }
    }
}

// ---------------------- 로또 번호 카드 -----------------------------

@Composable
fun LottoCard(
    elementColor: Color,
    title: String,
    numbers: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PencilLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PencilDark
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                numbers.forEach { n ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, elementColor),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(
                                text = n.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PencilDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- 헬퍼: 띠 계산 -----------------------------

private fun getZodiacFromYear(year: Int): String {
    // 0: 원숭이, 1: 닭, 2: 개, 3: 돼지, 4: 쥐, 5: 소, 6: 호랑이, 7: 토끼, 8: 용, 9: 뱀, 10: 말, 11: 양
    val animals = listOf("원숭이", "닭", "개", "돼지", "쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양")
    val index = ((year % 12) + 12) % 12
    val animal = animals[index]
    return "${animal}띠"
}

// ---------------------- 헬퍼: 오행 타입 / 색 / 이모지 -----------------------------

private enum class ElementType { WOOD, FIRE, EARTH, METAL, WATER }

private fun elementTypeFromYear(birthYear: Int?): ElementType {
    if (birthYear == null) return ElementType.FIRE
    return when (((birthYear % 5) + 5) % 5) {
        0 -> ElementType.WOOD
        1 -> ElementType.FIRE
        2 -> ElementType.EARTH
        3 -> ElementType.METAL
        else -> ElementType.WATER
    }
}

private fun elementNameFromType(type: ElementType): String = when (type) {
    ElementType.WOOD -> "목(木)"
    ElementType.FIRE -> "화(火)"
    ElementType.EARTH -> "토(土)"
    ElementType.METAL -> "금(金)"
    ElementType.WATER -> "수(水)"
}

private fun elementColorFromType(type: ElementType): Color = when (type) {
    ElementType.WOOD -> Color(0xFF66A86E) // 초록
    ElementType.FIRE -> FireAccent         // 기존 포인트 색
    ElementType.EARTH -> Color(0xFFB59473) // 흙 느낌 브라운
    ElementType.METAL -> Color(0xFFB0BEC5) // 회색 메탈
    ElementType.WATER -> Color(0xFF4FC3F7) // 파랑
}

private fun elementEmojiFromType(type: ElementType): String = when (type) {
    ElementType.WOOD -> "🌿"
    ElementType.FIRE -> "🔥"
    ElementType.EARTH -> "🪨"
    ElementType.METAL -> "💰"
    ElementType.WATER -> "💧"
}

// ---------------------- 헬퍼: 오행 이름 (사주 문구용) -----------------------------

private fun getElementName(birthYear: Int?): String {
    if (birthYear == null) return "균형 있는"
    val type = elementTypeFromYear(birthYear)
    return elementNameFromType(type)
}

// ---------------------- 헬퍼: 사주 문구 생성 (간단 룰 기반) -----------------------------

private fun generateFortune(
    name: String,
    birthYear: Int?,
    gender: String?
): Pair<String, String> {
    val baseName = if (name.isBlank()) "손님" else name
    val key = baseName + "|" + (birthYear ?: 0) + "|" + (gender ?: "N")
    val random = Random(key.hashCode())

    val titles = listOf(
        "열정이 살아나는 날",
        "마음이 편안해지는 날",
        "관계 운이 좋은 날",
        "집중력이 빛나는 날",
        "새로운 기회를 만나는 날"
    )

    val elementName = getElementName(birthYear)

    val bodies = listOf(
        "오늘은 $elementName 기운이 강한 날입니다. 하고 싶었던 일을 과감하게 시작해 보세요.",
        "오늘은 $elementName 기운이 잔잔하게 흐르는 날입니다. 마음 정리와 휴식에 좋은 시간이에요.",
        "오늘은 $elementName 기운 덕분에 사람들과의 인연이 활발해집니다. 연락이 온다면 가능하면 받아 주세요.",
        "오늘은 $elementName 기운으로 집중력이 좋아지는 날입니다. 미뤄둔 공부나 작업을 끝내기 좋습니다.",
        "오늘은 $elementName 기운이 새로운 문을 열어 줍니다. 평소와 다른 선택이 행운을 가져올 수 있어요."
    )

    val index = random.nextInt(titles.size)
    return titles[index] to bodies[index]
}

// ---------------------- 헬퍼: 로또 번호 생성 (항상 같은 입력 → 같은 번호) -----------------------------

private fun generateLottoNumbers(
    name: String,
    birthYear: Int?,
    gender: String?
): List<Int> {
    val key = (name.ifBlank { "NO_NAME" } + "|" + (birthYear ?: 0) + "|" + (gender ?: "N"))
    val random = Random(key.hashCode() * 31 + 7)

    return (1..45).shuffled(random).take(6).sorted()
}

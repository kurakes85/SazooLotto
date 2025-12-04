package com.example.sazoolotto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sazoolotto.logic.BirthTimeSection
import com.example.sazoolotto.logic.Gender
import com.example.sazoolotto.logic.LottoResult
import java.time.LocalDate

// [색상 도우미 함수] 번호에 따라 로또 공 색깔을 반환
fun getBallColor(number: Int): Color {
    return when (number) {
        in 1..10 -> Color(0xFFFBC02D) // 노랑
        in 11..20 -> Color(0xFF2196F3) // 파랑
        in 21..30 -> Color(0xFFF44336) // 빨강
        in 31..40 -> Color(0xFF757575) // 회색
        else -> Color(0xFF4CAF50)      // 초록
    }
}

@Composable
fun LastLottoResultCard(
    round: Int,
    date: String,
    numbers: List<Int>,
    bonus: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🏆 지난주 1등 당첨 (${round}회)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                numbers.forEach { num ->
                    SmallBall(num)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(" + ", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                SmallBall(bonus)
            }
            Text("$date 추첨", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// [수정됨] 이제 작은 공도 번호에 맞춰 색깔이 나옵니다!
@Composable
fun SmallBall(number: Int) {
    val color = getBallColor(number)
    Box(modifier = Modifier.size(30.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text("$number", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BigSajuInputCard(onCalculateClick: (Int, Int, Int, Gender, BirthTimeSection) -> Unit) {
    var selectedYear by remember { mutableStateOf("1970") }
    var selectedMonth by remember { mutableStateOf("1") }
    var selectedDay by remember { mutableStateOf("1") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var selectedTime by remember { mutableStateOf(BirthTimeSection.UNKNOWN) }

    val years = (1940..2005).map { it.toString() }.reversed()
    val months = (1..12).map { it.toString() }
    val maxDay = try { LocalDate.of(selectedYear.toInt(), selectedMonth.toInt(), 1).lengthOfMonth() } catch (e: Exception) { 31 }
    val days = (1..maxDay).map { it.toString() }
    LaunchedEffect(maxDay) { if (selectedDay.toInt() > maxDay) selectedDay = "1" }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📅 내 사주 입력하기", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(24.dp))

            BigDropdownSelector("년", selectedYear, years) { selectedYear = it }
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                BigDropdownSelector("월", selectedMonth, months, modifier = Modifier.weight(1f).padding(end = 4.dp)) { selectedMonth = it }
                BigDropdownSelector("일", selectedDay, days, modifier = Modifier.weight(1f).padding(start = 4.dp)) { selectedDay = it }
            }
            Spacer(modifier = Modifier.height(16.dp)); Divider(); Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                GenderButton(Gender.MALE, selectedGender) { selectedGender = it }; Spacer(modifier = Modifier.width(8.dp))
                GenderButton(Gender.FEMALE, selectedGender) { selectedGender = it }
            }
            Spacer(modifier = Modifier.height(16.dp))

            BigDropdownSelector("", selectedTime.displayName, BirthTimeSection.values().map { it.displayName }) { name ->
                selectedTime = BirthTimeSection.values().find { it.displayName == name } ?: BirthTimeSection.UNKNOWN
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onCalculateClick(selectedYear.toInt(), selectedMonth.toInt(), selectedDay.toInt(), selectedGender, selectedTime) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))) {
                Text("✨ 운세 & 번호 보기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.GenderButton(gender: Gender, current: Gender, onClick: (Gender) -> Unit) {
    val isSelected = gender == current
    Button(onClick = { onClick(gender) }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFF3F51B5) else Color(0xFFF0F0F0)), shape = RoundedCornerShape(12.dp)) {
        Text(gender.displayName, color = if (isSelected) Color.White else Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BigDropdownSelector(
    label: String,
    currentValue: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if(label.isEmpty()) currentValue else "$currentValue$label", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(24.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White).heightIn(max = 250.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(if(label.isEmpty()) option else "$option$label", fontSize = 18.sp) }, onClick = { onOptionSelected(option); expanded = false }) }
        }
    }
}

@Composable
fun BigResultDisplayCard(result: LottoResult, onShareClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = if (result.step == 3) Color(0xFFFFD700) else Color(0xFFE3F2FD), shape = RoundedCornerShape(50)) {
                Text(if (result.step == 3) " ✨ 프리미엄 결과 ✨ " else " 현재 단계: ${result.step} / 3 ", modifier = Modifier.padding(16.dp, 8.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(Color(result.element.colorHex), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("오늘의 기운: ${result.element.kName}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            Text(result.mainFortune, fontSize = 20.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, modifier = Modifier.padding(top = 16.dp))
            if (result.subAdvice.isNotEmpty()) Text("💡 ${result.subAdvice}", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(24.dp)); Divider(); Spacer(modifier = Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { result.recommendNumbers.take(3).forEach { BigLottoBall(it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { result.recommendNumbers.drop(3).take(3).forEach { BigLottoBall(it) } }
            }
            if (result.step >= 2 && result.excludedNumbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(Modifier.background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text("⚠️ 액운 숫자 제외됨: ${result.excludedNumbers.joinToString(", ")}", color = Color(0xFFD32F2F), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            if (result.step == 3) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(Modifier.background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp)).padding(16.dp)) { Text(result.premiumFortune, fontSize = 18.sp, lineHeight = 28.sp, textAlign = TextAlign.Center) }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val shareText = "[사주로또]\n🔮 ${result.element.kName}\n📜 ${result.mainFortune}\n🍀 번호: ${result.recommendNumbers.joinToString(", ")}\n\n※ 재미로 보는 사주로또"
                    onShareClick(shareText)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color.Black), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Share, null); Spacer(modifier = Modifier.width(8.dp)); Text("공유하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// [수정됨] BigLottoBall도 공통 함수 사용
@Composable
fun BigLottoBall(number: Int) {
    val color = getBallColor(number)
    Box(Modifier.size(64.dp).background(color, CircleShape).border(1.dp, Color.Black.copy(0.1f), CircleShape), Alignment.Center) {
        Text("$number", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
    }
}

@Composable
fun BigAdStepButton(currentStep: Int, onAdWatchClick: (Int) -> Unit) {
    if (currentStep >= 3) return
    val (title, subtitle) = if (currentStep == 1) "📺 2단계: 액운 숫자 제외하기" to "광고보고 나쁜 숫자 3개 피해서 다시 뽑기" else "📺 3단계: 프리미엄 운세 보기" to "광고보고 최종 상세 풀이 확인하기"
    Button(onClick = { onAdWatchClick(currentStep) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(80.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), elevation = ButtonDefaults.buttonElevation(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 14.sp, color = Color.White.copy(0.9f))
        }
    }
}
package com.example.sazoolotto.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sazoolotto.logic.FiveElement
import com.example.sazoolotto.logic.RealLottoEngine
import com.example.sazoolotto.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime

// 1. 사용자 입력 카드
@Composable
fun UserInputCard(
    birthDateText: String, birthTimeText: String,
    onBirthDateClick: () -> Unit, onBirthTimeClick: () -> Unit,
    gender: String?, onGenderChange: (String?) -> Unit,
    elementColor: Color, zodiacText: String?, dayGanjiText: String?,
    isInputValid: Boolean, onReset: () -> Unit, onConfirm: () -> Unit
) {
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
                // 🔥 [변경] 버튼 텍스트 수정
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(isInputValid) elementColor else DisabledGrey, contentColor = if(isInputValid) Color.White else Color.Gray),
                    enabled = isInputValid
                ) { Text("사주 로또번호 확인") }
            }
        }
    }
}

// 2. 기본 운세 카드
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

// 3. 프리미엄 운세 카드
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

// 4. 로또 공 디자인
@Composable
fun RealLottoBall(number: Int, isBonus: Boolean = false) {
    val color = when (number) {
        in 1..10 -> BallYellow; in 11..20 -> BallBlue; in 21..30 -> BallRed; in 31..40 -> BallGray; else -> BallGreen
    }
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) { Text("$number", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
    }
}

// 5. 실제 당첨 번호 카드
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
                RealLottoBall(number = result.bonus)
            }
        }
    }
}

// 6. 추천 로또 카드
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
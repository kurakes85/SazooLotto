package com.example.sazoolotto.logic

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 1. 서버에서 받아올 데이터 형태 (JSON 모델)
data class LottoApiResponse(
    @SerializedName("returnValue") val result: String, // "success" or "fail"
    @SerializedName("drwNoDate") val date: String?,
    @SerializedName("drwNo") val round: Int,
    @SerializedName("drwtNo1") val no1: Int,
    @SerializedName("drwtNo2") val no2: Int,
    @SerializedName("drwtNo3") val no3: Int,
    @SerializedName("drwtNo4") val no4: Int,
    @SerializedName("drwtNo5") val no5: Int,
    @SerializedName("drwtNo6") val no6: Int,
    @SerializedName("bnusNo") val bonus: Int
)

// 2. API 인터페이스 정의
interface LottoService {
    @GET("common.do?method=getLottoNumber")
    suspend fun getLottoNumber(@Query("drwNo") round: Int): LottoApiResponse
}

// 3. 실제 동작을 담당하는 매니저 클래스
class RealLottoManager {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.dhlottery.co.kr/") // 동행복권 공식 주소
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(LottoService::class.java)

    // 최신 회차 계산기 (2002-12-07이 1회차)
    fun calculateLatestRound(): Int {
        val startDate = LocalDate.of(2002, 12, 7)
        val today = LocalDate.now()

        // 오늘이 토요일이고 오후 9시 전이면 아직 추첨 전이라 이전 회차를 보여줘야 함
        // (간단하게 구현하기 위해 토요일은 무조건 이전 회차로 계산하거나,
        // 일단 날짜 차이로 계산 후 API가 실패하면 -1회차를 재요청하는 방식 사용)

        val weeks = ChronoUnit.WEEKS.between(startDate, today)
        return (weeks + 1).toInt()
    }

    // 서버에서 번호 가져오기
    suspend fun fetchLatestLotto(): LottoApiResponse? {
        val round = calculateLatestRound()
        return try {
            // 1. 계산된 회차로 요청
            var response = service.getLottoNumber(round)

            // 2. 만약 아직 추첨 안 해서 실패("fail")했다면, 전 회차(-1) 재요청
            if (response.result == "fail") {
                response = service.getLottoNumber(round - 1)
            }

            if (response.result == "success") response else null
        } catch (e: Exception) {
            Log.e("RealLottoManager", "통신 실패: ${e.message}")
            null
        }
    }
}
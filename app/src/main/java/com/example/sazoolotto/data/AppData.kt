package com.example.sazoolotto.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// 데이터 저장소 싱글톤 (앱 전체에서 공유)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_saju_data")

// 유저 정보 데이터 모델 (화면에서 쓰기 편하게 묶음)
data class UserPreferences(
    val version: Int,
    val birthDate: String?,
    val birthTime: String?,
    val gender: String?,
    val lastDate: String,
    val drawCount: Int
)
package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wahidiya_prefs")

class UserPreferences(private val context: Context) {
    private val keyStaffCode = stringPreferencesKey("staff_code")
    private val keyStaffPin = stringPreferencesKey("staff_pin")
    private val keyAdminLoggedIn = booleanPreferencesKey("admin_logged_in")
    private val keyCustomOrgId = stringPreferencesKey("custom_org_id")

    val staffCode: Flow<String> = context.dataStore.data.map { it[keyStaffCode] ?: "01" }
    val staffPin: Flow<String> = context.dataStore.data.map { it[keyStaffPin] ?: "" }
    val adminLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[keyAdminLoggedIn] ?: false }
    val customOrgId: Flow<String> = context.dataStore.data.map { it[keyCustomOrgId] ?: "" }

    suspend fun saveStaffCode(code: String) {
        context.dataStore.edit { it[keyStaffCode] = code }
    }

    suspend fun saveStaffPin(pin: String) {
        context.dataStore.edit { it[keyStaffPin] = pin }
    }

    suspend fun setAdminLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[keyAdminLoggedIn] = loggedIn }
    }

    suspend fun saveCustomOrgId(orgId: String) {
        context.dataStore.edit { it[keyCustomOrgId] = orgId.trim() }
    }
}

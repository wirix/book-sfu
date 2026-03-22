package com.example.chitfin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object UserKeys {
    val NAME    = stringPreferencesKey("user_name")
    val EMAIL   = stringPreferencesKey("user_email")
    val PASSWORD = stringPreferencesKey("user_password")   // В реальном приложении НЕ храни пароль в plaintext!
}

// Repository-подобный класс (можно вынести в отдельный файл позже)
class UserPreferences(private val context: Context) {

    val userFlow: Flow<User?> = context.userDataStore.data.map { prefs ->
        val name = prefs[UserKeys.NAME]
        val email = prefs[UserKeys.EMAIL]
        val pass = prefs[UserKeys.PASSWORD]

        if (name != null && email != null && pass != null) {
            User(name, email, pass)
        } else null
    }

    suspend fun saveUser(user: User) {
        context.userDataStore.edit { prefs ->
            prefs[UserKeys.NAME] = user.name
            prefs[UserKeys.EMAIL] = user.email
            prefs[UserKeys.PASSWORD] = user.password
        }
    }

    suspend fun clearUser() {
        context.userDataStore.edit { it.clear() }
    }
}

data class User(
    val name: String,
    val email: String,
    val password: String
)
package com.example.examplesse.data

import com.example.examplesse.di.IODispatcher
import com.example.examplesse.di.ProfileApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class ProfileSSEDataSource @Inject constructor(
    okHttpClient: OkHttpClient,
    @ProfileApi url: String,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    coroutineScope: CoroutineScope,
) : SSEDataSource<List<String>>(
    okHttpClient = okHttpClient,
    url = url,
    ioDispatcher = ioDispatcher,
    coroutineScope = coroutineScope,
    dataExtractor = { extractProfilesList(it) }
) {
    companion object {
        private fun extractProfilesList(data: String): List<String> {
            val json = JSONObject(data)
            val profilesJSONArray = json
                .getJSONObject("data")
                .getJSONArray("profiles")

            return List(profilesJSONArray.length()) { index ->
                profilesJSONArray.getString(index)
            }
        }
    }
}
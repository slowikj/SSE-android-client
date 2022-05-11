package com.example.examplesse.data

import android.util.Log
import com.example.examplesse.di.IODispatcher
import com.example.examplesse.di.ProfileApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class ProfilesEvent {
    data class Data(val profiles: List<String>) : ProfilesEvent()

    object Loading : ProfilesEvent()

    data class Error(val throwable: Throwable?) : ProfilesEvent()
}

class ProfilesStreamingDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ProfileApi private val url: String,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope,
) {
    val profilesFlow: Flow<ProfilesEvent> = callbackFlow {
        val eventSource = startObservingProfiles(this)
        awaitClose { eventSource.cancel() }
    }.flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfilesEvent.Loading
        )

    private fun startObservingProfiles(
        producer: ProducerScope<ProfilesEvent>
    ): EventSource {
        val eventSourceListener = object : EventSourceListener() {
            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                Log.d("ProfilesRepository", "onClosed")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                Log.d("ProfilesRepository", "onEvent data = $data; id = $id; type = $type; ")

                if (data != "null") { // data == "null" means keep-alive message
                    coroutineScope.launch {
                        producer.send(ProfilesEvent.Loading)
                        delay(1000)
                        producer.send(ProfilesEvent.Data(extractProfilesList(data)))
                    }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                Log.d("ProfilesRepository", "onFailure; throwable = $t; response = $response")
                producer.trySend(ProfilesEvent.Error(t))
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                Log.d("ProfilesRepository", "onOpen; response = $response")
            }

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

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        val eventSource = EventSources
            .createFactory(okHttpClient)
            .newEventSource(request = request, listener = eventSourceListener)

        okHttpClient.newCall(request).enqueue(responseCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("ProfilesRepository; enqueue", "API call failure ${e.localizedMessage}")
                coroutineScope.launch {
                    producer.send(ProfilesEvent.Error(e))
                    producer.close(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ProfilesRepository; enqueue", "API call success ${response.body?.string()}")
            }
        })
        return eventSource
    }
}

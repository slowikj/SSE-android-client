package com.example.examplesse.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

sealed class Event<M> {
    data class Data<M>(val data: M) : Event<M>()

    data class Error<M>(val throwable: Throwable?) : Event<M>()
}

open class SSEDataSource<M>(
    private val okHttpClient: OkHttpClient,
    private val url: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope,
    private val dataExtractor: (String) -> M
) {
    val flow: Flow<Event<M>> = callbackFlow {
        val eventSource = startObservingProfiles(this)
        awaitClose { eventSource.cancel() }
    }.flowOn(ioDispatcher)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    private fun startObservingProfiles(
        producer: ProducerScope<Event<M>>
    ): EventSource {
        val eventSourceListener = object : EventSourceListener() {
            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                Log.d("SSEDataSource", "onClosed")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                Log.d("SSEDataSource", "onEvent data = $data; id = $id; type = $type; ")
                if (data != "null") { // data == "null" means keep-alive message
                    producer.trySend(Event.Data(dataExtractor(data)))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                Log.d("SSEDataSource", "onFailure; throwable = $t; response = $response")
                producer.trySend(Event.Error(t))
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                Log.d("SSEDataSource", "onOpen; response = $response")
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        return EventSources
            .createFactory(okHttpClient)
            .newEventSource(request = request, listener = eventSourceListener)
    }
}

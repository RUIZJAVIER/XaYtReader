package com.deedeedev.ytreader.data

import android.content.Context
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock

class AiChatRepositoryTest {
    private val repository = AiChatRepository(
        context = mock<Context>(),
        client = OkHttpClient()
    )

    @Test
    fun `buildEndpoint appends chat completions path to a base url`() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            repository.buildEndpoint("https://api.example.com/v1/")
        )
    }

    @Test
    fun `buildEndpoint keeps a full chat completions url`() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            repository.buildEndpoint("https://api.example.com/v1/chat/completions")
        )
    }

    @Test
    fun `parseResponseText returns the first assistant message`() {
        val body = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hola"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals("Hola", repository.parseResponseText(body))
    }

    @Test
    fun `parseResponseText rejects a response without content`() {
        assertNull(repository.parseResponseText("""{"choices":[{"message":{"content":null}}]}"""))
        assertNull(repository.parseResponseText("not json"))
    }
}

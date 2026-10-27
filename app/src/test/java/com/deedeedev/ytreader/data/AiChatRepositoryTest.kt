package com.deedeedev.ytreader.data

import android.content.Context
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `buildModelsEndpoint appends models path to a base url`() {
        assertEquals(
            "https://api.example.com/v1/models",
            repository.buildModelsEndpoint("https://api.example.com/v1/")
        )
        assertEquals(
            "https://api.example.com/v1/models",
            repository.buildModelsEndpoint("https://api.example.com/v1/models")
        )
    }

    @Test
    fun `parseModelIds extracts sorted and unique model ids`() {
        val body = """
            {
              "data": [
                { "id": "gpt-4o-mini", "object": "model" },
                { "id": "gpt-4o" },
                { "id": "gpt-4o" },
                { "id": "" },
                { "id": null }
              ]
            }
        """.trimIndent()

        assertEquals(listOf("gpt-4o", "gpt-4o-mini"), repository.parseModelIds(body))
    }

    @Test
    fun `parseModelIds returns empty list for invalid payloads`() {
        assertTrue(repository.parseModelIds("not json").isEmpty())
        assertTrue(repository.parseModelIds("""{"data":[]}""").isEmpty())
        assertTrue(repository.parseModelIds("""{"object":"list"}""").isEmpty())
    }
}

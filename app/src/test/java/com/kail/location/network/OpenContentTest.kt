package com.kail.location.network

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class OpenContentTest {
    private val bundled = """[{"id":1,"title":"Offline","answerMd":"Bundled help","sort":1}]"""
    private val remote = """[{"id":2,"title":"Online","answerMd":"Remote help","sort":1}]"""

    @Test fun offlineModeDoesNotReadCacheOrWriteFiles() {
        val repository = ContentRepository(null,
            { error("Offline mode must not read a provider cache") },
            { _, _ -> error("Offline mode must not write cache") }, { bundled })
        assertEquals("Offline", repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().title)
    }

    @Test fun successfulRemoteContentIsValidatedAndCached() {
        var saved: String? = null
        val repository = ContentRepository({ remote }, { error("Cache should not be read") },
            { name, json -> assertEquals("faqs.json", name); saved = json }, { bundled })
        assertEquals(2L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
        assertEquals(remote, saved)
    }

    @Test fun offlineRequestUsesLastValidCache() {
        val repository = ContentRepository({ throw IOException("Offline") }, { remote },
            { _, _ -> error("Must not write failed responses") }, { bundled })
        assertEquals(2L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
    }

    @Test fun malformedRemoteContentDoesNotPoisonCache() {
        var writes = 0
        val repository = ContentRepository({ "<html>error</html>" }, { remote },
            { _, _ -> writes++ }, { bundled })
        assertEquals(2L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
        assertEquals(0, writes)
    }

    @Test fun corruptCacheAndFailedRemoteFallBackToBundledContent() {
        val repository = ContentRepository({ throw IOException("HTTP 500") }, { "broken" },
            { _, _ -> }, { bundled })
        assertEquals(1L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
    }

    @Test fun cacheReadFailureFallsBackToBundledContent() {
        val repository = ContentRepository({ throw IOException() }, { throw IOException() },
            { _, _ -> }, { bundled })
        assertEquals(1L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
    }

    @Test fun cacheWriteFailureDoesNotDiscardFreshContent() {
        val repository = ContentRepository({ remote }, { null },
            { _, _ -> throw IOException("No space") }, { bundled })
        assertEquals(2L, repository.load("faqs.json", ContentParser::faqs).getOrThrow().single().id)
    }

    @Test fun emptyRemoteArrayIsAuthoritative() {
        val repository = ContentRepository({ "[]" }, { remote }, { _, _ -> }, { bundled })
        assertTrue(repository.load("faqs.json", ContentParser::faqs).getOrThrow().isEmpty())
    }

    @Test fun faqsAreSortedAndMarkdownIsPreserved() {
        val faqs = ContentParser.faqs("""[
            {"id":1,"title":"Second","answerMd":"**bold**\nnext","sort":2},
            {"id":2,"title":"First","answerMd":"first","sort":1}]
        """)
        assertEquals(listOf(2L, 1L), faqs.map { it.id })
        assertEquals("**bold**\nnext", faqs.last().answerMd)
    }

    @Test fun duplicateIdsAreRejected() {
        assertTrue(runCatching { ContentParser.faqs("[${bundled.removeSurrounding("[", "]")},${bundled.removeSurrounding("[", "]")}]") }.isFailure)
        val notice = """{"id":1,"title":"Notice","content":"Hello"}"""
        assertTrue(runCatching { ContentParser.notices("[$notice,$notice]") }.isFailure)
    }

    @Test fun missingRequiredFieldsAndProprietaryEnvelopeAreRejected() {
        assertTrue(runCatching { ContentParser.faqs("""[{"id":1,"title":"Missing answer"}]""") }.isFailure)
        assertTrue(runCatching { ContentParser.faqs("""{"code":0,"data":[]}""") }.isFailure)
    }

    @Test fun noticeOptionalFieldsHaveDefaults() {
        val notice = ContentParser.notices("""[{"id":9,"title":"Hello","content":"World"}]""").single()
        assertEquals(0, notice.type)
        assertEquals("", notice.createTime)
    }

    @Test fun allSourcesFailReturnsFailure() {
        val repository = ContentRepository({ "broken" }, { null }, { _, _ -> }, { "broken" })
        assertTrue(repository.load("faqs.json", ContentParser::faqs).isFailure)
    }
}

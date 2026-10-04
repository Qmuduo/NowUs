package app.nowus.android.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NowUsApiTest {
    @Test fun noteWireContractSendsRevisionAndRestoreNeverSendsDeletedText() = kotlinx.coroutines.runBlocking {
        val received=java.util.concurrent.CopyOnWriteArrayList<Triple<String,String,String>>()
        val headers=java.util.concurrent.CopyOnWriteArrayList<String>()
        val server=java.net.ServerSocket(0,3,java.net.InetAddress.getByName("127.0.0.1"))
        server.soTimeout=15_000
        val serverError=java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val worker=Thread {
            try {
                repeat(3) {
                    server.accept().use { socket ->
                        socket.soTimeout=15_000
                        val input=socket.getInputStream().bufferedReader(Charsets.UTF_8)
                        val request=input.readLine().split(" ")
                        val lines=mutableListOf<String>()
                        var line=input.readLine()
                        while(line.isNotEmpty()) { lines+=line;line=input.readLine() }
                        headers+=lines.joinToString("\n")
                        val size=lines.firstOrNull { it.startsWith("Content-Length:",true) }?.substringAfter(':')?.trim()?.toInt() ?: 0
                        val body=CharArray(size)
                        var read=0
                        while(read<size) { read+=input.read(body,read,size-read) }
                        received+=Triple(request[0],request[1],String(body))
                        val response="""{"note":{"text":"published","updatedAt":"2026-09-30T00:00:00.123456+00:00"}}"""
                        val packet=if(request[0]=="DELETE") "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n"
                            else "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.toByteArray().size}\r\nConnection: close\r\n\r\n$response"
                        socket.getOutputStream().write(packet.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch(error:Throwable) { serverError.set(error) }
        }.apply { isDaemon=true;start() }
        try {
            val client=NowUsApiClient("http://127.0.0.1:${server.localPort}")
            val saved=client.saveNote("token","published")
            client.deleteNote("token",saved.updatedAt)
            val restored=client.restoreNote("token",saved.updatedAt)
            worker.join(15_000)
            org.junit.Assert.assertNull(serverError.get())
            org.junit.Assert.assertEquals(3,headers.size)
            headers.forEach { org.junit.Assert.assertTrue(it.contains("Authorization: Bearer token")) }
            assertEquals("2026-09-30T00:00:00.123456+00:00",restored.updatedAt)
            assertEquals("""{"text":"published"}""",received[0].third)
            assertEquals("DELETE",received[1].first)
            assertEquals("/v1/me/note?expectedRevision=2026-09-30T00%3A00%3A00.123456%2B00%3A00",received[1].second)
            assertEquals("POST",received[2].first)
            assertEquals("/v1/me/note/restore",received[2].second)
            assertEquals("""{"expectedRevision":"2026-09-30T00:00:00.123456+00:00"}""",received[2].third)
        } finally { server.close();worker.join(1_000) }
    }

    @Test fun parsesZuluAndExplicitUtcOffsets() {
        val expected = Instant.parse("2026-10-30T07:36:18.754991Z")

        assertEquals(expected, parseApiTimestamp("2026-10-30T07:36:18.754991Z"))
        assertEquals(expected, parseApiTimestamp("2026-10-30T07:36:18.754991+00:00"))
    }
}

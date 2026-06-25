package uz.shuhratdev

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class AudioStreamService : Service() {

    private val TAG = "AudioStreamService"
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var audioTrack: AudioTrack? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            initAudioAndNetwork()
        }
        return START_STICKY
    }

    private fun initAudioAndNetwork() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME) // Minimal lag uchun
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            Log.d(TAG, "AudioTrack muvaffaqiyatli ishga tushdi.")
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack yaratishda xatolik: ${e.message}")
        }

        // Tarmoq oqimini alohida Thread ichida boshlaymiz
        thread(start = true, isDaemon = true) {
            try {
                val port = 12344
                // ADB tunnel uchun aynan localhost interfeysini tinglaymiz
                val localAddress = InetAddress.getByName("127.0.0.1")
                serverSocket = ServerSocket(port, 1, localAddress)

                Log.d(TAG, "[*] Server 127.0.0.1:$port portida ulanishni kutyapti...")

                while (isRunning) {
                    clientSocket = serverSocket?.accept()
                    Log.d(TAG, "[+] Kompyuter muvaffaqiyatli ulandi: ${clientSocket?.remoteSocketAddress}")

                    val inputStream = clientSocket?.getInputStream()
                    val buffer = ByteArray(512) // Kichik bufer = minimal lag

                    while (isRunning) {
                        val bytesRead = inputStream?.read(buffer) ?: -1
                        if (bytesRead == -1) {
                            Log.d(TAG, "[-] Kompyuter ulanishni uzdi.")
                            break
                        }
                        // Ovozni naushnikka (Earpods) chiqarish
                        audioTrack?.write(buffer, 0, bytesRead)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tarmoq xatoligi: ${e.message}")
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "AUDIO_STREAM_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Audio Stream", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PC Audio Sink")
            .setContentText("Kompyuterdan ovoz qabul qilinmoqda...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            clientSocket?.close()
            serverSocket?.close()
            audioTrack?.stop()
            audioTrack?.release()
            Log.d(TAG, "Servis to'xtatildi va resurslar tozalandi.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
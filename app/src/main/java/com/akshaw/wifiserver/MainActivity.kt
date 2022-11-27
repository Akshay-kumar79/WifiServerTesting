package com.akshaw.wifiserver

import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.akshaw.wifiserver.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var SERVER_IP: String? = ""
    val SERVER_PORT = 8080

    private var socket: Socket? = null

    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            SERVER_IP = getLocalIpAddress()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val serverSocket = ServerSocket(SERVER_PORT)
                withContext(Dispatchers.Main) {
                    binding.tvMessages.text = "Not connected"
                    binding.tvIP.text = "IP: $SERVER_IP"
                    binding.tvPort.text = "Port: $SERVER_PORT"
                }
                socket = serverSocket.accept()

                inputStream = socket!!.getInputStream()
                outputStream = socket!!.getOutputStream()

                withContext(Dispatchers.Main) {
                    binding.tvMessages.text = "Connected\n"
                }

                val buffer = ByteArray(1024)
                var bytes: Int
                while (socket != null) {
                    bytes = inputStream!!.read(buffer)
                    if (bytes > 0) {
                        val finalByte = bytes
                        withContext(Dispatchers.Main) {
                            val message = String(buffer, 0, finalByte)
                            binding.tvMessages.append("server: $message\n")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.v("MYTAG", "error: ${e.localizedMessage}")
            }
        }

        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        outputStream?.let {
                            it.write(message.toByteArray())
                            Log.v("MYTAG", "error: ${message}")
                        }
                        withContext(Dispatchers.Main){
                            binding.tvMessages.append("server: $message\n")
                            binding.etMessage.setText("")
                        }
                    } catch (e: IOException) {
                        Log.v("MYTAG", "error: ${e.localizedMessage}")
                    }
                }
            }
        }

    }

    @Throws(UnknownHostException::class)
    private fun getLocalIpAddress(): String? {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }
}
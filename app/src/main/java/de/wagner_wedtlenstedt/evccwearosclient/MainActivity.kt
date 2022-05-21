package de.wagner_wedtlenstedt.evccwearosclient

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View.GONE
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import de.wagner_wedtlenstedt.evccwearosclient.databinding.ActivityMainBinding
import de.wagner_wedtlenstedt.evccwearosclient.ui.MainEvccWidget
import de.wagner_wedtlenstedt.evccwearosclient.viewmodel.EvccViewModel


class MainActivity : ComponentActivity(){

    private lateinit var mainEvccWidget:MainEvccWidget

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm:EvccViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val progressBar = binding.progressBar

        val outMetrics = DisplayMetrics()
        baseContext.display?.getMetrics(outMetrics)

        val widgetSize = outMetrics.widthPixels.toFloat()

        mainEvccWidget = MainEvccWidget(widgetSize, binding.imageView, applicationContext, resources)

        vm = ViewModelProvider(this)[EvccViewModel::class.java]

        val connectivityManager = getSystemService(ConnectivityManager::class.java)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // The Wi-Fi network has been acquired, bind it to use this network by default
                connectivityManager.bindProcessToNetwork(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // The Wi-Fi network has been disconnected
            }
        }
        connectivityManager.requestNetwork(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            callback
        )

        vm.getEvccLiveData()?.observe(this) {
            if (it != null) {
                progressBar.visibility = GONE
                mainEvccWidget.update(it)
            } else {
                showToast()
            }
        }

        vm.startEvccStateUpdateRoutine()
    }

    private fun showToast(){
        Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
    }
}
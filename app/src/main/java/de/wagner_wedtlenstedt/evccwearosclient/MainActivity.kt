package de.wagner_wedtlenstedt.evccwearosclient

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import de.wagner_wedtlenstedt.evccwearosclient.databinding.ActivityMainBinding
import de.wagner_wedtlenstedt.evccwearosclient.ui.MainEvccWidget
import de.wagner_wedtlenstedt.evccwearosclient.viewmodel.EvccViewModel
import kotlin.math.abs
import kotlin.math.min
import android.view.View.VISIBLE
import kotlin.math.max


class MainActivity : ComponentActivity(){

    private lateinit var mainEvccWidget:MainEvccWidget

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm:EvccViewModel

    private val batteryPowerThreshold = 50

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                mainEvccWidget.update(it)

                updateTextElements(it,binding)
            } else {
                showToast()
            }
        }

        vm.startEvccStateUpdateRoutine()
    }

    private fun showToast(){
        Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
    }

    private fun updateTextElements(it: EvccStateModel, binding: ActivityMainBinding) {
        binding.textLayoutContainer1.visibility = VISIBLE
        binding.textLayoutContainer2.visibility = VISIBLE
        binding.textLayoutContainer3.visibility = VISIBLE
        binding.textLayoutContainer4.visibility = VISIBLE
        binding.textLayoutContainer5.visibility = VISIBLE
        binding.textLayoutContainer6.visibility = VISIBLE
        binding.textLayoutContainer7.visibility = VISIBLE
        binding.textLayoutContainer8.visibility = VISIBLE
        binding.textLayoutContainer9.visibility = VISIBLE

        val homePower = it.result?.homePower ?: 0.0f
        val batteryPower = it.result?.batteryPower ?: 0.0f
        val loadpointChargePower = it.result?.loadpoints?.first()?.chargePower ?: 0.0f
        val gridPower = it.result?.gridPower ?: 0.0f
        val isPvConfigured = it.result?.pvConfigured ?: false
        val pvPower = it.result?.pvPower ?: 0.0f
        val homeBatterySoc = it.result?.batterySoC ?: 0

        val gridImport = max(0.0f, gridPower)
        val pvExport = max(0.0f, gridPower * -1.0f)
        val pvProduction = if (isPvConfigured) abs(pvPower) else pvExport

        val batteryPowerAdjusted = if (abs(batteryPower) < batteryPowerThreshold) 0.0f else batteryPower
        val batteryDischarge = maxOf(0.0f, batteryPowerAdjusted)
        val batteryCharge = minOf(0.0f, batteryPowerAdjusted) * -1.0f

        val ownPower = batteryDischarge + pvProduction
        val consumption = homePower + batteryCharge + loadpointChargePower
        val selfConsumption = min(ownPower, consumption)



        val overallPowerConsumptions = selfConsumption + pvExport + gridImport

        var textViewInRight = binding.textViewInRight
        textViewInRight.text = "${pvProduction + batteryDischarge + gridImport} W"

        var textViewErzeugung = binding.textViewErzeugungsValue
        textViewErzeugung.text = "${pvProduction} W"

        var textViewBatterieEntladung = binding.textViewBatterieEntladungValue
        textViewBatterieEntladung.text = "${homeBatterySoc}% / ${batteryDischarge} W"

        var textViewNetzbezug = binding.textViewNetzbezugValue
        textViewNetzbezug.text = "${gridImport} W"

        var textViewOutRight = binding.textViewOutRight
        textViewOutRight.text = "${homePower + loadpointChargePower + batteryCharge + pvExport} W"

        var textViewHouse = binding.textViewHouseValue
        textViewHouse.text = "${homePower} W"

        var textViewLadepunkt = binding.textViewLadepunktValue
        textViewLadepunkt.text = "${loadpointChargePower} W"

        var textViewBatterieLadung = binding.textViewBatterieLadenValue
        textViewBatterieLadung.text = "${homeBatterySoc}% / ${batteryCharge} W"

        var textViewEinspeisung = binding.textViewEinspeisungValue
        textViewEinspeisung.text = "${pvExport} W"
    }
}
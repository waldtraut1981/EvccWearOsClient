package de.wagner_wedtlenstedt.evccwearosclient

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import de.wagner_wedtlenstedt.evccwearosclient.databinding.ActivityMainBinding
import de.wagner_wedtlenstedt.evccwearosclient.ui.MainEvccWidget
import de.wagner_wedtlenstedt.evccwearosclient.viewmodel.EvccViewModel
import kotlin.math.abs
import kotlin.math.max


class MainActivity : ComponentActivity(){

    private lateinit var mainEvccWidget:MainEvccWidget

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm:EvccViewModel

    private val batteryPowerThreshold = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val outMetrics = DisplayMetrics()
        baseContext.display?.getMetrics(outMetrics)

        val widgetSize = outMetrics.widthPixels.toFloat()

        mainEvccWidget = MainEvccWidget(widgetSize, binding.imageView, applicationContext, resources)

        vm = ViewModelProvider(this)[EvccViewModel::class.java]

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

        val textViewInRight = binding.textViewInRight
        val textViewErzeugung = binding.textViewErzeugungsValue
        val textViewBatterieEntladung = binding.textViewBatterieEntladungValue
        val textViewNetzbezug = binding.textViewNetzbezugValue
        val textViewOutRight = binding.textViewOutRight
        val textViewHouse = binding.textViewHouseValue
        val textViewLadepunkt = binding.textViewLadepunktValue
        val textViewBatterieLadung = binding.textViewBatterieLadenValue
        val textViewEinspeisung = binding.textViewEinspeisungValue

        textViewInRight.text = String.format("%.0f W",pvProduction + batteryDischarge + gridImport)
        textViewErzeugung.text = String.format("%.0f W",pvProduction)
        textViewBatterieEntladung.text = String.format("%d%% / %.0f W",homeBatterySoc,batteryDischarge)
        textViewNetzbezug.text = String.format("%.0f W",gridImport)
        textViewOutRight.text = String.format("%.0f W",homePower + loadpointChargePower + batteryCharge + pvExport)
        textViewHouse.text = String.format("%.0f W",homePower)
        textViewLadepunkt.text = String.format("%.0f W",loadpointChargePower)
        textViewBatterieLadung.text = String.format("%d%% / %.0f W",homeBatterySoc,batteryCharge)
        textViewEinspeisung.text = String.format("%.0f W",pvExport)
    }
}
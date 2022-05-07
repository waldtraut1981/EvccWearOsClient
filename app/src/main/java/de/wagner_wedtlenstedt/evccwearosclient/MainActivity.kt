package de.wagner_wedtlenstedt.evccwearosclient

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.caverock.androidsvg.SVG
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import de.wagner_wedtlenstedt.evccwearosclient.databinding.ActivityMainBinding
import de.wagner_wedtlenstedt.evccwearosclient.viewmodel.EvccViewModel
import java.lang.Integer.max
import java.lang.Math.toRadians
import kotlin.math.*


class MainActivity : ComponentActivity(){

    private val greenColor = Color.argb(255, 102, 216, 90)
    private val yellowColor = Color.argb(255, 200, 216, 90)
    private val redColor = Color.argb(255, 200, 0, 0)
    private val whiteColor = Color.argb(255, 255, 255, 255)
    private val blackColor = Color.argb(255, 0, 0, 0)
    private val darkGreyColor = Color.argb(255, 70, 70, 70)
    private val lightGreyColor = Color.argb(255, 150, 150, 150)

    private val batteryPowerThreshold = 50
    private val strokeEnergy = 25.0f
    private val strokeLegend = 2.0f
    private val width = 240.0f
    private val height = 240.0f
    private val paddingMiddle = 30.0f
    private val paddingLegend = 15.0f
    private val paddingInner = paddingMiddle + strokeEnergy + paddingLegend
    private val iconSize = 15.0f
    private val arcFullDegrees = 170.0f

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm:EvccViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageView = binding.imageView

        vm = ViewModelProvider(this)[EvccViewModel::class.java]

        vm.getEvccLiveData()?.observe(this) {
            if (it != null) {
                //TODO: retrieve size from view
                val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                updatePowerDistributionArc(it, canvas)
                updateLoadpointArc(it, canvas)
                updateCenterInfos(it,canvas)

                imageView.setImageBitmap(bitmap)
            } else {
                showToast()
            }
        }

        vm.startEvccStateUpdateRoutine()
    }

    private fun updateCenterInfos(it: EvccStateModel, canvas: Canvas) {
        val mode = it.result?.loadpoints?.first()?.mode ?: ""
        val vehicleRange = it.result?.loadpoints?.first()?.vehicleRange ?: 0.0f

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = whiteColor
        paint.strokeWidth = 0.25f

        canvas.drawText("Mode:",width/2.0f - 20.0f,height/2.0f - 20.0f,paint)
        canvas.drawText("$mode",width/2.0f - 20.0f,height/2.0f - 5.0f,paint)
        canvas.drawText("Range:",width/2.0f - 20.0f,height/2.0f + 10.0f,paint)
        canvas.drawText("$vehicleRange km",width/2.0f - 20.0f,height/2.0f + 25.0f,paint)
    }

    private fun updateLoadpointArc(it: EvccStateModel, canvas: Canvas) {
        val isCarConnected = it.result?.loadpoints?.first()?.connected ?: false
        val isCharging = it.result?.loadpoints?.first()?.enabled ?: false
        val vehicleSoC = it.result?.loadpoints?.first()?.vehicleSoC ?: 0
        val minSoC = it.result?.loadpoints?.first()?.minSoC ?: 0
        val targetSoC = it.result?.loadpoints?.first()?.targetSoC ?: 0
        val loadpointName = it.result?.loadpoints?.first()?.title ?: ""
        val vehicleName = it.result?.loadpoints?.first()?.vehicleTitle ?: ""

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = strokeEnergy
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = darkGreyColor

        val arcLoadpoint = RectF()
        arcLoadpoint.set((strokeEnergy/2) + paddingMiddle, (strokeEnergy/2) + paddingMiddle, width-paddingMiddle-(strokeEnergy/2), height-paddingMiddle-(strokeEnergy/2))

        val startAngleLoadpointArc = (180.0f - arcFullDegrees)/2.0f + 90

        canvas.drawArc(arcLoadpoint, startAngleLoadpointArc, arcFullDegrees, false, paint)

        val isBelowMinSoc = vehicleSoC < minSoC

        val socColor = if (isCarConnected) ( if(isBelowMinSoc) redColor else greenColor) else lightGreyColor

        val sweepAngleVehicleSoc = arcFullDegrees * vehicleSoC/100.0f
        drawLoadpointPart(
            startAngleLoadpointArc,
            sweepAngleVehicleSoc,
            canvas,
            arcLoadpoint,
            socColor,
            vehicleSoC,
            isCharging
        )

        drawSocLine(arcFullDegrees*targetSoC/100,targetSoC,canvas,whiteColor)

        if(isBelowMinSoc){
            drawSocLine(arcFullDegrees*minSoC/100,targetSoC,canvas,redColor)
        }

        drawLoadpointLegend(loadpointName,vehicleName,canvas)
    }

    private fun drawLoadpointLegend(loadpointName: String, vehicleName: String, canvas: Canvas) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = whiteColor
        paint.strokeWidth = 0.25f

        val arcLoadpointLegend = RectF()
        arcLoadpointLegend.set((strokeLegend/2) + paddingLegend, (strokeLegend/2) + paddingLegend, width-paddingLegend-(strokeLegend/2), height-paddingLegend-(strokeLegend/2))

        with(canvas) {
            val path = Path()
            path.addArc(arcLoadpointLegend, 90.0f + arcFullDegrees/2.0f - 20.0f, arcFullDegrees)
            val loadpointString = "$loadpointName - $vehicleName"
            drawTextOnPath(
                loadpointString,
                path,
                0.0f,
                5.0f,
                paint
            )
        }
    }

    private fun drawSocLine(angle: Float, targetSoC: Int, canvas: Canvas, lineColor: Int) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = lineColor
        paint.strokeWidth = strokeLegend

        val rotateVectorYStart = height/2.0f - paddingMiddle
        val rotateVectorYEnd = height/2.0f - paddingMiddle - strokeEnergy

        val degreeOffset = 180.0 + (180.0 - arcFullDegrees.toDouble())/2.0
        val rotatedXStart = width/2.0f + rotateVectorYStart * sin(toRadians(angle.toDouble() + degreeOffset))
        val rotatedYStart = height/2.0f - rotateVectorYStart * cos(toRadians(angle.toDouble() + degreeOffset))
        val rotatedXEnd = width/2.0f + rotateVectorYEnd * sin(toRadians(angle.toDouble() + degreeOffset))
        val rotatedYEnd = height/2.0f - rotateVectorYEnd * cos(toRadians(angle.toDouble() + degreeOffset))

        canvas.drawLine(rotatedXStart.toFloat(),rotatedYStart.toFloat(),rotatedXEnd.toFloat(),rotatedYEnd.toFloat(),paint)

    }

    private fun showToast(){
        Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
    }

    private fun updatePowerDistributionArc(
        evccStateModel: EvccStateModel,
        canvas: Canvas
    ) {
        val homePower = evccStateModel.result?.homePower ?: 0
        val batteryPower = evccStateModel.result?.batteryPower ?: 0
        val loadpointChargePower = evccStateModel.result?.loadpoints?.first()?.chargePower ?: 0
        val gridPower = evccStateModel.result?.gridPower ?: 0
        val isPvConfigured = evccStateModel.result?.pvConfigured ?: false
        val pvPower = evccStateModel.result?.pvPower ?: 0
        val homeBatterySoc = evccStateModel.result?.batterySoC ?: 0
        val batteryIconId = getBatteryIcon(homeBatterySoc)

        val gridImport = max(0,gridPower)
        val pvExport = max(0, gridPower * -1)
        val pvProduction = if (isPvConfigured) abs(pvPower) else pvExport

        val batteryPowerAdjusted = if (abs(batteryPower) < batteryPowerThreshold) 0 else batteryPower
        val batteryDischarge = max(0, batteryPowerAdjusted)
        val batteryCharge = min(0, batteryPowerAdjusted) * -1

        val ownPower = batteryDischarge + pvProduction
        val consumption = homePower + batteryCharge + loadpointChargePower
        val selfConsumption = min(ownPower, consumption)

        val overallPowerConsumptions = selfConsumption + pvExport + gridImport

        val arcEnergyFlow = RectF()
        val arcOuterLegend = RectF()
        val arcInnerLegend = RectF()
        arcEnergyFlow.set((strokeEnergy/2) + paddingMiddle, (strokeEnergy/2) + paddingMiddle, width-paddingMiddle-(strokeEnergy/2), height-paddingMiddle-(strokeEnergy/2))
        arcOuterLegend.set((strokeLegend/2) + paddingLegend, (strokeLegend/2) + paddingLegend, width-paddingLegend-(strokeLegend/2), height-paddingLegend-(strokeLegend/2))
        arcInnerLegend.set((strokeLegend/2) + paddingInner, (strokeLegend/2) + paddingInner, width-paddingInner-(strokeLegend/2), height-paddingInner-(strokeLegend/2))

        val sweepAngleSelfConsumption = arcFullDegrees * selfConsumption.toFloat()/overallPowerConsumptions.toFloat()
        val sweepAnglePvExport = arcFullDegrees * pvExport.toFloat()/overallPowerConsumptions.toFloat()
        val sweepAngleGridImport = arcFullDegrees * gridImport.toFloat()/overallPowerConsumptions.toFloat()

        drawEnergyFlowPart(
            0.0f,
                     sweepAngleSelfConsumption,
                     canvas,
                     arcEnergyFlow,
                     greenColor,
                     selfConsumption)
        drawEnergyFlowPart(
            sweepAngleSelfConsumption,
            sweepAnglePvExport,
            canvas,
            arcEnergyFlow,
            yellowColor,
            pvExport
        )
        drawEnergyFlowPart(
            sweepAngleSelfConsumption+sweepAnglePvExport,
            sweepAngleGridImport,
            canvas,
            arcEnergyFlow,
            redColor,
            gridImport
        )

        //draw outer elements
        val sweepAnglePvProduction = arcFullDegrees * pvProduction/(ownPower + gridImport)
        val sweepAngleBatteryDischarge = arcFullDegrees * batteryDischarge/(ownPower + gridImport)
        val sweepAngleGridPower = arcFullDegrees * gridImport/(ownPower + gridImport)

        drawLegendPart(canvas,
                       arcOuterLegend,
                       0.0f,
                       sweepAnglePvProduction,
                       listOf(R.raw.sun))
        drawLegendPart(canvas,
                       arcOuterLegend,
                       sweepAnglePvProduction,
                       sweepAngleBatteryDischarge,
                       listOf(batteryIconId, R.raw.anglesup))
        drawLegendPart(canvas,
                       arcOuterLegend,
              sweepAnglePvProduction + sweepAngleBatteryDischarge,
                       sweepAngleGridPower,
                       listOf(R.raw.grid,R.raw.anglesup))

        //draw inner elements
        val sweepAngleHomePower = arcFullDegrees * homePower/(consumption+pvExport)
        val sweepAngleBatteryChargePower = arcFullDegrees * batteryCharge/(consumption+pvExport)
        val sweepAngleLoadpointChargePower = arcFullDegrees * loadpointChargePower/(consumption+pvExport)

        drawLegendPart(canvas,
            arcInnerLegend,
            0.0f,
            sweepAngleHomePower,
            listOf(R.raw.house),
            true)

        drawLegendPart(canvas,
            arcInnerLegend,
            sweepAngleHomePower,
            sweepAngleBatteryChargePower,
            listOf(batteryIconId,R.raw.anglesdown),
            true)

        drawLegendPart(canvas,
            arcInnerLegend,
            sweepAngleHomePower + sweepAngleBatteryChargePower,
            sweepAngleLoadpointChargePower,
            listOf(R.raw.car),
            true)

        drawLegendPart(canvas,
            arcInnerLegend,
            sweepAngleHomePower + sweepAngleBatteryChargePower + sweepAngleLoadpointChargePower,
            sweepAnglePvExport,
            listOf(R.raw.grid,R.raw.anglesdown),
            true)
    }

    private fun getBatteryIcon(soc:Int): Int {
        return when{
            soc > 80 -> R.raw.batteryfull
            soc > 60 -> R.raw.batterythreequarters
            soc > 40 -> R.raw.batteryhalf
            soc > 20 -> R.raw.batteryquarter
            else -> R.raw.batteryempty
        }
    }

    private fun drawLegendPart(canvas: Canvas,
                               rect: RectF,
                               startAngle: Float,
                               sweepAngle: Float,
                               resourceIds: List<Int>,
                               isInner: Boolean = false){
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = whiteColor
        paint.strokeWidth = strokeLegend

        val padding = if (isInner) 3*paddingLegend+strokeEnergy else paddingLegend+1

        drawLine(padding, canvas,paint, startAngle, isInner)

        canvas.drawArc(rect, startAngle - arcFullDegrees/2.0f, sweepAngle, false, paint)

        drawLine(padding, canvas,paint,startAngle + sweepAngle, isInner)

        if(sweepAngle > iconSize * resourceIds.size) {
            val rotateVectorYStart = height / 2.0f - padding

            val degreeOffset = (180.0 - arcFullDegrees.toDouble())/2.0

            val rotatedXStart =
                width / 2.0f + rotateVectorYStart * sin(toRadians(startAngle.toDouble() + degreeOffset + sweepAngle.toDouble() / 2.0))
            val rotatedYStart =
                height / 2.0f - rotateVectorYStart * cos(toRadians(startAngle.toDouble() + degreeOffset + sweepAngle.toDouble() / 2.0))

            paint.style = Paint.Style.FILL
            paint.color = blackColor

            val iconRect = RectF()
            iconRect.set(
                rotatedXStart.toFloat() - iconSize/2.0f,
                rotatedYStart.toFloat() - iconSize/2.0f,
                rotatedXStart.toFloat() + iconSize/2.0f,
                rotatedYStart.toFloat() + iconSize/2.0f
            )

            for(resourceId in resourceIds){
                canvas.drawRect(iconRect, paint)

                val svg = SVG.getFromResource(resources, resourceId)
                svg.documentHeight = iconSize
                svg.documentWidth = iconSize
                svg.renderToCanvas(canvas, iconRect)

                iconRect.set(iconRect.left,iconRect.top-iconSize,iconRect.right,iconRect.bottom-iconSize)
            }
        }
    }

    private fun drawEnergyFlowPart(
        startAngle: Float,
        sweepAngle: Float,
        canvas: Canvas,
        rect: RectF,
        color: Int,
        power: Int
    ) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = strokeEnergy
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = color

        canvas.drawArc(rect, startAngle - arcFullDegrees/2.0f, sweepAngle, false, paint)

        if(sweepAngle > 30.0f) {
            paint.color = blackColor
            paint.strokeWidth = 0.25f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeCap = Paint.Cap.ROUND

            //val strokeLength = Math.PI * (rect.width() - strokeEnergy) * sweepAngle / 360.0 * 0.5
            with(canvas) {
                val path = Path()
                path.addArc(rect, startAngle - arcFullDegrees/2.0f, sweepAngle)
                val powerString = createPowerString(power)
                drawTextOnPath(
                    powerString,
                    path,
                    5.0f,//strokeLength.toFloat() + if(power>1000) 5.0f else 0.0f,
                    strokeEnergy / 5.0f,
                    paint
                )
            }
        }
    }

    private fun drawLoadpointPart(
        startAngle: Float,
        sweepAngle: Float,
        canvas: Canvas,
        rect: RectF,
        color: Int,
        soc: Int,
        isCharging: Boolean
    ) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = strokeEnergy
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = color

        canvas.drawArc(rect, startAngle, sweepAngle, false, paint)

        if(sweepAngle > 30.0f) {
            paint.color = blackColor
            paint.strokeWidth = 0.25f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeCap = Paint.Cap.ROUND

            //val strokeLength = Math.PI * (rect.width() - strokeEnergy) * sweepAngle / 360.0 * 0.5
            with(canvas) {
                val path = Path()
                path.addArc(rect, startAngle+sweepAngle, -sweepAngle)
                val socString = "$soc %"
                drawTextOnPath(
                    socString,
                    path,
                    20.0f,//strokeLength.toFloat() + if(power>1000) 5.0f else 0.0f,
                    strokeEnergy / 5.0f,
                    paint
                )
            }
        }
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)


    private fun createPowerString(value: Int):String {
        return if(value > 1000) "${(value.toFloat()/1000.0f).format(1)} W" else "$value W"
    }

    private fun drawLine(
        padding: Float,
        canvas: Canvas,
        paint: Paint,
        angle: Float = 0.0f,
        isInner: Boolean
    ) {
        val offset = if(isInner) -10.0f else 10.0f
        val rotateVectorYStart = height/2.0f - padding
        val rotateVectorYEnd = height/2.0f - (padding + offset)

        val degreeOffset = (180.0 - arcFullDegrees.toDouble())/2.0
        val rotatedXStart = width/2.0f + rotateVectorYStart * sin(toRadians(angle.toDouble() + degreeOffset))
        val rotatedYStart = height/2.0f - rotateVectorYStart * cos(toRadians(angle.toDouble() + degreeOffset))
        val rotatedXEnd = width/2.0f + rotateVectorYEnd * sin(toRadians(angle.toDouble() + degreeOffset))
        val rotatedYEnd = height/2.0f - rotateVectorYEnd * cos(toRadians(angle.toDouble() + degreeOffset))

        canvas.drawLine(rotatedXStart.toFloat(),rotatedYStart.toFloat(),rotatedXEnd.toFloat(),rotatedYEnd.toFloat(),paint)
    }
}
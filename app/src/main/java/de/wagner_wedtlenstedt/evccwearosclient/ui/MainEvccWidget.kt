package de.wagner_wedtlenstedt.evccwearosclient.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.Paint.Style
import android.graphics.Paint.Style.*
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat.getFont
import com.caverock.androidsvg.SVG
import de.wagner_wedtlenstedt.evccwearosclient.R
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import kotlin.math.*

class MainEvccWidget(
    private val widgetSize: Float,
    private val imageView: ImageView,
    private val applicationContext: Context,
    private val resources: Resources
) {

    private lateinit var currentEvccState: EvccStateModel
    private val greenColor = Color.argb(255, 15, 222, 65)
    private val yellowColor = Color.argb(255, 250, 240, 0)
    private val orangeColor = Color.argb(255, 250, 125, 0)
    private val whiteColor = Color.argb(255, 255, 255, 255)
    private val blackColor = Color.argb(255, 0, 0, 0)
    private val darkGreyColor = Color.argb(255, 70, 70, 70)
    private val lightGreyColor = Color.argb(255, 150, 150, 150)

    private val batteryPowerThreshold = 50
    private var strokeEnergy = 25.0f
    private var strokeLegend = 2.0f
    private var paddingMiddle = 30.0f
    private var paddingLegend = 15.0f
    private var paddingInner = paddingMiddle + strokeEnergy + paddingLegend
    private var iconSize = 20.0f
    private val arcFullDegrees = 170.0f
    private var leftPaddingCenterText = 20.0f
    private val startAngleLoadpointArc = (180.0f - arcFullDegrees)/2.0f + 90

    private var textSize = 12.0f

    init {
        val factor = widgetSize/240.0f

        strokeEnergy *= factor
        strokeLegend *= factor
        paddingMiddle *= factor
        paddingLegend *= factor
        paddingInner = paddingMiddle + strokeEnergy + paddingLegend
        iconSize *= factor
        textSize *= factor
        leftPaddingCenterText *= factor

        /*val clickListener: View.OnClickListener = View.OnClickListener { v ->
            if (v.equals(imageView)) {
                println("available vehicles: " + currentEvccState?.result?.vehicles)
            }
        }

        imageView.setOnClickListener(clickListener)*/
    }

    fun update(it: EvccStateModel) {
        currentEvccState = it
        val bitmap = Bitmap.createBitmap(widgetSize.toInt(), widgetSize.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        updatePowerDistributionArc(it, canvas)
        updateLoadpointArc(it, canvas)
        updateCenterInfos(it,canvas)

        imageView.setImageBitmap(bitmap)
    }

    private fun createBasePaint(style: Style, color: Int, strokeWidth: Float): Paint {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.strokeCap = Paint.Cap.BUTT
        paint.style = style
        paint.color = color
        paint.strokeWidth = strokeWidth
        return paint
    }

    private fun createFontPaint(color: Int, bold: Boolean = false): Paint {
        val font = if(bold){
            getFont(applicationContext, R.font.montserratbold)
        } else{
            getFont(applicationContext, R.font.montserratmedium)
        }

        val paint = createBasePaint(FILL_AND_STROKE, color,0.25f)
        paint.typeface = font
        paint.textSize = textSize

        return paint
    }

    private fun createArcPaint(color: Int): Paint {
        return createBasePaint(STROKE, color, strokeEnergy)
    }

    private fun createLinePaint(color: Int): Paint {
        return createBasePaint(STROKE, color, strokeLegend)
    }

    private fun createIconBackgroundPaint(color: Int): Paint {
        return createBasePaint(FILL, color, strokeLegend)
    }

    private fun updateCenterInfos(it: EvccStateModel, canvas: Canvas) {
        val mode = it.result?.loadpoints?.first()?.mode ?: ""
        val vehicleRange = it.result?.loadpoints?.first()?.vehicleRange ?: 0.0f

        val paintMedium = createFontPaint(whiteColor)

        val xPosition = widgetSize/2.0f - leftPaddingCenterText
        canvas.drawText("Mode:",xPosition,widgetSize/2.0f - textSize*2.0f,paintMedium)
        canvas.drawText("Range:",xPosition,widgetSize/2.0f + textSize,paintMedium)

        val paintBold = createFontPaint(whiteColor, true)

        canvas.drawText(mode,xPosition,widgetSize/2.0f - textSize,paintBold)
        canvas.drawText("$vehicleRange km",xPosition,widgetSize/2.0f + 2*textSize,paintBold)

        val connected = it.result?.loadpoints?.first()?.connected ?: false
        val charging = it.result?.loadpoints?.first()?.charging?: false

        val iconRect = RectF()

        val xValue = widgetSize/2.0f - widgetSize * 0.2f
        val yValue = widgetSize/2.0f

        iconRect.set(
            xValue - iconSize/2.0f,
            yValue - iconSize/2.0f,
            xValue + iconSize/2.0f,
            yValue + iconSize/2.0f
        )

        val svg: SVG = if(connected){
            if(charging){
                SVG.getFromResource(resources, R.raw.lightning)
            } else {
                SVG.getFromResource(resources, R.raw.car)
            }
        } else {
            SVG.getFromResource(resources, R.raw.cablecharge)
        }

        svg.documentHeight = iconSize
        svg.documentWidth = iconSize
        svg.renderToCanvas(canvas, iconRect)
    }

    private fun createEnergyRect(): RectF {
        val rect = RectF()
        rect.set((strokeEnergy/2) + paddingMiddle, (strokeEnergy/2) + paddingMiddle, widgetSize-paddingMiddle-(strokeEnergy/2), widgetSize-paddingMiddle-(strokeEnergy/2))
        return rect
    }

    private fun createLoadpointRect(): RectF {
        val rect = RectF()
        rect.set((strokeLegend/2) + paddingLegend, (strokeLegend/2) + paddingLegend, widgetSize-paddingLegend-(strokeLegend/2), widgetSize-paddingLegend-(strokeLegend/2))
        return rect
    }

    private fun updateLoadpointArc(it: EvccStateModel, canvas: Canvas) {
        val isCarConnected = it.result?.loadpoints?.first()?.connected ?: false
        val vehicleSoc = it.result?.loadpoints?.first()?.vehicleSoc ?: 0
        val minSoc = it.result?.loadpoints?.first()?.minSoc ?: 0
        val targetSoc = it.result?.loadpoints?.first()?.targetSoc ?: 0
        val loadpointName = it.result?.loadpoints?.first()?.title ?: ""
        val vehicleName = it.result?.loadpoints?.first()?.vehicleTitle ?: ""

        val paint = createArcPaint(darkGreyColor)
        val isBelowMinSoc = vehicleSoc < minSoc

        val arcLoadpoint = createEnergyRect()
        val sweepAngleVehicleSoc = arcFullDegrees * vehicleSoc/100.0f

        canvas.drawArc(arcLoadpoint, startAngleLoadpointArc, arcFullDegrees, false, paint)

        val socColor = if (isCarConnected) ( if(isBelowMinSoc) orangeColor else greenColor) else lightGreyColor

        drawLoadpointPart(
            sweepAngleVehicleSoc,
            canvas,
            arcLoadpoint,
            socColor,
            vehicleSoc
        )

        drawSocLine(arcFullDegrees*targetSoc/100,canvas,whiteColor)

        if(isBelowMinSoc){
            drawSocLine(arcFullDegrees*minSoc/100,canvas,orangeColor)
        }

        drawLoadpointLegend(loadpointName,vehicleName,canvas)
    }

    private fun drawLoadpointLegend(loadpointName: String, vehicleName: String, canvas: Canvas) {
        val paint = createFontPaint(whiteColor)

        val arcLoadpointLegend = createLoadpointRect()

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

    private fun updatePowerDistributionArc(
        evccStateModel: EvccStateModel,
        canvas: Canvas
    ) {
        val homePower = evccStateModel.result?.homePower ?: 0.0f
        val batteryPower = evccStateModel.result?.batteryPower ?: 0.0f
        val loadpointChargePower = evccStateModel.result?.loadpoints?.first()?.chargePower ?: 0.0f
        val gridPower = evccStateModel.result?.gridPower ?: 0.0f
        val isPvConfigured = evccStateModel.result?.pvConfigured ?: false
        val pvPower = evccStateModel.result?.pvPower ?: 0.0f
        val homeBatterySoc = evccStateModel.result?.batterySoC ?: 0
        val batteryIconId = getBatteryIcon(homeBatterySoc)

        val gridImport = max(0.0f, gridPower)
        val pvExport = max(0.0f, gridPower * -1.0f)
        val pvProduction = if (isPvConfigured) abs(pvPower) else pvExport

        val batteryPowerAdjusted = if (abs(batteryPower) < batteryPowerThreshold) 0.0f else batteryPower
        val batteryDischarge = max(0.0f, batteryPowerAdjusted)
        val batteryCharge = min(0.0f, batteryPowerAdjusted) * -1.0f

        val ownPower = batteryDischarge + pvProduction
        val consumption = homePower + batteryCharge + loadpointChargePower
        val selfConsumption = min(ownPower, consumption)

        val overallPowerConsumptions = selfConsumption + pvExport + gridImport

        val arcEnergyFlow = RectF()
        val arcOuterLegend = RectF()
        val arcInnerLegend = RectF()
        arcEnergyFlow.set((strokeEnergy/2) + paddingMiddle, (strokeEnergy/2) + paddingMiddle, widgetSize-paddingMiddle-(strokeEnergy/2), widgetSize-paddingMiddle-(strokeEnergy/2))
        arcOuterLegend.set((strokeLegend/2) + paddingLegend, (strokeLegend/2) + paddingLegend, widgetSize-paddingLegend-(strokeLegend/2), widgetSize-paddingLegend-(strokeLegend/2))
        arcInnerLegend.set((strokeLegend/2) + paddingInner, (strokeLegend/2) + paddingInner, widgetSize-paddingInner-(strokeLegend/2), widgetSize-paddingInner-(strokeLegend/2))

        val sweepAngleSelfConsumption = arcFullDegrees * selfConsumption / overallPowerConsumptions
        val sweepAnglePvExport = arcFullDegrees * pvExport / overallPowerConsumptions
        val sweepAngleGridImport = arcFullDegrees * gridImport / overallPowerConsumptions

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
            orangeColor,
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
            listOf(R.raw.anglesup,batteryIconId))
        drawLegendPart(canvas,
            arcOuterLegend,
            sweepAnglePvProduction + sweepAngleBatteryDischarge,
            sweepAngleGridPower,
            listOf(R.raw.anglesup,R.raw.grid))

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
            listOf(R.raw.anglesdown,batteryIconId),
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
            listOf(R.raw.anglesdown,R.raw.grid),
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
        val paint = createLinePaint(whiteColor)

        val padding = if (isInner) 3*paddingLegend+strokeEnergy else paddingLegend+1

        drawLegendLine(padding, canvas,paint, startAngle, isInner)

        canvas.drawArc(rect, startAngle - arcFullDegrees/2.0f, sweepAngle, false, paint)

        drawLegendLine(padding, canvas,paint,startAngle + sweepAngle, isInner)

        if(sweepAngle > iconSize * resourceIds.size) {
            val rotateVectorYStart = widgetSize / 2.0f - padding

            val degreeOffset = (180.0 - arcFullDegrees.toDouble())/2.0

            val degreesPerIcon = atan(iconSize.toDouble()/rotateVectorYStart) * 180.0 / Math.PI
            val rotationAngleStartInDegree = startAngle.toDouble() + degreeOffset+ sweepAngle.toDouble() / 2.0 - (degreesPerIcon * (resourceIds.size - 1))/2.0

            for((index,resourceId) in resourceIds.withIndex()){
                val rotationAngleRad = Math.toRadians(rotationAngleStartInDegree + index*degreesPerIcon)

                val rotatedXStart = widgetSize / 2.0f + rotateVectorYStart * sin(rotationAngleRad)
                val rotatedYStart = widgetSize / 2.0f - rotateVectorYStart * cos(rotationAngleRad)

                val iconBackgroundPaint = createIconBackgroundPaint(blackColor)

                val iconRect = RectF()
                iconRect.set(
                    rotatedXStart.toFloat() - iconSize/2.0f,
                    rotatedYStart.toFloat() - iconSize/2.0f,
                    rotatedXStart.toFloat() + iconSize/2.0f,
                    rotatedYStart.toFloat() + iconSize/2.0f
                )

                canvas.drawRect(iconRect, iconBackgroundPaint)

                val svg = SVG.getFromResource(resources, resourceId)
                svg.documentHeight = iconSize
                svg.documentWidth = iconSize
                svg.renderToCanvas(canvas, iconRect)
            }
        }
    }

    private fun drawEnergyFlowPart(
        startAngle: Float,
        sweepAngle: Float,
        canvas: Canvas,
        rect: RectF,
        color: Int,
        power: Float
    ) {
        val paint = createArcPaint(color)
        val sAngle = startAngle - arcFullDegrees/2.0f

        canvas.drawArc(rect, sAngle, sweepAngle, false, paint)

        if(sweepAngle > 30.0f) {
            val textPaint = createFontPaint(blackColor)

            with(canvas) {
                val path = Path()
                path.addArc(rect, sAngle, sweepAngle)
                drawTextOnPath(
                    createPowerString(power),
                    path,
                    5.0f,
                    strokeEnergy / 5.0f,
                    textPaint
                )
            }
        }
    }

    private fun drawLoadpointPart(
        sweepAngle: Float,
        canvas: Canvas,
        rect: RectF,
        color: Int,
        soc: Int
    ) {
        val paint = createArcPaint(color)

        canvas.drawArc(rect, startAngleLoadpointArc, sweepAngle, false, paint)

        if(sweepAngle > 30.0f) {
            val textPaint = createFontPaint(blackColor)

           with(canvas) {
                val path = Path()
                path.addArc(rect, startAngleLoadpointArc+sweepAngle, -sweepAngle)
                drawTextOnPath(
                    "$soc %",
                    path,
                    20.0f,
                    strokeEnergy / 5.0f,
                    textPaint
                )
            }
        }
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)


    private fun createPowerString(value: Float):String {
        return if(value > 1000)
            "${(value/1000.0f).format(1)} kW"
        else
            "${value.format(0)} W"
    }

    private fun drawSocLine(angle: Float, canvas: Canvas, lineColor: Int) {
        val paint = createLinePaint(lineColor)

        val rotateVectorYStart = widgetSize/2.0f - paddingMiddle
        val rotateVectorYEnd = widgetSize/2.0f - paddingMiddle - strokeEnergy

        drawLine(canvas,paint,angle,rotateVectorYStart,rotateVectorYEnd,false)
    }

    private fun drawLegendLine(
        padding: Float,
        canvas: Canvas,
        paint: Paint,
        angle: Float = 0.0f,
        isInner: Boolean
    ) {
        val offset = if(isInner) -10.0f else 10.0f
        val rotateVectorYStart = widgetSize/2.0f - padding
        val rotateVectorYEnd = widgetSize/2.0f - (padding + offset)

        drawLine(canvas,paint,angle,rotateVectorYStart,rotateVectorYEnd)
    }

    private fun drawLine(
        canvas: Canvas,
        paint: Paint,
        angle: Float = 0.0f,
        yStart: Float,
        yEnd: Float,
        rightSide:Boolean = true
    ) {
        val rotatedXStart = rotateX(yStart,angle,rightSide)
        val rotatedYStart = rotateY(yStart,angle,rightSide)
        val rotatedXEnd = rotateX(yEnd,angle,rightSide)
        val rotatedYEnd = rotateY(yEnd,angle,rightSide)

        canvas.drawLine(rotatedXStart,rotatedYStart,rotatedXEnd,rotatedYEnd,paint)
    }

    private fun rotateX(y:Float,angle:Float,rightSide:Boolean = true):Float {
        val sideOffset = if (rightSide) 0.0 else 180.0
        val degreeOffset = sideOffset + (180.0 - arcFullDegrees.toDouble())/2.0
        return widgetSize/2.0f + y * sin(Math.toRadians(angle.toDouble() + degreeOffset)).toFloat()
    }

    private fun rotateY(y:Float,angle:Float,rightSide:Boolean = true):Float {
        val sideOffset = if (rightSide) 0.0 else 180.0
        val degreeOffset = sideOffset + (180.0 - arcFullDegrees.toDouble())/2.0
        return widgetSize/2.0f - y * cos(Math.toRadians(angle.toDouble() + degreeOffset)).toFloat()
    }
}
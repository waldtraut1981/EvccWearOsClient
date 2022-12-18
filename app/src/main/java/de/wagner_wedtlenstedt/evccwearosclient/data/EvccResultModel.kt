package de.wagner_wedtlenstedt.evccwearosclient.data

data class EvccResultModel(
    var batteryConfigured:Boolean?=false,
    var batteryPower:Float?=0.0f,
    var batterySoC:Int?=0,
    var gridConfigured:Boolean?=false,
    var gridPower:Float?=0.0f,
    var homePower:Float?=0.0f,
    var loadpoints:List<EvccLoadpointModel>?=null,
    var prioritySoC:Int?=0,
    var pvConfigured:Boolean?=false,
    var pvPower:Float?=0.0f,
    var siteTitle:String?=""
)

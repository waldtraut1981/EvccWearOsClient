package de.wagner_wedtlenstedt.evccwearosclient.data

data class EvccResultModel(
    var batteryConfigured:Boolean?=false,
    var batteryPower:Int?=0,
    var batterySoC:Int?=0,
    var gridConfigured:Boolean?=false,
    var gridPower:Int?=0,
    var homePower:Int?=0,
    var loadpoints:List<EvccLoadpointModel>?=null,
    var prioritySoC:Int?=0,
    var pvConfigured:Boolean?=false,
    var pvPower:Int?=0,
    var siteTitle:String?=""
)

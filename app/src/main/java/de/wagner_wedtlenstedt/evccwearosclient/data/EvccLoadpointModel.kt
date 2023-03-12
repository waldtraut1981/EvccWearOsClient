package de.wagner_wedtlenstedt.evccwearosclient.data

data class EvccLoadpointModel(
    var chargePower:Float?=0.0f,
    var charging:Boolean?=false,
    var connected:Boolean?=false,
    var enabled:Boolean?=false,
    var minSoc:Int?=0,
    var mode:String?="",
    var targetSoc:Int?=0,
    var title:String?="",
    var vehicleRange:Int?=0,
    var vehicleSoc:Int?=0,
    var vehicleTitle:String?=""
)

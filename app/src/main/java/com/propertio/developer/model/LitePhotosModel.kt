package com.propertio.developer.model

data class LitePhotosModel (
    val id : Int? = null,
    val projectId : String? = null,
    var filePath : String? = null,
    var isCover : Int = 0,
    var caption : String? = null,
)

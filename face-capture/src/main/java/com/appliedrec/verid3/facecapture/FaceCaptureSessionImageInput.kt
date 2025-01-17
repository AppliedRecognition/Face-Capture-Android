package com.appliedrec.verid3.facecapture

import com.appliedrec.verid3.common.IImage

data class FaceCaptureSessionImageInput(val serialNumber: ULong, val time: Long, val image: IImage)

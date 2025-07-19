package com.servicecenter.l2validation.data.local.entities
// Code Reviewed
import android.os.Parcel
import android.os.Parcelable

data class QualityCheck(
    var qc_parameter_id: Int,
    var qc_question: String?="",
    var answer: String?=""
): Parcelable {
  constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(qc_parameter_id)
        parcel.writeString(qc_question)
        parcel.writeString(answer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<QualityCheck> {
        override fun createFromParcel(parcel: Parcel): QualityCheck {
            return QualityCheck(parcel)
        }

        override fun newArray(size: Int): Array<QualityCheck?> {
            return arrayOfNulls(size)
        }
    }
}



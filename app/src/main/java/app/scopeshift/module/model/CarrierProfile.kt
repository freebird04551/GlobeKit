package app.scopeshift.module.model

class CarrierProfile(
    val id: String,
    val displayName: String,
    val mcc: String,
    val mnc: String,
) {
    val operatorNumeric: String = mcc + mnc

    val mccInt: Int = mcc.toInt()

    val mncInt: Int = mnc.toInt()

    override fun toString(): String = displayName
}

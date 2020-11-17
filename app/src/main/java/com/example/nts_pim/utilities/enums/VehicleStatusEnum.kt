package com.example.nts_pim.utilities.enums

/**
 * Trip status was changed from Pick_up to PICK_UP
 */

enum class VehicleStatusEnum(val status: String) {
    TRIP_ASSIGNED("Assigned"),
    TRIP_ON_SITE("On_Site"),
    TRIP_PICKED_UP("PICKED_UP"),
    TRIP_END("End"),
    Trip_Closed("Closed")
}

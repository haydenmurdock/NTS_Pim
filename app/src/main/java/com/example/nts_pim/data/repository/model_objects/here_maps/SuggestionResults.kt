package com.example.nts_pim.data.repository.model_objects.here_maps

import org.json.JSONObject
import android.util.Log


class SuggestionResults(json: String):JSONObject(json) {

    var title = this.optString(JSON_TITLE)
    var highlightedTitle = this.optString(JSON_HIGHLIGHTED_TITLE)
    val vicinity = this.optString(JSON_VICINITY)
    var highlightedVicinity = this.optString(JSON_HIGHLIGHTED_VICINITY).replace("\n", " ")
    val tag = this.optString(JSON_TAG)
    val lat = this.optDouble(JSON_LAT)
    val lng = this.optDouble(JSON_LNG)
    val distance = this.getDouble(JSON_DISTANCE)



    companion object {
        private const val JSON_TITLE = "title"
        private const val JSON_HIGHLIGHTED_TITLE = "highlightedTitle"
        private const val JSON_VICINITY = "vicinity"
        private const val JSON_HIGHLIGHTED_VICINITY = "highlightedVicinity"
        private const val JSON_TAG = "tag"
        private const val JSON_LAT = "lat"
        private const val JSON_LNG = "lng"
        private const val JSON_DISTANCE = "distance"
    }


}
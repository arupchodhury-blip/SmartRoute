package ca.smartroute.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MainActivity : Activity(), LocationListener {
    private lateinit var map: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var speedText: TextView
    private lateinit var statusText: TextView
    private lateinit var destinationInput: EditText
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var lastLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 410
        private val OTTAWA = GeoPoint(45.4215, -75.6972)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 18, 16, 12)
            setBackgroundColor(Color.rgb(18, 24, 32))
        }

        val heading = TextView(this).apply {
            text = "SmartRoute"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(2, 2, 2, 12)
        }
        root.addView(heading)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        destinationInput = EditText(this).apply {
            hint = "Search destination or address"
            setSingleLine(true)
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(14, 8, 14, 8)
            background = roundedBackground(Color.rgb(39, 48, 60), 12)
        }
        searchRow.addView(destinationInput, LinearLayout.LayoutParams(0, dp(52), 1f))
        val searchButton = Button(this).apply {
            text = "SEARCH"
            setOnClickListener { searchDestination() }
        }
        searchRow.addView(searchButton, LinearLayout.LayoutParams(dp(104), dp(52)))
        root.addView(searchRow)

        destinationInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchDestination()
                true
            } else false
        }

        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(OTTAWA)
            setBackgroundColor(Color.rgb(30, 38, 48))
        }
        root.addView(map, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 8, 14, 8)
            background = roundedBackground(Color.rgb(39, 48, 60), 12)
        }
        val speedColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        speedColumn.addView(TextView(this).apply {
            text = "CURRENT SPEED"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        })
        speedText = TextView(this).apply {
            text = "-- km/h"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        speedColumn.addView(speedText)
        infoPanel.addView(speedColumn, LinearLayout.LayoutParams(0, -2, 1f))
        statusText = TextView(this).apply {
            text = "Map ready • Ottawa"
            textSize = 12f
            setTextColor(Color.rgb(145, 220, 205))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        infoPanel.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        val panelParams = LinearLayout.LayoutParams(-1, -2)
        panelParams.topMargin = dp(10)
        root.addView(infoPanel, panelParams)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val myLocationButton = Button(this).apply {
            text = "MY LOCATION"
            setOnClickListener { requestLocationAndCenter() }
        }
        actions.addView(myLocationButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        val clearButton = Button(this).apply {
            text = "CLEAR PIN"
            setOnClickListener {
                destinationMarker?.let { map.overlays.remove(it) }
                destinationMarker = null
                map.invalidate()
                statusText.text = "Destination cleared"
            }
        }
        actions.addView(clearButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        val actionParams = LinearLayout.LayoutParams(-1, -2)
        actionParams.topMargin = dp(6)
        root.addView(actions, actionParams)

        setContentView(root)
        requestLocationAndCenter()
    }

    private fun searchDestination() {
        val query = destinationInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a destination first", Toast.LENGTH_SHORT).show()
            return
        }
        statusText.text = "Searching…"
        Thread {
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(this, Locale.getDefault())
                    .getFromLocationName(query, 1)
                runOnUiThread {
                    val address = results?.firstOrNull()
                    if (address == null) {
                        statusText.text = "No result found"
                        Toast.makeText(this, "No location found. Try a more complete address.", Toast.LENGTH_LONG).show()
                    } else {
                        val point = GeoPoint(address.latitude, address.longitude)
                        destinationMarker?.let { map.overlays.remove(it) }
                        destinationMarker = Marker(map).apply {
                            position = point
                            title = address.getAddressLine(0) ?: query
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(destinationMarker)
                        map.controller.animateTo(point)
                        map.controller.setZoom(16.0)
                        map.invalidate()
                        statusText.text = "Destination pin added"
                        Toast.makeText(this, "Destination found and pinned", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    statusText.text = "Search unavailable"
                    Toast.makeText(this, "Search needs an internet connection. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun requestLocationAndCenter() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            statusText.text = "Allow location to show your position"
            return
        }
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider == null) {
                statusText.text = "Turn on Location in phone settings"
                return
            }
            locationManager.requestLocationUpdates(provider, 2000L, 2f, this)
            val current = locationManager.getLastKnownLocation(provider)
            if (current != null) onLocationChanged(current)
            else statusText.text = "Waiting for GPS location…"
        } catch (_: SecurityException) {
            statusText.text = "Location permission is required"
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        speedText.text = if (location.hasSpeed()) "%.0f km/h".format(location.speed * 3.6f) else "0 km/h"
        val point = GeoPoint(location.latitude, location.longitude)
        if (userMarker == null) {
            userMarker = Marker(map).apply {
                title = "You are here"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(userMarker)
            map.controller.animateTo(point)
            map.controller.setZoom(15.0)
        }
        userMarker?.position = point
        map.invalidate()
        statusText.text = "GPS location updated"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) requestLocationAndCenter()
            else statusText.text = "Location permission not granted"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
        if (::locationManager.isInitialized) requestLocationAndCenter()
    }

    override fun onPause() {
        if (::map.isInitialized) map.onPause()
        if (::locationManager.isInitialized) locationManager.removeUpdates(this)
        super.onPause()
    }

    override fun onDestroy() {
        if (::map.isInitialized) map.onDetach()
        super.onDestroy()
    }

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

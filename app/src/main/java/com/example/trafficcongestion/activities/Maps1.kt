package com.example.trafficcongestion.activities

import android.database.MatrixCursor
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.example.trafficcongestion.R
import kotlinx.android.synthetic.main.activity_maps1.*
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt


class Maps1 : AppCompatActivity() {

    private var callout: Callout? = null
    private var addressGeocodeParameters: GeocodeParameters? = null
    // create a picture marker symbol
    private val pinstartSymbol: PictureMarkerSymbol? by lazy { createStartSymbol() }
    private val pinendSymbol: PictureMarkerSymbol? by lazy { createEndSymbol() }
    // create a locator task from an online service
    private val locatorTask: LocatorTask by lazy { LocatorTask(getString(R.string.locator_task_uri)) }
    // create a new Graphics Overlay
    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private var mstart:SearchView?=null
    private var mend:SearchView?=null
    private var resetbtn: Button?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps1)

        resetbtn=findViewById(R.id.resetbtn)

        mstart=findViewById(R.id.start_searchView)
        mend=findViewById(R.id.end_searchView)

        mstart!!.isIconified=false
        mend!!.isIconified=false
        //closes keyboard
        mstart!!.clearFocus()
        mend!!.clearFocus()


        val topographicMap = ArcGISMap(Basemap.createOpenStreetMap())
        mapView1.apply {
            // set the map to be displayed in the mapview
            map = topographicMap
            setViewpoint(Viewpoint(27.669785, 85.320230, 1000.0))
            // define the graphics overlay and add it to the map view
            graphicsOverlays.add(graphicsOverlay)
            // add listener to handle screen taps
            onTouchListener = object : DefaultMapViewOnTouchListener(this@Maps1, mapView1) {
                override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                    identifyGraphic(motionEvent)
                    return true
                }

            }
        }

        setupstart()
        setupend()
        resetbtn!!.setOnClickListener{
            resetsearch()
        }
    }

    private fun resetsearch() {
        graphicsOverlay.graphics.clear()
        val topographicMap = ArcGISMap(Basemap.createOpenStreetMap())
        mapView1.apply {
            // set the map to be displayed in the mapview
            map = topographicMap
            setViewpoint(Viewpoint(27.669785, 85.320230, 1000.0))
            // define the graphics overlay and add it to the map view
            graphicsOverlays.add(graphicsOverlay)
        }
    }

    /**
     * Sets up the address SearchView and uses MatrixCursor to show suggestions to the user as text is entered.
     */
    private fun setupstart() {
        addressGeocodeParameters = GeocodeParameters().apply {
            // get place name and street address attributes
            resultAttributeNames.addAll(listOf("PlaceName", "City", "Country"))
            // return only the closest result
            maxResults = 3

            start_searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(address: String): Boolean {
                    geoCodeTypedAddress(address)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {

                    // if the newText string isn't empty, get suggestions from the locator task
                    if (newText.isNotEmpty()) {
                        val suggestionsFuture = locatorTask.suggestAsync(newText)
                        suggestionsFuture.addDoneListener {
                            try {
                                // get the results of the async operation
                                val suggestResults = suggestionsFuture.get()
                                // set up parameters for searching with MatrixCursor
                                val address = "address"
                                val columnNames = arrayOf(BaseColumns._ID, address)
                                val suggestionsCursor = MatrixCursor(columnNames)

                                // add each address suggestion to a new row
                                for ((key, result) in suggestResults.withIndex()) {
                                    suggestionsCursor.addRow(arrayOf<Any>(key, result.label))
                                }
                                // column names for the adapter to look at when mapping data
                                val cols = arrayOf(address)
                                // ids that show where data should be assigned in the layout
                                val to = intArrayOf(R.id.suggestion_address)
                                // define SimpleCursorAdapter
                                val suggestionAdapter = SimpleCursorAdapter(
                                    this@Maps1,
                                    R.layout.suggestion, suggestionsCursor, cols, to, 0
                                )

                                start_searchView.suggestionsAdapter = suggestionAdapter
                                // handle an address suggestion being chosen
                                start_searchView.setOnSuggestionListener(object :
                                    SearchView.OnSuggestionListener {
                                    override fun onSuggestionSelect(position: Int): Boolean {
                                        return false
                                    }

                                    override fun onSuggestionClick(position: Int): Boolean {
                                        // get the selected row
                                        (suggestionAdapter.getItem(position) as? MatrixCursor)?.let { selectedRow ->
                                            // get the row's index
                                            val selectedCursorIndex = selectedRow.getColumnIndex(
                                                address
                                            )
                                            // get the string from the row at index
                                            val selectedAddress = selectedRow.getString(
                                                selectedCursorIndex
                                            )
                                            // use clicked suggestion as query
                                            start_searchView.setQuery(selectedAddress, true)
                                        }
                                        return true
                                    }
                                })
                            } catch (e: Exception) {
                                Log.e("", "Geocode suggestion error: " + e.message)
                                Toast.makeText(
                                    applicationContext,
                                    "Geocode suggestion error",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    }
                    return true
                }
            })
        }
        mend!!.isIconified=false
    }
    private fun setupend() {

        addressGeocodeParameters = GeocodeParameters().apply {
            // get place name and street address attributes
            resultAttributeNames.addAll(listOf("PlaceName", "City", "Country"))
            // return only the closest result
            maxResults = 3

            end_searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(address: String): Boolean {
                    // geocode typed address
                    geoCodeTypedAddress1(address)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {

                    // if the newText string isn't empty, get suggestions from the locator task
                    if (newText.isNotEmpty()) {
                        val suggestionsFuture = locatorTask.suggestAsync(newText)
                        suggestionsFuture.addDoneListener {
                            try {
                                // get the results of the async operation
                                val suggestResults = suggestionsFuture.get()
                                // set up parameters for searching with MatrixCursor
                                val address = "address"
                                val columnNames = arrayOf(BaseColumns._ID, address)
                                val suggestionsCursor = MatrixCursor(columnNames)

                                // add each address suggestion to a new row
                                for ((key, result) in suggestResults.withIndex()) {
                                    suggestionsCursor.addRow(arrayOf<Any>(key, result.label))
                                }
                                // column names for the adapter to look at when mapping data
                                val cols = arrayOf(address)
                                // ids that show where data should be assigned in the layout
                                val to = intArrayOf(R.id.suggestion_address1)
                                // define SimpleCursorAdapter
                                val suggestionAdapter = SimpleCursorAdapter(
                                    this@Maps1,
                                    R.layout.suggestion, suggestionsCursor, cols, to, 0
                                )

                                end_searchView.suggestionsAdapter = suggestionAdapter
                                // handle an address suggestion being chosen
                                end_searchView.setOnSuggestionListener(object :
                                    SearchView.OnSuggestionListener {
                                    override fun onSuggestionSelect(position: Int): Boolean {
                                        return false
                                    }

                                    override fun onSuggestionClick(position: Int): Boolean {
                                        // get the selected row
                                        (suggestionAdapter.getItem(position) as? MatrixCursor)?.let { selectedRow ->
                                            // get the row's index
                                            val selectedCursorIndex = selectedRow.getColumnIndex(
                                                address
                                            )
                                            // get the string from the row at index
                                            val selectedAddress = selectedRow.getString(
                                                selectedCursorIndex
                                            )
                                            // use clicked suggestion as query
                                            end_searchView.setQuery(selectedAddress, true)
                                        }
                                        return true
                                    }
                                })
                            } catch (e: Exception) {
                                Log.e("", "Geocode suggestion error: " + e.message)
                                Toast.makeText(
                                    applicationContext,
                                    "Geocode suggestion error",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    }
                    return true
                }
            })
        }
        mstart!!.isIconified=false
    }

    /**
     * Geocode an address passed in by the user.
     *
     * @param address the address read in from searchViews
     */
    private fun geoCodeTypedAddress(address: String) {

        locatorTask.addDoneLoadingListener {
            if (locatorTask.loadStatus == LoadStatus.LOADED) {
                // run the locatorTask geocode task, passing in the address
                val geocodeResultFuture =
                    locatorTask.geocodeAsync(address, addressGeocodeParameters)
                geocodeResultFuture.addDoneListener {
                    try {
                        // get the results of the async operation
                        val geocodeResults = geocodeResultFuture.get()
                        if (geocodeResults.isNotEmpty()) {
                            displaySearchResultOnMap(geocodeResults[0])
                        } else {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.location_not_found) + address,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is ExecutionException, is InterruptedException -> {
                                Log.e("", "Geocode error: " + e.message)
                                Toast.makeText(
                                    applicationContext,
                                    getString(R.string.geo_locate_error),
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                            else -> throw e
                        }
                    }
                }
            } else {
                locatorTask.retryLoadAsync()
            }
        }
        locatorTask.loadAsync()
    }
    private fun geoCodeTypedAddress1(address: String) {

        locatorTask.addDoneLoadingListener {
            if (locatorTask.loadStatus == LoadStatus.LOADED) {
                // run the locatorTask geocode task, passing in the address
                val geocodeResultFuture =
                    locatorTask.geocodeAsync(address, addressGeocodeParameters)
                geocodeResultFuture.addDoneListener {
                    try {
                        // get the results of the async operation
                        val geocodeResults = geocodeResultFuture.get()
                        if (geocodeResults.isNotEmpty()) {
                            displaySearchResultOnMap(geocodeResults[0])
                        } else {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.location_not_found) + address,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is ExecutionException, is InterruptedException -> {
                                Log.e("", "Geocode error: " + e.message)
                                Toast.makeText(
                                    applicationContext,
                                    getString(R.string.geo_locate_error),
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                            else -> throw e
                        }
                    }
                }
            } else {
                locatorTask.retryLoadAsync()
            }
        }
        locatorTask.loadAsync()
    }

    /**
     * Turns a GeocodeResult into a Point and adds it to a graphic overlay on the map.
     *
     * @param geocodeResult a single geocode result
     */
    private fun displaySearchResultOnMap(geocodeResult: GeocodeResult) {
        // clear graphics overlay of existing graphics
        mapView1.graphicsOverlays.clear()
        graphicsOverlay.graphics.clear()
        // create a list of points from the geocode results
        val resultPoints: List<Point> = ArrayList()
        for (result in GeocodeResult) {
            // create graphic object for resulting location
            val resultPoint = result.displayLocation
            val resultLocGraphic = Graphic(resultPoint, result.attributes, pinstartSymbol)
            // add graphic to location layer
            graphicsOverlay.graphics.add(resultLocGraphic)
            resultPoints.add(resultPoint)
        }
        // create graphic object for resulting location
        val resultPoint = geocodeResult.displayLocation
        val resultLocationGraphic = Graphic(resultPoint, geocodeResult.attributes, pinstartSymbol)
        // add graphic to location layer
        graphicsOverlay.graphics.add(resultLocationGraphic)
        mapView1.setViewpointAsync(Viewpoint(geocodeResult.extent), 1f)
        showCallout(resultLocationGraphic)
    }
    private fun displaySearchResultOnMap1(geocodeResult: GeocodeResult) {
        // clear graphics overlay of existing graphics
        // create graphic object for resulting location
        val resultPoint = geocodeResult.displayLocation
        val resultLocationGraphic = Graphic(resultPoint, geocodeResult.attributes, pinendSymbol)
        // add graphic to location layer
        graphicsOverlay.graphics.add(resultLocationGraphic)
        mapView1.setViewpointAsync(Viewpoint(geocodeResult.extent), 1f)
        showCallout(resultLocationGraphic)
    }

    /**
     * Identifies and shows a call out on a tapped graphic.
     *
     * @param motionEvent the motion event containing a tapped screen point
     */
    private fun identifyGraphic(motionEvent: MotionEvent) {
        // get the screen point
        val screenPoint: android.graphics.Point = android.graphics.Point(
            motionEvent.x.roundToInt(), motionEvent.y.roundToInt()
        )
        // from the graphics overlay, get the graphics near the tapped location
        val identifyResultsFuture: ListenableFuture<IdentifyGraphicsOverlayResult> =
            mapView1.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10.0, false)
        identifyResultsFuture.addDoneListener{
            try {
                val identifyGraphicsOverlayResult: IdentifyGraphicsOverlayResult = identifyResultsFuture.get()
                val graphics = identifyGraphicsOverlayResult.graphics
                // get the first graphic identified
                if (graphics.isNotEmpty()) {
                    val identifiedGraphic: Graphic = graphics[0]
                    // show the callout of the identified graphic
                    showCallout(identifiedGraphic)
                } else {
                    // dismiss the callout if no graphic is identified (e.g. tapping away from the graphic)
                    callout?.dismiss()
                }
            } catch (e: Exception) {
                Log.e("", "Identify error: " + e.message)
            }
        }
    }

    /**
     * Shows the given graphic's attributes as a call out.
     *
     * @param graphic the graphic containing the attributes to be displayed
     */
    private fun showCallout(graphic: Graphic) {
        // create a text view for the callout
        val calloutContent = TextView(applicationContext).apply {
            setTextColor(Color.BLACK)
            // get the graphic attributes for place name and street address, and display them as text in the callout
            this.text = graphic.attributes["PlaceName"].toString() + "\n" +
                    graphic.attributes["City"].toString()
        }
        // get the center of the graphic to set the callout location
        val centerOfGraphic = graphic.geometry.extent.center
        val calloutLocation = graphic.computeCalloutLocation(centerOfGraphic, mapView1)

        callout = mapView1.callout.apply {
            showOptions = Callout.ShowOptions(true, true, true)
            content = calloutContent
            // set the leader position using the callout location
            setGeoElement(graphic, calloutLocation)
            // show callout beneath graphic
            style.leaderPosition = Callout.Style.LeaderPosition.UPPER_MIDDLE
            // show the callout
            show()
        }
    }

    /**
     *  Creates a picture marker symbol from the pin icon, and sets it to half of its original size.
     */
    private fun createStartSymbol(): PictureMarkerSymbol? {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.ic_source) as BitmapDrawable?
        val pinSymbol: PictureMarkerSymbol
        try {
            pinSymbol = PictureMarkerSymbol.createAsync(pinDrawable).get()
            pinSymbol.width = 25f
            pinSymbol.height = 40f
            return pinSymbol
        } catch (e: Exception) {
            when (e) {
                is ExecutionException, is InterruptedException -> {
                    Log.e("", "Picture Marker Symbol error: " + e.message)
                    Toast.makeText(
                        applicationContext,
                        "Failed to load pin drawable.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                else -> throw e
            }
        }
        return null
    }
    private fun createEndSymbol(): PictureMarkerSymbol? {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.ic_destination) as BitmapDrawable?
        val pinSymbol: PictureMarkerSymbol
        try {
            pinSymbol = PictureMarkerSymbol.createAsync(pinDrawable).get()
            pinSymbol.width = 25f
            pinSymbol.height =40f
            return pinSymbol
        } catch (e: Exception) {
            when (e) {
                is ExecutionException, is InterruptedException -> {
                    Log.e("", "Picture Marker Symbol error: " + e.message)
                    Toast.makeText(
                        applicationContext,
                        "Failed to load pin drawable.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                else -> throw e
            }
        }
        return null
    }


    override fun onPause() {
        super.onPause()
        mapView1.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView1.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView1.dispose()
    }

}
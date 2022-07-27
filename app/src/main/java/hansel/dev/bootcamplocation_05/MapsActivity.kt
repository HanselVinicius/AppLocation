package hansel.dev.bootcamplocation_05

import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import hansel.dev.bootcamplocation_05.databinding.ActivityMapsBinding
import java.util.*

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    private lateinit var Map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallBack: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_REQUEST = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallBack = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)

                if (p0 != null) {
                    lastLocation = p0.lastLocation
                }
                placeMarkerOnMap(LatLng(lastLocation.latitude,lastLocation.longitude))
            }
        }

        createLocationRequest()

    }


    override fun onMapReady(googleMap: GoogleMap) {
        Map = googleMap

        // Add a marker in Sydney and move the camera
        val myPlace = LatLng(-34.0, 151.0)
        Map.addMarker(MarkerOptions().position(myPlace).title("Marker in Sydney"))
        Map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 12f))

        Map.uiSettings.isZoomControlsEnabled
        Map.setOnMarkerClickListener(this)


        setUpMap()
    }

    //IDE bug permission already requested on Manifest
    @SuppressLint("MissingPermission")
    private fun setUpMap(){
        if(ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)

            return
        }

        Map.isMyLocationEnabled = true

        fusedProviderClient.lastLocation.addOnSuccessListener (this){ location ->
            if (location != null){
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                Map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,12f))
            }
        }

    }

    private fun placeMarkerOnMap(location: LatLng){
        val markerOptions = MarkerOptions().position(location)
        //custom icon
        //markerOptions.icon(BitmapDescriptorFactory.
        // fromBitmap(BitmapFactory.
        // decodeResource(resources,path)))

        val titleStr = getAdress(location)
        markerOptions.title(titleStr)
        Map.addMarker(markerOptions)
    }

    private fun getAdress(latLng: LatLng):String{
        val geocoder: Geocoder
        val adresses: List<Address>
        geocoder = Geocoder(this , Locale.getDefault())



        adresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

        val address = adresses[0].getAddressLine(0)
        val city = adresses[0].locality
        val state = adresses[0].adminArea
        val country = adresses[0].countryName
        val postalCode = adresses[0].postalCode
        return address
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null)
    }

    private fun createLocationRequest(){
        locationRequest = LocationRequest()

        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener{ e ->
            if (e is ResolvableApiException){
                try {
                    e.startResolutionForResult(this@MapsActivity,
                    REQUEST_CHECK_REQUEST)
                }catch (sendEX: IntentSender.SendIntentException){ }


            }
        }


    }

    override fun onPause() {
        super.onPause()
        fusedProviderClient.removeLocationUpdates(locationCallBack)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState){
            startLocationUpdates()
        }
    }


    override fun onMarkerClick(p0: Marker) = false
}
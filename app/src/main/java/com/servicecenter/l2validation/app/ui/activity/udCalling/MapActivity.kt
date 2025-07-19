package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.calculateDistance
import com.servicecenter.l2validation.app.ui.viewmodel.MapViewModel
import com.servicecenter.l2validation.databinding.ActivityMapBinding
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MapActivity : BaseActivity<ActivityMapBinding, MapViewModel>(), OnMapReadyCallback,
    OnClickListener {
    override fun getLayout(): Int = R.layout.activity_map
    override fun getViewModels(): Class<MapViewModel> {
        return MapViewModel::class.java
    }
    private var mMap: GoogleMap? = null
    private var feLat: Double = 0.0
    private var feLong: Double = 0.0
    private var consigneeLat: Double = 0.0
    private var consigneeLong: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        binding.productDetails.tvHeadingName.text = getString(R.string.ud_location)
        binding.productDetails.ivBackArrow.setOnClickListener(this)
        binding.btnBack.setOnClickListener(this)


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        feLat = intent.getDoubleExtra(Constants.FE_LAT, 0.0)
        feLong = intent.getDoubleExtra(Constants.FE_LONG, 0.0)
        consigneeLat = intent.getDoubleExtra(Constants.CONSIGNEE_LAT, 0.0)
        consigneeLong = intent.getDoubleExtra(Constants.CONSIGNEE_LONG, 0.0)
        val value = calculateDistance(feLat, feLong, consigneeLat, consigneeLong)
        binding.feNameTv.text =
            "UD Marked at ${String.format("%.2f", value)} Km Away from Consignee Location"
    }


    override fun onMapReady(p0: GoogleMap) {

        mMap = p0
        addMarker(mMap!!)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.iv_back_arrow -> finish()

            R.id.btnBack -> finish()
        }
    }

    private fun addMarker(googleMap: GoogleMap) {
        val currentLatLng = LatLng(
            feLat,
            feLong
        )
        val markerOptions = MarkerOptions()
            .position(currentLatLng)
            .title("Rider Location")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bike_rider))

        val dcLatLng = LatLng(
            consigneeLat,
            consigneeLong
        )
        val markerOptions1 = MarkerOptions()
            .position(dcLatLng)
            .title("Home")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_dc))

        googleMap.apply {
            clear()
            uiSettings.isZoomGesturesEnabled = true
            animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
            uiSettings.isZoomGesturesEnabled = true
            addMarker(markerOptions)
            addMarker(markerOptions1)
            addPolyline(
                PolylineOptions()
                    .add(currentLatLng, dcLatLng)
                    .width(10f)
                    .color(android.graphics.Color.BLACK)
            )
        }
    }

}
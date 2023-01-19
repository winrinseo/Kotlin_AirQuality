package com.example.airquality

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

class LocationProvider (var context : Context){
    private var location : Location? = null
    private var locationManager : LocationManager? = null

    init{
        getLocation()
    }

    private fun getLocation() : Location?{
        try{
            locationManager = context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager
            var gpsLocation : Location? = null
            var networkLocation : Location? = null
            var isGPSEnabled : Boolean = locationManager!!.isProviderEnabled(
                LocationManager.GPS_PROVIDER)
            var isNetworkEnabled : Boolean = locationManager!!.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)
            if(!isGPSEnabled && !isNetworkEnabled){
                return null
            }else{
                val hasFileLocationPermission = ContextCompat.checkSelfPermission(
                    context,android.Manifest.permission.ACCESS_FINE_LOCATION
                )
                val hasCoarseLocationPermission =
                    ContextCompat.checkSelfPermission( context , android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if(hasFileLocationPermission != PackageManager.PERMISSION_GRANTED ||
                        hasCoarseLocationPermission !=PackageManager.PERMISSION_GRANTED)
                    return null

                if(isNetworkEnabled){
                    networkLocation = locationManager?.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                }
                if(isGPSEnabled){
                    gpsLocation = locationManager?.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
                }

                if(gpsLocation != null && networkLocation != null){
                    if(networkLocation.accuracy > gpsLocation.accuracy){
                        location = networkLocation
                        return networkLocation
                    }else {
                        location = gpsLocation
                        return gpsLocation
                    }
                }else{
                    if(gpsLocation != null){
                        location = gpsLocation
                    }else{
                        location = networkLocation
                    }
                }
            }
        }catch(e : Exception){
            e.printStackTrace()
        }
        return location
    }


    fun getLocationLatitude() : Double{
        return location?.latitude ?: 0.0
    }

    fun getLocationLongitude() : Double{
        return location?.longitude ?: 0.0
    }

}
package com.example.airquality

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.airquality.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    //런타임 권한 요청 시 필요한 코드
    private val PERMISSIONS_REQUEST_CODE = 100
    //요청할 권한 목록
    var REQUESTED_PERMISSIONS = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION)
    
    //위치 정보 요청 시 필요한 런처
    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
    }

    private fun checkAllPermissions(){

        if(!isLocationServiceAvailable()){
            showDialogLocationServiceSetting()
        }else{
            isRunTimePermissionsGranted()
        }
    }
    private fun isLocationServiceAvailable() : Boolean{
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }
    private fun isRunTimePermissionsGranted(){
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.ACCESS_FINE_LOCATION)

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity,REQUESTED_PERMISSIONS,PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUESTED_PERMISSIONS.size){

            var checkResult = true

            for(result in grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    checkResult = false
                    break
                }

                if(checkResult){
                    //위칫값을 확인 할 수 있음
                }else{
                    Toast.makeText(this@MainActivity,
                    "퍼미션이 거부되었습니다.",Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    private fun showDialogLocationServiceSetting(){

    }


}
package com.example.airquality

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.airquality.databinding.ActivityMainBinding
import com.example.airquality.retrofit.AirQualityResponse
import com.example.airquality.retrofit.AirQualityService
import com.example.airquality.retrofit.RetrofitConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    //런타임 권한 요청 시 필요한 코드
    private val PERMISSIONS_REQUEST_CODE = 100
    //요청할 권한 목록
    var REQUESTED_PERMISSIONS = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION)
    
    //위치 정보 요청 시 필요한 런처
    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent>
    private lateinit var locationProvider : LocationProvider

    var latitude = 0.0
    var longitude = 0.0

    val startMapActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
        object : ActivityResultCallback<ActivityResult>{
            override fun onActivityResult(result: ActivityResult?) {
                if(result?.resultCode ?: 0 == Activity.RESULT_OK){
                    latitude = result?.data?.getDoubleExtra("latitude",0.0) ?: 0.0
                    longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                    updateUI()
                }
            }
        })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        binding.btnRefresh.setOnClickListener{
            updateUI()
        }
        setFab()
    }

    private fun setFab(){
        binding.fab.setOnClickListener{
            val intent = Intent(this,MapActivity::class.java)
            intent.putExtra("currentLat",latitude)
            intent.putExtra("currentLng",longitude)
            startMapActivityResult.launch(intent)
        }
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

            }
            if(checkResult){
                //위칫값을 확인 할 수 있음
                updateUI()
            }else{
                Toast.makeText(this@MainActivity,
                    "퍼미션이 거부되었습니다.",Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun showDialogLocationServiceSetting(){
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            result->
            if(isLocationServiceAvailable()){
                isRunTimePermissionsGranted()
            }else{
                Toast.makeText(this@MainActivity, "위치서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                finish()
            }

        }

        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정" ,DialogInterface.OnClickListener{
            dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        })
        builder.setNegativeButton("취소" ,DialogInterface.OnClickListener{
                dialog, id ->
            dialog.cancel()
            Toast.makeText(this@MainActivity,"위치 사용 확인 후 사용하세요",Toast.LENGTH_LONG).show()
            finish()
        })
        builder.create().show()
    }


    private fun updateUI(){
        locationProvider = LocationProvider(this@MainActivity)

        if(latitude == 0.0 && longitude == 0.0) {
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }
        if(latitude != 0.0 || longitude != 0.0) {
            //위치정보 획득
            //가지고온 현재 위치값으로 UI업데이트

            val address = getCurrentAddress(latitude,longitude)

            address?.let{
                binding.tvLocationTitle.text = "${it.thoroughfare}"
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }

        }else{
            Toast.makeText(this@MainActivity,"위치정보를 가져올 수 없습니다.",Toast.LENGTH_SHORT).show()
        }
        getAirQualityData(latitude,longitude)
    }

    private fun getAirQualityData(lat : Double , lon : Double){
        var retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)
        retrofitAPI.getAirQualityData(lat.toString(),lon.toString()
        ,"1cae650d-ea6e-4844-9378-8d5fb3e26ec3").enqueue(object : Callback<AirQualityResponse> {
            override fun onResponse(
                call: Call<AirQualityResponse>,
                response: Response<AirQualityResponse>
            ) {
                if(response.isSuccessful){
                    Toast.makeText(this@MainActivity, "업데이트 완료", Toast.LENGTH_SHORT).show()
                    response.body()?.let{
                        updateAirUI(it)
                    }
                }else{
                    Toast.makeText(this@MainActivity,"업데이트 실패",Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@MainActivity,"업데이트 실패",Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAirUI(airQualityData : AirQualityResponse){
        val pollutionData = airQualityData.data.current.pollution

        binding.tvCount.text = pollutionData.aqius.toString()

        var dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        var dateFormater : DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        binding.tvCheckTime.text = dateTime.format(dateFormater).toString()

        when(pollutionData.aqius){
            in 0..50 -> {
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }
            in 51..150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }
            in 151..200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }
            in 201..250 -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }
    private fun getCurrentAddress(latitude : Double,longitude:Double) : Address?{
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses : List<Address>?

        addresses = try{
            geocoder.getFromLocation(latitude,longitude,7)
        }catch (e: IOException){
            Toast.makeText(this@MainActivity, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_SHORT).show()
            return null
        }catch (e : IllegalAccessException){
            Toast.makeText(this@MainActivity, "잘못된 좌표입니다.", Toast.LENGTH_SHORT).show()
            return null
        }

        if(addresses == null ||addresses.size == 0){
            Toast.makeText(this@MainActivity, "주소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return null
        }

        val address = addresses[0]
        return address
    }


}
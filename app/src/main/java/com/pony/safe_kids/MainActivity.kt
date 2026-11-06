package com.pony.safe_kids

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pony.safe_kids.service.CabinMonitorService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created")

        startService(Intent(this, CabinMonitorService::class.java))
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}

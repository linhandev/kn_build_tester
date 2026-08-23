package com.tencent.tmm.knoiapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tencent.tmm.knoi.initialize
import com.tencent.tmm.knoi.sample.ServiceInCommonApi
import com.tencent.tmm.knoi.sample.TestServiceInCommonApi
import com.tencent.tmm.knoi.service.KNOI
import com.tencent.tmm.knoiapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    val mainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        initialize()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mainBinding.tvTestKnoiC.setOnClickListener {
            val res = KNOI.get<TestServiceInCommonApi>("TestServiceCInCommon").methodWithIntReturnInt(1)
            Toast.makeText(this,"getService:" + res,Toast.LENGTH_LONG).show()
        }
        mainBinding.tvTestKnoi.setOnClickListener {
            val res = KNOI.get<TestServiceInCommonApi>().methodWithIntReturnInt(1)
            Toast.makeText(this,"getService:" + res,Toast.LENGTH_LONG).show()
        }
        mainBinding.tvTestKnoiString.setOnClickListener {
            val res = KNOI.get<ServiceInCommonApi>().methodWithIntReturnInt()
            Toast.makeText(this,"getService:" + res,Toast.LENGTH_LONG).show()
        }

    }
}
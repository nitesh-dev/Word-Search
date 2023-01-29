package com.flaxstudio.wordsearch

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate


class SplashScreenActivity : AppCompatActivity() {

    val thisActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // below api 29, disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // hide title bar
        this.supportActionBar?.hide()

        // enable fullscreen
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        setContentView(R.layout.activity_splash_screen)

        startTimer()

    }


    private fun startTimer(){
        object : CountDownTimer(1500, 1000){
            override fun onTick(millisecond: Long) {
                // do nothing
            }

            override fun onFinish() {

                // start home activity
                val intent = Intent(thisActivity, HomeActivity::class.java)
                startActivity(intent)
            }

        }.start()
    }
}
package com.flaxstudio.wordsearch

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var levelTextView: TextView
    private lateinit var playButton: RelativeLayout
    private lateinit var coinsTextView: TextView

    private var isSoundOn = true
    private var isMusicOn = true
    private var levelDifficulty = LevelDifficulty.Easy
    private var coins = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        // below api 29, disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        this.supportActionBar?.hide()

        // enable fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setContentView(R.layout.activity_home)

        setUpAds()

        coinsTextView = findViewById(R.id.home_coins_text_view)

        sharedPreferences = getSharedPreferences("DATA", MODE_PRIVATE)

        levelTextView = findViewById(R.id.home_level_text_view)

        getSavedData()

        playButton = findViewById(R.id.play_image_button)
        playButton.setOnClickListener {
            loadLevel()
        }

        findViewById<RelativeLayout>(R.id.setting_button).setOnClickListener {
            openSettingDialog()
        }

        findViewById<RelativeLayout>(R.id.about_button).setOnClickListener {
            openAboutDialog()
        }

        findViewById<RelativeLayout>(R.id.earn_coins_relative_layout_button).setOnClickListener {
            openWatchAdsDialog()
        }

        findViewById<RelativeLayout>(R.id.get_pro_relative_layout_button).setOnClickListener {
            openGetProDialog()
        }

        setAnimation()

    }


    // ---------------------------------- Ads working area ------------------------------------------

    private val rewardedAdsId = "ca-app-pub-3940256099942544/5224354917"  // test ads
//    val rewardedAdsId = "ca-app-pub-5701673420267529/7271236698"  // original

    private var mRewardedAd: RewardedAd? = null
    private val TAG = "MainActivity"

    private fun setUpAds() {

        MobileAds.initialize(this) {}

        if(!isInternetConnected(applicationContext)){
            openConnectionErrorDialog(false)
        }else{
            loadRewardedAd()
        }
    }

    private var isAdsLoading = false

    private fun loadRewardedAd() {
        if (mRewardedAd == null) {
            isAdsLoading = true
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this, rewardedAdsId, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, adError.message)
                        isAdsLoading = false
                        mRewardedAd = null
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.e(TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                        isAdsLoading = false
                    }
                }
            )
        }
    }

    private fun showRewardedVideo() {

        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.e(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    Log.e(TAG, "Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                }
            }

            mRewardedAd?.show(
                this,
                OnUserEarnedRewardListener() {
                    fun onUserEarnedReward(rewardItem: RewardItem) {
                        val rewardAmount = 40
                        coins += rewardAmount
                        saveGameData()

                        // updating ui
                        coinsTextView.text = coins.toString()


                        Log.e("TAG", "User earned the reward: $rewardAmount")
                    }
                }
            )
        }else{
            loadRewardedAd()
        }
    }


    // ------------------------------------------------------------------

    private fun isInternetConnected(context: Context): Boolean{

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network)?: return false

            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                else -> false
            }
        }else{
            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo?: return false

            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }


    private fun setAnimation() {

        playButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.change_ui_size_anim))

    }

    override fun onResume() {
        super.onResume()

        getSavedData()

    }

    private var currentLevelNum = 0

    private fun saveGameData() {
        val edit = sharedPreferences.edit()
        edit.putBoolean("isSoundOn", isSoundOn)
        edit.putBoolean("isMusicOn", isMusicOn)
        edit.putString("levelDifficulty", levelDifficulty.toString())
        edit.putInt("Coins", coins)

        edit.apply()
    }

    @SuppressLint("SetTextI18n")
    private fun getSavedData() {

        currentLevelNum = sharedPreferences.getInt("Level", 1)
        levelTextView.text = "Level $currentLevelNum"

        isSoundOn = sharedPreferences.getBoolean("isSoundOn", true)
        isMusicOn = sharedPreferences.getBoolean("isMusicOn", true)
        coins = sharedPreferences.getInt("Coins", 100)

        coinsTextView.text = coins.toString()

        when (sharedPreferences.getString("levelDifficulty", "Easy")) {
            "Easy" -> {
                levelDifficulty = LevelDifficulty.Easy
            }
            "Medium" -> {
                levelDifficulty = LevelDifficulty.Medium
            }
            "Hard" -> {
                levelDifficulty = LevelDifficulty.Hard
            }
        }

    }

    private fun loadLevel() {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)

    }


    // ------------------------------------- dialogs ----------------------------------------------

    private lateinit var settingDialog: Dialog
    lateinit var aboutDialog: Dialog
    lateinit var watchAdsDialog: Dialog
    lateinit var connectionErrorDialog: Dialog
    lateinit var getProAppDialog: Dialog


    private fun openConnectionErrorDialog(isWatchingAds: Boolean = true) {

        connectionErrorDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        connectionErrorDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        connectionErrorDialog.setContentView(R.layout.internet_connection_error_dialog)

        connectionErrorDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            connectionErrorDialog.dismiss()

            if(isWatchingAds){
                openWatchAdsDialog()
            }
        }

        val retryButton = connectionErrorDialog.findViewById<Button>(R.id.retry_button)
        retryButton.setOnClickListener {

            if(isInternetConnected(applicationContext)){

                loadRewardedAd()
                connectionErrorDialog.dismiss()

                if(isWatchingAds){
                    openWatchAdsDialog()
                }

            }
        }
        connectionErrorDialog.setCancelable(false)

        connectionErrorDialog.show()

    }


    private fun openSettingDialog() {

        settingDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        settingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        settingDialog.setContentView(R.layout.setting_dialog_layout)

        settingDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            saveGameData()
            settingDialog.dismiss()
        }

        val soundButton = settingDialog.findViewById<ToggleButton>(R.id.sound_toggle_button)
        soundButton.isChecked = isSoundOn
        soundButton.setOnCheckedChangeListener { _, isChecked ->
            isSoundOn = isChecked
        }

        val musicButton = settingDialog.findViewById<ToggleButton>(R.id.music_toggle_button)
        musicButton.isChecked = isMusicOn
        musicButton.setOnCheckedChangeListener { _, isChecked ->
            isMusicOn = isChecked
        }

        val radioGroup = settingDialog.findViewById<RadioGroup>(R.id.level_difficulty_radio_group)
        when (levelDifficulty) {
            LevelDifficulty.Easy -> {
                radioGroup.check(R.id.level_easy_radio)
            }
            LevelDifficulty.Medium -> {
                radioGroup.check(R.id.level_medium_radio)
            }
            LevelDifficulty.Hard -> {
                radioGroup.check(R.id.level_hard_radio)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedRadioButtonId ->

            when (settingDialog.findViewById<RadioButton>(checkedRadioButtonId).text) {
                "Easy" -> {
                    levelDifficulty = LevelDifficulty.Easy
                }
                "Medium" -> {
                    levelDifficulty = LevelDifficulty.Medium
                }
                "Hard" -> {
                    levelDifficulty = LevelDifficulty.Hard
                }
            }
        }

        settingDialog.setCanceledOnTouchOutside(false)
        settingDialog.show()
    }

    private fun openAboutDialog() {

        aboutDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        aboutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        aboutDialog.setContentView(R.layout.about_dialog_layout)

        aboutDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            aboutDialog.dismiss()
        }

        aboutDialog.setCancelable(false)
        aboutDialog.show()

    }

    private fun openWatchAdsDialog() {

        watchAdsDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        watchAdsDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        watchAdsDialog.setContentView(R.layout.watch_ads_dialog_layout)

        watchAdsDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            watchAdsDialog.dismiss()
        }

        watchAdsDialog.findViewById<Button>(R.id.watch_ads_button).setOnClickListener {

            if(isInternetConnected(applicationContext)){
                showRewardedVideo()
            }else{

                watchAdsDialog.dismiss()
                openConnectionErrorDialog()
            }

        }

        watchAdsDialog.setCancelable(false)
        watchAdsDialog.show()

    }

    private fun openGetProDialog() {

        getProAppDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        getProAppDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        getProAppDialog.setContentView(R.layout.get_pro_dialog_layout)

        getProAppDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            getProAppDialog.dismiss()
        }

        getProAppDialog.setCancelable(false)
        getProAppDialog.show()

    }

}
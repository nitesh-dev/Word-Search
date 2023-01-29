package com.flaxstudio.wordsearch

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.flaxstudio.wordsearch.custom_views.WordSearchView
import com.flaxstudio.wordsearch.custom_views.WordTaskView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.runBlocking
import java.util.*

class GameActivity : AppCompatActivity() {

    lateinit var wordSearchView: WordSearchView
    lateinit var selectedSearch: TextView
    lateinit var wordTaskView: WordTaskView
    lateinit var timerTextView: TextView

    lateinit var levelNameTextView: TextView
    lateinit var levelNumTextView: TextView

    var currentLevelNum: Int = 1
    private var levelDifficulty = LevelDifficulty.Easy
    var coins = 100

    var totalAttempts = 0
    var timeTakenToCompleteLevel = 0L

    var totalWordToSearch = 0
    var wordSearchedNum = 0

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // below api 29, disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        this.supportActionBar?.hide()

        // enable fullscreen
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        setContentView(R.layout.activity_game)

        setUpAds()

        sharedPreferences = getSharedPreferences("DATA", MODE_PRIVATE)
        start()

        levelNumTextView = findViewById(R.id.level_num_text_view)
        timerTextView = findViewById(R.id.timer_textview)
        levelNameTextView = findViewById(R.id.level_name)
        selectedSearch = findViewById(R.id.selectedSearchText)
        selectedSearch.visibility = View.INVISIBLE

        wordSearchView = findViewById(R.id.wordSearchView)
        wordSearchView.searchTextView = selectedSearch

        wordTaskView = findViewById(R.id.wordTaskView)

        findViewById<RelativeLayout>(R.id.get_hint_button).setOnClickListener {

            if(wordSearchView.isHintPossible()){
                if(coins >= 20){
                    coins -= 20
                    wordSearchView.showHint()
                    saveGameData()

                }else{
                    pauseTimer()
                    showNotEnoughCoinsDialog(false)
                }
            }
        }

        wordSearchView.setOnSearchValueChangeListener(object : WordSearchView.OnSearchValueChangeListener{
            override fun onSearchValueChangeListener(searchedValue: String) {
                selectedSearch.text = searchedValue
            }

        })

        wordSearchView.setOnValueSearchedListener(object : WordSearchView.OnValueSearchedListener{
            override fun onValueSearchedListener(searchedValue: String): Boolean {

                val isCorrect = wordTaskView.matchSearchedWord(searchedValue)

                if(isCorrect){
                    wordSearchedNum += 1
                }
                totalAttempts += 1
                return isCorrect
            }

        })

        wordSearchView.setOnSetUpWordsListener(object : WordSearchView.OnSetUpWordsListener{
            override fun onSetUpWordsListener(wordsArray: Array<String>) {
                wordTaskView.startWordTaskView(wordsArray)

                totalWordToSearch = wordsArray.size
                wordSearchedNum = 0
                totalAttempts = 0
                setTimer(wordsArray.size)
            }

        })


        wordSearchView.setOnLevelComplete(object : WordSearchView.OnLevelComplete{

            override fun levelCompleted() {

                currentLevelNum += 1
                saveGameData()
                showLevelCompleteDialog()
            }

        })


        // start game
        startGame(false)

    }


    // ---------------------------------- Ads working area ------------------------------------------

    private val rewardedAdsId = "ca-app-pub-3940256099942544/5224354917"  // test ads
//    val rewardedAdsId = "ca-app-pub-5701673420267529/7271236698"  // original

    private var mRewardedAd: RewardedAd? = null
    private val TAG = "GameActivity"

    private fun setUpAds() {

        MobileAds.initialize(this) {}
        loadRewardedAd()
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
                        notEnoughCoinsDialog.findViewById<TextView>(R.id.not_enough_coins_dialog_coins_text_view).text = coins.toString()

                        Log.e("TAG", "User earned the reward: $rewardAmount")
                    }
                }
            )
        }else{
            loadRewardedAd()
        }
    }


    // ------------------------------------------------------------------

    private fun getSavedData(){
        currentLevelNum = sharedPreferences.getInt("Level", 1)
        coins = sharedPreferences.getInt("Coins", 100)

        levelDifficulty = LevelDifficulty.valueOf(sharedPreferences.getString("levelDifficulty", "Easy").toString())
    }

    private fun saveGameData(){
        val edit = sharedPreferences.edit()
        edit.putInt("Level", currentLevelNum)
        edit.putInt("Coins", coins)

        edit.apply()
    }


    private val levels = ArrayList<LevelType>()
    private fun start(){

        // adding levels
        levels.add(LevelType("Animals", R.array.Animals))
        levels.add(LevelType("Birds", R.array.Birds))
        levels.add(LevelType("Plants", R.array.Plants))
        levels.add(LevelType("Sports", R.array.Sports))
        levels.add(LevelType("Trees", R.array.Trees))
        levels.add(LevelType("Fruits", R.array.Fruits))
        levels.add(LevelType("Vegetables", R.array.Vegetables))

        levels.add(LevelType("Foods", R.array.Foods))
        levels.add(LevelType("Jobs", R.array.Jobs))
        levels.add(LevelType("Accessories", R.array.Accessories))
        levels.add(LevelType("Transports", R.array.Transports))
        levels.add(LevelType("Colors", R.array.Colors))

        getSavedData()

    }

    private val maxRows: Int = 15
    private val minRows: Int = 5
    private var levelRows: Int = 0

    private val maxColumns: Int = 10
    private val minColumns: Int  = 5
    private var levelColumns: Int = 0

    private val maxWordsLength = 20
    private val minWordsLength = 5
    private var totalWordsPresent: Int = 5

    private fun calculateLevel(){


        // 1
        totalWordsPresent = minWordsLength + currentLevelNum / minWordsLength

        if(totalWordsPresent > maxWordsLength){
            totalWordsPresent = maxWordsLength
        }

        // 2
        levelColumns = minColumns + currentLevelNum / 30

        if(levelColumns > maxColumns){
            levelColumns = maxColumns
        }

        // 3
        levelRows = minRows + currentLevelNum / 20

        if(levelRows > maxRows){
            levelRows = maxRows
        }

    }


    private val tempWordsArray = ArrayList<String>()
    @SuppressLint("SetTextI18n")
    private fun startGame(isPlayAgain: Boolean){

        wordTaskView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.view_scale_up_animation))
        wordSearchView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.view_scale_up_animation))

        // check for restart game
        if(isPlayAgain){
            wordSearchView.startWordSearch(tempWordsArray.toTypedArray(), levelRows, levelColumns)
            return
        }

        calculateLevel()

//        val randLevel = (0 until levels.size).shuffled().last()
        var randLevel = currentLevelNum

        if(currentLevelNum >= levels.size){

            randLevel = currentLevelNum % levels.size
        }
        val wordsArray = resources.getStringArray(levels[randLevel].arrayId).toMutableList()

        levelNameTextView.text = levels[randLevel].levelName
        levelNumTextView.text = "Lv: $currentLevelNum"

        tempWordsArray.clear()

        // choosing words
        val maxWordLength = levelRows.coerceAtLeast(levelColumns)
        var randIndex = 0
        while (wordsArray.size > 0){

            if(tempWordsArray.size == totalWordsPresent){
                break
            }

            randIndex = (wordsArray.indices).shuffled().last()

            if(wordsArray[randIndex].length > maxWordLength){
                wordsArray.removeAt(randIndex)
            }else{

                // check for duplicate
                if(!tempWordsArray.contains(wordsArray[randIndex])){
                    tempWordsArray.add(wordsArray[randIndex])
                }

                wordsArray.removeAt(randIndex)
            }

//            // break if all if array empty
            if(wordsArray.size == 0){
                break
            }
        }

        // starting
        wordSearchView.startWordSearch(tempWordsArray.toTypedArray(), levelRows, levelColumns)
    }

    private fun setTimer(totalWordSize: Int){

        // second per word
        val secondPerWord = when (levelDifficulty) {
            LevelDifficulty.Easy -> {
                6
            }
            LevelDifficulty.Medium -> {
                5
            }
            else -> {
                4
            }
        }

        val seconds = (totalWordSize*secondPerWord).toLong()
        timerTextView.text = DateUtils.formatElapsedTime(seconds)

        timeTakenToCompleteLevel = 0
        isTimerOn = true
        startTimer(seconds)

    }

    private var isTimerOn = false

    private var isTimeOutDialogOpened = false
    private lateinit var activeTimer: CountDownTimer
    private var activeTimerSecond = 0L
    private fun startTimer(seconds: Long){

        activeTimer = object : CountDownTimer(seconds * 1000, 1000){
            override fun onTick(millisecond: Long) {

                timeTakenToCompleteLevel += 1
                activeTimerSecond = millisecond / 1000
                timerTextView.text = DateUtils.formatElapsedTime(activeTimerSecond)
            }

            override fun onFinish() {
                wordSearchView.isTimeOut = true
                showTimeOutDialog()
            }

        }.start()

    }

    private fun removeTimer(){
        isTimerOn = false
        activeTimer.cancel()
    }


    override fun onStop() {
        super.onStop()
        pauseTimer()
    }

    override fun onRestart() {
        super.onRestart()

        if(isTimerOn){
            resumeTimer()
        }

    }


    private fun pauseTimer(){
        activeTimer.cancel()

    }

    private fun resumeTimer(){
        activeTimerSecond += 1
        startTimer(activeTimerSecond)
    }


    private fun getSomeTime(){

        runBlocking {
            saveGameData()
        }

        startTimer(10)
    }

    private fun skipLevel(){
        currentLevelNum += 1

        runBlocking {
            saveGameData()
        }

        startGame(false)
    }

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


    // ------------------ dialogs --------------------------------
    private lateinit var timeOutDialog: Dialog
    private lateinit var levelCompleteDialog: Dialog
    private lateinit var notEnoughCoinsDialog: Dialog
    private lateinit var connectionErrorDialog: Dialog

    private fun openConnectionErrorDialog(previousDialogData: Boolean, isOpenedFromAdsDialog: Boolean = false) {

        connectionErrorDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        connectionErrorDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        connectionErrorDialog.setContentView(R.layout.internet_connection_error_dialog)

        connectionErrorDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            connectionErrorDialog.dismiss()

            if(isOpenedFromAdsDialog){
                showNotEnoughCoinsDialog(previousDialogData)
            }
        }

        val retryButton = connectionErrorDialog.findViewById<Button>(R.id.retry_button)
        retryButton.setOnClickListener {

            if(isInternetConnected(applicationContext)){

                loadRewardedAd()
                connectionErrorDialog.dismiss()

                if(isOpenedFromAdsDialog){
                    showNotEnoughCoinsDialog(previousDialogData)
                }

            }
        }
        connectionErrorDialog.setCancelable(false)

        connectionErrorDialog.show()

    }



    @SuppressLint("SetTextI18n")
    private fun showTimeOutDialog(){

        isTimerOn = false
        isTimeOutDialogOpened = true
        timeOutDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        timeOutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        timeOutDialog.setContentView(R.layout.time_out_dialog)
        timeOutDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        timeOutDialog.setCancelable(false)

        timeOutDialog.findViewById<TextView>(R.id.accuracy_text_view).text = "Accuracy: " + (wordSearchedNum / totalAttempts.toFloat() * 100).toInt() + "%"
        timeOutDialog.findViewById<TextView>(R.id.word_left_text_view).text = "Word left: " + (totalWordToSearch - wordSearchedNum)

        timeOutDialog.findViewById<RelativeLayout>(R.id.home_button).setOnClickListener {

            finish()
        }

        timeOutDialog.findViewById<RelativeLayout>(R.id.get_time_button).setOnClickListener {

            isTimeOutDialogOpened = false

            if(coins >= 20){
                coins -= 20
                timeOutDialog.dismiss()
                wordSearchView.isTimeOut = false
                getSomeTime()

            }else{
                timeOutDialog.dismiss()
                showNotEnoughCoinsDialog()
            }
        }

        val playAgainButton = timeOutDialog.findViewById<RelativeLayout>(R.id.game_play_again)
        playAgainButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.change_ui_size_anim))

        playAgainButton.setOnClickListener {

            startGame(true)
            timeOutDialog.dismiss()
        }

        timeOutDialog.findViewById<RelativeLayout>(R.id.play_next_button).setOnClickListener {

            isTimeOutDialogOpened = false

            if(coins >= 50){
                coins -= 50
                timeOutDialog.dismiss()
                skipLevel()

            }else{

                timeOutDialog.dismiss()
                showNotEnoughCoinsDialog()

            }
        }

        timeOutDialog.show()

    }


    private fun showNotEnoughCoinsDialog(isOpenedFromTimeoutDialog: Boolean = true){

        notEnoughCoinsDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        notEnoughCoinsDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        notEnoughCoinsDialog.setContentView(R.layout.not_enough_coins_layout)
        notEnoughCoinsDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        notEnoughCoinsDialog.setCancelable(false)

        notEnoughCoinsDialog.findViewById<RelativeLayout>(R.id.close_button).setOnClickListener {

            notEnoughCoinsDialog.dismiss()

            if(isOpenedFromTimeoutDialog){
                showTimeOutDialog()
            }

            if(!isOpenedFromTimeoutDialog){
                resumeTimer()
            }
        }

        val coinTextView = notEnoughCoinsDialog.findViewById<TextView>(R.id.not_enough_coins_dialog_coins_text_view)
        coinTextView.text = coins.toString()


        notEnoughCoinsDialog.findViewById<Button>(R.id.watch_ads_button).setOnClickListener{

            if(isInternetConnected(applicationContext)){
                showRewardedVideo()
            }else{

                notEnoughCoinsDialog.dismiss()
                openConnectionErrorDialog(isOpenedFromTimeoutDialog)
            }
        }


        notEnoughCoinsDialog.show()
    }


    @SuppressLint("SetTextI18n")
    private fun showLevelCompleteDialog(){

        isTimerOn = false
        removeTimer()

        levelCompleteDialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
        levelCompleteDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        levelCompleteDialog.setContentView(R.layout.level_complete_layout)
        levelCompleteDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        levelCompleteDialog.setCancelable(false)

        levelCompleteDialog.findViewById<RelativeLayout>(R.id.get_time_button).setOnClickListener {

            finish()
            levelCompleteDialog.dismiss()
        }

        levelCompleteDialog.findViewById<TextView>(R.id.level_text_view).text = "Level: " + (currentLevelNum - 1)
        levelCompleteDialog.findViewById<TextView>(R.id.accuracy_text_view).text = "Accuracy: " + (wordSearchedNum / totalAttempts.toFloat() * 100).toInt() + "%"
        levelCompleteDialog.findViewById<TextView>(R.id.word_left_text_view).text = "Time: " + DateUtils.formatElapsedTime(timeTakenToCompleteLevel)

        levelCompleteDialog.findViewById<RelativeLayout>(R.id.play_next_button).setOnClickListener{

            startGame(false)
            levelCompleteDialog.dismiss()
        }


        levelCompleteDialog.show()
    }

}

data class LevelType(val levelName: String, val arrayId: Int)
enum class LevelDifficulty{Easy, Medium, Hard}
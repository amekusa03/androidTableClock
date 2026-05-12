package com.example.screensaver

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.AlarmManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MyDreamService : DreamService() {

    private lateinit var container: FrameLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var starField: FrameLayout
    private lateinit var alarmText: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM月dd日 (E)", Locale.getDefault())
    private val alarmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var isNightMode: Boolean? = null

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true

        setContentView(R.layout.dream_layout)
        
        container = findViewById(R.id.dream_container)
        clockText = findViewById(R.id.dream_clock)
        dateText = findViewById(R.id.dream_date)
        starField = findViewById(R.id.star_field)
        alarmText = findViewById(R.id.alarm_info)

        createStars()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.post(updateTimeRunnable)
        startBurnInProtection()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun updateUI() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        
        // 昼夜の判定 (6時〜18時を昼とする)
        val currentlyNight = hour < 6 || hour >= 18
        
        if (isNightMode != currentlyNight) {
            isNightMode = currentlyNight
            applyTheme(currentlyNight)
        }

        // 時刻と日付の更新
        clockText.text = timeFormat.format(now.time)
        dateText.text = dateFormat.format(now.time)

        // アラーム情報の更新
        updateAlarmInfo()
    }

    private fun applyTheme(isNight: Boolean) {
        if (isNight) {
            container.setBackgroundResource(R.drawable.night_sky_bg)
            starField.visibility = View.VISIBLE
            clockText.setTextColor(0xFFE0E0E0.toInt())
            dateText.setTextColor(0x80A0A0A0.toInt())
        } else {
            container.setBackgroundResource(R.drawable.day_sky_bg)
            starField.visibility = View.GONE
            clockText.setTextColor(0xFFFFFFFF.toInt())
            dateText.setTextColor(0xCCF0F0F0.toInt())
        }
    }

    private fun updateAlarmInfo() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock
        
        if (nextAlarm != null) {
            val alarmTime = Date(nextAlarm.triggerTime)
            alarmText.text = "⏰ " + alarmFormat.format(alarmTime)
            alarmText.visibility = View.VISIBLE
        } else {
            alarmText.visibility = View.GONE
        }
    }

    private fun createStars() {
        val random = Random(System.currentTimeMillis())
        starField.removeAllViews() // 再生成防止
        for (i in 0..60) {
            val star = View(this)
            val size = random.nextInt(2, 6)
            val params = FrameLayout.LayoutParams(size, size)
            
            star.layoutParams = params
            star.setBackgroundColor(0xFFFFFFFF.toInt())
            star.alpha = random.nextFloat() * 0.8f + 0.2f
            
            starField.post {
                star.x = random.nextFloat() * starField.width
                star.y = random.nextFloat() * starField.height
                starField.addView(star)
                
                val anim = ObjectAnimator.ofFloat(star, "alpha", star.alpha, 0.1f)
                anim.duration = 1500L + random.nextLong(2000)
                anim.repeatCount = ObjectAnimator.INFINITE
                anim.repeatMode = ObjectAnimator.REVERSE
                anim.start()
            }
        }
    }

    private fun startBurnInProtection() {
        val clockContainer = findViewById<View>(R.id.clock_container)
        val animX = PropertyValuesHolder.ofFloat("translationX", -30f, 30f)
        val animY = PropertyValuesHolder.ofFloat("translationY", -30f, 30f)
        
        ObjectAnimator.ofPropertyValuesHolder(clockContainer, animX, animY).apply {
            duration = 20000 // 20秒かけてゆっくり動く
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }
}

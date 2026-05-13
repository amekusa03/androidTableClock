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

/**
 * ベッドサイドでの使用を想定したスクリーンセーバーサービス。
 * 時間帯によるテーマ切り替え（昼：青空 / 夜：星空）、巨大な時計表示、
 * 次のアラーム時刻の自動表示、および焼き付き防止機能を備えています。
 */
class MyDreamService : DreamService() {

    private lateinit var container: FrameLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var starField: FrameLayout
    private lateinit var alarmText: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    // UI表示用のフォーマット設定（時刻、日付、アラーム）
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM月dd日 (E)", Locale.getDefault())
    private val alarmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 現在のモード（昼/夜）の状態管理用フラグ
    private var isNightMode: Boolean? = null

    // 1秒ごとにUIを更新するループタスク
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }

    /**
     * スクリーンセーバーがウィンドウにアタッチされた際の初期設定。
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // ユーザー操作に反応しない設定（誤操作防止）
        isInteractive = false
        // ステータスバーなどを非表示にして全画面表示
        isFullscreen = true

        // レイアウトを適用
        setContentView(R.layout.dream_layout)
        
        container = findViewById(R.id.dream_container)
        clockText = findViewById(R.id.dream_clock)
        dateText = findViewById(R.id.dream_date)
        starField = findViewById(R.id.star_field)
        alarmText = findViewById(R.id.alarm_info)

        // 背景の星を生成
        createStars()
    }

    /**
     * スクリーンセーバーの表示が開始された時の処理。
     */
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        // 時刻更新ループを開始
        handler.post(updateTimeRunnable)
        // 焼き付き防止アニメーションを開始
        startBurnInProtection()
    }

    /**
     * スクリーンセーバーが終了した時の処理。
     */
    override fun onDreamingStopped() {
        super.onDreamingStopped()
        // 更新ループを停止
        handler.removeCallbacks(updateTimeRunnable)
    }

    /**
     * 時刻、日付、昼夜テーマ、アラーム情報を更新する。
     */
    private fun updateUI() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        
        // 6時〜18時を「昼」と判定
        val currentlyNight = hour < 6 || hour >= 18
        
        if (isNightMode != currentlyNight) {
            isNightMode = currentlyNight
            applyTheme(currentlyNight)
        }

        // テキストの更新
        clockText.text = timeFormat.format(now.time)
        dateText.text = dateFormat.format(now.time)

        // 次のアラーム設定を確認
        updateAlarmInfo()
    }

    /**
     * 昼夜に応じて背景や色を切り替える。
     */
    private fun applyTheme(isNight: Boolean) {
        if (isNight) {
            // 夜モード
            container.setBackgroundResource(R.drawable.night_sky_bg)
            starField.visibility = View.VISIBLE
            clockText.setTextColor(0xFFE0E0E0.toInt())
            dateText.setTextColor(0x80A0A0A0.toInt())
        } else {
            // 昼モード
            container.setBackgroundResource(R.drawable.day_sky_bg)
            starField.visibility = View.GONE
            clockText.setTextColor(0xFFFFFFFF.toInt())
            dateText.setTextColor(0xCCF0F0F0.toInt())
        }
    }

    /**
     * システムに設定されている次のアラーム時刻を表示する。
     */
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

    /**
     * 夜空にまたたく星を生成する。
     */
    private fun createStars() {
        val random = Random(System.currentTimeMillis())
        starField.removeAllViews() 
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
                
                // またたきアニメーション
                val anim = ObjectAnimator.ofFloat(star, "alpha", star.alpha, 0.1f)
                anim.duration = 1500L + random.nextLong(2000)
                anim.repeatCount = ObjectAnimator.INFINITE
                anim.repeatMode = ObjectAnimator.REVERSE
                anim.start()
            }
        }
    }

    /**
     * 画面の焼き付きを防止するため、時計をゆっくり動かす。
     */
    private fun startBurnInProtection() {
        val clockContainer = findViewById<View>(R.id.clock_container)
        val animX = PropertyValuesHolder.ofFloat("translationX", -30f, 30f)
        val animY = PropertyValuesHolder.ofFloat("translationY", -30f, 30f)
        
        ObjectAnimator.ofPropertyValuesHolder(clockContainer, animX, animY).apply {
            duration = 20000 
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }
}

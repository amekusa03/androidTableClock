package com.example.screensaver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            // スクリーンセーバーの設定画面を開く
            val intent = Intent(Settings.ACTION_DREAM_SETTINGS)
            startActivity(intent)
        }
    }
}

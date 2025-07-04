package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Dashboard"

        binding.addManuallyBtn.setOnClickListener {
            startActivity(Intent(this,AddExpenseActivity::class.java))
        }

        binding.viewHistoryBtn.setOnClickListener {
            startActivity(Intent(this,HistoryActivity::class.java))
        }

        binding.settingsBtn.setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.statisticsBtn.setOnClickListener{
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.scanReceiptBtn.setOnClickListener{
            startActivity(Intent(this, ScanReceiptActivity::class.java))
        }
        // Примеры обработки нажатий
//        binding.scanReceiptBtn.setOnClickListener {
//            // TODO: Запуск сканирования
//        }
//
//        binding.viewHistoryBtn.setOnClickListener {
//            // TODO: Показ истории
//        }

        // Добавь обработчики для всех остальных кнопок
    }
}
package com.example.receipt_scanner

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.receipt_scanner.databinding.ActivityScanReceiptBinding
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ScanReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanReceiptBinding

    private lateinit var imageUri: Uri

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScanReceipt.setOnClickListener {
            checkPermissionsAndOpenDialog()
        }
        // Register camera result
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) processImageWithOCR(imageUri)
        }

        // Register gallery result
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { processImageWithOCR(it) }
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] == true

            when {
                cameraGranted -> {
                    showImageSourceDialog()
                }

                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    showPermissionSettingsDialog()
                }

                else -> {
                    AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Camera access is needed to scan receipts. Would you like to try again?")
                        .setPositiveButton("Retry") { _, _ ->
                            checkPermissionsAndOpenDialog()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

    }


    private fun checkPermissionsAndOpenDialog() {
        val cameraPermission = Manifest.permission.CAMERA

        val isCameraGranted = ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED

        if (isCameraGranted) {
            showImageSourceDialog()
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionSettingsDialog()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
        }



    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Add Receipt")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("receipt_", ".jpg", cacheDir)
        imageUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        cameraLauncher.launch(imageUri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun processImageWithOCR(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE  // ⏳ Show loader
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, uri)
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE  // ❌ Hide loader on failure
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.progressBar.visibility = View.GONE  // ✅ Hide loader
                val recognizedText = visionText.text
                Log.d("OCR", "Recognized Text: ${recognizedText}")

                if (recognizedText.isBlank()) {
                    showError("No text recognized. Try again.")
                    return@addOnSuccessListener
                }

                val amount = extractAmount(recognizedText)
                val date = extractDate(recognizedText)
                val category = guessCategoryFromText(recognizedText)
                Log.d("OCR", "Amount: €$amount, Date: $date, Category: $category")
                if (amount != null) {
                    showConfirmationDialog(amount, date, category)
                    showSuccess("Amount found: €$amount")
                    // TODO: proceed to category extraction or save
                } else {

                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE  // ❌ Hide loader on error
                showError("OCR failed: ${it.message}")
            }
    }

    private fun extractAmount(text: String): Double? {
        val euroAmounts = mutableListOf<Double>()

        val lines = text.lines()
        for (line in lines) {
            // Only lines that contain € or EUR
            if (line.contains("€") || line.contains("EUR", ignoreCase = true)) {
                val pattern = Regex("""(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})""")
                val match = pattern.find(line)
                if (match != null) {
                    val raw = match.value
                    try {
                        val normalized = raw.replace(".", "").replace(",", ".")
                        euroAmounts.add(normalized.toDouble())
                    } catch (_: NumberFormatException) {
                    }
                }
            }
        }
        Log.d("OCR", "Recognized amountsssss: ${euroAmounts}")

        // 🧠 If we found nothing with €, fallback: pick highest raw-looking number
        if (euroAmounts.isEmpty()) {
            val fallbackPattern = Regex("""(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})""")
            for (match in fallbackPattern.findAll(text)) {
                try {
                    val normalized = match.value.replace(".", "").replace(",", ".")
                    euroAmounts.add(normalized.toDouble())
                } catch (_: NumberFormatException) {
                }
            }
        }

        // ✅ Return the highest amount found
        return euroAmounts.maxOrNull()
    }

    private fun extractDate(text: String): String? {
        val pattern = Regex("""\d{2}[./-]\d{2}[./-]\d{4}""")
        return pattern.find(text)?.value
    }

    private fun guessCategoryFromText(text: String): String {
        val lowerText = text.lowercase()

        return when {
            // 🥦 Food
            listOf("edeka", "rewe", "aldi", "lidl", "kaufland", "bäcker", "cafe", "restaurant", "imbiss", "essen", "snack").any { it in lowerText } -> "Food"

            // 🚗 Transport
            listOf("bahn", "db", "ticket", "ubahn", "bus", "fahrkarte", "taxi", "moia", "vbb").any { it in lowerText } -> "Transport"

            // 🛍️ Shopping
            listOf("h&m", "zara", "primark", "dm", "rossmann", "media markt", "saturn", "elektronik", "kleidung").any { it in lowerText } -> "Shopping"

            // 🏥 Health
            listOf("apotheke", "arzt", "praxis", "rezept", "medikament", "gesundheit", "zahnarzt").any { it in lowerText } -> "Health"

            // ❓ Other
            else -> "Other"
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                // TODO: Save to Firestore or classify category
            }
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Camera access is permanently denied. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showConfirmationDialog(amount: Double, date: String?, category: String) {
        val message = buildString {
            append("We extracted the following:\n\n")
            append("• Amount: €$amount\n")
            if (date != null) append("• Date: $date\n")
            append("• Category: $category\n\n")
            append("Do you want to save it?")
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Expense")
            .setMessage(message)
            .setPositiveButton("Save") { _, _ ->
                saveToFirebase(amount, category, date)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Edit") { _, _ ->
                openManualEntryScreen(amount, category, date)
            }
            .show()
    }

    private fun saveToFirebase(amount: Double, category: String, dateString: String?) {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Log.d("DEBUG", "dateString: '$dateString'")
            val parsedDate = dateString?.let { parseDateSmart(it) }
            if (parsedDate != null) {
                val expenseDate = Timestamp(parsedDate)

                val expense = hashMapOf(
                    "amount" to amount,
                    "category" to category,
                    "expenseDate" to expenseDate,
                    "userId" to userId,
                    "timestamp" to Timestamp.now()
                )

                db.collection("expenses")
                    .add(expense)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Saved to Firebase!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            else {
                Toast.makeText(this, "Invalid or missing date", Toast.LENGTH_SHORT).show()

            }
            } catch (e: Exception) {
                Toast.makeText(this, "Date parsing failed", Toast.LENGTH_SHORT).show()
            }

        }



    private fun openManualEntryScreen(amount: Double, category: String,dateString: String?) {
        val intent = Intent(this, EditExpenseActivity::class.java).apply {
            putExtra("prefill_amount", amount)
            putExtra("prefill_category", category)
            putExtra("prefill_date", dateString)
        }
        startActivity(intent)
    }

    private fun parseDateSmart(dateString: String): Date? {
        val possibleFormats = listOf(
            "dd.MM.yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd.MM.yy",
            "dd/MM/yy"
        )

        for (format in possibleFormats) {
            try {
                val formatter = SimpleDateFormat(format, Locale.getDefault())
                return formatter.parse(dateString)
            } catch (_: Exception) {
                // Игнорируем и пробуем следующий
            }
        }

        return null // Не удалось распарсить
    }
}


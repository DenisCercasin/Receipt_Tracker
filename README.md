# 🧾 Receipt Tracker

**University**: Hochschule für Wirtschaft und Recht Berlin

**Module:** App Development in Android – Summer Semester 2025  
**Professor:** Holger Zimmermann  

**Submission Date:** 16 August 2025  
**Author:** Denis Cercasin

---

## Goal

The aim of this project is to design and implement a fully functional Android application that enables users to scan receipts, automatically detect expense amounts, categorize spending, and analyze expenses through visual statistics.  
This project fulfills the requirements of the module by integrating multiple functionalities, Firebase database, user authentication, and a clean, user-friendly interface.

---

## Description

**Receipt Tracker** is an Android application that helps users manage their expenses efficiently.  
The app uses **ML Kit’s offline OCR** to extract text from scanned receipts, detects amounts via **regular expressions**, and assigns categories automatically using keyword logic.  
All data is stored remotely using **Firebase Firestore** and can be viewed in detailed monthly statistics with **MPAndroidChart** visualizations.

**Main Features:**
- **Receipt Scanning:** Use the device camera to capture receipts.
- **Offline OCR Recognition:** ML Kit’s on-device text recognition extracts amounts without an internet connection.
- **Automatic Categorization:** Assigns categories (e.g., Groceries, Fuel) based on keywords.
- **Remote Storage:** Firebase Firestore ensures secure and synchronized data storage.
- **Statistics Dashboard:** Pie chart & bar chart views of expenses per month.
- **Manual Entry:** Add expenses without scanning.
- **Login & Registration:** Secure user authentication.

---

## Contributor

- Denis Cercasin – Developer

---

## Target Users & Background Motivation

While there are many similar expense tracking apps, Receipt Tracker was designed with a specific target group in mind:  
citizens of **Moldova, Romania, and Ukraine**.  

Unlike Western European countries, these regions do not have many widely recognized, Europe-wide retail chains.  
This app’s categorization and keyword recognition system is planned to be tailored for local shop names and receipt formats.  

For testing and academic submission purposes, the current version is adapted for Germany.  
In future iterations, it will be localized to meet the needs of the originally intended markets.

---

## Scope

| Feature | Status |
|---------|--------|
| Login / Register | ✅ |
| Camera-based receipt scan | ✅ |
| OCR text extraction (offline) | ✅ |
| Automatic amount & category detection | ✅ |
| Manual expense entry | ✅ |
| Firebase Firestore storage | ✅ |
| Monthly statistics with charts | ✅ |
| Expense history list | ✅ |
| Edit / Delete expenses | ✅ |

---

## How to Run

### Option 1 – Run from Source (Android Studio)
1. **Clone this repository**
   ```bash
   git clone https://github.com/yourusername/receipt-tracker.git
   ```
2. **Open in Android Studio**
- **File → Open** → Select the cloned project folder.

3. **Sync Gradle**
- Let Android Studio download all dependencies automatically.

4. **Build & Run**
- Connect an Android device or start an emulator.  
- Click ▶ Run in Android Studio.

5. **Requirements**

- Android Studio
- SDK 33–35  
- Internet connection for first run (for dependencies)  
- Camera permission enabled

### Option 2 – Install via APK

1. Go to the [`/apk`](apk/) folder in this repository.  
2. Download the latest `.apk` file to your Android device.  
3. Enable installation from unknown sources (if prompted).  
4. Install and launch the app.

---

## Next Ideas

- Cloud synchronization for multi-device usage with user-specific settings.
- Export expense reports to PDF or CSV.
- AI-powered category detection using a trained machine learning model.
- Budget alerts when monthly spending exceeds a predefined limit.
- Multi-language UI support (German, Romanian, Ukrainian, Russian).

---

## Limitations

- Current testing version is adapted for German receipts and keywords; localization for Moldova, Romania, and Ukraine is planned.
- OCR accuracy may vary depending on receipt quality, font, and lighting conditions.
- Category detection relies on keyword matching and may require user corrections.
- Remote storage requires an internet connection; offline-only mode is not yet supported.

---

## License

This project was developed for **educational purposes** at the  
Hochschule für Wirtschaft und Recht Berlin – Department of Computer Science.  

**No commercial use intended.**

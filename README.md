# SMS Commander 📱

An Android application that listens for incoming SMS messages and executes tasks on your device (e.g., turning off and locking the screen when an SMS containing **`red`** is received from any sender).

---

## ⚡ How It Works
1. **SMS Listener (`SmsReceiver`)**: Catches the system `SMS_RECEIVED` broadcast with highest priority (`999`), extracts the text body and sender.
2. **Screen Lock (`LockAccessibilityService`)**: Uses Android's `AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN`.
   * **Why Accessibility?** Unlike Device Administration which forces PIN entry on wake, locking via Accessibility preserves your **Fingerprint Unlock** on Redmi Note 10 Pro!
3. **MIUI Optimization**: Built-in shortcuts to configure Xiaomi's Autostart and Battery Saver policies so MIUI never kills the app in sleep mode.

---

## 📥 How to Download & Install via GitHub

1. Push this project to your GitHub repository (see [GitHub Setup](#github-setup) below).
2. Go to the **Actions** tab on your GitHub repository.
3. Once the **Build Android APK** workflow finishes, click on it.
4. Under **Artifacts**, download **`SMSCommander-debug`** (contains `app-debug.apk`).
5. Transfer or open the link directly on your phone and tap **Install**.

---

## 🔧 One-Time Setup on Redmi Note 10 Pro (MIUI / HyperOS)

After installing the APK on your Redmi Note 10 Pro, open the app and perform these 3 quick steps:

### 1. Grant SMS Permission
* In the app, tap **Grant** next to `1. SMS Permission` and allow SMS permissions.

### 2. Enable Accessibility Service
* Tap **Enable** next to `2. Screen Lock Service`.
* Your phone will open Accessibility Settings:
  * Tap **Downloaded Apps** (or *Installed Services*).
  * Tap **SMS Commander Screen Lock** and switch the toggle **ON**.

### 3. Configure MIUI Battery & Autostart (Essential)
MIUI will kill background apps unless given permission to run:
* **Autostart**: Tap the **Autostart** button in the app (or go to `Settings > Apps > Manage Apps > SMS Commander`) and toggle **Autostart ON**.
* **Battery Saver**: Tap the **Battery Saver** button in the app, select `SMS Commander`, and choose **No restrictions** (instead of *MIUI Battery saver*).

---

## 🧪 Testing

1. **Instant Button Test**:
   * In the app dashboard, tap **"Test Screen Off Now"**. The screen should turn off and lock instantly. Press the power button and unlock with your fingerprint.
2. **Live SMS Test**:
   * From another phone (or online SMS service), send a text message containing `red` to your phone number.
   * Your phone will vibrate briefly and turn off the screen immediately!

---

## 🚀 GitHub Setup (How to Push from Terminal)

In the project folder (`d:\app dev\sms-commander`), run:

```bash
git init
git add .
git commit -m "Initial commit of SMS Commander app"
git branch -M main
git remote add origin https://github.com/<YOUR-USERNAME>/<YOUR-REPO-NAME>.git
git push -u origin main
```

As soon as you push, GitHub Actions will automatically compile your APK!

package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Calendar

sealed class ActionResult {
    data class Success(val message: String, val intent: Intent?) : ActionResult()
    data class MissingPermission(
        val permission: String,
        val permissionName: String,
        val message: String,
        val settingsIntent: Intent? = null
    ) : ActionResult()
    data class AndroidRestriction(val message: String, val intent: Intent?) : ActionResult()
    data class AppNotFound(val message: String) : ActionResult()
    data class Failed(val message: String) : ActionResult()
    object NotACommand : ActionResult()
}

class PhoneActionManager(private val context: Context) {

    /**
     * Inspects user input for device control commands in Bangla and English.
     * Returns an ActionResult representing the outcome.
     */
    fun processCommand(command: String): ActionResult {
        val raw = command.trim()
        val normalized = raw.lowercase()

        return when {
            // 1. YouTube
            isYouTubeCommand(normalized) -> handleOpenYouTube()

            // 2. Phone Call
            isCallCommand(normalized) -> handleMakeCall(raw)

            // 3. SMS
            isSmsCommand(normalized) -> handleSendSms(raw)

            // 4. Wi-Fi
            isWifiCommand(normalized) -> handleWifi()

            // 5. Camera & Take Photo
            isTakePhotoCommand(normalized) -> handleTakePhoto()
            isCameraCommand(normalized) -> handleOpenCamera()

            // 6. Music
            isMusicCommand(normalized) -> handlePlayMusic(raw)

            // 7. Alarm
            isAlarmCommand(normalized) -> handleSetAlarm(raw)

            // 8. Settings
            isSettingsCommand(normalized) -> handleOpenSettings()

            // 9. Bluetooth
            isBluetoothCommand(normalized) -> handleOpenBluetooth()

            else -> ActionResult.NotACommand
        }
    }

    // ================== INTENT DETECTORS ==================

    private fun isYouTubeCommand(text: String): Boolean {
        return text.contains("youtube") || text.contains("ইউটিউব") ||
                text.contains("open yt") || text.contains("yt open")
    }

    private fun isCallCommand(text: String): Boolean {
        return text.startsWith("call ") || text.contains(" কল দাও") ||
                text.contains(" কল করো") || text.contains("কে কল দাও") ||
                text.contains("কে কল করো") || text.contains("phone call") ||
                text.startsWith("phone ")
    }

    private fun isSmsCommand(text: String): Boolean {
        return text.contains("sms") || text.contains("এসএমএস") ||
                text.contains("মেসেজ পাঠাও") || text.contains("message পাঠাও") ||
                text.contains("send message") || text.contains("send sms")
    }

    private fun isWifiCommand(text: String): Boolean {
        return text.contains("wi-fi") || text.contains("wifi") ||
                text.contains("ওয়াইফাই") || text.contains("ওয়াইফাই")
    }

    private fun isCameraCommand(text: String): Boolean {
        return text.contains("open camera") || text.contains("ক্যামেরা খোলো") ||
                text.contains("ক্যামেরা অন") || text.contains("start camera") ||
                text.contains("launch camera")
    }

    private fun isTakePhotoCommand(text: String): Boolean {
        return text.contains("take a photo") || text.contains("take photo") ||
                text.contains("take picture") || text.contains("ছবি তোলো") ||
                text.contains("ছবি তুলো") || text.contains("snap a photo")
    }

    private fun isMusicCommand(text: String): Boolean {
        return text.contains("play music") || text.contains("play song") ||
                text.contains("গান বাজাও") || text.contains("গান চালাও") ||
                text.contains("মিউজিক চালাও") || text.contains("মিউজিক বাজাও") ||
                text.contains("open spotify") || text.contains("গান শোনো")
    }

    private fun isAlarmCommand(text: String): Boolean {
        return text.contains("alarm") || text.contains("অ্যালার্ম") ||
                text.contains("alaram") || text.contains("এলার্ম")
    }

    private fun isSettingsCommand(text: String): Boolean {
        return text.contains("open settings") || text.contains("সেটিংস খোলো") ||
                text.contains("সেটিংস ওপেন") || text.contains("phone settings") ||
                text == "settings" || text == "সেটিংস"
    }

    private fun isBluetoothCommand(text: String): Boolean {
        return text.contains("bluetooth") || text.contains("ব্লুটুথ")
    }

    // ================== ACTION HANDLERS ==================

    private fun handleOpenYouTube(): ActionResult {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActionResult.Success("ইউটিউব ওপেন করা হচ্ছে।", launchIntent)
        } else {
            // Universal intent to open YouTube (opens YouTube app via App Links or browser)
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ActionResult.Success("ইউটিউব ওপেন করা হচ্ছে।", webIntent)
        }
    }

    private fun handleMakeCall(rawInput: String): ActionResult {
        // Check permissions
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val targetQuery = extractContactOrNumber(rawInput)

        if (targetQuery.isBlank()) {
            return ActionResult.Failed("কাকে কল করতে চান? দয়া করে নাম বা নম্বরটি বলুন।")
        }

        val isDirectDigits = targetQuery.matches(Regex("^[+0-9\\- ]{4,20}$"))
        val phoneNumber: String?

        if (isDirectDigits) {
            phoneNumber = targetQuery.replace(" ", "")
        } else {
            // Lookup contact by name
            if (!hasContactsPermission) {
                return ActionResult.MissingPermission(
                    permission = Manifest.permission.READ_CONTACTS,
                    permissionName = "Contacts (যোগাযোগ)",
                    message = "আপনার প্রয়োজনীয় Contacts permission দেওয়া নেই। আগে permission enable করুন।",
                    settingsIntent = createAppSettingsIntent()
                )
            }
            phoneNumber = findContactPhoneNumber(targetQuery)
            if (phoneNumber == null) {
                return ActionResult.Failed(
                    "'$targetQuery' নামের কোনো কনট্যাক্ট পাওয়া যায়নি। দয়া করে নম্বরটি সরাসরি বলুন।"
                )
            }
        }

        // Now initiate call
        if (!hasCallPermission) {
            // Provide dialer as safe fallback and notify user about direct call permission
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return ActionResult.MissingPermission(
                permission = Manifest.permission.CALL_PHONE,
                permissionName = "Phone Call (কল)",
                message = "সরাসরি কল করার permission দেওয়া নেই। ডায়ালারে নম্বর প্রস্তুত করা হয়েছে, permission enable করলে সরাসরি কল হবে।",
                settingsIntent = dialIntent
            )
        }

        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.Success("$targetQuery-কে কল করা হচ্ছে...", callIntent)
    }

    private fun handleSendSms(rawInput: String): ActionResult {
        val targetQuery = extractContactOrNumber(rawInput)
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val isDirectDigits = targetQuery.matches(Regex("^[+0-9\\- ]{4,20}$"))
        val phoneNumber: String? = if (isDirectDigits) {
            targetQuery.replace(" ", "")
        } else if (targetQuery.isNotEmpty()) {
            if (!hasContactsPermission) {
                return ActionResult.MissingPermission(
                    permission = Manifest.permission.READ_CONTACTS,
                    permissionName = "Contacts (যোগাযোগ)",
                    message = "আপনার প্রয়োজনীয় Contacts permission দেওয়া নেই। আগে permission enable করুন।",
                    settingsIntent = createAppSettingsIntent()
                )
            }
            findContactPhoneNumber(targetQuery)
        } else {
            null
        }

        val uri = if (!phoneNumber.isNullOrBlank()) {
            Uri.parse("smsto:$phoneNumber")
        } else {
            Uri.parse("smsto:")
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (canResolve(smsIntent)) {
            val destText = if (!phoneNumber.isNullOrBlank()) "$targetQuery-কে " else ""
            ActionResult.Success("${destText}এসএমএস পাঠানোর জন্য অ্যাপ ওপেন করা হচ্ছে।", smsIntent)
        } else {
            ActionResult.AppNotFound("আপনার ডিভাইসে কোনো মেসেজিং বা SMS অ্যাপ পাওয়া যায়নি।")
        }
    }

    private fun handleWifi(): ActionResult {
        // On modern Android (Android 10+ / Q+), toggling Wi-Fi directly without user prompt
        // is strictly blocked by the Android OS framework (WifiManager.setWifiEnabled is deprecated & restricted).
        // The supported Android API is Settings.Panel.ACTION_WIFI or Settings.ACTION_WIFI_SETTINGS.
        val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }

        return ActionResult.AndroidRestriction(
            message = "Android সুরক্ষানীতির কারণে সরাসরি Wi-Fi টগল করা যায় না। আপনার সুবিধার্থে Wi-Fi সেটিংস প্যানেল ওপেন করে দেওয়া হচ্ছে।",
            intent = panelIntent
        )
    }

    private fun handleOpenCamera(): ActionResult {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera) {
            return ActionResult.MissingPermission(
                permission = Manifest.permission.CAMERA,
                permissionName = "Camera (ক্যামেরা)",
                message = "আপনার প্রয়োজনীয় Camera permission দেওয়া নেই। আগে permission enable করুন।",
                settingsIntent = createAppSettingsIntent()
            )
        }

        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (canResolve(cameraIntent)) {
            ActionResult.Success("ক্যামেরা ওপেন করা হচ্ছে।", cameraIntent)
        } else {
            val fallbackCapture = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (canResolve(fallbackCapture)) {
                ActionResult.Success("ক্যামেরা ওপেন করা হচ্ছে।", fallbackCapture)
            } else {
                ActionResult.AppNotFound("ডিভাইসে কোনো উপযুক্ত ক্যামেরা অ্যাপ পাওয়া যায়নি।")
            }
        }
    }

    private fun handleTakePhoto(): ActionResult {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera) {
            return ActionResult.MissingPermission(
                permission = Manifest.permission.CAMERA,
                permissionName = "Camera (ক্যামেরা)",
                message = "আপনার প্রয়োজনীয় Camera permission দেওয়া নেই। আগে permission enable করুন।",
                settingsIntent = createAppSettingsIntent()
            )
        }

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (canResolve(captureIntent)) {
            ActionResult.Success("ছবি তোলার জন্য ক্যামেরা রেডি করা হচ্ছে।", captureIntent)
        } else {
            handleOpenCamera()
        }
    }

    private fun handlePlayMusic(rawInput: String): ActionResult {
        // Try Spotify first if mentioned
        val pm = context.packageManager
        if (rawInput.lowercase().contains("spotify")) {
            val spotifyIntent = pm.getLaunchIntentForPackage("com.spotify.music")
            if (spotifyIntent != null) {
                spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return ActionResult.Success("Spotify মিউজিক চালু করা হচ্ছে।", spotifyIntent)
            }
        }

        // Try generic music player intent
        val musicIntent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (canResolve(musicIntent)) {
            return ActionResult.Success("মিউজিক প্লেয়ার চালু করা হচ্ছে।", musicIntent)
        }

        // Try YouTube Music
        val ytMusic = pm.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
        if (ytMusic != null) {
            ytMusic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return ActionResult.Success("YouTube Music চালু করা হচ্ছে।", ytMusic)
        }

        return ActionResult.Success("মিউজিক প্লেয়ার চালু করা হচ্ছে।", musicIntent)
    }

    private fun handleSetAlarm(rawInput: String): ActionResult {
        val (hour, minute) = parseAlarmTime(rawInput)

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Mahi AI Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val displayMin = String.format("%02d", minute)
        return ActionResult.Success(
            "${displayHour}:${displayMin} ${amPm}-এর জন্য অ্যালার্ম সেট করা হয়েছে।",
            alarmIntent
        )
    }

    private fun handleOpenSettings(): ActionResult {
        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.Success("ফোন সেটিংস ওপেন করা হচ্ছে।", settingsIntent)
    }

    private fun handleOpenBluetooth(): ActionResult {
        val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.Success("ব্লুটুথ সেটিংস ওপেন করা হচ্ছে।", btIntent)
    }

    // ================== HELPERS ==================

    private fun canResolve(intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    private fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Extracts name or digits from call/sms command.
     */
    private fun extractContactOrNumber(text: String): String {
        var cleaned = text.trim()

        val prefixes = listOf(
            "call ", "কল দাও ", "কল করো ", "ফোন দাও ", "ফোন করো ",
            "send sms to ", "send message to ", "মেসেজ দাও ", "এসএমএস দাও ",
            "sms to ", "message to "
        )
        for (prefix in prefixes) {
            if (cleaned.lowercase().startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
                break
            }
        }

        val suffixes = listOf(
            "কে কল দাও", "কে কল করো", "কে ফোন দাও", "কে ফোন করো",
            "কে এসএমএস পাঠাও", "কে মেসেজ পাঠাও", "কে মেসেজ দাও",
            " কল দাও", " কল করো", " ফোন করো"
        )
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.removeSuffix(suffix).trim()
                break
            }
        }

        return cleaned
    }

    /**
     * Queries device contacts content provider for matching phone number.
     */
    private fun findContactPhoneNumber(nameQuery: String): String? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIdx >= 0) cursor.getString(numberIdx) else null
            } else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Extracts hour and minute from natural language, defaulting to next hour.
     */
    private fun parseAlarmTime(text: String): Pair<Int, Int> {
        val lower = text.lowercase()

        // Check for specific digit pattern e.g. "7:30", "7.30"
        val timeRegex = Regex("(\\d{1,2})[:.](\\d{2})")
        val match = timeRegex.find(lower)
        if (match != null) {
            var h = match.groupValues[1].toIntOrNull() ?: 7
            val m = match.groupValues[2].toIntOrNull() ?: 0
            if (lower.contains("pm") || lower.contains("রাত") || lower.contains("সন্ধ্যা")) {
                if (h < 12) h += 12
            } else if (lower.contains("am") || lower.contains("সকাল")) {
                if (h == 12) h = 0
            }
            return Pair(h, m)
        }

        // Check single digit hour e.g. "7 am", "7 pm", "৭টা", "সকাল ৭"
        val hourRegex = Regex("(\\d{1,2})\\s*(am|pm|টা|ta)?")
        val hourMatch = hourRegex.find(lower)
        if (hourMatch != null) {
            var h = hourMatch.groupValues[1].toIntOrNull() ?: 7
            val modifier = hourMatch.groupValues[2]
            if (modifier == "pm" || lower.contains("রাত") || lower.contains("সন্ধ্যা") || lower.contains("বিকেল")) {
                if (h < 12) h += 12
            } else if (modifier == "am" || lower.contains("সকাল")) {
                if (h == 12) h = 0
            }
            return Pair(h, 0)
        }

        // Bengali number words
        if (lower.contains("সাত") || lower.contains("৭")) return Pair(7, 0)
        if (lower.contains("আট") || lower.contains("৮")) return Pair(8, 0)
        if (lower.contains("ছয়") || lower.contains("৬")) return Pair(6, 0)

        // Default: next hour
        val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        return Pair(cal.get(Calendar.HOUR_OF_DAY), 0)
    }
}

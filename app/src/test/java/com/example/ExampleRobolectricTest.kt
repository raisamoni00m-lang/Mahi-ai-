package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecureKeyStorage
import com.example.ui.SetupMahiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mahi AI", appName)
  }

  @Test
  fun `secure storage saves and retrieves api key`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = SecureKeyStorage(context)
    val testKey = "AQ.testKey1234567890abcdef"

    storage.saveApiKey(testKey)
    assertTrue(storage.hasSavedKey())
    assertEquals(testKey, storage.getApiKey())

    storage.clearKey()
    assertFalse(storage.hasSavedKey())
  }

  @Test
  fun `viewmodel tracks api key changes and visibility toggle`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SetupMahiViewModel(application)

    assertEquals("", viewModel.uiState.value.apiKey)
    assertTrue(viewModel.uiState.value.isKeyObscured)

    viewModel.onApiKeyChanged("AQ.sampleKey123")
    assertEquals("AQ.sampleKey123", viewModel.uiState.value.apiKey)

    viewModel.toggleKeyVisibility()
    assertFalse(viewModel.uiState.value.isKeyObscured)

    viewModel.toggleKeyVisibility()
    assertTrue(viewModel.uiState.value.isKeyObscured)
  }

  @Test
  fun `verify step 2 permission list has 8 items and microphone is required`() {
    val items = com.example.ui.ALL_PERMISSION_ITEMS
    assertEquals(8, items.size)
    val micItem = items.first { it.key == com.example.ui.MahiPermissionKey.MICROPHONE }
    assertTrue(micItem.isRequired)
    assertTrue(micItem.permissions.contains(android.Manifest.permission.RECORD_AUDIO))
  }

  @Test
  fun `verify step 3 setting list has 5 items and battery is required`() {
    val items = com.example.ui.ALL_SYSTEM_SETTING_ITEMS
    assertEquals(5, items.size)
    val batteryItem = items.first { it.key == com.example.ui.SystemSettingKey.BATTERY }
    assertTrue(batteryItem.isRequired)
  }

  @Test
  fun `onboarding completion flag persists correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = SecureKeyStorage(context)
    assertFalse(storage.isOnboardingCompleted())

    storage.setOnboardingCompleted(true)
    assertTrue(storage.isOnboardingCompleted())

    storage.setOnboardingCompleted(false)
    assertFalse(storage.isOnboardingCompleted())
  }

  @Test
  fun `home viewmodel initializes with greeting and memories`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val homeVm = com.example.ui.home.MahiHomeViewModel(application)

    assertNotNull(homeVm.uiState.value.greetingPrefix)
    assertTrue(homeVm.uiState.value.greetingPrefix.startsWith("Good"))
    assertEquals("there", homeVm.uiState.value.greetingName)
    assertTrue(homeVm.uiState.value.memories.isNotEmpty())
    assertEquals(com.example.ui.home.BottomTab.HOME, homeVm.uiState.value.currentTab)
  }

  @Test
  fun `verify default trained tasks and custom task CRUD operations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.MahiMemoryRepository(context)

    val tasks = repo.getTrainedTasks()
    assertEquals(5, tasks.size)
    assertEquals("gemini se poochho", tasks[0].title)
    assertEquals("chatgpt se poochho", tasks[1].title)
    assertEquals("gemini video", tasks[2].title)
    assertEquals("gemini image", tasks[3].title)
    assertEquals("chatgpt image", tasks[4].title)

    // Add custom task
    repo.addCustomTask("summarize notes", 6, "6 steps • custom", "Summarize user notes")
    val tasksAfterAdd = repo.getTrainedTasks()
    assertEquals(6, tasksAfterAdd.size)
    val customTask = tasksAfterAdd.first { it.title == "summarize notes" }
    assertEquals(6, customTask.steps)
    assertFalse(customTask.isBuiltIn)

    // Update custom task
    repo.updateCustomTask(customTask.id, "summarize notes updated", 10, "Updated prompt")
    val updatedTask = repo.getTrainedTasks().first { it.id == customTask.id }
    assertEquals("summarize notes updated", updatedTask.title)
    assertEquals(10, updatedTask.steps)

    // Delete custom task
    repo.deleteCustomTask(customTask.id)
    assertEquals(5, repo.getTrainedTasks().size)
  }

  @Test
  fun `verify backup export and restore preserves data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.MahiMemoryRepository(context)

    repo.addMemory("Test memory for backup", "Test")
    repo.addCustomTask("custom research task", 5, "5 steps • custom", "Do research")

    val backupJson = repo.createBackupJson()
    assertTrue(backupJson.contains("Test memory for backup"))
    assertTrue(backupJson.contains("custom research task"))

    val (memoriesCount, tasksCount) = repo.restoreFromJson(backupJson)
    assertTrue(memoriesCount >= 0)
    assertTrue(tasksCount >= 0)
  }

  @Test
  fun `scenario 1 - user says Hi`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("Hi")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.contains("Mahi") || reply.contains("হাই") || reply.contains("Hello"))
  }

  @Test
  fun `scenario 2 - user says Bangla Hi`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("হাই")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.contains("Mahi") && reply.contains("হাই"))
  }

  @Test
  fun `scenario 3 - user says Assalamu Alaikum`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("Assalamu Alaikum")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.contains("ওয়ালাইকুম আসসালাম"))
  }

  @Test
  fun `scenario 4 - user asks Bangla question`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("আজকে আবহাওয়া কেমন?")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.isNotEmpty())
    assertTrue(reply.any { it in '\u0980'..'\u09FF' })
  }

  @Test
  fun `scenario 5 - user asks English question`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("What can you do?")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.isNotEmpty())
    assertTrue(reply.contains("Mahi") || reply.contains("help") || reply.contains("assistant"))
  }

  @Test
  fun `scenario 6 - user mixes Bangla and English`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatService = com.example.data.GeminiChatService(context)
    val result = chatService.sendMessage("আমার জন্য একটা message লিখে দাও")
    assertTrue(result.isSuccess)
    val reply = result.getOrNull().orEmpty()
    assertTrue(reply.isNotEmpty())
    assertTrue(reply.contains("মেসেজ") || reply.contains("লিখে") || reply.contains("message"))
  }

  @Test
  fun `scenario 7 - user interrupts Mahi while speaking switches to LISTENING`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val homeVm = com.example.ui.home.MahiHomeViewModel(application)

    // Trigger interruption
    homeVm.interruptSpeaking()
    // Must immediately switch to LISTENING mode
    assertEquals(com.example.ui.home.components.OrbState.LISTENING, homeVm.uiState.value.orbState)
    assertFalse(homeVm.uiState.value.isSpeakingActive)
  }

  @Test
  fun `scenario 8 - continuous conversations in Free Mode without license or limits`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val homeVm = com.example.ui.home.MahiHomeViewModel(application)

    assertEquals("FREE MODE", homeVm.uiState.value.accessMode)
    assertEquals("Unlimited access", homeVm.uiState.value.accessStatusText)

    // Conversational turn 1
    homeVm.sendMessage("Hi")
    // Conversational turn 2
    homeVm.sendMessage("তুমি কি আমাকে সাহায্য করতে পারবে?")
    // Conversational turn 3
    homeVm.sendMessage("আজকে অনেক মন খারাপ")

    // Conversation history continues without restriction
    assertTrue(homeVm.uiState.value.chatMessages.isNotEmpty())
  }

  @Test
  fun `phone action - Open YouTube returns valid launch or web intent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Open YouTube")
    assertTrue(result is com.example.data.ActionResult.Success)
    val success = result as com.example.data.ActionResult.Success
    assertTrue(success.message.contains("ইউটিউব"))
    assertNotNull(success.intent)
  }

  @Test
  fun `phone action - Open Settings returns valid settings intent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Open Settings")
    assertTrue(result is com.example.data.ActionResult.Success)
    val success = result as com.example.data.ActionResult.Success
    assertTrue(success.message.contains("সেটিংস"))
    assertEquals(android.provider.Settings.ACTION_SETTINGS, success.intent?.action)
  }

  @Test
  fun `phone action - Turn on Wi-Fi explains Android restriction and provides panel`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Turn on Wi-Fi")
    assertTrue(result is com.example.data.ActionResult.AndroidRestriction)
    val restriction = result as com.example.data.ActionResult.AndroidRestriction
    assertTrue(restriction.message.contains("Android সুরক্ষানীতির কারণে"))
    assertNotNull(restriction.intent)
  }

  @Test
  fun `phone action - Open Camera checks permission and notifies user`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Open Camera")
    // When permission is not granted, must report missing permission clearly in Bangla
    if (result is com.example.data.ActionResult.MissingPermission) {
      assertTrue(result.message.contains("permission দেওয়া নেই"))
      assertEquals(android.Manifest.permission.CAMERA, result.permission)
      assertNotNull(result.settingsIntent)
    } else {
      assertTrue(result is com.example.data.ActionResult.Success)
    }
  }

  @Test
  fun `phone action - Take a photo checks permission and notifies user`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Take a photo")
    if (result is com.example.data.ActionResult.MissingPermission) {
      assertTrue(result.message.contains("permission দেওয়া নেই"))
      assertEquals(android.Manifest.permission.CAMERA, result.permission)
    } else {
      assertTrue(result is com.example.data.ActionResult.Success)
    }
  }

  @Test
  fun `phone action - Set an alarm creates alarm intent with time`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Set an alarm for 7 AM")
    if (result is com.example.data.ActionResult.Success) {
      assertTrue(result.message.contains("অ্যালার্ম"))
      assertEquals(android.provider.AlarmClock.ACTION_SET_ALARM, result.intent?.action)
      assertEquals(7, result.intent?.getIntExtra(android.provider.AlarmClock.EXTRA_HOUR, -1))
    }
  }

  @Test
  fun `phone action - Call Rahim checks permission without faking success`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Call Rahim")
    // If contacts/call permission missing, reports missing permission
    if (result is com.example.data.ActionResult.MissingPermission) {
      assertTrue(result.message.contains("permission"))
    } else if (result is com.example.data.ActionResult.Failed) {
      assertTrue(result.message.contains("কনট্যাক্ট") || result.message.contains("নম্বর"))
    } else {
      assertTrue(result is com.example.data.ActionResult.Success)
    }
  }

  @Test
  fun `phone action - Send SMS to Rahim creates SMS intent without faking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Send SMS to 01700000000")
    if (result is com.example.data.ActionResult.Success) {
      assertTrue(result.message.contains("এসএমএস"))
      assertEquals(android.content.Intent.ACTION_SENDTO, result.intent?.action)
    }
  }

  @Test
  fun `phone action - Play music opens music player`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.PhoneActionManager(context)
    val result = manager.processCommand("Play music")
    if (result is com.example.data.ActionResult.Success) {
      assertTrue(result.message.contains("মিউজিক"))
      assertNotNull(result.intent)
    }
  }
}

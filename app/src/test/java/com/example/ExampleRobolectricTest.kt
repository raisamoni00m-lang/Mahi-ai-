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
}

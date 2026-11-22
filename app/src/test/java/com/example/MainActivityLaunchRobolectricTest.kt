package com.example

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration as WorkConfiguration
import androidx.work.WorkManager
import com.example.data.local.AgeConsentStatus
import com.example.data.local.AppLanguage
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.AppPreferencesState
import com.example.ui.screens.ParentalConsentScreen
import com.example.ui.theme.DevoirsTNTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Tests Robolectric vérifiant :
 * 1. Le lancement de MainActivity sans crash dans les 3 langues (FR, AR RTL, EN).
 * 2. La présence de LocalActivityResultRegistryOwner et LocalOnBackPressedDispatcherOwner
 *    malgré le remplacement de LocalContext par createConfigurationContext, permettant
 *    rememberLauncherForActivityResult (POST_NOTIFICATIONS) et BackHandler sans IllegalStateException.
 * 3. L'écran de consentement parental pour les moins de 15 ans.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(ctx, WorkConfiguration.Builder().build())
        }
    }

    @Test
    fun `MainActivity se lance sans crash en FR AR et EN`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AppPreferencesRepository(appContext)

        for (language in listOf(AppLanguage.FR, AppLanguage.AR, AppLanguage.EN)) {
            prefs.setLanguage(language)
            prefs.setAgeConsentStatus(AgeConsentStatus.UNSET)

            val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            val activity = controller.get()
            assertNotNull("MainActivity doit être créée pour ${language.code}", activity)
            assertFalse(
                "MainActivity ne doit pas se terminer en erreur pour ${language.code}",
                activity.isFinishing
            )

            // Vérifie également après validation du consentement (transition vers AuthScreen)
            prefs.setAgeConsentStatus(AgeConsentStatus.ADULT_OR_15_PLUS)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertFalse(activity.isFinishing)

            controller.pause().stop().destroy()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `rememberLauncherForActivityResult et BackHandler fonctionnent en FR AR RTL et EN`() {
        val activity = composeTestRule.activity
        var activeLanguage by androidx.compose.runtime.mutableStateOf(AppLanguage.FR)
        var resolvedRegistryOwner: Any? = null
        var resolvedBackOwner: Any? = null
        var resolvedDirection: LayoutDirection? = null
        var backHandled = false
        var permissionLaunchSucceeded = false

        composeTestRule.setContent {
            val language = activeLanguage
            val baseContext = LocalContext.current
            val baseConfiguration = LocalConfiguration.current
            val localizedContext = remember(language, baseConfiguration) {
                val locale = language.toLocale()
                val config = Configuration(baseConfiguration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                baseContext.createConfigurationContext(config)
            }
            val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                LocalActivityResultRegistryOwner provides activity,
                LocalOnBackPressedDispatcherOwner provides activity
            ) {
                resolvedRegistryOwner = LocalActivityResultRegistryOwner.current
                resolvedBackOwner = LocalOnBackPressedDispatcherOwner.current
                resolvedDirection = LocalLayoutDirection.current

                BackHandler { backHandled = true }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* résultat géré */ }

                Button(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        permissionLaunchSucceeded = true
                    },
                    modifier = Modifier.testTag("launch_notif_perm_${language.code}")
                ) {
                    Text("Request permission")
                }
            }
        }

        for (language in listOf(AppLanguage.FR, AppLanguage.AR, AppLanguage.EN)) {
            backHandled = false
            permissionLaunchSucceeded = false
            composeTestRule.runOnUiThread {
                activeLanguage = language
            }
            composeTestRule.waitForIdle()

            assertSame(
                "LocalActivityResultRegistryOwner doit pointer vers l'Activity pour ${language.code}",
                activity,
                resolvedRegistryOwner
            )
            assertSame(
                "LocalOnBackPressedDispatcherOwner doit pointer vers l'Activity pour ${language.code}",
                activity,
                resolvedBackOwner
            )
            assertEquals(
                if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr,
                resolvedDirection
            )

            composeTestRule.onNodeWithTag("launch_notif_perm_${language.code}").performClick()
            composeTestRule.waitForIdle()
            assertTrue(
                "Le lancement de la permission POST_NOTIFICATIONS doit réussir pour ${language.code}",
                permissionLaunchSucceeded
            )

            composeTestRule.runOnUiThread {
                activity.onBackPressedDispatcher.onBackPressed()
            }
            composeTestRule.waitForIdle()
            assertTrue("BackHandler doit être invoqué pour ${language.code}", backHandled)
        }
    }

    @Test
    fun `ecran de consentement parental moins de 15 ans exige accord du parent et desactive IA par defaut`() {
        var savedStatus: AgeConsentStatus? = null
        var savedAiOverride: Boolean? = null

        composeTestRule.setContent {
            DevoirsTNTheme(darkTheme = false) {
                ParentalConsentScreen(
                    preferences = AppPreferencesState(language = AppLanguage.FR),
                    onSelectLanguage = {},
                    onCompleteConsent = { status, aiOverride ->
                        savedStatus = status
                        savedAiOverride = aiOverride
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("age_option_under_15").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Sans case cochée par le parent, le bouton reste désactivé
        composeTestRule.onNodeWithTag("parental_consent_confirm_btn")
            .performScrollTo()
            .assertIsNotEnabled()

        // Une fois la case parentale cochée, validation autorisée (Tuteur IA désactivé par défaut)
        composeTestRule.onNodeWithTag("parental_consent_checkbox_row")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parental_consent_confirm_btn")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(AgeConsentStatus.MINOR_WITH_PARENTAL_CONSENT, savedStatus)
        assertEquals(false, savedAiOverride)
    }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AgeConsentStatus
import com.example.data.local.AppLanguage
import com.example.data.local.AppPreferencesState
import com.example.ui.components.adaptiveEmerald

private enum class AgeBracketChoice {
    FIFTEEN_OR_OLDER,
    UNDER_FIFTEEN
}

/**
 * Écran de vérification d'âge et de consentement parental pour les élèves de moins de 15 ans.
 * Permet au parent ou tuteur légal de valider l'utilisation de l'application et de choisir
 * d'activer ou non le Tuteur IA avant l'accès à la connexion Google.
 */
@Composable
fun ParentalConsentScreen(
    preferences: AppPreferencesState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onCompleteConsent: (status: AgeConsentStatus, aiTutorEnabledOverride: Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAgeBracket by rememberSaveable { mutableStateOf<AgeBracketChoice?>(null) }
    var parentApproved by rememberSaveable { mutableStateOf(false) }
    var allowAiTutorForMinor by rememberSaveable { mutableStateOf(false) }

    val emerald = adaptiveEmerald()
    val heroGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f),
            MaterialTheme.colorScheme.background
        )
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("parental_consent_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(heroGradient)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sélecteur de langue FR / العربية / EN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = preferences.language == lang,
                            onClick = { onSelectLanguage(lang) },
                            modifier = Modifier.testTag("consent_lang_${lang.code}"),
                            label = { Text(lang.nativeLabel) }
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FamilyRestroom,
                            contentDescription = stringResource(R.string.cd_parental_consent_icon),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.consent_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.consent_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Choix de la tranche d'âge
                AgeChoiceCard(
                    title = stringResource(R.string.consent_age_15_plus_title),
                    subtitle = stringResource(R.string.consent_age_15_plus_subtitle),
                    icon = Icons.Filled.Person,
                    selected = selectedAgeBracket == AgeBracketChoice.FIFTEEN_OR_OLDER,
                    onClick = { selectedAgeBracket = AgeBracketChoice.FIFTEEN_OR_OLDER },
                    modifier = Modifier.testTag("age_option_15_plus")
                )

                AgeChoiceCard(
                    title = stringResource(R.string.consent_age_under_15_title),
                    subtitle = stringResource(R.string.consent_age_under_15_subtitle),
                    icon = Icons.Filled.FamilyRestroom,
                    selected = selectedAgeBracket == AgeBracketChoice.UNDER_FIFTEEN,
                    onClick = { selectedAgeBracket = AgeBracketChoice.UNDER_FIFTEEN },
                    modifier = Modifier.testTag("age_option_under_15")
                )

                // Formulaire d'autorisation parentale affiché pour les élèves de moins de 15 ans
                if (selectedAgeBracket == AgeBracketChoice.UNDER_FIFTEEN) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parental_consent_panel"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = emerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.consent_parent_section_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = stringResource(R.string.consent_parent_info_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Choix parental sur le Tuteur IA (désactivé par défaut pour les moins de 15 ans)
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = stringResource(R.string.cd_ai_tutor_switch_icon),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.consent_parent_ai_toggle_title),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.consent_parent_ai_toggle_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = allowAiTutorForMinor,
                                        onCheckedChange = { allowAiTutorForMinor = it },
                                        modifier = Modifier.testTag("parental_consent_ai_switch")
                                    )
                                }
                            }

                            // Case à cocher obligatoire du parent / tuteur légal
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Checkbox) {
                                        parentApproved = !parentApproved
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("parental_consent_checkbox_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = parentApproved,
                                    onCheckedChange = { parentApproved = it },
                                    modifier = Modifier.testTag("parental_consent_checkbox")
                                )
                                Text(
                                    text = stringResource(R.string.consent_parent_checkbox_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val canContinue = when (selectedAgeBracket) {
                    AgeBracketChoice.FIFTEEN_OR_OLDER -> true
                    AgeBracketChoice.UNDER_FIFTEEN -> parentApproved
                    null -> false
                }

                Button(
                    onClick = {
                        when (selectedAgeBracket) {
                            AgeBracketChoice.FIFTEEN_OR_OLDER -> {
                                onCompleteConsent(AgeConsentStatus.ADULT_OR_15_PLUS, null)
                            }
                            AgeBracketChoice.UNDER_FIFTEEN -> {
                                if (parentApproved) {
                                    onCompleteConsent(
                                        AgeConsentStatus.MINOR_WITH_PARENTAL_CONSENT,
                                        allowAiTutorForMinor
                                    )
                                }
                            }
                            null -> Unit
                        }
                    },
                    enabled = canContinue,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag(
                            if (selectedAgeBracket == AgeBracketChoice.UNDER_FIFTEEN) {
                                "parental_consent_confirm_btn"
                            } else {
                                "consent_continue_btn"
                            }
                        )
                ) {
                    Text(
                        text = if (selectedAgeBracket == AgeBracketChoice.UNDER_FIFTEEN) {
                            stringResource(R.string.consent_parent_confirm_btn)
                        } else {
                            stringResource(R.string.consent_continue_btn)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AgeChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (selected) 2.dp else 1.dp,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

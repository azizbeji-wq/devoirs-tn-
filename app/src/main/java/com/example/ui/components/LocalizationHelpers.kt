package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.ThemeMode
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.domain.bac.BacMention
import com.example.domain.bac.BacSession
import com.example.ui.viewmodel.BacCalcTab

/**
 * Fonctions utilitaires de localisation dynamique des modèles métier (FR / AR / EN).
 * Permettent de conserver les identifiants canoniques en base Room tout en traduisant l'UI.
 */

@Composable
fun CycleLevel.localizedLabel(): String = stringResource(
    when (this) {
        CycleLevel.ALL -> R.string.level_all_label
        CycleLevel.PRIMAIRE_6 -> R.string.level_p6_label
        CycleLevel.COLLEGE_7 -> R.string.level_c7_label
        CycleLevel.COLLEGE_8 -> R.string.level_c8_label
        CycleLevel.COLLEGE_9 -> R.string.level_c9_label
        CycleLevel.LYCEE_1 -> R.string.level_l1_label
        CycleLevel.LYCEE_2 -> R.string.level_l2_label
        CycleLevel.LYCEE_3 -> R.string.level_l3_label
        CycleLevel.BAC -> R.string.level_bac_label
        CycleLevel.UNIVERSITE -> R.string.level_univ_label
    }
)

@Composable
fun CycleLevel.localizedShortLabel(): String = stringResource(
    when (this) {
        CycleLevel.ALL -> R.string.level_all_short
        CycleLevel.PRIMAIRE_6 -> R.string.level_p6_short
        CycleLevel.COLLEGE_7 -> R.string.level_c7_short
        CycleLevel.COLLEGE_8 -> R.string.level_c8_short
        CycleLevel.COLLEGE_9 -> R.string.level_c9_short
        CycleLevel.LYCEE_1 -> R.string.level_l1_short
        CycleLevel.LYCEE_2 -> R.string.level_l2_short
        CycleLevel.LYCEE_3 -> R.string.level_l3_short
        CycleLevel.BAC -> R.string.level_bac_short
        CycleLevel.UNIVERSITE -> R.string.level_univ_short
    }
)

@Composable
fun CycleLevel.localizedCycle(): String = stringResource(
    when (this) {
        CycleLevel.ALL -> R.string.level_all_cycle
        CycleLevel.PRIMAIRE_6 -> R.string.cycle_primaire
        CycleLevel.COLLEGE_7, CycleLevel.COLLEGE_8, CycleLevel.COLLEGE_9 -> R.string.cycle_college
        CycleLevel.LYCEE_1, CycleLevel.LYCEE_2, CycleLevel.LYCEE_3, CycleLevel.BAC -> R.string.cycle_lycee
        CycleLevel.UNIVERSITE -> R.string.cycle_superieur
    }
)

@Composable
fun TunisianSection.localizedLabel(): String = stringResource(
    when (this) {
        TunisianSection.ALL -> R.string.section_all_label
        TunisianSection.TRONC_COMMUN -> R.string.section_tc_label
        TunisianSection.MATHS -> R.string.section_maths_label
        TunisianSection.SCIENCES_EXP -> R.string.section_sc_exp_label
        TunisianSection.INFORMATIQUE -> R.string.section_info_label
        TunisianSection.ECO_GESTION -> R.string.section_eco_label
        TunisianSection.TECHNIQUE -> R.string.section_tech_label
        TunisianSection.LETTRES -> R.string.section_lettres_label
        TunisianSection.PREPA_LICENCE -> R.string.section_sup_label
    }
)

@Composable
fun TunisianSection.localizedShortLabel(): String = stringResource(
    when (this) {
        TunisianSection.ALL -> R.string.section_all_short
        TunisianSection.TRONC_COMMUN -> R.string.section_tc_short
        TunisianSection.MATHS -> R.string.section_maths_short
        TunisianSection.SCIENCES_EXP -> R.string.section_sc_exp_short
        TunisianSection.INFORMATIQUE -> R.string.section_info_short
        TunisianSection.ECO_GESTION -> R.string.section_eco_short
        TunisianSection.TECHNIQUE -> R.string.section_tech_short
        TunisianSection.LETTRES -> R.string.section_lettres_short
        TunisianSection.PREPA_LICENCE -> R.string.section_sup_short
    }
)

@Composable
fun DevoirType.localizedLabel(): String = stringResource(
    when (this) {
        DevoirType.ALL -> R.string.type_all_label
        DevoirType.CONTROLE -> R.string.type_dc_label
        DevoirType.SYNTHESE -> R.string.type_ds_label
        DevoirType.EXAMEN -> R.string.type_exam_label
    }
)

@Composable
fun DevoirType.localizedBadge(): String = stringResource(
    when (this) {
        DevoirType.ALL -> R.string.type_all_badge
        DevoirType.CONTROLE -> R.string.type_dc_badge
        DevoirType.SYNTHESE -> R.string.type_ds_badge
        DevoirType.EXAMEN -> R.string.type_exam_badge
    }
)

@Composable
fun Trimester.localizedLabel(): String = stringResource(
    when (this) {
        Trimester.ALL -> R.string.tri_all_label
        Trimester.T1 -> R.string.tri_t1_label
        Trimester.T2 -> R.string.tri_t2_label
        Trimester.T3 -> R.string.tri_t3_label
        Trimester.NATIONAL -> R.string.tri_nat_label
    }
)

@Composable
fun Trimester.localizedShortLabel(): String = stringResource(
    when (this) {
        Trimester.ALL -> R.string.tri_all_short
        Trimester.T1 -> R.string.tri_t1_short
        Trimester.T2 -> R.string.tri_t2_short
        Trimester.T3 -> R.string.tri_t3_short
        Trimester.NATIONAL -> R.string.tri_nat_short
    }
)

@Composable
fun BacCalcTab.localizedLabel(): String = stringResource(
    when (this) {
        BacCalcTab.ANNUELLE -> R.string.bac_tab_annual
        BacCalcTab.EXAMEN -> R.string.bac_tab_exam
        BacCalcTab.FG -> R.string.bac_tab_fg
        BacCalcTab.SIMULATEUR -> R.string.bac_tab_sim
    }
)

@Composable
fun BacMention.localizedLabel(): String = stringResource(
    when (this) {
        BacMention.TRES_BIEN -> R.string.bac_mention_tb
        BacMention.BIEN -> R.string.bac_mention_b
        BacMention.ASSEZ_BIEN -> R.string.bac_mention_ab
        BacMention.PASSABLE -> R.string.bac_mention_p
        BacMention.AUCUNE -> R.string.bac_mention_none
        BacMention.INCONNU -> R.string.bac_mention_pending
    }
)

@Composable
fun BacSession.localizedLabel(): String = stringResource(
    when (this) {
        BacSession.PRINCIPALE -> R.string.bac_session_principale
        BacSession.CONTROLE -> R.string.bac_session_controle
        BacSession.REFUSE -> R.string.bac_session_refuse
        BacSession.INCONNU -> R.string.bac_session_inconnu
    }
)

@Composable
fun ThemeMode.localizedLabel(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }
)

/**
 * Traduit le nom canonique d'une matière stocké en base (ex. "Mathématiques") dans la langue active.
 */
@Composable
fun localizedSubjectName(canonicalSubject: String): String = when {
    canonicalSubject.equals("Toutes", ignoreCase = true) -> stringResource(R.string.all_subjects_label)
    canonicalSubject.contains("Math", ignoreCase = true) -> stringResource(R.string.subj_maths)
    canonicalSubject.contains("Physique", ignoreCase = true) -> stringResource(R.string.subj_physique)
    canonicalSubject.contains("SVT", ignoreCase = true) -> stringResource(R.string.subj_svt)
    canonicalSubject.contains("Info", ignoreCase = true) -> stringResource(R.string.subj_info)
    canonicalSubject.contains("Éco", ignoreCase = true) || canonicalSubject.contains("Gestion", ignoreCase = true) -> stringResource(R.string.subj_eco)
    canonicalSubject.contains("Tech", ignoreCase = true) -> stringResource(R.string.subj_tech)
    canonicalSubject.contains("Philo", ignoreCase = true) -> stringResource(R.string.subj_philo)
    canonicalSubject.contains("Français", ignoreCase = true) -> stringResource(R.string.subj_francais)
    canonicalSubject.contains("Arabe", ignoreCase = true) -> stringResource(R.string.subj_arabe)
    canonicalSubject.contains("Anglais", ignoreCase = true) -> stringResource(R.string.subj_anglais)
    else -> canonicalSubject
}

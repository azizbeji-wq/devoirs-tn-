package com.example.data.local

import com.example.data.model.AppFeatureIdea
import com.example.data.model.SectionCoefficientPreset
import com.example.data.model.StudyMethodGuide
import com.example.data.model.SubjectCoefficient

/**
 * Données éditoriales statiques (fiches méthodes et boîte à idées).
 * Les devoirs sont désormais externalisés en JSON dans `app/src/main/assets/devoirs/`
 * et chargés dynamiquement dans Room via [DevoirAssetLoader].
 */
object TunisianCurriculumSeed {

    val coefficientPresets: List<SectionCoefficientPreset> = listOf(
        SectionCoefficientPreset(
            id = "bac_maths",
            title = "Bac Mathématiques",
            subtitle = "Total coefficients : 16,5 (avec option)",
            subjects = listOf(
                SubjectCoefficient("maths", "Mathématiques", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("physique", "Physique-Chimie", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("info", "Informatique", 1.5),
                SubjectCoefficient("svt", "SVT", 1.0),
                SubjectCoefficient("arabe", "Arabe", 1.0),
                SubjectCoefficient("francais", "Français", 1.0),
                SubjectCoefficient("anglais", "Anglais", 1.0),
                SubjectCoefficient("philo", "Philosophie", 1.0),
                SubjectCoefficient("sport", "Éducation Physique", 1.0),
                SubjectCoefficient("option", "Matière Optionnelle", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "bac_scexp",
            title = "Bac Sciences Expérimentales",
            subtitle = "SVT & Physique coeff 4 · Maths coeff 3",
            subjects = listOf(
                SubjectCoefficient("svt", "SVT", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("physique", "Physique-Chimie", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("maths", "Mathématiques", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("info", "Informatique", 1.5),
                SubjectCoefficient("arabe", "Arabe", 1.0),
                SubjectCoefficient("francais", "Français", 1.0),
                SubjectCoefficient("anglais", "Anglais", 1.0),
                SubjectCoefficient("philo", "Philosophie", 1.0),
                SubjectCoefficient("sport", "Éducation Physique", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "bac_info",
            title = "Bac Sciences de l'Informatique",
            subtitle = "Algo & STI coeff 3 · Maths coeff 3",
            subjects = listOf(
                SubjectCoefficient("algo", "Algorithme & Prog.", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("sti", "Systèmes & Tech. Info (STI)", 3.0),
                SubjectCoefficient("maths", "Mathématiques", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("physique", "Physique", 2.0),
                SubjectCoefficient("arabe", "Arabe", 1.0),
                SubjectCoefficient("francais", "Français", 1.0),
                SubjectCoefficient("anglais", "Anglais", 1.0),
                SubjectCoefficient("philo", "Philosophie", 1.0),
                SubjectCoefficient("sport", "Éducation Physique", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "bac_eco",
            title = "Bac Économie & Gestion",
            subtitle = "Économie & Gestion coeff 3 · Maths coeff 2,5",
            subjects = listOf(
                SubjectCoefficient("eco", "Économie", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("gestion", "Gestion", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("maths", "Mathématiques", 2.5, hasTwoControlExams = true),
                SubjectCoefficient("histgeo", "Histoire-Géographie", 2.0),
                SubjectCoefficient("info", "Informatique", 1.5),
                SubjectCoefficient("arabe", "Arabe", 1.0),
                SubjectCoefficient("francais", "Français", 1.0),
                SubjectCoefficient("anglais", "Anglais", 1.0),
                SubjectCoefficient("philo", "Philosophie", 1.0),
                SubjectCoefficient("sport", "Éducation Physique", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "bac_tech",
            title = "Bac Sciences Techniques",
            subtitle = "Technologie coeff 4 · Maths & Physique coeff 3",
            subjects = listOf(
                SubjectCoefficient("tech", "Technologie", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("maths", "Mathématiques", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("physique", "Physique-Chimie", 3.0, hasTwoControlExams = true),
                SubjectCoefficient("info", "Informatique", 1.5),
                SubjectCoefficient("arabe", "Arabe", 1.0),
                SubjectCoefficient("francais", "Français", 1.0),
                SubjectCoefficient("anglais", "Anglais", 1.0),
                SubjectCoefficient("philo", "Philosophie", 1.0),
                SubjectCoefficient("sport", "Éducation Physique", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "bac_lettres",
            title = "Bac Lettres",
            subtitle = "Arabe & Philo coeff 4 · Histoire-Géo coeff 3",
            subjects = listOf(
                SubjectCoefficient("arabe", "Arabe", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("philo", "Philosophie", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("histgeo", "Histoire-Géographie", 3.0),
                SubjectCoefficient("francais", "Français", 2.0),
                SubjectCoefficient("anglais", "Anglais", 2.0),
                SubjectCoefficient("info", "Informatique", 1.5),
                SubjectCoefficient("sport", "Éducation Physique", 1.0)
            )
        ),
        SectionCoefficientPreset(
            id = "college_9eme",
            title = "9ème Année de Base (Concours Pilote)",
            subtitle = "Matières clés du diplôme de fin d'études de base",
            subjects = listOf(
                SubjectCoefficient("maths", "Mathématiques", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("arabe", "Arabe", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("francais", "Français", 4.0, hasTwoControlExams = true),
                SubjectCoefficient("anglais", "Anglais", 2.0),
                SubjectCoefficient("svt", "SVT", 2.0),
                SubjectCoefficient("physique", "Physique", 2.0),
                SubjectCoefficient("info", "Informatique", 1.0)
            )
        )
    )

    val studyMethodGuides: List<StudyMethodGuide> = listOf(
        StudyMethodGuide(
            id = "methode_ds_tunisien",
            title = "Réussir la semaine des Devoirs de Synthèse (DS)",
            category = "Stratégie Trimestrielle",
            durationTag = "14 jours avant",
            summary = "En Tunisie, le DS compte double (coeff 2) dans la moyenne trimestrielle. Voici le rétroplanning efficace sur 2 semaines.",
            steps = listOf(
                "J-14 à J-10 : Refaire les fiches de cours des 3 matières à plus fort coefficient de ta section.",
                "J-9 à J-5 : Traiter 2 Devoirs de Synthèse complets d'autres lycées (Lycées Pilotes + régionaux) en temps réel avec le chronomètre.",
                "J-4 à J-2 : Noter chaque erreur récurrente dans un « Carnet d'erreurs » (signes, unités SI, justifications).",
                "La veille : Zéro nuit blanche ! Relecture calme des formules clés pendant 45 minutes puis 8h de sommeil."
            ),
            tunisianTip = "Astuce Régime TN : Gagner +2 points au DS d'une matière coeff 4 rapporte autant que +8 points dans une matière coeff 1 !"
        ),
        StudyMethodGuide(
            id = "methode_maths_bac",
            title = "Méthode de rédaction en Mathématiques (Bac & Pilote)",
            category = "Mathématiques",
            durationTag = "Barème Officiel",
            summary = "Comment décrocher les points de rigueur sur les suites, complexes et études de fonctions sans perdre de demi-points.",
            steps = listOf(
                "Toujours citer explicitement le théorème utilisé (TVI, théorème des gendarmes, récurrence complète avec initialisation + hérédité + conclusion).",
                "Vérifier l'ensemble de définition et la continuité avant toute dérivation ou bijection.",
                "Encadrer clairement chaque résultat final et vérifier la cohérence numérique sur un cas simple (ex: n=0, x=0)."
            ),
            tunisianTip = "Dans les barèmes du Bac tunisien, les questions sont souvent enchaînées : même si tu bloques à la 2a), utilise le résultat admis pour faire la 2b) !"
        ),
        StudyMethodGuide(
            id = "methode_concours_9eme",
            title = "Préparer le Concours de la 9ème (Lycées Pilotes)",
            category = "Collège · 9ème",
            durationTag = "Plan Annuel",
            summary = "Viser une moyenne ≥ 15/20 au concours national pour intégrer un Lycée Pilote (Bourguiba, Ariana, Sfax, Sousse, Monastir…).",
            steps = listOf(
                "Miser sur le trio gagnant : Maths, Arabe et Français (les plus forts coefficients du concours).",
                "Travailler les annales des 8 dernières sessions nationales en conditions d'examen.",
                "Soigner la production écrite en Arabe et Français : 35 % de la note repose sur la structure et la richesse du vocabulaire."
            ),
            tunisianTip = "Commence par les exercices de langue et géométrie rapide pour sécuriser 12 points dans les 50 premières minutes."
        ),
        StudyMethodGuide(
            id = "methode_physique_rc_rl",
            title = "Maîtriser les circuits RC, RL et RLC en Physique",
            category = "Physique-Chimie",
            durationTag = "Fiche Express",
            summary = "Les incontournables qui tombent dans plus de 90 % des devoirs de synthèse et du Bac scientifique en Tunisie.",
            steps = listOf(
                "Toujours flécher le sens du courant i et les tensions en convention récepteur sur le schéma avant d'écrire la loi des mailles.",
                "Vérifier l'homogénéité de la constante de temps : τ = RC (s) et τ = L/R (s).",
                "Savoir lire graphiquement τ par la tangente à l'origine ou la valeur 0,63·E (charge) / 0,37·E (décharge)."
            ),
            tunisianTip = "Ne jamais oublier les unités (mA → A, μF → F, mH → H) lors de l'application numérique !"
        ),
        StudyMethodGuide(
            id = "methode_pomodoro_ramadan",
            title = "Organisation des révisions : Cycles 45/10",
            category = "Productivité",
            durationTag = "Quotidien",
            summary = "Garder une concentration maximale après les cours du lycée ou de la faculté sans s'épuiser.",
            steps = listOf(
                "Découper chaque séance en blocs de 45 minutes sans téléphone suivis de 10 minutes de vraie pause.",
                "Alterner une matière scientifique (calcul/exercices) et une matière littéraire (lecture/langues).",
                "Utiliser la règle des 3 révisions (J+1, J+7, J+21) pour ancrer les théorèmes et dates en mémoire longue."
            ),
            tunisianTip = "Le matin très tôt (Fajr / 6h) est idéal pour la mémorisation (SVT, Histoire-Géo, Philo), et l'après-midi pour les devoirs pratiques."
        ),
        StudyMethodGuide(
            id = "methode_philo_bac",
            title = "Réussir l'épreuve de Philosophie & Langues au Bac",
            category = "Lettres & Tronc Commun",
            durationTag = "Méthodologie",
            summary = "Transformer la Philosophie et les Langues en bonus précieux pour décrocher une mention Bien ou Très Bien.",
            steps = listOf(
                "Analyser chaque mot-clé du sujet pendant 15 minutes au brouillon avant de rédiger la problématique.",
                "Mobiliser 2 à 3 références philosophiques précises et bien expliquées (Platon, Descartes, Kant, Rousseau, Ibn Khaldoun, Sartre).",
                "Relire sa copie 10 minutes avant la fin uniquement pour corriger accords, conjugaisons et ponctuation."
            ),
            tunisianTip = "Citer Ibn Khaldoun (Al-Muqaddima) sur l'État, la société ou le travail valorise particulièrement une copie en Tunisie."
        )
    )

    val appFeatureIdeas: List<AppFeatureIdea> = listOf(
        AppFeatureIdea(
            id = "idea_bank_lycees",
            title = "Banque Collaborative de Devoirs par Gouvernorat & Lycée",
            category = "Contenu & Communauté",
            targetAudience = "Collège · Lycée · Bac",
            impactLabel = "Impact Très Fort",
            description = "Permettre aux enseignants et élèves de partager des devoirs réels classés par gouvernorat (Tunis, Sfax, Sousse, Nabeul, Gabès…), lycée pilote/régional, trimestre et année scolaire.",
            concreteExample = "Ex : Filtrer « Bac Maths · DS2 · Lycée Pilote Sfax · 2025 » et comparer les sujets de plusieurs régions."
        ),
        AppFeatureIdea(
            id = "idea_offline_packs",
            title = "Packs de Révision 100 % Hors-Ligne (PDF + Corrigés)",
            category = "Accessibilité",
            targetAudience = "Tous les niveaux",
            impactLabel = "Essentiel en Tunisie",
            description = "Télécharger tout un trimestre (ex: « Pack Trimestre 1 — 9ème Année ») en un clic sur Wi-Fi pour réviser partout sans consommer de forfait 4G.",
            concreteExample = "Ex : Un pack léger (< 5 Mo) contenant 20 devoirs + barèmes officiels consultables dans le bus ou à la maison."
        ),
        AppFeatureIdea(
            id = "idea_calendrier_dc_ds",
            title = "Calendrier National & Alertes DC / DS / Bac",
            category = "Organisation",
            targetAudience = "Élèves & Parents",
            impactLabel = "Très Utile",
            description = "Un calendrier synchronisé sur les dates officielles du Ministère de l'Éducation (semaine bloquée des DS, vacances, Bac Sport, Bac Pratique, Session Principale et Contrôle) avec rappels automatiques.",
            concreteExample = "Ex : Compte à rebours « J-18 avant la semaine bloquée du Trimestre 2 » + planning automatique de révision."
        ),
        AppFeatureIdea(
            id = "idea_qcm_flash",
            title = "Quiz Éclair 5 Minutes & Défis entre Camarades",
            category = "Gamification",
            targetAudience = "Collège & Lycée",
            impactLabel = "Engagement Fort",
            description = "Transformer les questions rapides (QCM SVT, formules Physique, grammaire Français/Anglais, dérivées Maths) en mini-défis quotidiens avec séries (streaks) et classement de classe.",
            concreteExample = "Ex : « Défi 10 questions sur les Circuits RC » en 5 minutes dans le transport scolaire."
        ),
        AppFeatureIdea(
            id = "idea_formules_poche",
            title = "Aide-Mémoire & Fiches Formules de Poche (Arabe / Français)",
            category = "Révision Rapide",
            targetAudience = "Primaire → Prépa",
            impactLabel = "Indispensable",
            description = "Un dictionnaire bilingue (FR/AR) des formules, théorèmes, constantes physiques et schémas SVT légendés pour réviser en 2 minutes avant d'entrer en salle.",
            concreteExample = "Ex : Fiche « Primitives usuelles & Intégrales » ou « Vocabulaire scientifique bilingue 7ème/8ème »."
        ),
        AppFeatureIdea(
            id = "idea_orientation_score",
            title = "Simulateur de Score d'Orientation Universitaire (FG)",
            category = "Post-Bac & Université",
            targetAudience = "Bacheliers",
            impactLabel = "Différenciant",
            description = "Calculer automatiquement la Formule Globale (FG) du Bac tunisien selon la section et estimer l'accessibilité aux filières (Médecine, INSAT, Prépa IPEIT, ENIT, IHEC, TBS, FST…).",
            concreteExample = "Ex : Entrer ses notes du Bac Maths et voir immédiatement son score FG comparé aux derniers scores d'orientation."
        ),
        AppFeatureIdea(
            id = "idea_espace_univ",
            title = "Module Supérieur : TD, Examens & PFE (Licence / Prépa / Ingénieur)",
            category = "Étudiants Université",
            targetAudience = "Facultés & Écoles",
            impactLabel = "Forte Demande",
            description = "Étendre la bibliothèque aux étudiants universitaires (FST, ISI, ISET, INSAT, FSEG, Prépa MP/PC/PT) avec séries de TD, examens de rattrapage et idées de projets PFE.",
            concreteExample = "Ex : « Licence Info L1 · Semestre 1 · Examen Algèbre + Corrigé » ou « Catalogue d'idées PFE Android/Web »."
        ),
        AppFeatureIdea(
            id = "idea_entraide_profs",
            title = "Espace Questions-Réponses Modéré par des Enseignants",
            category = "Entraide",
            targetAudience = "Élèves bloqués sur un exercice",
            impactLabel = "Haute Valeur",
            description = "Permettre à un élève de poster une question précise sur un exercice d'un devoir et recevoir des indices étape par étape validés par des professeurs tunisiens.",
            concreteExample = "Ex : Bouton « Demander un indice sur l'Exercice 2 » directement sous un devoir de synthèse."
        )
    )
}

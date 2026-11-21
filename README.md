# Devoirs TN — Bibliothèque de devoirs corrigés pour élèves tunisiens

**Devoirs TN** est une application Android moderne (Kotlin, Jetpack Compose, Material 3, Room, MVVM) conçue pour accompagner les élèves tunisiens du **Collège (7ème, 8ème, 9ème année)** et du **Lycée (1ère, 2ème, 3ème année et Baccalauréat)** tout au long de l'année scolaire.

Elle fonctionne **100 % hors ligne** pour la bibliothèque, le calculateur du Bac, les QCM Flash et le suivi de progression, et intègre en option un **Tuteur IA** alimenté par **Gemini via Firebase AI Logic** pour expliquer les corrigés étape par étape.

---

## Fonctionnalités principales

- **Bibliothèque de devoirs corrigés (DC, DS & Bac)** :
  - Sujets structurés en JSON (`app/src/main/assets/devoirs/`) couvrant les matières clés du programme officiel tunisien (Mathématiques, Physique-Chimie, SVT, Informatique, Français, Anglais, Arabe, Philosophie, Économie & Gestion).
  - **Recherche plein texte** dans les titres, les chapitres et les **énoncés d'exercices**, insensible aux accents et à la casse, avec **mise en évidence visuelle** des termes trouvés et aperçu contextuel.
  - Filtres combinables par niveau, section tunisienne, type d'épreuve (DC/DS/Examen), trimestre, matière et favoris.
- **Mode Examen en conditions réelles** :
  - Chronomètre calé sur la durée officielle du sujet, verrouillage des corrigés pendant l'épreuve, auto-évaluation exercice par exercice et sauvegarde automatique dans l'historique.
- **Tuteur IA pédagogique (Français & Arabe RTL)** :
  - Bouton *« Explique-moi cette correction »* adapté au niveau du devoir.
  - Aide progressive (*Indice 1 → Indice 2 → Solution complète*) sans divulguer la réponse trop tôt.
  - Chat contextuel par exercice sauvegardé dans Room, gestion robuste du mode hors ligne et des erreurs.
- **Calculateur du Baccalauréat Tunisien** :
  - Saisie des notes de contrôle (DC) et de synthèse (DS) par trimestre `(DC + 2×DS) / 3`, moyenne annuelle `(T1 + 2×T2 + 2×T3) / 5`, moyenne d'examen, moyenne générale configurable (`25 % / 75 %`), **Formule Globale (FG)** d'orientation avec coefficients modifiables, **simulateur inversé** (*« Quelle note à l'examen pour atteindre X ? »*), mention et session.
  - Paramètres centralisés dans `app/src/main/assets/bac_config.json`.
- **Mon Espace Révision** :
  - Courbe d'évolution des notes sur 20 par matière (dessinée nativement sur Canvas Compose), détection des chapitres à renforcer, **QCM Flash** (5 questions par chapitre), **rappels locaux DC/DS** (notifications J-7, J-3, J-1 via WorkManager).
- **Multilingue, RTL, Thème & Accessibilité** :
  - Interface traduite en **Français (FR)**, **Arabe (العربية avec mise en page RTL complète)** et **Anglais (EN)** avec sélecteur instantané dans l'application.
  - Mode **Clair**, **Sombre** et **Suivre le système** à contraste élevé (WCAG AA).
  - Cibles tactiles ≥ 48 dp, descriptions TalkBack (`contentDescription`) et respect du redimensionnement système des polices (`sp`).
  - **Politique de confidentialité** transparente intégrée dans l'onglet *Mon Espace → Paramètres*.

---

## Captures d'écran

Déposez vos captures dans le dossier `docs/screenshots/` en suivant cette convention de nommage :

| Écran | Emplacement attendu |
| :--- | :--- |
| Accueil & Matières (Clair / Sombre) | `docs/screenshots/01_matieres.png` |
| Bibliothèque & Recherche surlignée | `docs/screenshots/02_bibliotheque_recherche.png` |
| Détail devoir, Mode Examen & Tuteur IA | `docs/screenshots/03_detail_tuteur_ia.png` |
| Calculateur du Bac Tunisien & Simulateur | `docs/screenshots/04_calculateur_bac.png` |
| Mon Espace (Courbe, QCM Flash, Paramètres FR/AR/EN) | `docs/screenshots/05_espace_parametres.png` |
| Interface arabe RTL & Mode Sombre | `docs/screenshots/06_arabe_rtl_sombre.png` |

---

## Installation et lancement

### Prérequis
- **Android Studio** (Meerkat / Ladybug ou supérieur) avec JDK 11+
- **Android SDK** (`compileSdk = 36`, `minSdk = 24`)
- Aucune clé API manuelle n'est requise en local pour les fonctionnalités hors ligne ; le Tuteur IA utilise **Firebase AI Logic** avec attestation **App Check** (voir `.env.example`).

### Compilation & exécution
1. Cloner le dépôt :
   ```bash
   git clone https://github.com/<votre-utilisateur>/devoirs-tn.git
   cd devoirs-tn
   ```
2. (Optionnel) Copier `.env.example` vers `.env` si vous ajoutez des intégrations tierces ultérieures.
3. Ouvrir le projet dans Android Studio ou compiler via Gradle :
   ```bash
   gradle :app:assembleDebug
   ```
4. Lancer la suite complète de tests unitaires et Robolectric (JVM, sans émulateur) :
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## Architecture du projet

L'application suit une architecture **MVVM + Clean Layers** modulaire :

```text
app/src/main/
├── assets/
│   ├── bac_config.json          # Coefficients officiels, pondérations et seuils du Bac
│   └── devoirs/                 # Sujets JSON versionnés par matière / niveau
├── java/com/example/
│   ├── data/
│   │   ├── local/               # Room (DAO, Entités, Migrations), DevoirAssetLoader, AppPreferencesRepository
│   │   ├── model/               # Modèles métier (DevoirItem, CycleLevel, TunisianSection, etc.)
│   │   ├── notifications/       # WorkManager (ExamReminderWorker, ExamReminderScheduler)
│   │   └── repository/          # DevoirRepository, ProgressRepository, AiTutorRepository (Firebase AI Logic)
│   ├── domain/
│   │   ├── bac/                 # BacCalculator (logique pure sans dépendance Android)
│   │   ├── progress/            # ProgressAnalytics (séries, courbes, chapitres faibles)
│   │   └── search/              # DevoirSearchEngine (recherche plein texte & plages de surlignage)
│   ├── ui/
│   │   ├── components/          # Composants M3 réutilisables, Localisation, Paramètres & Confidentialité
│   │   ├── screens/             # Écrans Compose (Matières, Bibliothèque, Détail, Bac, Idées, Espace)
│   │   ├── theme/               # Palette tunisienne clair/sombre, Typographie, Formes
│   │   └── viewmodel/           # DevoirsViewModel, AiTutorViewModel, ProgressViewModel
│   └── MainActivity.kt          # Point d'entrée, App Check, locale dynamique FR/AR/EN & RTL, Edge-to-Edge
└── res/
    ├── values/strings.xml       # Français (défaut)
    ├── values-ar/strings.xml    # Arabe (RTL)
    └── values-en/strings.xml    # Anglais
```

---

## Ajouter du contenu pédagogique

Tous les devoirs sont stockés au format JSON documenté et chargés automatiquement dans Room grâce au champ `contentVersion` (sans jamais écraser les favoris ni la progression de l'élève).

Consultez le guide complet **[`README_CONTENU.md`](README_CONTENU.md)** pour :
- le schéma JSON détaillé d'un devoir,
- les règles de nommage et de versionnage (`contentVersion`),
- les bonnes pratiques de rédaction conformes aux programmes tunisiens,
- la validation automatique par les tests unitaires (`DevoirJsonParserTest`).

---

## Contribuer

Les contributions (nouveaux devoirs vérifiés, corrections de barèmes, améliorations d'accessibilité ou de traductions FR/AR/EN) sont les bienvenues !

1. Créez un *fork* du dépôt puis une branche dédiée (`git checkout -b feat/nouveau-sujet-bac`).
2. Respectez les conventions du projet :
   - Aucun texte d'interface en dur dans les Composables (ajoutez chaque chaîne dans `values`, `values-ar` et `values-en`).
   - Commentez le code en français et conservez des cibles tactiles ≥ 48 dp.
   - Ajoutez ou mettez à jour les tests unitaires correspondants.
3. Vérifiez que la compilation et tous les tests passent :
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Ouvrez une *Pull Request* en décrivant clairement vos modifications.

---

## Licence

Ce projet est distribué sous licence **MIT**. Consultez le fichier [`LICENSE`](LICENSE) pour plus de détails.

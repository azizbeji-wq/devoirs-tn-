# Devoirs TN — Bibliothèque de devoirs corrigés pour élèves tunisiens

**Devoirs TN** est une application Android moderne (Kotlin, Jetpack Compose, Material 3, Room, MVVM) conçue pour accompagner les élèves tunisiens du **Collège (7ème, 8ème, 9ème année)** et du **Lycée (1ère, 2ème, 3ème année et Baccalauréat)** tout au long de l'année scolaire.

Elle unifie un **catalogue PDF distant** (`catalogue.json` + téléchargement hors ligne avec vérification **SHA-256**) et des **sujets historiques consultables en texte** (`assets/devoirs/*.json`) dans une base Room unique sans doublons, sans jamais perdre les favoris ni la progression de l'élève.

---

## Fonctionnalités principales

- **Catalogue unifié PDF & Texte (DC, DS & Bac)** :
  - **Source principale PDF** (`catalogue.json`) hébergée en ligne avec cache disque local, repli hors ligne automatique et téléchargement en arrière-plan (WorkManager + reprise `.part` + vérification d'intégrité **SHA-256** et en-tête `%PDF-`).
  - **Migration transparente des anciens sujets JSON** (`app/src/main/assets/devoirs/*.json`) en sujets **« Consultables en texte »** lorsqu'ils ne disposent pas encore de PDF, dédupliqués par `id` (le catalogue PDF est prioritaire).
  - **Lecteur PDF natif** (`PdfRenderer`, sans dépendance tierce) avec zoom pincement/double-tap, chronomètre flottant et verrouillage du corrigé pendant l'épreuve.
  - **Recherche plein texte** (Room FTS4 + OCR ML Kit sur PDF téléchargés + énoncés texte), insensible aux accents et à la casse, avec surlignage visuel.
  - **Badge « Corrigé inclus » fiable** : affiché uniquement lorsqu'une URL de corrigé PDF (`corrigeUrl`) ou un corrigé texte existe réellement.
- **Mode Examen en conditions réelles** :
  - Chronomètre calé sur la durée officielle du sujet, verrouillage des corrigés pendant l'épreuve, auto-évaluation exercice par exercice et historique sauvegardé dans Room.
- **Tuteur IA pédagogique (Français & Arabe RTL)** :
  - Alimenté par **Gemini via Firebase AI Logic** (sans clé API dans le code), protégé par **Firebase App Check**.
  - Bouton *« Explique cette page »* dans le lecteur PDF (envoi d'une image redimensionnée de la page) et aide progressive (*Indice 1 → Indice 2 → Solution complète*).
  - **Quota quotidien par appareil** (20 requêtes/jour par défaut) affiché en direct à l'élève, avec réinitialisation automatique le lendemain.
  - **Contrôle parental** : interrupteur dédié dans *Mon Espace → Paramètres* permettant à un parent de désactiver complètement le Tuteur IA.
- **Calculateur du Baccalauréat Tunisien** :
  - Moyennes trimestrielles `(DC + 2×DS) / 3`, moyenne annuelle `(T1 + 2×T2 + 2×T3) / 5`, moyenne d'examen, moyenne générale configurable (`25 % / 75 %`), **Formule Globale (FG)** et simulateur inversé.
  - Barèmes centralisés dans `app/src/main/assets/bac_config.json`.
- **Mon Espace Révision & Stockage** :
  - Courbe d'évolution des notes sur 20 par matière (Compose Canvas), objectif hebdomadaire modifiable, **QCM Flash**, **rappels DC/DS** (WorkManager), gestionnaire de **Stockage hors ligne** (packs par trimestre, Wi-Fi uniquement par défaut, alerte > 20 Mo en données mobiles).
- **Multilingue, RTL, Thème & Confidentialité** :
  - Interface traduite en **Français (FR)**, **Arabe (العربية RTL)** et **Anglais (EN)**, mode **Clair / Sombre / Système**, cibles tactiles ≥ 48 dp et politique de confidentialité transparente intégrée dans l'application.

---

## Configuration Firebase

Le Tuteur IA utilise **Firebase AI Logic** (Gemini) sécurisé par **Firebase App Check** : **aucune clé API Gemini n'est stockée dans le code source ni dans l'APK**.

### 1. Créer le projet Firebase et ajouter `google-services.json`
1. Créez un projet dans la [Console Firebase](https://console.firebase.google.com/).
2. Enregistrez une application Android avec le même `applicationId` que celui défini dans `app/build.gradle.kts`.
3. Téléchargez le fichier `google-services.json` et placez-le dans **`app/google-services.json`**.
4. Le plugin Google Services (`com.google.gms.google-services`) injecte automatiquement la configuration au moment de la compilation Gradle.

### 2. Activer Firebase AI Logic (Gemini)
1. Dans la Console Firebase, ouvrez **AI → Firebase AI Logic** (Gemini Developer API ou Vertex AI backend).
2. Activez le backend **Google AI (`GenerativeBackend.googleAI()`)** pour votre projet.
3. L'application instancie le modèle `gemini-2.5-flash` via `Firebase.ai(backend = GenerativeBackend.googleAI())` dans `TutorAiRepository.kt` (aucune clé `GEMINI_API_KEY` requise).

### 3. Configurer Firebase App Check
1. Dans la Console Firebase, ouvrez **Build → App Check**.
2. Enregistrez votre application Android avec le fournisseur **Play Integrity** (pour la production / Play Store).
3. Pour les tests en développement (`BuildConfig.DEBUG`), `configureAppCheck()` active automatiquement `DebugAppCheckProviderFactory` :
   - Récupérez le jeton de débogage affiché dans Logcat au démarrage (ou fournissez-le via l'intent de test) et enregistrez-le dans **App Check → Manage debug tokens**.
4. Activez l'application (*Enforce*) d'App Check sur **Firebase AI Logic** afin d'empêcher tout abus de quota en dehors de votre APK signé.

> **Confidentialité & contrôle parental :** Les images de pages PDF redimensionnées et les questions posées au Tuteur IA sont transmises à Gemini via Firebase AI Logic sans aucune identité utilisateur associée. Un parent peut désactiver totalement le Tuteur IA depuis *Mon Espace → Paramètres*.

---

## Héberger les PDF

Les fichiers PDF ne sont **pas embarqués dans l'APK** afin de garder l'application légère. Ils sont décrits par un manifeste `catalogue.json` téléchargé au démarrage (avec cache local et repli hors ligne automatique).

### 1. Choisir un hébergement HTTPS statique
Tout hébergement statique HTTPS prenant en charge les requêtes `Range` (reprise de téléchargement `.part`) convient :
- **Cloudflare R2** (avec domaine public HTTPS / CDN) — sans frais de bande passante sortante,
- **Firebase Hosting** ou **Cloud Storage for Firebase**,
- **GitHub Releases** ou **GitHub Pages** (pour un dépôt public de sujets libres),
- Serveur **Nginx / S3 + CloudFront** en HTTPS.

### 2. Calculer les empreintes SHA-256 des PDF
L'application vérifie automatiquement l'empreinte SHA-256 de chaque PDF après téléchargement si `sujetSha256` ou `corrigeSha256` est renseigné (champs optionnels et rétrocompatibles). En cas de non-correspondance, le fichier corrompu est immédiatement supprimé et une erreur explicite est affichée.

Sous Linux / macOS :
```bash
sha256sum maths_bac_ds1_sujet.pdf maths_bac_ds1_corrige.pdf
```
Sous Windows (PowerShell) :
```powershell
Get-FileHash .\maths_bac_ds1_sujet.pdf -Algorithm SHA256
```

### 3. Publier `catalogue.json`
Déposez vos PDF et votre fichier `catalogue.json` sur votre serveur HTTPS en incrémentant `catalogueVersion` et le champ `version` des devoirs modifiés :

```json
{
  "schemaVersion": 3,
  "catalogueVersion": 4,
  "devoirs": [
    {
      "id": "college_9eme_maths_t1_ds1",
      "titre": "DS N°1 — Théorème de Thalès et calcul algébrique",
      "matiere": "Mathématiques",
      "niveau": "COLLEGE_9",
      "section": "TRONC_COMMUN",
      "type": "SYNTHESE",
      "trimestre": "T1",
      "duree": 120,
      "difficulte": "Standard",
      "chapitres": ["Thalès", "Calcul littéral"],
      "source": "Sujet d'entraînement",
      "version": 4,
      "tailleMo": 3.4,
      "sujetUrl": "https://cdn.votre-domaine.tn/pdfs/9eme_t1_maths_ds1_sujet.pdf",
      "corrigeUrl": "https://cdn.votre-domaine.tn/pdfs/9eme_t1_maths_ds1_corrige.pdf",
      "sujetSizeBytes": 2105400,
      "corrigeSizeBytes": 1458200,
      "sujetSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "corrigeSha256": "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"
    }
  ]
}
```
- Si un sujet ne possède pas encore de corrigé, mettez `"corrigeUrl": null` : la carte n'affichera pas « Corrigé inclus ».
- Si un ancien devoir texte (`assets/devoirs/*.json`) possède le même `id` qu'une entrée PDF dans `catalogue.json`, la version PDF remplace automatiquement la version texte tout en conservant `isFavorite` et `isCompleted`.

### 4. Configurer `CATALOGUE_URL` dans `.env`
Créez un fichier `.env` à la racine du projet (non versionné, basé sur `.env.example`) et définissez l'URL HTTPS de votre manifeste :

```dotenv
CATALOGUE_URL=https://cdn.votre-domaine.tn/catalogue.json
```

`app/build.gradle.kts` injecte automatiquement cette valeur dans `BuildConfig.CATALOGUE_URL`. Si l'URL est absente, invalide ou injoignable, l'application **ne plante jamais** : elle affiche un message explicite dans *Mon Espace → Stockage* et continue de fonctionner avec le dernier cache local ou le catalogue embarqué.

---

## Installation et lancement

### Prérequis
- **Android Studio** (Meerkat / Ladybug ou supérieur) avec JDK 11+
- **Android SDK** (`compileSdk = 36`, `minSdk = 24`)

### Compilation & exécution
1. Cloner le dépôt :
   ```bash
   git clone https://github.com/<votre-utilisateur>/devoirs-tn.git
   cd devoirs-tn
   ```
2. Configurer Firebase (`app/google-services.json`) et, si besoin, `.env` avec `CATALOGUE_URL=...`.
3. Compiler l'application :
   ```bash
   gradle :app:assembleDebug
   ```
4. Lancer les tests unitaires JVM :
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
│   ├── catalogue.json           # Catalogue PDF principal (URLs HTTPS, tailles, SHA-256 optionnels)
│   └── devoirs/                 # Anciens sujets JSON migrés en « consultables en texte » sans doublons
├── java/com/example/
│   ├── data/
│   │   ├── ai/                  # TutorAiRepository (Firebase AI Logic), AppCheckConfig, PdfTextExtractor (OCR)
│   │   ├── download/            # PdfStorageManager, PdfIntegrityVerifier (SHA-256), PdfDownloadWorker (WorkManager)
│   │   ├── local/               # Room v9 (Migrations, DAO, FTS4), DevoirCatalogueUnifier, RemoteCatalogueService, AppPreferencesRepository
│   │   ├── model/               # Modèles métier (DevoirItem PDF/Texte, CycleLevel, TunisianSection)
│   │   ├── notifications/       # WorkManager (ExamReminderWorker, ExamReminderScheduler)
│   │   └── repository/          # DevoirRepository, ProgressRepository, SubjectRepository
│   ├── domain/
│   │   ├── bac/                 # BacCalculator (logique pure sans dépendance Android)
│   │   ├── progress/            # ProgressAnalytics (séries, courbes, chapitres faibles)
│   │   └── search/              # DevoirSearchEngine (recherche plein texte PDF + texte + surlignage)
│   ├── ui/
│   │   ├── components/          # Composants M3, AiTutorSheet (quota quotidien), SettingsPrivacySection (contrôle parental)
│   │   ├── pdf/                 # PdfDocumentController (PdfRenderer + cache LRU)
│   │   ├── screens/             # Matières, Bibliothèque, Détail (PDF & Texte), Lecteur PDF, Bac, Méthodes, Espace & Stockage
│   │   ├── theme/               # Palette tunisienne clair/sombre, Typographie, Formes
│   │   └── viewmodel/           # DevoirsViewModel, TutorViewModel, StorageViewModel, ExamModeViewModel
│   └── MainActivity.kt          # Point d'entrée, App Check, locale dynamique FR/AR/EN & RTL, Edge-to-Edge
└── res/
    ├── values/strings.xml       # Français (défaut)
    ├── values-ar/strings.xml    # Arabe (RTL)
    └── values-en/strings.xml    # Anglais
```

Consultez également [`README_CONTENU.md`](README_CONTENU.md) pour le détail des schémas de contenu pédagogique.

---

## Licence

Ce projet est distribué sous licence **MIT**. Consultez le fichier [`LICENSE`](LICENSE) pour plus de détails.

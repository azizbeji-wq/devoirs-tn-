# Guide de Contenu — Devoirs TN (Catalogue en ligne & PDF hors ligne)

Dans **Devoirs TN**, les fichiers PDF des sujets et des corrigés sont **hébergés en ligne** (jamais embarqués dans l'APK) et téléchargés à la demande dans le stockage privé de l'application (`filesDir/pdfs/<id>/`).

---

## 1. Fonctionnement du catalogue (`catalogue.json`)

Au démarrage de l'application :
1. `RemoteCatalogueService` tente de récupérer la dernière version de `catalogue.json` depuis l'URL publique configurée (sans aucune clé ni secret).
2. En cas de succès, le JSON brut est mis en cache localement dans `filesDir/catalogue/catalogue_cache.json` et synchronisé dans Room via le champ `version` **sans jamais effacer les favoris (`isFavorite`), la progression (`isCompleted`) ni les PDF déjà téléchargés** (sauf si une nouvelle `version` rend un PDF obsolète).
3. En absence de réseau, l'application reste 100 % fonctionnelle grâce au dernier cache local (`catalogue_cache.json`), à la base Room et aux PDF déjà téléchargés.

---

## 2. Schéma du fichier `catalogue.json`

```json
{
  "schemaVersion": 3,
  "catalogueVersion": 3,
  "devoirs": [
    {
      "id": "college_9eme_maths_ds1",
      "titre": "Devoir de Synthèse N°1 — Arithmétique, Factorisation & Thalès",
      "matiere": "Mathématiques",
      "niveau": "COLLEGE_9",
      "section": "TRONC_COMMUN",
      "type": "SYNTHESE",
      "trimestre": "T1",
      "duree": 120,
      "difficulte": "Fondamental",
      "chapitres": ["PGCD & Racines carrées", "Factorisation", "Théorème de Thalès"],
      "source": "Sujet d'entraînement",
      "version": 3,
      "tailleMo": 4.2,
      "sujetUrl": "https://cdn.devoirstn.example/pdf/college_9eme_maths_ds1_sujet.pdf",
      "corrigeUrl": "https://cdn.devoirstn.example/pdf/college_9eme_maths_ds1_corrige.pdf",
      "sujetSizeBytes": 2411724,
      "corrigeSizeBytes": 1992294,
      "sujetSha256": "optionnel_sha256_hex_64_caracteres",
      "corrigeSha256": "optionnel_sha256_hex_64_caracteres"
    }
  ]
}
```

### Description des champs

| Champ | Type | Obligatoire | Description |
| :--- | :--- | :---: | :--- |
| `id` | `String` | Oui | Identifiant unique en `snake_case` (ex. `college_9eme_maths_ds1`). |
| `titre` | `String` | Oui | Titre complet affiché dans la bibliothèque et la fiche devoir. |
| `matiere` | `String` | Oui | Matière tunisienne (`Mathématiques`, `Physique-Chimie`, `SVT`, `Informatique`, `Français`, `Arabe`, `Anglais`, `Économie & Gestion`…). |
| `niveau` | `String` | Oui | `PRIMAIRE_6`, `COLLEGE_7`, `COLLEGE_8`, `COLLEGE_9`, `LYCEE_1`, `LYCEE_2`, `LYCEE_3`, `BAC`, `UNIVERSITE`. |
| `section` | `String` | Oui | `TRONC_COMMUN`, `MATHS`, `SCIENCES_EXP`, `INFORMATIQUE`, `ECO_GESTION`, `TECHNIQUE`, `LETTRES`, `PREPA_LICENCE`. |
| `type` | `String` | Oui | `CONTROLE`, `SYNTHESE`, `EXAMEN`. |
| `trimestre` | `String` | Oui | `T1`, `T2`, `T3`, `NATIONAL`. |
| `duree` | `Int` | Oui | Durée officielle en minutes (ex. `60`, `120`, `180`). |
| `difficulte` | `String` | Non | `Fondamental`, `Standard`, `Bac Type`, `Avancé` (défaut : `Standard`). |
| `chapitres` | `List<String>` | Non | Mots-clés des chapitres couverts (indexés par la recherche). |
| `source` | `String` | Non | Mention d'origine (défaut : `Sujet d'entraînement`). |
| `version` | `Int` | Oui | Numéro de version incrémental ; incrémenter pour propager une mise à jour. |
| `tailleMo` | `Double` | Non | Taille estimée totale (sujet + corrigé) en Mo pour l'affichage et l'alerte > 20 Mo. |
| `sujetUrl` | `String` | Oui | URL HTTPS publique du PDF du sujet. |
| `corrigeUrl` | `String?` | Non | URL HTTPS publique du PDF du corrigé (`null` si non disponible). |
| `sujetSizeBytes` | `Long?` | Non | Taille exacte en octets du sujet pour vérification d'intégrité. |
| `corrigeSizeBytes` | `Long?` | Non | Taille exacte en octets du corrigé pour vérification d'intégrité. |
| `sujetSha256` | `String?` | Non | Empreinte SHA-256 hexadécimale du sujet (vérifiée avant validation). |
| `corrigeSha256` | `String?` | Non | Empreinte SHA-256 hexadécimale du corrigé (vérifiée avant validation). |

---

## 3. Téléchargements, intégrité et stockage

- **Reprise (`Range`)** : chaque téléchargement écrit dans `<nom>.pdf.part` ; en cas d'annulation ou de coupure réseau, WorkManager reprend à l'octet suivant via l'en-tête HTTP `Range: bytes=<offset>-`.
- **Intégrité** : avant de valider un fichier `.pdf`, `PdfIntegrityVerifier` vérifie l'en-tête magique `%PDF-`, la somme **SHA-256** (si fournie) ou la **taille exacte en octets**.
- **Réseaux** : par défaut, les téléchargements sont restreints au **Wi-Fi (`NetworkType.UNMETERED`)**. L'élève peut autoriser les données mobiles depuis l'écran **Stockage** ; tout téléchargement supérieur à **20 Mo** sur réseau mobile affiche un avertissement explicite.

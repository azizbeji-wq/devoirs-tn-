package com.example.data.local

import com.example.data.model.AppFeatureIdea
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.SectionCoefficientPreset
import com.example.data.model.StudyMethodGuide
import com.example.data.model.SubjectCoefficient
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection

object TunisianCurriculumSeed {

    val devoirs: List<DevoirItem> = listOf(
        DevoirItem(
            id = "bac_maths_ds1_analyse",
            title = "Devoir de Synthèse N°1 — Suites, Limites & Nombres Complexes",
            subject = "Mathématiques",
            level = CycleLevel.BAC,
            section = TunisianSection.MATHS,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 240,
            totalPoints = 20,
            difficulty = "Pilote",
            sourceSchool = "Lycée Pilote Bourguiba · Tunis",
            chapterTags = listOf("Nombres complexes", "Suites réelles", "Continuité & TVI", "Isométries"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Nombres complexes et lieu géométrique",
                    points = "5 pts",
                    statement = "Le plan complexe est muni d'un repère orthonormé direct (O, u, v).\n1. Résoudre dans ℂ l'équation (E) : z² - 2(1 + i√3)z + 4i√3 = 0.\n2. On note A et B les points d'affixes respectives z_A = 2 et z_B = 2i√3.\n   a) Écrire z_A et z_B sous forme exponentielle.\n   b) Montrer que le triangle OAB est rectangle en O et déterminer l'affixe du centre Ω de son cercle circonscrit.\n3. Pour tout z ≠ 2, on pose Z = (z - 2i√3) / (z - 2). Déterminer l'ensemble des points M(z) tels que Z soit imaginaire pur.",
                    solution = "1. Δ' = (1 + i√3)² - 4i√3 = 1 + 2i√3 - 3 - 4i√3 = -2 - 2i√3 = (1 - i√3)².\nLes racines sont z₁ = (1 + i√3) + (1 - i√3) = 2 et z₂ = (1 + i√3) - (1 - i√3) = 2i√3.\n2. a) z_A = 2·e^(i·0) et z_B = 2√3·e^(iπ/2).\n   b) arg(z_B / z_A) ≡ π/2 [2π], donc (OA) ⊥ (OB). L'hypoténuse est [AB], donc Ω est le milieu de [AB] : z_Ω = (2 + 2i√3)/2 = 1 + i√3.\n3. Z imaginaire pur ⇔ arg((z - z_B)/(z - z_A)) ≡ π/2 [π] ou z = z_B.\nDonc M appartient au cercle de diamètre [AB] privé du point A."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Suite récurrente et théorème des gendarmes",
                    points = "7 pts",
                    statement = "On considère la suite (u_n) définie par u₀ = 2 et u_{n+1} = (1/2)·(u_n + 3/u_n) pour tout n ∈ ℕ.\n1. Montrer par récurrence que pour tout n ∈ ℕ, u_n ≥ √3.\n2. Étudier la monotonie de la suite (u_n) et en déduire qu'elle converge vers une limite ℓ à déterminer.\n3. Montrer que pour tout n ∈ ℕ : 0 ≤ u_{n+1} - √3 ≤ (1/2)(u_n - √3).\n4. En déduire la limite de u_n et une valeur approchée de √3 à 10⁻³ près.",
                    solution = "1. Initialisation : u₀ = 2 ≥ √3 (car 4 ≥ 3). Hérédité : u_{n+1} - √3 = (u_n - √3)² / (2u_n) ≥ 0 car u_n > 0.\n2. u_{n+1} - u_n = (3 - u_n²) / (2u_n) ≤ 0 car u_n ≥ √3. Donc (u_n) est décroissante et minorée par √3 : elle converge vers ℓ ≥ √3 vérifiant ℓ = (ℓ + 3/ℓ)/2 ⇒ ℓ² = 3 ⇒ ℓ = √3.\n3. u_{n+1} - √3 = (u_n - √3) · [(u_n - √3) / (2u_n)]. Comme 0 ≤ u_n - √3 < u_n, le rapport est ≤ 1/2.\n4. Par récurrence : 0 ≤ u_n - √3 ≤ (1/2)^n (2 - √3) → 0. Dès n = 2, u₂ = 1,7321 approche √3 à 10⁻³ près."
                ),
                DevoirExercise(
                    number = 3,
                    title = "Étude complète d'une fonction irrationnelle",
                    points = "8 pts",
                    statement = "Soit f la fonction définie sur [0, +∞[ par f(x) = x - √(x² + 1) + 2.\n1. Calculer lim_{x→+∞} f(x) et interpréter géométriquement le résultat.\n2. Étudier la dérivabilité de f sur [0, +∞[ et dresser son tableau de variation.\n3. Montrer que f réalise une bijection de [0, +∞[ sur un intervalle J à préciser.\n4. Expliciter f⁻¹(y) pour tout y ∈ J.",
                    solution = "1. En multipliant par l'expression conjuguée : x - √(x² + 1) = -1 / (x + √(x² + 1)) → 0 quand x → +∞. Ainsi lim f(x) = 2 : la droite y = 2 est asymptote horizontale au voisinage de +∞.\n2. f'(x) = 1 - x / √(x² + 1) > 0 car √(x² + 1) > x ≥ 0. Donc f est strictement croissante de f(0) = 1 vers 2.\n3. f est continue et strictement croissante sur [0, +∞[, donc bijective de [0, +∞[ sur J = [1, 2[.\n4. y = f(x) ⇔ √(x² + 1) = x + 2 - y. En élevant au carré : x² + 1 = x² + 2x(2 - y) + (2 - y)², d'où f⁻¹(y) = [1 - (2 - y)²] / [2(2 - y)]."
                )
            )
        ),
        DevoirItem(
            id = "bac_scexp_dc1_physique",
            title = "Devoir de Contrôle N°1 — Dipôle RC & Cinétique Chimique",
            subject = "Physique-Chimie",
            level = CycleLevel.BAC,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Lycée Pilote de l'Ariana",
            chapterTags = listOf("Dipôle RC", "Constante de temps τ", "Cinétique chimique", "Vitesse volumique"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Charge d'un condensateur à travers un résistor",
                    points = "11 pts",
                    statement = "On réalise un circuit série comportant un générateur idéal de tension E = 12 V, un conducteur ohmique de résistance R = 1,0 kΩ et un condensateur initialement déchargé de capacité C = 47 μF.\n1. Établir l'équation différentielle vérifiée par la tension u_C(t) aux bornes du condensateur.\n2. Vérifier que u_C(t) = E(1 - e^(-t/τ)) est solution et exprimer τ en fonction de R et C.\n3. Calculer τ, l'intensité initiale I₀ et l'énergie maximale E_max emmagasinée par le condensateur.",
                    solution = "1. Loi des mailles : u_R + u_C = E ⇒ R·i + u_C = E. Or i = C·(du_C/dt), donc RC·(du_C/dt) + u_C = E.\n2. En dérivant u_C(t) : du_C/dt = (E/τ)e^(-t/τ). En reportant dans l'équation différentielle, on obtient (RC/τ - 1)·E·e^(-t/τ) = 0, d'où τ = R·C.\n3. τ = 1000 × 47×10⁻⁶ = 47 ms.\nI₀ = E / R = 12 / 1000 = 12 mA.\nE_max = ½ C E² = 0,5 × 47×10⁻⁶ × 144 = 3,38 mJ."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Suivi cinétique de l'oxydation des ions iodure",
                    points = "9 pts",
                    statement = "À t = 0, on mélange V₁ = 50 mL d'une solution de peroxodisulfate S₂O₈²⁻ (C₁ = 0,10 mol·L⁻¹) avec V₂ = 50 mL d'une solution d'iodure de potassium I⁻ (C₂ = 0,40 mol·L⁻¹).\nÉquation bilan : S₂O₈²⁻ + 2 I⁻ → 2 SO₄²⁻ + I₂.\n1. Dresser le tableau d'avancement et identifier le réactif limitant.\n2. Déterminer la concentration finale maximale [I₂]_max.\n3. Comment évolue la vitesse volumique de réaction au cours du temps ? Justifier.",
                    solution = "1. n₀(S₂O₈²⁻) = 5,0×10⁻³ mol ; n₀(I⁻) = 2,0×10⁻² mol.\nComparaison : 5,0×10⁻³ / 1 < 2,0×10⁻² / 2 (1,0×10⁻²). Donc S₂O₈²⁻ est le réactif limitant et x_max = 5,0×10⁻³ mol.\n2. Volume total V_T = 100 mL = 0,10 L. [I₂]_max = x_max / V_T = 5,0×10⁻² mol·L⁻¹.\n3. La vitesse volumique diminue au cours du temps car les concentrations des réactifs (facteur cinétique) diminuent progressivement jusqu'à s'annuler à l'état final."
                )
            )
        ),
        DevoirItem(
            id = "bac_scexp_ds2_svt",
            title = "Devoir de Synthèse N°2 — Génétique Humaine & Neurophysiologie",
            subject = "SVT",
            level = CycleLevel.BAC,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T2,
            durationMinutes = 180,
            totalPoints = 20,
            difficulty = "Bac Type",
            sourceSchool = "Lycée Pilote de Sfax",
            chapterTags = listOf("Génétique humaine", "Pedigree", "Potentiel d'action", "Synapse"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Transmission d'une maladie héréditaire (Pedigree)",
                    points = "10 pts",
                    statement = "Dans un arbre généalogique familial, deux parents phénotypiquement sains (I₁ et I₂) donnent naissance à une fille II₂ atteinte d'une anomalie métabolique rare.\n1. L'allèle responsable de la maladie est-il dominant ou récessif ? Justifier rigoureusement.\n2. Discuter la localisation chromosomique (autosomale ou liée au chromosome X) de ce gène.\n3. Donner les génotypes certains de I₁, I₂ et II₂.",
                    solution = "1. Deux parents sains I₁ et I₂ ont un enfant atteint II₂ : l'allèle morbide m était masqué chez les parents, il est donc récessif (noté m) face à l'allèle normal dominant N.\n2. Si le gène était porté par X, la fille atteinte II₂ (X^m//X^m) aurait reçu un X^m de son père I₁. Or I₁ serait alors atteint (X^m//Y), ce qui est contredit par l'énoncé (I₁ est sain). La transmission est donc autosomale récessive.\n3. Génotypes : I₁ (N//m), I₂ (N//m), II₂ (m//m)."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Transmission synaptique et intégration neuronale",
                    points = "10 pts",
                    statement = "On stimule successivement deux afférences A et B reliées à un motoneurone M.\n- Stimulation isolée de A : enregistrement en M d'une dépolarisation de +8 mV (seuil de déclenchement = +15 mV).\n- Stimulation isolée de B : enregistrement en M d'une hyperpolarisation de -5 mV.\n1. Qualifier les synapses A-M et B-M en justifiant.\n2. Expliquer quel phénomène permet à la stimulation rapprochée de A deux fois de suite de déclencher un potentiel d'action en M.",
                    solution = "1. A-M est une synapse excitatrice car elle engendre un PPSE (+8 mV, entrée de Na⁺). B-M est une synapse inhibitrice car elle engendre un PPSI (-5 mV, entrée de Cl⁻ ou sortie de K⁺).\n2. Deux stimulations très rapprochées sur A produisent une sommation temporelle des deux PPSE : la dépolarisation cumulée atteint/dépasse le seuil de +15 mV au cône axonique, déclenchant un potentiel d'action propagé."
                )
            )
        ),
        DevoirItem(
            id = "bac_info_ds1_algo",
            title = "Devoir de Synthèse N°1 — Algorithmes Python, Récursivité & SQL",
            subject = "Informatique",
            level = CycleLevel.BAC,
            section = TunisianSection.INFORMATIQUE,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            totalPoints = 20,
            difficulty = "Bac Type",
            sourceSchool = "Lycée Pilote de Sousse",
            chapterTags = listOf("Python", "Récursivité", "Tri par insertion", "Bases de données SQL"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Nombre premier circulaire & fonction récursive",
                    points = "10 pts",
                    statement = "Un entier N > 1 est dit « super-pair » si tous ses chiffres sont pairs.\n1. Écrire en Python une fonction récursive `est_super_pair(n)` qui retourne `True` si n est super-pair, `False` sinon.\n2. Écrire une procédure `filtrer_fichier(source, dest)` qui lit un fichier texte contenant un entier par ligne et recopie dans `dest` uniquement les nombres super-pairs triés par ordre croissant.",
                    solution = "1. Solution Python récursive :\n```python\ndef est_super_pair(n):\n    if n < 10:\n        return n % 2 == 0\n    if (n % 10) % 2 != 0:\n        return False\n    return est_super_pair(n // 10)\n```\n2. Lecture, filtrage, tri (tri par sélection/insertion) et écriture ligne par ligne :\n```python\ndef filtrer_fichier(source, dest):\n    with open(source, 'r') as f:\n        vals = [int(l.strip()) for l in f if l.strip() and est_super_pair(int(l.strip()))]\n    vals.sort()\n    with open(dest, 'w') as g:\n        for v in vals:\n            g.write(str(v) + '\\n')\n```"
                ),
                DevoirExercise(
                    number = 2,
                    title = "Requêtes SQL — Gestion d'une bibliothèque scolaire",
                    points = "10 pts",
                    statement = "Soit la base relationnelle :\n- ELEVE(IdEleve, Nom, Niveau, Section)\n- EMPRUNT(IdEmprunt, #IdEleve, #CodeLivre, DateEmp)\n- LIVRE(CodeLivre, Titre, Matiere, NbExemplaires)\nÉcrire en SQL :\n1. La liste des titres des livres de « Mathématiques » ayant au moins 3 exemplaires.\n2. Le nombre d'emprunts effectués par chaque élève de niveau « Bac » trié par ordre décroissant.",
                    solution = "1.\n```sql\nSELECT Titre\nFROM LIVRE\nWHERE Matiere = 'Mathématiques' AND NbExemplaires >= 3;\n```\n2.\n```sql\nSELECT E.Nom, COUNT(M.IdEmprunt) AS TotalEmprunts\nFROM ELEVE E\nJOIN EMPRUNT M ON E.IdEleve = M.IdEleve\nWHERE E.Niveau = 'Bac'\nGROUP BY E.IdEleve, E.Nom\nORDER BY TotalEmprunts DESC;\n```"
                )
            )
        ),
        DevoirItem(
            id = "bac_eco_ds1_economie",
            title = "Devoir de Synthèse N°1 — Croissance Économique & Élasticité",
            subject = "Économie & Gestion",
            level = CycleLevel.BAC,
            section = TunisianSection.ECO_GESTION,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            totalPoints = 20,
            difficulty = "Bac Type",
            sourceSchool = "Lycée Menzah 6 · Tunis",
            chapterTags = listOf("PIB & Croissance", "Élasticité-prix", "Productivité", "Dissertation éco"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Calculs économiques : PIB réel et taux de croissance",
                    points = "10 pts",
                    statement = "Une économie présente un PIB nominal de 145 milliards DT en 2024 et 158 milliards DT en 2025, avec un indice des prix passé de 100 à 105.\n1. Calculer le taux de croissance du PIB nominal entre 2024 et 2025.\n2. Calculer le PIB réel de 2025 aux prix de 2024 puis le taux de croissance réel.\n3. Expliquer pourquoi le PIB réel est un meilleur indicateur de la création de richesse.",
                    solution = "1. Taux nominal = ((158 - 145) / 145) × 100 = +8,97 %.\n2. PIB réel 2025 = (158 / 105) × 100 = 150,48 milliards DT.\nTaux de croissance réel = ((150,48 - 145) / 145) × 100 = +3,78 %.\n3. Le PIB réel élimine l'effet de l'inflation (hausse des prix) et mesure uniquement l'augmentation réelle des volumes produits."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Mini-dissertation : Investissement et croissance",
                    points = "10 pts",
                    statement = "Sujet : « Montrez comment l'investissement matériel et immatériel stimule la croissance économique durable en Tunisie. »\nProposer un plan structuré en deux parties et rédiger l'introduction.",
                    solution = "Introduction : Accroche sur les défis de compétitivité, définition de l'investissement (FBCF + recherche/formation), problématique et annonce du plan.\nPlan recommandé :\n- I. L'investissement matériel, moteur de la demande et des capacités productives (effet multiplicateur keynésien, infrastructures).\n- II. L'investissement immatériel, levier de productivité globale et d'innovation (capital humain, R&D, transition numérique)."
                )
            )
        ),
        DevoirItem(
            id = "bac_tech_dc2_technique",
            title = "Devoir de Contrôle N°2 — GRAFCET & Circuits Logiques Séquentiels",
            subject = "Technologie",
            level = CycleLevel.BAC,
            section = TunisianSection.TECHNIQUE,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Lycée Technique de Nabeul",
            chapterTags = listOf("GRAFCET", "Bascule JK", "Tableau de Karnaugh", "Vérin double effet"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Simplification par tableau de Karnaugh",
                    points = "8 pts",
                    statement = "Un système d'alarme possède 3 capteurs a, b, c. La sortie S = 1 lorsque exactement deux capteurs ou tous les trois sont à 1.\n1. Dresser la table de vérité de S.\n2. Simplifier S à l'aide d'un tableau de Karnaugh.\n3. Donner le logigramme minimal à l'aide de portes ET / OU.",
                    solution = "1. Les mintermes donnant S = 1 sont : (0,1,1), (1,0,1), (1,1,0) et (1,1,1).\n2. Regroupements par paires dans Karnaugh : S = a·b + b·c + a·c.\n3. Logigramme : 3 portes ET à 2 entrées (ab, bc, ac) reliées à une porte OU à 3 entrées."
                ),
                DevoirExercise(
                    number = 2,
                    title = "GRAFCET d'un poste de perçage automatisé",
                    points = "12 pts",
                    statement = "En appuyant sur départ cycle (dcy) avec pièce présente (p), un vérin V1 serre la pièce jusqu'au capteur s1, puis la broche descend (V2+) jusqu'à b1, remonte (V2-) jusqu'à b0, et V1 desserre jusqu'à s0.\n1. Identifier les capteurs et actionneurs.\n2. Écrire les équations d'activation et de désactivation des étapes X1 et X2.",
                    solution = "1. Capteurs : dcy, p, s0, s1, b0, b1. Actionneurs : distributeurs bistables commandant V1+, V1-, V2+, V2-.\n2. Étape 0 (attente) → T0 = dcy·p → Étape 1 (V1+) → T1 = s1 → Étape 2 (V2+) → T2 = b1 → Étape 3 (V2-) → T3 = b0 → Étape 4 (V1-) → T4 = s0 → Étape 0.\nActivation X1 = X0·dcy·p ; Désactivation X1 = X2.\nActivation X2 = X1·s1 ; Désactivation X2 = X3."
                )
            )
        ),
        DevoirItem(
            id = "bac_lettres_exam_philo",
            title = "Sujet Bac Blanc — Dissertation Philosophique : Liberté & Autrui",
            subject = "Philosophie",
            level = CycleLevel.BAC,
            section = TunisianSection.LETTRES,
            type = DevoirType.EXAMEN,
            trimester = Trimester.NATIONAL,
            durationMinutes = 240,
            totalPoints = 20,
            difficulty = "Bac Type",
            sourceSchool = "Session Nationale · Programme Tunisien",
            chapterTags = listOf("Autrui", "Liberté", "L'État & la Loi", "Méthodologie Bac"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Questions de compréhension et définition conceptuelle",
                    points = "6 pts",
                    statement = "1. Distinguer « liberté naturelle » et « liberté civile » chez Rousseau.\n2. Expliquer brièvement en quoi le regard d'autrui peut être à la fois limite et condition de la conscience de soi.",
                    solution = "1. La liberté naturelle consiste à suivre ses impulsions sans borne autre que la force individuelle ; la liberté civile naît du contrat social : « l'obéissance à la loi qu'on s'est prescrite est liberté ».\n2. Chez Sartre, le regard d'autrui peut réifier (« l'enfer, c'est les autres »), mais chez Hegel et Levinas, la reconnaissance par autrui est indispensable pour accéder à la conscience de soi et à la responsabilité éthique."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Dissertation : « La présence d'autrui est-elle un obstacle à ma liberté ? »",
                    points = "14 pts",
                    statement = "Traiter le sujet en respectant la démarche analytique et critique attendue au Baccalauréat tunisien (Problématisation, Thèse, Antithèse, Dépassement/Synthèse).",
                    solution = "Problématique : Si autrui semble d'abord restreindre mon champ d'action par ses désirs et son jugement, n'est-il pas paradoxalement le médiateur grâce auquel ma liberté devient réelle et sensée ?\n- Moment 1 (Thèse) : Autrui comme limite et menace (conflit des consciences, conformisme social, regard qui fige).\n- Moment 2 (Antithèse) : Autrui comme condition d'épanouissement (langage, coopération, reconnaissance mutuelle, solidarité).\n- Moment 3 (Dépassement) : La liberté authentique n'est pas l'isolement solitaire mais la co-liberté régulée par le droit et l'éthique."
                )
            )
        ),
        DevoirItem(
            id = "college_9eme_concours_maths",
            title = "Concours Pilote 9ème Année — Algèbre, Thalès & Pythagore",
            subject = "Mathématiques",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.EXAMEN,
            trimester = Trimester.NATIONAL,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Pilote",
            sourceSchool = "Concours d'Accès aux Lycées Pilotes · Tunisie",
            chapterTags = listOf("Racines carrées", "Théorème de Thalès", "Identités remarquables", "Inéquations"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Calculs algébriques et radicaux",
                    points = "8 pts",
                    statement = "On donne A = √48 - 2√27 + √75 et B = (2√3 - 1)² - (√3 + 2)(√3 - 2).\n1. Simplifier A et montrer que A = 3√3.\n2. Développer et réduire B.\n3. Résoudre dans ℝ l'inéquation : 2(x - 3) + 5 ≤ 4x - 7 et représenter les solutions sur une droite graduée.",
                    solution = "1. A = 4√3 - 6√3 + 5√3 = 3√3.\n2. B = (12 - 4√3 + 1) - (3 - 4) = 13 - 4√3 - (-1) = 14 - 4√3.\n3. 2x - 6 + 5 ≤ 4x - 7 ⇒ -2x ≤ -6 ⇒ x ≥ 3. Donc S_ℝ = [3, +∞[."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Configuration de Thalès et triangle rectangle",
                    points = "12 pts",
                    statement = "Soit ABC un triangle tel que AB = 6 cm, AC = 8 cm et BC = 10 cm.\n1. Montrer que ABC est rectangle en A.\n2. Soit M ∈ [AB] tel que AM = 2,4 cm. La parallèle à (BC) passant par M coupe [AC] en N.\n   Calculer AN et MN.\n3. Calculer l'aire du trapèze BMNC.",
                    solution = "1. AB² + AC² = 36 + 64 = 100 = BC². D'après la réciproque du théorème de Pythagore, ABC est rectangle en A.\n2. Comme (MN) // (BC), d'après Thalès : AM/AB = AN/AC = MN/BC = 2,4/6 = 0,4.\nDonc AN = 0,4 × 8 = 3,2 cm et MN = 0,4 × 10 = 4 cm.\n3. Aire(ABC) = (6×8)/2 = 24 cm² ; Aire(AMN) = (2,4×3,2)/2 = 3,84 cm².\nAire(BMNC) = 24 - 3,84 = 20,16 cm²."
                )
            )
        ),
        DevoirItem(
            id = "college_9eme_ds1_francais",
            title = "Devoir de Synthèse N°1 — Compréhension, Langue & Essai",
            subject = "Français",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Collège Pilote de Monastir",
            chapterTags = listOf("Compréhension", "Subordonnée relative", "Voix passive", "Production écrite"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Langue : Syntaxe et conjugaison",
                    points = "8 pts",
                    statement = "1. Transformer à la voix passive : « Les élèves préparent activement l'exposition scientifique. »\n2. Relier les deux phrases par un pronom relatif convenable : « C'est un roman passionnant. Je t'ai parlé de ce roman hier. »\n3. Mettre le verbe entre parenthèses au mode et temps convenables : « Bien qu'il (être) fatigué, il a terminé son devoir. »",
                    solution = "1. « L'exposition scientifique est préparée activement par les élèves. »\n2. « C'est un roman passionnant dont je t'ai parlé hier. »\n3. « Bien qu'il soit fatigué, il a terminé son devoir. » (Subjonctif présent après « bien que »)."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Essai argumentatif (15–20 lignes)",
                    points = "12 pts",
                    statement = "Sujet : « Pensez-vous que les écrans et smartphones aident les collégiens à mieux réussir leurs études ou les distraient-ils ? »\nRédigez un paragraphe argumenté illustré par deux exemples précis.",
                    solution = "Grille de réussite (Barème tunisien) :\n- Compréhension du sujet et position claire (3 pts).\n- Deux arguments distincts (ex: accès aux devoirs corrigés / dictionnaires vs perte de concentration et sommeil) avec connecteurs logiques : D'une part, En outre, Cependant, En conclusion (5 pts).\n- Correction de la langue, orthographe et ponctuation (4 pts)."
                )
            )
        ),
        DevoirItem(
            id = "lycee_1ere_dc1_maths",
            title = "Devoir de Contrôle N°1 — Activités Numériques & Vecteurs",
            subject = "Mathématiques",
            level = CycleLevel.LYCEE_1,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 60,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Lycée El Khaznadar · Tunis",
            chapterTags = listOf("PGCD / PPCM", "Puissances", "Vecteurs & Translations", "Colinéarité"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Arithmétique : PGCD, PPCM et fraction irréductible",
                    points = "10 pts",
                    statement = "1. Décomposer 420 et 504 en produits de facteurs premiers.\n2. En déduire PGCD(420, 504) et PPCM(420, 504).\n3. Rendre la fraction 420 / 504 irréductible.",
                    solution = "1. 420 = 2² × 3 × 5 × 7 ; 504 = 2³ × 3² × 7.\n2. PGCD(420, 504) = 2² × 3 × 7 = 84.\nPPCM(420, 504) = 2³ × 3² × 5 × 7 = 2520.\n3. 420 / 504 = (420 ÷ 84) / (504 ÷ 84) = 5 / 6."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Calcul vectoriel dans un parallélogramme",
                    points = "10 pts",
                    statement = "Soit ABCD un parallélogramme de centre O.\n1. Simplifier la somme vectorielle : u = AB + CD + BC + DA.\n2. Construire le point E tel que AE = AB + AD.\n3. Que représente E par rapport au parallélogramme ABCD ?",
                    solution = "1. Par relation de Chasles : u = (AB + BC) + (CD + DA) = AC + CA = 0 (vecteur nul).\n2. Dans le parallélogramme ABCD, AB + AD = AC (règle du parallélogramme).\n3. Donc AE = AC, ce qui signifie que le point E est confondu avec le point C."
                )
            )
        ),
        DevoirItem(
            id = "lycee_2eme_ds2_physique",
            title = "Devoir de Synthèse N°2 — Loi d'Ohm, Puissance & Solutions Aqueuses",
            subject = "Physique-Chimie",
            level = CycleLevel.LYCEE_2,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T2,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Lycée Pilote de Bizerte",
            chapterTags = listOf("Générateur (E, r)", "Point de fonctionnement", "Concentration molaire", "Dilution"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Association générateur et conducteur ohmique",
                    points = "11 pts",
                    statement = "Un générateur de f.e.m E = 9 V et de résistance interne r = 2 Ω alimente un résistor R = 16 Ω.\n1. Calculer l'intensité I du courant dans le circuit.\n2. Calculer la tension U aux bornes du résistor et le rendement η du générateur.",
                    solution = "1. Loi de Pouillet : I = E / (R + r) = 9 / (16 + 2) = 0,5 A.\n2. U = R·I = 16 × 0,5 = 8 V.\nRendement : η = U / E = 8 / 9 ≈ 88,9 %."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Préparation d'une solution par dilution",
                    points = "9 pts",
                    statement = "On dispose d'une solution mère S₀ de sulfate de cuivre de concentration C₀ = 0,50 mol·L⁻¹. On souhaite préparer V₁ = 250 mL d'une solution fille S₁ de concentration C₁ = 0,05 mol·L⁻¹.\n1. Calculer le volume V₀ de solution mère à prélever.\n2. Citer la verrerie précise nécessaire pour cette dilution.",
                    solution = "1. Conservation de la matière : C₀·V₀ = C₁·V₁ ⇒ V₀ = (0,05 × 250) / 0,50 = 25 mL.\n2. Verrerie : pipette jaugée de 25 mL avec propipette, fiole jaugée de 250 mL, pissette d'eau distillée et bécher."
                )
            )
        ),
        DevoirItem(
            id = "lycee_3eme_dc2_anglais",
            title = "Devoir de Contrôle N°2 — Reading, Grammar & Guided Writing",
            subject = "Anglais",
            level = CycleLevel.LYCEE_3,
            section = TunisianSection.MATHS,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 60,
            totalPoints = 20,
            difficulty = "Standard",
            sourceSchool = "Lycée Pilote de Gabès",
            chapterTags = listOf("Tenses & Conditionals", "Word Formation", "Essay Writing", "Education & Tech"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Language: Put the bracketed words in the right tense or form",
                    points = "10 pts",
                    statement = "1. If Tunisian students (organize) __________ their revision earlier, they will feel much more confident.\n2. Renewable energy plays an (increase) __________ important role in Tunisia's economy.\n3. By the end of June, the candidates (complete) __________ all their national exams.\n4. She succeeded thanks to her (determine) __________ and daily discipline.",
                    solution = "1. organize (Type 1 conditional: Present Simple + will).\n2. increasingly (Adverb modifying adjective 'important').\n3. will have completed (Future Perfect triggered by 'By the end of June').\n4. determination (Noun after possessive 'her')."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Writing: 12-line Article for School Magazine",
                    points = "10 pts",
                    statement = "Topic: Write a short article giving three practical tips to help 3rd-year high school students balance exam preparation and healthy habits.",
                    solution = "Scoring criteria:\n- Adherence to task & format (title + 3 clear tips: active recall, 45-min study blocks, 8h sleep) : 4 pts.\n- Language accuracy & connectors (First, Furthermore, Finally) : 4 pts.\n- Mechanics (spelling, capitalization, punctuation) : 2 pts."
                )
            )
        ),
        DevoirItem(
            id = "college_7eme_dc1_maths",
            title = "Devoir de Contrôle N°1 — Priorités Opératoires & Symétrie",
            subject = "Mathématiques",
            level = CycleLevel.COLLEGE_7,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 60,
            totalPoints = 20,
            difficulty = "Fondamental",
            sourceSchool = "Collège Ibn Khaldoun · Tunis",
            chapterTags = listOf("Priorités opératoires", "Nombres décimaux", "Médiatrice", "Angles"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Calculs avec priorités et distributivité",
                    points = "10 pts",
                    statement = "Calculer en détaillant les étapes :\n1. A = 24 - 3 × (5 + 2) + 12 ÷ 4\n2. B = 17 × 99 (en utilisant la distributivité de la multiplication)\n3. Trouver le chiffre manquant pour que 45_2 soit divisible par 9.",
                    solution = "1. A = 24 - 3 × 7 + 3 = 24 - 21 + 3 = 6.\n2. B = 17 × (100 - 1) = 1700 - 17 = 1683.\n3. Somme des chiffres : 4 + 5 + ? + 2 = 11 + ?. Le multiple de 9 suivant est 18, donc le chiffre manquant est 7 (4572)."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Médiatrice d'un segment et triangle isocèle",
                    points = "10 pts",
                    statement = "Soit [AB] un segment de longueur 6 cm et Δ sa médiatrice passant par le milieu I.\n1. On place un point M sur Δ tel que MA = 5 cm. Quelle est la longueur MB ? Justifier.\n2. Quelle est la nature du triangle MAB ?",
                    solution = "1. Tout point appartenant à la médiatrice d'un segment est équidistant de ses extrémités. Comme M ∈ Δ, MB = MA = 5 cm.\n2. Le triangle MAB possède deux côtés égaux (MA = MB = 5 cm) : il est isocèle en M."
                )
            )
        ),
        DevoirItem(
            id = "college_8eme_dc2_svt",
            title = "Devoir de Contrôle N°2 — Reproduction & Tectonique des Plaques",
            subject = "SVT",
            level = CycleLevel.COLLEGE_8,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 60,
            totalPoints = 20,
            difficulty = "Fondamental",
            sourceSchool = "Collège Pilote de Nabeul",
            chapterTags = listOf("Séismes", "Plaques lithosphériques", "Reproduction végétale"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Origine des séismes et failles",
                    points = "10 pts",
                    statement = "1. Définir le foyer (hypocentre) et l'épicentre d'un séisme.\n2. Expliquer pourquoi la région méditerranéenne connaît une activité sismique notable.",
                    solution = "1. Le foyer est le point en profondeur où se produit la rupture des roches ; l'épicentre est le point à la surface situé à la verticale du foyer où les secousses sont les plus fortes.\n2. La Méditerranée est située à la zone de convergence (rapprochement) entre la plaque africaine et la plaque eurasiatique, générant des contraintes et des failles actives."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Pollinisation et fécondation chez les plantes à fleurs",
                    points = "10 pts",
                    statement = "Remettre dans l'ordre chronologique : formation du fruit, pollinisation, germination du grain de pollen, fécondation, formation de la graine.",
                    solution = "Ordre exact : 1) Pollinisation → 2) Germination du grain de pollen → 3) Fécondation → 4) Transformation de l'ovule en graine et de l'ovaire en fruit."
                )
            )
        ),
        DevoirItem(
            id = "primaire_6eme_concours_maths",
            title = "Concours 6ème Année — Problèmes de Pourcentages & Périmètres",
            subject = "Mathématiques",
            level = CycleLevel.PRIMAIRE_6,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.EXAMEN,
            trimester = Trimester.NATIONAL,
            durationMinutes = 60,
            totalPoints = 20,
            difficulty = "Concours 6ème",
            sourceSchool = "Concours d'Entrée aux Collèges Pilotes (Sixième)",
            chapterTags = listOf("Fractions", "Pourcentages", "Aire du rectangle", "Vitesse moyenne"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Problème : Achat de fournitures scolaires",
                    points = "10 pts",
                    statement = "Sami dispose de 60 dinars. Il dépense les 2/5 de sa somme pour acheter un cartable, puis 25 % du reste pour des cahiers.\n1. Quel est le prix du cartable ?\n2. Quelle somme lui reste-t-il après tous ses achats ?",
                    solution = "1. Prix du cartable = 60 × 2 / 5 = 24 dinars.\nReste après le cartable = 60 - 24 = 36 dinars.\n2. Prix des cahiers = 36 × 25 / 100 = 9 dinars.\nSomme finale restante = 36 - 9 = 27 dinars."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Géométrie : Champ rectangulaire",
                    points = "10 pts",
                    statement = "Un terrain rectangulaire a un périmètre de 180 m. Sa longueur dépasse sa largeur de 20 m.\n1. Calculer le demi-périmètre, la longueur et la largeur du terrain.\n2. Calculer son aire en m² puis en ares.",
                    solution = "1. Demi-périmètre = 180 ÷ 2 = 90 m.\nLargeur = (90 - 20) ÷ 2 = 35 m ; Longueur = 35 + 20 = 55 m.\n2. Aire = 55 × 35 = 1925 m² = 19,25 ares."
                )
            )
        ),
        DevoirItem(
            id = "univ_prepa_ds1_analyse",
            title = "Devoir Surveillé Prépa MP/PC — Développements Limités & Intégrales",
            subject = "Mathématiques",
            level = CycleLevel.UNIVERSITE,
            section = TunisianSection.PREPA_LICENCE,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            totalPoints = 20,
            difficulty = "Supérieur",
            sourceSchool = "IPEIT / IPEIM · Prépa Tunisie",
            chapterTags = listOf("Développements limités", "Équivalents", "Intégration par parties", "Séries numériques"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Développement limité et limite indéterminée",
                    points = "10 pts",
                    statement = "1. Donner le développement limité à l'ordre 3 au voisinage de 0 de f(x) = ln(1 + x) - sin(x).\n2. En déduire lim_{x→0} [ln(1 + x) - sin(x)] / x².",
                    solution = "1. ln(1 + x) = x - x²/2 + x³/3 + o(x³) et sin(x) = x - x³/6 + o(x³).\nPar différence : f(x) = -x²/2 + (1/3 + 1/6)x³ + o(x³) = -x²/2 + x³/2 + o(x³).\n2. f(x)/x² = -1/2 + x/2 + o(x) → -1/2 quand x → 0."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Intégrale de Wallis et convergence",
                    points = "10 pts",
                    statement = "Soit I_n = ∫₀^(π/2) sin^n(t) dt.\n1. Calculer I₀ et I₁.\n2. Par une intégration par parties, établir la relation de récurrence : n·I_n = (n - 1)·I_{n-2} pour n ≥ 2.",
                    solution = "1. I₀ = π/2 et I₁ = [-cos(t)]₀^(π/2) = 1.\n2. En posant u(t) = sin^(n-1)(t) et v'(t) = sin(t) : I_n = [-cos(t)sin^(n-1)(t)]₀^(π/2) + (n-1)∫₀^(π/2) cos²(t)sin^(n-2)(t) dt = (n-1)(I_{n-2} - I_n), d'où n·I_n = (n - 1)·I_{n-2}."
                )
            )
        ),
        DevoirItem(
            id = "college_9eme_ds1_arabe",
            title = "Devoir de Synthèse N°1 — Étude de Texte, Grammaire & Essai (العربية)",
            subject = "Arabe",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Pilote",
            sourceSchool = "Collège Pilote de Tunis",
            chapterTags = listOf("دراسة نص", "النعت والبدل", "الإنتاج الكتابي", "Concours 9ème"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Compréhension et langue (الفهم واللغة)",
                    points = "10 pts",
                    statement = "1. استخرج من النص قرينتين تدلان على قيمة العلم والاجتهاد في بناء المجتمع.\n2. اشكل الكلمات المسطّرة شكلا تاما وحدّد وظيفتها النحوية (فاعل، مفعول به، بدل مطابق).\n3. حوّل الجملة الفعلية التالية إلى جملة اسمية مفيدة للمعنى مع المحافظة على الزمن.",
                    solution = "1. تعتمد الإجابة على استخراج عبارتين دقيقتين من النص مع ربطهما بالفكرة الرئيسية (2.5 ن).\n2. التحقق من علامات الإعراب الأصلية والفرعية والتمييز بين النعت الحقيقي والبدل المطابق (4 ن).\n3. تقديم المسند إليه (المبتدأ) وإدخال الناسخ المناسب عند الحاجة مع سلامة التوافق (3.5 ن)."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Production écrite (الإنتاج الكتابي)",
                    points = "10 pts",
                    statement = "الموضوع : « يرى البعض أن المطالعة الورقية فقدت مكانتها أمام الوسائط الرقمية الحديثة. »\nحرّر مقالا حجاجيا (15 إلى 20 سطرا) تبدي فيه رأيك مدعّما بحجج متنوّعة وأمثلة من الواقع التونسي.",
                    solution = "مقاييس الإصلاح الرسمية :\n- المقدمة (التمهيد + طرح الإشكالية) : 2 ن.\n- الجوهر (بناء موقف واضح + حجتان متماسكتان + شواهد وروابط حجاجية مثل : لا شك أن، غير أن، فضلا عن ذلك) : 5 ن.\n- الخاتمة + سلامة اللغة والرسم وعلامات التنقيط : 3 ن."
                )
            )
        ),
        DevoirItem(
            id = "bac_lettres_dc1_arabe",
            title = "Devoir de Contrôle N°1 — Littérature Classique & Poésie (الأدب العربي)",
            subject = "Arabe",
            level = CycleLevel.BAC,
            section = TunisianSection.LETTRES,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 120,
            totalPoints = 20,
            difficulty = "Bac Type",
            sourceSchool = "Lycée Pilote de Kairouan",
            chapterTags = listOf("الشعر الحماسي", "أبو الطيب المتنبي", "المقال الأدبي", "البلاغة"),
            exercises = listOf(
                DevoirExercise(
                    number = 1,
                    title = "Analyse poétique et rhétorique (تحليل قصيدة)",
                    points = "10 pts",
                    statement = "انطلاقا من محور « الحماسة في الشعر العربي القديم » :\n1. بيّن مظاهر تضخيم الذات الشاعرة في شعر المتنبي من خلال الإيقاع والمعجم.\n2. استخرج صورة شعرية (تشبيه أو استعارة) واشرح وظيفتها الحجاجية والجمالية.",
                    solution = "1. يتجلّى تضخيم الذات عبر ضمير المتكلم، أفعال الإرادة والتحدي، والبحور الخليلية الفخمة كالبحر الطويل والبسيط.\n2. تحليل أركان الصورة (المشبه، المشبه به، وجه الشبه) وبيان أثرها في إبراز البطولة والتفوق المعنوي."
                ),
                DevoirExercise(
                    number = 2,
                    title = "Essai littéraire (المقال الأدبي)",
                    points = "10 pts",
                    statement = "الموضوع : « هل كانت الحماسة في شعر المتنبي مجرّد فخر فردي أم تعبيرا عن أزمة عصره وطموحه الحضاري؟ »\nاقترح تخطيطا مفصّلا وحرّر فقرة التعديل والتجاوز.",
                    solution = "التخطيط المقترح :\n- القسم الأول : الحماسة بوصفها فخرا فرديا متعاليا يمجّد الأنا الشاعرة.\n- القسم الثاني : البعد الحضاري والإنساني للحماسة بوصفها مقاومة للانحطاط السياسي والاجتماعي.\n- التجاوز : توحّد التجربة الذاتية بالهمّ الجماعي هو سرّ خلود شعر المتنبي."
                )
            )
        )
    )

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

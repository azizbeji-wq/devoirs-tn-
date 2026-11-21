package com.example.data.local

data class FlashQcmQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class FlashQcmChapter(
    val id: String,
    val chapterName: String,
    val subject: String,
    val levelLabel: String,
    val questions: List<FlashQcmQuestion>
)

/**
 * Banque de QCM Flash (5 questions vérifiées par chapitre) alignée sur les programmes tunisiens.
 */
object FlashQcmBank {

    val chapters: List<FlashQcmChapter> = listOf(
        FlashQcmChapter(
            id = "qcm_maths_complexes",
            chapterName = "Nombres complexes",
            subject = "Mathématiques",
            levelLabel = "Bac · Maths / Sc. Exp",
            questions = listOf(
                FlashQcmQuestion(
                    id = "c1",
                    prompt = "Quel est le module du nombre complexe z = 3 - 4i ?",
                    options = listOf("5", "7", "√7", "25"),
                    correctIndex = 0,
                    explanation = "|z| = √(3² + (-4)²) = √(9 + 16) = √25 = 5."
                ),
                FlashQcmQuestion(
                    id = "c2",
                    prompt = "Pour tout réel θ, que vaut e^(iπ) + 1 ?",
                    options = listOf("2", "0", "2i", "-1"),
                    correctIndex = 1,
                    explanation = "Identité d'Euler : e^(iπ) = cos(π) + i·sin(π) = -1, donc e^(iπ) + 1 = 0."
                ),
                FlashQcmQuestion(
                    id = "c3",
                    prompt = "Si z = 1 + i, quel est un argument de z ?",
                    options = listOf("π/6", "π/3", "π/4", "π/2"),
                    correctIndex = 2,
                    explanation = "|z| = √2, cos θ = √2/2 et sin θ = √2/2 ⇒ arg(z) ≡ π/4 [2π]."
                ),
                FlashQcmQuestion(
                    id = "c4",
                    prompt = "Le quotient (z - z_B) / (z - z_A) est imaginaire pur non nul si et seulement si :",
                    options = listOf(
                        "A, B, M sont alignés",
                        "(MA) ⊥ (MB) avec M ≠ A et M ≠ B",
                        "MA = MB",
                        "M est le milieu de [AB]"
                    ),
                    correctIndex = 1,
                    explanation = "Un imaginaire pur a pour argument ±π/2 [π], ce qui traduit l'orthogonalité des droites (MA) et (MB)."
                ),
                FlashQcmQuestion(
                    id = "c5",
                    prompt = "Que vaut i²⁰²⁶ ?",
                    options = listOf("1", "-1", "i", "-i"),
                    correctIndex = 1,
                    explanation = "2026 = 4 × 506 + 2, donc i²⁰²⁶ = (i⁴)⁵⁰⁶ × i² = 1 × (-1) = -1."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_maths_suites",
            chapterName = "Suites réelles",
            subject = "Mathématiques",
            levelLabel = "3ème & Bac",
            questions = listOf(
                FlashQcmQuestion(
                    id = "s1",
                    prompt = "Toute suite croissante et majorée dans ℝ est :",
                    options = listOf("Divergente vers +∞", "Convergente", "Périodique", "Constante"),
                    correctIndex = 1,
                    explanation = "Théorème de convergence monotone : toute suite croissante et majorée converge vers une limite finie."
                ),
                FlashQcmQuestion(
                    id = "s2",
                    prompt = "Soit (u_n) géométrique de raison q = 1/2 et u₀ = 8. Que vaut u₃ ?",
                    options = listOf("4", "2", "1", "1/2"),
                    correctIndex = 2,
                    explanation = "u₃ = u₀ · q³ = 8 × (1/2)³ = 8 × 1/8 = 1."
                ),
                FlashQcmQuestion(
                    id = "s3",
                    prompt = "Si (u_n) est arithmétique de raison r = -3 et u₁ = 10, que vaut u₅ ?",
                    options = listOf("-2", "1", "-5", "22"),
                    correctIndex = 0,
                    explanation = "u₅ = u₁ + (5 - 1)·r = 10 + 4 × (-3) = 10 - 12 = -2."
                ),
                FlashQcmQuestion(
                    id = "s4",
                    prompt = "Quelle est la limite quand n → +∞ de (3n² + 2) / (n² - 5) ?",
                    options = listOf("0", "+∞", "3", "-2/5"),
                    correctIndex = 2,
                    explanation = "Rapport des termes de plus haut degré : 3n² / n² = 3."
                ),
                FlashQcmQuestion(
                    id = "s5",
                    prompt = "Pour q ∈ ]-1, 1[, la limite de qⁿ quand n → +∞ vaut :",
                    options = listOf("1", "0", "+∞", "q"),
                    correctIndex = 1,
                    explanation = "Lorsque |q| < 1, la suite géométrique (qⁿ) converge toujours vers 0."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_maths_thales",
            chapterName = "Théorème de Thalès",
            subject = "Mathématiques",
            levelLabel = "Collège · 9ème",
            questions = listOf(
                FlashQcmQuestion(
                    id = "t1",
                    prompt = "Dans un triangle ABC, M ∈ [AB], N ∈ [AC] et (MN) // (BC). Si AM = 3, AB = 9 et AC = 12, que vaut AN ?",
                    options = listOf("3", "4", "6", "9"),
                    correctIndex = 1,
                    explanation = "AM/AB = AN/AC ⇒ 3/9 = AN/12 ⇒ AN = 12 × (1/3) = 4."
                ),
                FlashQcmQuestion(
                    id = "t2",
                    prompt = "Quelle condition indispensable permet d'appliquer le théorème direct de Thalès ?",
                    options = listOf(
                        "Un angle droit",
                        "Deux droites parallèles",
                        "Un triangle isocèle",
                        "Des côtés égaux"
                    ),
                    correctIndex = 1,
                    explanation = "Le théorème de Thalès exige deux droites sécantes coupées par deux droites parallèles."
                ),
                FlashQcmQuestion(
                    id = "t3",
                    prompt = "Avec AM/AB = 0,4 et BC = 10 cm et (MN) // (BC), que vaut MN ?",
                    options = listOf("2,5 cm", "4 cm", "6 cm", "14 cm"),
                    correctIndex = 1,
                    explanation = "MN/BC = AM/AB = 0,4 ⇒ MN = 0,4 × 10 = 4 cm."
                ),
                FlashQcmQuestion(
                    id = "t4",
                    prompt = "La réciproque du théorème de Thalès sert à démontrer que :",
                    options = listOf(
                        "Un triangle est rectangle",
                        "Deux droites sont parallèles",
                        "Deux angles sont supplémentaires",
                        "Un point est un milieu"
                    ),
                    correctIndex = 1,
                    explanation = "Si les rapports sont égaux et les points alignés dans le même ordre, alors les droites sont parallèles."
                ),
                FlashQcmQuestion(
                    id = "t5",
                    prompt = "Si le rapport de réduction de Thalès est k = 1/2, le rapport des aires Aire(AMN)/Aire(ABC) vaut :",
                    options = listOf("1/2", "1/4", "1/8", "2"),
                    correctIndex = 1,
                    explanation = "Les aires sont multipliées par k² = (1/2)² = 1/4."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_phys_rc",
            chapterName = "Dipôle RC",
            subject = "Physique-Chimie",
            levelLabel = "Bac Scientifique",
            questions = listOf(
                FlashQcmQuestion(
                    id = "rc1",
                    prompt = "Quelle est l'expression de la constante de temps τ d'un dipôle RC ?",
                    options = listOf("τ = R / C", "τ = C / R", "τ = R · C", "τ = 1 / (RC)"),
                    correctIndex = 2,
                    explanation = "τ = R · C s'exprime en secondes (Ω × F = s)."
                ),
                FlashQcmQuestion(
                    id = "rc2",
                    prompt = "Lors de la charge d'un condensateur sous tension E, quelle vaut u_C(t = τ) ?",
                    options = listOf("0,37 · E", "0,50 · E", "0,63 · E", "0,99 · E"),
                    correctIndex = 2,
                    explanation = "u_C(τ) = E(1 - e⁻¹) ≈ 0,632 · E."
                ),
                FlashQcmQuestion(
                    id = "rc3",
                    prompt = "Quelle est l'énergie électrique emmagasinée par un condensateur C chargé sous tension U ?",
                    options = listOf("E = C · U", "E = ½ C · U²", "E = ½ C² · U", "E = U² / (2C)"),
                    correctIndex = 1,
                    explanation = "Formule officielle : E_e = ½ · C · u_C² (en Joules)."
                ),
                FlashQcmQuestion(
                    id = "rc4",
                    prompt = "Avec R = 2 kΩ et C = 50 μF, que vaut τ ?",
                    options = listOf("10 ms", "100 ms", "25 ms", "1 s"),
                    correctIndex = 1,
                    explanation = "τ = 2000 × 50×10⁻⁶ = 0,10 s = 100 ms."
                ),
                FlashQcmQuestion(
                    id = "rc5",
                    prompt = "En régime permanent (charge terminée), l'intensité du courant i dans la branche du condensateur vaut :",
                    options = listOf("E / R", "C · E", "0 A", "E / (2R)"),
                    correctIndex = 2,
                    explanation = "Lorsque u_C = E = constante, i = C · (du_C/dt) = 0 A."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_phys_cinetique",
            chapterName = "Cinétique chimique",
            subject = "Physique-Chimie",
            levelLabel = "Bac Scientifique",
            questions = listOf(
                FlashQcmQuestion(
                    id = "cin1",
                    prompt = "Comment évolue généralement la vitesse volumique d'une réaction au cours du temps ?",
                    options = listOf(
                        "Elle augmente puis se stabilise",
                        "Elle reste constante",
                        "Elle diminue jusqu'à s'annuler",
                        "Elle double à chaque t₁/₂"
                    ),
                    correctIndex = 2,
                    explanation = "Les réactifs sont consommés, leurs concentrations diminuent donc la vitesse décroît."
                ),
                FlashQcmQuestion(
                    id = "cin2",
                    prompt = "Lequel de ces paramètres est un facteur cinétique ?",
                    options = listOf(
                        "La masse molaire du solvant",
                        "La température du milieu",
                        "La couleur du bécher",
                        "L'indice de réfraction"
                    ),
                    correctIndex = 1,
                    explanation = "Une hausse de température augmente l'agitation thermique et la fréquence des chocs efficaces."
                ),
                FlashQcmQuestion(
                    id = "cin3",
                    prompt = "Le temps de demi-réaction t₁/₂ est la durée nécessaire pour que l'avancement x atteigne :",
                    options = listOf("x_f", "x_f / 2", "2 · x_f", "x_f / 4"),
                    correctIndex = 1,
                    explanation = "Par définition : x(t₁/₂) = x_f / 2."
                ),
                FlashQcmQuestion(
                    id = "cin4",
                    prompt = "Que réalise une trempe (refroidissement brutal + dilution) lors d'un dosage cinétique ?",
                    options = listOf(
                        "Elle accélère la réaction",
                        "Elle bloque pratiquement la réaction",
                        "Elle change le réactif limitant",
                        "Elle augmente x_max"
                    ),
                    correctIndex = 1,
                    explanation = "La trempe diminue brutalement T et les concentrations pour figer l'état du système avant titrage."
                ),
                FlashQcmQuestion(
                    id = "cin5",
                    prompt = "Un catalyseur agit sur une réaction lente en :",
                    options = listOf(
                        "Modifiant le rendement final",
                        "Augmentant la vitesse sans être consommé globalement",
                        "Déplaçant l'équilibre chimique",
                        "Augmentant x_max"
                    ),
                    correctIndex = 1,
                    explanation = "Un catalyseur accélère l'atteinte de l'état final et est régénéré en fin de réaction."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_svt_genetique",
            chapterName = "Génétique humaine",
            subject = "SVT",
            levelLabel = "Bac · Sc. Exp",
            questions = listOf(
                FlashQcmQuestion(
                    id = "g1",
                    prompt = "Dans un pedigree, deux parents phénotypiquement sains ont un enfant malade. L'allèle de la maladie est :",
                    options = listOf("Dominant", "Récessif", "Codominant", "Toujours lié à Y"),
                    correctIndex = 1,
                    explanation = "L'allèle morbide existait chez les parents sans s'exprimer : il est masqué, donc récessif."
                ),
                FlashQcmQuestion(
                    id = "g2",
                    prompt = "Deux parents sains ont une fille atteinte d'une maladie récessive. Le gène est :",
                    options = listOf(
                        "Porté par Y",
                        "Lié à X récessif",
                        "Autosomal récessif",
                        "Autosomal dominant"
                    ),
                    correctIndex = 2,
                    explanation = "Si le gène était sur X, le père aurait transmis X^m et serait malade ; c'est donc autosomal récessif."
                ),
                FlashQcmQuestion(
                    id = "g3",
                    prompt = "Un père atteint d'une maladie dominante liée au chromosome X transmet la maladie à :",
                    options = listOf(
                        "Tous ses garçons uniquement",
                        "Toutes ses filles uniquement",
                        "50 % de ses garçons",
                        "Aucun de ses enfants"
                    ),
                    correctIndex = 1,
                    explanation = "Le père transmet son chromosome X à toutes ses filles et son chromosome Y à ses garçons."
                ),
                FlashQcmQuestion(
                    id = "g4",
                    prompt = "Chez deux parents hétérozygotes (N//m) pour une maladie autosomale récessive, le risque d'avoir un enfant atteint est :",
                    options = listOf("1/2", "1/4", "3/4", "1/8"),
                    correctIndex = 1,
                    explanation = "Échiquier de croisement : 1/4 N//N, 2/4 N//m (sains), 1/4 m//m (atteint)."
                ),
                FlashQcmQuestion(
                    id = "g5",
                    prompt = "Laquelle de ces techniques permet un diagnostic prénatal chromosomique ?",
                    options = listOf("Électrocardiogramme", "Amniocentèse + caryotype", "Radiographie pulmonaire", "Antibiogramme"),
                    correctIndex = 1,
                    explanation = "L'amniocentèse prélève des cellules fœtales pour établir le caryotype ou analyser l'ADN."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_info_python",
            chapterName = "Python & Récursivité",
            subject = "Informatique",
            levelLabel = "2ème · 3ème · Bac Info",
            questions = listOf(
                FlashQcmQuestion(
                    id = "p1",
                    prompt = "Que doit impérativement contenir une fonction récursive pour ne pas boucler à l'infini ?",
                    options = listOf("Une boucle while", "Une condition d'arrêt (cas de base)", "Un fichier texte", "Une variable globale"),
                    correctIndex = 1,
                    explanation = "Sans cas de base atteint en un nombre fini d'appels, Python lève RecursionError."
                ),
                FlashQcmQuestion(
                    id = "p2",
                    prompt = "En Python, que retourne l'expression `17 // 5` et `17 % 5` ?",
                    options = listOf("3 et 2", "3.4 et 2", "2 et 3", "4 et 1"),
                    correctIndex = 0,
                    explanation = "17 = 5 × 3 + 2 : quotient entier `//` = 3, reste `%` = 2."
                ),
                FlashQcmQuestion(
                    id = "p3",
                    prompt = "Dans un tableau trié par insertion, quelle est la complexité au pire cas ?",
                    options = listOf("O(1)", "O(log n)", "O(n)", "O(n²)"),
                    correctIndex = 3,
                    explanation = "Si le tableau est trié en ordre inverse, le tri par insertion effectue n(n-1)/2 comparaisons, soit O(n²)."
                ),
                FlashQcmQuestion(
                    id = "p4",
                    prompt = "En SQL, quelle clause filtre les groupes après un `GROUP BY` ?",
                    options = listOf("WHERE", "ORDER BY", "HAVING", "DISTINCT"),
                    correctIndex = 2,
                    explanation = "`WHERE` filtre les lignes avant regroupement ; `HAVING` filtre les résultats agrégés après `GROUP BY`."
                ),
                FlashQcmQuestion(
                    id = "p5",
                    prompt = "Que retourne `'tunis'[::-1]` en Python ?",
                    options = listOf("'tunis'", "'sinut'", "'tuni'", "Erreur"),
                    correctIndex = 1,
                    explanation = "Le découpage avec pas -1 inverse la chaîne : `'sinut'`."
                )
            )
        ),
        FlashQcmChapter(
            id = "qcm_eco_pib",
            chapterName = "PIB & Croissance",
            subject = "Économie & Gestion",
            levelLabel = "3ème & Bac Éco",
            questions = listOf(
                FlashQcmQuestion(
                    id = "e1",
                    prompt = "Comment passe-t-on du PIB nominal au PIB réel (aux prix de l'année de base) ?",
                    options = listOf(
                        "PIB nominal × Indice des prix",
                        "(PIB nominal / Indice des prix) × 100",
                        "PIB nominal + Inflation",
                        "PIB nominal - Exportations"
                    ),
                    correctIndex = 1,
                    explanation = "Déflater le PIB nominal par l'indice des prix élimine l'effet de l'inflation."
                ),
                FlashQcmQuestion(
                    id = "e2",
                    prompt = "Le PIB est égal à la somme :",
                    options = listOf(
                        "Des chiffres d'affaires",
                        "Des valeurs ajoutées + impôts sur produits - subventions",
                        "Des salaires uniquement",
                        "Des exportations nettes"
                    ),
                    correctIndex = 1,
                    explanation = "Additionner les valeurs ajoutées évite de compter plusieurs fois les consommations intermédiaires."
                ),
                FlashQcmQuestion(
                    id = "e3",
                    prompt = "Si la demande est Qd = 100 - 2P et l'offre Qo = 20 + 2P, quel est le prix d'équilibre P* ?",
                    options = listOf("10 DT", "20 DT", "25 DT", "40 DT"),
                    correctIndex = 1,
                    explanation = "100 - 2P = 20 + 2P ⇒ 80 = 4P ⇒ P* = 20 DT."
                ),
                FlashQcmQuestion(
                    id = "e4",
                    prompt = "En gestion (Système Comptable Tunisien), le Fonds de Roulement (FR) vaut :",
                    options = listOf(
                        "Actifs non courants - Passifs courants",
                        "Ressources stables - Actifs non courants",
                        "Banque + Caisse",
                        "Capitaux propres - Dettes"
                    ),
                    correctIndex = 1,
                    explanation = "FR = (Capitaux propres + Passifs non courants) - Actifs non courants."
                ),
                FlashQcmQuestion(
                    id = "e5",
                    prompt = "Une croissance économique obtenue principalement grâce au progrès technique et à la productivité est dite :",
                    options = listOf("Extensive", "Intensive", "Nulle", "Déflationniste"),
                    correctIndex = 1,
                    explanation = "La croissance intensive repose sur les gains de productivité globale des facteurs (PGF)."
                )
            )
        )
    )

    fun findByChapterName(chapterName: String): FlashQcmChapter? {
        val normalized = chapterName.trim().lowercase()
        return chapters.firstOrNull { chapter ->
            chapter.chapterName.lowercase() == normalized ||
                chapter.chapterName.lowercase().contains(normalized) ||
                normalized.contains(chapter.chapterName.lowercase())
        }
    }
}

package com.example.data.model

enum class CycleLevel(
    val id: String,
    val label: String,
    val shortLabel: String,
    val cycle: String
) {
    ALL("ALL", "Tous niveaux", "Tous", "Tous"),
    PRIMAIRE_6("PRIMAIRE_6", "6ème Primaire (Concours)", "6ème", "Primaire"),
    COLLEGE_7("COLLEGE_7", "7ème Année de Base", "7ème", "Collège"),
    COLLEGE_8("COLLEGE_8", "8ème Année de Base", "8ème", "Collège"),
    COLLEGE_9("COLLEGE_9", "9ème Base & Pilote", "9ème", "Collège"),
    LYCEE_1("LYCEE_1", "1ère Secondaire", "1ère", "Lycée"),
    LYCEE_2("LYCEE_2", "2ème Secondaire", "2ème", "Lycée"),
    LYCEE_3("LYCEE_3", "3ème Secondaire", "3ème", "Lycée"),
    BAC("BAC", "Baccalauréat (4ème)", "Bac", "Lycée"),
    UNIVERSITE("UNIVERSITE", "Université & Prépa", "Univ / Prépa", "Supérieur");

    companion object {
        fun fromId(id: String): CycleLevel = entries.find { it.id == id } ?: ALL
    }
}

enum class TunisianSection(
    val id: String,
    val label: String,
    val shortLabel: String
) {
    ALL("ALL", "Toutes sections", "Toutes"),
    TRONC_COMMUN("TRONC_COMMUN", "Tronc Commun", "Commun"),
    MATHS("MATHS", "Mathématiques", "Maths"),
    SCIENCES_EXP("SCIENCES_EXP", "Sciences Expérimentales", "Sc. Exp"),
    INFORMATIQUE("INFORMATIQUE", "Sciences de l'Informatique", "Info"),
    ECO_GESTION("ECO_GESTION", "Économie & Gestion", "Éco-Gest"),
    TECHNIQUE("TECHNIQUE", "Sciences Techniques", "Technique"),
    LETTRES("LETTRES", "Lettres", "Lettres"),
    PREPA_LICENCE("PREPA_LICENCE", "Prépa / Licence", "Supérieur");

    companion object {
        fun fromId(id: String): TunisianSection = entries.find { it.id == id } ?: TRONC_COMMUN
    }
}

enum class DevoirType(
    val id: String,
    val label: String,
    val badge: String
) {
    ALL("ALL", "Tous types", "Tous"),
    CONTROLE("CONTROLE", "Devoir de Contrôle", "DC"),
    SYNTHESE("SYNTHESE", "Devoir de Synthèse", "DS"),
    EXAMEN("EXAMEN", "Sujet Bac / Concours", "Examen");

    companion object {
        fun fromId(id: String): DevoirType = entries.find { it.id == id } ?: CONTROLE
    }
}

enum class Trimester(
    val id: String,
    val label: String,
    val shortLabel: String
) {
    ALL("ALL", "Tous trimestres", "Tous"),
    T1("T1", "Trimestre 1", "T1"),
    T2("T2", "Trimestre 2", "T2"),
    T3("T3", "Trimestre 3", "T3"),
    NATIONAL("NATIONAL", "Session Nationale", "National");

    companion object {
        fun fromId(id: String): Trimester = entries.find { it.id == id } ?: T1
    }
}

data class DevoirExercise(
    val number: Int,
    val title: String,
    val points: String,
    val statement: String,
    val solution: String
)

data class DevoirItem(
    val id: String,
    val title: String,
    val subject: String,
    val level: CycleLevel,
    val section: TunisianSection,
    val type: DevoirType,
    val trimester: Trimester,
    val durationMinutes: Int,
    val totalPoints: Int,
    val difficulty: String,
    val sourceSchool: String,
    val chapterTags: List<String>,
    val exercises: List<DevoirExercise>,
    val isFavorite: Boolean = false,
    val isCompleted: Boolean = false
)

data class SubjectCoefficient(
    val id: String,
    val name: String,
    val coefficient: Double,
    val hasTwoControlExams: Boolean = false
)

data class SectionCoefficientPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val subjects: List<SubjectCoefficient>
)

data class StudyMethodGuide(
    val id: String,
    val title: String,
    val category: String,
    val durationTag: String,
    val summary: String,
    val steps: List<String>,
    val tunisianTip: String
)

data class AppFeatureIdea(
    val id: String,
    val title: String,
    val category: String,
    val targetAudience: String,
    val impactLabel: String,
    val description: String,
    val concreteExample: String
)

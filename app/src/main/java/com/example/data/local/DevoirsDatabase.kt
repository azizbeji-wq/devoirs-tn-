package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DevoirEntity::class,
        StarredIdeaEntity::class,
        TutorMessageEntity::class,
        ExamAttemptEntity::class,
        ExamReminderEntity::class,
        QcmResultEntity::class,
        SubjectEntity::class,
        DevoirPdfContentFtsEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class DevoirsDatabase : RoomDatabase() {
    abstract fun devoirDao(): DevoirDao
    abstract fun starredIdeaDao(): StarredIdeaDao
    abstract fun tutorMessageDao(): TutorMessageDao
    abstract fun examAttemptDao(): ExamAttemptDao
    abstract fun examReminderDao(): ExamReminderDao
    abstract fun qcmResultDao(): QcmResultDao
    abstract fun subjectDao(): SubjectDao
    abstract fun devoirPdfContentDao(): DevoirPdfContentDao

    companion object {
        @Volatile
        private var INSTANCE: DevoirsDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tutor_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `devoirId` TEXT NOT NULL,
                        `exerciseNumber` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_tutor_messages_devoir_ex`
                    ON `tutor_messages` (`devoirId`, `exerciseNumber`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `devoirs` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'fr'"
                )
                db.execSQL(
                    "ALTER TABLE `devoirs` ADD COLUMN `isTrainingSubject` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `devoirs` ADD COLUMN `contentVersion` INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `devoirId` TEXT NOT NULL,
                        `devoirTitle` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `chapterTagsSerialized` TEXT NOT NULL,
                        `attemptedAtMillis` INTEGER NOT NULL,
                        `officialDurationSeconds` INTEGER NOT NULL,
                        `actualDurationSeconds` INTEGER NOT NULL,
                        `exerciseScoresSerialized` TEXT NOT NULL,
                        `totalScore` REAL NOT NULL,
                        `maxScore` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_attempts_devoirId` ON `exam_attempts` (`devoirId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_attempts_subject` ON `exam_attempts` (`subject`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_attempts_attemptedAtMillis` ON `exam_attempts` (`attemptedAtMillis`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `examType` TEXT NOT NULL,
                        `examDateMillis` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_reminders_examDateMillis` ON `exam_reminders` (`examDateMillis`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qcm_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `chapterId` TEXT NOT NULL,
                        `chapterName` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `completedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_qcm_results_chapterId` ON `qcm_results` (`chapterId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_qcm_results_completedAtMillis` ON `qcm_results` (`completedAtMillis`)"
                )
            }
        }

        // Migration vers la version 5 : création de la table `subjects` pour les matières scolaires
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `id` TEXT NOT NULL,
                        `canonicalName` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration vers la version 6 : passage des devoirs au format PDF (sujetPdf / corrigePdf)
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `devoirs_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `levelId` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `typeId` TEXT NOT NULL,
                        `trimesterId` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `chapterTagsSerialized` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `sizeMb` REAL NOT NULL,
                        `sujetPdf` TEXT NOT NULL,
                        `corrigePdf` TEXT,
                        `isFavorite` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `devoirs_new` (
                        `id`, `title`, `subject`, `levelId`, `sectionId`, `typeId`, `trimesterId`,
                        `durationMinutes`, `difficulty`, `chapterTagsSerialized`, `source`,
                        `version`, `sizeMb`, `sujetPdf`, `corrigePdf`, `isFavorite`, `isCompleted`
                    )
                    SELECT
                        `id`, `title`, `subject`, `levelId`, `sectionId`, `typeId`, `trimesterId`,
                        `durationMinutes`, `difficulty`, `chapterTagsSerialized`, `sourceSchool`,
                        0, 0.0, '', NULL, `isFavorite`, `isCompleted`
                    FROM `devoirs`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `devoirs`")
                db.execSQL("ALTER TABLE `devoirs_new` RENAME TO `devoirs`")
            }
        }

        // Migration vers la version 7 : PDF hébergés en ligne (sujetUrl / corrigeUrl),
        // vérification d'intégrité (taille / SHA-256) et stockage privé hors ligne.
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `devoirs_v7` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `levelId` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `typeId` TEXT NOT NULL,
                        `trimesterId` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `chapterTagsSerialized` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `sizeMb` REAL NOT NULL,
                        `sujetUrl` TEXT NOT NULL,
                        `corrigeUrl` TEXT,
                        `sujetSizeBytes` INTEGER NOT NULL,
                        `corrigeSizeBytes` INTEGER NOT NULL,
                        `sujetSha256` TEXT,
                        `corrigeSha256` TEXT,
                        `localSujetPath` TEXT,
                        `localCorrigePath` TEXT,
                        `downloadedBytes` INTEGER NOT NULL,
                        `downloadedVersion` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `devoirs_v7` (
                        `id`, `title`, `subject`, `levelId`, `sectionId`, `typeId`, `trimesterId`,
                        `durationMinutes`, `difficulty`, `chapterTagsSerialized`, `source`,
                        `version`, `sizeMb`, `sujetUrl`, `corrigeUrl`,
                        `sujetSizeBytes`, `corrigeSizeBytes`, `sujetSha256`, `corrigeSha256`,
                        `localSujetPath`, `localCorrigePath`, `downloadedBytes`, `downloadedVersion`,
                        `isFavorite`, `isCompleted`
                    )
                    SELECT
                        `id`, `title`, `subject`, `levelId`, `sectionId`, `typeId`, `trimesterId`,
                        `durationMinutes`, `difficulty`, `chapterTagsSerialized`, `source`,
                        0, `sizeMb`, '', NULL,
                        0, 0, NULL, NULL,
                        NULL, NULL, 0, 0,
                        `isFavorite`, `isCompleted`
                    FROM `devoirs`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `devoirs`")
                db.execSQL("ALTER TABLE `devoirs_v7` RENAME TO `devoirs`")
            }
        }

        // Migration vers la version 8 : ajout de la table virtuelle FTS4 pour l'index OCR plein texte des PDF
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `devoir_pdf_contents_fts` USING FTS4(
                        `devoirId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `chapters` TEXT NOT NULL,
                        `extractedText` TEXT NOT NULL,
                        `ocrSource` TEXT NOT NULL,
                        `indexedAtMillis` INTEGER NOT NULL,
                        tokenize=unicode61
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration vers la version 9 : ajout de `exercisesSerialized` pour unifier les anciens sujets JSON
        // consultables en texte avec le catalogue PDF principal sans effacer favoris ni progression.
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `devoirs` ADD COLUMN `exercisesSerialized` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): DevoirsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevoirsDatabase::class.java,
                    "devoirs_tn_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

const { describe, it, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

let testEnv;

function validUserProfile(userId, now) {
  return {
    userId,
    displayName: "Amine Ben Ali",
    weeklyGoalTarget: 5,
    favoriteDevoirIds: ["math_bac_t1_01"],
    completedDevoirIds: ["phys_bac_t1_01"],
    starredMethodIds: ["methode_ds_maths"],
    createdAt: now,
    updatedAt: now,
  };
}

function validExamAttempt(userId, now) {
  return {
    userId,
    devoirId: "math_bac_t1_01",
    devoirTitle: "Devoir de Synthèse N°1 — Analyse",
    subject: "Mathématiques",
    totalScore: 16.5,
    maxScore: 20,
    actualDurationSeconds: 5400,
    attemptedAtMillis: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
}

function validReminder(userId, now) {
  return {
    userId,
    title: "DS Physique — Dipôle RC",
    subject: "Physique-Chimie",
    examType: "DS",
    examDateMillis: Date.now() + 86400000,
    createdAt: now,
    updatedAt: now,
  };
}

function validSubject(userId, now) {
  return {
    userId,
    name: "Options Allemand",
    subtitle: "Compréhension et expression écrite",
    createdAt: now,
    updatedAt: now,
  };
}

describe("Devoirs TN Firestore Security Rules", () => {
  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: "127.0.0.1",
        port: 8085,
        rules: fs.readFileSync("firestore.rules", "utf8"),
      },
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  it("denies all unauthenticated reads and writes across all collections", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    const now = new Date(Date.now() - 1000);

    await assertFails(anonDb.collection("users").doc(ALICE_UID).get());
    await assertFails(anonDb.collection("users").get());
    await assertFails(
      anonDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, now))
    );
    await assertFails(
      anonDb.collection("users").doc(ALICE_UID).collection("examAttempts").get()
    );
    await assertFails(
      anonDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("reminders")
        .doc("rem_1")
        .set(validReminder(ALICE_UID, now))
    );
    await assertFails(
      anonDb.collection("users").doc(ALICE_UID).collection("subjects").get()
    );
  });

  it("allows owner CRUD on /users/{userId} while blocking list and cross-user access", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    const createdTime = new Date(Date.now() - 2000);
    const updatedTime = new Date(Date.now() - 500);

    // Owner create & get
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .set(validUserProfile(ALICE_UID, createdTime))
    );
    await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

    // Enumeration of /users is always denied
    await assertFails(aliceDb.collection("users").get());

    // Owner partial update matching CloudSyncRepository.updateUserStudyProfile
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).update({
        displayName: "Amine B.",
        weeklyGoalTarget: 7,
        favoriteDevoirIds: ["math_bac_t1_01", "svt_bac_t1_02"],
        completedDevoirIds: ["phys_bac_t1_01"],
        starredMethodIds: ["methode_ds_maths"],
        updatedAt: updatedTime,
      })
    );

    // Cross-user isolation
    await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
    await assertFails(
      bobDb.collection("users").doc(ALICE_UID).update({
        weeklyGoalTarget: 1,
        updatedAt: updatedTime,
      })
    );
    await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

    // Owner delete
    await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
  });

  it("rejects extra/shadow keys, invalid types, oversized bounds, future timestamps, and immutable field mutations on /users/{userId}", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const now = new Date(Date.now() - 2000);
    const future = new Date(Date.now() + 60000);

    // Reject extra 'id' field (must use @DocumentId, never persisted)
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).set({
        ...validUserProfile(ALICE_UID, now),
        id: ALICE_UID,
      })
    );

    // Reject out-of-range weeklyGoalTarget
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).set({
        ...validUserProfile(ALICE_UID, now),
        weeklyGoalTarget: 25,
      })
    );

    // Reject oversized displayName (> 120 chars)
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).set({
        ...validUserProfile(ALICE_UID, now),
        displayName: "A".repeat(121),
      })
    );

    // Reject future timestamp
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).set({
        ...validUserProfile(ALICE_UID, now),
        createdAt: future,
      })
    );

    // Create valid doc first
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, now))
    );

    // Reject mutating immutable createdAt
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
    );

    // Reject mutating immutable userId
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        userId: BOB_UID,
        updatedAt: new Date(Date.now() - 500),
      })
    );
  });

  it("enforces ownership, list queries, validation, and isolation on examAttempts subcollection", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    const now = new Date(Date.now() - 2000);

    const attemptRef = aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("examAttempts")
      .doc("attempt_math_1");

    await assertSucceeds(attemptRef.set(validExamAttempt(ALICE_UID, now)));
    await assertSucceeds(attemptRef.get());
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("examAttempts")
        .orderBy("createdAt", "desc")
        .get()
    );

    // Cross-user list and read denied
    await assertFails(
      bobDb.collection("users").doc(ALICE_UID).collection("examAttempts").get()
    );
    await assertFails(
      bobDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("examAttempts")
        .doc("attempt_math_1")
        .get()
    );

    // Invalid document ID (contains space / dot)
    await assertFails(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("examAttempts")
        .doc("bad.id space")
        .set(validExamAttempt(ALICE_UID, now))
    );

    // Spoofed userId inside payload rejected
    await assertFails(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("examAttempts")
        .doc("attempt_spoof")
        .set(validExamAttempt(BOB_UID, now))
    );

    // Owner delete succeeds
    await assertSucceeds(attemptRef.delete());
  });

  it("enforces ownership, list queries, validation, and isolation on reminders and subjects subcollections", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    const now = new Date(Date.now() - 2000);

    // Reminders
    const reminderRef = aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_phys_1");

    await assertSucceeds(reminderRef.set(validReminder(ALICE_UID, now)));
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("reminders")
        .orderBy("examDateMillis", "asc")
        .get()
    );
    await assertFails(
      bobDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("reminders")
        .doc("rem_phys_1")
        .delete()
    );
    await assertSucceeds(reminderRef.delete());

    // Subjects
    const subjectRef = aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subjects")
      .doc("subj_german_1");

    await assertSucceeds(subjectRef.set(validSubject(ALICE_UID, now)));
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("subjects")
        .orderBy("createdAt", "asc")
        .get()
    );
    await assertFails(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("subjects")
        .doc("subj_invalid")
        .set({
          ...validSubject(ALICE_UID, now),
          name: "",
        })
    );
    await assertFails(
      bobDb.collection("users").doc(ALICE_UID).collection("subjects").get()
    );
    await assertSucceeds(subjectRef.delete());
  });
});

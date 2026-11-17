const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_slife";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const SPOOF_UID = "spoof_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user: cannot read or write orgs or subcollections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("orgs").doc("wahidiya_main").get());
  await assertFails(unauth.collection("orgs/wahidiya_main/staff").get());
  await assertFails(
    unauth.collection("orgs/wahidiya_main/attendance").doc("att_2026-11-17_01").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      date: "2026-11-17",
      code: "01",
      name: "Test",
      branch: "Main",
      inTime: "09:30",
      outTime: "",
      inDist: "10",
      outDist: "",
      source: "Mobile",
      note: "",
      upd: "now",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email spoofing test: unverified slife626@gmail.com cannot access wahidiya_main", async () => {
  const spoofDb = testEnv.authenticatedContext(SPOOF_UID, {
    email: "slife626@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(spoofDb.collection("orgs").doc("wahidiya_main").get());
  await assertFails(spoofDb.collection("orgs/wahidiya_main/staff").get());
});

test("Bootstrapped admin (verified email) can create, update, list, and delete in wahidiya_main", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "slife626@gmail.com",
    email_verified: true,
  }).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    adminDb.collection("orgs").doc("wahidiya_main").set({
      ownerId: ADMIN_UID,
      memberUids: [ADMIN_UID],
      orgName: "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("orgs/wahidiya_main/staff").doc("staff_01").set({
      ownerId: ADMIN_UID,
      memberUids: [ADMIN_UID, BOB_UID],
      code: "01",
      name: "রহিম",
      designation: "বিক্রয় কর্মী",
      branch: "হেড অফিস",
      shift: "সাধারণ",
      pin: "1234",
      status: "Active",
      weeklyOff: "",
      joinDate: "2025-01-01",
      leaveDate: "",
      reservedDays: "12",
      probationMonths: "6",
      faceJson: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(adminDb.collection("orgs/wahidiya_main/staff").get());
});

test("User isolation & member array-contains query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Alice owns her personal workspace orgs/${ALICE_UID}
  await assertSucceeds(
    aliceDb.collection("orgs").doc(ALICE_UID).set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      orgName: "Alice Org",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`orgs/${ALICE_UID}/attendance`).doc("att_2026-11-17_01").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      date: "2026-11-17",
      code: "01",
      name: "Alice Staff",
      branch: "Main",
      inTime: "09:30",
      outTime: "",
      inDist: "12",
      outDist: "",
      source: "Mobile",
      note: "",
      upd: "2026-11-17T03:30:00Z",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's workspace (get doc, unfiltered list, or date-filtered list)
  await assertFails(bobDb.collection(`orgs/${ALICE_UID}/attendance`).doc("att_2026-11-17_01").get());
  await assertFails(bobDb.collection(`orgs/${ALICE_UID}/attendance`).get());
  await assertFails(bobDb.collection(`orgs/${ALICE_UID}/attendance`).where("date", "==", "2026-11-17").get());
  const bobEmptyRes = await assertSucceeds(
    bobDb.collection(`orgs/${ALICE_UID}/attendance`).where("memberUids", "array-contains", BOB_UID).get()
  );
  if (!bobEmptyRes.empty) {
    throw new Error("Expected Bob array-contains query on Alice workspace to return 0 docs");
  }

  // Seed shared doc (b_1) and admin-only doc (b_admin_only) inside wahidiya_main
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orgs/wahidiya_main/branches").doc("b_1").set({
      ownerId: ADMIN_UID,
      memberUids: [ADMIN_UID, BOB_UID],
      branch: "চকবাজার",
      lat: "22.35",
      lng: "91.83",
      radius: "150",
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("orgs/wahidiya_main/branches").doc("b_admin_only").set({
      ownerId: ADMIN_UID,
      memberUids: [ADMIN_UID],
      branch: "হেড অফিস",
      lat: "22.35",
      lng: "91.83",
      radius: "150",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Filtered query by Bob succeeds; unfiltered query by Bob fails
  await assertSucceeds(
    bobDb.collection("orgs/wahidiya_main/branches").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("orgs/wahidiya_main/branches").get());
});

test("Hardening checks: shadow update, future timestamp, @DocumentId id key, and immutable createdAt rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const futureTs = new Date(Date.now() + 60000);

  // Reject ghost field
  await assertFails(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_ghost").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      branch: "Test",
      lat: "22.0",
      lng: "91.0",
      radius: "150",
      isVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_future").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      branch: "Test",
      lat: "22.0",
      lng: "91.0",
      radius: "150",
      createdAt: futureTs,
      updatedAt: futureTs,
    })
  );

  // Reject @DocumentId "id" key in body
  await assertFails(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_id").set({
      id: "b_id",
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      branch: "Test",
      lat: "22.0",
      lng: "91.0",
      radius: "150",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid create + valid partial update + reject immutable createdAt edit
  await assertSucceeds(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_ok").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      branch: "Main",
      lat: "22.35",
      lng: "91.83",
      radius: "150",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_ok").update({
      radius: "200",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection(`orgs/${ALICE_UID}/branches`).doc("b_ok").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

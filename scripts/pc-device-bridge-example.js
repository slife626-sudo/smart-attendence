/**
 * Local PC / Attendance Device -> Firebase Firestore Cloud Bridge Example
 *
 * Allows a local computer connected to a biometric / ZKTeco / card attendance device
 * to push live IN/OUT punches directly into the same Cloud Firestore database used
 * by the Android app. Every connected mobile phone updates automatically in real time.
 *
 * Prerequisites on your local PC:
 *   npm install firebase-admin
 *   Download a Firebase Service Account JSON key from Firebase Console ->
 *   Project Settings -> Service Accounts -> Generate new private key -> save as serviceAccountKey.json
 */

const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const serviceAccount = require("./serviceAccountKey.json");

const DATABASE_ID = "ai-studio-android-attendan-c7921a1d-711f-4edf-b1b9-651fd7c7b48d";
const ORG_ID = "wahidiya_main"; // Match active Workspace ID in Android App Admin Settings

const app = initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore(app, DATABASE_ID);

function safeIdPart(raw) {
  return String(raw || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]/g, "_");
}

/**
 * Push a device punch (IN or OUT) into Firestore:
 * Path: orgs/<ORG_ID>/attendance/att_<date>_<code>
 */
async function pushDevicePunch({ code, date, timeHHMM, type, deviceName = "PC Device" }) {
  const orgRef = db.collection("orgs").doc(ORG_ID);
  const orgSnap = await orgRef.get();
  const orgData = orgSnap.exists ? orgSnap.data() : {};
  const ownerId = orgData.ownerId || "pc_device_bridge";
  const memberUids = Array.isArray(orgData.memberUids) ? orgData.memberUids : [];

  const staffSnap = await orgRef.collection("staff").doc(`stf_${safeIdPart(code)}`).get();
  const staff = staffSnap.exists ? staffSnap.data() : {};

  const docId = `att_${safeIdPart(date)}_${safeIdPart(code)}`;
  const attRef = orgRef.collection("attendance").doc(docId);
  const attSnap = await attRef.get();

  const punchTime = String(timeHHMM).slice(0, 5); // "HH:MM" 24h
  const nowStamp = new Date().toISOString().slice(0, 19);

  if (!attSnap.exists) {
    await attRef.set({
      ownerId,
      memberUids,
      date,
      code: String(code),
      name: staff.name || "",
      branch: staff.branch || "",
      inTime: type === "IN" ? punchTime : "",
      outTime: type === "OUT" ? punchTime : "",
      inDist: type === "IN" ? "Device" : "",
      outDist: type === "OUT" ? "Device" : "",
      source: deviceName,
      note: "",
      upd: nowStamp,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  } else {
    const patch = {
      memberUids,
      source: deviceName,
      upd: nowStamp,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (type === "IN") {
      patch.inTime = punchTime;
      patch.inDist = "Device";
    } else if (type === "OUT") {
      patch.outTime = punchTime;
      patch.outDist = "Device";
    }
    await attRef.update(patch);
  }

  console.log(`[Firestore Sync] ${type} punch synced for ${code} at ${date} ${punchTime}`);
}

// Example usage:
if (require.main === module) {
  pushDevicePunch({
    code: "01",
    date: new Date().toISOString().slice(0, 10),
    timeHHMM: "09:28",
    type: "IN",
    deviceName: "Biometric PC Bridge",
  }).catch((err) => {
    console.error("Failed to sync device punch:", err);
    process.exit(1);
  });
}

module.exports = { pushDevicePunch };

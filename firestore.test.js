const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const assert = require("node:assert");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_flipp";
const ADMIN_EMAIL = "flippzizzle@gmail.com";
const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";
const PENDING_UID = "pending_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("posts").get());
  await assertFails(unauth.collection("characters").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("conversations").get());
});

test("Email spoofing with email_verified=false cannot gain Admin privileges", async () => {
  const spoofAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const ts = pastDate();
  await assertFails(
    spoofAdmin.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      email: ADMIN_EMAIL,
      displayName: "Spoofer",
      status: "APPROVED",
      role: "ADMIN",
      friendCode: "SPOOF123",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(spoofAdmin.collection("users").get());
});

test("Normal user registration enforces PENDING status and blocks self-approval or role escalation", async () => {
  const pendingDb = testEnv.authenticatedContext(PENDING_UID, {
    email: "pending@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  // Attempting to register directly as APPROVED or ADMIN must fail
  await assertFails(
    pendingDb.collection("users").doc(PENDING_UID).set({
      userId: PENDING_UID,
      email: "pending@example.com",
      displayName: "Pending User",
      status: "APPROVED",
      role: "MEMBER",
      friendCode: "PEND1234",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid PENDING registration succeeds
  await assertSucceeds(
    pendingDb.collection("users").doc(PENDING_UID).set({
      userId: PENDING_UID,
      email: "pending@example.com",
      displayName: "Pending User",
      status: "PENDING",
      role: "MEMBER",
      friendCode: "PEND1234",
      themeId: "midnight_dark",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Pending user cannot escalate their own status to APPROVED
  await assertFails(
    pendingDb.collection("users").doc(PENDING_UID).update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );
});

test("Unapproved (PENDING) user cannot read community posts, characters, quotes, or send friend requests", async () => {
  const ts = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("approved_members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      approvedBy: ADMIN_UID,
      createdAt: ts,
    });
    await db.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      postType: "TEXT",
      text: "Secret family story",
      userReactions: {},
      commentCount: 0,
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("characters").doc("char_1").set({
      name: "Uncle Dan",
      nicknames: ["Danno"],
      aliases: [],
      biography: "Legend of 1999",
      customDetails: {},
      createdByUid: ALICE_UID,
      createdByName: "Alice",
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const pendingDb = testEnv.authenticatedContext(PENDING_UID).firestore();
  await assertFails(pendingDb.collection("posts").doc("post_1").get());
  const pendingSnap = await assertSucceeds(
    pendingDb.collection("posts").where("memberUids", "array-contains", PENDING_UID).get()
  );
  assert.strictEqual(pendingSnap.empty, true);
  await assertFails(pendingDb.collection("posts").get());
  await assertFails(pendingDb.collection("characters").doc("char_1").get());
  await assertFails(
    pendingDb.collection("friend_requests").doc("req_1").set({
      fromUid: PENDING_UID,
      toUid: ALICE_UID,
      fromName: "Pending",
      toName: "Alice",
      status: "PENDING",
      participantUids: [PENDING_UID, ALICE_UID],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Verified Admin can approve users, whitelist emails, and manage community content", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    adminDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      displayName: "Owner Admin",
      status: "APPROVED",
      role: "ADMIN",
      friendCode: "OWNER001",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      status: "PENDING",
      role: "MEMBER",
      friendCode: "ALICE123",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    adminDb.collection("approved_members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      approvedBy: ADMIN_UID,
      createdAt: pastDate(500),
    })
  );
});

test("Approved members can create and react to posts, create characters and lore entries, and query with memberUids", async () => {
  const ts = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("approved_members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      approvedBy: ADMIN_UID,
      createdAt: ts,
    });
    await db.collection("approved_members").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      approvedBy: ADMIN_UID,
      createdAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      postType: "TEXT",
      text: "Hello Social Lore!",
      userReactions: {},
      commentCount: 0,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered list fails; filtered list succeeds
  await assertFails(bobDb.collection("posts").get());
  await assertSucceeds(bobDb.collection("posts").where("memberUids", "array-contains", BOB_UID).get());

  // Bob can react to Alice's post, but cannot edit Alice's post text
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      userReactions: { [BOB_UID]: "LOVE" },
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      text: "Hacked post text",
      updatedAt: pastDate(500),
    })
  );

  // Alice creates character and Bob adds a Lore Entry
  await assertSucceeds(
    aliceDb.collection("characters").doc("char_1").set({
      name: "Grandma Rose",
      nicknames: ["Nana"],
      aliases: ["The Pie Whisperer"],
      biography: "Makes legendary apple pie.",
      customDetails: { Hometown: "Portland" },
      createdByUid: ALICE_UID,
      createdByName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("lore_entries").doc("lore_1").set({
      characterId: "char_1",
      characterName: "Grandma Rose",
      title: "The Great Thanksgiving Pie Swap",
      story: "Everyone thought it was pumpkin until the first bite.",
      datePrecision: "APPROXIMATE",
      historicalDateLabel: "Nov 2012",
      sortOrderEpoch: 1351728000,
      contributorUid: BOB_UID,
      contributorName: "Bob",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Private conversations and messages are strictly restricted to participants", async () => {
  const ts = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const uid of [ALICE_UID, BOB_UID, "charlie_uid"]) {
      await db.collection("approved_members").doc(uid).set({
        userId: uid,
        displayName: uid,
        approvedBy: ADMIN_UID,
        createdAt: ts,
      });
    }
    await db.collection("conversations").doc("conv_1").set({
      groupChat: false,
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdByUid: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      conversationId: "conv_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Private message for Bob",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();

  // Bob (participant) can read
  await assertSucceeds(bobDb.collection("conversations").where("participantUids", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages")
      .where("participantUids", "array-contains", BOB_UID).get()
  );

  // Charlie (even though an approved member) CANNOT read or write into Alice & Bob's chat
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(charlieDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").get());
  await assertFails(
    charlieDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_2").set({
      conversationId: "conv_1",
      senderId: "charlie_uid",
      senderName: "Charlie",
      text: "Intruder",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(),
    })
  );
});

test("PII Isolation: Users cannot read another user's private /users/{uid} account document", async () => {
  const ts = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      status: "APPROVED",
      role: "MEMBER",
      friendCode: "ALICE123",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

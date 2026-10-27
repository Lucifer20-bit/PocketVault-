const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("transactions").doc("tx1").set({
      userId: BOB_UID,
      title: "Groceries",
      amount: 45.5,
      type: "expense",
      category: "Food",
      paymentMethod: "Credit Card",
      date: "2026-10-27",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx1").get());
});

test("Authenticated user: can create valid transaction for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set({
      userId: ALICE_UID,
      title: "Coffee",
      amount: 4.5,
      type: "expense",
      category: "Food",
      paymentMethod: "Cash",
      date: "2026-10-27",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create transaction with spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx2").set({
      userId: BOB_UID,
      title: "Sneaky",
      amount: 50,
      type: "expense",
      category: "Food",
      paymentMethod: "Cash",
      date: "2026-10-27",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create and read budgets for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b1").set({
      userId: ALICE_UID,
      category: "Food",
      monthlyLimit: 500,
      month: "2026-10",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
});

test("Authenticated user: can create and read subscriptions for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subscriptions").doc("sub1").set({
      userId: ALICE_UID,
      name: "Netflix",
      amount: 15.99,
      billingCycle: "monthly",
      category: "Entertainment",
      nextRenewalDate: "2026-11-01",
      paymentMethod: "Credit Card",
      isActive: true,
      createdAt: new Date(),
    })
  );
});

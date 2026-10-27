package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Budget
import com.example.data.model.Subscription
import com.example.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class PocketVaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addTransaction_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PocketVaultRepository(firestore)

    val tx = Transaction(
      title = "Groceries",
      amount = 45.90,
      type = "expense",
      category = "Food",
      paymentMethod = "Credit Card",
      date = "2026-10-27"
    )
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addTransaction(uid, tx)
    }
    assertTrue(result.isSuccess)
    val txId = result.getOrThrow()
    assertTrue(txId.isNotEmpty())

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { it.any { item -> item.id == txId } }
    }
    assertTrue(list.any { it.id == txId })
  }

  @Test
  fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PocketVaultRepository(firestore)

    // Sign out to test unauthenticated access
    auth.signOut()

    var thrown: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first()
      }
    } catch (e: Throwable) {
      thrown = e
    }
    assertNotNull("Expected permission denied exception", thrown)
    val firestoreException = checkNotNull(thrown!!.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun budgetAndSubscription_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PocketVaultRepository(firestore)

    val budget = Budget(
      category = "Entertainment",
      monthlyLimit = 200.0,
      month = "2026-10"
    )
    val budgetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.setBudget(uid, budget)
    }
    assertTrue(budgetResult.isSuccess)

    val sub = Subscription(
      name = "Spotify",
      amount = 10.99,
      billingCycle = "monthly",
      category = "Entertainment",
      nextRenewalDate = "2026-11-15",
      paymentMethod = "Debit Card",
      isActive = true
    )
    val subResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addSubscription(uid, sub)
    }
    assertTrue(subResult.isSuccess)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}

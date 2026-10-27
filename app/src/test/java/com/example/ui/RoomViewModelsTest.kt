package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PocketVaultDatabase
import com.example.data.local.entity.BudgetEnvelopeEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PocketVaultRoomRepository
import com.example.ui.budgets.EnvelopeManagementViewModel
import com.example.ui.components.ExpenseEntryViewModel
import com.example.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomViewModelsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: PocketVaultDatabase
    private lateinit var repository: PocketVaultRoomRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PocketVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PocketVaultRoomRepository(
            database.transactionDao(),
            database.budgetEnvelopeDao(),
            database.subscriptionDao()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun expenseEnvelopeSubscriptionEntities_roomStorage_insertAndQuery() = runBlocking {
        val expenseDao = database.expenseDao()
        val envelopeDao = database.envelopeDao()
        val subscriptionDao = database.subscriptionDao()

        // 1. Insert and query ExpenseEntity
        val expense = com.example.data.local.entity.ExpenseEntity(
            title = "Whole Foods Organic",
            amount = 112.40,
            category = "Food",
            paymentMethod = "Debit Card",
            date = "2026-10-27",
            notes = "Weekly grocery haul"
        )
        val expenseId = expenseDao.insertExpense(expense)
        assertTrue(expenseId > 0)
        val expenses = expenseDao.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals("Whole Foods Organic", expenses[0].title)

        // 2. Insert and query EnvelopeEntity
        val envelope = com.example.data.local.entity.EnvelopeEntity(
            category = "Dining Out",
            monthlyLimit = 300.0,
            spent = 145.0,
            month = "2026-10"
        )
        val envelopeId = envelopeDao.insertEnvelope(envelope)
        assertTrue(envelopeId > 0)
        val envelopes = envelopeDao.getAllEnvelopes().first()
        assertEquals(1, envelopes.size)
        assertEquals("Dining Out", envelopes[0].category)
        assertEquals(300.0, envelopes[0].monthlyLimit, 0.01)

        // 3. Insert and query SubscriptionEntity
        val sub = com.example.data.local.entity.SubscriptionEntity(
            name = "YouTube Premium",
            amount = 13.99,
            billingCycle = "monthly",
            category = "Entertainment",
            nextRenewalDate = "2026-11-20",
            paymentMethod = "Credit Card",
            isActive = true
        )
        val subId = subscriptionDao.insertSubscription(sub)
        assertTrue(subId > 0)
        val subs = subscriptionDao.getAllSubscriptions().first()
        assertEquals(1, subs.size)
        assertEquals("YouTube Premium", subs[0].name)
    }

    @Test
    fun expenseEntryViewModel_validationAndInsert_success() = runBlocking {
        val viewModel = ExpenseEntryViewModel(repository)

        // Invalid: empty title and zero amount
        viewModel.onTitleChange("")
        viewModel.onAmountChange("0")
        viewModel.save()

        val stateAfterInvalid = viewModel.formState.value
        assertNotNull(stateAfterInvalid.titleError)
        assertNotNull(stateAfterInvalid.amountError)
        assertFalse(stateAfterInvalid.saveSuccess)

        // Valid: grocery expense
        viewModel.onTitleChange("Trader Joe's")
        viewModel.onAmountChange("85.50")
        viewModel.onTypeChange("expense")
        viewModel.onCategoryChange("Food")
        viewModel.onPaymentMethodChange("Credit Card")
        viewModel.onDateChange("2026-10-27")

        var saved = false
        viewModel.save(onSuccess = { saved = true })
        withTimeout(3000L) { viewModel.formState.first { it.saveSuccess } }

        assertTrue(saved)
        val allTx = withTimeout(3000L) { repository.observeAllTransactions().first { it.isNotEmpty() } }
        assertEquals(1, allTx.size)
        assertEquals("Trader Joe's", allTx[0].title)
        assertEquals(85.50, allTx[0].amount, 0.01)
    }

    @Test
    fun envelopeManagementViewModel_statusAlertsCalculation_safeWarningExceeded() = runBlocking {
        val viewModel = EnvelopeManagementViewModel(repository)
        viewModel.selectMonth("2026-10")

        // 1. Create Envelopes
        // Food: Limit 200, Spent 100 -> 50% (SAFE)
        repository.insertEnvelope(BudgetEnvelopeEntity(category = "Food", monthlyLimit = 200.0, month = "2026-10"))
        repository.insertTransaction(TransactionEntity(title = "Groceries", amount = 100.0, type = "expense", category = "Food", paymentMethod = "Card", date = "2026-10-05"))

        // Transit: Limit 100, Spent 80 -> 80% (WARNING / Nearing Limit)
        repository.insertEnvelope(BudgetEnvelopeEntity(category = "Transit", monthlyLimit = 100.0, month = "2026-10"))
        repository.insertTransaction(TransactionEntity(title = "Gas", amount = 80.0, type = "expense", category = "Transit", paymentMethod = "Card", date = "2026-10-10"))

        // Entertainment: Limit 50, Spent 65 -> 130% (EXCEEDED)
        repository.insertEnvelope(BudgetEnvelopeEntity(category = "Entertainment", monthlyLimit = 50.0, month = "2026-10"))
        repository.insertTransaction(TransactionEntity(title = "Concert", amount = 65.0, type = "expense", category = "Entertainment", paymentMethod = "Card", date = "2026-10-15"))

        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = withTimeout(3000L) { viewModel.uiState.first { it.envelopes.size == 3 } }
        val foodEnvelope = uiState.envelopes.first { it.category == "Food" }
        val transitEnvelope = uiState.envelopes.first { it.category == "Transit" }
        val entertainmentEnvelope = uiState.envelopes.first { it.category == "Entertainment" }

        assertEquals(BudgetStatus.SAFE, foodEnvelope.status)
        assertEquals(BudgetStatus.WARNING, transitEnvelope.status)
        assertEquals(BudgetStatus.EXCEEDED, entertainmentEnvelope.status)

        assertEquals(350.0, uiState.totalBudget, 0.01)
        assertEquals(245.0, uiState.totalSpent, 0.01)
    }

    @Test
    fun dashboardViewModel_cashflowAndFiltering_accurateMetrics() = runBlocking {
        val viewModel = DashboardViewModel(repository)
        viewModel.selectMonth("2026-10")

        // Add 1 income and 2 expenses
        repository.insertTransaction(TransactionEntity(title = "Paycheck", amount = 3000.0, type = "income", category = "Salary", paymentMethod = "Transfer", date = "2026-10-01"))
        repository.insertTransaction(TransactionEntity(title = "Electric Bill", amount = 120.0, type = "expense", category = "Bills", paymentMethod = "Bank", date = "2026-10-08"))
        repository.insertTransaction(TransactionEntity(title = "Dinner", amount = 80.0, type = "expense", category = "Food", paymentMethod = "Card", date = "2026-10-12"))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = withTimeout(3000L) { viewModel.uiState.first { it.transactions.size == 3 } }
        assertEquals(3000.0, state.metrics.totalIncome, 0.01)
        assertEquals(200.0, state.metrics.totalExpense, 0.01)
        assertEquals(2800.0, state.metrics.netBalance, 0.01)

        // Filter only expenses
        viewModel.setTypeFilter("expense")
        testDispatcher.scheduler.advanceUntilIdle()

        val expenseOnlyState = withTimeout(3000L) { viewModel.uiState.first { it.typeFilter == "expense" } }
        assertEquals(2, expenseOnlyState.transactions.size)
        assertTrue(expenseOnlyState.transactions.all { it.type == "expense" })
    }
}

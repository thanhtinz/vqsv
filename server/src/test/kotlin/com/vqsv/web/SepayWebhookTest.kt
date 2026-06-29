package com.vqsv.web

import com.vqsv.entity.Account
import com.vqsv.entity.PaymentSettings
import com.vqsv.entity.PaymentTransaction
import com.vqsv.repository.AccountRepository
import com.vqsv.repository.PaymentSettingsRepository
import com.vqsv.repository.PaymentTransactionRepository
import com.vqsv.repository.TopupPackageRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.Optional

/**
 * SePay webhook auth + order-matching logic, with repos mocked (no DB).
 */
class SepayWebhookTest {

    private fun service(
        settings: PaymentSettings,
        tx: PaymentTransaction?
    ): Pair<TopupService, PaymentTransaction?> {
        val pkgRepo = mock(TopupPackageRepository::class.java)
        val payRepo = mock(PaymentTransactionRepository::class.java)
        val accRepo = mock(AccountRepository::class.java)
        val setRepo = mock(PaymentSettingsRepository::class.java)
        `when`(setRepo.findById(1)).thenReturn(Optional.of(settings))
        if (tx != null) `when`(payRepo.findById(tx.id)).thenReturn(Optional.of(tx))
        `when`(payRepo.save(org.mockito.ArgumentMatchers.any(PaymentTransaction::class.java)))
            .thenAnswer { it.getArgument(0) }
        val account = mock(Account::class.java)
        `when`(accRepo.findById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.of(account))
        `when`(accRepo.save(org.mockito.ArgumentMatchers.any(Account::class.java))).thenAnswer { it.getArgument(0) }
        return TopupService(pkgRepo, payRepo, accRepo, setRepo) to tx
    }

    private val enabled = PaymentSettings(
        id = 1, enabled = true, sepayApiKey = "SECRET-KEY", bankAccount = "0123456789",
        bankCode = "MBBank", accountHolder = "NGUYEN VAN A", prefix = "VQSV"
    )

    private fun pendingTx() = PaymentTransaction(
        id = 5, accountId = 99, amountVnd = 10_000, xuGranted = 10_000, provider = "SEPAY", status = "PENDING"
    )

    @Test
    fun `valid transfer credits the matching order`() {
        val (svc, tx) = service(enabled, pendingTx())
        val ok = svc.handleSepayWebhook("SECRET-KEY", "VQSV5 chuyen tien", 10_000, "in")
        assertTrue(ok)
        assertEquals("SUCCESS", tx!!.status)
    }

    @Test
    fun `wrong api key is rejected`() {
        val (svc, tx) = service(enabled, pendingTx())
        assertFalse(svc.handleSepayWebhook("WRONG", "VQSV5", 10_000, "in"))
        assertEquals("PENDING", tx!!.status)
    }

    @Test
    fun `disabled settings reject everything`() {
        val (svc, _) = service(enabled.copy(enabled = false), pendingTx())
        assertFalse(svc.handleSepayWebhook("SECRET-KEY", "VQSV5", 10_000, "in"))
    }

    @Test
    fun `content without the prefix-id does not match`() {
        val (svc, _) = service(enabled, pendingTx())
        assertFalse(svc.handleSepayWebhook("SECRET-KEY", "tien an trua", 10_000, "in"))
    }

    @Test
    fun `underpaid transfer is ignored`() {
        val (svc, tx) = service(enabled, pendingTx())
        assertFalse(svc.handleSepayWebhook("SECRET-KEY", "VQSV5", 5_000, "in"))
        assertEquals("PENDING", tx!!.status)
    }

    @Test
    fun `outgoing transfer is ignored`() {
        val (svc, _) = service(enabled, pendingTx())
        assertFalse(svc.handleSepayWebhook("SECRET-KEY", "VQSV5", 10_000, "out"))
    }

    @Test
    fun `an already-credited order acks idempotently`() {
        val done = pendingTx().copy(status = "SUCCESS")
        val (svc, _) = service(enabled, done)
        assertTrue(svc.handleSepayWebhook("SECRET-KEY", "VQSV5", 10_000, "in"))
    }
}

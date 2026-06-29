package com.vqsv.web.payment

import com.vqsv.entity.PaymentTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies the VNPAY HMAC-SHA512 signing/verification without any network or keys.
 * Re-implements the signature here so the test is independent of the provider code.
 */
class VnpayPaymentProviderTest {

    private val secret = "VQSVTESTSECRETKEY1234567890ABCDEF"
    private val provider = VnpayPaymentProvider(
        enabled = true,
        tmnCode = "TESTTMN",
        hashSecret = secret,
        payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
        returnUrl = "https://play.example.com/api/web/public/topup/vnpay/return"
    )

    private fun sign(params: Map<String, String>): String {
        val data = params.toSortedMap().entries.joinToString("&") {
            "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.US_ASCII)}"
        }
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA512"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `checkout url carries amount x100 and a signature`() {
        val url = provider.createCheckoutUrl(PaymentTransaction(id = 42, amountVnd = 10_000))
        assertNotNull(url)
        assertTrue(url!!.contains("vnp_Amount=1000000"), "amount must be VND x100")
        assertTrue(url.contains("vnp_TxnRef=42"))
        assertTrue(url.contains("vnp_SecureHash="))
    }

    @Test
    fun `disabled provider produces no checkout url`() {
        val off = VnpayPaymentProvider(false, "T", secret, "https://p", "https://r")
        assertEquals(null, off.createCheckoutUrl(PaymentTransaction(id = 1, amountVnd = 1000)))
    }

    @Test
    fun `a correctly signed success callback verifies and maps the tx`() {
        val params = linkedMapOf(
            "vnp_TxnRef" to "42",
            "vnp_Amount" to "1000000",
            "vnp_ResponseCode" to "00",
            "vnp_TransactionStatus" to "00",
            "vnp_TransactionNo" to "987654"
        )
        val signed = HashMap(params).apply { put("vnp_SecureHash", sign(params)) }
        val result = provider.verifyCallback(signed)
        assertTrue(result.success)
        assertEquals(42L, result.txId)
        assertEquals("987654", result.providerRef)
    }

    @Test
    fun `a tampered parameter fails signature verification`() {
        val params = linkedMapOf(
            "vnp_TxnRef" to "42",
            "vnp_Amount" to "1000000",
            "vnp_ResponseCode" to "00"
        )
        val signed = HashMap(params).apply { put("vnp_SecureHash", sign(params)) }
        signed["vnp_Amount"] = "1"   // tamper after signing
        assertFalse(provider.verifyCallback(signed).success)
    }

    @Test
    fun `a failed payment code is not treated as success even when signed`() {
        val params = linkedMapOf(
            "vnp_TxnRef" to "42",
            "vnp_ResponseCode" to "24",   // user cancelled
            "vnp_TransactionStatus" to "02"
        )
        val signed = HashMap(params).apply { put("vnp_SecureHash", sign(params)) }
        assertFalse(provider.verifyCallback(signed).success)
    }
}

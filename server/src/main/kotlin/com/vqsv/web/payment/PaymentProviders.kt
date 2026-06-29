package com.vqsv.web.payment

import com.vqsv.entity.PaymentTransaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Result of verifying a gateway callback (return URL / IPN). */
data class CallbackResult(
    val txId: Long?,
    val success: Boolean,
    val providerRef: String?
)

/**
 * A payment gateway. Implementations are Spring beans; [PaymentService] looks them
 * up by [name]. Add a new gateway (MoMo, ZaloPay, …) by adding another @Component.
 */
interface PaymentProvider {
    /** Uppercase id matching PaymentTransaction.provider, e.g. "VNPAY". */
    val name: String

    /** Is this provider configured/enabled? */
    val enabled: Boolean

    /**
     * URL to redirect the buyer to in order to pay. Return null for providers that
     * have no redirect (e.g. MANUAL — an admin confirms the transfer by hand).
     */
    fun createCheckoutUrl(tx: PaymentTransaction): String?

    /** Verify a callback's signed parameters and map it back to our transaction. */
    fun verifyCallback(params: Map<String, String>): CallbackResult
}

/** Default flow: no gateway — the player transfers manually and an admin approves. */
@Component
class ManualPaymentProvider : PaymentProvider {
    override val name = "MANUAL"
    override val enabled = true
    override fun createCheckoutUrl(tx: PaymentTransaction): String? = null
    override fun verifyCallback(params: Map<String, String>) = CallbackResult(null, false, null)
}

/**
 * VNPAY integration (https://sandbox.vnpayment.vn). The checkout URL and the
 * callbacks are signed with HMAC-SHA512 over the sorted, URL-encoded parameters.
 * Configure with vqsv.payment.vnpay.* (see application.yml).
 */
@Component
class VnpayPaymentProvider(
    @Value("\${vqsv.payment.vnpay.enabled:false}") override val enabled: Boolean,
    @Value("\${vqsv.payment.vnpay.tmn-code:}") private val tmnCode: String,
    @Value("\${vqsv.payment.vnpay.hash-secret:}") private val hashSecret: String,
    @Value("\${vqsv.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") private val payUrl: String,
    @Value("\${vqsv.payment.vnpay.return-url:http://localhost:8080/api/web/public/topup/vnpay/return}") private val returnUrl: String
) : PaymentProvider {

    override val name = "VNPAY"

    override fun createCheckoutUrl(tx: PaymentTransaction): String? {
        if (!enabled) return null
        val now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
        val fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        // VNPAY amount is in the smallest unit (VND x 100).
        val fields = sortedMapOf(
            "vnp_Version" to "2.1.0",
            "vnp_Command" to "pay",
            "vnp_TmnCode" to tmnCode,
            "vnp_Amount" to (tx.amountVnd.toLong() * 100).toString(),
            "vnp_CurrCode" to "VND",
            "vnp_TxnRef" to tx.id.toString(),
            "vnp_OrderInfo" to "Nap xu VQSV tx ${tx.id}",
            "vnp_OrderType" to "other",
            "vnp_Locale" to "vn",
            "vnp_ReturnUrl" to returnUrl,
            "vnp_IpAddr" to "127.0.0.1",
            "vnp_CreateDate" to now.format(fmt),
            "vnp_ExpireDate" to now.plusMinutes(15).format(fmt)
        )
        val hashData = fields.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val secureHash = hmacSHA512(hashSecret, hashData)
        return "$payUrl?$hashData&vnp_SecureHash=$secureHash"
    }

    override fun verifyCallback(params: Map<String, String>): CallbackResult {
        val received = params["vnp_SecureHash"]
        val signable = params.filterKeys { it != "vnp_SecureHash" && it != "vnp_SecureHashType" }
            .toSortedMap()
        val signData = signable.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val expected = hmacSHA512(hashSecret, signData)
        val signatureValid = received != null && expected.equals(received, ignoreCase = true)
        val txId = params["vnp_TxnRef"]?.toLongOrNull()
        val paid = params["vnp_ResponseCode"] == "00" && params["vnp_TransactionStatus"].let { it == null || it == "00" }
        return CallbackResult(txId, signatureValid && paid, params["vnp_TransactionNo"])
    }

    private fun enc(v: String): String = URLEncoder.encode(v, StandardCharsets.US_ASCII)

    private fun hmacSHA512(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA512"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

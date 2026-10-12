package com.example.flowmind.domain.privacy

/**
 * Utility object for detecting and redacting personally identifiable
 * information (PII) before data is sent to cloud inference endpoints.
 *
 * All patterns are compiled once at class-load time for efficiency.
 */
object PrivacyShield {

    /** Matches US Social Security Numbers in XXX-XX-XXXX format. */
    private val SSN_REGEX = Regex("""\b\d{3}-\d{2}-\d{4}\b""")

    /**
     * Matches common credit/debit card number formats:
     * 13–16 digits optionally separated by spaces or dashes.
     */
    private val CREDIT_CARD_REGEX = Regex("""\b(?:\d[ -]*?){13,16}\b""")

    /** Matches standard e-mail addresses. */
    private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}\b""")

    /** Matches US/international phone numbers (e.g. +1-800-555-0199, (555) 123-4567). */
    private val PHONE_REGEX = Regex("""(\+?\d[\d\s\-().]{7,}\d)""")

    /** Matches IPv4 addresses. */
    private val IPV4_REGEX = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")

    /**
     * Replaces all detected PII patterns in [input] with labelled placeholders.
     *
     * @param input Raw text that may contain sensitive information.
     * @return A copy of [input] with PII replaced by `[REDACTED_*]` tokens.
     */
    fun redactSensitiveInfo(input: String): String = input
        .replace(SSN_REGEX,         "[REDACTED_SSN]")
        .replace(CREDIT_CARD_REGEX, "[REDACTED_CC]")
        .replace(EMAIL_REGEX,       "[REDACTED_EMAIL]")
        .replace(PHONE_REGEX,       "[REDACTED_PHONE]")
        .replace(IPV4_REGEX,        "[REDACTED_IP]")

    /**
     * Returns `true` when [input] contains no PII patterns that should be
     * kept off cloud servers without prior redaction.
     *
     * @param input Raw text to inspect.
     */
    fun isSafeForCloud(input: String): Boolean =
        !SSN_REGEX.containsMatchIn(input) &&
        !CREDIT_CARD_REGEX.containsMatchIn(input) &&
        !EMAIL_REGEX.containsMatchIn(input) &&
        !PHONE_REGEX.containsMatchIn(input)
}

package com.example.flowmind.domain.privacy

import java.util.regex.Pattern

object PrivacyShield {
    
    // Simple regex for SSN (XXX-XX-XXXX)
    private val SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    
    // Simple regex for Credit Card (16 digits)
    private val CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b")
    
    // Simple regex for Emails
    private val EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")

    fun redactSensitiveInfo(input: String): String {
        var redacted = input
        
        redacted = SSN_PATTERN.matcher(redacted).replaceAll("[REDACTED_SSN]")
        redacted = CREDIT_CARD_PATTERN.matcher(redacted).replaceAll("[REDACTED_CC]")
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[REDACTED_EMAIL]")
        
        return redacted
    }

    fun isSafeForCloud(input: String): Boolean {
        // If the original input contains sensitive info, we flag it as not safe for raw cloud processing.
        val hasSsn = SSN_PATTERN.matcher(input).find()
        val hasCc = CREDIT_CARD_PATTERN.matcher(input).find()
        return !(hasSsn || hasCc)
    }
}

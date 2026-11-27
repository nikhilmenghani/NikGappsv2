package com.nikgapps.v2

import android.content.Context
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.Response

internal fun registryRetryAt(retryAfter: String?, reset: String?, now: Long): Long {
    fun plus(value: Long) = if (value > Long.MAX_VALUE - now) Long.MAX_VALUE else now + value
    val seconds = retryAfter?.trim()?.toLongOrNull()?.takeIf { it >= 0 && it <= Long.MAX_VALUE / 1000 }
    val relative = seconds?.let { plus(it * 1000) }
    val absolute = retryAfter?.let { text ->
        val position = ParsePosition(0)
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
            isLenient = false; timeZone = TimeZone.getTimeZone("GMT")
        }
        format.parse(text, position)?.time?.takeIf { position.index == text.length }
    }
    val resetAt = reset?.toLongOrNull()?.takeIf { it in 0..Long.MAX_VALUE / 1000 }?.times(1000)
    return (relative ?: absolute ?: resetAt ?: plus(60_000)).coerceAtLeast(plus(1000))
}
internal class RegistryRateLimitedException(val retryAtMillis: Long) : IOException("GitHub request limit reached. Try again after the cooldown; verified cached data remains available.")

/** Shared between metadata and later package downloads; restart cannot erase the server cooldown. */
internal class RegistryCooldown(private val now: () -> Long = System::currentTimeMillis) {
    private val deadline = MutableStateFlow<Long?>(null)
    val retryAtMillis = deadline.asStateFlow()
    private var save: (Long?) -> Unit = {}
    @Synchronized fun restore(stored: Long, persist: (Long?) -> Unit) {
        save = persist
        deadline.value = listOfNotNull(stored.takeIf { it > now() }, deadline.value).maxOrNull()
        clearExpired()
    }
    @Synchronized fun rateLimited(retryAfter: String?, reset: String?): Long {
        val value = maxOf(deadline.value ?: 0, registryRetryAt(retryAfter, reset, now()))
        deadline.value = value; save(value); return value
    }
    @Synchronized private fun clearExpired() {
        if ((deadline.value ?: 0) <= now()) { deadline.value = null; save(null) }
    }
    @Synchronized fun requireAvailable(url: HttpUrl) {
        if (!RegistryDelivery.trusted(url)) return
        clearExpired(); deadline.value?.let { throw RegistryRateLimitedException(it) }
    }
}
internal object RegistryAvailability {
    val cooldown = RegistryCooldown()
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("github-registry-availability", Context.MODE_PRIVATE)
        cooldown.restore(prefs.getLong("retry-at", 0)) { prefs.edit().putLong("retry-at", it ?: 0).apply() }
    }
}
internal data class RegistryRequestLimit(val remaining: Long?, val limit: Long?, val resetEpochSeconds: Long?,
    val retryAfter: String?, val resource: String?, val status: Int, val observedAtMillis: Long)
internal object RegistryRequestLimits {
    private val mutable = MutableStateFlow<RegistryRequestLimit?>(null)
    val latest = mutable.asStateFlow()
    fun observe(response: Response) {
        response.priorResponse?.let(::observe)
        if (!RegistryDelivery.trusted(response.request.url)) return
        mutable.value = RegistryRequestLimit(response.header("X-RateLimit-Remaining")?.toLongOrNull(),
            response.header("X-RateLimit-Limit")?.toLongOrNull(), response.header("X-RateLimit-Reset")?.toLongOrNull(),
            response.header("Retry-After"), response.header("X-RateLimit-Resource"), response.code, System.currentTimeMillis())
    }
}

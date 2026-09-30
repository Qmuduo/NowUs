package app.nowus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nowus.android.data.AccountSession
import app.nowus.android.data.ApiException
import app.nowus.android.data.AuthenticationApi
import app.nowus.android.data.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException


class AuthViewModel(
    private val api: AuthenticationApi,
    private val sessions: SessionStore,
) : ViewModel() {
    private val _step = MutableStateFlow(AuthStep.EMAIL)
    val step = _step.asStateFlow()
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    private val _code = MutableStateFlow("")
    val code = _code.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds = _cooldownSeconds.asStateFlow()

    fun setEmail(value: String) { _email.value = value; _error.value = null }
    fun setCode(value: String) { _code.value = value.filter(Char::isDigit).take(6); _error.value = null }
    fun changeEmail() { _step.value = AuthStep.EMAIL; _code.value = ""; _notice.value = null; _error.value = null }
    fun tickCooldown() { if (_cooldownSeconds.value > 0) _cooldownSeconds.value -= 1 }
    fun clearError() { _error.value = null }

    fun requestOtp() {
        if (_busy.value) return
        val address = _email.value.trim()
        if (address.isBlank()) { _error.value = "请输入邮箱地址"; return }
        _busy.value = true
        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            try {
                _cooldownSeconds.value = api.requestOtp(address)
                _step.value = AuthStep.CODE
                _notice.value = "验证码发送请求已受理。请检查收件箱和垃圾邮件；未收到时可以稍后重发。"
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                if (error.errorCode == "otp_resend_wait") {
                    _cooldownSeconds.value = maxOf(_cooldownSeconds.value, error.retryAfterSeconds ?: 60)
                }
                _error.value = errorMessage(error)
            } catch (error: Exception) {
                _error.value = errorMessage(error)
            } finally {
                _busy.value = false
            }
        }
    }

    fun verifyOtp() {
        if (_busy.value) return
        val address = _email.value.trim()
        val passcode = _code.value
        if (passcode.length != 6) { _error.value = "请输入邮件中的 6 位验证码"; return }
        _busy.value = true
        _error.value = null
        _notice.value = null
        viewModelScope.launch {
            try {
                val session: AccountSession = api.verifyOtp(address, passcode)
                sessions.save(session)
                _cooldownSeconds.value = 0
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _error.value = errorMessage(error)
            } finally {
                _busy.value = false
            }
        }
    }

    suspend fun clearSession() = sessions.clear()
    fun forgetSession() { viewModelScope.launch { sessions.clear() } }

    private fun errorMessage(error: Exception): String = when ((error as? ApiException)?.errorCode) {
        "email_invalid" -> "邮箱格式不正确，请检查后重试"
        "otp_invalid" -> "验证码错误，请再试一次"
        "otp_expired" -> "验证码已过期，请重新发送"
        "otp_missing" -> "验证码已使用或不存在，请重新发送"
        "otp_attempts_exceeded" -> "尝试次数已达上限，请重新发送"
        "otp_resend_wait" -> "请等待 ${((error as? ApiException)?.retryAfterSeconds ?: 60)} 秒后重发"
        "otp_hourly_limit", "otp_source_hourly_limit" -> "发送次数已达上限，请稍后再试"
        "mail_send_failed" -> "邮件发送失败，请稍后重试"
        "api_not_configured" -> "尚未配置服务地址，请联系开发者"
        else -> when (error) {
            is ApiException -> "服务请求失败，请稍后重试"
            is IOException -> "网络连接失败，请检查网络后重试"
            is java.time.DateTimeException -> "登录信息的时间格式不兼容，请重新发送验证码后重试"
            is java.security.GeneralSecurityException -> "手机安全存储不可用，请重启应用后重试"
            is IllegalStateException -> "手机未能保存登录会话，请重新发送验证码后重试"
            else -> "登录处理失败，请重新发送验证码后重试"
        }
    }
}

enum class AuthStep { EMAIL, CODE }

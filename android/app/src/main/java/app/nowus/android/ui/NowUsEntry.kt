package app.nowus.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nowus.android.AppViewModel
import app.nowus.android.AuthStep
import app.nowus.android.AuthViewModel
import app.nowus.android.data.AccountApi
import app.nowus.android.data.AccountRepository
import app.nowus.android.data.AccountSession
import app.nowus.android.data.AccountSnapshotStore
import app.nowus.android.data.SessionStore
import kotlinx.coroutines.delay


@Composable
fun NowUsEntry(
    authViewModel: AuthViewModel,
    sessionStore: SessionStore,
    api: AccountApi,
    snapshotStore: AccountSnapshotStore,
    demoViewModel: AppViewModel,
    demoRepository: app.nowus.android.data.StateRepository,
    pendingInviteCode: String?,
    onLaunchDemo: () -> Unit,
    onPendingInviteConsumed: () -> Unit,
) {
    val session by sessionStore.current.collectAsStateWithLifecycle()
    val cooldown by authViewModel.cooldownSeconds.collectAsStateWithLifecycle()
    var demoMode by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1_000)
            authViewModel.tickCooldown()
        }
    }
    val activeSession = session
    if (activeSession == null) {
        if (demoMode) {
            NowUsApp(demoViewModel, onExitDemo = { demoMode = false })
        } else {
            AuthenticationScreen(authViewModel, pendingInviteCode) { demoMode = true; onLaunchDemo() }
        }
    } else {
        val factory = remember(activeSession.userId, activeSession.accessToken) {
            val repository = AccountRepository(
                api = api,
                session = activeSession,
                cache = snapshotStore,
                onSessionExpired = { sessionStore.clear() },
            )
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(repository, demoRepository) as T
            }
        }
        val accountViewModel: AppViewModel = viewModel(
            key = "nowus-account-${activeSession.userId}-${activeSession.accessToken.hashCode()}",
            factory = factory,
        )
        NowUsApp(
            vm = accountViewModel,
            realAccount = true,
            pendingInviteCode = pendingInviteCode,
            onPendingInviteConsumed = onPendingInviteConsumed,
            onLogout = { accountViewModel.logout { authViewModel.forgetSession() } },
            draftScopeKey = "account:" + activeSession.userId,
        )
    }
}

@Composable
private fun AuthenticationScreen(vm: AuthViewModel, pendingInviteCode: String?, onLaunchDemo: () -> Unit) {
    val email by vm.email.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val cooldown by vm.cooldownSeconds.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(Page).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        androidx.compose.foundation.Image(
            painter = painterResource(app.nowus.android.R.drawable.logo_lockup),
            contentDescription = "NowUs 完整标志",
            modifier = Modifier.width(208.dp).height(58.dp),
        )
        Text("让相隔时区的日常，也能彼此靠近。", style = MaterialTheme.typography.titleMedium)
        if (pendingInviteCode != null) Text("已保存邀请码 $pendingInviteCode。登录后可查看邀请人和共享范围。", color = Accent)
        SectionCard(if (step == AuthStep.EMAIL) "邮箱登录 / 注册" else "输入邮箱验证码") {
            if (step == AuthStep.EMAIL) {
                Text("输入邮箱后，我们会发送一次性验证码。首次验证会创建账号，之后可恢复原资料与配对。")
                OutlinedTextField(
                    value = email,
                    onValueChange = vm::setEmail,
                    label = { Text("邮箱") },
                    modifier = Modifier.fillMaxWidth().testTag("authEmail"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                if (error != null) ErrorText(error!!)
                Button(enabled = !busy, onClick = vm::requestOtp, modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator()
                    else Text("发送 6 位验证码")
                }
            } else {
                Text("验证码已请求发送至 $email。请求受理不代表邮件已送达，请检查垃圾邮件。")
                OutlinedTextField(
                    value = code,
                    onValueChange = vm::setCode,
                    label = { Text("6 位验证码") },
                    modifier = Modifier.fillMaxWidth().testTag("authOtpCode"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("验证码 10 分钟有效；每 60 秒可重发一次。")
                if (notice != null) Text(notice!!, style = MaterialTheme.typography.bodySmall, color = Accent)
                if (error != null) ErrorText(error!!)
                Button(enabled = !busy, onClick = vm::verifyOtp, modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator() else Text("验证并继续")
                }
                OutlinedButton(
                    enabled = !busy && cooldown == 0,
                    onClick = vm::requestOtp,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (cooldown > 0) "${cooldown} 秒后可重发" else "重新发送验证码") }
                OutlinedButton(enabled = !busy, onClick = vm::changeEmail, modifier = Modifier.fillMaxWidth()) {
                    Text("更换邮箱")
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("只想体验页面？", style = MaterialTheme.typography.titleMedium)
                Text("本地体验版使用独立的演示数据，不登录、不上传，也不会创建真实邀请。")
                OutlinedButton(onClick = onLaunchDemo, modifier = Modifier.fillMaxWidth()) {
                    Text("打开本地体验版 · 演示数据")
                }
            }
        }
    }
}

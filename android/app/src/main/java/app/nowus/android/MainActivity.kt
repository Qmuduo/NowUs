package app.nowus.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nowus.android.data.EncryptedAccountSnapshotStore
import app.nowus.android.data.EncryptedPendingInviteStore
import app.nowus.android.data.EncryptedSessionStore
import app.nowus.android.data.LocalRepository
import app.nowus.android.data.NowUsApiClient
import app.nowus.android.ui.NowUsEntry
import app.nowus.android.ui.NowUsTheme
import kotlinx.coroutines.launch

private val Context.nowUsStore by preferencesDataStore(name = "nowus_local_v1")

class MainActivity : ComponentActivity() {
    private lateinit var pendingInvites: EncryptedPendingInviteStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingInvites = EncryptedPendingInviteStore(this)
        rememberInviteIntent(intent)

        val api = NowUsApiClient(BuildConfig.NOWUS_API_BASE_URL)
        val sessions = EncryptedSessionStore(this)
        val snapshots = EncryptedAccountSnapshotStore(this)
        val localRepository = LocalRepository(applicationContext.nowUsStore)
        val demoViewModel = ViewModelProvider(this, viewModelFactory { AppViewModel(localRepository) })[AppViewModel::class.java]
        val authViewModel = ViewModelProvider(this, viewModelFactory { AuthViewModel(api, sessions) })[AuthViewModel::class.java]

        setContent {
            val pendingCode by pendingInvites.code.collectAsStateWithLifecycle()
            NowUsTheme {
                NowUsEntry(
                    authViewModel = authViewModel,
                    sessionStore = sessions,
                    api = api,
                    snapshotStore = snapshots,
                    demoViewModel = demoViewModel,
                    demoRepository = localRepository,
                    pendingInviteCode = pendingCode,
                    onLaunchDemo = {},
                    onPendingInviteConsumed = { lifecycleScope.launch { pendingInvites.clear() } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        rememberInviteIntent(intent)
    }

    private fun rememberInviteIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "nowus" || uri.host != "invite") return
        val inviteCode = uri.pathSegments.singleOrNull()?.trim()?.uppercase().orEmpty()
        if (inviteCode.length == 10 && inviteCode.all(Char::isLetterOrDigit)) {
            lifecycleScope.launch { pendingInvites.save(inviteCode) }
        }
    }

    private fun <T : ViewModel> viewModelFactory(create: () -> T) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
}

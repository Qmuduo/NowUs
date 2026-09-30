package app.nowus.android
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import app.nowus.android.data.LocalRepository
import app.nowus.android.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
private val Context.nowUsStore by preferencesDataStore(name="nowus_local_v1")
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);enableEdgeToEdge()
  val vm=ViewModelProvider(this,object:ViewModelProvider.Factory{
   @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=AppViewModel(LocalRepository(applicationContext.nowUsStore)) as T
  })[AppViewModel::class.java]
  lifecycleScope.launch {repeatOnLifecycle(Lifecycle.State.STARTED){while(true){vm.refreshTime();delay(1000)}}}
  setContent {NowUsTheme {NowUsApp(vm)}}
 }
}

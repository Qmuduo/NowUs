package app.nowus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID

class AppViewModel(private val repository:StateRepository):ViewModel(){
 val state=MutableStateFlow<AppState?>(null)
 val error=MutableStateFlow<String?>(null)
 val saving=MutableStateFlow(false)
 val readFailed=MutableStateFlow(false)
 val now=MutableStateFlow(Instant.now())
 val demo=MutableStateFlow(false)
 private var loadJob:Job?=null
 init {retry()}
 fun retry(){
  loadJob?.cancel();error.value=null;readFailed.value=false
  loadJob=viewModelScope.launch {
   try{repository.states.collect{state.value=it}}
   catch(e:CancellationException){throw e}
   catch(e:Exception){readFailed.value=true;error.value="加载失败，请重试"}
  }
 }
 fun refreshTime(){if(!demo.value) now.value=Instant.now()}
 fun setDemo(enabled:Boolean){demo.value=enabled;now.value=if(enabled) Instant.parse("2026-09-29T14:00:00Z") else Instant.now()}
 fun clearError(){error.value=null}
 private fun write(onSuccess:()->Unit={},transform:(AppState)->AppState){
  if(saving.value)return
  saving.value=true;error.value=null
  viewModelScope.launch{
   try {repository.update(transform);onSuccess()}
   catch(e:CancellationException){throw e}
   catch(e:IllegalArgumentException){error.value=e.message?:"输入无效"}
   catch(e:Exception){error.value="保存失败，请重试"}
   finally {saving.value=false}
  }
 }
 private fun checked(result:RuleResult):AppState {require(result.error==null){result.error.orEmpty()};return result.state}
 fun saveProfile(profile:Profile,onSuccess:()->Unit={})=write(onSuccess){
  val validation=Rules.validateProfile(profile);require(validation.valid){validation.errors.joinToString("；")};it.copy(me=profile)
 }
 fun saveSchedule(schedule:Schedule,onSuccess:()->Unit={})=write(onSuccess){
  val errors=Rules.validateRhythm(schedule.weekday).errors+Rules.validateRhythm(schedule.rest).errors
  require(errors.isEmpty()){errors.distinct().joinToString("；")};it.copy(schedule=schedule)
 }
 fun completeSetup(onSuccess:()->Unit={})=write(onSuccess){it.copy(setupComplete=true)}
 fun saveNote(text:String,onSuccess:()->Unit={})=write(onSuccess){checked(Rules.saveNote(it,text,Instant.now().toEpochMilli()))}
 fun temporary(available:Boolean,minutes:Int,onSuccess:()->Unit={})=write({refreshTime();onSuccess()}){
  require(minutes in listOf(30,60,180)){"请选择有效时长"}
  val start=Instant.now().toEpochMilli();it.copy(temporary=TemporaryStatus(available,start+minutes*60000L,start))
 }
 fun resetTemporary()=write{it.copy(temporary=null)}
 fun createInvite()=write{checked(Rules.createInvite(it,UUID.randomUUID().toString().take(8).uppercase(),Instant.now().toEpochMilli()))}
 fun revokeInvite()=write{Rules.revokeInvite(it).state}
 fun acceptInvite(code:String,partner:Profile,onSuccess:()->Unit={})=write(onSuccess){checked(Rules.acceptInvite(it,code.trim().uppercase(),partner,Instant.now().toEpochMilli()))}
 fun samplePartner()=write{
  require(it.partner!=null){"请先演示配对"}
  it.copy(partnerSchedule=Schedule(
   Rhythm(sleepStart="23:00",sleepEnd="07:00",activity="上课",activityStart="09:00",activityEnd="17:00",contactStart="10:00",contactEnd="10:30"),
   Rhythm(sleepStart="23:00",sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")
  ))
 }
}

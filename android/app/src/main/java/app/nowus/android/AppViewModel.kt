package app.nowus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nowus.android.data.ApiException
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID

data class LocalDataImport(val profile:Profile,val schedule:Schedule?,val note:Note?)

class AppViewModel(private val repository:StateRepository, private val localDemoRepository:StateRepository?=null):ViewModel(){
 val state=MutableStateFlow<AppState?>(null)
 val error=MutableStateFlow<String?>(null)
 val saving=MutableStateFlow(false)
 val readFailed=MutableStateFlow(false)
 val now=MutableStateFlow(Instant.now())
 val demo=MutableStateFlow(false)
 private val _invitePreview=MutableStateFlow<InvitePreview?>(null)
 val invitePreview=_invitePreview.asStateFlow()
 private val _localImport=MutableStateFlow<LocalDataImport?>(null)
 val localImport=_localImport.asStateFlow()
 private val _deletedNote=MutableStateFlow<Note?>(null)
 val deletedNote=_deletedNote.asStateFlow()
 private var draftJob:Job?=null
 private var loadJob:Job?=null
 init {retry()}
 fun retry(){
  loadJob?.cancel();error.value=null;readFailed.value=false
  loadJob=viewModelScope.launch {
   try{repository.states.collect{state.value=it;_deletedNote.value=it.deletedNote}}
   catch(e:CancellationException){throw e}
   catch(e:Exception){readFailed.value=true;error.value="加载失败，请重试"}
  }
 }
 fun refreshTime(){if(!demo.value) now.value=Instant.now()}
 fun setDemo(enabled:Boolean){demo.value=enabled;now.value=if(enabled) Instant.parse("2026-09-29T12:00:00Z") else Instant.now()}
 fun clearError(){error.value=null}
 private fun action(onSuccess:()->Unit={},operation:suspend()->Unit){
  if(saving.value)return
  saving.value=true;error.value=null
  viewModelScope.launch{
   try {operation();onSuccess()}
   catch(e:CancellationException){throw e}
   catch(e:IllegalArgumentException){error.value=e.message?:"输入无效"}
   catch(e:ApiException){error.value=errorMessage(e)}
   catch(e:Exception){error.value="保存失败，请重试"}
   finally {saving.value=false}
  }
 }
 private fun write(onSuccess:()->Unit={},transform:(AppState)->AppState)=action(onSuccess){repository.update(transform)}
 private fun checked(result:RuleResult):AppState {require(result.error==null){result.error.orEmpty()};return result.state}
 fun saveProfile(profile:Profile,onSuccess:()->Unit={})=write(onSuccess){
  val validation=Rules.validateProfile(profile);require(validation.valid){validation.errors.joinToString("；")};it.copy(me=profile)
 }
 fun saveSchedule(schedule:Schedule,onSuccess:()->Unit={})=write(onSuccess){
  val errors=Rules.validateRhythm(schedule.weekday).errors+Rules.validateRhythm(schedule.rest).errors
  require(errors.isEmpty()){errors.distinct().joinToString("；")};it.copy(schedule=schedule)
 }
 fun completeSetup(onSuccess:()->Unit={})=write(onSuccess){it.copy(setupComplete=true)}
 fun updateNoteDraft(text:String){
  val previous=draftJob
  draftJob=viewModelScope.launch{
   previous?.join()
   try{repository.updateNoteDraft(text)}
   catch(e:CancellationException){throw e}
   catch(e:Exception){error.value="草稿保存失败，请重试"}
  }
 }
 fun saveNote(text:String,onSuccess:()->Unit={})=action(onSuccess){
  draftJob?.join()
  repository.saveNote(text,Instant.now().toEpochMilli())
 }
 fun deleteNote(expectedNote:Note,onSuccess:()->Unit={})=action(onSuccess){
  draftJob?.join()
  repository.deleteNote(expectedNote)
 }
 fun restoreNote(note:Note,onSuccess:()->Unit={})=action(onSuccess){repository.restoreNote(note)}
 fun temporary(available:Boolean,minutes:Int,onSuccess:()->Unit={})=write({refreshTime();onSuccess()}){
  require(minutes in listOf(30,60,180)){"请选择有效时长"}
  val start=Instant.now().toEpochMilli();it.copy(temporary=TemporaryStatus(available,start+minutes*60000L,start))
 }
 fun resetTemporary(onSuccess:()->Unit={})=write(onSuccess){it.copy(temporary=null)}
 fun createInvite()=action{repository.createInvitation(UUID.randomUUID().toString().take(10).uppercase(),Instant.now().toEpochMilli())}
 fun revokeInvite()=action{repository.revokeInvitation()}
 fun previewInvite(code:String){_invitePreview.value=null;action{_invitePreview.value=repository.previewInvitation(code.trim(),Instant.now().toEpochMilli())}}
 fun acceptInvite(code:String,partner:Profile?,onSuccess:()->Unit={})=action({ _invitePreview.value=null;onSuccess() }){repository.acceptInvitation(code.trim().uppercase(),partner,Instant.now().toEpochMilli())}
 fun setSharing(enabled:Boolean)=action{repository.setSharing(enabled)}
 fun unpair(onSuccess:()->Unit={})=action(onSuccess){repository.unpair()}
 fun logout(onSuccess:()->Unit={})=action(onSuccess){repository.logout()}
 fun inspectLocalImport(){
  if(saving.value)return
  error.value=null
  viewModelScope.launch{
   try{
    val local=localDemoRepository?.states?.first()?:throw IllegalStateException("没有可导入的本机体验版资料")
    require(Rules.validateProfile(local.me).valid){"本机体验版中没有有效的本人资料"}
    require(local.schedule!=null||local.note!=null){"本机体验版中没有可导入的作息或留言"}
    _localImport.value=LocalDataImport(local.me,local.schedule,local.note)
   }catch(e:CancellationException){throw e}
   catch(e:Exception){error.value=e.message?:"本机体验版资料无法读取"}
  }
 }
 fun cancelLocalImport(){_localImport.value=null}
 fun importLocalData(onSuccess:()->Unit={})=action({ _localImport.value=null;onSuccess() }){
  val imported=_localImport.value?:throw IllegalStateException("请先检查本机体验版资料")
  repository.update{it.copy(me=imported.profile,schedule=imported.schedule?:it.schedule,note=imported.note?:it.note)}
 }
 fun samplePartner()=write{
  require(it.partner!=null){"请先演示配对"}
  it.copy(partnerSchedule=Schedule(
   Rhythm(sleepStart="23:00",sleepEnd="07:00",activity="上课",activityStart="09:00",activityEnd="17:00",contactStart="10:00",contactEnd="10:30"),
   Rhythm(sleepStart="23:00",sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")
  ))
 }

 private fun errorMessage(error:ApiException)=when(error.errorCode){
  "already_paired"->"其中一方已经配对，无法接受该邀请"
  "invite_self_accept"->"不能接受自己创建的邀请"
  "invite_invalid"->"找不到这个邀请码，请检查后重试"
  "invite_expired"->"邀请已过期，请让邀请人重新创建"
  "invite_revoked"->"邀请已撤销，请让邀请人重新创建"
  "invite_used"->"邀请已被接受，请刷新状态"
  "invite_sharing_paused"->"邀请人的分享已暂停"
  "invite_profile_missing"->"邀请人尚未完成昵称和城市设置"
  "sharing_paused"->"分享已暂停，暂时看不到对方资料"
  "not_paired"->"当前没有有效配对"
  "profile_invalid"->"昵称或城市信息无效"
  "rhythm_invalid"->"作息时间无效，请检查后重试"
  "note_conflict"->"留言已更新，请刷新后重试"
  "note_revision_invalid"->"留言版本无效，请刷新后重试"
  "note_empty"->"留言不能为空"
  "note_too_long"->"留言最多 120 个字符"
  "mail_send_failed"->"邮件发送失败，请稍后重试"
  "session_invalid","session_required"->"登录已过期，请重新登录"
  else->"保存失败，请重试"
 }
}

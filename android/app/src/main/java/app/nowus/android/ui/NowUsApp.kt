package app.nowus.android.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nowus.android.AppViewModel
import app.nowus.android.data.EncryptedNoteDraftStore
import app.nowus.android.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun defaultSchedule(cityId:String="beijing",template:RoutineTemplate=RoutineTemplate.STUDENT)=RoutineTemplates.forCity(cityId,template)

@Composable fun NowUsApp(
 vm:AppViewModel,
 realAccount:Boolean=false,
 pendingInviteCode:String?=null,
 onPendingInviteConsumed:()->Unit={},
 onLogout:()->Unit={},
 onExitDemo:()->Unit={},
 draftScopeKey:String?=null,
){
 val state by vm.state.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val saving by vm.saving.collectAsStateWithLifecycle()
 val readFailed by vm.readFailed.collectAsStateWithLifecycle()
 val now by vm.now.collectAsStateWithLifecycle()
 val demo by vm.demo.collectAsStateWithLifecycle()
 LaunchedEffect(vm){while(isActive){vm.refreshTime();delay(1000)}}
 var route by rememberSaveable {mutableIntStateOf(0)}
 var dayFocus by rememberSaveable {mutableStateOf<String?>(null)}
 val snackbar= remember { SnackbarHostState() }
 val scope= rememberCoroutineScope()
 BackHandler(enabled=route!=0){route=if(route in 4..6)3 else 0;if(route==0)dayFocus=null}
 val current=state
 if(current==null){
  Box(Modifier.fillMaxSize().background(Page).safeDrawingPadding().padding(24.dp)){
   Column(verticalArrangement=Arrangement.spacedBy(20.dp)){
    Text("NowUs",style=MaterialTheme.typography.headlineLarge)
    Text(error?:"正在打开我们的本地时光…")
    if(error!=null)Button(onClick=vm::retry){Text("重试加载")} else CircularProgressIndicator()
   }
  };return
 }
 LaunchedEffect(realAccount,pendingInviteCode,current.setupComplete){
  if(realAccount&&!pendingInviteCode.isNullOrBlank()&&current.setupComplete)route=6
 }
 val noteDraftScope = draftScopeKey ?: if(realAccount) "profile:${current.me.name}:${current.me.cityId}" else "local"
 if(!current.setupComplete){Onboarding(vm,current,error,saving,realAccount,pendingInviteCode,onPendingInviteConsumed);return}
 val tabStates=rememberSaveableStateHolder()
 Scaffold(
  containerColor=Page,
  snackbarHost={SnackbarHost(snackbar)},
  bottomBar={NowUsNavigation(if(route in 0..2)route else -1){selected->route=selected;dayFocus=null}}
 ){insets->
  tabStates.SaveableStateProvider(route){
   Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(top=if(route==0)0.dp else 12.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(when(route){0->2.dp;2->10.dp;else->12.dp})){
    if(realAccount&&route in 0..2&&(current.syncStale||current.sharingPaused))PrivacyStatusBanner(current,onRetry=vm::retry)
    if(readFailed)Button(onClick=vm::retry){Text("重试加载")}
    if(route!=2)error?.let { ErrorText(it) }
    if(route==0)Home(
     vm,current,now,error,saving,realAccount,pendingInviteCode,onPendingInviteConsumed,
     onSettings={route=3},onDay={instant->dayFocus=instant.toString();route=1},onNotes={route=2},onPair={route=6},
    )
    if(route==1)Timeline(current,now,dayFocus?.let(Instant::parse),onSettings={route=3})
    if(route==2)NotesPage(vm,current,now,error,saving,realAccount,onSettings={route=3},onDeleted={
     scope.launch {
      val result=snackbar.showSnackbar("便签已删除",actionLabel="撤销",withDismissAction=true)
      if(result==SnackbarResult.ActionPerformed)vm.undoDeleteNote()
     }
    },draftScopeKey=noteDraftScope)
    if(route==3)MySettingsDaylight(vm,current,error,saving,demo,realAccount,onLogout,{route=4},{route=5},{route=6},{route=0})
    if(route==4)MyRhythm(vm,current,error,saving,demo,realAccount,now,onLogout,{route=3})
    if(route==5)BrandPage(onBack={route=3})
    if(route==6)PairPage(vm,current,error,saving,realAccount,now,pendingInviteCode,onPendingInviteConsumed,onAccepted={route=0},onBack={route=3})
    if(!realAccount&&route==3)TextButton(onClick=onExitDemo){Text("返回登录")}
    if(realAccount&&pendingInviteCode!=null&&route==3){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("已保存邀请 $pendingInviteCode",color=Accent);TextButton(onClick=onPendingInviteConsumed){Text("清除")}}
    }
   }
  }
 }
}

@Composable fun ErrorText(error:String){Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(12.dp)){Text(error,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.onErrorContainer)}}
@Composable fun SectionCard(title:String,content:@Composable ColumnScope.()->Unit){
 Card(shape=RoundedCornerShape(10.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.titleMedium);content()}
 }
}
@Composable private fun Home(
 vm:AppViewModel,state:AppState,now:Instant,error:String?,saving:Boolean,realAccount:Boolean,
 pendingInviteCode:String?,onPendingInviteConsumed:()->Unit,onSettings:()->Unit,onDay:(Instant)->Unit,onNotes:()->Unit,
 onPair:()->Unit,
){
 val partner=state.partner.takeUnless{state.sharingPaused||state.syncStale}
 val visible=if(partner==state.partner)state else state.copy(partner=null,partnerSchedule=null,partnerTemporary=null,partnerNote=null)
 Row(Modifier.fillMaxWidth().padding(top=9.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
  androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.logo_lockup),contentDescription="NowUs",modifier=Modifier.width(108.dp).height(30.dp))
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
   StatusQuickAction(vm,state,now,error,saving,realAccount)
   IconButton(onClick=onSettings,modifier=Modifier.size(44.dp).semantics{contentDescription="打开我的设置"}){
    Icon(painterResource(app.nowus.android.R.drawable.icon_settings),null,tint=Accent)
   }
  }
 }
 if(partner!=null){
  val crossed=now.atZone(state.me.zone()).toLocalDate()!=now.atZone(partner.zone()).toLocalDate()
  Text(if(crossed)"你这里，已经是明天。" else "你的夜晚，他的早晨。",fontSize=24.sp,lineHeight=36.sp,color=Ink)
  Text("${state.me.name} 与 ${partner.name} · 各自生活，也彼此惦记",style=MaterialTheme.typography.bodyMedium,color=Muted)
 }else{
  Text("先过好自己的一天。",fontSize=24.sp,lineHeight=36.sp,color=Ink)
  Text("配对后，再把彼此的日常放在一起。",style=MaterialTheme.typography.bodyMedium,color=Muted)
 }
 ClockPair(visible,now,Modifier.padding(top=12.dp))
 if(partner!=null){
  Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.SpaceBetween){
   Text(offsetText(state.me,partner,now),fontSize=11.sp,color=Muted)
   Text(if(state.partnerSchedule==null)"作息待补充" else "活动按通常作息",fontSize=11.sp,color=Muted)
  }
  if(state.partnerSchedule==null)Text("活动与联系偏好保持未知；只有对方主动设置为可联系的时段会显示绿色细线。",style=MaterialTheme.typography.bodySmall,color=Muted)
  val partnerNote=visible.partnerNote
  if(partnerNote!=null){
   PaperNote(partnerNote,state.me,partner,now,full=false,modifier=Modifier.testTag("partner-note-preview").clickable(onClick=onNotes))
  }else if(state.sharingPaused||state.syncStale){
   Text(if(state.sharingPaused)"分享已暂停，便签与共同时间暂不显示。" else "暂时无法确认分享权限；对方的便签暂不可见。",style=MaterialTheme.typography.bodySmall,color=Muted)
  }else{
   PaperNote(Note("",now.toEpochMilli()),state.me,partner,now,full=false,empty=true,modifier=Modifier.testTag("partner-note-preview").clickable(onClick=onNotes))
  }
 }else if(state.paired){
  Text(if(state.sharingPaused)"分享已暂停，对方资料暂不可见。" else "配对已建立；对方尚未填写可共享的昵称与城市。",color=Muted,style=MaterialTheme.typography.bodySmall)
 }else{
  Surface(color=Tint,shape=RoundedCornerShape(17.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().clickable(onClick=onPair)){
   Row(Modifier.padding(horizontal=17.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Icon(painterResource(app.nowus.android.R.drawable.nowus_mark),null,tint=Accent,modifier=Modifier.size(33.dp))
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
     Text("给彼此留一个位置",color=Ink,fontSize=16.sp,fontWeight=FontWeight.Medium)
     Text("邀请另一半，把两地日常放在一起。",color=Muted,fontSize=12.sp,lineHeight=18.sp)
    }
    Icon(painterResource(app.nowus.android.R.drawable.icon_arrow),null,tint=Accent,modifier=Modifier.size(19.dp))
   }
  }
 }
 val windows by produceState<List<Window>?>(null,visible,now.epochSecond/60){
  value=null
  value=withContext(Dispatchers.Default){partner?.let{TimeEngine.commonWindows(now,now.plusSeconds(7*86400),visible.me,visible.schedule,visible.temporary,it,visible.partnerSchedule,visible.partnerTemporary)}?:emptyList()}
 }
 ContactWindowCard(visible,now,windows,onOpenTimeline=onDay)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun StatusQuickAction(vm:AppViewModel,state:AppState,now:Instant,error:String?,saving:Boolean,realAccount:Boolean){
 var open by rememberSaveable{mutableStateOf(false)}
 var available by rememberSaveable{mutableStateOf(true)}
 var minutes by rememberSaveable{mutableIntStateOf(60)}
 var followUsual by rememberSaveable{mutableStateOf(true)}
 val active=state.temporary?.takeIf{now.toEpochMilli() in it.fromMillis until it.untilMillis}
 val label=when{active==null->"我的状态";active.available->"现在愿意联系";else->"暂时不方便"}
 Surface(color=if(active==null)Color.Transparent else Tint,shape=RoundedCornerShape(14.dp),modifier=Modifier.heightIn(min=44.dp).clickable{vm.clearError();followUsual=active==null;available=active?.available?:true;open=true}){
  Row(Modifier.padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
   Icon(painterResource(if(active?.available==false)app.nowus.android.R.drawable.icon_pause else app.nowus.android.R.drawable.icon_sun),null,tint=if(active==null)Muted else Accent,modifier=Modifier.size(16.dp))
   Text(label,color=if(active==null)Muted else Accent,fontSize=11.sp,maxLines=1)
  }
 }
 if(open)ModalBottomSheet(onDismissRequest={open=false},containerColor=Panel,contentColor=Ink){
  Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   SheetTitle("此刻，按你的节奏"){open=false}
   Text("临时状态优先于通常联系偏好。",fontSize=11.sp,color=Muted)
   StatusChoice("现在愿意联系",app.nowus.android.R.drawable.icon_sun,!followUsual&&available){available=true;followUsual=false}
   StatusChoice("暂时不方便",app.nowus.android.R.drawable.icon_pause,!followUsual&&!available){available=false;followUsual=false}
   StatusChoice("跟随通常偏好",app.nowus.android.R.drawable.icon_day,followUsual){followUsual=true}
   Text("持续时间",fontSize=10.sp,color=Muted,modifier=Modifier.padding(top=2.dp))
   var durationExpanded by rememberSaveable{mutableStateOf(false)}
   Box(Modifier.fillMaxWidth()){
    OutlinedButton(onClick={durationExpanded=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Text(when(minutes){30->"30 分钟";60->"1 小时";else->"3 小时"},color=Ink)
      Text("⌄",fontSize=18.sp,color=Muted)
     }
    }
    DropdownMenu(expanded=durationExpanded,onDismissRequest={durationExpanded=false}){
     listOf(30,60,180).forEach{duration->DropdownMenuItem(text={Text(when(duration){30->"30 分钟";60->"1 小时";else->"3 小时"})},onClick={minutes=duration;durationExpanded=false})}
    }
   }
   if(error!=null)ErrorText(error)
   Button(enabled=!saving,onClick={if(followUsual)vm.resetTemporary{open=false}else vm.temporary(available,minutes){open=false}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("保存状态")}
   if(!realAccount)Text("本地体验 · 状态只保存到本机。",style=MaterialTheme.typography.bodySmall,color=Muted)
  }
 }
}

@Composable private fun StatusChoice(label:String,icon:Int,selected:Boolean,onClick:()->Unit){
 Surface(
  modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(onClick=onClick),
  color=if(selected)Tint else Panel,
  shape=RoundedCornerShape(10.dp),
  border=androidx.compose.foundation.BorderStroke(if(selected)1.2.dp else 1.dp,if(selected)Accent else Line),
 ){
  Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   Text(label,fontSize=12.sp,color=if(selected)Accent else Ink)
   Icon(painterResource(icon),null,tint=if(selected)Accent else Muted,modifier=Modifier.size(17.dp))
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NotesPage(vm:AppViewModel,state:AppState,now:Instant,error:String?,saving:Boolean,realAccount:Boolean,onSettings:()->Unit,onDeleted:()->Unit,draftScopeKey:String){
 var editorOpen by rememberSaveable{mutableStateOf(false)}
 var confirmDelete by rememberSaveable{mutableStateOf(false)}
 val context=LocalContext.current
 val noteDraftStore=remember(context){EncryptedNoteDraftStore(context)}
 val restoredDraft=remember(draftScopeKey,state.note?.updatedMillis){noteDraftStore.load(draftScopeKey)}
 var draft by rememberSaveable(draftScopeKey){mutableStateOf(restoredDraft?:state.note?.text.orEmpty())}
 var wasEdited by rememberSaveable(draftScopeKey){mutableStateOf(restoredDraft!=null&&restoredDraft!=state.note?.text)}
 var noteFocused by remember{mutableStateOf(false)}
 LaunchedEffect(state.note?.updatedMillis,draftScopeKey){if(!wasEdited)draft=state.note?.text.orEmpty()}
 val partner=state.partner.takeUnless{state.sharingPaused||state.syncStale}
 val received=if(partner==null)null else state.partnerNote
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
  Text("留给彼此",style=MaterialTheme.typography.headlineSmall,color=Ink)
  IconButton(onClick=onSettings,modifier=Modifier.size(40.dp).semantics{contentDescription="打开我的设置"}){Icon(painterResource(app.nowus.android.R.drawable.icon_settings),null,tint=Accent)}
 }
 Spacer(Modifier.height(4.dp))
 Text("把想说的话，轻轻放在这里。",style=MaterialTheme.typography.bodyMedium,color=Muted)
 when{
  state.syncStale&&realAccount->SectionCard("同步暂不可用"){Text("无法确认分享权限；对方留言暂不可见。联网后重试同步。",color=Muted);OutlinedButton(onClick=vm::retry){Text("重试同步")}}
  state.sharingPaused&&realAccount->Text("分享已暂停，对方留言暂不显示。",style=MaterialTheme.typography.bodySmall,color=Muted)
  partner==null&&realAccount&&state.paired->Text("对方资料尚未同步，留言保持未知。",style=MaterialTheme.typography.bodySmall,color=Muted)
  received!=null->PaperNote(received,state.me,partner!!,now,full=true,modifier=Modifier.testTag("partner-note-full"))
  partner!=null->PaperNote(Note("",now.toEpochMilli()),state.me,partner,now,full=true,empty=true,modifier=Modifier.testTag("partner-note-full"))
  else->SectionCard("尚未配对"){Text("可以先写一张便签，配对后再共享。",color=Muted)}
 }
 Column(Modifier.fillMaxWidth()) {
 Row(Modifier.fillMaxWidth().padding(top=0.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
  Text("♡  看见就好，不用急着回。",color=Muted,fontSize=12.sp)
 }
  Spacer(Modifier.height(20.dp))
 Surface(color=Panel,shape=RoundedCornerShape(15.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().testTag("own-note-surface")){
  Column(Modifier.padding(horizontal=17.dp,vertical=13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Text("我的便签 · ${partner?.let{"给 ${it.name}"}?:"尚未共享"}",color=Muted,fontSize=12.sp)
    if(state.note!=null)IconButton(onClick={if(!wasEdited){draft=state.note.text;wasEdited=true};vm.clearError();editorOpen=true},modifier=Modifier.size(36.dp).semantics{contentDescription="编辑我的便签"}){Icon(painterResource(app.nowus.android.R.drawable.icon_edit),null,tint=Accent)}
   }
   Text(state.note?.text?:"今天有什么小事，想让对方知道？",color=if(state.note==null)Muted else Ink,fontFamily=FontFamily.Serif,fontSize=18.sp,lineHeight=32.sp)
   if(state.note!=null)TextButton(enabled=!saving,onClick={confirmDelete=true},contentPadding=PaddingValues(horizontal=0.dp),modifier=Modifier.heightIn(min=38.dp)){Text("删除我的便签",fontSize=11.sp,color=Muted)}
  }
 }
 Spacer(Modifier.height(15.dp))
 Button(onClick={
  if(!wasEdited||draft==state.note?.text)draft=state.note?.text.orEmpty()
  vm.clearError();editorOpen=true
 },modifier=Modifier.fillMaxWidth().height(48.dp)){
  Text(if(state.note==null)"写下第一张便签" else "写一张新便签")
 }
 Spacer(Modifier.height(10.dp))
 Text("每人保留一张 · 新便签替换旧便签 · 最多 120 字",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodySmall,color=Muted)
 }
 if(error!=null&&!editorOpen)ErrorText(error)
 if(confirmDelete)AlertDialog(
  onDismissRequest={confirmDelete=false},title={Text("删除这张便签？")},
  text={Text("删除后，对方将看不到你的这张便签。你可以随时再写一张。")},
   confirmButton={TextButton(enabled=!saving,onClick={vm.deleteNote{confirmDelete=false;draft="";wasEdited=false;noteDraftStore.clear(draftScopeKey);onDeleted()}}){Text("删除留言")}},
  dismissButton={TextButton(onClick={confirmDelete=false}){Text("再想想")}},
 )
 if(editorOpen)ModalBottomSheet(onDismissRequest={editorOpen=false},containerColor=Panel,contentColor=Ink){
  Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   SheetTitle("写给" + (partner?.name ?: "对方") + "的便签"){editorOpen=false}
    Text("给${partner?.name?:"对方"}的留言",color=Muted,style=MaterialTheme.typography.bodySmall)
   androidx.compose.foundation.text.BasicTextField(
    value=draft,onValueChange={draft=it;wasEdited=true;noteDraftStore.save(draftScopeKey,it)},
     modifier=Modifier.fillMaxWidth().heightIn(min=155.dp).clip(RoundedCornerShape(12.dp)).background(Paper).border(if(noteFocused)1.5.dp else 1.dp,if(noteFocused)Accent else PaperBottom,RoundedCornerShape(12.dp)).onFocusChanged{noteFocused=it.isFocused}.padding(13.dp).testTag("noteDraft"),
    textStyle=androidx.compose.ui.text.TextStyle(color=PaperInk,fontFamily=FontFamily.Serif,fontSize=21.sp,lineHeight=40.sp),
    decorationBox={inner->Box(Modifier.fillMaxWidth().heightIn(min=125.dp)){if(draft.isEmpty())Text("今天想告诉对方什么？",color=PaperInk.copy(alpha=.6f),fontFamily=FontFamily.Serif,fontSize=21.sp,lineHeight=40.sp);inner()}},
   )
   Text("${draft.codePointCount(0,draft.length)} / 120 字",Modifier.fillMaxWidth(),textAlign=TextAlign.End,color=if(draft.codePointCount(0,draft.length)>120)MaterialTheme.colorScheme.error else Muted,fontSize=11.sp)
   if(error!=null)ErrorText(error)
    Button(enabled=!saving,onClick={vm.saveNote(draft.trim()){editorOpen=false;wasEdited=false;noteDraftStore.clear(draftScopeKey)}} ,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Text("贴上这张便签")}
    Text(if(realAccount)"草稿保存在本机；贴上后同步给当前配对伴侣。" else "草稿自动保存在本机，关闭后也不会丢失。贴上后替换当前便签。",style=MaterialTheme.typography.bodySmall,color=Muted,modifier=Modifier.padding(bottom=20.dp))
 }
}
}

@Composable private fun SyncStatus(state:AppState){
 val last=state.lastSyncMillis
 val text=when{
  last==null->"正在与账号同步…"
  state.syncStale->"同步失败或离线 · 最后成功同步 ${shortDateTime(Instant.ofEpochMilli(last),state.me)}"
  else->"已同步 · ${shortDateTime(Instant.ofEpochMilli(last),state.me)}"
 }
 Text(text,style=MaterialTheme.typography.bodySmall,color=if(state.syncStale)MaterialTheme.colorScheme.error else Muted)
 if(state.sharingPaused)Text("分享已暂停 · 对方资料当前不可见",style=MaterialTheme.typography.bodySmall,color=Muted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun TemporaryCard(vm:AppViewModel,state:AppState,now:Instant,demo:Boolean,error:String?,saving:Boolean){
 var open by rememberSaveable {mutableStateOf(false)}
 var available by rememberSaveable {mutableStateOf(true)}
 var minutes by rememberSaveable {mutableIntStateOf(30)}
 SectionCard("此刻的联系意愿"){
  val temporary=state.temporary
  if(temporary!=null && now.toEpochMilli() in temporary.fromMillis until temporary.untilMillis){
   Text("主动设置：${if(temporary.available) "可联系" else "忙碌"} · 至 ${localTime(Instant.ofEpochMilli(temporary.untilMillis),state.me)}",color=Accent)
   TextButton(enabled=!saving,onClick=vm::resetTemporary){Text("恢复通常联系偏好")}
  }else Text("当前按通常联系偏好；活动与联系意愿独立。",color=Muted)
  if(demo)Text("退出固定演示后，可设置真实联系意愿。",style=MaterialTheme.typography.bodySmall,color=Muted)
  Button(enabled=!demo,onClick={vm.clearError();open=true}){Text("设置临时联系意愿")}
  if(error!=null&&!open)ErrorText(error)
 }
 if(open)EditorDialog("设置临时联系意愿",{open=false}){
  Text("只改变联系偏好，不改变上班、休息或睡眠活动。")
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
   FilterChip(selected=available,onClick={available=true},label={Text("可联系")})
   FilterChip(selected=!available,onClick={available=false},label={Text("忙碌")})
  }
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   listOf(30,60,180).forEach{duration->FilterChip(selected=minutes==duration,onClick={minutes=duration},label={Text("${duration}分")})}
  }
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.temporary(available,minutes){open=false}}){Text("保存临时意愿")}
 }
}
@Composable private fun NoteCard(vm:AppViewModel,state:AppState,error:String?,saving:Boolean,realAccount:Boolean){
 var open by rememberSaveable {mutableStateOf(false)}
 var draft by rememberSaveable {mutableStateOf(state.note?.text.orEmpty())}
 val partner=state.partner
 val partnerNote=state.partnerNote
 Card(shape=RoundedCornerShape(10.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Text(if(partnerNote!=null)"${partner?.name?:"对方"}的留言" else "留一句话",style=MaterialTheme.typography.titleMedium)
   }
   if(partnerNote!=null){
    Text(partnerNote.text,style=MaterialTheme.typography.bodyLarge,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
    Text(shortDateTime(Instant.ofEpochMilli(partnerNote.updatedMillis),state.me),style=MaterialTheme.typography.bodySmall,color=Muted)
   }else Text(if(partner!=null)"对方还没有留下当前留言。" else "给下一次相遇，留一句期待。",style=MaterialTheme.typography.bodyMedium,color=Muted)
   if(state.note!=null){
    Text("我的当前留言",style=MaterialTheme.typography.bodySmall,color=Muted)
    Text(state.note.text,style=MaterialTheme.typography.bodyMedium)
   }
   OutlinedButton(onClick={vm.clearError();draft=state.note?.text.orEmpty();open=true},shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Accent),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
    Text(if(state.note!=null)"编辑留言" else if(partnerNote!=null&&partner!=null)"回${partner.name}一句" else "写留言",fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
   }
   if(state.note!=null)TextButton(enabled=!saving,onClick={vm.saveNote(""){draft=""}}){Text("删除留言")}
   Text(if(realAccount)"当前留言仅配对伴侣可见" else "本地体验 · 留言仅保存到本机",style=MaterialTheme.typography.bodySmall,color=Muted)
  }
 }
 if(open)EditorDialog("留一句话",{open=false}){
  OutlinedTextField(draft,{draft=it},label={Text("留言")},supportingText={Text("${draft.codePointCount(0,draft.length)} / 120 字 · ${if(realAccount)"配对伴侣可见" else "仅在本机"}")},modifier=Modifier.fillMaxWidth().testTag("noteDraft"),minLines=3)
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.saveNote(draft){open=false}}){Text("保存留言")}
 }
}

@Composable private fun Invitation(
 vm:AppViewModel,state:AppState,saving:Boolean,error:String?,realAccount:Boolean=false,
 pendingInviteCode:String?=null,onPendingInviteConsumed:()->Unit={},onAccepted:()->Unit={},
){
 if(realAccount){RealInvitation(vm,state,saving,error,pendingInviteCode,onPendingInviteConsumed,onAccepted);return}
 var code by rememberSaveable {mutableStateOf("")}
 var partnerName by rememberSaveable {mutableStateOf("")}
 var partnerCity by rememberSaveable {mutableStateOf("new-york")}
 Text("本机演示邀请 · 填写模拟对方的昵称和城市，接受后作息仍未知。",style=MaterialTheme.typography.bodySmall,color=Muted)
 val invite=state.invite
 if(invite!=null){Text("邀请码：${invite.code}",style=MaterialTheme.typography.titleMedium)
 Text(if(invite.revoked)"已撤销" else "有效至 ${shortDateTime(Instant.ofEpochMilli(invite.expiresMillis),state.me)}（真实时间）",style=MaterialTheme.typography.bodySmall)
 if(!invite.revoked)TextButton(enabled=!saving,onClick=vm::revokeInvite){Text("撤销演示邀请")}}
 Button(enabled=!saving,onClick=vm::createInvite){Text(if(invite==null)"创建演示邀请" else "更新演示邀请")}
 OutlinedTextField(code,{code=it},label={Text("输入本机演示邀请码")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 ProfileFields(partnerName,{partnerName=it},partnerCity,{partnerCity=it})
 Button(enabled=!saving,onClick={vm.acceptInvite(code,Profile(partnerName.trim(),partnerCity),onAccepted)}){Text("接受演示邀请")}
 if(error!=null)ErrorText(error)
}

@Composable private fun RealInvitation(
 vm:AppViewModel,state:AppState,saving:Boolean,error:String?,pendingInviteCode:String?,onPendingInviteConsumed:()->Unit,onAccepted:()->Unit,
){
 var code by rememberSaveable {mutableStateOf(pendingInviteCode.orEmpty())}
 val preview by vm.invitePreview.collectAsStateWithLifecycle()
 val context=LocalContext.current
 LaunchedEffect(pendingInviteCode){
  if(!pendingInviteCode.isNullOrBlank()){
   code=pendingInviteCode
   if(preview?.code!=pendingInviteCode)vm.previewInvite(pendingInviteCode)
  }
 }
 SectionCard("创建我的邀请"){
  Text("有效期 24 小时。对方接受前可查看你的昵称、城市和共享范围。接受后双方都能暂停分享或解除配对。")
  val invite=state.invite
  if(invite!=null){
   Text("邀请码：${invite.code}",style=MaterialTheme.typography.titleMedium)
   Text("有效至 ${shortDateTime(Instant.ofEpochMilli(invite.expiresMillis),state.me)}（服务器时间）",style=MaterialTheme.typography.bodySmall,color=Muted)
   if(invite.code.isNotBlank()){
    val link="nowus://invite/${invite.code}"
    Text(link,style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={
     val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"加入 NowUs 与我同步跨时区日常：$link（邀请码 ${invite.code}）")}
     context.startActivity(Intent.createChooser(send,"分享 NowUs 邀请"))
    }){Text("分享邀请链接")}
   }else Text("此设备未保存邀请码。可撤销并重建邀请。",style=MaterialTheme.typography.bodySmall,color=Muted)
   TextButton(enabled=!saving,onClick=vm::revokeInvite){Text("撤销邀请")}
  }
  Button(enabled=!saving,onClick=vm::createInvite){Text(if(invite==null)"创建邀请" else "撤销并重建邀请")}
 }
 SectionCard("接受伴侣邀请"){
  Text("输入邀请码，或打开伴侣分享的 NowUs 邀请链接。确认前会显示邀请人和共享范围。")
  OutlinedTextField(code,{code=it.uppercase().filter{ch->ch.isLetterOrDigit()}.take(10)},label={Text("10 位邀请码")},modifier=Modifier.fillMaxWidth().testTag("realInviteCode"),singleLine=true)
  Button(enabled=!saving&&code.length==10,onClick={vm.previewInvite(code)}){Text("查看邀请")}
  if(preview!=null && preview!!.code==code){
   HorizontalDivider()
   Text("邀请来自：${preview!!.inviter.name} · ${preview!!.inviter.cityName()}",style=MaterialTheme.typography.titleMedium)
   Text("共享范围：${preview!!.scope.joinToString("、")}")
   Text("有效至 ${shortDateTime(Instant.ofEpochMilli(preview!!.expiresMillis),state.me)}（服务器时间）",style=MaterialTheme.typography.bodySmall,color=Muted)
   Text("接受后，对方也会看到你主动填写并保存的资料。未填写内容保持未知。")
   Button(enabled=!saving,onClick={vm.acceptInvite(preview!!.code,null){onPendingInviteConsumed();onAccepted()}}){Text("接受并配对")}
  }
  if(error!=null)ErrorText(error)
 }
}

@Composable private fun Onboarding(vm:AppViewModel,state:AppState,error:String?,saving:Boolean,realAccount:Boolean=false,pendingInviteCode:String?=null,onPendingInviteConsumed:()->Unit={}){
 val readFailed by vm.readFailed.collectAsStateWithLifecycle()
 var step by rememberSaveable {mutableIntStateOf(if(state.schedule!=null)2 else if(state.me.name.isNotBlank())1 else 0)}
 var name by rememberSaveable {mutableStateOf(state.me.name)}
 var city by rememberSaveable {mutableStateOf(state.me.cityId)}
 var scheduleJson by rememberSaveable {mutableStateOf(Json.encodeToString(state.schedule?:defaultSchedule(city)))}
 val schedule=Json.decodeFromString<Schedule>(scheduleJson)
 BackHandler(enabled=step>0){step--}
 Column(Modifier.fillMaxSize().background(Page).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Text("NowUs",color=Accent,style=MaterialTheme.typography.headlineLarge)
  Text("${step+1} / 4 · ${listOf("认识你","我的节奏","留一个位置","准备好了")[step]}",style=MaterialTheme.typography.titleMedium)
  Text(if(realAccount)"真实账号 · 资料保存到账号；不导入演示伴侣资料" else "本地体验 · 邀请和对方仅作演示",color=Muted)
  if(error!=null)ErrorText(error)
  when(step){
   0->{Text("从你的城市开始",style=MaterialTheme.typography.headlineSmall);ProfileFields(name,{name=it},city,{city=it;if(state.schedule==null)scheduleJson=Json.encodeToString(defaultSchedule(it))});Button(enabled=!saving,onClick={vm.saveProfile(Profile(name.trim(),city)){step=1}}){Text("下一步：我的节奏")}}
   1->{Text("让作息替你轻声说明",style=MaterialTheme.typography.headlineSmall);if(realAccount&&state.schedule==null)Text("先从按城市提供的可编辑作息建议开始；调整并保存后才会同步。",style=MaterialTheme.typography.bodySmall,color=Muted);RhythmFields(schedule,city,{scheduleJson=Json.encodeToString(it)});Button(enabled=!saving,onClick={vm.saveSchedule(schedule){step=2}}){Text("保存节奏并继续")}}
   2->{SectionCard(if(realAccount)"伴侣邀请" else "演示邀请"){
    if(state.partner==null&&!state.paired)Invitation(vm,state,saving,error,realAccount,pendingInviteCode,onPendingInviteConsumed){step=3}
    else {Text(if(realAccount)"已与 ${state.partner?.name?:"伴侣"} 配对。对方未填写的资料会保持未知。" else "已模拟配对 ${state.partner?.name}，对方作息待补充");Button(onClick={step=3}){Text("继续")}}
   };if(state.partner==null&&!state.paired)OutlinedButton(onClick={vm.clearError();step=3}){Text(if(realAccount)"稍后再配对" else "先独自使用")}}
   3->{Text("我们的时间，从此开始",style=MaterialTheme.typography.headlineSmall);Text(if(realAccount)"你的资料与节奏将保存在账号中；伴侣资料只在配对后共享。" else if(state.partner==null)"已保存你的资料与节奏。可以先独自使用，稍后再尝试演示配对。" else "本机演示配对已完成。只有显式填入示例后才会计算对方作息。");Button(enabled=!saving,onClick={vm.completeSetup()}){Text("开始使用")}}
  }
  if(error!=null)ErrorText(error)
  if(readFailed)Button(onClick=vm::retry){Text("重试加载")}
  if(step>0)TextButton(onClick={step--}){Text("上一步")}
 }
}

@Composable private fun ProfileFields(name:String,onName:(String)->Unit,city:String,onCity:(String)->Unit){
 OutlinedTextField(name,onName,label={Text("昵称")},supportingText={Text("1–20 个字符")},modifier=Modifier.fillMaxWidth().testTag("profileName"),singleLine=true)
 var expanded by remember {mutableStateOf(false)}
 Box{
  OutlinedButton(onClick={expanded=true}){Text("城市：${Cities.byId(city)?.name?:"请选择"} ▾")}
  DropdownMenu(expanded,{expanded=false}){Cities.all.forEach{item->DropdownMenuItem(text={Text("${item.name} · ${item.zoneId}")},onClick={onCity(item.id);expanded=false})}}
 }
 Text("时间会按城市的真实时区与夏令时计算。",style=MaterialTheme.typography.bodySmall,color=Muted)
}
@Composable private fun RhythmFields(schedule:Schedule,cityId:String,onChange:(Schedule)->Unit){
 var rest by rememberSaveable {mutableStateOf(false)}
 var pendingTemplate by remember { mutableStateOf<RoutineTemplate?>(null) }
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  RoutineTemplate.entries.forEach { template->
   FilterChip(selected=schedule.templateId==template.id,onClick={pendingTemplate=template},label={Text("${template.title}模板")})
  }
 }
 Text("已按${Cities.byId(cityId)?.name?:"所选城市"}所在地区给出参考时间。模板可逐项修改，不代表每个人的实际作息。切换模板会替换工作日与休息日两张作息表。",style=MaterialTheme.typography.bodySmall,color=Muted)
 if(pendingTemplate!=null)AlertDialog(
  onDismissRequest={pendingTemplate=null},
  title={Text("应用${pendingTemplate!!.title}模板？")},
  text={Text("这会替换尚未保存的工作日和休息日时段。应用后仍可逐项修改。")},
  confirmButton={TextButton(onClick={onChange(RoutineTemplates.forCity(cityId,pendingTemplate!!));pendingTemplate=null}){Text("应用模板")}},
  dismissButton={TextButton(onClick={pendingTemplate=null}){Text("继续编辑")}}
 )
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  FilterChip(selected=!rest,onClick={rest=false},label={Text("工作日")})
  FilterChip(selected=rest,onClick={rest=true},label={Text("休息日")})
 }
 Text(if(rest)"休息日 · 周六、周日" else "工作日 · 周一至周五",style=MaterialTheme.typography.titleMedium)
 val rhythm=if(rest)schedule.rest else schedule.weekday
 val dayKey=if(rest)"rest" else "weekday"
 fun update(value:Rhythm){onChange(if(rest)schedule.copy(rest=value)else schedule.copy(weekday=value))}
 Text("滚动选择每段时间。睡觉可以跨夜；其他时段按当天安排且不可互相重叠。未安排的空档会显示为“未安排”。",style=MaterialTheme.typography.bodySmall,color=Muted)
 if(rhythm.blocks.isEmpty()){
  Text("这份作息来自旧版单段安排。选择另一种模板可补充早餐、通勤、午餐、午休等可编辑时段。",style=MaterialTheme.typography.bodySmall,color=Muted)
  TimePair("睡眠","rhythm-$dayKey-sleep",rhythm.sleepStart,rhythm.sleepEnd,{update(rhythm.copy(sleepStart=it))},{update(rhythm.copy(sleepEnd=it))})
  OutlinedTextField(rhythm.activity,{update(rhythm.copy(activity=it))},label={Text("活动名称")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  TimePair("活动","rhythm-$dayKey-activity",rhythm.activityStart,rhythm.activityEnd,{update(rhythm.copy(activityStart=it))},{update(rhythm.copy(activityEnd=it))})
 }else{
  rhythm.blocks.forEach { block->
   val timeTitle=if(block.id=="sleep")"睡眠" else block.label
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedTextField(block.label,{label->update(updateBlocks(rhythm,rhythm.blocks.map { if(it.id==block.id)it.copy(label=label)else it }))},label={Text("时段名称")},modifier=Modifier.weight(1f).testTag("rhythm-$dayKey-${block.id}-label"),singleLine=true)
    if(block.id!="sleep")TextButton(onClick={update(updateBlocks(rhythm,rhythm.blocks.filterNot { it.id==block.id }))}){Text("删除")}
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Text("类别",style=MaterialTheme.typography.bodySmall,color=Muted)
    RoutineCategoryPicker(block.category,"rhythm-$dayKey-${block.id}-category"){category->
     update(updateBlocks(rhythm,rhythm.blocks.map { if(it.id==block.id)it.copy(category=category)else it }))
    }
   }
   TimePair(timeTitle,"rhythm-$dayKey-${block.id}",block.start,block.end,
    {time->update(updateBlocks(rhythm,rhythm.blocks.map { if(it.id==block.id)it.copy(start=time)else it }))},
    {time->update(updateBlocks(rhythm,rhythm.blocks.map { if(it.id==block.id)it.copy(end=time)else it }))})
  }
  OutlinedButton(enabled=rhythm.blocks.size<20,onClick={
   val occupied=(0 until 1440).toSet().filter { minute->rhythm.blocks.any { TimeEngine.contains(minute,it.start,it.end) } }.toSet()
   val start=(0..1380 step 30).firstOrNull { candidate->(candidate until candidate+30).none { it in occupied } }?:1320
   val id="custom-${(rhythm.blocks.mapNotNull { it.id.substringAfter("custom-","").toIntOrNull() }.maxOrNull()?:0)+1}"
   update(updateBlocks(rhythm,rhythm.blocks+RoutineBlock(id,"新时段",String.format(Locale.ROOT,"%02d:%02d",start/60,start%60),String.format(Locale.ROOT,"%02d:%02d",(start+30)/60,(start+30)%60))))
  },modifier=Modifier.fillMaxWidth()){Text(if(rhythm.blocks.size<20)"＋ 添加时段" else "已达到 20 个时段上限")}
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("通常联系偏好",Modifier.weight(1f));Switch(checked=rhythm.contactKnown,onCheckedChange={update(rhythm.copy(contactKnown=it))})}
 Text(if(rhythm.contactKnown)"以下时段愿意联系，不推断实时状态" else "联系意愿保持未知")
 if(rhythm.contactKnown)TimePair("联系","rhythm-$dayKey-contact",rhythm.contactStart,rhythm.contactEnd,{update(rhythm.copy(contactStart=it))},{update(rhythm.copy(contactEnd=it))})
 Text("保存时同时校验工作日和休息日模板。联系偏好仍可与上课或工作时段重合，但不能覆盖睡觉时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
}

@Composable private fun RoutineCategoryPicker(category:RoutineCategory,testTag:String,onCategoryChange:(RoutineCategory)->Unit){
 var expanded by rememberSaveable(testTag){mutableStateOf(false)}
 Box {
  AssistChip(
   onClick={expanded=true},
   label={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Box(Modifier.size(13.dp).background(routineCategoryColor(category),CircleShape))
    Text(category.title,color=Ink)
   }},
   colors=AssistChipDefaults.assistChipColors(containerColor=routineCategoryColor(category).copy(alpha=.68f),labelColor=Ink),
   modifier=Modifier.testTag(testTag)
  )
  DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
   RoutineCategory.entries.filter { it!=RoutineCategory.UNSCHEDULED }.forEach { option->
    DropdownMenuItem(
     text={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
      RadioButton(selected=option==category,onClick=null)
      Box(Modifier.size(13.dp).background(routineCategoryColor(option),CircleShape))
      Text(option.title)
     }},
     onClick={onCategoryChange(option);expanded=false},
     modifier=Modifier.testTag("routine-category-${option.name.lowercase(Locale.ROOT)}")
    )
   }
  }
 }
}

private fun updateBlocks(rhythm:Rhythm,blocks:List<RoutineBlock>):Rhythm{
 val sleep=blocks.first { it.id=="sleep" }
 val primary=blocks.firstOrNull { it.id.startsWith("morning-") }
 return rhythm.copy(sleepStart=sleep.start,sleepEnd=sleep.end,activity=primary?.label?:"休息",activityStart=primary?.start?:"10:00",activityEnd=primary?.end?:"12:00",blocks=blocks)
}
@Composable private fun TimePair(title:String,tagPrefix:String,start:String,end:String,onStart:(String)->Unit,onEnd:(String)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text(title,style=MaterialTheme.typography.titleSmall)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   TimeWheelField("${title}开始",start,"$tagPrefix-start",onStart,Modifier.weight(1f))
   TimeWheelField("${title}结束",end,"$tagPrefix-end",onEnd,Modifier.weight(1f))
  }
 }
}
@Composable private fun TimeWheelField(label:String,time:String,testTag:String,onTimeChange:(String)->Unit,modifier:Modifier=Modifier){
 val minute=TimeEngine.parseMinute(time)?.coerceIn(0,1439)?:0
 val displayedTime=String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60)
 var showPicker by rememberSaveable { mutableStateOf(false) }
 Column(modifier){
  Text(label,style=MaterialTheme.typography.bodySmall,color=Muted)
  OutlinedButton(onClick={showPicker=true},modifier=Modifier.fillMaxWidth().testTag(testTag)){
   Text(displayedTime,style=MaterialTheme.typography.titleMedium)
  }
 }
 if(showPicker)TimeWheelDialog(label,displayedTime,{onTimeChange(it);showPicker=false},{showPicker=false})
}

@Composable private fun TimeWheelDialog(label:String,initialTime:String,onConfirm:(String)->Unit,onDismiss:()->Unit){
 val initialMinute=TimeEngine.parseMinute(initialTime)?.coerceIn(0,1439)?:0
 var hour by rememberSaveable(initialTime){mutableIntStateOf(initialMinute/60)}
 var minute by rememberSaveable(initialTime){mutableIntStateOf(initialMinute%60)}
 val displayedTime=String.format(Locale.ROOT,"%02d:%02d",hour,minute)
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text("设置$label")},
  text={
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
     TimeWheelColumn("小时",24,hour,"time-wheel-hour"){hour=it}
     Text(":",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(horizontal=8.dp))
     TimeWheelColumn("分钟",60,minute,"time-wheel-minute"){minute=it}
    }
    Text(displayedTime,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
   }
  },
  confirmButton={TextButton(onClick={onConfirm(displayedTime)}){Text("确定")} },
  dismissButton={TextButton(onClick=onDismiss){Text("取消")} }
 )
}

@Composable private fun TimeWheelColumn(label:String,count:Int,selected:Int,testTag:String,onSelected:(Int)->Unit){
 val listState=rememberLazyListState(initialFirstVisibleItemIndex=selected.coerceIn(0,count-1))
 val flingBehavior=rememberSnapFlingBehavior(listState,snapPosition=SnapPosition.Center)
 LaunchedEffect(listState,count){
  snapshotFlow{listState.firstVisibleItemIndex}.distinctUntilChanged().collect{index->
   onSelected(index.coerceIn(0,count-1))
  }
 }
 Column(horizontalAlignment=Alignment.CenterHorizontally){
  Text(label,style=MaterialTheme.typography.labelMedium,color=Muted)
  Box(Modifier.width(88.dp).height(240.dp)){
   LazyColumn(
    state=listState,
    flingBehavior=flingBehavior,
    modifier=Modifier.fillMaxSize().testTag(testTag),
    horizontalAlignment=Alignment.CenterHorizontally
   ){
    items(count+4){itemIndex->
     val value=itemIndex-2
     Box(Modifier.fillMaxWidth().height(48.dp),contentAlignment=Alignment.Center){
      if(value in 0 until count){
       val isSelected=value==selected
       Text(
        String.format(Locale.ROOT,"%02d",value),
        style=if(isSelected)MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
        color=if(isSelected)MaterialTheme.colorScheme.primary else Muted
       )
      }
     }
    }
   }
   Box(
    Modifier.align(Alignment.Center).fillMaxWidth().height(48.dp)
     .border(1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(8.dp))
   )
  }
 }
}

@Composable private fun MySettings(
 vm:AppViewModel,state:AppState,error:String?,saving:Boolean,demo:Boolean,realAccount:Boolean,now:Instant,onLogout:()->Unit,
 pendingInviteCode:String?,onPendingInviteConsumed:()->Unit,onOpenRhythm:()->Unit,onOpenBrand:()->Unit,onOpenPair:()->Unit,onBack:()->Unit,
){
 var profileOpen by rememberSaveable{mutableStateOf(false)}
 var confirmPause by rememberSaveable{mutableStateOf(false)}
 var confirmUnpair by rememberSaveable{mutableStateOf(false)}
 var name by rememberSaveable{mutableStateOf(state.me.name)}
 var city by rememberSaveable{mutableStateOf(state.me.cityId)}
 val imported by vm.localImport.collectAsStateWithLifecycle()
 PageHeader("我的设置",onBack)
 Text("各自的日常，也要有安心的边界。",style=MaterialTheme.typography.bodyMedium,color=Muted)
 SectionCard(state.me.name.ifBlank{"我的资料"}){
  Text("${state.me.cityName()} · ${state.me.zone().id}",color=Muted)
  Text(if(realAccount)"配对后，对方可见你主动填写并分享的资料。" else "资料与节奏保存在本机。",style=MaterialTheme.typography.bodySmall,color=Muted)
  TextButton(onClick={vm.clearError();profileOpen=true}){Text("编辑昵称与城市")}
 }
 Surface(color=Panel,shape=RoundedCornerShape(12.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().clickable(onClick=onOpenRhythm)){
  Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Text("我的节奏",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=Ink)
   Text("工作日与休息日作息",color=Muted,fontSize=11.sp)
   Icon(painterResource(app.nowus.android.R.drawable.icon_arrow),null,tint=Muted,modifier=Modifier.size(19.dp))
  }
 }
 TemporaryCard(vm,state,now,demo,error,saving)
 SectionCard("分享"){
  Text(if(state.sharingPaused)"分享已暂停，对方的资料暂不可见。" else "仅当前配对伴侣可以读取你主动填写的城市、作息、联系偏好与便签。",style=MaterialTheme.typography.bodySmall,color=Muted)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   Column(Modifier.weight(1f)){
    Text(if(state.sharingEnabled&&!state.sharingPaused)"正在分享" else "分享已暂停",color=Ink)
    Text("你可以随时恢复或暂停。",color=Muted,fontSize=11.sp)
   }
   Switch(checked=state.sharingEnabled&&!state.sharingPaused,onCheckedChange={enabled->if(enabled)vm.setSharing(true) else confirmPause=true},enabled=!saving)
  }
 }
 if(realAccount){
  SectionCard("邀请与配对"){
   Text("邀请前会显示对方与共享范围；双方确认后才建立配对。",style=MaterialTheme.typography.bodySmall,color=Muted)
   OutlinedButton(onClick=onOpenPair,modifier=Modifier.fillMaxWidth()){Text(if(state.paired)"查看配对与邀请" else "邀请与配对")}
   if(state.paired)TextButton(enabled=!saving,onClick={confirmUnpair=true}){Text("解除配对")}
  }
  SectionCard("账号与同步"){
   SyncStatus(state)
   if(state.syncStale)OutlinedButton(onClick=vm::retry){Text("重试同步")}
   Text("最后成功同步：${state.lastSyncMillis?.let{shortDateTime(Instant.ofEpochMilli(it),state.me)}?:"尚未成功"}",style=MaterialTheme.typography.bodySmall,color=Muted)
   TextButton(enabled=!saving,onClick=onLogout){Text("退出登录")}
  }
  SectionCard("导入本机体验资料"){
   Text("只导入你自己的昵称、城市、作息和留言；不上传模拟伴侣或演示时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
   OutlinedButton(enabled=!saving,onClick=vm::inspectLocalImport){Text("检查可导入资料")}
  }
 }else{
  SectionCard("本地体验"){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("固定时间演示",Modifier.weight(1f));Switch(checked=demo,onCheckedChange=vm::setDemo)}
   Text("演示时钟独立于真实本地体验数据与邀请有效期。",style=MaterialTheme.typography.bodySmall,color=Muted)
   if(state.partner!=null&&state.partnerSchedule==null)Button(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}
  }
  SectionCard("邀请与配对"){
   Text("本地演示邀请只保存在这台设备，不会创建真实账号配对。",style=MaterialTheme.typography.bodySmall,color=Muted)
   OutlinedButton(onClick=onOpenPair,modifier=Modifier.fillMaxWidth()){Text(if(state.paired)"查看配对与邀请" else "邀请与配对")}
  }
  if(state.paired)SectionCard("分享与配对"){
   Text("演示伴侣资料仅在本机体验模式内可见。",style=MaterialTheme.typography.bodySmall,color=Muted)
   TextButton(enabled=!saving,onClick={confirmUnpair=true}){Text("解除演示配对")}
  }
 }
 SectionCard("品牌"){
  Row(Modifier.fillMaxWidth().clickable(onClick=onOpenBrand).padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   Text("图标与 Logo",color=Ink)
   Icon(painterResource(app.nowus.android.R.drawable.icon_arrow),null,tint=Muted,modifier=Modifier.size(19.dp))
  }
 }
 if(error!=null)ErrorText(error)
 if(profileOpen)EditorDialog("编辑我的资料",{profileOpen=false}){
  ProfileFields(name,{name=it},city,{city=it})
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.saveProfile(Profile(name.trim(),city)){profileOpen=false}}){Text("保存资料")}
 }
 if(confirmPause)AlertDialog(
  onDismissRequest={confirmPause=false},title={Text("暂停分享日常？")},
  text={Text("对方暂时看不到你的作息、联系偏好和便签。你可以随时恢复分享。")},
  confirmButton={TextButton(enabled=!saving,onClick={vm.setSharing(false);confirmPause=false}){Text("暂停分享")}},
  dismissButton={TextButton(onClick={confirmPause=false}){Text("保持分享")}},
 )
 if(confirmUnpair)AlertDialog(
  onDismissRequest={confirmUnpair=false},title={Text("解除配对？")},
  text={Text("双方将立即失去对彼此资料的访问。你自己的资料会保留；之后可重新邀请配对。")},
  confirmButton={TextButton(enabled=!saving,onClick={vm.unpair{confirmUnpair=false}}){Text("解除配对")}},
  dismissButton={TextButton(onClick={confirmUnpair=false}){Text("取消")}},
 )
 if(imported!=null)AlertDialog(
  onDismissRequest=vm::cancelLocalImport,title={Text("确认导入本人资料？")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("昵称：${imported!!.profile.name} · 城市：${imported!!.profile.cityName()}")
   Text("作息：${if(imported!!.schedule==null)"未填写" else "将上传"} · 留言：${if(imported!!.note==null)"未填写" else "将上传"}")
   Text("只上传以上本人资料；不上传模拟伴侣、邀请或固定演示时间。")
  }},
  confirmButton={TextButton(enabled=!saving,onClick={vm.importLocalData()}){Text("确认并上传")}},
  dismissButton={TextButton(onClick=vm::cancelLocalImport){Text("取消")}},
 )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MySettingsDaylight(
 vm:AppViewModel,state:AppState,error:String?,saving:Boolean,demo:Boolean,realAccount:Boolean,onLogout:()->Unit,
 onOpenRhythm:()->Unit,onOpenBrand:()->Unit,onOpenPair:()->Unit,onBack:()->Unit,
){
 var profileOpen by rememberSaveable{mutableStateOf(false)}
 var confirmPause by rememberSaveable{mutableStateOf(false)}
 var confirmUnpair by rememberSaveable{mutableStateOf(false)}
 var name by rememberSaveable{mutableStateOf(state.me.name)}
 var city by rememberSaveable{mutableStateOf(state.me.cityId)}
 val imported by vm.localImport.collectAsStateWithLifecycle()

 PageHeader("我的",onBack)
 Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.size(42.dp).clip(CircleShape).background(if(state.me.cityId=="beijing")Night else Daylight),contentAlignment=Alignment.Center){
    Text(avatarInitial(state.me.name,"我"),color=if(state.me.cityId=="beijing")OnNight else DaylightInk,fontSize=14.sp)
  }
  Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
   Text(state.me.name.ifBlank{"我的资料"},fontSize=14.sp,fontWeight=FontWeight.Medium,color=Ink)
   Text("${state.me.cityName()} · ${state.me.zone().id}",fontSize=11.sp,color=Muted)
  }
 }
 Text("我的日常",fontSize=10.sp,letterSpacing=1.sp,color=Muted,modifier=Modifier.padding(top=2.dp,bottom=1.dp))
 SettingsEntryDaylight(app.nowus.android.R.drawable.icon_day,"我的节奏","工作日与休息日的通常安排",onOpenRhythm)
 SettingsEntryDaylight(app.nowus.android.R.drawable.icon_settings,"昵称与城市","手动选择，不获取实时位置",{vm.clearError();profileOpen=true})
 Spacer(Modifier.height(14.dp))
 Row(Modifier.fillMaxWidth().padding(top=2.dp,bottom=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
  Text("我们之间",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
  Text(if(state.paired)"${state.me.name} · ${state.partner?.name?:"对方"}" else "尚未配对",fontSize=10.sp,color=Muted)
 }
 SettingsEntryDaylight(
  app.nowus.android.R.drawable.icon_shield,
  if(state.sharingPaused||!state.sharingEnabled)"分享已暂停" else "正在分享日常",
  if(state.sharingPaused||!state.sharingEnabled)"对方暂时看不到你的共享资料" else "作息、联系偏好和当前留言",
  {if(state.paired){if(state.sharingPaused||!state.sharingEnabled)vm.setSharing(true) else confirmPause=true}},
  enabled=state.paired,
  trailing={Switch(checked=state.paired&&state.sharingEnabled&&!state.sharingPaused,onCheckedChange={enabled->if(enabled)vm.setSharing(true) else confirmPause=true},enabled=state.paired&&!saving,modifier=Modifier.semantics{contentDescription="向对方分享日常"})},
 )
 SettingsEntryDaylight(app.nowus.android.R.drawable.icon_link,"邀请与配对流程","查看首次使用的设计",onOpenPair)
 if(state.paired)SettingsEntryDaylight(app.nowus.android.R.drawable.icon_pause,"解除配对","自己的节奏会保留",{confirmUnpair=true})
 SettingsEntryDaylight(app.nowus.android.R.drawable.nowus_mark,"应用图标与 Logo","查看新一版品牌设计",onOpenBrand)
 Text("你的生活由你决定。\n这里不记录在线时长，也不推测实时行踪。",fontSize=10.sp,lineHeight=16.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=10.dp))

 if(realAccount){
  Spacer(Modifier.height(8.dp));Text("账号与同步",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
  SyncStatus(state)
  if(state.syncStale)TextButton(onClick=vm::retry){Text("重试同步")}
  Text("最后成功同步：${state.lastSyncMillis?.let{shortDateTime(Instant.ofEpochMilli(it),state.me)}?:"尚未成功"}",style=MaterialTheme.typography.bodySmall,color=Muted)
  TextButton(enabled=!saving,onClick=onLogout){Text("退出登录")}
  Spacer(Modifier.height(8.dp));Text("导入本机体验资料",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
  Text("只导入本人资料；不会上传模拟伴侣或演示时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
  TextButton(enabled=!saving,onClick=vm::inspectLocalImport){Text("检查可导入资料")}
 }else{
  Spacer(Modifier.height(8.dp));Text("本地体验",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
  Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("固定时间演示",Modifier.weight(1f),color=Ink);Switch(checked=demo,onCheckedChange=vm::setDemo)}
  Text("演示时钟独立于真实本地体验数据与邀请有效期。",style=MaterialTheme.typography.bodySmall,color=Muted)
  if(state.partner!=null&&state.partnerSchedule==null)TextButton(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}
  if(state.partner!=null)TextButton(enabled=!saving,onClick={confirmUnpair=true}){Text("解除演示配对")}
 }
 if(error!=null)ErrorText(error)

 if(profileOpen)EditorDialog("编辑昵称与城市",{profileOpen=false}){
  ProfileFields(name,{name=it},city,{city=it})
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.saveProfile(Profile(name.trim(),city)){profileOpen=false}}){Text("保存资料")}
 }
 if(confirmPause)ModalBottomSheet(onDismissRequest={confirmPause=false},containerColor=Panel,contentColor=Ink){
  Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    SheetTitle("暂停分享日常？"){confirmPause=false}
   Text("对方暂时看不到你的作息、联系偏好和留言。你可以随时恢复。",color=Muted)
   Button(enabled=!saving,onClick={vm.setSharing(false);confirmPause=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("暂停分享")}
    TextButton(onClick={confirmPause=false},modifier=Modifier.align(Alignment.Start)){Text("保持分享")}
  }
 }
 if(confirmUnpair)ModalBottomSheet(onDismissRequest={confirmUnpair=false},containerColor=Panel,contentColor=Ink){
  Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    SheetTitle("解除与" + (state.partner?.name ?: "对方") + "的配对？"){confirmUnpair=false}
   Text("双方将立即失去对彼此资料的访问。你自己的资料会保留；之后可重新邀请配对。",color=Muted)
   Button(enabled=!saving,onClick={vm.unpair{confirmUnpair=false}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("解除配对")}
    TextButton(onClick={confirmUnpair=false},modifier=Modifier.align(Alignment.Start)){Text("保留配对")}
  }
 }
 if(imported!=null)AlertDialog(
  onDismissRequest=vm::cancelLocalImport,title={Text("确认导入本人资料？")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("昵称：${imported!!.profile.name} · 城市：${imported!!.profile.cityName()}")
   Text("作息：${if(imported!!.schedule==null)"未填写" else "将上传"} · 留言：${if(imported!!.note==null)"未填写" else "将上传"}")
   Text("只上传以上本人资料；不上传模拟伴侣、邀请或固定演示时间。")
  }},
  confirmButton={TextButton(enabled=!saving,onClick={vm.importLocalData()}){Text("确认并上传")}},
  dismissButton={TextButton(onClick=vm::cancelLocalImport){Text("取消")}},
 )
}

@Composable private fun SettingsEntryDaylight(
 icon:Int,title:String,subtitle:String,onClick:()->Unit,enabled:Boolean=true,trailing:(@Composable ()->Unit)?=null,
){
 Column(Modifier.fillMaxWidth()){
  Row(
   Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(enabled=enabled,onClick=onClick).padding(vertical=10.dp),
   verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(11.dp),
  ){
   Icon(painterResource(icon),null,tint=Muted,modifier=Modifier.size(17.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Text(title,fontSize=12.sp,color=if(enabled)Ink else Muted)
    Text(subtitle,fontSize=10.sp,lineHeight=14.sp,color=Muted)
   }
   if(trailing!=null)trailing() else Icon(painterResource(app.nowus.android.R.drawable.icon_arrow),null,tint=Muted,modifier=Modifier.size(17.dp))
  }
  HorizontalDivider(color=Line,thickness=1.dp)
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PairPage(
 vm:AppViewModel,state:AppState,error:String?,saving:Boolean,realAccount:Boolean,now:Instant,
 pendingInviteCode:String?,onPendingInviteConsumed:()->Unit,onAccepted:()->Unit,onBack:()->Unit,
){
 val clipboard=LocalClipboardManager.current
 val context=LocalContext.current
 val invite=state.invite
 var inviteCode by rememberSaveable(pendingInviteCode){mutableStateOf(pendingInviteCode.orEmpty())}
 var partnerName by rememberSaveable{mutableStateOf("")}
 var partnerCity by rememberSaveable{mutableStateOf("new-york")}
 var accepting by rememberSaveable(pendingInviteCode){mutableStateOf(!pendingInviteCode.isNullOrBlank())}
 PageHeader("邀请另一半",onBack)
 Spacer(Modifier.height(15.dp))
  Box(Modifier.fillMaxWidth().height(150.dp),contentAlignment=Alignment.Center){
   Row(Modifier.requiredWidth(236.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
    Box(Modifier.size(100.dp).background(Night,CircleShape),contentAlignment=Alignment.Center){Icon(painterResource(app.nowus.android.R.drawable.icon_moon),null,tint=OnNight,modifier=Modifier.size(32.dp))}
    Box(Modifier.size(36.dp).offset(x=(-20).dp).background(Panel,CircleShape),contentAlignment=Alignment.Center){Icon(painterResource(app.nowus.android.R.drawable.icon_link),null,tint=Accent,modifier=Modifier.size(20.dp))}
    Box(Modifier.size(100.dp).offset(x=(-40).dp).background(Daylight,CircleShape),contentAlignment=Alignment.Center){Icon(painterResource(app.nowus.android.R.drawable.icon_sun),null,tint=DaylightInk,modifier=Modifier.size(32.dp))}
  }
 }
 Text("从你的此刻，\n到你们的日常。",Modifier.fillMaxWidth().padding(top=8.dp),textAlign=TextAlign.Center,color=Ink,fontSize=28.sp,lineHeight=45.sp)
 Text("各自拥有一天，\n也找到属于两个人的时间。",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted,fontSize=12.sp,lineHeight=19.sp)
 Surface(color=Panel,shape=RoundedCornerShape(17.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().padding(top=7.dp)){
  Column(Modifier.padding(horizontal=16.dp,vertical=23.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(if(invite!=null&&!invite.revoked)"邀请已建立 · 等待对方接受" else "你的专属邀请 · ${if(realAccount)"24 小时有效" else "本机演示"}",color=Muted,fontSize=10.sp)
   Text(if(invite!=null&&!invite.revoked)(if(realAccount)invite.code.chunked(3).joinToString(" ") else "NU · ${invite.code.chunked(3).joinToString(" ")}") else if(state.paired)"已建立配对" else "创建后显示给你",color=if(invite!=null&&!invite.revoked)Accent else Muted,fontSize=if(invite!=null&&!invite.revoked)23.sp else 14.sp,fontWeight=if(invite!=null&&!invite.revoked)FontWeight.Medium else FontWeight.Normal,letterSpacing=if(invite!=null&&!invite.revoked)2.sp else 0.sp)
   if(invite!=null&&!invite.revoked){
    Row(Modifier.fillMaxWidth().heightIn(min=30.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Text("有效至 ${shortDateTime(Instant.ofEpochMilli(invite.expiresMillis),state.me)} · 仅限一人接受",Modifier.weight(1f),color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,lineHeight=15.sp)
     Text("撤销邀请",Modifier.clickable(enabled=!saving,onClick=vm::revokeInvite).padding(horizontal=4.dp,vertical=4.dp),color=Muted,fontSize=10.sp)
    }
   }else Text("创建后可分享；邀请 24 小时内有效，仅限一人接受",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,lineHeight=15.sp)
  }
 }
 Button(enabled=!saving&&(invite!=null&&!invite.revoked||!state.paired),onClick={
  if(invite!=null&&!invite.revoked){
   clipboard.setText(AnnotatedString(if(realAccount)"nowus://invite/${invite.code}" else invite.code))
   android.widget.Toast.makeText(context,"已复制邀请代码",android.widget.Toast.LENGTH_SHORT).show()
  }else vm.createInvite()
 },modifier=Modifier.fillMaxWidth().padding(top=3.dp).heightIn(min=46.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Panel)){
  Text(if(invite!=null&&!invite.revoked)"复制邀请代码" else if(state.paired)"已建立配对" else if(realAccount)"创建我的邀请" else "创建演示邀请")
 }
 Column(Modifier.fillMaxWidth().padding(top=7.dp,bottom=3.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  PairShareLine(app.nowus.android.R.drawable.icon_check,"分享城市、通常作息和联系偏好")
  PairShareLine(app.nowus.android.R.drawable.icon_check,"分享你们各自的一条当前留言")
  PairShareLine(app.nowus.android.R.drawable.icon_shield,"随时暂停分享，不需要定位")
 }
 TextButton(onClick={accepting=true},modifier=Modifier.fillMaxWidth().heightIn(min=42.dp)){Text("我收到了一份邀请",color=Ink)}
 if(!realAccount)Text("本地体验 · 邀请仅在本机模拟，不会建立真实配对。",style=MaterialTheme.typography.bodySmall,color=Muted,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
 if(error!=null)ErrorText(error)
 if(accepting)ModalBottomSheet(onDismissRequest={accepting=false},containerColor=Panel,contentColor=Ink){
  InviteAcceptance(vm,state,saving,error,realAccount,pendingInviteCode,inviteCode,{inviteCode=it},partnerName,{partnerName=it},partnerCity,{partnerCity=it},onPendingInviteConsumed,onAccepted){accepting=false}
 }
}

@Composable private fun PairShareLine(icon:Int,label:String){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
  Icon(painterResource(icon),null,tint=Accent,modifier=Modifier.size(17.dp))
  Text(label,color=Ink,fontSize=12.sp,lineHeight=17.sp)
 }
}

@Composable private fun InviteAcceptance(
 vm:AppViewModel,state:AppState,saving:Boolean,error:String?,realAccount:Boolean,pendingInviteCode:String?,
 code:String,onCodeChange:(String)->Unit,partnerName:String,onPartnerName:(String)->Unit,partnerCity:String,onPartnerCity:(String)->Unit,
 onPendingInviteConsumed:()->Unit,onAccepted:()->Unit,onDismiss:()->Unit,
){
 val preview by vm.invitePreview.collectAsStateWithLifecycle()
 LaunchedEffect(pendingInviteCode){if(realAccount&&!pendingInviteCode.isNullOrBlank()){onCodeChange(pendingInviteCode);if(preview?.code!=pendingInviteCode)vm.previewInvite(pendingInviteCode)}}
  val matchingPreview=preview?.takeIf{it.code==code}
  Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
   SheetTitle(if(realAccount&&matchingPreview!=null) "接受" + matchingPreview.inviter.name + "的邀请" else "接受伴侣邀请",onDismiss)
  if(realAccount){
   if(matchingPreview==null){
    Text("输入邀请码，确认邀请人和共享范围后再配对。",color=Muted,style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(code,{onCodeChange(it.uppercase().filter(Char::isLetterOrDigit).take(10))},label={Text("10 位邀请码")},modifier=Modifier.fillMaxWidth().testTag("realInviteCode"),singleLine=true)
    Button(enabled=!saving&&code.length==10,onClick={vm.previewInvite(code)},modifier=Modifier.fillMaxWidth()){Text("查看邀请")}
   }else{
    HorizontalDivider(color=Line)
    Text("${matchingPreview.inviter.name}在${matchingPreview.inviter.cityName()}，想与你分享彼此的日常。",color=Muted,style=MaterialTheme.typography.bodySmall)
    matchingPreview.scope.forEach{PairShareLine(app.nowus.android.R.drawable.icon_check,it)}
    PairShareLine(app.nowus.android.R.drawable.icon_shield,"可随时暂停分享或解除配对")
    Text("有效至 ${shortDateTime(Instant.ofEpochMilli(matchingPreview.expiresMillis),state.me)}（服务器时间） · 未填写内容保持未知。",color=Muted,fontSize=10.sp,lineHeight=13.sp)
    Button(enabled=!saving,onClick={vm.acceptInvite(matchingPreview.code,null){onPendingInviteConsumed();onAccepted();onDismiss()}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("接受并配对")}
    TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.Start)){Text("暂时不接受",color=Muted)}
   }
  }else{
   Text("本机体验邀请只会在这台设备上模拟，不会建立真实配对。",color=Muted,style=MaterialTheme.typography.bodySmall)
   OutlinedTextField(code,onCodeChange,label={Text("输入本机演示邀请码")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   ProfileFields(partnerName,onPartnerName,partnerCity,onPartnerCity)
   Button(enabled=!saving,onClick={vm.acceptInvite(code,Profile(partnerName.trim(),partnerCity),onAccepted)},modifier=Modifier.fillMaxWidth()){Text("接受演示邀请")}
  }
  if(error!=null)ErrorText(error)
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MyRhythm(vm:AppViewModel,state:AppState,error:String?,saving:Boolean,demo:Boolean,realAccount:Boolean,now:Instant,onLogout:()->Unit,onBack:()->Unit){
 var draft by rememberSaveable{mutableStateOf(Json.encodeToString(state.schedule?:defaultSchedule(state.me.cityId)))}
 var rhythmSaved by rememberSaveable{mutableStateOf(false)}
 var restDay by rememberSaveable{mutableStateOf(false)}
 var editingId by rememberSaveable{mutableStateOf<String?>(null)}
 var editingContact by rememberSaveable{mutableStateOf(false)}
 var templateMenu by rememberSaveable{mutableStateOf(false)}
 var pendingTemplate by remember{mutableStateOf<RoutineTemplate?>(null)}
 val schedule=Json.decodeFromString<Schedule>(draft)
 val selected=if(restDay)schedule.rest else schedule.weekday
 val edited=if(selected.blocks.isNotEmpty())selected else selected.copy(blocks=listOf(
  RoutineBlock("sleep","睡眠",selected.sleepStart,selected.sleepEnd,RoutineCategory.SLEEP),
  RoutineBlock("main",selected.activity,selected.activityStart,selected.activityEnd,RoutineCategory.OTHER),
 ))
 val blocks=edited.blocks.sortedWith(compareBy<RoutineBlock>({if(it.id=="sleep"||it.category==RoutineCategory.SLEEP)0 else 1},{TimeEngine.parseMinute(it.start)?:0}))
 fun saveDraft(value:Schedule){draft=Json.encodeToString(value);rhythmSaved=false}
 fun updateDay(value:Rhythm){saveDraft(if(restDay)schedule.copy(rest=value)else schedule.copy(weekday=value))}
 PageHeader("我的节奏",onBack)
 Text("安排好平常的一天，就不用每天填写。",style=MaterialTheme.typography.bodyMedium,color=Muted)
 Row(Modifier.fillMaxWidth().background(Line,RoundedCornerShape(12.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
  listOf(false to "工作日",true to "休息日").forEach{(isRest,label)->
   Surface(color=if(restDay==isRest)Panel else Color.Transparent,shape=RoundedCornerShape(9.dp),shadowElevation=if(restDay==isRest)2.dp else 0.dp,modifier=Modifier.weight(1f).heightIn(min=40.dp).clickable{restDay=isRest}){
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(label,color=if(restDay==isRest)Ink else Muted,fontSize=12.sp,fontWeight=if(restDay==isRest)FontWeight.Medium else FontWeight.Normal)}
   }
  }
 }
 RhythmOverview(state.me,edited,restDay)
 Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
  Text("通常的一天",style=MaterialTheme.typography.titleSmall,color=Ink,fontWeight=FontWeight.SemiBold)
  TextButton(enabled=blocks.size<20,onClick={
   val occupied=(0 until 1440).filter{minute->blocks.any{TimeEngine.contains(minute,it.start,it.end)}}.toSet()
   val start=(0..1380 step 30).firstOrNull{candidate->(candidate until candidate+30).none{it in occupied}}?:1320
   val id="custom-${(blocks.mapNotNull{it.id.substringAfter("custom-","").toIntOrNull()}.maxOrNull()?:0)+1}"
   val item=RoutineBlock(id,"新时段",String.format(Locale.ROOT,"%02d:%02d",start/60,start%60),String.format(Locale.ROOT,"%02d:%02d",(start+30)/60,(start+30)%60),RoutineCategory.OTHER)
   updateDay(updateBlocks(edited,blocks+item));editingId=id
  }){Text(if(blocks.size<20)"+ 添加时段" else "已达到 20 个时段上限",fontSize=11.sp,color=Accent)}
 }
 blocks.forEach{block->
  Row(Modifier.fillMaxWidth().heightIn(min=61.dp).clickable{editingId=block.id}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Box(Modifier.size(39.dp).background(Panel,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(painterResource(routineIcon(block.category)),null,tint=Muted,modifier=Modifier.size(19.dp))}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Text(block.label,color=Ink,fontSize=12.sp,fontWeight=FontWeight.Medium)
    Text("${block.start} – ${block.end}${if(block.start>block.end)" · 次日" else ""}",color=Muted,fontSize=11.sp)
   }
   Icon(painterResource(app.nowus.android.R.drawable.icon_arrow),null,tint=Muted,modifier=Modifier.size(16.dp))
  }
  HorizontalDivider(color=Line,thickness=1.dp)
 }
 Surface(color=Tint,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().padding(top=7.dp)){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Text("愿意联系的时间",style=MaterialTheme.typography.titleSmall,color=Ink,fontWeight=FontWeight.SemiBold)
    TextButton(onClick={editingContact=true},contentPadding=PaddingValues(horizontal=6.dp,vertical=0.dp)){Text(if(edited.contactKnown)"编辑" else "设置",fontSize=11.sp,color=Accent)}
   }
   Text(if(edited.contactKnown)"${edited.contactStart} – ${edited.contactEnd}" else "保持未知",fontSize=22.sp,color=Ink)
   Text("这是联系偏好，不是随时回复的承诺。",fontSize=10.sp,color=Muted)
  }
 }
 Box{
  TextButton(onClick={templateMenu=true}){Text("应用参考作息模板",fontSize=11.sp,color=Muted)}
  DropdownMenu(expanded=templateMenu,onDismissRequest={templateMenu=false}){
   RoutineTemplate.entries.forEach{template->DropdownMenuItem(text={Text("${template.title}模板")},onClick={pendingTemplate=template;templateMenu=false})}
  }
 }
 if(realAccount&&state.schedule==null)Text("这是按城市提供的可编辑作息建议。修改并保存后才会同步到账号。",color=Muted,style=MaterialTheme.typography.bodySmall)
 Button(enabled=!saving,onClick={rhythmSaved=false;vm.saveSchedule(schedule){rhythmSaved=true}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Panel)){Text("保存我的节奏")}
 if(error!=null)ErrorText(error)
 if(rhythmSaved)Text(if(realAccount)"已保存并同步" else "已保存到本机",color=Accent)
 if(!realAccount&&demo)Text("此刻的作息仅供固定时间演示；关闭演示后会回到真实时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
 if(pendingTemplate!=null)AlertDialog(onDismissRequest={pendingTemplate=null},title={Text("应用${pendingTemplate!!.title}模板？")},text={Text("这会替换尚未保存的工作日和休息日时段。应用后仍可逐项修改。")},confirmButton={TextButton(onClick={saveDraft(RoutineTemplates.forCity(state.me.cityId,pendingTemplate!!));pendingTemplate=null}){Text("应用模板")}},dismissButton={TextButton(onClick={pendingTemplate=null}){Text("继续编辑")} })
 val edit=editingId?.let{id->blocks.firstOrNull{it.id==id}}
 if(edit!=null)ModalBottomSheet(onDismissRequest={editingId=null},containerColor=Panel,contentColor=Ink){
  var label by rememberSaveable(edit.id){mutableStateOf(edit.label)}
  Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("编辑时段",style=MaterialTheme.typography.titleLarge);TextButton(onClick={editingId=null}){Text("完成",color=Accent)}}
   OutlinedTextField(label,{value->label=value;updateDay(updateBlocks(edited,blocks.map{if(it.id==edit.id)it.copy(label=value)else it}))},label={Text("时段名称")},modifier=Modifier.fillMaxWidth().testTag("rhythm-edit-${edit.id}-label"),singleLine=true)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("类别",style=MaterialTheme.typography.bodySmall,color=Muted);RoutineCategoryPicker(edit.category,"rhythm-edit-${edit.id}-category"){category->updateDay(updateBlocks(edited,blocks.map{if(it.id==edit.id)it.copy(label=label,category=category)else it}))}}
   TimePair(if(edit.id=="sleep")"睡眠" else edit.label,"rhythm-edit-${edit.id}",edit.start,edit.end,
    {time->updateDay(updateBlocks(edited,blocks.map{if(it.id==edit.id)it.copy(label=label,start=time)else it}))},
    {time->updateDay(updateBlocks(edited,blocks.map{if(it.id==edit.id)it.copy(label=label,end=time)else it}))})
   if(edit.id!="sleep")TextButton(onClick={updateDay(updateBlocks(edited,blocks.filterNot{it.id==edit.id}));editingId=null},modifier=Modifier.fillMaxWidth()){Text("删除此时段",color=Muted)}
  }
 }
 if(editingContact)ModalBottomSheet(onDismissRequest={editingContact=false},containerColor=Panel,contentColor=Ink){
  var known by rememberSaveable(restDay,edited.contactKnown){mutableStateOf(edited.contactKnown)}
  var start by rememberSaveable(restDay,edited.contactStart){mutableStateOf(edited.contactStart)}
  var end by rememberSaveable(restDay,edited.contactEnd){mutableStateOf(edited.contactEnd)}
  Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("愿意联系的时间",style=MaterialTheme.typography.titleLarge)
   Text("这是通常的联系偏好，不代表对方在线或会立即回复。",style=MaterialTheme.typography.bodySmall,color=Muted)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("按我的意愿显示",color=Ink);Switch(checked=known,onCheckedChange={known=it})}
   if(known)TimePair("联系时间","rhythm-${if(restDay)"rest" else "weekday"}-contact",start,end,{start=it},{end=it})
   Button(onClick={updateDay(edited.copy(contactKnown=known,contactStart=start,contactEnd=end));editingContact=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("完成")}
  }
 }
}

@Composable private fun PageHeader(title:String,onBack:()->Unit){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
  IconButton(onClick=onBack,modifier=Modifier.size(40.dp).semantics{contentDescription="返回"}){Icon(painterResource(app.nowus.android.R.drawable.icon_back),null,tint=Accent)}
  Text(title,style=MaterialTheme.typography.headlineSmall,color=Ink)
 }
}

@Composable internal fun SheetTitle(title:String,onClose:()->Unit){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
  Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,color=Ink)
  IconButton(onClick=onClose,modifier=Modifier.size(40.dp).semantics{contentDescription="关闭弹层"}){
   Icon(painterResource(app.nowus.android.R.drawable.icon_close),null,tint=Muted,modifier=Modifier.size(20.dp))
  }
 }
}

@Composable private fun PrivacyStatusBanner(state:AppState,onRetry:()->Unit){
 Surface(color=Daylight,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(horizontal=13.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   Text(if(state.syncStale)"同步暂不可用 · 对方资料已隐藏" else "分享已暂停 · 对方资料已隐藏",color=DaylightInk,fontSize=12.sp,fontWeight=FontWeight.Medium)
   if(state.syncStale)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("联网后可重试同步。",color=DaylightInk,fontSize=11.sp);TextButton(onClick=onRetry){Text("重试",color=Accent)}}
  }
 }
}

private fun routineIcon(category:RoutineCategory)=when(category){RoutineCategory.SLEEP->app.nowus.android.R.drawable.icon_moon;RoutineCategory.STUDY_WORK->app.nowus.android.R.drawable.icon_work;RoutineCategory.MEAL->app.nowus.android.R.drawable.icon_cup;else->app.nowus.android.R.drawable.icon_day}

internal fun avatarInitial(name:String,fallback:String="…"):String{
 val points=name.trim().codePoints().toArray()
 if(points.isEmpty())return fallback
 val last=points.last()
 val selected=if(points.size>1&&Character.UnicodeScript.of(last)==Character.UnicodeScript.HAN)last else points.first()
 return String(Character.toChars(selected))
}

@Composable private fun RhythmOverview(profile:Profile,rhythm:Rhythm,rest:Boolean){
 val blocks=if(rhythm.blocks.isEmpty())listOf(RoutineBlock("sleep","睡眠",rhythm.sleepStart,rhythm.sleepEnd,RoutineCategory.SLEEP),RoutineBlock("main",rhythm.activity,rhythm.activityStart,rhythm.activityEnd,RoutineCategory.OTHER))else rhythm.blocks
 val sleep=blocks.firstOrNull{it.id=="sleep"||it.category==RoutineCategory.SLEEP}
 val start=sleep?.let{TimeEngine.parseMinute(it.start)}?:0
 val end=sleep?.let{TimeEngine.parseMinute(it.end)}?:0
 val hours=if(sleep==null)0.0 else (((end-start+1440)%1440)/60.0)
 Surface(color=Night,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(horizontal=15.dp,vertical=15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("${profile.cityName()} · ${if(rest)"周六、周日" else "周一至周五"}",color=NightSub,fontSize=10.sp)
   Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(5.dp)){
    Text("睡眠 ${String.format(Locale.ROOT,"%.1f",hours).trimEnd('0').trimEnd('.')} 小时",color=OnNight,fontSize=20.sp,fontWeight=FontWeight.Medium)
    Text("· ${blocks.size} 段日常安排",color=NightSub,fontSize=10.sp,modifier=Modifier.padding(bottom=3.dp))
   }
   RhythmTrack(rhythm)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){listOf("00","06","12","18","24").forEach{Text(it,color=NightSub,fontSize=10.sp,fontFamily=FontFamily.Monospace)}}
  }
 }
}

@Composable private fun RhythmTrack(rhythm:Rhythm){
 Canvas(Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(4.dp))){
   drawRoundRect(NightSub)
   val blocks=if(rhythm.blocks.isNotEmpty())rhythm.blocks else listOf(
    RoutineBlock("sleep","睡觉",rhythm.sleepStart,rhythm.sleepEnd,RoutineCategory.SLEEP),
    RoutineBlock("main",rhythm.activity,rhythm.activityStart,rhythm.activityEnd,RoutineCategory.OTHER),
   )
   fun minute(value:String)=value.take(2).toIntOrNull()?.times(60)?.plus(value.takeLast(2).toIntOrNull()?:0)?:0
   blocks.forEach{block->
    val start=minute(block.start);val end=minute(block.end)
    val color=when(block.category){RoutineCategory.SLEEP->NightSub;RoutineCategory.STUDY_WORK->Peach;RoutineCategory.MEAL->Daylight;else->Tint}
    fun mark(a:Int,b:Int){if(b>a)drawRect(color,androidx.compose.ui.geometry.Offset(size.width*a/1440f,0f),androidx.compose.ui.geometry.Size(size.width*(b-a)/1440f,size.height))}
    if(end>start)mark(start,end)else{mark(start,1440);mark(0,end)}
   }
 }
}

@Composable private fun BrandPage(onBack:()->Unit){
 PageHeader("图标与 Logo",onBack)
 Column(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)){
  androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.nowus_mark),contentDescription="NowUs 彩色标记",modifier=Modifier.size(112.dp))
  Text("各自的日常，\n相接的我们。",fontSize=24.sp,lineHeight=40.sp,color=Ink,textAlign=TextAlign.Center)
  androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.logo_lockup),contentDescription="NowUs 完整标志",modifier=Modifier.width(208.dp).height(58.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Surface(color=Panel,shape=RoundedCornerShape(13.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.weight(1f)){
    Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.nowus_mark),null,modifier=Modifier.size(54.dp));Text("彩色标记",fontSize=10.sp,color=Muted)}
   }
   Surface(color=Panel,shape=RoundedCornerShape(13.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.weight(1f)){
    Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.nowus_mark_mono),null,modifier=Modifier.size(54.dp));Text("单色版本",fontSize=10.sp,color=Muted)}
   }
   Surface(color=Night,shape=RoundedCornerShape(13.dp),modifier=Modifier.weight(1f)){
    Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){androidx.compose.foundation.Image(painterResource(app.nowus.android.R.drawable.nowus_adaptive_foreground),null,modifier=Modifier.size(54.dp));Text("深色背景",fontSize=10.sp,color=NightSub)}
   }
  }
  Text("夜蓝代表一方的夜晚，晨光代表另一方的白天。中间共用的一笔，是两个人愿意留给彼此的时间。",color=Muted,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
 }
}
@Composable private fun EditorDialog(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
 Dialog(onDismissRequest=onDismiss){
  Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){
   Column(Modifier.fillMaxWidth().heightIn(max=560.dp).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text(title,style=MaterialTheme.typography.titleLarge);content();TextButton(onClick=onDismiss){Text("关闭")}
   }
  }
 }
}

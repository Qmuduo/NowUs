package app.nowus.android.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nowus.android.AppViewModel
import app.nowus.android.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun defaultSchedule(cityId:String="beijing",template:RoutineTemplate=RoutineTemplate.STUDENT)=RoutineTemplates.forCity(cityId,template)

@Composable fun NowUsApp(
 vm:AppViewModel,
 realAccount:Boolean=false,
 pendingInviteCode:String?=null,
 onPendingInviteConsumed:()->Unit={},
 onLogout:()->Unit={},
 onExitDemo:()->Unit={},
){
 val state by vm.state.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val saving by vm.saving.collectAsStateWithLifecycle()
 val readFailed by vm.readFailed.collectAsStateWithLifecycle()
 val now by vm.now.collectAsStateWithLifecycle()
 val demo by vm.demo.collectAsStateWithLifecycle()
 LaunchedEffect(vm){while(isActive){vm.refreshTime();delay(1000)}}
 var tab by rememberSaveable {mutableIntStateOf(0)}
 BackHandler(enabled=tab!=0){tab=0}
 val current=state
 if(current==null){
  Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)){
   Column(verticalArrangement=Arrangement.spacedBy(20.dp)){
    Text("NowUs",style=MaterialTheme.typography.headlineLarge)
    Text(error?:"正在打开我们的本地时光…")
    if(error!=null)Button(onClick=vm::retry){Text("重试加载")} else CircularProgressIndicator()
   }
  };return
 }
 if(!current.setupComplete){Onboarding(vm,current,error,saving,realAccount,pendingInviteCode,onPendingInviteConsumed);return}
 val tabStates=rememberSaveableStateHolder()
 Scaffold(containerColor=Page,bottomBar={
  NavigationBar(containerColor=MaterialTheme.colorScheme.surface){
   listOf("此刻","我们的一天","我的节奏").forEachIndexed {index,label->
    NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Text(listOf("◉","☷","◷")[index],fontSize=22.sp)},label={Text(label)})
   }
  }
 }){insets->
  tabStates.SaveableStateProvider(tab){
  Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
   Text("NowUs",style=MaterialTheme.typography.titleLarge,color=Forest)
   Text(if(realAccount)"真实账号 · 作息与留言仅和已配对伴侣共享" else "本地体验 · 演示配对，无账号同步",style=MaterialTheme.typography.bodySmall,color=Muted)
   if(realAccount && pendingInviteCode != null){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     Text("已保存邀请 $pendingInviteCode",style=MaterialTheme.typography.bodySmall,color=Forest)
     TextButton(onClick=onPendingInviteConsumed){Text("清除")}
    }
   }
   if(realAccount)SyncStatus(current)
   if(demo){ErrorText("固定演示时间 · 2026/9/29 · 不代表此刻");Button(onClick={vm.setDemo(false)}){Text("退出固定演示")}}
   if(!realAccount)TextButton(onClick=onExitDemo){Text("返回登录")}
   if(error!=null)ErrorText(error!!)
   if(readFailed)Button(onClick=vm::retry){Text("重试加载")}
   if(realAccount&&current.syncStale)OutlinedButton(onClick=vm::retry){Text("重试同步")}
   when(tab){0->Home(vm,current,now,demo,error,saving,realAccount,pendingInviteCode,onPendingInviteConsumed);1->Timeline(current,now);else->MyRhythm(vm,current,error,saving,demo,realAccount,onLogout)}
   Spacer(Modifier.height(18.dp))
  }
  }
 }
}

@Composable fun ErrorText(error:String){Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(12.dp)){Text(error,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.onErrorContainer)}}
@Composable fun SectionCard(title:String,content:@Composable ColumnScope.()->Unit){
 Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.titleLarge);content()}
 }
}
@Composable private fun Clock(profile:Profile,schedule:Schedule?,temporary:TemporaryStatus?,now:Instant,modifier:Modifier=Modifier,simulated:Boolean=false,compact:Boolean=false,partner:Boolean=false){
 val hour=now.atZone(profile.zone()).hour;val night=hour<7||hour>=19
 Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=if(night)Night else Sunlight),modifier=modifier.fillMaxWidth()){
  Column(Modifier.padding(if(compact)14.dp else 22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   val color=if(night)Color.White else Ink
   Text("${profile.name} · ${profile.cityName()} · ${if(simulated)"模拟对方" else if(partner)"伴侣" else "我"}",color=color,style=MaterialTheme.typography.titleMedium)
   Text(localTime(now,profile),fontSize=if(compact)32.sp else 46.sp,lineHeight=if(compact)40.sp else 54.sp,color=color)
   Text(localDate(now,profile),color=color)
   val activity=TimeEngine.activityAt(profile,schedule,null,now)
   Text(activity.label,color=color,style=MaterialTheme.typography.titleMedium)
   Text(sourceText(activity.source),color=color,style=MaterialTheme.typography.bodySmall)
   val contact=TimeEngine.contactAt(profile,schedule,temporary,now)
   Text(when(contact){true->"联系意愿：可联系";false->"联系意愿：暂不联系";null->"联系意愿：未知"},color=color)
   if(temporary!=null && now.toEpochMilli() in temporary.fromMillis until temporary.untilMillis)Text("主动设置 · 到 ${localTime(Instant.ofEpochMilli(temporary.untilMillis),profile)} 恢复通常偏好",color=color,style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Composable private fun Home(vm:AppViewModel,state:AppState,now:Instant,demo:Boolean,error:String?,saving:Boolean,realAccount:Boolean,pendingInviteCode:String?,onPendingInviteConsumed:()->Unit){
 Text("不同的时间，同一份惦念",style=MaterialTheme.typography.headlineSmall)
 val partner=state.partner
 BoxWithConstraints(Modifier.fillMaxWidth()){
  if(partner!=null && maxWidth>=320.dp && LocalDensity.current.fontScale<=1.3f){
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Clock(state.me,state.schedule,state.temporary,now,modifier=Modifier.weight(1f),compact=true)
    Clock(partner,state.partnerSchedule,state.partnerTemporary,now,Modifier.weight(1f),simulated=!realAccount,compact=true,partner=realAccount)
   }
  }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Clock(state.me,state.schedule,state.temporary,now)
   if(partner!=null)Clock(partner,state.partnerSchedule,state.partnerTemporary,now,simulated=!realAccount,partner=realAccount)
  }
 }
 if(partner!=null){
  Text(offsetText(state.me,partner,now),color=Muted)
  if(state.partnerSchedule==null)SectionCard("对方作息待补充"){
   Text(if(realAccount)"对方尚未填写通常作息，活动和联系意愿保持未知。未知不会被当作有空。" else "演示已配对，但没有对方作息。未知不会被当作空闲。")
   if(!realAccount)Button(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}
   if(error!=null)ErrorText(error)
  }
  if(realAccount)SectionCard("对方的留言"){
   Text(state.partnerNote?.text?:"对方还没有留下当前留言。")
  }
 }else if(realAccount&&state.syncStale)SectionCard("伴侣资料暂不可用"){
  Text("无法确认当前分享权限，因此暂不显示缓存的伴侣资料。联网后重试同步即可恢复可访问内容。")
 }else if(realAccount&&state.sharingPaused)SectionCard("分享已暂停"){
  Text("你或对方暂停了分享。对方的资料不会显示；可在“我的节奏”恢复分享。")
 }else if(realAccount&&state.paired)SectionCard("配对已建立"){
  Text("对方尚未填写可共享的昵称与城市；内容保持未知，不会用演示资料补齐。")
 }else SectionCard(if(realAccount)"邀请伴侣" else "把时光留一个位置"){
  Text(if(realAccount)"创建邀请链接或输入伴侣的邀请码。对方接受前会先看到你的昵称、城市和共享范围。" else "目前独自使用。演示邀请只在这台设备上模拟配对，不会发送给任何人。")
  Invitation(vm,state,saving,error,realAccount,pendingInviteCode,onPendingInviteConsumed)
 }
 if(realAccount&&partner==null&&state.partnerNote!=null)SectionCard("对方的留言"){
  Text(state.partnerNote.text)
  Text("对方尚未填写昵称与城市 · ${shortDateTime(Instant.ofEpochMilli(state.partnerNote.updatedMillis),state.me)}",style=MaterialTheme.typography.bodySmall,color=Muted)
 }
 val windows by produceState<List<Window>?>(null,state,now.epochSecond/60){
  value=withContext(Dispatchers.Default){partner?.let{TimeEngine.commonWindows(now,now.plusSeconds(7*86400),state.me,state.schedule,state.temporary,it,state.partnerSchedule)}?:emptyList()}
 }
 SectionCard("可以一起的片刻"){
  Text("未来七天 · 来自通常联系偏好与主动设置",style=MaterialTheme.typography.bodySmall,color=Muted)
  val next=windows?.firstOrNull{it.end>now}
  if(windows==null)Text("正在寻找共同窗口…")
  else if(next==null)Text(if(partner==null||state.partnerSchedule==null)"资料尚不完整，暂不能计算共同窗口" else "未来七天暂无共同联系窗口")
  else{
   val start=maxOf(next.start,now)
   Text(if(next.start<=now)"此刻可以一起" else "下一个共同窗口",color=Forest,style=MaterialTheme.typography.titleMedium)
   Text("我：${shortDateTime(start,state.me)} – ${shortDateTime(next.end,state.me)}")
   Text("${partner!!.name}：${shortDateTime(start,partner)} – ${shortDateTime(next.end,partner)}")
   Text("时区与日期按所在地计算；不会推断对方实时状态。",style=MaterialTheme.typography.bodySmall,color=Muted)
  }
 }
  NoteCard(vm,state,error,saving,realAccount)
 TemporaryCard(vm,state,now,demo,error,saving)
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
   Text("主动设置：${if(temporary.available) "可联系" else "忙碌"} · 至 ${localTime(Instant.ofEpochMilli(temporary.untilMillis),state.me)}",color=Forest)
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
 SectionCard(if(realAccount)"我的当前留言" else "留一句话"){
  Text(state.note?.text?:"给我们的下一次相遇，留一句期待。")
  if(state.note!=null)Text(if(realAccount)"仅配对伴侣可见 · ${shortDateTime(Instant.ofEpochMilli(state.note.updatedMillis),state.me)}" else "仅保存到本机 · ${shortDateTime(Instant.ofEpochMilli(state.note.updatedMillis),state.me)}",style=MaterialTheme.typography.bodySmall,color=Muted)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={vm.clearError();draft=state.note?.text.orEmpty();open=true}){Text(if(state.note==null)"写留言" else "编辑留言")}
   if(state.note!=null)TextButton(enabled=!saving,onClick={vm.saveNote(""){draft=""}}){Text("删除留言")}
  }
  if(error!=null&&!open)ErrorText(error)
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
 Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Text("NowUs",color=Forest,style=MaterialTheme.typography.headlineLarge)
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

@Composable private fun MyRhythm(vm:AppViewModel,state:AppState,error:String?,saving:Boolean,demo:Boolean,realAccount:Boolean=false,onLogout:()->Unit={}){
 var profileOpen by rememberSaveable {mutableStateOf(false)}
 var name by rememberSaveable {mutableStateOf(state.me.name)}
 var city by rememberSaveable {mutableStateOf(state.me.cityId)}
 var draft by rememberSaveable {mutableStateOf(Json.encodeToString(state.schedule?:defaultSchedule(state.me.cityId)))}
 var rhythmSaved by rememberSaveable {mutableStateOf(false)}
 Text("我的节奏",style=MaterialTheme.typography.headlineSmall)
 SectionCard("${state.me.name} · ${state.me.cityName()}"){
   Text(if(realAccount)"仅编辑自己的资料；配对后伴侣可见" else "资料与节奏仅保存在本机",color=Muted)
  TextButton(onClick={vm.clearError();profileOpen=true}){Text("编辑昵称与城市")}
 }
 SectionCard("通常作息"){
   if(realAccount&&state.schedule==null)Text("以下是可编辑的常见作息建议；修改并点击保存后才会同步给账号。",style=MaterialTheme.typography.bodySmall,color=Muted)
  val schedule=Json.decodeFromString<Schedule>(draft)
  RhythmFields(schedule,state.me.cityId,{draft=Json.encodeToString(it);rhythmSaved=false})
  Button(enabled=!saving,onClick={rhythmSaved=false;vm.saveSchedule(schedule){rhythmSaved=true}}){Text("保存我的节奏")}
  if(error!=null)ErrorText(error)
   if(rhythmSaved)Text(if(realAccount)"已保存并同步" else "已保存到本机",color=Forest)
 }
  if(!realAccount)SectionCard("演示设置"){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("固定时间演示",Modifier.weight(1f));Switch(checked=demo,onCheckedChange=vm::setDemo)}
  Text("开启后固定为 2026/9/29，北京 22:00。退出或重新启动回到真实时间。邀请有效期始终使用真实时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
  if(state.partner!=null){Text("模拟对方：${state.partner.name} · ${state.partner.cityName()}");if(state.partnerSchedule==null)Button(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}}else Invitation(vm,state,saving,error)
 }
  if(realAccount){
   var confirmUnpair by rememberSaveable {mutableStateOf(false)}
   val imported by vm.localImport.collectAsStateWithLifecycle()
   if(state.paired)SectionCard("分享与配对"){
    Text(if(state.sharingPaused)"至少一方暂停了分享，对方资料当前不可见。" else "分享开启时，仅当前配对伴侣能读取你已填写的资料。")
    Button(enabled=!saving,onClick={vm.setSharing(!state.sharingEnabled)}){Text(if(state.sharingEnabled)"暂停我的分享" else "恢复我的分享")}
    TextButton(enabled=!saving,onClick={confirmUnpair=true}){Text("解除配对")}
   }else SectionCard("分享与配对"){Text("当前尚未配对。")}
   SectionCard("导入本机体验资料"){
    Text("可检查并确认导入你自己的演示昵称、城市、作息和留言。模拟伴侣、邀请与演示时间不会导入。")
    OutlinedButton(enabled=!saving,onClick=vm::inspectLocalImport){Text("检查可导入资料")}
   }
   SectionCard("账号"){
    Text("最后成功同步：${state.lastSyncMillis?.let{shortDateTime(Instant.ofEpochMilli(it),state.me)}?:"尚未成功"}",style=MaterialTheme.typography.bodySmall,color=Muted)
    TextButton(enabled=!saving,onClick=onLogout){Text("退出登录")}
   }
   if(error!=null)ErrorText(error)
   if(imported!=null)AlertDialog(
    onDismissRequest=vm::cancelLocalImport,
    title={Text("确认导入本人资料？")},
    text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text("昵称：${imported!!.profile.name} · 城市：${imported!!.profile.cityName()}")
     Text("工作日和休息日作息：${if(imported!!.schedule==null)"未填写" else "将上传"}")
     Text("当前留言：${if(imported!!.note==null)"未填写" else "将上传"}")
     Text("只上传以上本人资料；不上传模拟伴侣、演示邀请或固定演示时间。")
    }},
    confirmButton={TextButton(enabled=!saving,onClick={vm.importLocalData()}){Text("确认并上传")}},
    dismissButton={TextButton(onClick=vm::cancelLocalImport){Text("取消")}},
   )
   if(confirmUnpair)AlertDialog(
    onDismissRequest={confirmUnpair=false},title={Text("解除配对？")},
    text={Text("双方将立即失去对彼此资料的访问。你自己的资料会保留；之后可重新邀请配对。")},
    confirmButton={TextButton(enabled=!saving,onClick={vm.unpair{confirmUnpair=false}}){Text("解除配对")}},
    dismissButton={TextButton(onClick={confirmUnpair=false}){Text("取消")}},
   )
  }
 if(profileOpen)EditorDialog("编辑我的资料",{profileOpen=false}){
  ProfileFields(name,{name=it},city,{city=it})
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.saveProfile(Profile(name.trim(),city)){profileOpen=false}}){Text("保存资料")}
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

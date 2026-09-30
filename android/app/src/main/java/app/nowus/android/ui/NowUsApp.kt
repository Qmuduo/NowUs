package app.nowus.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nowus.android.AppViewModel
import app.nowus.android.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant

fun defaultSchedule()=Schedule(Rhythm(),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00"))

@Composable fun NowUsApp(vm:AppViewModel){
 val state by vm.state.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val saving by vm.saving.collectAsStateWithLifecycle()
 val readFailed by vm.readFailed.collectAsStateWithLifecycle()
 val now by vm.now.collectAsStateWithLifecycle()
 val demo by vm.demo.collectAsStateWithLifecycle()
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
 if(!current.setupComplete){Onboarding(vm,current,error,saving);return}
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
   Text("本地体验 · 演示配对，无账号同步",style=MaterialTheme.typography.bodySmall,color=Muted)
   if(demo){ErrorText("固定演示时间 · 2026/9/29 · 不代表此刻");Button(onClick={vm.setDemo(false)}){Text("退出固定演示")}}
   if(error!=null)ErrorText(error!!)
   if(readFailed)Button(onClick=vm::retry){Text("重试加载")}
   when(tab){0->Home(vm,current,now,demo,error,saving);1->Timeline(current,now);else->MyRhythm(vm,current,error,saving,demo)}
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
@Composable private fun Clock(profile:Profile,schedule:Schedule?,temporary:TemporaryStatus?,now:Instant,modifier:Modifier=Modifier,simulated:Boolean=false,compact:Boolean=false){
 val hour=now.atZone(profile.zone()).hour;val night=hour<7||hour>=19
 Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=if(night)Night else Sunlight),modifier=modifier.fillMaxWidth()){
  Column(Modifier.padding(if(compact)14.dp else 22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   val color=if(night)Color.White else Ink
   Text("${profile.name} · ${profile.cityName()}${if(simulated) " · 模拟对方" else " · 我"}",color=color,style=MaterialTheme.typography.titleMedium)
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
@Composable private fun Home(vm:AppViewModel,state:AppState,now:Instant,demo:Boolean,error:String?,saving:Boolean){
 Text("不同的时间，同一份惦念",style=MaterialTheme.typography.headlineSmall)
 val partner=state.partner
 BoxWithConstraints(Modifier.fillMaxWidth()){
  if(partner!=null && maxWidth>=320.dp && LocalDensity.current.fontScale<=1.3f){
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Clock(state.me,state.schedule,state.temporary,now,modifier=Modifier.weight(1f),compact=true)
    Clock(partner,state.partnerSchedule,null,now,Modifier.weight(1f),simulated=true,compact=true)
   }
  }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Clock(state.me,state.schedule,state.temporary,now)
   if(partner!=null)Clock(partner,state.partnerSchedule,null,now,simulated=true)
  }
 }
 if(partner!=null){
  Text(offsetText(state.me,partner,now),color=Muted)
  if(state.partnerSchedule==null)SectionCard("对方作息待补充"){
   Text("演示已配对，但没有对方作息。未知不会被当作空闲。")
   Button(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}
   if(error!=null)ErrorText(error)
  }
 }else SectionCard("把时光留一个位置"){
  Text("目前独自使用。演示邀请只在这台设备上模拟配对，不会发送给任何人。")
  Invitation(vm,state,saving,error)
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
 NoteCard(vm,state,error,saving)
 TemporaryCard(vm,state,now,demo,error,saving)
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
@Composable private fun NoteCard(vm:AppViewModel,state:AppState,error:String?,saving:Boolean){
 var open by rememberSaveable {mutableStateOf(false)}
 var draft by rememberSaveable {mutableStateOf(state.note?.text.orEmpty())}
 SectionCard("留一句话"){
  Text(state.note?.text?:"给我们的下一次相遇，留一句期待。")
  if(state.note!=null)Text("仅保存到本机 · ${shortDateTime(Instant.ofEpochMilli(state.note.updatedMillis),state.me)}",style=MaterialTheme.typography.bodySmall,color=Muted)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={vm.clearError();open=true}){Text(if(state.note==null)"写留言" else "编辑留言")}
   if(state.note!=null)TextButton(enabled=!saving,onClick={vm.saveNote(""){draft=""}}){Text("删除留言")}
  }
  if(error!=null&&!open)ErrorText(error)
 }
 if(open)EditorDialog("留一句话",{open=false}){
  OutlinedTextField(draft,{draft=it},label={Text("留言")},supportingText={Text("${draft.codePointCount(0,draft.length)} / 120 字 · 仅在本机")},modifier=Modifier.fillMaxWidth().testTag("noteDraft"),minLines=3)
  if(error!=null)ErrorText(error)
  Button(enabled=!saving,onClick={vm.saveNote(draft){open=false}}){Text("保存留言")}
 }
}

@Composable private fun Invitation(vm:AppViewModel,state:AppState,saving:Boolean,error:String?,onAccepted:()->Unit={}){
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

@Composable private fun Onboarding(vm:AppViewModel,state:AppState,error:String?,saving:Boolean){
 val readFailed by vm.readFailed.collectAsStateWithLifecycle()
 var step by rememberSaveable {mutableIntStateOf(if(state.schedule!=null)2 else if(state.me.name.isNotBlank())1 else 0)}
 var name by rememberSaveable {mutableStateOf(state.me.name)}
 var city by rememberSaveable {mutableStateOf(state.me.cityId)}
 var scheduleJson by rememberSaveable {mutableStateOf(Json.encodeToString(state.schedule?:defaultSchedule()))}
 val schedule=Json.decodeFromString<Schedule>(scheduleJson)
 BackHandler(enabled=step>0){step--}
 Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Text("NowUs",color=Forest,style=MaterialTheme.typography.headlineLarge)
  Text("${step+1} / 4 · ${listOf("认识你","我的节奏","留一个位置","准备好了")[step]}",style=MaterialTheme.typography.titleMedium)
  Text("本地体验 · 邀请和对方仅作演示",color=Muted)
  if(error!=null)ErrorText(error)
  when(step){
   0->{Text("从你的城市开始",style=MaterialTheme.typography.headlineSmall);ProfileFields(name,{name=it},city,{city=it});Button(enabled=!saving,onClick={vm.saveProfile(Profile(name.trim(),city)){step=1}}){Text("下一步：我的节奏")}}
   1->{Text("让作息替你轻声说明",style=MaterialTheme.typography.headlineSmall);RhythmFields(schedule,{scheduleJson=Json.encodeToString(it)});Button(enabled=!saving,onClick={vm.saveSchedule(schedule){step=2}}){Text("保存节奏并继续")}}
   2->{SectionCard("演示邀请"){
    if(state.partner==null)Invitation(vm,state,saving,error){step=3}else {Text("已模拟配对 ${state.partner.name}，对方作息待补充");Button(onClick={step=3}){Text("继续")}}
   };if(state.partner==null)OutlinedButton(onClick={vm.clearError();step=3}){Text("先独自使用")}}
   3->{Text("我们的时间，从此开始",style=MaterialTheme.typography.headlineSmall);Text(if(state.partner==null)"已保存你的资料与节奏。可以先独自使用，稍后再尝试演示配对。" else "本机演示配对已完成。只有显式填入示例后才会计算对方作息。");Button(enabled=!saving,onClick={vm.completeSetup()}){Text("开始使用")}}
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
@Composable private fun RhythmFields(schedule:Schedule,onChange:(Schedule)->Unit){
 var rest by rememberSaveable {mutableStateOf(false)}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  FilterChip(selected=!rest,onClick={rest=false},label={Text("工作日")})
  FilterChip(selected=rest,onClick={rest=true},label={Text("休息日")})
 }
 Text(if(rest)"休息日 · 周六、周日" else "工作日 · 周一至周五",style=MaterialTheme.typography.titleMedium)
 val rhythm=if(rest)schedule.rest else schedule.weekday
 fun update(value:Rhythm){onChange(if(rest)schedule.copy(rest=value)else schedule.copy(weekday=value))}
 Text("时间格式 HH:mm；睡眠可以跨夜。活动与联系独立，均不得与睡眠重叠。",style=MaterialTheme.typography.bodySmall,color=Muted)
 TimePair("睡眠",rhythm.sleepStart,rhythm.sleepEnd,{update(rhythm.copy(sleepStart=it))},{update(rhythm.copy(sleepEnd=it))})
 OutlinedTextField(rhythm.activity,{update(rhythm.copy(activity=it))},label={Text("活动名称")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 TimePair("活动",rhythm.activityStart,rhythm.activityEnd,{update(rhythm.copy(activityStart=it))},{update(rhythm.copy(activityEnd=it))})
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("通常联系偏好",Modifier.weight(1f));Switch(checked=rhythm.contactKnown,onCheckedChange={update(rhythm.copy(contactKnown=it))})}
 Text(if(rhythm.contactKnown)"以下时段愿意联系，不推断实时状态" else "联系意愿保持未知")
 if(rhythm.contactKnown)TimePair("联系",rhythm.contactStart,rhythm.contactEnd,{update(rhythm.copy(contactStart=it))},{update(rhythm.copy(contactEnd=it))})
 Text("保存时同时校验工作日和休息日模板。",style=MaterialTheme.typography.bodySmall,color=Muted)
}
@Composable private fun TimePair(title:String,start:String,end:String,onStart:(String)->Unit,onEnd:(String)->Unit){
 Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
  OutlinedTextField(start,onStart,label={Text("${title}开始")},modifier=Modifier.weight(1f),singleLine=true)
  OutlinedTextField(end,onEnd,label={Text("${title}结束")},modifier=Modifier.weight(1f),singleLine=true)
 }
}
@Composable private fun MyRhythm(vm:AppViewModel,state:AppState,error:String?,saving:Boolean,demo:Boolean){
 var profileOpen by rememberSaveable {mutableStateOf(false)}
 var name by rememberSaveable {mutableStateOf(state.me.name)}
 var city by rememberSaveable {mutableStateOf(state.me.cityId)}
 var draft by rememberSaveable {mutableStateOf(Json.encodeToString(state.schedule?:defaultSchedule()))}
 var rhythmSaved by rememberSaveable {mutableStateOf(false)}
 Text("我的节奏",style=MaterialTheme.typography.headlineSmall)
 SectionCard("${state.me.name} · ${state.me.cityName()}"){
  Text("资料与节奏仅保存在本机",color=Muted)
  TextButton(onClick={vm.clearError();profileOpen=true}){Text("编辑昵称与城市")}
 }
 SectionCard("通常作息"){
  val schedule=Json.decodeFromString<Schedule>(draft)
  RhythmFields(schedule,{draft=Json.encodeToString(it);rhythmSaved=false})
  Button(enabled=!saving,onClick={rhythmSaved=false;vm.saveSchedule(schedule){rhythmSaved=true}}){Text("保存我的节奏")}
  if(error!=null)ErrorText(error)
  if(rhythmSaved)Text("已保存到本机",color=Forest)
 }
 SectionCard("演示设置"){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("固定时间演示",Modifier.weight(1f));Switch(checked=demo,onCheckedChange=vm::setDemo)}
  Text("开启后固定为 2026/9/29，北京 22:00。退出或重新启动回到真实时间。邀请有效期始终使用真实时间。",style=MaterialTheme.typography.bodySmall,color=Muted)
  if(state.partner!=null){Text("模拟对方：${state.partner.name} · ${state.partner.cityName()}");if(state.partnerSchedule==null)Button(enabled=!saving,onClick=vm::samplePartner){Text("填入对方示例作息")}}else Invitation(vm,state,saving,error)
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

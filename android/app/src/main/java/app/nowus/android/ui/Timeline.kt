package app.nowus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.Duration
import kotlin.math.max

private data class DayData(val bounds:Window,val me:List<Segment>,val partner:List<Segment>,val windows:List<Window>)
@Composable fun Timeline(state:AppState,now:Instant){
 var dateText by rememberSaveable {mutableStateOf(now.atZone(state.me.zone()).toLocalDate().toString())}
 var full by rememberSaveable {mutableStateOf(false)}
 var detail by remember {mutableStateOf<Pair<Profile,Segment>?>(null)}
 val date=LocalDate.parse(dateText)
 val bounds=TimeEngine.dayBounds(date,state.me.zone())
 val nearby=if(!full && now>=bounds.start && now<bounds.end)Window(maxOf(bounds.start,now.minusSeconds(3*3600)),minOf(bounds.end,now.plusSeconds(3*3600)))else bounds
 val data by produceState<DayData?>(null,state,dateText,full,now.epochSecond/60){
  value=withContext(Dispatchers.Default){DayData(nearby,TimeEngine.segments(nearby.start,nearby.end,state.me,state.schedule),state.partner?.let{TimeEngine.segments(nearby.start,nearby.end,it,state.partnerSchedule)}?:emptyList(),state.partner?.let{TimeEngine.commonWindows(nearby.start,nearby.end,state.me,state.schedule,state.temporary,it,state.partnerSchedule)}?:emptyList())}
 }
 Text("我们的一天",style=MaterialTheme.typography.headlineSmall)
 Text("同一高度，是同一个瞬间。活动各自连续；绿色带表示共同联系窗口。",color=Muted)
 Text(date.toString(),modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleMedium)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
  TextButton(onClick={dateText=date.minusDays(1).toString()},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("前一天")}
  TextButton(onClick={dateText=now.atZone(state.me.zone()).toLocalDate().toString()},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("今天")}
  TextButton(onClick={dateText=date.plusDays(1).toString()},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("后一天")}
 }
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  FilterChip(selected=!full,onClick={full=false},label={Text("此刻附近")})
  FilterChip(selected=full,onClick={full=true},label={Text("查看全天")})
 }
 if(date!=now.atZone(state.me.zone()).toLocalDate() && !full)Text("选定日期显示全天；返回今天后显示此刻附近。",style=MaterialTheme.typography.bodySmall)
 val current=data
 if(current==null)Text("正在铺开这一天…") else {
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Column(Modifier.weight(1f)){Text("${state.me.name} · ${state.me.cityName()}",style=MaterialTheme.typography.titleMedium);Text(localDate(current.bounds.start,state.me),style=MaterialTheme.typography.bodySmall)}
   Column(Modifier.weight(1f)){Text(state.partner?.let{"${it.name} · ${it.cityName()}"}?:"对方尚未加入",style=MaterialTheme.typography.titleMedium);Text(state.partner?.let{localDate(current.bounds.start,it)}?:"作息未知",style=MaterialTheme.typography.bodySmall)}
  }
  AlignedTracks(current,state,now){profile,segment->detail=profile to segment}
  if(current.windows.isEmpty())Text("这段时间暂无可确认的共同窗口",style=MaterialTheme.typography.bodySmall,color=Muted)
  else current.windows.forEach{window->
   SectionCard("共同联系窗口"){
    Text("我：${shortDateTime(window.start,state.me)} – ${shortDateTime(window.end,state.me)}")
    state.partner?.let{Text("${it.name}：${shortDateTime(window.start,it)} – ${shortDateTime(window.end,it)}")}
   }
  }
  Text("每一段的详情",style=MaterialTheme.typography.titleLarge)
  Text("短时段也可以在下方查看完整日期与来源。",style=MaterialTheme.typography.bodySmall,color=Muted)
  listOf(state.me to current.me,state.partner to current.partner).forEach { (profile,segments)->
   if(profile!=null)segments.forEach{segment->
    OutlinedButton(onClick={detail=profile to segment},modifier=Modifier.fillMaxWidth()){
     Column(Modifier.fillMaxWidth()){
      Text("${profile.name} · ${segment.activity.label}")
      Text("${shortDateTime(segment.start,profile)} – ${shortDateTime(segment.end,profile)}",style=MaterialTheme.typography.bodySmall)
      Text(sourceText(segment.activity.source),style=MaterialTheme.typography.bodySmall)
     }
    }
   }
  }
 }
 detail?.let { (profile,segment)->
  AlertDialog(onDismissRequest={detail=null},title={Text("${profile.name} · ${segment.activity.label}")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("${profile.cityName()}：${shortDateTime(segment.start,profile)} – ${shortDateTime(segment.end,profile)}")
   Text(sourceText(segment.activity.source));Text("活动与联系偏好独立；作息不能代表本人实时状态。")
  }},confirmButton={TextButton(onClick={detail=null}){Text("知道了")}})
 }
}
@Composable private fun AlignedTracks(data:DayData,state:AppState,now:Instant,onSegment:(Profile,Segment)->Unit){
 val seconds=Duration.between(data.bounds.start,data.bounds.end).seconds.toFloat()
 val height=max(420f,seconds/3600f*76f).dp
 fun fraction(at:Instant)=Duration.between(data.bounds.start,at).seconds.toFloat()/seconds
 if(now>=data.bounds.start && now<data.bounds.end)Text("绿色横线 · 此刻：我 ${shortDateTime(now,state.me)}${state.partner?.let{" · ${it.name} ${shortDateTime(now,it)}"}.orEmpty()}",style=MaterialTheme.typography.bodySmall,color=Forest)
 BoxWithConstraints(Modifier.fillMaxWidth().height(height).background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp))){
  val gutter=if(LocalDensity.current.fontScale>1.3f)72.dp else 56.dp
  val tickLabelHeight=with(LocalDensity.current){38.sp.toDp()}
  val trackWidth=(maxWidth-gutter*2-14.dp)/2
  data.windows.forEach{window->
   Box(Modifier.offset(x=gutter,y=height*fraction(window.start)).width(maxWidth-gutter*2).height(height*(fraction(window.end)-fraction(window.start))).background(Forest.copy(alpha=.18f)))
  }
  var tick=data.bounds.start.atZone(state.me.zone()).truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1).toInstant()
  while(tick<data.bounds.end){
   val time=tick
   val position=height*fraction(time)
   val labelPosition=(position-tickLabelHeight/2).coerceIn(0.dp,height-tickLabelHeight)
   Text(shortDateTime(time,state.me).replace(" ","\n"),modifier=Modifier.offset(y=labelPosition).width(gutter),style=MaterialTheme.typography.bodySmall,color=Muted)
   HorizontalDivider(Modifier.offset(x=gutter,y=position).width(maxWidth-gutter*2),color=Muted.copy(alpha=.16f))
   Text(state.partner?.let{shortDateTime(time,it).replace(" ","\n")}?:"未知",modifier=Modifier.offset(x=maxWidth-gutter,y=labelPosition).width(gutter).padding(start=4.dp),style=MaterialTheme.typography.bodySmall,color=Muted)
   tick=tick.plusSeconds(3600)
  }
  fun segmentColor(segment:Segment)=when{segment.activity.source==ActivitySource.UNKNOWN->Color(0xFFE9ECE8);segment.activity.label=="睡觉"->Night;else->Sage}
  @Composable fun Track(profile:Profile,segments:List<Segment>,x:androidx.compose.ui.unit.Dp){
   segments.forEach{segment->
    val segmentHeight=height*(fraction(segment.end)-fraction(segment.start))
    val night=segment.activity.label=="睡觉"
    Box(Modifier.offset(x=x,y=height*fraction(segment.start)).width(trackWidth).height(segmentHeight).padding(vertical=1.dp).background(segmentColor(segment),RoundedCornerShape(9.dp)).clickable{onSegment(profile,segment)}.semantics{contentDescription="${profile.name} ${segment.activity.label} ${shortDateTime(segment.start,profile)} 至 ${shortDateTime(segment.end,profile)} ${sourceText(segment.activity.source)}"}.padding(8.dp)){
     if(segmentHeight>46.dp)Text(segment.activity.label,color=if(night)Color.White else Ink,style=MaterialTheme.typography.bodyMedium)
    }
   }
  }
  Track(state.me,data.me,gutter)
  state.partner?.let{Track(it,data.partner,gutter+trackWidth+14.dp)}
  data.windows.forEach{window->
   Box(Modifier.offset(x=gutter+trackWidth,y=height*fraction(window.start)).width(14.dp).height(height*(fraction(window.end)-fraction(window.start))).background(Forest))
  }
  if(state.partner==null)Box(Modifier.offset(x=gutter+trackWidth+14.dp).width(trackWidth).fillMaxHeight().background(Color(0xFFE9ECE8),RoundedCornerShape(9.dp)).padding(8.dp)){Text("未加入\n作息未知",style=MaterialTheme.typography.bodyMedium,color=Muted)}
  if(now>=data.bounds.start && now<data.bounds.end){
   val position=height*fraction(now)
   HorizontalDivider(Modifier.offset(x=gutter,y=position).width(maxWidth-gutter*2),thickness=2.dp,color=Forest)
  }
 }
 Text("左右刻度分别为双方城市的日期与时间；双方轨道按同一瞬间对齐。绿色中线表示共同窗口。",style=MaterialTheme.typography.bodySmall,color=Muted)
}

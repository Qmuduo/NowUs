package app.nowus.android.domain

object Rules {
 fun validateProfile(profile: Profile): ValidationResult {
  val errors=mutableListOf<String>()
  if(profile.name.isBlank() || profile.name.codePointCount(0,profile.name.length)>20) errors.add("昵称应为 1–20 个字符")
  if(Cities.byId(profile.cityId)==null) errors.add("请选择有效城市")
  return ValidationResult(errors.isEmpty(),errors)
 }
 fun validateRhythm(rhythm: Rhythm): ValidationResult {
  if(rhythm.blocks.isNotEmpty()) return validateRoutineBlocks(rhythm)
  val errors=mutableListOf<String>()
  val sleepStart=TimeEngine.parseMinute(rhythm.sleepStart);val sleepEnd=TimeEngine.parseMinute(rhythm.sleepEnd)
  val activityStart=TimeEngine.parseMinute(rhythm.activityStart);val activityEnd=TimeEngine.parseMinute(rhythm.activityEnd)
  val contactStart=if(rhythm.contactKnown)TimeEngine.parseMinute(rhythm.contactStart) else null
  val contactEnd=if(rhythm.contactKnown)TimeEngine.parseMinute(rhythm.contactEnd) else null
  if(sleepStart==null || sleepEnd==null || activityStart==null || activityEnd==null || sleepStart==sleepEnd || activityStart==activityEnd ||
   (rhythm.contactKnown && (contactStart==null || contactEnd==null || contactStart==contactEnd))) errors.add("时间格式应为 HH:mm，起止不得相同")
  if(errors.isEmpty()) {
   if(activityStart!!>=activityEnd!!) errors.add("活动时段不可跨日")
   else if(TimeEngine.intervalsOverlap(sleepStart!!,sleepEnd!!,activityStart,activityEnd)) errors.add("活动不可与睡眠重叠")
   if(rhythm.contactKnown && contactStart!=null && contactEnd!=null && TimeEngine.intervalsOverlap(sleepStart!!,sleepEnd!!,contactStart,contactEnd)) errors.add("联系不可与睡眠重叠")
  }
  if(rhythm.activity.isBlank()) errors.add("请填写活动名称")
  return ValidationResult(errors.isEmpty(),errors)
 }
 private fun validateRoutineBlocks(rhythm: Rhythm): ValidationResult {
  val errors=mutableListOf<String>()
  val blocks=rhythm.blocks
  if(blocks.size>20 || blocks.any { it.id.isBlank() } || blocks.map { it.id }.distinct().size!=blocks.size || blocks.count { it.id=="sleep" }!=1) {
   errors.add("日常时段列表无效")
  }
  if(blocks.any { it.label.isBlank() || it.label.codePointCount(0,it.label.length)>20 }) errors.add("时段名称应为 1–20 个字符")
  val parsed=blocks.map { block->ParsedRoutineBlock(block,TimeEngine.parseMinute(block.start),TimeEngine.parseMinute(block.end)) }
  val invalid=parsed.any { item->
   val start=item.start;val end=item.end
   start==null || end==null || start==end || (item.block.id!="sleep" && start>=end)
  }
  if(invalid) errors.add("时段时间格式无效，睡觉以外的时段不可跨日")
  if(!invalid && blocks.size<=20) {
   val overlap=parsed.indices.any { i->(i+1 until parsed.size).any { j->
    val first=parsed[i];val second=parsed[j]
    TimeEngine.intervalsOverlap(first.start!!,first.end!!,second.start!!,second.end!!)
   }}
   if(overlap) errors.add("日常时段不能重叠")
  }
  if(rhythm.contactKnown) {
   val start=TimeEngine.parseMinute(rhythm.contactStart);val end=TimeEngine.parseMinute(rhythm.contactEnd)
   if(start==null || end==null || start==end) errors.add("时间格式应为 HH:mm，起止不得相同")
   else if(!invalid && blocks.size<=20) {
    val sleep=parsed.firstOrNull { it.block.id=="sleep" }
    if(sleep?.start!=null && sleep.end!=null && TimeEngine.intervalsOverlap(sleep.start,sleep.end,start,end)) errors.add("联系不可与睡眠重叠")
   }
  }
  return ValidationResult(errors.isEmpty(),errors.distinct())
 }
 fun saveNote(state: AppState,text: String,nowMillis: Long): RuleResult = if(text.codePointCount(0,text.length)>120) RuleResult(state,"留言最多 120 个字符") else RuleResult(state.copy(note=if(text.isBlank()) null else Note(text,nowMillis)))
 fun createInvite(state: AppState,code: String,nowMillis: Long): RuleResult = when {
  state.partner!=null -> RuleResult(state,"已经配对")
  code.isBlank() -> RuleResult(state,"邀请码不能为空")
  else -> RuleResult(state.copy(invite=Invite(code,Math.addExact(nowMillis,86400000))))
 }
 fun revokeInvite(state: AppState)=RuleResult(state.copy(invite=state.invite?.copy(revoked=true)))
 fun acceptInvite(state: AppState,code: String,partner: Profile,nowMillis: Long): RuleResult {
  val invite=state.invite
  val error=when {
   state.partner!=null -> "已经配对"
   invite==null || invite.code!=code -> "邀请码错误"
   invite.revoked -> "邀请已撤销"
   nowMillis>=invite.expiresMillis -> "邀请已过期"
   !validateProfile(partner).valid -> "伴侣资料无效"
   else -> null
  }
  return if(error!=null) RuleResult(state,error) else RuleResult(state.copy(partner=partner,partnerSchedule=null,invite=null))
 }
}

private data class ParsedRoutineBlock(val block:RoutineBlock,val start:Int?,val end:Int?)

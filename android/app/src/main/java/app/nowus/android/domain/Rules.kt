package app.nowus.android.domain

object Rules {
 fun validateProfile(profile: Profile): ValidationResult {
  val errors=mutableListOf<String>()
  if(profile.name.isBlank() || profile.name.codePointCount(0,profile.name.length)>20) errors.add("昵称应为 1–20 个字符")
  if(Cities.byId(profile.cityId)==null) errors.add("请选择有效城市")
  return ValidationResult(errors.isEmpty(),errors)
 }
 fun validateRhythm(rhythm: Rhythm): ValidationResult {
  val pairs=listOf(rhythm.sleepStart to rhythm.sleepEnd,rhythm.activityStart to rhythm.activityEnd,rhythm.contactStart to rhythm.contactEnd)
  val errors=mutableListOf<String>()
  if(pairs.any {TimeEngine.parseMinute(it.first)==null || TimeEngine.parseMinute(it.second)==null || it.first==it.second}) errors.add("时间格式应为 HH:mm，起止不得相同")
  if(errors.isEmpty()) {
   if(TimeEngine.parseMinute(rhythm.activityStart)!!>=TimeEngine.parseMinute(rhythm.activityEnd)!!) errors.add("活动时段不可跨日")
   if((0 until 1440).any {TimeEngine.contains(it,rhythm.sleepStart,rhythm.sleepEnd) && TimeEngine.contains(it,rhythm.activityStart,rhythm.activityEnd)}) errors.add("活动不可与睡眠重叠")
   if(rhythm.contactKnown && (0 until 1440).any {TimeEngine.contains(it,rhythm.sleepStart,rhythm.sleepEnd) && TimeEngine.contains(it,rhythm.contactStart,rhythm.contactEnd)}) errors.add("联系不可与睡眠重叠")
  }
  if(rhythm.activity.isBlank()) errors.add("请填写活动名称")
  return ValidationResult(errors.isEmpty(),errors)
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

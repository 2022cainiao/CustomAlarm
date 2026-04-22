const STORAGE_KEY = "custom_alarm_preview_state_v1";

const text = {
  weekdays: [
    [1, "周一"],
    [2, "周二"],
    [3, "周三"],
    [4, "周四"],
    [5, "周五"],
    [6, "周六"],
    [7, "周日"],
  ],
  repeatOnce: "仅一次",
  repeatWorkdays: "工作日",
  repeatWeekend: "周末",
  repeatEveryday: "每天",
  unnamedAlarm: "未命名闹钟",
  effective: "生效中",
  blocked: "未生效",
  snooze: "贪睡",
  vibrate: "震动",
  nextAlarm: "下一次响铃",
  none: "暂无",
  noActiveAlarms: "当前没有生效中的闹钟",
  routineGroups: "作息组数量",
  routineGroupHint: "每个作息组都有独立的总开关，且不会覆盖组内单闹钟状态。",
  overviewTitle: "实时总览",
  noActiveScheduled: "当前没有生效中的闹钟",
  nextRingAt: "下一次响铃时间",
  overviewCopy: "作息闹钟是否生效 = 作息组总开关 + 子闹钟开关同时开启。",
  standardActive: "普通闹钟启用",
  routineActive: "作息闹钟生效",
  groupsEnabled: "作息组启用",
  exactAlarm: "精确闹钟",
  exactAlarmDesc: "精确系统调度能力",
  notifications: "通知权限",
  notificationsDesc: "响铃通知展示能力",
  enabled: "已开启",
  disabled: "已关闭",
  currentlyInactive: "当前未生效",
  edit: "编辑",
  delete: "删除",
  move: "移动",
  moveIntoRoutine: "移入作息组",
  noStandardAlarms: "还没有普通闹钟",
  createWithAdd: "可以通过上方按钮新增一个。",
  next: "下次",
  groupEnabled: "作息组已开启",
  groupDisabled: "作息组已关闭",
  alarmsInside: "组内闹钟",
  openDetail: "查看详情",
  editGroup: "编辑作息组",
  noRoutineGroups: "还没有作息组",
  standardAlarm: "普通闹钟",
  routineAlarm: "作息闹钟",
  noTimeline: "当前没有生效闹钟",
  timelineHint: "启用普通闹钟或作息组后，这里会显示统一时间线。",
  alarmsInsideCount: "组内闹钟",
  effectiveCount: "当前生效",
  noAlarmsYet: "还没有闹钟",
  addFirstAlarm: "给这个作息组添加第一个闹钟吧。",
  standardAlarmEditor: "普通闹钟",
  routineAlarmEditor: "作息闹钟",
  editAlarm: "编辑闹钟",
  addAlarm: "新增闹钟",
  label: "标签",
  enableAfterSave: "保存后启用",
  routineEditorHint: "作息闹钟是否生效，取决于作息组和子闹钟是否同时开启。",
  standardEditorHint: "普通闹钟只受自身开关控制。",
  routineGroupEditor: "作息组",
  editRoutineGroup: "编辑作息组",
  addRoutineGroup: "新建作息组",
  routineName: "作息组名称",
  enableGroupAfterSave: "保存后启用作息组",
  routineOverwriteHint: "作息组总开关不会覆盖组内单个闹钟的开关状态。",
  routineNameRequired: "请输入作息组名称。",
  routineSaved: "作息组已保存。",
  invalidTime: "时间输入不合法。",
  createRoutineFirst: "请先创建作息组。",
  alarmSaved: "闹钟已保存。",
  deleted: "已删除。",
  exportFileName: "custom-alarm-preview-state.json",
  demoReset: "演示数据已重置。",
  moveAlarmTitle: "移动闹钟",
  moveNormalDescription: "选择要移入的作息组。",
  moveRoutineDescription: "你可以把这个闹钟移回普通闹钟，或移动到其他作息组。",
  moveToStandard: "移回普通闹钟",
  noOtherRoutineGroups: "没有其他可移动的作息组",
  movedToStandard: "闹钟已移回普通闹钟。",
  movedToGroup: "闹钟已移动到作息组。",
  close: "关闭",
  save: "保存",
  reset: "重置",
  permissions: "权限",
  home: "首页",
  preview: "网页预览",
  customAlarmTitle: "自定义闹钟与作息组",
  heroCopy: "在 Android 真机打磨前，先预览核心体验：普通闹钟、作息组、组级总开关，以及组内闹钟统一管理与快速移动。",
  standardSection: "普通闹钟",
  routineSection: "作息模式",
  timelineTitle: "统一时间线",
  timelineTag: "即将响铃",
  detailTag: "作息详情",
  detailPlaceholderTitle: "请选择一个作息组",
  detailPlaceholderCopy: "右侧会展示作息组总开关，以及该组内的全部闹钟。",
  addAlarmButton: "+ 添加闹钟",
  addRoutineButton: "+ 新建作息组",
  addRoutineAlarmButton: "+ 添加组内闹钟",
  exportJson: "导出 JSON",
  settingsTitle: "闹钟能力开关",
  exactAlarmSettingCopy: "模拟 Android 的精确闹钟调度权限。",
  notificationsSettingCopy: "模拟响铃通知是否可用。",
  groupOwnership: "所属作息组",
  repeatLabel: "重复日期",
  hourLabel: "小时",
  minuteLabel: "分钟",
  vibrateLabel: "震动",
  snoozeLabel: "贪睡",
  snoozeMinutesLabel: "贪睡分钟",
  moveEmptyCopy: "当前没有可选的目标作息组。",
};

function uid() {
  return `${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function createNormalAlarm(label, hour, minute, repeatDays, enabled) {
  return {
    id: uid(),
    type: "normal",
    label,
    hour,
    minute,
    repeatDays,
    vibrate: true,
    snoozeEnabled: true,
    snoozeMinutes: 10,
    enabled,
    routineGroupId: null,
  };
}

function createRoutineAlarm(label, hour, minute, repeatDays, routineGroupId, enabled) {
  return {
    ...createNormalAlarm(label, hour, minute, repeatDays, enabled),
    type: "routine",
    routineGroupId,
  };
}

function demoState() {
  const workdayId = uid();
  const weekendId = uid();
  return {
    permissions: { exactAlarm: true, notifications: true },
    selectedRoutineId: workdayId,
    routineGroups: [
      { id: workdayId, name: "工作日作息", enabled: true },
      { id: weekendId, name: "周末作息", enabled: false },
    ],
    alarms: [
      createNormalAlarm("早起学习", 6, 40, [1, 2, 3, 4, 5], true),
      createNormalAlarm("午休提醒", 13, 10, [], false),
      createRoutineAlarm("起床", 7, 0, [1, 2, 3, 4, 5], workdayId, true),
      createRoutineAlarm("出门准备", 8, 5, [1, 2, 3, 4, 5], workdayId, true),
      createRoutineAlarm("周末起床", 9, 20, [6, 7], weekendId, true),
    ],
  };
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return demoState();
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed?.alarms) || !Array.isArray(parsed?.routineGroups)) return demoState();
    return normalizeState(parsed);
  } catch {
    return demoState();
  }
}

function normalizeState(rawState) {
  const fallback = demoState();
  const permissions = {
    exactAlarm: rawState?.permissions?.exactAlarm ?? fallback.permissions.exactAlarm,
    notifications: rawState?.permissions?.notifications ?? fallback.permissions.notifications,
  };
  const routineGroups = rawState.routineGroups.map((routine) => ({
    id: routine.id || uid(),
    name: routine.name || "未命名作息组",
    enabled: routine.enabled ?? true,
  }));

  const validRoutineIds = new Set(routineGroups.map((routine) => routine.id));
  const alarms = rawState.alarms.map((alarm) => {
    const isRoutine = alarm.type === "routine" && validRoutineIds.has(alarm.routineGroupId);
    return {
      id: alarm.id || uid(),
      type: isRoutine ? "routine" : "normal",
      label: alarm.label || "",
      hour: Number.isFinite(alarm.hour) ? alarm.hour : 7,
      minute: Number.isFinite(alarm.minute) ? alarm.minute : 0,
      repeatDays: Array.isArray(alarm.repeatDays) ? alarm.repeatDays : [],
      vibrate: alarm.vibrate ?? true,
      snoozeEnabled: alarm.snoozeEnabled ?? true,
      snoozeMinutes: Number.isFinite(alarm.snoozeMinutes) ? alarm.snoozeMinutes : 10,
      enabled: alarm.enabled ?? true,
      routineGroupId: isRoutine ? alarm.routineGroupId : null,
    };
  });

  return {
    permissions,
    selectedRoutineId: validRoutineIds.has(rawState.selectedRoutineId)
      ? rawState.selectedRoutineId
      : routineGroups[0]?.id || null,
    routineGroups,
    alarms,
  };
}

let state = loadState();
const editor = { mode: "alarm", alarmType: "normal", editingId: null };
const moveDialog = { alarmId: null };

const $ = (selector) => document.querySelector(selector);
const els = {
  pageTitle: $("#pageTitle"),
  heroEyebrow: $("#heroEyebrow"),
  heroTitle: $("#heroTitle"),
  heroCopy: $("#heroCopy"),
  screenLabel: $("#screenLabel"),
  screenTitle: $("#screenTitle"),
  permissionButton: $("#openSettingsButton"),
  resetButton: $("#resetDemoButton"),
  standardKicker: $("#standardKicker"),
  standardTitle: $("#standardTitle"),
  addNormalAlarmButton: $("#addNormalAlarmButton"),
  routineKicker: $("#routineKicker"),
  routineTitle: $("#routineTitle"),
  addRoutineButton: $("#addRoutineButton"),
  timelineTag: $("#timelineTag"),
  timelineTitle: $("#timelineTitle"),
  exportStateButton: $("#exportStateButton"),
  detailTag: $("#detailTag"),
  detailPlaceholderTitle: $("#detailPlaceholderTitle"),
  detailPlaceholderCopy: $("#detailPlaceholderCopy"),
  editRoutineButton: $("#editRoutineButton"),
  addRoutineAlarmButton: $("#addRoutineAlarmButton"),
  editorCloseButton: $("#closeEditorButton"),
  editorDeleteButton: $("#deleteButton"),
  editorSaveButton: $("#saveButton"),
  settingsCloseButton: $("#closeSettingsButton"),
  settingsTag: $("#settingsTag"),
  settingsTitle: $("#settingsTitle"),
  exactAlarmTitle: $("#exactAlarmTitle"),
  exactAlarmCopy: $("#exactAlarmCopy"),
  notificationsTitle: $("#notificationsTitle"),
  notificationsCopy: $("#notificationsCopy"),
  timeHourLabel: $("#timeHourLabel"),
  timeMinuteLabel: $("#timeMinuteLabel"),
  routineGroupLabel: $("#routineGroupLabel"),
  repeatLabel: $("#repeatLabel"),
  vibrateLabel: $("#vibrateLabel"),
  snoozeLabel: $("#snoozeLabel"),
  snoozeMinutesLabel: $("#snoozeMinutesLabel"),
  moveModal: $("#moveModal"),
  moveTitle: $("#moveTitle"),
  moveDescription: $("#moveDescription"),
  moveOptions: $("#moveOptions"),
  moveToStandardButton: $("#moveToStandardButton"),
  moveCloseButton: $("#closeMoveButton"),
  heroStats: $("#heroStats"),
  overviewCard: $("#overviewCard"),
  permissionStrip: $("#permissionStrip"),
  normalAlarmList: $("#normalAlarmList"),
  routineGroupList: $("#routineGroupList"),
  timelineList: $("#timelineList"),
  detailEmpty: $("#detailEmpty"),
  detailContent: $("#detailContent"),
  detailTitle: $("#detailTitle"),
  detailSummary: $("#detailSummary"),
  detailGroupToggle: $("#detailGroupToggle"),
  detailAlarmList: $("#detailAlarmList"),
  editorModal: $("#editorModal"),
  editorTag: $("#editorTag"),
  editorTitle: $("#editorTitle"),
  editorForm: $("#editorForm"),
  timeFields: $("#timeFields"),
  labelFieldTitle: $("#labelFieldTitle"),
  hourInput: $("#hourInput"),
  minuteInput: $("#minuteInput"),
  labelInput: $("#labelInput"),
  routineGroupField: $("#routineGroupField"),
  routineGroupSelect: $("#routineGroupSelect"),
  weekdayBlock: $("#weekdayBlock"),
  weekdayGrid: $("#weekdayGrid"),
  alarmToggleFields: $("#alarmToggleFields"),
  vibrateInput: $("#vibrateInput"),
  snoozeEnabledInput: $("#snoozeEnabledInput"),
  snoozeMinutesInput: $("#snoozeMinutesInput"),
  snoozeField: $("#snoozeField"),
  enabledInput: $("#enabledInput"),
  enabledFieldLabel: $("#enabledFieldLabel"),
  editorHint: $("#editorHint"),
  settingsModal: $("#settingsModal"),
  exactPermissionToggle: $("#exactPermissionToggle"),
  notificationPermissionToggle: $("#notificationPermissionToggle"),
  toast: $("#toast"),
};

function hydrateStaticText() {
  document.title = text.customAlarmTitle;
  els.pageTitle.textContent = text.customAlarmTitle;
  els.heroEyebrow.textContent = text.preview;
  els.heroTitle.textContent = text.customAlarmTitle;
  els.heroCopy.textContent = text.heroCopy;
  els.screenLabel.textContent = text.home;
  els.screenTitle.textContent = text.customAlarmTitle;
  els.permissionButton.textContent = text.permissions;
  els.resetButton.textContent = text.reset;
  els.standardKicker.textContent = text.standardSection;
  els.standardTitle.textContent = text.standardSection;
  els.addNormalAlarmButton.textContent = text.addAlarmButton;
  els.routineKicker.textContent = text.routineSection;
  els.routineTitle.textContent = "作息组";
  els.addRoutineButton.textContent = text.addRoutineButton;
  els.timelineTag.textContent = text.timelineTag;
  els.timelineTitle.textContent = text.timelineTitle;
  els.exportStateButton.textContent = text.exportJson;
  els.detailTag.textContent = text.detailTag;
  els.detailPlaceholderTitle.textContent = text.detailPlaceholderTitle;
  els.detailPlaceholderCopy.textContent = text.detailPlaceholderCopy;
  els.editRoutineButton.textContent = text.editGroup;
  els.addRoutineAlarmButton.textContent = text.addRoutineAlarmButton;
  els.editorCloseButton.textContent = text.close;
  els.editorDeleteButton.textContent = text.delete;
  els.editorSaveButton.textContent = text.save;
  els.settingsCloseButton.textContent = text.close;
  els.settingsTag.textContent = text.permissions;
  els.settingsTitle.textContent = text.settingsTitle;
  els.exactAlarmTitle.textContent = text.exactAlarm;
  els.exactAlarmCopy.textContent = text.exactAlarmSettingCopy;
  els.notificationsTitle.textContent = text.notifications;
  els.notificationsCopy.textContent = text.notificationsSettingCopy;
  els.timeHourLabel.textContent = text.hourLabel;
  els.timeMinuteLabel.textContent = text.minuteLabel;
  els.routineGroupLabel.textContent = text.groupOwnership;
  els.repeatLabel.textContent = text.repeatLabel;
  els.vibrateLabel.textContent = text.vibrateLabel;
  els.snoozeLabel.textContent = text.snoozeLabel;
  els.snoozeMinutesLabel.textContent = text.snoozeMinutesLabel;
  els.moveCloseButton.textContent = text.close;
  els.moveToStandardButton.textContent = text.moveToStandard;
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function toast(message) {
  els.toast.textContent = message;
  els.toast.classList.remove("hidden");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => els.toast.classList.add("hidden"), 1800);
}

function formatTime(hour, minute) {
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function getGroup(id) {
  return state.routineGroups.find((item) => item.id === id);
}

function repeatText(days) {
  const sorted = [...days].sort((a, b) => a - b).join(",");
  if (!sorted) return text.repeatOnce;
  if (sorted === "1,2,3,4,5") return text.repeatWorkdays;
  if (sorted === "6,7") return text.repeatWeekend;
  if (sorted === "1,2,3,4,5,6,7") return text.repeatEveryday;
  return sorted
    .split(",")
    .map((day) => text.weekdays.find(([value]) => value === Number(day))?.[1])
    .join(" ");
}

function isEffective(alarm) {
  if (alarm.type === "normal") return alarm.enabled;
  return alarm.enabled && Boolean(getGroup(alarm.routineGroupId)?.enabled);
}

function byTime(a, b) {
  return a.hour * 60 + a.minute - (b.hour * 60 + b.minute);
}

function upcoming() {
  return [...state.alarms].filter(isEffective).sort(byTime);
}

function routineStats(routine) {
  const alarms = state.alarms.filter((alarm) => alarm.routineGroupId === routine.id).sort(byTime);
  const active = alarms.filter((alarm) => alarm.enabled && routine.enabled);
  return { alarms, active, next: active[0] || null };
}

function ensureSelectedRoutine() {
  if (state.selectedRoutineId && getGroup(state.selectedRoutineId)) return;
  state.selectedRoutineId = state.routineGroups[0]?.id || null;
}

function alarmBadges(alarm) {
  return `
    <span class="badge ${isEffective(alarm) ? "active" : "sleeping"}">${isEffective(alarm) ? text.effective : text.blocked}</span>
    <span class="badge">${repeatText(alarm.repeatDays)}</span>
    ${alarm.snoozeEnabled ? `<span class="badge">${text.snooze} ${alarm.snoozeMinutes} 分钟</span>` : ""}
    ${alarm.vibrate ? `<span class="badge">${text.vibrate}</span>` : ""}
  `;
}

function renderHero() {
  const next = upcoming()[0];
  els.heroStats.innerHTML = `
    <div class="stat-card">
      <p class="panel-tag">${text.nextAlarm}</p>
      <strong>${next ? formatTime(next.hour, next.minute) : text.none}</strong>
      <div class="stat-meta">${next ? next.label || text.unnamedAlarm : text.noActiveAlarms}</div>
    </div>
    <div class="stat-card">
      <p class="panel-tag">${text.routineGroups}</p>
      <strong>${state.routineGroups.length}</strong>
      <div class="stat-meta">${text.routineGroupHint}</div>
    </div>
  `;
}

function renderOverview() {
  const standardActive = state.alarms.filter((alarm) => alarm.type === "normal" && alarm.enabled).length;
  const routineActive = state.alarms.filter((alarm) => alarm.type === "routine" && isEffective(alarm)).length;
  const groupsEnabled = state.routineGroups.filter((item) => item.enabled).length;
  const next = upcoming()[0];
  els.overviewCard.innerHTML = `
    <p class="screen-label">${text.overviewTitle}</p>
    <h3>${next ? `${text.nextRingAt} ${formatTime(next.hour, next.minute)}` : text.noActiveScheduled}</h3>
    <p class="overview-copy">${text.overviewCopy}</p>
    <div class="overview-grid">
      <div class="overview-chip"><span>${text.standardActive}</span><strong>${standardActive}</strong></div>
      <div class="overview-chip"><span>${text.routineActive}</span><strong>${routineActive}</strong></div>
      <div class="overview-chip"><span>${text.groupsEnabled}</span><strong>${groupsEnabled}/${state.routineGroups.length}</strong></div>
    </div>
  `;
}

function renderPermissions() {
  els.permissionStrip.innerHTML = [
    [text.exactAlarm, state.permissions.exactAlarm, text.exactAlarmDesc],
    [text.notifications, state.permissions.notifications, text.notificationsDesc],
  ]
    .map(
      ([title, enabled, desc]) => `
        <div class="permission-pill">
          <div><strong>${title}</strong><div class="alarm-meta">${desc}</div></div>
          <span class="permission-status ${enabled ? "" : "off"}">${enabled ? text.enabled : text.disabled}</span>
        </div>
      `,
    )
    .join("");
}

function alarmCard(alarm, variant = "normal") {
  const moveLabel = variant === "normal" ? text.moveIntoRoutine : text.move;
  return `
    <article class="${variant === "routine" ? "detail-alarm-card" : "alarm-card"}">
      <div class="alarm-top">
        <div>
          <div class="alarm-time">${formatTime(alarm.hour, alarm.minute)}</div>
          <strong>${alarm.label || text.unnamedAlarm}</strong>
          <div class="alarm-meta">${isEffective(alarm) ? repeatText(alarm.repeatDays) : text.currentlyInactive}</div>
          <div class="badge-row">${alarmBadges(alarm)}</div>
        </div>
        <label class="switch">
          <input type="checkbox" data-action="toggle-alarm" data-id="${alarm.id}" ${alarm.enabled ? "checked" : ""} />
          <span class="switch-track"></span>
        </label>
      </div>
      <div class="alarm-actions">
        <button class="pill-button" data-action="edit-alarm" data-id="${alarm.id}">${text.edit}</button>
        <button class="pill-button" data-action="move-alarm" data-id="${alarm.id}">${moveLabel}</button>
        <button class="pill-button" data-action="delete-alarm" data-id="${alarm.id}">${text.delete}</button>
      </div>
    </article>
  `;
}

function renderNormalAlarms() {
  const alarms = state.alarms.filter((alarm) => alarm.type === "normal").sort(byTime);
  els.normalAlarmList.innerHTML = alarms.length
    ? alarms.map((alarm) => alarmCard(alarm, "normal")).join("")
    : `<article class="alarm-card"><strong>${text.noStandardAlarms}</strong><div class="alarm-meta">${text.createWithAdd}</div></article>`;
}

function renderRoutineGroups() {
  els.routineGroupList.innerHTML = state.routineGroups.length
    ? state.routineGroups
        .map((routine) => {
          const stats = routineStats(routine);
          return `
            <article class="routine-card">
              <div class="routine-top">
                <div>
                  <strong>${routine.name}</strong>
                  <div class="routine-meta">${stats.active.length} 个生效闹钟 | ${text.next} ${stats.next ? formatTime(stats.next.hour, stats.next.minute) : text.none}</div>
                  <div class="badge-row">
                    <span class="badge ${routine.enabled ? "active" : "sleeping"}">${routine.enabled ? text.groupEnabled : text.groupDisabled}</span>
                    <span class="badge">${stats.alarms.length} 个${text.alarmsInside}</span>
                  </div>
                </div>
                <label class="switch">
                  <input type="checkbox" data-action="toggle-group" data-id="${routine.id}" ${routine.enabled ? "checked" : ""} />
                  <span class="switch-track"></span>
                </label>
              </div>
              <div class="routine-actions">
                <button class="pill-button" data-action="open-group" data-id="${routine.id}">${text.openDetail}</button>
                <button class="pill-button" data-action="edit-group" data-id="${routine.id}">${text.editGroup}</button>
              </div>
            </article>
          `;
        })
        .join("")
    : `<article class="routine-card"><strong>${text.noRoutineGroups}</strong></article>`;
}

function renderTimeline() {
  const alarms = upcoming();
  els.timelineList.innerHTML = alarms.length
    ? alarms
        .map(
          (alarm) => `
            <article class="timeline-card">
              <strong>${formatTime(alarm.hour, alarm.minute)} | ${alarm.label || text.unnamedAlarm}</strong>
              <div class="timeline-meta">${alarm.type === "normal" ? text.standardAlarm : getGroup(alarm.routineGroupId)?.name || text.routineAlarm}</div>
            </article>
          `,
        )
        .join("")
    : `<article class="timeline-card"><strong>${text.noTimeline}</strong><div class="timeline-meta">${text.timelineHint}</div></article>`;
}

function renderDetail() {
  const routine = getGroup(state.selectedRoutineId);
  if (!routine) {
    els.detailEmpty.classList.remove("hidden");
    els.detailContent.classList.add("hidden");
    return;
  }

  const stats = routineStats(routine);
  els.detailEmpty.classList.add("hidden");
  els.detailContent.classList.remove("hidden");
  els.detailTitle.textContent = routine.name;
  els.detailSummary.textContent = `${stats.alarms.length} 个${text.alarmsInsideCount} | ${stats.active.length} 个${text.effectiveCount}`;
  els.detailGroupToggle.checked = routine.enabled;
  els.detailAlarmList.innerHTML = stats.alarms.length
    ? stats.alarms.map((alarm) => alarmCard(alarm, "routine")).join("")
    : `<article class="detail-alarm-card"><strong>${text.noAlarmsYet}</strong><div class="detail-subtext">${text.addFirstAlarm}</div></article>`;
}

function renderWeekdays(selected = []) {
  els.weekdayGrid.innerHTML = text.weekdays
    .map(
      ([value, label]) =>
        `<button type="button" class="weekday-chip ${selected.includes(value) ? "active" : ""}" data-action="toggle-weekday" data-day="${value}">${label}</button>`,
    )
    .join("");
}

function renderRoutineOptions(selectedId) {
  if (!state.routineGroups.length) {
    els.routineGroupSelect.innerHTML = `<option value="">${text.createRoutineFirst}</option>`;
    return;
  }

  els.routineGroupSelect.innerHTML = state.routineGroups
    .map(
      (routine) =>
        `<option value="${routine.id}" ${routine.id === selectedId ? "selected" : ""}>${routine.name}</option>`,
    )
    .join("");
}

function openAlarmEditor({ alarmType, alarmId = null, routineGroupId = null }) {
  const alarm = state.alarms.find((item) => item.id === alarmId);
  editor.mode = "alarm";
  editor.alarmType = alarmType;
  editor.editingId = alarmId;
  els.editorModal.classList.remove("hidden");
  els.editorTag.textContent = alarmType === "normal" ? text.standardAlarmEditor : text.routineAlarmEditor;
  els.editorTitle.textContent = alarm ? text.editAlarm : text.addAlarm;
  els.labelFieldTitle.textContent = text.label;
  els.enabledFieldLabel.textContent = text.enableAfterSave;
  els.timeFields.classList.remove("hidden");
  els.weekdayBlock.classList.remove("hidden");
  els.alarmToggleFields.classList.remove("hidden");
  els.snoozeField.classList.remove("hidden");
  els.editorDeleteButton.classList.toggle("hidden", !alarm);
  els.hourInput.value = alarm?.hour ?? 7;
  els.minuteInput.value = alarm?.minute ?? 0;
  els.labelInput.value = alarm?.label ?? "";
  els.vibrateInput.checked = alarm?.vibrate ?? true;
  els.snoozeEnabledInput.checked = alarm?.snoozeEnabled ?? true;
  els.snoozeMinutesInput.value = alarm?.snoozeMinutes ?? 10;
  els.enabledInput.checked = alarm?.enabled ?? true;
  els.routineGroupField.classList.toggle("hidden", alarmType !== "routine");
  renderRoutineOptions(alarm?.routineGroupId || routineGroupId || state.selectedRoutineId);
  renderWeekdays(alarm?.repeatDays || []);
  els.editorHint.textContent = alarmType === "routine" ? text.routineEditorHint : text.standardEditorHint;
}

function openRoutineEditor(groupId = null) {
  const routine = getGroup(groupId);
  editor.mode = "routine";
  editor.editingId = groupId;
  els.editorModal.classList.remove("hidden");
  els.editorTag.textContent = text.routineGroupEditor;
  els.editorTitle.textContent = routine ? text.editRoutineGroup : text.addRoutineGroup;
  els.labelFieldTitle.textContent = text.routineName;
  els.enabledFieldLabel.textContent = text.enableGroupAfterSave;
  els.timeFields.classList.add("hidden");
  els.weekdayBlock.classList.add("hidden");
  els.alarmToggleFields.classList.add("hidden");
  els.snoozeField.classList.add("hidden");
  els.routineGroupField.classList.add("hidden");
  els.editorDeleteButton.classList.toggle("hidden", !routine);
  els.labelInput.value = routine?.name || "";
  els.enabledInput.checked = routine?.enabled ?? true;
  els.editorHint.textContent = text.routineOverwriteHint;
}

function closeEditor() {
  els.editorModal.classList.add("hidden");
}

function getMoveTargets(alarm) {
  if (!alarm) return [];
  return state.routineGroups.filter((routine) => routine.id !== alarm.routineGroupId);
}

function openMoveDialog(alarmId) {
  const alarm = state.alarms.find((item) => item.id === alarmId);
  if (!alarm) return;

  moveDialog.alarmId = alarmId;
  els.moveModal.classList.remove("hidden");
  els.moveTitle.textContent = text.moveAlarmTitle;
  els.moveDescription.textContent = alarm.type === "normal" ? text.moveNormalDescription : text.moveRoutineDescription;
  els.moveToStandardButton.classList.toggle("hidden", alarm.type !== "routine");

  const moveTargets = getMoveTargets(alarm);
  els.moveOptions.innerHTML = moveTargets.length
    ? moveTargets
        .map(
          (target) => `
            <button type="button" class="move-option" data-action="move-to-group" data-group-id="${target.id}">
              <strong>${target.name}</strong>
              <span>将闹钟移动到这个作息组</span>
            </button>
          `,
        )
        .join("")
    : `<div class="move-empty">${alarm.type === "normal" ? text.moveEmptyCopy : text.noOtherRoutineGroups}</div>`;
}

function closeMoveDialog() {
  moveDialog.alarmId = null;
  els.moveModal.classList.add("hidden");
  els.moveOptions.innerHTML = "";
}

function moveAlarmToStandard(alarmId) {
  const alarm = state.alarms.find((item) => item.id === alarmId);
  if (!alarm) return;
  alarm.type = "normal";
  alarm.routineGroupId = null;
  ensureSelectedRoutine();
  closeMoveDialog();
  renderAll();
  toast(text.movedToStandard);
}

function moveAlarmToGroup(alarmId, groupId) {
  const alarm = state.alarms.find((item) => item.id === alarmId);
  const targetGroup = getGroup(groupId);
  if (!alarm || !targetGroup) return;
  alarm.type = "routine";
  alarm.routineGroupId = groupId;
  state.selectedRoutineId = groupId;
  closeMoveDialog();
  renderAll();
  toast(text.movedToGroup);
}

function saveEditor(event) {
  event.preventDefault();

  if (editor.mode === "routine") {
    const name = els.labelInput.value.trim();
    if (!name) {
      toast(text.routineNameRequired);
      return;
    }

    if (editor.editingId) {
      const target = getGroup(editor.editingId);
      if (!target) return;
      target.name = name;
      target.enabled = els.enabledInput.checked;
    } else {
      const newGroup = { id: uid(), name, enabled: els.enabledInput.checked };
      state.routineGroups.push(newGroup);
      state.selectedRoutineId = newGroup.id;
    }

    closeEditor();
    renderAll();
    toast(text.routineSaved);
    return;
  }

  const payload = {
    id: editor.editingId || uid(),
    type: editor.alarmType,
    label: els.labelInput.value.trim(),
    hour: Number(els.hourInput.value),
    minute: Number(els.minuteInput.value),
    repeatDays: [...els.weekdayGrid.querySelectorAll(".weekday-chip.active")].map((item) => Number(item.dataset.day)),
    vibrate: els.vibrateInput.checked,
    snoozeEnabled: els.snoozeEnabledInput.checked,
    snoozeMinutes: Number(els.snoozeMinutesInput.value || 10),
    enabled: els.enabledInput.checked,
    routineGroupId: editor.alarmType === "routine" ? els.routineGroupSelect.value : null,
  };

  if (payload.hour < 0 || payload.hour > 23 || payload.minute < 0 || payload.minute > 59) {
    toast(text.invalidTime);
    return;
  }

  if (payload.type === "routine" && !payload.routineGroupId) {
    toast(text.createRoutineFirst);
    return;
  }

  const index = state.alarms.findIndex((item) => item.id === payload.id);
  if (index >= 0) {
    state.alarms[index] = payload;
  } else {
    state.alarms.push(payload);
  }

  if (payload.routineGroupId) {
    state.selectedRoutineId = payload.routineGroupId;
  }

  closeEditor();
  renderAll();
  toast(text.alarmSaved);
}

function deleteCurrent() {
  if (editor.mode === "routine" && editor.editingId) {
    state.alarms = state.alarms.filter((alarm) => alarm.routineGroupId !== editor.editingId);
    state.routineGroups = state.routineGroups.filter((routine) => routine.id !== editor.editingId);
    ensureSelectedRoutine();
  }

  if (editor.mode === "alarm" && editor.editingId) {
    state.alarms = state.alarms.filter((alarm) => alarm.id !== editor.editingId);
  }

  closeEditor();
  renderAll();
  toast(text.deleted);
}

function exportJson() {
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = text.exportFileName;
  link.click();
  URL.revokeObjectURL(url);
}

function handleActionClick(target) {
  const actionTarget = target.closest("[data-action]");
  if (!actionTarget) return;

  const { action, id, groupId } = actionTarget.dataset;

  switch (action) {
    case "toggle-weekday":
      actionTarget.classList.toggle("active");
      break;
    case "open-group":
      state.selectedRoutineId = id;
      renderAll();
      break;
    case "edit-group":
      openRoutineEditor(id);
      break;
    case "edit-alarm": {
      const alarm = state.alarms.find((item) => item.id === id);
      if (!alarm) return;
      openAlarmEditor({
        alarmType: alarm.type,
        alarmId: id,
        routineGroupId: alarm.routineGroupId,
      });
      break;
    }
    case "delete-alarm":
      state.alarms = state.alarms.filter((alarm) => alarm.id !== id);
      renderAll();
      toast(text.deleted);
      break;
    case "move-alarm":
      openMoveDialog(id);
      break;
    case "move-to-group":
      if (moveDialog.alarmId) moveAlarmToGroup(moveDialog.alarmId, groupId);
      break;
    default:
      break;
  }
}

function handleToggleChange(target) {
  const { action, id } = target.dataset;
  if (!action) return;

  if (action === "toggle-group") {
    const routine = getGroup(id);
    if (!routine) return;
    routine.enabled = target.checked;
    renderAll();
    return;
  }

  if (action === "toggle-alarm") {
    const alarm = state.alarms.find((item) => item.id === id);
    if (!alarm) return;
    alarm.enabled = target.checked;
    renderAll();
  }
}

function renderAll() {
  ensureSelectedRoutine();
  saveState();
  renderHero();
  renderOverview();
  renderPermissions();
  renderNormalAlarms();
  renderRoutineGroups();
  renderTimeline();
  renderDetail();
  els.exactPermissionToggle.checked = state.permissions.exactAlarm;
  els.notificationPermissionToggle.checked = state.permissions.notifications;
}

hydrateStaticText();

document.addEventListener("click", (event) => handleActionClick(event.target));
document.addEventListener("change", (event) => handleToggleChange(event.target));

els.detailGroupToggle.addEventListener("change", (event) => {
  const routine = getGroup(state.selectedRoutineId);
  if (!routine) return;
  routine.enabled = event.target.checked;
  renderAll();
});

els.addNormalAlarmButton.addEventListener("click", () => openAlarmEditor({ alarmType: "normal" }));
els.addRoutineButton.addEventListener("click", () => openRoutineEditor());
els.addRoutineAlarmButton.addEventListener("click", () => {
  if (!state.selectedRoutineId) {
    toast(text.createRoutineFirst);
    return;
  }
  openAlarmEditor({ alarmType: "routine", routineGroupId: state.selectedRoutineId });
});
els.editRoutineButton.addEventListener("click", () => openRoutineEditor(state.selectedRoutineId));
els.editorCloseButton.addEventListener("click", closeEditor);
els.permissionButton.addEventListener("click", () => els.settingsModal.classList.remove("hidden"));
els.settingsCloseButton.addEventListener("click", () => els.settingsModal.classList.add("hidden"));
els.moveCloseButton.addEventListener("click", closeMoveDialog);
els.moveToStandardButton.addEventListener("click", () => {
  if (moveDialog.alarmId) moveAlarmToStandard(moveDialog.alarmId);
});
els.resetButton.addEventListener("click", () => {
  state = demoState();
  closeEditor();
  closeMoveDialog();
  renderAll();
  toast(text.demoReset);
});
els.exportStateButton.addEventListener("click", exportJson);
els.editorForm.addEventListener("submit", saveEditor);
els.editorDeleteButton.addEventListener("click", deleteCurrent);
els.exactPermissionToggle.addEventListener("change", (event) => {
  state.permissions.exactAlarm = event.target.checked;
  renderAll();
});
els.notificationPermissionToggle.addEventListener("change", (event) => {
  state.permissions.notifications = event.target.checked;
  renderAll();
});

window.addEventListener("click", (event) => {
  if (event.target === els.editorModal) closeEditor();
  if (event.target === els.settingsModal) els.settingsModal.classList.add("hidden");
  if (event.target === els.moveModal) closeMoveDialog();
});

renderAll();

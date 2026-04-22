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
  routineGroupHint: "每个作息组都有独立的总开关。",
  overviewTitle: "实时总览",
  noActiveScheduled: "当前没有生效中的闹钟",
  nextRingAt: "下一次响铃时间",
  overviewCopy: "作息闹钟是否生效 = 作息组总开关 + 子闹钟开关。",
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
  alarmSaved: "闹钟已保存。",
  deleted: "已删除。",
  exportFileName: "custom-alarm-preview-state.json",
  demoReset: "演示数据已重置。",
};

function uid() {
  return `${Date.now()}_${Math.random().toString(16).slice(2)}`;
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
      normalAlarm("早起学习", 6, 40, [1, 2, 3, 4, 5], true),
      normalAlarm("午休提醒", 13, 10, [], false),
      routineAlarm("起床", 7, 0, [1, 2, 3, 4, 5], workdayId, true),
      routineAlarm("出门准备", 8, 5, [1, 2, 3, 4, 5], workdayId, true),
      routineAlarm("周末起床", 9, 20, [6, 7], weekendId, true),
    ],
  };
}

function normalAlarm(label, hour, minute, repeatDays, enabled) {
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

function routineAlarm(label, hour, minute, repeatDays, routineGroupId, enabled) {
  return {
    ...normalAlarm(label, hour, minute, repeatDays, enabled),
    type: "routine",
    routineGroupId,
  };
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return demoState();
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed.alarms) || !Array.isArray(parsed.routineGroups)) return demoState();
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
  const alarms = rawState.alarms.map((alarm) => ({
    id: alarm.id || uid(),
    type: alarm.type === "routine" ? "routine" : "normal",
    label: alarm.label || "",
    hour: Number.isFinite(alarm.hour) ? alarm.hour : 7,
    minute: Number.isFinite(alarm.minute) ? alarm.minute : 0,
    repeatDays: Array.isArray(alarm.repeatDays) ? alarm.repeatDays : [],
    vibrate: alarm.vibrate ?? true,
    snoozeEnabled: alarm.snoozeEnabled ?? true,
    snoozeMinutes: Number.isFinite(alarm.snoozeMinutes) ? alarm.snoozeMinutes : 10,
    enabled: alarm.enabled ?? true,
    routineGroupId: alarm.type === "routine" && validRoutineIds.has(alarm.routineGroupId)
      ? alarm.routineGroupId
      : null,
  }));

  const selectedRoutineId = validRoutineIds.has(rawState.selectedRoutineId)
    ? rawState.selectedRoutineId
    : routineGroups[0]?.id || null;

  return {
    permissions,
    selectedRoutineId,
    routineGroups,
    alarms,
  };
}

let state = loadState();
const editor = { mode: "alarm", alarmType: "normal", editingId: null };

const $ = (selector) => document.querySelector(selector);
const els = {
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
  deleteButton: $("#deleteButton"),
  settingsModal: $("#settingsModal"),
  exactPermissionToggle: $("#exactPermissionToggle"),
  notificationPermissionToggle: $("#notificationPermissionToggle"),
  toast: $("#toast"),
};

const weekdays = text.weekdays;

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function toast(message) {
  els.toast.textContent = message;
  els.toast.classList.remove("hidden");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => els.toast.classList.add("hidden"), 1800);
}

function time(hour, minute) {
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function group(id) {
  return state.routineGroups.find((item) => item.id === id);
}

function repeatText(days) {
  const sorted = [...days].sort((a, b) => a - b).join(",");
  if (!sorted) return text.repeatOnce;
  if (sorted === "1,2,3,4,5") return text.repeatWorkdays;
  if (sorted === "6,7") return text.repeatWeekend;
  if (sorted === "1,2,3,4,5,6,7") return text.repeatEveryday;
  return sorted.split(",").map((day) => weekdays.find(([value]) => value === Number(day))?.[1]).join(" ");
}

function isEffective(alarm) {
  if (alarm.type === "normal") return alarm.enabled;
  return alarm.enabled && Boolean(group(alarm.routineGroupId)?.enabled);
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
      <strong>${next ? time(next.hour, next.minute) : text.none}</strong>
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
    <h3>${next ? `${text.nextRingAt} ${time(next.hour, next.minute)}` : text.noActiveScheduled}</h3>
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
  ].map(([title, enabled, desc]) => `
    <div class="permission-pill">
      <div><strong>${title}</strong><div class="alarm-meta">${desc}</div></div>
      <span class="permission-status ${enabled ? "" : "off"}">${enabled ? text.enabled : text.disabled}</span>
    </div>
  `).join("");
}

function alarmCard(alarm, compact = false) {
  return `
    <article class="${compact ? "detail-alarm-card" : "alarm-card"}">
      <div class="alarm-top">
        <div>
          <div class="alarm-time">${time(alarm.hour, alarm.minute)}</div>
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
        <button class="pill-button" data-action="delete-alarm" data-id="${alarm.id}">${text.delete}</button>
      </div>
    </article>
  `;
}

function renderNormalAlarms() {
  const alarms = state.alarms.filter((alarm) => alarm.type === "normal").sort(byTime);
  els.normalAlarmList.innerHTML = alarms.length
    ? alarms.map((alarm) => alarmCard(alarm)).join("")
    : `<article class="alarm-card"><strong>${text.noStandardAlarms}</strong><div class="alarm-meta">${text.createWithAdd}</div></article>`;
}

function renderRoutineGroups() {
  els.routineGroupList.innerHTML = state.routineGroups.length
    ? state.routineGroups.map((routine) => {
      const stats = routineStats(routine);
      return `
        <article class="routine-card">
          <div class="routine-top">
            <div>
              <strong>${routine.name}</strong>
              <div class="routine-meta">${stats.active.length} 个生效闹钟 | ${text.next} ${stats.next ? time(stats.next.hour, stats.next.minute) : text.none}</div>
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
    }).join("")
    : `<article class="routine-card"><strong>${text.noRoutineGroups}</strong></article>`;
}

function renderTimeline() {
  const alarms = upcoming();
  els.timelineList.innerHTML = alarms.length
    ? alarms.map((alarm) => `<article class="timeline-card"><strong>${time(alarm.hour, alarm.minute)} | ${alarm.label || text.unnamedAlarm}</strong><div class="timeline-meta">${alarm.type === "normal" ? text.standardAlarm : group(alarm.routineGroupId)?.name || text.routineAlarm}</div></article>`).join("")
    : `<article class="timeline-card"><strong>${text.noTimeline}</strong><div class="timeline-meta">${text.timelineHint}</div></article>`;
}

function renderDetail() {
  const routine = group(state.selectedRoutineId);
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
    ? stats.alarms.map((alarm) => alarmCard(alarm, true)).join("")
    : `<article class="detail-alarm-card"><strong>${text.noAlarmsYet}</strong><div class="detail-subtext">${text.addFirstAlarm}</div></article>`;
}

function renderWeekdays(selected = []) {
  els.weekdayGrid.innerHTML = weekdays.map(([value, label]) => `<button type="button" class="weekday-chip ${selected.includes(value) ? "active" : ""}" data-action="toggle-weekday" data-day="${value}">${label}</button>`).join("");
}

function renderRoutineOptions(selectedId) {
  els.routineGroupSelect.innerHTML = state.routineGroups.map((routine) => `<option value="${routine.id}" ${routine.id === selectedId ? "selected" : ""}>${routine.name}</option>`).join("");
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
  els.deleteButton.classList.toggle("hidden", !alarm);
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
  const routine = group(groupId);
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
  els.deleteButton.classList.toggle("hidden", !routine);
  els.labelInput.value = routine?.name || "";
  els.enabledInput.checked = routine?.enabled ?? true;
  els.editorHint.textContent = text.routineOverwriteHint;
}

function closeEditor() {
  els.editorModal.classList.add("hidden");
}

function saveEditor(event) {
  event.preventDefault();
  if (editor.mode === "routine") {
    const name = els.labelInput.value.trim();
    if (!name) return toast(text.routineNameRequired);
    if (editor.editingId) {
      const target = group(editor.editingId);
      target.name = name;
      target.enabled = els.enabledInput.checked;
    } else {
      const newGroup = { id: uid(), name, enabled: els.enabledInput.checked };
      state.routineGroups.push(newGroup);
      state.selectedRoutineId = newGroup.id;
    }
    closeEditor();
    renderAll();
    return toast(text.routineSaved);
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
  if (payload.hour < 0 || payload.hour > 23 || payload.minute < 0 || payload.minute > 59) return toast(text.invalidTime);
  const index = state.alarms.findIndex((item) => item.id === payload.id);
  if (index >= 0) state.alarms[index] = payload;
  else state.alarms.push(payload);
  if (payload.routineGroupId) state.selectedRoutineId = payload.routineGroupId;
  closeEditor();
  renderAll();
  toast(text.alarmSaved);
}

function deleteCurrent() {
  if (editor.mode === "routine" && editor.editingId) {
    state.alarms = state.alarms.filter((alarm) => alarm.routineGroupId !== editor.editingId);
    state.routineGroups = state.routineGroups.filter((routine) => routine.id !== editor.editingId);
    if (state.selectedRoutineId === editor.editingId) state.selectedRoutineId = null;
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

function bindDynamicInteractions() {
  document.querySelectorAll('[data-action="toggle-weekday"]').forEach((button) => {
    button.addEventListener("click", () => {
      button.classList.toggle("active");
    });
  });

  document.querySelectorAll('[data-action="open-group"]').forEach((button) => {
    button.addEventListener("click", () => {
      state.selectedRoutineId = button.dataset.id;
      renderAll();
    });
  });

  document.querySelectorAll('[data-action="edit-group"]').forEach((button) => {
    button.addEventListener("click", () => {
      openRoutineEditor(button.dataset.id);
    });
  });

  document.querySelectorAll('[data-action="edit-alarm"]').forEach((button) => {
    button.addEventListener("click", () => {
      const alarm = state.alarms.find((item) => item.id === button.dataset.id);
      if (!alarm) return;
      openAlarmEditor({
        alarmType: alarm.type,
        alarmId: button.dataset.id,
        routineGroupId: alarm.routineGroupId,
      });
    });
  });

  document.querySelectorAll('[data-action="delete-alarm"]').forEach((button) => {
    button.addEventListener("click", () => {
      state.alarms = state.alarms.filter((alarm) => alarm.id !== button.dataset.id);
      renderAll();
    });
  });

  document.querySelectorAll('[data-action="toggle-group"]').forEach((input) => {
    input.addEventListener("change", () => {
      const routine = group(input.dataset.id);
      if (!routine) return;
      routine.enabled = input.checked;
      renderAll();
    });
  });

  document.querySelectorAll('[data-action="toggle-alarm"]').forEach((input) => {
    input.addEventListener("change", () => {
      const alarm = state.alarms.find((item) => item.id === input.dataset.id);
      if (!alarm) return;
      alarm.enabled = input.checked;
      renderAll();
    });
  });
}

function renderAll() {
  saveState();
  renderHero();
  renderOverview();
  renderPermissions();
  renderNormalAlarms();
  renderRoutineGroups();
  renderTimeline();
  renderDetail();
  bindDynamicInteractions();
  els.exactPermissionToggle.checked = state.permissions.exactAlarm;
  els.notificationPermissionToggle.checked = state.permissions.notifications;
}

els.detailGroupToggle.addEventListener("change", (event) => {
  const routine = group(state.selectedRoutineId);
  if (routine) routine.enabled = event.target.checked;
  renderAll();
});
$("#addNormalAlarmButton").addEventListener("click", () => openAlarmEditor({ alarmType: "normal" }));
$("#addRoutineButton").addEventListener("click", () => openRoutineEditor());
$("#addRoutineAlarmButton").addEventListener("click", () => openAlarmEditor({ alarmType: "routine", routineGroupId: state.selectedRoutineId }));
$("#editRoutineButton").addEventListener("click", () => openRoutineEditor(state.selectedRoutineId));
$("#closeEditorButton").addEventListener("click", closeEditor);
$("#openSettingsButton").addEventListener("click", () => els.settingsModal.classList.remove("hidden"));
$("#closeSettingsButton").addEventListener("click", () => els.settingsModal.classList.add("hidden"));
$("#resetDemoButton").addEventListener("click", () => {
  state = demoState();
  renderAll();
  toast(text.demoReset);
});
$("#exportStateButton").addEventListener("click", exportJson);
els.editorForm.addEventListener("submit", saveEditor);
els.deleteButton.addEventListener("click", deleteCurrent);
els.exactPermissionToggle.addEventListener("change", (event) => {
  state.permissions.exactAlarm = event.target.checked;
  renderAll();
});
els.notificationPermissionToggle.addEventListener("change", (event) => {
  state.permissions.notifications = event.target.checked;
  renderAll();
});

renderAll();

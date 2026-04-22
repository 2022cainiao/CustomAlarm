const STORAGE_KEY = "custom_alarm_preview_state_v1";

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
      { id: workdayId, name: "Workday routine", enabled: true },
      { id: weekendId, name: "Weekend routine", enabled: false },
    ],
    alarms: [
      normalAlarm("Morning study", 6, 40, [1, 2, 3, 4, 5], true),
      normalAlarm("Lunch break", 13, 10, [], false),
      routineAlarm("Wake up", 7, 0, [1, 2, 3, 4, 5], workdayId, true),
      routineAlarm("Leave home prep", 8, 5, [1, 2, 3, 4, 5], workdayId, true),
      routineAlarm("Weekend wake-up", 9, 20, [6, 7], weekendId, true),
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
    name: routine.name || "Untitled routine",
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

const weekdays = [
  [1, "Mon"],
  [2, "Tue"],
  [3, "Wed"],
  [4, "Thu"],
  [5, "Fri"],
  [6, "Sat"],
  [7, "Sun"],
];

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
  if (!sorted) return "One time";
  if (sorted === "1,2,3,4,5") return "Workdays";
  if (sorted === "6,7") return "Weekend";
  if (sorted === "1,2,3,4,5,6,7") return "Every day";
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
    <span class="badge ${isEffective(alarm) ? "active" : "sleeping"}">${isEffective(alarm) ? "Effective" : "Blocked"}</span>
    <span class="badge">${repeatText(alarm.repeatDays)}</span>
    ${alarm.snoozeEnabled ? `<span class="badge">Snooze ${alarm.snoozeMinutes}m</span>` : ""}
    ${alarm.vibrate ? `<span class="badge">Vibrate</span>` : ""}
  `;
}

function renderHero() {
  const next = upcoming()[0];
  els.heroStats.innerHTML = `
    <div class="stat-card">
      <p class="panel-tag">Next alarm</p>
      <strong>${next ? time(next.hour, next.minute) : "None"}</strong>
      <div class="stat-meta">${next ? next.label || "Unnamed alarm" : "No active alarms"}</div>
    </div>
    <div class="stat-card">
      <p class="panel-tag">Routine groups</p>
      <strong>${state.routineGroups.length}</strong>
      <div class="stat-meta">Each group has its own master switch.</div>
    </div>
  `;
}

function renderOverview() {
  const standardActive = state.alarms.filter((alarm) => alarm.type === "normal" && alarm.enabled).length;
  const routineActive = state.alarms.filter((alarm) => alarm.type === "routine" && isEffective(alarm)).length;
  const groupsEnabled = state.routineGroups.filter((item) => item.enabled).length;
  const next = upcoming()[0];
  els.overviewCard.innerHTML = `
    <p class="screen-label">Live overview</p>
    <h3>${next ? `Next ring at ${time(next.hour, next.minute)}` : "No active alarm scheduled"}</h3>
    <p class="overview-copy">Routine alarm effective state = group switch + child alarm switch.</p>
    <div class="overview-grid">
      <div class="overview-chip"><span>Standard active</span><strong>${standardActive}</strong></div>
      <div class="overview-chip"><span>Routine active</span><strong>${routineActive}</strong></div>
      <div class="overview-chip"><span>Groups enabled</span><strong>${groupsEnabled}/${state.routineGroups.length}</strong></div>
    </div>
  `;
}

function renderPermissions() {
  els.permissionStrip.innerHTML = [
    ["Exact alarm", state.permissions.exactAlarm, "Precise system scheduling"],
    ["Notifications", state.permissions.notifications, "Ringing notification surface"],
  ].map(([title, enabled, desc]) => `
    <div class="permission-pill">
      <div><strong>${title}</strong><div class="alarm-meta">${desc}</div></div>
      <span class="permission-status ${enabled ? "" : "off"}">${enabled ? "Enabled" : "Disabled"}</span>
    </div>
  `).join("");
}

function alarmCard(alarm, compact = false) {
  return `
    <article class="${compact ? "detail-alarm-card" : "alarm-card"}">
      <div class="alarm-top">
        <div>
          <div class="alarm-time">${time(alarm.hour, alarm.minute)}</div>
          <strong>${alarm.label || "Unnamed alarm"}</strong>
          <div class="alarm-meta">${isEffective(alarm) ? repeatText(alarm.repeatDays) : "Currently inactive"}</div>
          <div class="badge-row">${alarmBadges(alarm)}</div>
        </div>
        <label class="switch">
          <input type="checkbox" data-action="toggle-alarm" data-id="${alarm.id}" ${alarm.enabled ? "checked" : ""} />
          <span class="switch-track"></span>
        </label>
      </div>
      <div class="alarm-actions">
        <button class="pill-button" data-action="edit-alarm" data-id="${alarm.id}">Edit</button>
        <button class="pill-button" data-action="delete-alarm" data-id="${alarm.id}">Delete</button>
      </div>
    </article>
  `;
}

function renderNormalAlarms() {
  const alarms = state.alarms.filter((alarm) => alarm.type === "normal").sort(byTime);
  els.normalAlarmList.innerHTML = alarms.length
    ? alarms.map((alarm) => alarmCard(alarm)).join("")
    : `<article class="alarm-card"><strong>No standard alarms yet</strong><div class="alarm-meta">Create one with the add button.</div></article>`;
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
              <div class="routine-meta">${stats.active.length} effective alarms | next ${stats.next ? time(stats.next.hour, stats.next.minute) : "None"}</div>
              <div class="badge-row">
                <span class="badge ${routine.enabled ? "active" : "sleeping"}">${routine.enabled ? "Group enabled" : "Group disabled"}</span>
                <span class="badge">${stats.alarms.length} alarms inside</span>
              </div>
            </div>
            <label class="switch">
              <input type="checkbox" data-action="toggle-group" data-id="${routine.id}" ${routine.enabled ? "checked" : ""} />
              <span class="switch-track"></span>
            </label>
          </div>
          <div class="routine-actions">
            <button class="pill-button" data-action="open-group" data-id="${routine.id}">Open detail</button>
            <button class="pill-button" data-action="edit-group" data-id="${routine.id}">Edit group</button>
          </div>
        </article>
      `;
    }).join("")
    : `<article class="routine-card"><strong>No routine groups yet</strong></article>`;
}

function renderTimeline() {
  const alarms = upcoming();
  els.timelineList.innerHTML = alarms.length
    ? alarms.map((alarm) => `<article class="timeline-card"><strong>${time(alarm.hour, alarm.minute)} | ${alarm.label || "Unnamed alarm"}</strong><div class="timeline-meta">${alarm.type === "normal" ? "Standard alarm" : group(alarm.routineGroupId)?.name || "Routine"}</div></article>`).join("")
    : `<article class="timeline-card"><strong>No active alarms</strong><div class="timeline-meta">Enable a standard alarm or routine group to populate this timeline.</div></article>`;
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
  els.detailSummary.textContent = `${stats.alarms.length} alarms inside | ${stats.active.length} effective`;
  els.detailGroupToggle.checked = routine.enabled;
  els.detailAlarmList.innerHTML = stats.alarms.length
    ? stats.alarms.map((alarm) => alarmCard(alarm, true)).join("")
    : `<article class="detail-alarm-card"><strong>No alarms yet</strong><div class="detail-subtext">Add the first alarm to this routine group.</div></article>`;
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
  els.editorTag.textContent = alarmType === "normal" ? "Standard Alarm" : "Routine Alarm";
  els.editorTitle.textContent = alarm ? "Edit alarm" : "Add alarm";
  els.labelFieldTitle.textContent = "Label";
  els.enabledFieldLabel.textContent = "Enable after save";
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
  els.editorHint.textContent = alarmType === "routine"
    ? "Effective state = routine group enabled AND child alarm enabled."
    : "Standard alarms are controlled by their own switch only.";
}

function openRoutineEditor(groupId = null) {
  const routine = group(groupId);
  editor.mode = "routine";
  editor.editingId = groupId;
  els.editorModal.classList.remove("hidden");
  els.editorTag.textContent = "Routine Group";
  els.editorTitle.textContent = routine ? "Edit routine group" : "New routine group";
  els.labelFieldTitle.textContent = "Routine name";
  els.enabledFieldLabel.textContent = "Enable group after save";
  els.timeFields.classList.add("hidden");
  els.weekdayBlock.classList.add("hidden");
  els.alarmToggleFields.classList.add("hidden");
  els.snoozeField.classList.add("hidden");
  els.routineGroupField.classList.add("hidden");
  els.deleteButton.classList.toggle("hidden", !routine);
  els.labelInput.value = routine?.name || "";
  els.enabledInput.checked = routine?.enabled ?? true;
  els.editorHint.textContent = "The group switch does not overwrite child alarm switches.";
}

function closeEditor() {
  els.editorModal.classList.add("hidden");
}

function saveEditor(event) {
  event.preventDefault();
  if (editor.mode === "routine") {
    const name = els.labelInput.value.trim();
    if (!name) return toast("Routine name is required.");
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
    return toast("Routine group saved.");
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
  if (payload.hour < 0 || payload.hour > 23 || payload.minute < 0 || payload.minute > 59) return toast("Invalid time.");
  const index = state.alarms.findIndex((item) => item.id === payload.id);
  if (index >= 0) state.alarms[index] = payload;
  else state.alarms.push(payload);
  if (payload.routineGroupId) state.selectedRoutineId = payload.routineGroupId;
  closeEditor();
  renderAll();
  toast("Alarm saved.");
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
  toast("Deleted.");
}

function exportJson() {
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "custom-alarm-preview-state.json";
  link.click();
  URL.revokeObjectURL(url);
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
  els.exactPermissionToggle.checked = state.permissions.exactAlarm;
  els.notificationPermissionToggle.checked = state.permissions.notifications;
}

document.addEventListener("click", (event) => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const { action, id, day } = target.dataset;
  if (action === "toggle-weekday") return target.classList.toggle("active");
  if (action === "open-group") state.selectedRoutineId = id;
  if (action === "edit-group") return openRoutineEditor(id);
  if (action === "edit-alarm") {
    const alarm = state.alarms.find((item) => item.id === id);
    return openAlarmEditor({ alarmType: alarm.type, alarmId: id, routineGroupId: alarm.routineGroupId });
  }
  if (action === "delete-alarm") state.alarms = state.alarms.filter((alarm) => alarm.id !== id);
  renderAll();
});

document.addEventListener("change", (event) => {
  const action = event.target.dataset.action;
  if (action === "toggle-group") group(event.target.dataset.id).enabled = event.target.checked;
  if (action === "toggle-alarm") state.alarms.find((alarm) => alarm.id === event.target.dataset.id).enabled = event.target.checked;
  if (action) renderAll();
});

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
  toast("Demo reset.");
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

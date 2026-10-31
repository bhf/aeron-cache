{{/*
Expand the name of the chart.
*/}}
{{- define "aeroncache-ws.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "aeroncache-ws.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Workload (StatefulSet) name. When near caching is enabled we suffix it with "-near" so the near
caching variant is visible in the pod name (StatefulSet pods are "<workload>-<ordinal>"). Only the
workload's own name changes; the Service, its selector labels and the governing serviceName keep using
the plain fullname, so service discovery and cross-component wiring are unaffected.
*/}}
{{- define "aeroncache-ws.workloadName" -}}
{{- if .Values.nearCacheEnabled -}}
{{- printf "%s-near" (include "aeroncache-ws.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- include "aeroncache-ws.fullname" . -}}
{{- end -}}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "aeroncache-ws.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "aeroncache-ws.labels" -}}
helm.sh/chart: {{ include "aeroncache-ws.chart" . }}
{{ include "aeroncache-ws.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "aeroncache-ws.selectorLabels" -}}
app.kubernetes.io/name: {{ include "aeroncache-ws.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "aeroncache-ws.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "aeroncache-ws.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
OpenTelemetry Java agent environment. Activates the agent jar baked into the image (see the app's
build.gradle.kts extraDirectories) via JAVA_TOOL_OPTIONS and points its metrics pipeline at a
Prometheus scrape endpoint. Traces/logs exporters are left configurable (default: none) so turning
on metrics does not also start shipping traces to an endpoint that may not exist. Rendered only when
javaagent.enabled is true, so a disabled app gets no -javaagent flag and is never instrumented.
*/}}
{{- define "aeroncache-ws.javaagentEnv" -}}
- name: JAVA_TOOL_OPTIONS
  value: "-javaagent:{{ .Values.javaagent.agentPath }}"
- name: OTEL_JAVAAGENT_ENABLED
  value: "true"
- name: OTEL_SERVICE_NAME
  value: {{ default (include "aeroncache-ws.fullname" .) .Values.javaagent.serviceName | quote }}
- name: OTEL_METRICS_EXPORTER
  value: "prometheus"
- name: OTEL_EXPORTER_PROMETHEUS_HOST
  value: "0.0.0.0"
- name: OTEL_EXPORTER_PROMETHEUS_PORT
  value: {{ .Values.javaagent.metricsPort | quote }}
- name: OTEL_TRACES_EXPORTER
  value: {{ .Values.javaagent.tracesExporter | quote }}
- name: OTEL_LOGS_EXPORTER
  value: {{ .Values.javaagent.logsExporter | quote }}
{{- with .Values.javaagent.extraEnv }}
{{- toYaml . }}
{{- end }}
{{- end }}

{{/*
Aeron C media driver native sidecar. Rendered as an initContainer entry with
restartPolicy: Always so the driver starts before the app and terminates after it.
The startupProbe gates the app container on the CnC file existing, so the Aeron
client never races ahead of the driver.
*/}}
{{- define "aeroncache-ws.mediadriver" -}}
{{- $md := .Values.externalMediaDriver -}}
- name: aeron-media-driver
  image: "{{ $md.image.repository }}:{{ $md.image.tag }}"
  imagePullPolicy: {{ $md.image.pullPolicy }}
  restartPolicy: Always
  env:
    - name: AERON_DIR
      value: {{ $md.aeronDir | quote }}
    - name: AERON_THREADING_MODE
      value: {{ $md.threadingMode | quote }}
    {{- if .Values.aeronCacheTermLength }}
    - name: AERON_CACHE_TERM_LENGTH
      value: {{ .Values.aeronCacheTermLength | quote }}
    {{- end }}
    {{- with $md.extraEnv }}
    {{- toYaml . | nindent 4 }}
    {{- end }}
  startupProbe:
    exec:
      command: ["sh", "-c", "test -e {{ $md.aeronDir }}/cnc.dat"]
    periodSeconds: 1
    failureThreshold: 30
  readinessProbe:
    exec:
      command: ["sh", "-c", "test -e {{ $md.aeronDir }}/cnc.dat"]
    periodSeconds: 5
  {{- with $md.resources }}
  resources:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.volumeMounts }}
  volumeMounts:
    {{- toYaml . | nindent 4 }}
  {{- end }}
{{- end }}

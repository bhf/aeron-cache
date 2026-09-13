{{/*
Expand the name of the chart.
*/}}
{{- define "aeroncache-sse.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "aeroncache-sse.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "aeroncache-sse.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "aeroncache-sse.labels" -}}
helm.sh/chart: {{ include "aeroncache-sse.chart" . }}
{{ include "aeroncache-sse.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "aeroncache-sse.selectorLabels" -}}
app.kubernetes.io/name: {{ include "aeroncache-sse.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "aeroncache-sse.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "aeroncache-sse.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Aeron C media driver native sidecar. Rendered as an initContainer entry with
restartPolicy: Always so the driver starts before the app and terminates after it.
The startupProbe gates the app container on the CnC file existing, so the Aeron
client never races ahead of the driver.
*/}}
{{- define "aeroncache-sse.mediadriver" -}}
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

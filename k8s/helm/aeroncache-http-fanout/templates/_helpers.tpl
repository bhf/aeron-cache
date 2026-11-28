{{/*
Expand the name of the chart.
*/}}
{{- define "aeroncache-http-fanout.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "aeroncache-http-fanout.fullname" -}}
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
ConfigMap name. Defaults to "<fullname>-config" so it tracks the release like the other resources;
override aeroncacheFanoutConfigmap to pin a name. Read by the ConfigMap and the Deployment.
*/}}
{{- define "aeroncache-http-fanout.configmapName" -}}
{{- .Values.aeroncacheFanoutConfigmap | default (printf "%s-config" (include "aeroncache-http-fanout.fullname" .)) -}}
{{- end }}

{{/*
Workload (StatefulSet) name. The aggregator is stateless so this is simply the fullname.
*/}}
{{- define "aeroncache-http-fanout.workloadName" -}}
{{- include "aeroncache-http-fanout.fullname" . -}}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "aeroncache-http-fanout.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "aeroncache-http-fanout.labels" -}}
helm.sh/chart: {{ include "aeroncache-http-fanout.chart" . }}
{{ include "aeroncache-http-fanout.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "aeroncache-http-fanout.selectorLabels" -}}
app.kubernetes.io/name: {{ include "aeroncache-http-fanout.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "aeroncache-http-fanout.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "aeroncache-http-fanout.fullname" .) .Values.serviceAccount.name }}
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
{{- define "aeroncache-http-fanout.javaagentEnv" -}}
- name: JAVA_TOOL_OPTIONS
  value: "-javaagent:{{ .Values.javaagent.agentPath }}"
- name: OTEL_JAVAAGENT_ENABLED
  value: "true"
- name: OTEL_SERVICE_NAME
  value: {{ default (include "aeroncache-http-fanout.fullname" .) .Values.javaagent.serviceName | quote }}
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

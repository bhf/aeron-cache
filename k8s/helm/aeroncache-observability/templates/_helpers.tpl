{{/*
Chart name.
*/}}
{{- define "aeroncache-observability.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Fully qualified app name.
*/}}
{{- define "aeroncache-observability.fullname" -}}
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
Per-component names. With fullnameOverride "aeroncache" these are aeroncache-prometheus /
aeroncache-grafana, which the Grafana datasource and the NOTES rely on.
*/}}
{{- define "aeroncache-observability.prometheus.fullname" -}}
{{- printf "%s-prometheus" (include "aeroncache-observability.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "aeroncache-observability.grafana.fullname" -}}
{{- printf "%s-grafana" (include "aeroncache-observability.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Chart label value.
*/}}
{{- define "aeroncache-observability.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "aeroncache-observability.labels" -}}
helm.sh/chart: {{ include "aeroncache-observability.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: aeroncache-observability
{{- end }}

{{/*
Service account name used by Prometheus.
*/}}
{{- define "aeroncache-observability.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (printf "%s-prometheus" (include "aeroncache-observability.fullname" .)) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

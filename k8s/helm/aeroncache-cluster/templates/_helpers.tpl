{{/*
Expand the name of the chart.
*/}}
{{- define "aeroncache-cluster.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "aeroncache-cluster.fullname" -}}
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
{{- define "aeroncache-cluster.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "aeroncache-cluster.labels" -}}
helm.sh/chart: {{ include "aeroncache-cluster.chart" . }}
{{ include "aeroncache-cluster.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "aeroncache-cluster.selectorLabels" -}}
app.kubernetes.io/name: {{ include "aeroncache-cluster.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "aeroncache-cluster.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "aeroncache-cluster.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Clustertools container
*/}}
{{- define "aeroncache-cluster.clustertools" -}}
- name: aeroncache-http-clustertools
  image: "{{ .Values.clustertools.image.repository }}:{{ .Values.clustertools.image.tag }}"
  imagePullPolicy: {{ .Values.clustertools.image.pullPolicy }}
  ports:
    - name: http
      containerPort: {{ .Values.clustertools.port }}
      protocol: TCP
  command:
    - "/bin/bash"
    - "-c"
    - |
      CLUSTER_NODE=${POD_NAME##*-}
      if [ -n "$CACHE_DATA_DIR_BASE" ]; then
        export CACHE_DATA_DIR="${CACHE_DATA_DIR_BASE}/node${CLUSTER_NODE}/cluster"
        # ClusterToolsConfig reads CLUSTER_FOLDER (not CACHE_DATA_DIR) for the cluster dir the
        # snapshot/purge tools operate on; point it at this node's cluster dir on the shared volume.
        # ARCHIVE_DIR then resolves to the sibling node${CLUSTER_NODE}/archive automatically.
        export CLUSTER_FOLDER="${CACHE_DATA_DIR_BASE}/node${CLUSTER_NODE}/cluster"
      fi
      {{- if .Values.externalMediaDriver.enabled }}
      # External driver: the node and driver share this fixed Aeron directory.
      export AERON_DIR="{{ .Values.externalMediaDriver.aeronDir }}"
      {{- else }}
      # Embedded driver: CacheNodeApplication derives its dir as
      # <Aeron default>-<nodeId>-driver, i.e. /dev/shm/aeron-<user>-<nodeId>-driver.
      export AERON_DIR="/dev/shm/aeron-$(id -un)-${CLUSTER_NODE}-driver"
      {{- end }}
      exec java --enable-preview --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED {{ if .Values.externalMediaDriver.enabled }}-Daeron.dir={{ .Values.externalMediaDriver.aeronDir }} {{ end }}-cp @/app/jib-classpath-file com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication
  env:
    - name: POD_NAME
      valueFrom:
        fieldRef:
          fieldPath: metadata.name
    {{- if .Values.persistence.enabled }}
    - name: CACHE_DATA_DIR_BASE
      value: {{ .Values.persistence.mountPath }}
    {{- end }}
    {{- with .Values.clustertools.snapshot }}
    - name: SNAPSHOT_SCHEDULE_ENABLED
      value: {{ .scheduleEnabled | quote }}
    - name: SNAPSHOT_INTERVAL
      value: {{ .interval | quote }}
    - name: SNAPSHOT_RETENTION_COUNT
      value: {{ .retentionCount | int64 | quote }}
    - name: SNAPSHOT_CHECK_STRATEGY
      value: {{ .check.strategy | quote }}
    - name: SNAPSHOT_CHECK_POLL_INTERVAL
      value: {{ .check.pollInterval | quote }}
    - name: SNAPSHOT_CHECK_STATE_CHANGE_THRESHOLD
      value: {{ .check.stateChangeThreshold | int64 | quote }}
    - name: SNAPSHOT_CHECK_ARCHIVE_GROWTH_BYTES
      value: {{ .check.archiveGrowthBytes | int64 | quote }}
    {{- end }}
  envFrom:
    - configMapRef:
        name: {{ .Values.aeroncacheClusterConfigmap }}
  {{- with .Values.clustertools.livenessProbe }}
  livenessProbe:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.clustertools.readinessProbe }}
  readinessProbe:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.clustertools.resources }}
  resources:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.volumeMounts }}
  volumeMounts:
    {{- toYaml . | nindent 4 }}
    {{- if $.Values.persistence.enabled }}
    - name: data
      mountPath: {{ $.Values.persistence.mountPath }}
    {{- end }}
  {{- end }}
{{- end }}

{{/*
Aeron C media driver native sidecar for the cluster pod. Rendered as an initContainer entry with
restartPolicy: Always so the driver starts before the node and terminates after it. The node,
clustertools and driver share the `shm` volume at aeronDir; the driver does NOT mount the data PVC.
The startupProbe gates the node container on the CnC file existing so the Aeron client never races
the driver.
*/}}
{{- define "aeroncache-cluster.mediadriver" -}}
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

# Shared GHCR image-source toggle for the aeroncache Helm charts.
#
# By default the charts run the images built into Minikube's Docker daemon (see the top-level
# `make build-backend` / `build-frontend`): image.repository is a bare name and image.pullPolicy is
# Never. Pass GHCR=true to any install target to instead pull the released images from the GitHub
# Container Registry (ghcr.io/<owner>/...), as published by .github/workflows/publish-ghcr.yml.
#
#   make install-all GHCR=true
#   make install-all GHCR=true GHCR_TAG=1.2.3            # a specific release instead of latest
#   make install-all GHCR=true GHCR_OWNER=myfork         # a different ghcr.io owner
#   make install-all GHCR=true IMAGE_PULL_SECRET=ghcr    # if the packages are private
#
# Overridable settings (command-line values propagate to the per-chart sub-makes):
#   GHCR=true                use ghcr.io images instead of the local Minikube images
#   GHCR_OWNER=bhf           ghcr.io owner / repo path (GHCR paths are lower-case)
#   GHCR_TAG=latest          image tag to pull (a release is tagged e.g. 1.2.3, plus latest)
#   GHCR_REGISTRY=ghcr.io    registry host
#   GHCR_PULL_POLICY=IfNotPresent   use Always to always re-pull a moving tag such as latest
#   IMAGE_PULL_SECRET=<name> name of a pre-created imagePullSecret (needed only for private packages)
#
# The external Aeron C media driver (aeroncache-media-driver) is published to GHCR too, so charts
# that carry an externalMediaDriver block point it at GHCR under GHCR=true as well. The sidecar
# itself still defaults to off (enable it with EXTERNAL_MEDIA_DRIVER=true).
GHCR_REGISTRY ?= ghcr.io
GHCR_OWNER ?= bhf
GHCR_TAG ?= latest
GHCR_PULL_POLICY ?= IfNotPresent

# $(call ghcr_image,<ghcr-image-name>,<values-key>) -> the --set flags that point the Helm values
# key <values-key> (e.g. `image` or `clustertools.image`) at ghcr.io/<owner>/<name>:<tag>. Expands
# to nothing when GHCR is unset, so install targets fall back to their local values.yaml images.
ghcr_image = $(if $(GHCR),--set $(2).repository=$(GHCR_REGISTRY)/$(GHCR_OWNER)/$(1) --set $(2).pullPolicy=$(GHCR_PULL_POLICY) --set $(2).tag=$(GHCR_TAG))

# Optional imagePullSecret for private GHCR packages; empty unless both GHCR and IMAGE_PULL_SECRET set.
ghcr_secret = $(if $(GHCR),$(if $(IMAGE_PULL_SECRET),--set imagePullSecrets[0].name=$(IMAGE_PULL_SECRET)))

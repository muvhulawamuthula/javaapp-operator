# JavaApp Operator — common tasks
# Override on the CLI, e.g.: make image-push REGISTRY=quay.io GROUP=myorg VERSION=0.1.0

MVN      ?= ./mvnw
REGISTRY ?= quay.io
GROUP    ?= muvhulawamuthula
NAME     ?= javaapp-operator
VERSION  ?= 0.1.0
IMG       = $(REGISTRY)/$(GROUP)/$(NAME):$(VERSION)
BUNDLE_IMG= $(REGISTRY)/$(GROUP)/$(NAME)-bundle:$(VERSION)
BUNDLE_DIR= target/bundle/$(NAME)

.PHONY: help build test verify it crd bundle install-crd deploy undeploy \
        image-push bundle-build bundle-push bundle-validate run-bundle clean

help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

build: ## Compile and produce the jar + generated manifests/bundle
	$(MVN) -ntp package -DskipTests

test: ## Run unit tests
	$(MVN) -ntp test

verify: ## Compile, generate, and run unit tests
	$(MVN) -ntp verify

it: ## Run integration tests against the cluster in ~/.kube/config
	$(MVN) -ntp verify -DskipITs=false

crd: build ## Regenerate the checked-in CRD from the build output
	cp target/kubernetes/javaapps.example.io-v1.yml config/crd/javaapps.example.io-v1.yml

install-crd: ## Apply the CRD to the current cluster
	kubectl apply -f config/crd/javaapps.example.io-v1.yml

deploy: build install-crd ## Deploy the operator (RBAC + Deployment + ServiceMonitor)
	kubectl apply -f target/kubernetes/kubernetes.yml

undeploy: ## Remove the operator and CRD
	-kubectl delete -f target/kubernetes/kubernetes.yml
	-kubectl delete -f config/crd/javaapps.example.io-v1.yml

image-push: ## Build and push the operator image ($(IMG))
	$(MVN) -ntp package -Pci -Dquarkus.container-image.push=true \
		-Dquarkus.container-image.registry=$(REGISTRY) \
		-Dquarkus.container-image.group=$(GROUP) \
		-Dquarkus.container-image.tag=$(VERSION)

bundle-build: build ## Build the OLM bundle image ($(BUNDLE_IMG))
	docker build -f $(BUNDLE_DIR)/bundle.Dockerfile -t $(BUNDLE_IMG) $(BUNDLE_DIR)

bundle-push: ## Push the OLM bundle image
	docker push $(BUNDLE_IMG)

bundle-validate: ## Validate the OLM bundle image (requires operator-sdk)
	operator-sdk bundle validate $(BUNDLE_IMG)

run-bundle: ## Install the operator on a cluster via OLM (requires operator-sdk + OLM)
	operator-sdk run bundle $(BUNDLE_IMG)

clean: ## Remove build output
	$(MVN) -ntp clean

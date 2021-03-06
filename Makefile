# Make targets for the Exchange REST API server
# Before using this, you must set the following environment variable:
# DOCKER_REGISTRY - hostname of the docker registry to push newly built containers to

SHELL = /bin/bash -e
DOCKER_REGISTRY ?= openhorizon
ARCH ?= amd64
DOCKER_NAME ?= exchange-api
VERSION = $(shell cat src/main/resources/version.txt)
DOCKER_NETWORK=exchange-api-network
DOCKER_TAG ?= $(VERSION)
DOCKER_OPTS ?= --no-cache
COMPILE_CLEAN ?= clean
image-string = $(DOCKER_REGISTRY)/$(ARCH)_exchange-api

# Some of these vars are also used by the Dockerfiles
JETTY_VERSION ?= 9.4
# try to sync this version with the version of scala you have installed on your dev machine, and with what is specified in build.sbt
SCALA_VERSION ?= 2.12.4
SCALA_VERSION_SHORT ?= 2.12
# JETTY_VERSION ?= 9.4.1.v20170120 <- we are now using the jetty docker container, instead of installing it ourselves
# this version corresponds to the Version variable in project/build.scala
EXCHANGE_API_WAR_VERSION ?= 0.1.0
EXCHANGE_API_DIR ?= /src/github.com/open-horizon/exchange-api
EXCHANGE_API_PORT ?= 8080
EXCHANGE_CONFIG_DIR ?= /etc/horizon/exchange
OS := $(shell uname)
ifeq ($(OS),Darwin)
  # Mac OS X
  EXCHANGE_HOST_CONFIG_DIR ?= /private$(EXCHANGE_CONFIG_DIR)/docker
else
  # Assume Linux (could test by test if OS is Linux)
  EXCHANGE_HOST_CONFIG_DIR ?= $(EXCHANGE_CONFIG_DIR)
endif


default: .docker-exec-run

# Removes exec container, but not bld container
clean: clean-exec-image

clean-exec-image:
	- docker rm -f $(DOCKER_NAME) 2> /dev/null || :
	- docker rmi $(image-string):{$(DOCKER_TAG),latest} 2> /dev/null || :
	rm -f .docker-exec .docker-exec-run

# Also remove the bld image/container
clean-all: clean
	- docker rm -f $(DOCKER_NAME)_bld 2> /dev/null || :
	- docker rmi $(image-string):bld 2> /dev/null || :
	- docker network remove $(DOCKER_NETWORK) 2> /dev/null || :
	rm -f .docker-bld

# rem-docker-bld:
# 	- docker rm -f $(DOCKER_NAME)_bld 2> /dev/null || :

docker: .docker-exec

docker-network:
	docker network create $(DOCKER_NETWORK)

# Using dot files to hold the modification time the docker image and container were built
.docker-bld: docker-network
	docker build -t $(image-string):bld $(DOCKER_OPTS) -f Dockerfile-bld --build-arg SCALA_VERSION=$(SCALA_VERSION) .
	- docker rm -f $(DOCKER_NAME)_bld 2> /dev/null || :
	docker run --name $(DOCKER_NAME)_bld --network $(DOCKER_NETWORK) -d -t -v $(CURDIR):$(EXCHANGE_API_DIR) $(image-string):bld /bin/bash
	@touch $@

.docker-compile: $(wildcard src/main/scala/com/horizon/exchangeapi/*) $(wildcard src/main/resources/*) .docker-bld
	docker exec -t $(DOCKER_NAME)_bld /bin/bash -c "cd $(EXCHANGE_API_DIR) && ./sbt package"
	# war file ends up in: ./target/scala-$SCALA_VERSION_SHORT/exchange-api_$SCALA_VERSION_SHORT-$EXCHANGE_API_WAR_VERSION.war
	@touch $@

.docker-exec: .docker-compile
	docker pull jetty:$(JETTY_VERSION)
	docker build -t $(image-string):$(DOCKER_TAG) $(DOCKER_OPTS) -f Dockerfile-exec --build-arg JETTY_VERSION=$(JETTY_VERSION) --build-arg SCALA_VERSION=$(SCALA_VERSION) --build-arg SCALA_VERSION_SHORT=$(SCALA_VERSION_SHORT) --build-arg EXCHANGE_API_WAR_VERSION=$(EXCHANGE_API_WAR_VERSION) .
	@touch $@

# rem-docker-exec:
# 	- docker rm -f $(DOCKER_NAME) 2> /dev/null || :

.docker-exec-run: .docker-exec
	- docker rm -f $(DOCKER_NAME) 2> /dev/null || :
	docker run --name $(DOCKER_NAME) --network $(DOCKER_NETWORK) -d -t -p $(EXCHANGE_API_PORT):$(EXCHANGE_API_PORT) -v $(EXCHANGE_HOST_CONFIG_DIR):$(EXCHANGE_CONFIG_DIR) $(image-string):$(DOCKER_TAG)
	@touch $@

# Run the automated tests in the bld container against the exchange svr running in the exec container
test:
docker-test: .docker-bld
	: $${EXCHANGE_ROOTPW:?}   # this verifies these env vars are set
	docker exec -t -e EXCHANGE_URL_ROOT=http://$(DOCKER_NAME):8080 -e "EXCHANGE_ROOTPW=$$EXCHANGE_ROOTPW" $(DOCKER_NAME)_bld /bin/bash -c 'cd $(EXCHANGE_API_DIR) && ./sbt test'

# Push the docker images to the registry w/o rebuilding them
docker-push-only:
	docker push $(image-string):$(DOCKER_TAG)
	docker tag $(image-string):$(DOCKER_TAG) $(image-string):latest
	docker push $(image-string):latest

# Push the image with the explicit version tag (so someone else can test it), but do not push the 'latest' tag so it does not get deployed to stg yet
docker-push-version-only:
	docker push $(image-string):$(DOCKER_TAG)

docker-push: docker docker-push-only

# Promote to prod by retagging to stable and pushing to the docker registry
docker-push-to-prod:
	docker tag $(image-string):$(DOCKER_TAG) $(image-string):stable
	docker push $(image-string):stable

# Get the latest version of the swagger ui from github and copy the dist dir into our repo
sync-swagger-ui:
	rm -rf /tmp/swagger-ui.backup
	mkdir -p /tmp/swagger-ui.backup
	cp -a src/main/webapp/* /tmp/swagger-ui.backup    # backup the version of swagger-ui we are currently using, in case the newer verion does not work
	git -C ../../swagger-api/swagger-ui pull    # update the repo
	mv src/main/webapp/index.html src/main/webapp/our-index.html   # we have our own main index.html, so move it out of the way temporaily
	rsync -aiu ../../swagger-api/swagger-ui/dist/ src/main/webapp      # copy the latest dist dir from the repo into our repo
	mv src/main/webapp/index.html src/main/webapp/swagger-index.html
	mv src/main/webapp/our-index.html src/main/webapp/index.html
	#sed -i '' 's/\(new SwaggerUi({\) *$$/\1 validatorUrl: null,/' src/main/webapp/swagger-index.html   # this is the only way to set validatorUrl to null in swagger

testmake:
	echo $(DOCKER_REGISTRY)

version:
	@echo $(VERSION)

.SECONDARY:

.PHONY: default clean clean-exec-image clean-all docker docker-network docker-test docker-push-only docker-push sync-swagger-ui testmake

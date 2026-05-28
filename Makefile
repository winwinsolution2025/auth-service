export DevID =ocid1.compartment.oc1..aaaaaaaadh37bvvavyij7uwekvq32eu6bgb2awddnmucisib6bdbsm4pcieq
IMAGE := letanthang/auth-service
dep:
	./mvnw dependency:resolve
gen:
	mvn clean generate-sources
gen/jooq:
	mvn clean install -P jooq-codegen
migrate:
	mvn liquibase:update
in:
	mvn clean install -DskipTests
run:
	mvn clean install -DskipTests
	./target/app-java25-native
jvm:
	mvn compile exec:java -Dexec.mainClass="com.example.authservice.Main"
native:
	javac -d out src/main/java/com/example/authservice/Main.java && native-image -cp out com.example.authservice.Main out/main  && ./out/main

# enable builder
buildx:
	docker buildx create --use

build:
	docker buildx build \
	--platform linux/arm64 \
	-t $(IMAGE) \
	--load \
      .

build/migration:
	docker buildx build \
	--file ./Dockerfile-migration \
	--platform linux/arm64 \
	-t $(IMAGE)-migration \
	--load \
	.
build/multi:
	docker buildx build \
	--platform linux/amd64,linux/arm64 \
	--build-arg MAVEN_IMAGE=maven:3.9.6-eclipse-temurin-17 \
	--build-arg JRE_IMAGE=eclipse-temurin:17-jre \
	-t $(IMAGE) \
	--push \
      .
up:
	docker compose up
pull:
	docker pull ap-singapore-1.ocir.io/axfnrpyfvlpv/auth-service:latest
push:
	docker build -t ap-singapore-1.ocir.io/axfnrpyfvlpv/auth-service .
	docker push ap-singapore-1.ocir.io/axfnrpyfvlpv/auth-service
oci/docker/login:
	docker login ap-singapore-1.ocir.io -u axfnrpyfvlpv/hcmut2025sa/anhchanto2025 -p "3)Q3iC3}3vO:V4RMI}tJ"
oci/ns:
	oci os ns get
oci/iam:
	 oci iam user get --user-id $$(oci iam user list --query "data[0].id" --raw-output)
oci/users:
	oci iam user list --all --query "data[].name" --output table
oci/me:
	oci iam user get --user-id $$(oci iam user list --query "data[?name=='ltthang.sdh241'].id | [0]" --raw-output)
oci/ls-domain-in-compartment:
# 	tenancy is also a compartment
	oci iam domain list --compartment-id ocid1.tenancy.oc1..aaaaaaaaxtbkp4ctt6kplr5iuse7e7v2fr6b56wnuydyt3cqz6763imlje6q --all
oci/domain/users:
	oci identity-domains users list --all --query "data.resources[].emails[0].value" --output table  --endpoint https://idcs-c344c7c91bd6488481e5351f77c8b5a8.ap-singapore-idcs-1.identity.ap-singapore-1.oci.oraclecloud.com:443
oci/profile:
	@grep "^\[" ~/.oci/config | tr -d "[]"
oci/image:
	oci artifacts container repository list --compartment-id ocid1.compartment.oc1..aaaaaaaadh37bvvavyij7uwekvq32eu6bgb2awddnmucisib6bdbsm4pcieq
oci/subnet:
	oci network subnet list --compartment-id $(DevID) -vcn-id ocid1.vcn.oc1.ap-singapore-1.amaaaaaa657ylliaykxlcvvqw2mu2z7gigybascjs7yvgjlopdtkcyqp4uwa
oci/pool:
	oci ce node-pool get --node-pool-id ocid1.nodepool.oc1.ap-singapore-1.aaaaaaaadm5rvfwkzh5xozgpxbbte3yfqq4iccnmhreuz6tv2noc47d363fq
oci/instance:
	oci compute instance get --instance-id ocid1.instance.oc1.ap-singapore-1.anzwsljr657yllicgr6p53c3qtjtbwukciksii5pftm27gma57ujfdofczja

stream:
	nats stream ls
stream/add:
	nats stream add auth_stream \
	--subjects auth.* \
	--storage file \
	--replicas 1 \
	--retention limits \
	--discard old \
	--deny-delete \
	--no-deny-purge \
	--allow-rollup \
	--max-age 2h \
	--max-msg-size 200kb \
	--dupe-window 1h \
	--max-msgs 100000 \
	--max-bytes 1Gb \
	--max-msgs-per-subject 1000

stream/del:
	nats stream rm
pub:
	nats pub auth.user_login_subject '{"user_id":1,"email":"thangdeptrai@gmail.com"}'
sub:
	nats sub auth.user_login_subject
consumer:
	nats consumer ls
pub/register:
	nats pub auth.user_register_subject '{"user_id":1,"email":"thangdeptrai@gmail.com"}'
sub/register:
	nats sub auth.user_register_subject
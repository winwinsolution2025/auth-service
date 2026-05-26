# Auth Service

## Maven
All dependencies:
- pom.xml
We use maven wrapper
- mvn wrapper:wrapper

## Installation
- cp samele.env .env 
- You need to install docker engine first : recommending orbstack
- You need to start user-service first
- You need to know to use docker
- You need to have a mysql db (docker-compose is recommended)
- You will need make tool (please install it by brew)
- Please update docker-compose.yaml with your database info, url, name, port, ...
- Maven sync to fix ide : right click on project -> Maven -> Sync Project

1. Build docker image
   - make build
   - make build/multi
2. Run:
   - make run
   - make r
   - make native
3. Run by docker
   - make up
4. Generate secret
   - openssl rand -base64 32

## Test and Deploy


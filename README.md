# Jahia GlobalLink Translation Connector

## Prerequisites

Build locally the module **globallink-connect-api-java** of translation.com

```sh
$ git clone https://github.com/translations-com/globallink-connect-api-java
$ cd globallink-connect-api-java
$ mvn clean install
```

## How to deploy

In a terminal run the following command line
```sh
mvn clean install jahia:deploy -P
```

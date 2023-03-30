# On Demand Component

This module is part of the Semantic Interoperability Component and provides on-demand access to the data. This component exposes a REST API, which enables historical data requests and, in addition, it also allows access to the current values using the last values request.


## Getting started

**Last values request**

The general parameters of these requests are the following:
*	"id": unique identifier of the data source.
*	"type": requested data type.
*	"service": Fiware service value of the data. Corresponds with the "service" attribute of the data source (if it is different from the default value).
*	"servicePath": Fiware servicePath value of the data. Corresponds with the "servicePath" attribute of the data source (if it is different from the default value).
*	"attributes": any parameters for filtering the data. The allowed parameters for a data source are described in the "attributes" field of the DataSource entity and their correspondence with the parameters of the agent is described in the "mapping" field (if these fields are empty or missing in the data source it means that no filtering attributes are allowed for that data source).


Last values requests return the data in the HTTP response message. These requests rely on the information stored in the Metadata Registry. If the request parameters cannot be correctly interpreted according to the registered metadata, the module will return a 400 error code. In addition, this request requires by default the use of an authentication header containing a token issued by Keycloak.


Example of last values request:

```
POST /current
Authorization: Bearer <token>

{
    "id": "urn:ngsi-ld:VPF:PortCall"
}
```


**Historical data request**

The general parameters of these requests are the following:
*	"id": unique identifier of the data source.
*	"type": requested data type.
*	"service": Fiware service value of the data. Corresponds with the "service" attribute of the data source (if it is different from the default value).
*	"servicePath": Fiware servicePath value of the data. Corresponds with the "servicePath" attribute of the data source (if it is different from the default value).
*	"callback": callback URL to receive the historical data.
*	"attributes": any parameters for filtering the data. The allowed parameters for a data source are described in the "attributes" field of the DataSource entity and their correspondence with the parameters of the agent is described in the "mapping" field (if these fields are empty or missing in the data source it means that no filtering attributes are allowed for that data source). Only the parameters defined under both "attributes" and "mapping" fields will be allowed in the requests to a data source.


The historical data requests rely on the information stored in the Metadata Registry. If the request parameters cannot be correctly processed according to the registered metadata, the module will return a 400 error code.


Due to the expected data volumes (and delays), the data is sent to the provided callback URL. The response to this request will be 200 OK if the request was processed correctly, otherwise it will return an error code. In addition, this request requires by default the use of an authentication header containing a token issued by Keycloak.


Example of historical data request:

```
POST /historical
Authorization: Bearer <token>

{
    "id": "urn:ngsi-ld:VPF:PortCall",
    "callback": "<MY_CALLBACK_URL>"
}
```

Example of historical data request with filtering parameters:

```
POST /historical
Authorization: Bearer <token>

{
	"id": "urn:ngsi-ld:ITI:Customs",
	"callback": "<MY_CALLBACK_URL>",
    "attributes": {
        "year": 2022,
        "month": 11
	}
}
```

The data source metadata of the second example would be defined as:

```
{
    "id": "urn:ngsi-ld:ITI:Customs",
    "type": "DataSource",
    "attributes": {
		"type": "StructuredValue",
		"value": {
			"START_YEAR": "number",
			"MONTH": "number"
		},
		"metadata": {}
	},
	"mapping": {
		"type": "StructuredValue",
		"value": {
			"year": "START_YEAR",
			"month": "MONTH"
		},
		"metadata": {}
	},
    "dataProvided": {
        "type": "StructuredValue",
        "value": {
			"id": "string",
			"customsProcedureID": "string",
			"declaringParty": "object",
			"exportingParty": "object",
			"importingParty": "object",
			"depotParty": "object",
			"customsOffice": "object",
			"customsLocation": "object",
			"customsProvince": "object",
			"customs": "string",
			"customsRegime": "string",
			"customsStatus": "string",
			"customsRegimeRequested": "number",
			"customsDocumentGrossWeight": "number",
			"customsDocumentType": "number",
			"customsDocumentAdmissionDate": "string",
			"customDocumentOriginCountry": "string",
			"customDocumentOriginProvince": "number",
			"customDocumentDestinationCountry": "string",
			"customDocumentDestinationProvince": "number",
			"exportLocation": "object",
			"year": "number",
			"month": "number",
			"TARIC": "number",
			"TARIC_N4": "number",
			"packageType": "string",
			"packageQuantity": "number",
			"portIndex": "number",
			"origin": "object",
			"originExpeditionCountry": "string",
			"destination": "object",
			"conveyor": "object",
			"hasPredeclaration": "boolean",
			"exitType": "string",
			"portCallReference": "number",
			"automaticTranshipment": "boolean",
			"seal": "array",
			"precedingRegimeRequested": "number",
			"unitNumber": "number",
			"statisticalValue": "number",
			"invoiceValue": "number",
			"countryCurrency": "string",
			"container": "number",
			"containerNumber": "string",
			"containerType": "string",
			"containerOperationType": "string",
			"containerNextPrevLoadDischargePortCode": "string",
			"containerGrossWeight": "number",
			"containerDeliveryPlaceCode": "string",
			"containerDischargePortCode": "string",
			"isVGM": "boolean",
			"fullEmpty": "string",
			"transportRegime": "string",
			"transportModeOnBorder": "number",
			"countryTransportMode": "number",
			"transportNationality": "string",
			"exchangeZone": "string",
			"transactionNature": "number",
			"deliveryConditions": "string",
			"contingent": "number",
			"tariffPreference": "number",
			"vesselFreight": "number",
			"fiscalAddressProvince": "number",
			"additionalCodes": "string",
			"type": "Customs"
        },
        "metadata": {}
    },
    "description": {
        "type": "Text",
        "value": "AEAT Customs data source",
        "metadata": {}
    },
    "onChain": {
        "type": "Boolean",
        "value": false,
        "metadata": {}
    },
    "dataModels": {
        "type": "Text",
        "value": "Customs/Customs/schema.json",
        "metadata": {}
    }
}
```


## Build from sources

Build Docker image:

`docker build --no-cache -t dataports/on-demand:<version> .`


You can run locally the Docker image using:

`docker run -d -p {TCP port}:4568 --name on-demand dataports/on-demand:<version> 4568 {Orion base URL}`



You can deploy the On Demand Component together with Orion using docker-compose:

`docker-compose up --build -d on-demand`



## Testing

Build using Maven:

```
mvn clean compile assembly:single
```


Run On Demand Component:

`java -jar ./target/OnDemand-0.0.1-SNAPSHOT-jar-with-dependencies.jar {TCP port} {Orion base URL}`


By default, the component requires the use of an authentication header containing a token issued by Keycloak. To test the component locally without mandatory authentication, you can run the component in test mode using:

`java -jar ./target/OnDemand-0.0.1-SNAPSHOT-jar-with-dependencies.jar {TCP port} {Orion base URL} testmode`


Test mode disables the mandatory authentication header and its use is not recommended unless the component is being tested locally.



Default TCP port for the REST API: 4568


Default Orion base URL: http://localhost:1026/


Swagger description of the API: http://localhost:4568/swagger


### Dependencies

The On Demand component depends on the following components:


#### Fiware Orion Context Broker

Orion stores the last values of the data and also the metadata of the connected data sources and agents (Metadata Registry).


[Orion documentation](https://fiware-orion.readthedocs.io/en/master/)


[Orion deployment using Docker](https://hub.docker.com/r/fiware/orion/)


#### Data Access Component

The On Demand component makes use of the Data Access Manager to activate the on demand agents when it receives a historical data request. The URL of the Data Access Manager is stored with the information of the on demand agents in the Metadata Registry. 


## License

This software is licensed under the MIT license. Please refer to the "LICENSE" file for more information.

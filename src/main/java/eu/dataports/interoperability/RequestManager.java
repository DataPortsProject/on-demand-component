/*
 * Copyright 2022 Universitat Politècnica de València
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.dataports.interoperability;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class RequestManager {
	OrionClient orionClient;
	static final String CALLBACK_URL = "CALLBACK_URL";
	static final String DEFAULT_ORION_URL = "http://localhost:1026";
	static final String AGENTNAME = "name";
	static final String ACCESS_URL = "accessURL"; // URL for the historical data calls (AgentImage entities)
	static final String MAPPING = "mapping";
	static final String QUERY_ATTRIBUTES = "attributes";
	private final Logger logger = LoggerFactory.getLogger(RequestManager.class);
	
	RequestManager (String url) {
		if (url!=null) orionClient = new OrionClient(url);
		else orionClient = new OrionClient(DEFAULT_ORION_URL);
	}
	
	// Last values request
	String lastValuesRequest(Query query, String auth) throws Exception {
		String response=null;
		if(checkAttributes(query, auth)) {
			if(orionClient!= null) {
				// Get data from Orion
				response = orionClient.getDataFromOrion(query, auth).toString();
			}else{
				// Test
				throw new Exception("ERROR: cannot connect to the Context Broker");
			}
		} else throw new Exception("Bad request: incorrect attributes");
		return response;
	}
	
	
	// Historical data request
	String historicalDataRequest(Query query, String auth) throws Exception {
		String response=null;
		if(orionClient!= null) {
			// Get data source(s) metadata
			JsonArray dataSources = orionClient.getDataSourcesFromOrion(query.getId(), query.getType(), auth);
			// Get agent image(s) metadata
			if (dataSources.size()==0) throw new Exception("No data source registered for this request");
			int numRequests = 0;
			for (int i=0; i<dataSources.size(); i++) {
				// Get the agent image metadata for this data source
				JsonObject source = dataSources.get(i).getAsJsonObject();
				if(checkAttributes(query, source)) {
					JsonObject agentImage = orionClient.getAgentImageMetadata(source.get("id").getAsString(), auth);
					if(agentImage!=null) {
						String imageName = agentImage.get(AGENTNAME).getAsString();
						// Create new Attributes object using the provided mappings
						JsonObject attributes;
						JsonObject queryAttributes = query.getAttributes();
						if(queryAttributes!=null && !queryAttributes.entrySet().isEmpty()) attributes= convertAttributes(queryAttributes, source.get(MAPPING).getAsJsonObject());
						else attributes = null;
						// Create request body
						JsonObject requestBody=createHistoricalDataRequestBody(imageName, query.getCallback(), attributes);
						// Send request to data access manager
						String url = agentImage.get(ACCESS_URL).getAsString();
						response = sendToDataAccessComponent(url,requestBody, auth);
						numRequests++;
					}
				}
			}
			if (numRequests == 0) throw new Exception("Bad request: incorrect attributes");
			// Create and return response (OK or error)
		}else{
			throw new Exception("ERROR: cannot connect to the Context Broker");
		}
		return response;
	}
	
	// Define attributes for the on-demand agent
	JsonObject convertAttributes(JsonObject queryAttributes, JsonObject mapping) throws Exception {
		if (queryAttributes!=null && queryAttributes.size()>0) {
			Set<Map.Entry<String, JsonElement>> entrySet = queryAttributes.entrySet();
			JsonObject newAttributes = new JsonObject();
			for(Map.Entry<String, JsonElement> entry : entrySet) {
				String key = entry.getKey();
				if(mapping.has(key)) {
					JsonElement element = mapping.get(key);
					if(element.isJsonPrimitive()) {
						String newKey = element.getAsString();
						String newValue = entry.getValue().getAsString();
						newAttributes.addProperty(newKey, newValue);
					}else if(element.isJsonArray()) {
						// Map to n attributes
						JsonArray entryObject = entry.getValue().getAsJsonArray();
						JsonArray elementArray = element.getAsJsonArray();
						for(int i=0; i<entryObject.size(); i++) {
							String newKey = elementArray.get(i).getAsString();
							String newValue = entryObject.get(i).getAsString();
							newAttributes.addProperty(newKey, newValue);
						}
					}else if(element.isJsonObject()) {
						JsonObject entryObject = entry.getValue().getAsJsonObject();
						JsonObject elementObject = element.getAsJsonObject();
						Set<Map.Entry<String, JsonElement>> elementEntrySet = entryObject.entrySet();
						for(Map.Entry<String, JsonElement> elementEntry : elementEntrySet) {
							String entryKey = elementEntry.getKey();
							if(elementObject.has(entryKey)) {
								String newKey = elementObject.get(entryKey).getAsString(); 
								String newValue = elementEntry.getValue().getAsString();
								newAttributes.addProperty(newKey, newValue);
							}else throw new Exception("No mapping defined for attribute " + key + "." + entryKey);
						}
					}
					
				}else throw new Exception("No mapping defined for attribute " + key);
			}
			return newAttributes;
		}else return queryAttributes;
	}
	
	
	boolean checkAttributes(Query q, JsonObject dataSource) throws Exception {
		boolean ok = true;
		JsonObject queryAttributes = q.getAttributes();
		if (queryAttributes!=null && queryAttributes.size()>0) {
			if(dataSource.has(QUERY_ATTRIBUTES) && !dataSource.get(QUERY_ATTRIBUTES).getAsJsonObject().entrySet().isEmpty()){
				JsonObject dataAttributes;
				if(!dataSource.has(MAPPING)) throw new Exception("No mapping defined");
				dataAttributes = dataSource.get(MAPPING).getAsJsonObject();
				if(dataAttributes.entrySet().isEmpty()) throw new Exception("No mapping defined");
				logger.debug("Data source attributes: " + dataAttributes.toString());
				Set<Map.Entry<String, JsonElement>> entrySet = queryAttributes.entrySet();
				for(Map.Entry<String, JsonElement> entry : entrySet) {
					// Check if all the attributes in the request are defined in the data source
					if (ok) {
						String key = entry.getKey();
						ok = dataAttributes.has(key);
					} else break;
				}
			}else ok = false;
			return ok;
		} else return ok;
	}
	
	
	boolean checkAttributes(Query q, String auth) throws Exception {
		boolean ok = true;
		JsonArray dataSources = orionClient.getDataSourcesFromOrion(q.getId(), q.getType(), auth);
		JsonObject queryAttributes = q.getAttributes();
		if(dataSources.size()>0) {
			if (queryAttributes!=null && queryAttributes.size()>0) {
				JsonObject source = dataSources.get(0).getAsJsonObject();
				if(source.has(QUERY_ATTRIBUTES) && !source.get(QUERY_ATTRIBUTES).isJsonNull()){
					JsonObject dataAttributes = source.get(QUERY_ATTRIBUTES).getAsJsonObject();
					Set<Map.Entry<String, JsonElement>> entrySet = queryAttributes.entrySet();
					for(Map.Entry<String, JsonElement> entry : entrySet) {
						// Check if all the attributes in the request are defined in the data source
						if (ok) {
							String key = entry.getKey();
							ok = dataAttributes.has(key);
						} else break;
					}
				}else ok = false;
				return ok;
			} else return ok;
		} else throw new Exception("No data source registered for this request");
	}
	
	
	// Create body of the request to the Data Access Manager
	JsonObject createHistoricalDataRequestBody(String agentImage, String callbackUrl, JsonObject attrs){
		// Create request body
		JsonObject requestBody = new JsonObject();
		java.util.Date date = new java.util.Date();
		requestBody.addProperty("name", "agent" + date.getTime());
		requestBody.addProperty("image", agentImage);
		JsonArray environment = new JsonArray();
		JsonObject callback = new JsonObject();
		callback.addProperty("key", CALLBACK_URL);
		callback.addProperty("value", callbackUrl);
		environment.add(callback);
		// Add attributes
		if(attrs!=null) {
			Set<Map.Entry<String, JsonElement>> entrySet = attrs.entrySet();
			for(Map.Entry<String, JsonElement> entry : entrySet) {
				JsonObject attribute = new JsonObject();
				attribute.addProperty("key", entry.getKey());
				attribute.addProperty("value", entry.getValue().getAsString());
				environment.add(attribute);
			}
		}
		requestBody.add("environment", environment);
		logger.debug(requestBody.toString());
		return requestBody;
	}
	
	// Send request to the Data Access Manager
	String sendToDataAccessComponent(String addr, JsonObject body, String auth) throws Exception {
		logger.debug(addr);
		URL url = new URL(addr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "*/*");
		if(auth != null) con.setRequestProperty("Authorization", auth);
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
		os.write(input, 0, input.length);			
		int responseCode = con.getResponseCode();
		if(responseCode>=200 && responseCode<=299){
			BufferedReader in = new BufferedReader(
 		    new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString();
		}else{
 			throw new Exception("Unsuccessful response code from Data Access Component: " + responseCode);
 		}
	}
	
}

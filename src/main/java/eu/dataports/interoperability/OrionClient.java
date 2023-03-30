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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OrionClient {
	private String orionUrl;
	static final String SERVICE_METADATA = "metadata";
	static final String DATASOURCE = "DataSource";
	static final String AGENT = "AgentImage";
	static final String ACCESS_URL = "accessURL"; // URL for the historical data calls (AgentImage entities)
	static final String REF_DATA_SOURCE = "refDataSource";
	static final String V2_ENTITIES = "v2/entities"; // "/v2/entities"
	static final String DATATYPE= "dataProvided.type";
	
	OrionClient(String url) {
		if(!url.endsWith("/")) url = url + "/";
		orionUrl = url;
	}
	
	JsonArray getDataFromOrion(Query query, String auth) throws Exception {
		// The query parameter id refers to the data source id. Include the entity id as a parameter in the queries?
		URL requestUrl = getOrionRequestUrl(null, query.getType(), query.getLimit(), query.getOffset(), query.getAttributes(), false);
		return getFromOrion(requestUrl, query.getService(), query.getServicePath(), auth);
	}
	
	JsonArray getFromOrion(URL url, String service, String servicePath, String auth) throws Exception {
		// Performs a GET request to Orion
		JsonArray entities = null;
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		//add request headers
		if(service != null) con.setRequestProperty("Fiware-Service", service);
		if(servicePath != null) con.setRequestProperty("Fiware-ServicePath", servicePath);
		if(auth != null) con.setRequestProperty("Authorization", auth);
		
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
			JsonParser parser = new JsonParser();
			entities = parser.parse(response.toString()).getAsJsonArray();
		}else{
 			throw new Exception("Unsuccessful response code from Context Broker: " + responseCode);
 		}
		return entities;
	}
	
	JsonArray getDataSourcesFromOrion (String id, String type, String auth) throws Exception {
		JsonArray dataSources = null;
		URL requestUrl;
		if(type==null || type.isEmpty()) requestUrl = getOrionRequestUrl(id, DATASOURCE, 0, 0, null, true); // keyValues
		else {
			// Select only the requested types
			JsonObject requestType = new JsonObject();
			requestType.addProperty(DATATYPE, type);
			requestUrl = getOrionRequestUrl(id, DATASOURCE, 0, 0, requestType, true);
		}
		dataSources = getFromOrion(requestUrl, SERVICE_METADATA, null, auth);
		return dataSources;
	}
		
	URL getOrionRequestUrl(String dataSourceId, String type, int limit, int offset, JsonObject attrs, boolean keyValues) throws URISyntaxException, MalformedURLException {
		// Generates URL for GET request to Orion
		URIBuilder builder = new URIBuilder(orionUrl + V2_ENTITIES);
		if(dataSourceId!=null && !dataSourceId.isEmpty()) builder.addParameter("id", dataSourceId);
		if(type!=null && !type.isEmpty()) builder.addParameter("type", type);
		// Add more parameters? -> options=count to get the total number of entities
		if(limit>0) {
			builder.addParameter("limit", String.valueOf(limit));
			builder.addParameter("offset", String.valueOf(offset));
		}
		if(attrs!=null) {
			// Add any extra attributes to query (as q parameter)
			String q = "";
			Set<Map.Entry<String, JsonElement>> entrySet = attrs.entrySet();
			for(Map.Entry<String, JsonElement> entry : entrySet) {
				JsonElement valueElement = entry.getValue();
				String newValue;
				if(valueElement.isJsonPrimitive()) newValue = valueElement.getAsString();
				else newValue= valueElement.toString();
				String newParam = entry.getKey() + "==" + newValue;
				if (!q.isEmpty()) q = q + ";"; // AND operator
				q = q + newParam;
			}
			if (!q.isEmpty()) builder.addParameter("q", q);
		}
		if(keyValues) builder.addParameter("options", "keyValues");
		URI uri = builder.build();
		return uri.toURL();
	}
	
	JsonObject getAgentImageMetadata (String dataSourceId, String auth) throws Exception {
		// Gets metadata of the on-demand agent image
		String queryString = REF_DATA_SOURCE + "==" + dataSourceId;
		URIBuilder builder = new URIBuilder(orionUrl + V2_ENTITIES);
		builder.addParameter("type", AGENT);
		builder.addParameter("q", queryString);
		builder.addParameter("options", "keyValues");
		URL requestUrl = builder.build().toURL();
		JsonArray agentData = getFromOrion(requestUrl, SERVICE_METADATA, null, auth);
		// The array should have only one element
		if(agentData.size()>0) {
			return agentData.get(0).getAsJsonObject();
		} else return null;	
	}
	
}

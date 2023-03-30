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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

public class OnDemandApi {
	static final int DEFAULT_PORT = 4568;
	static final String TEST_MODE = "testmode";
	private int port;
    private Service spark;
    private final Logger logger = LoggerFactory.getLogger(OnDemandApi.class);
    private RequestManager requestManager;
    private static boolean testMode = false;
	
    public OnDemandApi(int port, String orionUrl) {
    	this.port = port;
    	requestManager = new RequestManager(orionUrl);
    }
    
    public void start() {
    	spark = Service.ignite().port(port);
    	logger.info("Starting...");
    	if(testMode) logger.info("Test mode");
    	// Swagger UI
    	spark.staticFileLocation("/public");
    	spark.get("/swagger",(req, res) -> {
    		res.redirect("index.html");
    		res.header("Access-Control-Allow-Origin", "*");
    		res.header("Access-Control-Allow-Methods", "GET");
    		res.header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, Accept");
    		return null;
    		});
    	
    	spark.post("/current", (request, response) -> {
    		// Last values request
			String responseBody;
			String auth;
			try {
				JsonParser parser = new JsonParser();
				JsonObject reqBody = (JsonObject) parser.parse(request.body());
				auth = request.headers("Authorization");
				if(auth==null && testMode==false) {
					response.status(401);
					return "Unauthorized";
				}
				// Parse query
    			Query query = new Gson().fromJson(reqBody, Query.class);
				responseBody = requestManager.lastValuesRequest(query, auth);
			} catch (Exception e) {
				response.status(400);
                logger.error("Exception: " , e);
                return e.getMessage();
			}
    		
    		response.header("Content-Type", "application/json;charset=UTF-8");
    		response.header("Access-Control-Allow-Origin", "*");
    		response.header("Access-Control-Allow-Methods", "POST");
    		response.header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, Accept");
    		response.status(200);
    		return responseBody;
    	});
    	
    	spark.post("/historical", (request, response) -> {
    		// Historical data request
			String responseBody;
			String auth;
			try {
				JsonParser parser = new JsonParser();
				JsonObject reqBody = (JsonObject) parser.parse(request.body());
				auth = request.headers("Authorization");
				if(auth==null && testMode==false) {
					response.status(401);
					return "Unauthorized";
				}
				// Parse query
    			Query query = new Gson().fromJson(reqBody, Query.class);
				responseBody = requestManager.historicalDataRequest(query, auth);
			} catch (Exception e) {
				response.status(400);
				logger.error("Exception: " , e);
                return e.getMessage();
			}
    		
    		response.header("Content-Type", "application/json;charset=UTF-8");
    		response.header("Access-Control-Allow-Origin", "*");
    		response.header("Access-Control-Allow-Methods", "POST");
    		response.header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, Accept");
    		response.status(200);
    		return responseBody;
    	});
    	
    	//HTTP OPTIONS added for integration with nodejs applications
    	spark.options("/current", (request, response) -> {   
    		response.header("Access-Control-Allow-Origin", "*");
    		response.header("Access-Control-Allow-Methods", "POST");
    		response.header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, Accept");
    		response.header("Allow", "POST, OPTIONS");
    		response.status(200);
    		return "";
        });
    	spark.options("/historical", (request, response) -> {   
    		response.header("Access-Control-Allow-Origin", "*");
    		response.header("Access-Control-Allow-Methods", "POST");
    		response.header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, Accept");
    		response.header("Allow", "POST, OPTIONS");
    		response.status(200);
    		return "";
        });
    	
    }
    
    public void stop() {
        spark.stop();
    }
    
	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		String orionUrl = null;
    	if (args.length > 0){
    		port = Integer.parseInt(args[0]);
    		if (args.length > 1){
    			orionUrl = args[1];
    			if (args.length > 2 && args[2].equals(TEST_MODE)){
    				// Enable test mode (token not mandatory)
    				testMode = true;
    			}
    		}
    	}
    	new OnDemandApi(port, orionUrl).start();
	}

}

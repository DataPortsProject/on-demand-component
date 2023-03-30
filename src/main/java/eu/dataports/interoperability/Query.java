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

class Query {
	String service;
	String servicePath;
	String startDate;
	String endDate;
	int limit;
	int offset;
	String id;
	String type;
	String callback;
	JsonObject attributes;
	
    // Utility methods
	public String getService(){		
 		return service;
 	}
	
	public String getServicePath(){		
 		return servicePath;
 	}
	
 	public String getStartDate(){		
 		return startDate;
 	}
 	
 	public String getEndDate(){		
 		return endDate;
 	}
	
 	public int getLimit() {
 		// Default value in Orion = 20
 		return limit;
 	}
 	
 	public int getOffset() {
 		// Default value in Orion = 0
 		return offset;
 	}
 	
 	public String getId(){		
 		return id;
 	}
 	
 	public String getType(){		
 		return type;
 	}
 	
 	public String getCallback(){		
 		return callback;
 	}
 	
 	public JsonObject getAttributes() {
 		return attributes;
 	}
 	
 	// Test
    public String toString(){   	    	
    	Gson gson = new Gson();	
    	return gson.toJson(this);
    }
}

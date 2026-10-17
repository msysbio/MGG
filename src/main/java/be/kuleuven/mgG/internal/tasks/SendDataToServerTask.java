package be.kuleuven.mgG.internal.tasks;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.prefs.Preferences;
import org.cytoscape.work.AbstractTask;

import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.utils.LogUtils;


/**
 * This class represents a task for sending data to a server.
 * The task sends a JSON array as string to microbetag server URL and retrieves the server's response.
 */


public class SendDataToServerTask extends AbstractTask {

	// The public microbetag server, and the route that annotates the data sent
	public static final String PUBLIC_SERVER = "https://msysbio.gbiomed.kuleuven.be";
	private static final String ANNOTATE_ROUTE = "/upload-abundance-table-dev";

	// The last server used is remembered across Cytoscape sessions
	private static final Preferences PREFS = Preferences.userNodeForPackage(SendDataToServerTask.class);
	private static final String SERVER_PREF = "microbetagServer";
 	
	private  String serverResponse; // Stores the server response
	private final JSONObject dataObject; // The JSON array to send to the server
	private final JSONObject metaDataObject;
	private final JSONObject networkObject;
    private final MGGManager mggManager;  // The MGGManager instance for retrieving the JSON array
        
 	// @Tunables part -- Parameters/Arguments      

    
     @Tunable(
    		 description="Choose input type", 
    		 groups={"Input Parameters"}, 
    		 gravity=1.0, 
    		 required=true
    )
     public ListSingleSelection<String> INPUT = new ListSingleSelection<>(	);
     
     @Tunable(
    		 description="heterogeneous",
    		 tooltip="Consider confounding factors" , 
    		 groups={"Additional Parameter if Input is Abudance Table"}, 
    		 dependsOn = "INPUT=abundance table", 
    		 gravity=10.0, 
    		 required=true
    )
     public boolean HETEROGENEOUS=false;
     
     @Tunable(
    		 description="sensitive",
    		 tooltip="Use full abundance information (default: discretized)" , 
    		 groups={"Additional Parameter if Input is Abudance Table"}, 
    		 dependsOn = "INPUT=abundance table", 
    		 gravity=11.0, 
    		 required=true
    )
     public boolean SENSITIVE=false;
  
     
     @Tunable(
    		 description="max_k",
    		 tooltip="Maximum size of conditioning sets, high values can strongly increase runtime. \n"
    		 		+ "max_k = 0 results in no conditioning (univariate mode). (default: 3)" , 
    		 groups={"Additional Parameter if Input is Abudance Table"}, 
    		 dependsOn = "INPUT=abundance table", 
    		 gravity=11.0, 
    		 required=true
    )
     public int MAX_K=3;
       
     
     
     
     
     @Tunable(
    		 description="Choose delimiter", 
    		 groups={"Input Parameters"},
    		 tooltip="Delimiter used in your taxonomy",
    		 gravity=2.0, required=true
     )
     public ListSingleSelection<String> DELIMITER = new ListSingleSelection<>(";", "|","__","_");

     @Tunable(
    		 description="Choose taxonomy Database",
    		 tooltip="Choose the taxonomy in the abudance table among GTDB,"
    				 + " Silva(as in Dada2), microbetag_prep or other", 
    		groups={"Input Parameters"}, 
    		gravity=3.0, required=true
    )
     public ListSingleSelection<String> TAXONOMY = new ListSingleSelection<>("GTDB", "Silva","microbetag_prep", "other");
     
     @Tunable(
    		description="Phenotrex-based annotations", 
    		longDescription="Choose whether to get PHEN_TRAITS information.",
    		groups={"Input Parameters"}, 
     		tooltip="Choose whether to get Phenotypic traits based on genomic information",
     		gravity=4.0, 
     		exampleStringValue="True, False", 
     		required=true
     )
     public boolean PHEN_TRAITS=true;

     @Tunable(description="FAPROTAX annotations", longDescription="Choose whether to get FAPROTAX information.", groups={"Input Parameters"}, 
     		tooltip="Choose whether to get Phenotypic traits based on literature" , gravity=5.0, exampleStringValue="True, False", required=true)
     public boolean FAPROTAX=true; 

     @Tunable(description="Pathway Complementarity", longDescription="Choose whether to get the pathway complementarity.", 
     		 tooltip="Choose whether to get Pathway Complementarity annotations" ,groups={"Input Parameters"}, gravity=6.0, exampleStringValue="True, False", required=true)
     public boolean PATH_COMPLEMENTS=true;
     
     @Tunable(description="Seed scores and complements", tooltip="Choose whether to get the Seed Scores and it's complements.",
    		 longDescription="Choose whether to get the Seed Scores and  complements.", groups={"Input Parameters"}, gravity=7.0, exampleStringValue="True, False", required=true)
     public boolean SEED_COMPLEMENTS= false;
     
     @Tunable(description="Network clustering", longDescription="Choose whether to get NETWORK_CLUSTERING clustering", groups={"Input Parameters"}, 
      		tooltip="Choose whether to get NETWORK_CLUSTERING clustering" , gravity=9.0, exampleStringValue="True, False", required=true)
     public boolean NETWORK_CLUSTERING=false; 

     @Tunable(
    		 description="Consider Children taxa", 
    		 groups={"Input Parameters"},
			 dependsOn = "TAXONOMY=other",
    		 tooltip="Use strain genomes in case no type species genome supported", 
    		 gravity=8.0, 
    		 exampleStringValue="True, False", 
    		 required=true)
     public boolean GET_CHILDREN=false; 
     
     
     //@Tunable(description="NetCmpt", longDescription="Choose whether to use NetCmpt.", groups={"Input Settings"}, gravity=6.0, exampleStringValue="True, False", required=true)
     //public boolean netCmpt= true;
    				
    
   
    /**
     * Constructs a new SendDataToServerTask object.
     *
     * @param jsonArray   The JSON array to send to the server.
     * @param mggManager  The MGGManager instance for retrieving the JSON array.
     */
    
    public SendDataToServerTask(MGGManager mggManager) {
    	
		this.mggManager     = mggManager;
    	this.dataObject     = mggManager.getJsonObject();
    	this.metaDataObject = mggManager.getMetadataJsonObject();
    	this.networkObject  = mggManager.getNetworkObject();

        // Dynamically determine INPUT options based on whether networkObject is empty;
        // a server other than the public one needs a network
        if (!isPublicServer()) {
            this.INPUT = new ListSingleSelection<>("network");
        } else if (networkObject != null && !networkObject.isEmpty()) {
            this.INPUT = new ListSingleSelection<>("abundance table", "network");
        } else {
            this.INPUT = new ListSingleSelection<>("abundance table");
        }
    
    
    }
    
    /**
     * Whether the public server is set. Another server, e.g. one of your own genomes, does not infer networks:
     * it needs a network (its metadata mark the network's nodes of metadata variables).
     */
    public static boolean isPublicServer() {
        return annotateURL(getServer()).equals(annotateURL(PUBLIC_SERVER));
    }

    /** The microbetag server set with Apps > MGG > Set microbetag server; our public server by default. */
    public static String getServer() {
        return PREFS.get(SERVER_PREF, PUBLIC_SERVER);
    }

    public static void setServer(String server) {
        PREFS.put(SERVER_PREF, (server == null || server.trim().isEmpty()) ? PUBLIC_SERVER : server.trim());
    }

    /**
     * The URL to send the data to: a server's address, e.g. http://localhost:5000, gets the annotation route;
     * a full URL, with a path, is used as it is.
     */
    static String annotateURL(String server) {
        String url = (server == null || server.trim().isEmpty()) ? PUBLIC_SERVER : server.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // No path after the host: add the route
        if (url.indexOf('/', url.indexOf("//") + 2) < 0) {
            url += ANNOTATE_ROUTE;
        }
        return url;
    }

    /**
     * Runs the task to send data to the server.
     *
     * @param taskMonitor The task monitor to display progress and status messages.
     */
    
    
    @SuppressWarnings("unchecked")
	@Override
    public void run(TaskMonitor taskMonitor) {
    	
    	// Create a new JSONObject
        JSONObject jsonObject = new JSONObject();
        
        // The abundance table, or, for a network without one, the taxonomy of its nodes (node names and
        // taxonomies, without samples)
        JSONArray abundanceData = mggManager.abundanceOrTaxonomy();
        boolean taxonomyOnly = !(dataObject != null && dataObject.containsKey("data"));
        if (abundanceData != null) {
            jsonObject.put("abundance_data", abundanceData);
        }

        // Add the 'metadata' JSONArray from metaDataObject
        // With any server: for network inference (public server), and to mark the nodes of metadata variables
        // in the network given
        if (metaDataObject != null && metaDataObject.containsKey("metadata")) {
            JSONArray metaDataJsonArray = (JSONArray) metaDataObject.get("metadata");
            jsonObject.put("metadata", metaDataJsonArray);
        }
        
        // Add the 'network data' JSONArray from networkObject
        if (networkObject != null && networkObject.containsKey("network")) {
            JSONArray networkJsonArray = (JSONArray) networkObject.get("network");
            jsonObject.put("network", networkJsonArray);
        }
    	
        // Create a new JSONArray for the input parameters
	       JSONArray inputParameters = new JSONArray();
	        
	 
	        // Without an abundance table, the network is the input: there are no samples to infer one from
	        inputParameters.add("input_type:" + (taxonomyOnly ? "network" : INPUT.getSelectedValue()));
	        inputParameters.add("taxonomy:" + TAXONOMY.getSelectedValue());
	        inputParameters.add("delimiter:" + DELIMITER.getSelectedValue()); 
	        inputParameters.add("sensitive:" + SENSITIVE);
	        inputParameters.add("heterogeneous:" + HETEROGENEOUS);
	        inputParameters.add("max_k:" + MAX_K);
	        inputParameters.add("phen_traits:" + PHEN_TRAITS);
	        inputParameters.add("faprotax:" + FAPROTAX);
	        inputParameters.add("pathway_complementarity:" + PATH_COMPLEMENTS);
	        inputParameters.add("seed_complementarity:" + SEED_COMPLEMENTS);
	        inputParameters.add("network_clustering:" + NETWORK_CLUSTERING);
	        inputParameters.add("get_children:" + GET_CHILDREN);	
	        
	        // Add the input parameters to the jsonObject
	        jsonObject.put("parameters", inputParameters);
   	
    	
	    // Send data and args to server.. 	    
        taskMonitor.setTitle("Sending Data to Server");
        taskMonitor.setStatusMessage("Processing Data on Server( May take some time... )");       
        
       
        // .. wait for its response
        RequestConfig config = RequestConfig.custom()
        	    .setConnectTimeout(600 * 1000)  // time to establish the connection 
        	    .setSocketTimeout(600 * 1000)  // time waiting for data 
        	    .setConnectionRequestTimeout(600 * 1000) // time to wait for a connection from  manager/pool
        	    .build();

        
        CloseableHttpClient httpClient = HttpClients.custom()
          .setDefaultRequestConfig(config)
          .build() ;              
              

        try {
                String jsonQuery = jsonObject.toJSONString();
                // Set with Apps > MGG > Set microbetag server
                String serverURL = annotateURL(getServer());
                taskMonitor.setStatusMessage("Sending your data to " + serverURL);

                HttpPost httpPost = new HttpPost(serverURL);
                httpPost.setConfig(config);
                
                StringEntity entity = new StringEntity(jsonQuery);
                
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    
                    HttpEntity responseEntity = response.getEntity();
                    
                    String responseString = EntityUtils.toString(responseEntity);
                    
                    int statusCode = response.getStatusLine().getStatusCode();
                    
                    if (statusCode != 200 && statusCode != 202) {
                    	taskMonitor.showMessage(TaskMonitor.Level.ERROR,"Status code:" + statusCode);
                    }
                    
                    
                   try {
                	   JSONArray jsonResponse =  (JSONArray) new JSONParser().parse(responseString);
                  	   mggManager.setServerResponse(jsonResponse);
                  	   taskMonitor.setStatusMessage("Processing server response");
                  	   taskMonitor.setStatusMessage("Data sent to server and got the response successfully.");
                  	  
                  	   // Set jsonObject and metadataObject to null if the response is successful
                       if(responseString !=null) {
						 mggManager.setJsonObject(null);
						 mggManager.setMetadataJsonObject(null);
						 mggManager.setNetworkObject(null);
						 mggManager.setTaxonomyJsonObject(null);
						 mggManager.setPendingNetworkObject(null);
                        }                        
                      	  
                   } catch (Exception e) {
                	    // HTTP response is received, but it's not a JSONArray, [{}, {}] but a JSONObject {}  
		    	    	JSONObject jsonResponseError =  (JSONObject) new JSONParser().parse(responseString);
		    	    	String errorMessage = (String) jsonResponseError.get("error");
		    	    	String traceback    = (String) jsonResponseError.get("traceback");
		    	    	taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error:" +  errorMessage);
		    	    	taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Traceback:" + traceback);
						
						 // Set jsonObject and metadataObject to null if the response is successful
						if(responseString !=null) {
		                 	 mggManager.setJsonObject(null);
		                 	 mggManager.setMetadataJsonObject(null);
		                 	 mggManager.setNetworkObject(null);
		                 	 mggManager.setTaxonomyJsonObject(null);
		                 	 mggManager.setPendingNetworkObject(null);
		                 }
                	 }
                                                              
                } catch (Exception e) {
              	 
                    taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error while waiting for the response: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
                
        } catch (Exception e) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error while setting up the request or processing the response: " + e.getMessage());
            e.printStackTrace(System.out);

        } 
        
        finally {
			try {
				httpClient.close();
			
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}
            
			taskMonitor.setStatusMessage("Process finished");
        }    
    }
}
    

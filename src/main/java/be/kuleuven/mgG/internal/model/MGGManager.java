package be.kuleuven.mgG.internal.model;


import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.IN_TOOL_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import be.kuleuven.mgG.internal.tasks.ImportFileTaskFactory;
import be.kuleuven.mgG.internal.tasks.SendDataToServerTaskFactory;
import be.kuleuven.mgG.internal.view.JSONDisplayPanel;
import be.kuleuven.mgG.internal.view.MGGCytoPanel;
import be.kuleuven.mgG.internal.tasks.ShowResultsPanelTaskFactory ;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;



/**
 * The MGGManager class is responsible for managing the state of the MGG application.
 * It provides methods to store and retrieve data, execute tasks, and register services for the tasks and taskfactories to use instead of cyactivator
 * 
 */



public class MGGManager implements SessionAboutToBeSavedListener, SessionLoadedListener {
	
	
	public final static String APP_NAME = "be.kuleuven.mgG";
	public final static String SERVER_RESPONSE_FILE = "Response.json";
	
	//-----------------------------------------------------------
	final CommandExecutorTaskFactory commandExecutorTaskFactory;
	final SynchronousTaskManager<?> synchronousTaskManager;
	final TaskManager<?,?> dialogTaskManager;
	
	//----------------------------------------------------------------
	final TaskManager taskManager;
	final SynchronousTaskManager syncTaskManager;
	
	final CyServiceRegistrar cyRegistrar; 
	
	final AvailableCommands availableCommands;
	final CommandExecutorTaskFactory ceTaskFactory;
	
	private ShowResultsPanelTaskFactory resultsPanelTaskFactory;
	//-----------------------------------------------------------
	private MGGCytoPanel cytoPanel = null;
	
	  private CyNetwork newNetwork = null;
	//----------------------------------------------------------
	private JSONObject jsonObject;
	private JSONArray serverResponse;
	
	private JSONObject metadataObject;
	private JSONObject networkObject;
	
	private boolean highlightNeighbors = false;
	private boolean showMspecies = false;
	private boolean showSingletons = false;
	
	private String[] EnrichmentAttributeString;
	
	
	
	private Map<String, Color> channelColors;
	
	public static String[] channels = { "cooperation", "competition"};
	
	public static String HighlightNeighbors = "highlightNeighbors";
	public static String ShowSingletons = "showSingletons";

	public static String[] signs = {">","<"}; 

	
	//private Icon MGGicon;

	
	 /**
     * Constructor for the MGGManager class.
     * This constructor initializes the MGGManager with a CyServiceRegistrar, which is used to access Cytoscape services.
     * It also registers the MGGManager as a listener for session events, specifically when a session is about to be saved and when a session is loaded.
     *
     * @param cyRegistrar The CyServiceRegistrar used to access Cytoscape services.
     */
	
	public MGGManager(final CyServiceRegistrar cyRegistrar) {
		 // Store the CyServiceRegistrar
		this.cyRegistrar = cyRegistrar;
		
		 // Get Cytoscape services
		this.taskManager = cyRegistrar.getService(TaskManager.class);
		this.availableCommands = cyRegistrar.getService(AvailableCommands.class);
		this.ceTaskFactory = cyRegistrar.getService(CommandExecutorTaskFactory.class);
		this.syncTaskManager = cyRegistrar.getService(SynchronousTaskManager.class);
		
		// Register this manager as a listener for session events
		cyRegistrar.registerService(this, SessionAboutToBeSavedListener.class, new Properties());
		cyRegistrar.registerService(this, SessionLoadedListener.class, new Properties());
		
		synchronousTaskManager = cyRegistrar.getService(SynchronousTaskManager.class);
		commandExecutorTaskFactory = cyRegistrar.getService(CommandExecutorTaskFactory.class);
		dialogTaskManager = cyRegistrar.getService(TaskManager.class);
		//MGGicon = new ImageIcon(getClass().getResource("/images/scNetViz.png"));
		
		
		channelColors = new HashMap<>();
		// Set up our default channel colors
		
		channelColors.put("cooperation",Color.MAGENTA);
		channelColors.put("competition",Color.BLUE);
		
					
	}
	

	 /**
     * Sets the JSONObject from data(abundance table).
     * This method is used to store a JSONObject which can be used later.
     *
     * @param jsonArray The JSONArray object to be stored.
     */
    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    /**
     * Gets the stored JSONObject data(abundance table) .
     * This method is used to retrieve the stored JSONObject.
     *
     * @return The stored JSONObject.
     */
    public JSONObject getJsonObject() {
        return jsonObject;
    }
	
    
    /**
     * Sets the JSONObject of the metadata file.
     * 
     * This method is used to store a JSONObject which can be used later.
     * 
     * @param jsonArray The JSONObject to be stored.
     */
    public void setMetadataJsonObject(JSONObject jsonObject) {
        this.metadataObject= jsonObject;
    }

    /**
     * Gets the stored JSONObject from the metadata.
     * This method is used to retrieve the stored JSONObject.
     *
     * @return The stored JSONObject.
     */
    public JSONObject getMetadataJsonObject() {
        return metadataObject;
    }
    
    
    public void setNetworkObject(JSONObject jsonObject) {
    	this.networkObject=jsonObject;
    }
    
    public JSONObject getNetworkObject() {
    	return networkObject;
    }

    // The taxonomy of each node of a network, when there is no abundance table (Import Taxonomy):
    // {"taxonomy": [["seqId", "taxonomy"], [node, taxonomy], ...]}
    private JSONObject taxonomyObject;

    public JSONObject getTaxonomyJsonObject() {
        return taxonomyObject;
    }

    public void setTaxonomyJsonObject(JSONObject jsonObject) {
        this.taxonomyObject = jsonObject;
    }

    /**
     * What is sent to the server as the abundance data: the abundance table, or, without one, the taxonomy of
     * each node (a table of node names and taxonomies, without samples). Null if neither is imported.
     */
    public JSONArray abundanceOrTaxonomy() {
        if (jsonObject != null && jsonObject.containsKey("data")) {
            return (JSONArray) jsonObject.get("data");
        }
        if (taxonomyObject != null && taxonomyObject.containsKey("taxonomy")) {
            return (JSONArray) taxonomyObject.get("taxonomy");
        }
        return null;
    }

    /** Whether the taxa of a network are known: an abundance table or a taxonomy file is imported. */
    public boolean hasTaxa() {
        return abundanceOrTaxonomy() != null;
    }

    /**
     * After an import: loads the network imported before if its nodes are now all known, and says so; or says
     * what its nodes still miss. Empty if there is no pending network.
     */
    public String pendingNetworkNote() {
        if (pendingNetworkObject == null) {
            return "";
        }
        if (acceptPendingNetwork()) {
            return "\nThe network imported before is now loaded too: all its nodes are known.";
        }
        java.util.List<String> unknown = unknownNetworkNodes(pendingNetworkObject);
        return "\nThe network imported before still has " + unknown.size() + " node(s) without a taxonomy that are "
            + "not metadata variables either, e.g. " + String.join(", ", unknown.subList(0, Math.min(5, unknown.size())))
            + ".\n" + whatNodesNeed();
    }

    /** What the nodes of a network need, for the messages of the imports. */
    public static String whatNodesNeed() {
        return "Each node of the network needs a taxonomy, from an abundance table (Import Abundance Data) or a file "
            + "of node names and their taxonomies (Import Taxonomy), unless it is a metadata variable (Import Metadata).";
    }

    // A network whose nodes are not all known yet: kept until their taxonomies or the metadata are imported
    private JSONObject pendingNetworkObject;

    public JSONObject getPendingNetworkObject() {
        return pendingNetworkObject;
    }

    public void setPendingNetworkObject(JSONObject jsonObject) {
        this.pendingNetworkObject = jsonObject;
    }

    /**
     * The nodes of a network that are neither sequences of the abundance table (or nodes of the taxonomy file)
     * nor metadata variables.
     * A node of a metadata variable is named after it (e.g. ENV1, or ENV4_A for a value of a categorical
     * variable): its name starts with the name of a variable of the imported metadata (the first cell of each
     * of its rows), as the microbetag server tells them apart.
     */
    public java.util.List<String> unknownNetworkNodes(JSONObject networkJsonObject) {
        java.util.Set<String> nodeNamesInNetwork = new java.util.HashSet<>();
        org.json.simple.JSONArray networkData = (org.json.simple.JSONArray) networkJsonObject.get("network");
        for (int i = 1; i < networkData.size(); i++) {
            org.json.simple.JSONArray edge = (org.json.simple.JSONArray) networkData.get(i);
            nodeNamesInNetwork.add((String) edge.get(0));
            nodeNamesInNetwork.add((String) edge.get(1));
        }

        // Sequences of the abundance table, or nodes of the taxonomy file
        java.util.Set<String> nodeNamesInData = new java.util.HashSet<>();
        org.json.simple.JSONArray data = abundanceOrTaxonomy();
        if (data != null) {
            for (int i = 1; i < data.size(); i++) {
                nodeNamesInData.add((String) ((org.json.simple.JSONArray) data.get(i)).get(0));
            }
        }

        java.util.List<String> metavars = new java.util.ArrayList<>();
        JSONObject metadataJsonObject = getMetadataJsonObject();
        if (metadataJsonObject != null && metadataJsonObject.containsKey("metadata")) {
            for (Object row : (org.json.simple.JSONArray) metadataJsonObject.get("metadata")) {
                org.json.simple.JSONArray cells = (org.json.simple.JSONArray) row;
                if (!cells.isEmpty() && cells.get(0) != null && !cells.get(0).toString().trim().isEmpty()) {
                    metavars.add(cells.get(0).toString().trim());
                }
            }
        }

        java.util.List<String> unknown = new java.util.ArrayList<>();
        for (String node : nodeNamesInNetwork) {
            if (nodeNamesInData.contains(node)) {
                continue;
            }
            boolean isMetavar = false;
            for (String metavar : metavars) {
                if (node.startsWith(metavar)) {
                    isMetavar = true;
                    break;
                }
            }
            if (!isMetavar) {
                unknown.add(node);
            }
        }
        java.util.Collections.sort(unknown);
        return unknown;
    }

    /**
     * Accepts the pending network if its nodes are now all known (once their taxonomies or the metadata are
     * imported).
     * Returns true if it was accepted.
     */
    public boolean acceptPendingNetwork() {
        if (pendingNetworkObject != null && unknownNetworkNodes(pendingNetworkObject).isEmpty()) {
            setNetworkObject(pendingNetworkObject);
            pendingNetworkObject = null;
            return true;
        }
        return false;
    }
    
    
    /**
     * Sets the server response.
     * This method is used to store the server response which can be used later.
     * 
     * @param jsonResponse The server response in the form of a JSONArray.
     */
    public void setServerResponse(JSONArray jsonResponse) {
        this.serverResponse = jsonResponse;
    }
	

    /**
     * Gets the stored server response.
     * This method is used to retrieve the stored server response.
     *
     * @return The stored server response in the form of a JSONObject.
     */
    public JSONArray getServerResponse() {
        return this.serverResponse;
    }
    
    
    
    //Setters for phendb and faprotax
   // public void setPhendbAttributeList(String[] phendbAttributeList) {
    	
    //	this.phendbAttributeList = phendbAttributeList;
   // }
    
    public void setEnrichmentAttributeString(String[]termOptions) {
    	this.EnrichmentAttributeString=termOptions;
    }
    
   // //getters for phendb and faprotax attributes
   // public String[] getPhendbAttributeList(){
   // 	return phendbAttributeList;
   // }
    
    public String [] getEnrichmentAttributeString(){
    	return EnrichmentAttributeString;
    }
    
    
    
    public Map<String, Color> getChannelColors() { return channelColors; }

	public void setChannelColors(Map<String, Color> colorMap) { channelColors = colorMap; }
	public void setChannelColors(String colors) { 
		String[] colorStrs = colors.split("\\|");
		if (colorStrs.length != 8) return;

		channelColors = new HashMap<>();
		for (int i = 0; i < colorStrs.length; i++) {
			channelColors.put(channels[i], parseColor(colorStrs[i]));
		}
	}
	private Color parseColor(String s) {
		int r = 0, g = 0, b = 0;
		if (s.length() == 9)
			s = s.substring(3);
		else if (s.length() == 7)
			s = s.substring(1);
		else return Color.BLACK;

		r = Integer.parseInt(s.substring(0,2), 16);
		g = Integer.parseInt(s.substring(2,4), 16);
		b = Integer.parseInt(s.substring(4,6), 16);
		return new Color(r,g,b);
	}
	
  //-----------------------------addition------------------------------for Cytopanel------------------------------------------------------------------------------------------  
    
    public void setCytoPanel(MGGCytoPanel panel) {
  		this.cytoPanel = panel;
  	}
      
    public CyNetwork getCurrentNetwork() {
		CyNetwork network = cyRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
    if (network != null) return network;
    return newNetwork;
	}


    
	public void setShowResultsPanelTaskFactory(ShowResultsPanelTaskFactory factory) {
		resultsPanelTaskFactory = factory;		
	}

	public ShowResultsPanelTaskFactory getShowResultsPanelTaskFactory() {
		return resultsPanelTaskFactory;		
	}

	
	
	public boolean highlightNeighbors() { return highlightNeighbors; }

	public void setHighlightNeighbors(boolean set) { 
		highlightNeighbors = set; 
	}

	public boolean showMspecies() { return showMspecies; }
	public void setShowMspecies(boolean set) { 
		showMspecies = set; 
	}
	
	public boolean showSingletons() { return showSingletons; }

	public void setShowSingletons(boolean set) { 
		showSingletons = set; 
	}

	
	
	
	public void updateControls() {
		if (cytoPanel != null)
			cytoPanel.updateControls();
	}
	
	
    
    //------------------------------------------------SErvice Register and execute Tasks-----------------------------------------------------------------------------------------------
    
    
    
    
    public void executeCommand(String namespace, String command, 
            Map<String, Object> args, TaskObserver observer) {
    		TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
    		execute(ti, true);
    }
    
    public TaskIterator getCommandTaskIterator(String namespace, String command, 
            Map<String, Object> args, TaskObserver observer) {
    		return commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
    }
    
    
    public void execute(TaskIterator iterator, boolean synchronous) {
			if (synchronous) {
				synchronousTaskManager.execute(iterator);
				} else {
					dialogTaskManager.execute(iterator);
			}
	}
    	
    public CyNetworkView getCurrentNetworkView() {
		return cyRegistrar.getService(CyApplicationManager.class).getCurrentNetworkView();
	}
    
    /**
     * Executes a set of tasks.
     * This method is used to execute a set of tasks using the task manager.
     * The tasks are executed in the order they are added to the TaskIterator.
     *
     * @param tasks The TaskIterator containing the tasks to be executed.
     */
    
    public void executeTasks(TaskIterator tasks) {
        taskManager.execute(tasks);
    } 

 
			    /**
			     * Retrieves a service of the specified class.
			     * This method is used to get a service registered in the Cytoscape environment.
			     *
			     * @param serviceClass The class of the service to be retrieved.
			     * @return The service of the specified class.
			     */
    
    			public <S> S getService(Class<S> serviceClass) { 
    				return cyRegistrar.getService(serviceClass); 
    				
    			}
    		  
			    /**
			     * Retrieves a service of the specified class and filter.
			     * This method is used to get a service registered in the Cytoscape environment that matches a specific filter.
			     *
			     * @param serviceClass The class of the service to be retrieved.
			     * @param filter The filter to match the service against.
			     * @return The service of the specified class and filter.
			     */
    
    		  public <S> S getService(Class<S> serviceClass, String filter) { return
    		  cyRegistrar.getService(serviceClass, filter); }
    		  
    		  
    		  /**
    		     * Registers a service in the Cytoscape environment.
    		     * This method is used to register a service in the Cytoscape environment with the specified properties.
    		     *
    		     * @param service The service to be registered.
    		     * @param serviceClass The class of the service to be registered.
    		     * @param props The properties of the service to be registered.
    		     */
    		  
    		  public void registerService(Object service, Class<?> serviceClass, Properties
    		  props) { cyRegistrar.registerService(service, serviceClass, props); }
    		  
    		  
    		  /**
    		     * Unregisters a service from the Cytoscape environment.
    		     * This method is used to unregister a service from the Cytoscape environment.
    		     *
    		     * @param service The service to be unregistered.
    		     * @param serviceClass The class of the service to be unregistered.
    		     */
    		  
    		  public void unregisterService(Object service, Class<?> serviceClass) {
    		  cyRegistrar.unregisterService(service, serviceClass); }
	
    
    
    
    
    
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    		  public void showResultsPanel() {
    				if (cytoPanel == null) {
    					execute(resultsPanelTaskFactory.createTaskIterator(), true);
    				} else {
    					// Make sure we show it
    					cytoPanel.showCytoPanel();
    				}
    			}

    			public void hideResultsPanel() {
    				if (cytoPanel != null) {
    					cytoPanel.hideCytoPanel();
    				}
    			}

    			public void reinitResultsPanel(CyNetwork network) {
    				if (cytoPanel == null) {
    					execute(resultsPanelTaskFactory.createTaskIterator(), true);
    				} else {
    					// Make sure we show it
    					cytoPanel.reinitCytoPanel();
    				}
    			}

    			
    //------------------------------------------------------------------------------------------------------------------------
    //has functionality
    			
    			
    			public boolean haveClusterMaker() {
    				return availableCommands.getNamespaces().contains("cluster");
    			}			
    			
    			
    			
    			
    			
    			
    			
    			
    			
   //------------------------------------------------------------------------------------------ 			
    			
    	/**
    	 * Handles the SessionLoadedEvent.
    	 * This method is called when a session is loaded in Cytoscape.
    	 * It checks if there are any files related to the MGG application in the session and loads them if they exist.
    	 *
    	 * @param e The SessionLoadedEvent.
    	*/
	
    	@Override
    		  // See if we have data in the session, and load it if we do
    		public void handleEvent(SessionLoadedEvent e) {
			System.out.println("SessionLoaded");
			
			Map<String,List<File>> appFiles = e.getLoadedSession().getAppFileListMap();
			if (!appFiles.containsKey(APP_NAME)) {
				System.out.println("Don't see "+APP_NAME+"!");
				return;
			}

			List<File> mggFiles = appFiles.get(APP_NAME);
			Map<String, File> fileMap = new HashMap<>();
			for (File f: mggFiles) {
				System.out.println("File map has file: "+f.getName());
				fileMap.put(f.getName(),f);
			}

			if (!fileMap.containsKey(SERVER_RESPONSE_FILE)) {
				System.out.println("Don't see "+SERVER_RESPONSE_FILE+"!");
				return;
			}	
    	}
    	
    	 /**
         * Handles the SessionAboutToBeSavedEvent.
         * This method is called when a session is about to be saved in Cytoscape.
         * It saves the server response to a file and adds it to the session.
         *
         * @param e The SessionAboutToBeSavedEvent.
         */
    	@Override
    	public void handleEvent(SessionAboutToBeSavedEvent e) {
    	    String tmpDir = System.getProperty("java.io.tmpdir");
    	    File jsonFile = new File(tmpDir, SERVER_RESPONSE_FILE);

    	    try {
    	        // Check if serverResponse is not null and not empty
    	    	// The empty string is a string with zero length. The null value is not having a string at all.
    	        if (serverResponse != null && !serverResponse.isEmpty()) {
    	            FileOutputStream fos = new FileOutputStream(jsonFile);
    	            OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
    	            BufferedWriter writer = new BufferedWriter(osw);

    	            // Write the JSON string representation of serverResponse to file
    	            writer.write(serverResponse.toJSONString());
    	            writer.close();
    	            osw.close();
    	            fos.close();

    	            List<File> files = new ArrayList<>();
    	            files.add(jsonFile);

    	            try {
    	                e.addAppFiles(APP_NAME, files);
    	            } catch (Exception add) {
    	                add.printStackTrace();
    	            }
    	        } else {
    	            // Handle the case where serverResponse is null or empty
    	            System.err.println("serverResponse is null or empty, not writing to file.");
    	        }
    	    } catch (Exception jsonException) {
    	        jsonException.printStackTrace();
    	    }
    	}

}
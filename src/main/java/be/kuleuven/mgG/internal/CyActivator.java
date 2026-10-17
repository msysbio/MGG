package be.kuleuven.mgG.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;

import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.IN_TOOL_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;
import org.osgi.framework.BundleContext;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.service.util.AbstractCyActivator;

import be.kuleuven.mgG.internal.tasks.AboutTaskFactory;
import be.kuleuven.mgG.internal.tasks.SetServerTaskFactory;
import be.kuleuven.mgG.internal.tasks.CheckAbudanceFileTaskFactory;
import be.kuleuven.mgG.internal.tasks.CheckMetaDataFileTaskFactory;
import be.kuleuven.mgG.internal.tasks.CheckNetworkTaskFactory;
import be.kuleuven.mgG.internal.tasks.CreateMGGVisualStyleTaskFactory;
import be.kuleuven.mgG.internal.tasks.ImportFileTaskFactory;
import be.kuleuven.mgG.internal.tasks.ImportMetadataTaskFactory;
import be.kuleuven.mgG.internal.tasks.ImportTaxonomyTaskFactory;
import be.kuleuven.mgG.internal.tasks.ImportNetworkDataTaskFactory;
import be.kuleuven.mgG.internal.tasks.SendDataToServerTaskFactory;
import be.kuleuven.mgG.internal.tasks.ShowResultsPanelAction;
import be.kuleuven.mgG.internal.tasks.ShowResultsPanelTaskFactory;
import be.kuleuven.mgG.internal.utils.Mutils;
import be.kuleuven.mgG.internal.view.EnrichmentAnalysisTaskFactory;
import be.kuleuven.mgG.internal.model.MGGManager;



//import static org.cytoscape.work.ServiceProperties.TOOL_BAR_GRAVITY;
//import static org.cytoscape.work.ServiceProperties.TOOLTIP;
//import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
//import static org.cytoscape.work.ServiceProperties.ID;
//import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
//import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
//import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
//import static org.cytoscape.work.ServiceProperties.LARGE_ICON_URL;
//
//
//
//import org.cytoscape.application.CyApplicationManager;
//import org.cytoscape.application.events.SetCurrentNetworkListener;
//import org.cytoscape.application.swing.AbstractCyAction;
//import org.cytoscape.application.swing.CySwingApplication;
//import org.cytoscape.application.swing.CytoPanel;
//import org.cytoscape.application.swing.CytoPanelComponent;
//import org.cytoscape.application.swing.CytoPanelName;
//import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
//import org.cytoscape.io.BasicCyFileFilter;
//import org.cytoscape.io.DataCategory;
//import org.cytoscape.io.read.InputStreamTaskFactory;
//import org.cytoscape.model.CyNetworkFactory;
//import org.cytoscape.model.CyNetworkManager;
//import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
//import org.cytoscape.model.events.NetworkAddedListener;
//import org.cytoscape.model.subnetwork.CyRootNetworkManager;
//import org.cytoscape.view.model.CyNetworkViewManager;
//import org.json.simple.JSONArray;
//import org.osgi.framework.ServiceReference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.cytoscape.session.events.SessionLoadedListener;
//
//import be.kuleuven.mgG.internal.tasks.CreateMGGVisualStyle;
//import be.kuleuven.mgG.internal.tasks.CreateNetworkTaskFactory;
//import be.kuleuven.mgG.internal.tasks.GetTermsFromNetworkEnrichmentTaskFactory;
//import be.kuleuven.mgG.internal.tasks.MCLClusterTaskFactory;
//import be.kuleuven.mgG.internal.view.JSONDisplayPanel;








public class CyActivator extends AbstractCyActivator {

    public CyActivator() {
        super();
    }

    public void start(BundleContext bc) {

    	final StreamUtil streamUtil = getService(bc, StreamUtil.class);
        final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

        final MGGManager MGGManager = new MGGManager(serviceRegistrar);

        //MGGManager.addSource(new FileSource(NGGManager));


        // Get services
        //CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
        // CySwingApplication swingApplication = getService(bc, CySwingApplication.class);

        //CyNetworkFactory networkFactory =getService(bc, CyNetworkFactory.class);
        // CyNetworkManager networkManager = getService(bc, CyNetworkManager.class);




        // Register taskfactories

        ImportFileTaskFactory mggImportFileTaskFactory = new ImportFileTaskFactory(MGGManager);
        Properties props = new Properties();
        props.setProperty(TITLE, "Import Abundance Data");
        props.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data");
        props.setProperty(IN_TOOL_BAR, "FALSE");
        props.setProperty(IN_MENU_BAR, "TRUE");
        props.setProperty(MENU_GRAVITY, "2");
        props.setProperty(COMMAND_NAMESPACE, "MGG");
        props.setProperty(COMMAND_DESCRIPTION, "Load abudance table(TSV/CSV)");
        props.setProperty(COMMAND, "Load_Abudance");

        registerService(bc, mggImportFileTaskFactory, TaskFactory.class, props);

        ImportTaxonomyTaskFactory mggImportTaxonomyTaskFactory = new ImportTaxonomyTaskFactory(MGGManager);
        Properties taxonomyprops = new Properties();
        taxonomyprops.setProperty(TITLE, "Import Taxonomy");
        taxonomyprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data");
        taxonomyprops.setProperty(IN_TOOL_BAR, "FALSE");
        taxonomyprops.setProperty(IN_MENU_BAR, "TRUE");
        taxonomyprops.setProperty(MENU_GRAVITY, "2.5");
        taxonomyprops.setProperty(COMMAND_NAMESPACE, "MGG");
        taxonomyprops.setProperty(COMMAND_DESCRIPTION, "Load the taxonomies of the nodes of a network (node, taxonomy)");
        taxonomyprops.setProperty(COMMAND, "Load_Taxonomy");

        registerService(bc, mggImportTaxonomyTaskFactory, TaskFactory.class, taxonomyprops);

        ImportMetadataTaskFactory mggImportMetaDataTaskFactory = new ImportMetadataTaskFactory(MGGManager);
        Properties metadataprops = new Properties();
        metadataprops.setProperty(TITLE, "Import Metadata");
        metadataprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data");
        metadataprops.setProperty(IN_TOOL_BAR, "FALSE");
        metadataprops.setProperty(IN_MENU_BAR, "TRUE");
        metadataprops.setProperty(MENU_GRAVITY, "3");
        metadataprops.setProperty(COMMAND_NAMESPACE, "MGG");
        metadataprops.setProperty(COMMAND_DESCRIPTION, "Load Metadata File");
        metadataprops.setProperty(COMMAND, "Load_MetaData");

        registerService(bc, mggImportMetaDataTaskFactory, TaskFactory.class, metadataprops);
        
        
        ImportNetworkDataTaskFactory mggImportNetWorkTaskFactory=new ImportNetworkDataTaskFactory(MGGManager);
        Properties networkprops=new Properties();
        networkprops.setProperty(TITLE, "Import Current Network");
        networkprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data");
        networkprops.setProperty(IN_TOOL_BAR, "FALSE");
        networkprops.setProperty(IN_MENU_BAR, "TRUE");
        networkprops.setProperty(MENU_GRAVITY, "4");
        networkprops.setProperty(COMMAND_NAMESPACE, "MGG");
        networkprops.setProperty(COMMAND_DESCRIPTION, "Load Network Data");
        networkprops.setProperty(COMMAND, "Load_Network");
        
        registerService(bc, mggImportNetWorkTaskFactory, TaskFactory.class, networkprops);
        
        CheckAbudanceFileTaskFactory mggCheckAbudanceFileTaskFactory = new CheckAbudanceFileTaskFactory(MGGManager);
        Properties checkdataprops = new Properties();
        checkdataprops.setProperty(TITLE, "Check Abudance Data");
        checkdataprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data.Check Data Files");
        checkdataprops.setProperty(IN_TOOL_BAR, "FALSE");
        checkdataprops.setProperty(IN_MENU_BAR, "TRUE");
        checkdataprops.setProperty(MENU_GRAVITY, "5");
        checkdataprops.setProperty(COMMAND_NAMESPACE, "MGG");
        checkdataprops.setProperty(COMMAND_DESCRIPTION, "Check abudance data File");
        checkdataprops.setProperty(COMMAND, "Check_Abudance_Data");

        registerService(bc, mggCheckAbudanceFileTaskFactory, TaskFactory.class, checkdataprops);
        
        
        
        CheckMetaDataFileTaskFactory mggCheckMetaDataFileTaskFactory = new CheckMetaDataFileTaskFactory(MGGManager);
        Properties checkMetaDataprops = new Properties();
        checkMetaDataprops.setProperty(TITLE, "Check Metadata");
        checkMetaDataprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data.Check Data Files");
        checkMetaDataprops.setProperty(IN_TOOL_BAR, "FALSE");
        checkMetaDataprops.setProperty(IN_MENU_BAR, "TRUE");
        checkMetaDataprops.setProperty(MENU_GRAVITY, "6");
        checkMetaDataprops.setProperty(COMMAND_NAMESPACE, "MGG");
        checkMetaDataprops.setProperty(COMMAND_DESCRIPTION, "Check metadata File");
        checkMetaDataprops.setProperty(COMMAND, "Check_MetaData");

        registerService(bc, mggCheckMetaDataFileTaskFactory, TaskFactory.class, checkMetaDataprops);
        
        CheckNetworkTaskFactory mggCheckNetworkTaskFactory = new CheckNetworkTaskFactory (MGGManager);
        Properties checkNetworkprops = new Properties();
        checkNetworkprops.setProperty(TITLE, "Check  Network Data");
        checkNetworkprops.setProperty(PREFERRED_MENU, "Apps.MGG.Import Data.Check Data Files");
        checkNetworkprops.setProperty(IN_TOOL_BAR, "FALSE");
        checkNetworkprops.setProperty(IN_MENU_BAR, "TRUE");
        checkNetworkprops.setProperty(MENU_GRAVITY, "7");
        checkNetworkprops.setProperty(COMMAND_NAMESPACE, "MGG");
        checkNetworkprops.setProperty(COMMAND_DESCRIPTION, "Check network data");
        checkNetworkprops.setProperty(COMMAND, "Check_Network_Data");

        registerService(bc, mggCheckNetworkTaskFactory , TaskFactory.class, checkNetworkprops);
        
        CreateMGGVisualStyleTaskFactory mggCreateMGGVisualStyleTaskFactory = new CreateMGGVisualStyleTaskFactory (MGGManager);
        Properties createMGGvisualprops = new Properties();
        createMGGvisualprops.setProperty(TITLE, "MGG visual style");
        createMGGvisualprops.setProperty(PREFERRED_MENU, "Apps.MGG.MGG visual style");
        createMGGvisualprops.setProperty(IN_TOOL_BAR, "FALSE");
        createMGGvisualprops.setProperty(IN_MENU_BAR, "TRUE");
        createMGGvisualprops.setProperty(MENU_GRAVITY, "8");
        createMGGvisualprops.setProperty(COMMAND_NAMESPACE, "MGG");
        createMGGvisualprops.setProperty(COMMAND_DESCRIPTION, "Create MGG visual style");
        createMGGvisualprops.setProperty(COMMAND, "MGG_visual_style");

        registerService(bc, mggCreateMGGVisualStyleTaskFactory , TaskFactory.class, createMGGvisualprops);
        
        

   
        
        
        // Register taskfactory

        SendDataToServerTaskFactory sendDataToServerTaskFactory = new SendDataToServerTaskFactory( MGGManager);
        Properties Sendprops = new Properties();
        Sendprops.setProperty(TITLE, "Get Annotated Network");
        Sendprops.setProperty(PREFERRED_MENU, "Apps.MGG");
        Sendprops.setProperty(IN_TOOL_BAR, "FALSE");
        Sendprops.setProperty(IN_MENU_BAR, "TRUE");
        Sendprops.setProperty(MENU_GRAVITY, "1");
        Sendprops.setProperty(COMMAND_NAMESPACE, "MGG");
        Sendprops.setProperty(COMMAND_DESCRIPTION, "Upload data to Microbetag Server and Get the Network");
        Sendprops.setProperty(COMMAND, "Get_Network");

        registerService(bc, sendDataToServerTaskFactory, TaskFactory.class, Sendprops);



        // The microbetag server to send the data to: the public one, or your own
        SetServerTaskFactory setServerTaskFactory = new SetServerTaskFactory();
        Properties serverprops = new Properties();
        serverprops.setProperty(TITLE, "Set microbetag server");
        serverprops.setProperty(PREFERRED_MENU, "Apps.MGG");
        serverprops.setProperty(IN_TOOL_BAR, "FALSE");
        serverprops.setProperty(IN_MENU_BAR, "TRUE");
        serverprops.setProperty(MENU_GRAVITY, "9");
        serverprops.setProperty(COMMAND_NAMESPACE, "MGG");
        serverprops.setProperty(COMMAND_DESCRIPTION, "Set the microbetag server to send the data to");
        serverprops.setProperty(COMMAND, "Set_Server");

        registerService(bc, setServerTaskFactory, TaskFactory.class, serverprops);


        AboutTaskFactory mggAboutTaskFactory = new AboutTaskFactory ();
        Properties aboutprops = new Properties();
        aboutprops.setProperty(TITLE, "About MGG");
        aboutprops.setProperty(PREFERRED_MENU, "Apps.MGG");
        aboutprops.setProperty(IN_TOOL_BAR, "FALSE");
        aboutprops.setProperty(IN_MENU_BAR, "TRUE");
        aboutprops.setProperty(MENU_GRAVITY, "10");
        aboutprops.setProperty(COMMAND_NAMESPACE, "MGG");
        aboutprops.setProperty(COMMAND_DESCRIPTION, "Information about MGG");
        aboutprops.setProperty(COMMAND, "About_MGG");

        registerService(bc, mggAboutTaskFactory, TaskFactory.class,   aboutprops);
        
        

        EnrichmentAnalysisTaskFactory mggEnrichmentTaskFactory = new EnrichmentAnalysisTaskFactory(MGGManager);
        Properties enrichmentprops = new Properties();
        enrichmentprops.setProperty(TITLE, "MGG Enrichment");
        enrichmentprops.setProperty(PREFERRED_MENU, "Apps.MGG Enrichment");
        enrichmentprops.setProperty(IN_TOOL_BAR, "FALSE");
        enrichmentprops.setProperty(IN_MENU_BAR, "TRUE");
        enrichmentprops.setProperty(MENU_GRAVITY, "11");
        enrichmentprops.setProperty(COMMAND_NAMESPACE, "MGG");
        enrichmentprops.setProperty(COMMAND_DESCRIPTION, "Enrichment Analysis");
        enrichmentprops.setProperty(COMMAND, "MGG_Enrichment");

        registerService(bc, mggEnrichmentTaskFactory, TaskFactory.class,   enrichmentprops);
        
        
//        MCLClusterTaskFactory mggMCLClusterTaskFactory = new MCLClusterTaskFactory (MGGManager);
//        Properties clusterprops = new Properties();
//        clusterprops.setProperty(TITLE, " Leiden Clustering(clusterMaker)");
//        clusterprops.setProperty(PREFERRED_MENU, "Apps.MGG Clustering");
//        clusterprops.setProperty(IN_TOOL_BAR, "FALSE");
//        clusterprops.setProperty(IN_MENU_BAR, "TRUE");
//        clusterprops.setProperty(MENU_GRAVITY, "11");
//        clusterprops.setProperty(COMMAND_NAMESPACE, "MGG");
//        clusterprops.setProperty(COMMAND_DESCRIPTION, "Leiden Clustering");
//        clusterprops.setProperty(COMMAND, "MGG_Clustering");
//
//        registerService(bc, mggMCLClusterTaskFactory, TaskFactory.class,   clusterprops);
        
        
        
        
        

       // CreateMGGVisualStyle createVisualStyleAction = new CreateMGGVisualStyle(MGGManager);

       // registerService(bc, createVisualStyleAction, CyAction.class, new Properties());

        {
            ShowResultsPanelAction sra = new ShowResultsPanelAction("Show results panel", MGGManager);
            registerService(bc, sra, CyAction.class);

            ShowResultsPanelTaskFactory showResults = new ShowResultsPanelTaskFactory(MGGManager);
            //showResults.reregister();
            MGGManager.setShowResultsPanelTaskFactory(showResults);

            // Now bring up the side panel if the current network is a MGG network
            CyNetwork current = MGGManager.getCurrentNetwork();
            if (Mutils.ifHaveMGG(current)) {
                // It's the current network.  Bring up the results panel
                MGGManager.execute(showResults.createTaskIterator(), true);
            }
        }









    }




}










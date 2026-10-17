package be.kuleuven.mgG.internal.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.utils.Mutils;
import be.kuleuven.mgG.internal.utils.SwingLink;
import be.kuleuven.mgG.internal.utils.ViewUtils;

import java.awt.event.ActionListener;
import javax.swing.JButton;

import be.kuleuven.mgG.internal.utils.LogUtils;

public class MGGNodePanel extends AbstractMggPanel {

	
    private JCheckBox highlightBox;
    private JButton showMspecies;
    private boolean updating = false;
    private JPanel nodesPanel = null;
    private Color defaultBackground;

    private JPanel AllTraitsPanel = null;
    
    private JButton showSingletons;
  
    private JCheckBox modeToggleCheckbox;
    
    public static String Hide_Singletons = "Hide Singletons";

  
    public MGGNodePanel(final MGGManager manager) {

        super(manager);
        filters.get(currentNetwork).put("phendbScore", new HashMap < > ());     

        init();
        revalidate();
        repaint();

    }


    public void updateControls() {

    	updating = true;

        showSingletons.setSelected(manager.showSingletons());
        highlightBox.setSelected(manager.highlightNeighbors());
        showMspecies.setSelected(manager.showMspecies());
          
        updating = false;
    }
    
    
    

    private void init() {
        setLayout(new GridBagLayout());

        EasyGBC c = new EasyGBC();

        
        // Neighbors, singletons, show species buttons
        JPanel controlPanel = createControlPanel();
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        add(controlPanel, c.anchor("west").down().noExpand());

        
        JPanel mainPanel = new JPanel(); {
        
        	mainPanel.setLayout(new GridBagLayout());
            
        	mainPanel.setBackground(defaultBackground);
            
        	EasyGBC d = new EasyGBC();
 
            mainPanel.add(createAllTraitsPanel(), d.down().anchor("west").expandHoriz());
            
            mainPanel.add(createNodesPanel(), d.down().anchor("west").expandHoriz());

            // Haris: I think this is a hack so the panels are sticked on the upper part of the main panel 
            mainPanel.add(new JLabel(""), d.down().anchor("west").expandBoth());

        }
        JScrollPane scrollPane = new JScrollPane(
        		mainPanel, 
        		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
        		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(scrollPane, c.down().anchor("west").expandBoth());
    }





    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        EasyGBC d = new EasyGBC();
        controlPanel.setLayout(new GridBagLayout());

        EasyGBC upperGBC = new EasyGBC();
        JPanel upperPanel = new JPanel(new GridBagLayout());

        
        
        {
			highlightBox = new JCheckBox("Highlight first neighbors");
			highlightBox.setFont(labelFont);
			highlightBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							manager.setHighlightNeighbors(true);
							doHighlight(manager.getCurrentNetworkView());
						} else {
							manager.setHighlightNeighbors(false);
							clearHighlight(manager.getCurrentNetworkView());
						}
					}
			});
			 upperPanel.add(highlightBox,upperGBC.anchor("northwest").noExpand() );
        }
        
        {
	        showSingletons = new JButton(Hide_Singletons);
	        showSingletons.setToolTipText("Press to hide nodes that have no edges");
	        showSingletons.setFont(labelFont);
	        showSingletons.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                if (updating) return;
	                boolean isShowingSingletons = manager.showSingletons();
	                manager.setShowSingletons(!isShowingSingletons);
	                Mutils.hideSingletons(manager.getCurrentNetworkView(),!isShowingSingletons);
	                showSingletons.setText(isShowingSingletons ?  Hide_Singletons : Hide_Singletons);
	            }
	        });	        
	        upperPanel.add(showSingletons, upperGBC.right().insets(0,10,0,0).noExpand());	       
	        }
        
        upperPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        controlPanel.add(upperPanel, d.anchor("northwest").expandHoriz());
        
        
        JPanel lowerPanel = new JPanel();
		GridLayout layout2 = new GridLayout(2,2);
		layout2.setVgap(0);
		lowerPanel.setLayout(layout2);
		
		
		{
		showMspecies = new JButton("Show Species");
		showMspecies.setToolTipText(
				"Press to show only nodes that have been mapped (or correspond to a custom) genome, thus annotated using it."
		);
		showMspecies.setFont(labelFont);
		showMspecies.addActionListener(new ActionListener() {
		    private boolean isShown = false; // track current state	   
		    @Override
		    public void actionPerformed(ActionEvent e) {
//		        
		    	 if (updating) return; // Check if update is in progress and exit if it is

		         // Check if any other checkboxes are selected
		         boolean anyCheckboxSelected = isAnyCheckboxSelected();

		         if (!anyCheckboxSelected) {
		             // Toggle the state and update manager only if no checkboxes are selected
		             isShown = !isShown;
		             manager.setShowMspecies(isShown); 
		             Mutils.doShowMspecies(manager.getCurrentNetworkView(), isShown, manager.showSingletons(), anyCheckboxSelected);

		             // Update the button text based on the current state
		             showMspecies.setText(isShown ? "Show all nodes" : "Show Species");
		    }
		        }
		});

		lowerPanel.add(showMspecies);
		}
				
    	controlPanel.add(lowerPanel, d.down().anchor("west").expandHoriz());
    	
        updateControls();       
        controlPanel.setMaximumSize(new Dimension(300, 100));
        controlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return controlPanel;
    }



    private void doHighlight(CyNetworkView networkView) {

        if (networkView != null) {
            List < CyNode > nodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, Boolean.TRUE);
            if (nodes == null || nodes.size() == 0) {
                return;
            }

            Mutils.clearHighlight(manager, networkView);
            Mutils.highlight(manager, networkView, nodes);
        }
    }

    
    private void clearHighlight(CyNetworkView networkView) {
        Mutils.clearHighlight(manager, networkView);
    }


// -----------------------

//  Phenotrex and FAPROTAX traits 
    
// -----------------------
    
    private JPanel createNodesPanel() {

    	LogUtils.info("START BUILDING THE NODES PANEL");

    	nodesPanel = new JPanel();
        nodesPanel.setLayout(new GridBagLayout());
        EasyGBC c = new EasyGBC();

        if (currentNetwork != null) {
            List < CyNode > nodes = CyTableUtil.getNodesInState(currentNetwork, CyNetwork.SELECTED, true);
            for (CyNode node: nodes) {

            	JPanel newPanel = createNodePanel(node);
                
            	newPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                nodesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
            }
        }

        nodesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        CollapsablePanel collapsablePanel = new CollapsablePanel(
        		iconFont, 
        		"Selected nodes", 
        		nodesPanel, 
        		true, 
        		12
        );

        collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
        // This makes the collapsed panel to expand; 
        // setting collapsed as true when firing the Panel is necessary to have the option of toggling
        collapsablePanel.toggleSelection();   
        
        return collapsablePanel;
    }




    private void updateNodesPanel() {

    	if (nodesPanel == null) return;
        nodesPanel.removeAll();
        EasyGBC c = new EasyGBC();

        List < CyNode > nodes = CyTableUtil.getNodesInState(currentNetwork, CyNetwork.SELECTED, true);

        if (nodes.size() > 50) {
            return;
        }
        for (CyNode node: nodes) {
            JPanel newPanel = createNodePanel(node);
            newPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            nodesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
        }
        return;
    }


    private JPanel createNodePanel(CyNode node) {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // components use full horizontal space

        // Set constraints
        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Start from row 0
        gbc.ipady = 10;  // Like in the edge case, sets the height of a row in the table with the node's credentials, in pixels.
        gbc.anchor = GridBagConstraints.WEST; // Left-align 
        gbc.insets = new Insets(5, 5, 5, 5); // 5pixel marg


        CyNetwork currentNetwork = manager.getCurrentNetwork();
        if (currentNetwork == null) return panel;


        CyTable nodeTable = currentNetwork.getDefaultNodeTable();


        Object taxonValue = (nodeTable.getColumn("microbetag::taxon") != null) ? 
        		nodeTable.getRow(node.getSUID())
        		.get("microbetag::taxon", nodeTable.getColumn("microbetag::taxon").getType()) : null;

        JTextPane taxonPane = ViewUtils.createStyledLabelEdges(
        		"Taxon Name: " + (taxonValue != null ? taxonValue.toString() : "null")
        );
        panel.add(taxonPane, gbc);
        gbc.gridy++;

        
        
        Object idValue = (nodeTable.getColumn("name") != null) ? 
        		nodeTable.getRow(node.getSUID())
        		.get("name", nodeTable.getColumn("name").getType()) : null;
        JTextPane idPane = ViewUtils.createStyledLabelEdges(
        		"Sequence ID: " + (idValue != null ? idValue.toString() : "null")
        );
        panel.add(idPane, gbc);
        gbc.gridy++;
        
        
        Object taxonomyValue = (nodeTable.getColumn("microbetag::taxonomy") != null) ? 
        		nodeTable.getRow(node.getSUID())
        		.get("microbetag::taxonomy", nodeTable.getColumn("microbetag::taxonomy").getType()) : null;

       
        JTextPane taxonomyPane = ViewUtils.createStyledLabelEdges("Taxonomy:");
        JTextArea taxonomyArea = new JTextArea( 
        		(taxonomyValue != null ? taxonomyValue.toString() : "null")
        );
		ViewUtils.setJTextAreaAttributes(taxonomyArea);
        panel.add(taxonomyPane, gbc);
        gbc.gridy++;
        panel.add(taxonomyArea, gbc);
        gbc.gridy++;


        String[] attributes = {
        	    "microbetag::gtdb-genomes",
        	    "microbetag::ncbi-tax-id",
        	    "microbetag::ncbi-tax-level"
        	};

    	for (String attribute : attributes) {
    	    if (nodeTable.getColumn(attribute) != null) {
    	        Object attrValue = nodeTable.getRow(node.getSUID()).get(attribute, nodeTable.getColumn(attribute).getType());
    	        String attributeName = attribute.split("::")[1];

    	        if (attribute.equals("microbetag::ncbi-tax-id") && attrValue != null) {
    	            // Parse values as lists
    	            List<String> taxIds = new ArrayList<>();
    	            List<String> taxLevels = new ArrayList<>();
    	            List<String> genomeGroups = new ArrayList<>();

    	            Object taxIdObj = nodeTable.getRow(node.getSUID()).get("microbetag::ncbi-tax-id", Object.class);
    	            if (taxIdObj instanceof List<?>) {
    	                for (Object id : (List<?>) taxIdObj) taxIds.add(id.toString());
    	            }

    	            Object taxLevelObj = nodeTable.getRow(node.getSUID()).get("microbetag::ncbi-tax-level", Object.class);
    	            if (taxLevelObj instanceof List<?>) {
    	                for (Object level : (List<?>) taxLevelObj) taxLevels.add(level.toString());
    	            }

    	            Object genomeGroupObj = nodeTable.getRow(node.getSUID()).get("microbetag::gtdb-genomes", Object.class);
    	            if (genomeGroupObj instanceof List<?>) {
    	                for (Object genome : (List<?>) genomeGroupObj) genomeGroups.add(genome.toString());
    	            }

    	            // Genomes that are not GTDB accessions are of a server's own catalogue: listed apart, without a GTDB link
    	            List<String> localGenomes = new ArrayList<>();

    	            // Parse through the NCBI data     	            
    	            for (int i = 0; i < taxIds.size(); i++) {

    	            	String taxId = taxIds.get(i);
    	                String taxLevel = (i < taxLevels.size()) ? taxLevels.get(i) : "unknown";
    	                
    	                String genomesRaw = (i < genomeGroups.size()) ? genomeGroups.get(i) : "";

    	                JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    	                subPanel.setOpaque(false);

    	                JLabel idLabel = new JLabel("NCBI Tax ID | rank | GTDB: ");
    	                idLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    	                subPanel.add(idLabel);

    	                if (!isMissing(taxId)) {
    	                    String url = "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=" + taxId;
    	                    SwingLink link = new SwingLink(taxId, url, openBrowser);
    	                    subPanel.add(link);
    	                } else {
    	                    JLabel naLabel = new JLabel(taxId);
    	                    naLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    	                    subPanel.add(naLabel);
    	                }

    	                JLabel levelLabel = new JLabel(taxLevel);
    	                levelLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
    	                subPanel.add(levelLabel);

    	                if (!genomesRaw.equals("")) {
    	                    String[] genomeIds = genomesRaw.split("[,;]");
    	                    for (String genomeId : genomeIds) {
    	                        genomeId = genomeId.trim();
    	                        if (!isMissing(genomeId) && !isGtdbAccession(genomeId)) {
    	                            if (!localGenomes.contains(genomeId)) localGenomes.add(genomeId);
    	                        } else if (!isMissing(genomeId)) {
    	                            String genomeUrl = "https://gtdb.ecogenomic.org/genome?gid=" + genomeId;
    	                            SwingLink genomeLink = new SwingLink(genomeId, genomeUrl, openBrowser);
    	                            subPanel.add(genomeLink);
    	                        }
    	                    }
    	                }

    	                gbc.gridwidth = GridBagConstraints.REMAINDER;
    	                panel.add(subPanel, gbc);
    	                gbc.gridy++;
    	                gbc.gridwidth = 1;
    	            }

    	            if (!localGenomes.isEmpty()) {
    	                JPanel genomesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    	                genomesPanel.setOpaque(false);
    	                JLabel genomesLabel = new JLabel("Genomes: " + String.join(", ", localGenomes));
    	                genomesLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    	                genomesPanel.add(genomesLabel);
    	                gbc.gridwidth = GridBagConstraints.REMAINDER;
    	                panel.add(genomesPanel, gbc);
    	                gbc.gridy++;
    	                gbc.gridwidth = 1;
    	            }

    	        } else if (!attribute.equals("microbetag::gtdb-genomes") && !attribute.equals("microbetag::ncbi-tax-level")) {
    	            // Display other attributes normally
    	            JTextArea attributeArea = new JTextArea(attributeName + ": " + (attrValue != null ? attrValue.toString() : "null"));
    	            ViewUtils.setJTextAreaAttributes(attributeArea);
    	            gbc.gridwidth = 2;
    	            panel.add(attributeArea, gbc);
    	            gbc.gridy++;
    	            gbc.gridwidth = 1;
    	        }
    	    }
    	}


        // ----------------------For the nested phenDB CollapsablePanel------------------


        JPanel phenDBPanel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; //    cells uneditable
            }
        };

        model.addColumn("Feature");
        model.addColumn("Present");
        model.addColumn("Score");

        //  Instantiate table with the model
        JTable table = new JTable(model);

        // Adjust column widths
        TableColumnModel columnModel = table.getColumnModel();

        // Modify the widths 
        columnModel.getColumn(0).setPreferredWidth(5);
        columnModel.getColumn(1).setPreferredWidth(5);
        columnModel.getColumn(2).setPreferredWidth(5);

        
        boolean hasTruePresentValues = false; //flag for loop 

        for (CyColumn column: nodeTable.getColumns()) {
            String columnName = column.getName();

            // Check if column starts with "phendb" and NOT "phendbScore" 
            if (columnName.startsWith("phendb::") && !columnName.contains("phendbScore::")) {
            	
                // Extract  name
                String feature = columnName.replace("phendb::", "");

                // Check corresponding "Score" column exists under "phendbScore::" 
                CyColumn scoreColumn = nodeTable.getColumn("phendbScore::" + feature);
                
                if (scoreColumn != null) {
                	                	
                    Object presentObj = nodeTable.getRow(node.getSUID()).get(columnName, column.getType());
                    String presentValue = (presentObj == null) ? "null" : presentObj.toString();
                    
                    //  Phen trait from microbetag comes binary (True/False) 
                    if ("true".equalsIgnoreCase(presentValue)) {
                    
                    //this if added to choose to show only the true in present
                    	 
                    Object scoreObj = nodeTable.getRow(node.getSUID()).get(scoreColumn.getName(), scoreColumn.getType());
                    String scoreValue = (scoreObj == null) ? "null" : scoreObj.toString();
                    hasTruePresentValues = true; // Update the flag
                    
                    try {
                        double scoreAsDouble = Double.parseDouble(scoreValue);
                        scoreValue = String.format("%.2f", scoreAsDouble);
                    } catch (NumberFormatException e) {

                    }

                    model.addRow(new Object[] {
                        feature,
                        presentValue,
                        scoreValue
                    
                     });
                }
            }
        }
        }
        
        
        // Adjust column widths based on content
        for (int col = 0; col < table.getColumnCount(); col++) {
            int maxWidth = 0;
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, col);
                Component comp = table.prepareRenderer(renderer, row, col);
                maxWidth = Math.max(comp.getPreferredSize().width + 1, maxWidth);
            }
            if (maxWidth > 5) { // Ensure a minimum width
                table.getColumnModel().getColumn(col).setPreferredWidth(maxWidth);
            }
        }

        // Add table to a scroll pane:
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Set scroll pane size based on table size
        Dimension tableSize = table.getPreferredSize();
        tableScrollPane.setPreferredSize(
        		new Dimension(panel.getWidth() - 20, 
        				Math.min(tableSize.height + table.getTableHeader().getPreferredSize().height + 20, 200)
        		)
        );
        phenDBPanel.add(tableScrollPane, BorderLayout.CENTER);
        

        phenDBPanel.revalidate();
        phenDBPanel.repaint();

        //  Wrap the phenDBPanel inside a CollapsablePanel

        	Border etchedBorder = BorderFactory.createEtchedBorder();
        	Border emptyBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0);

    	// Add the phenDBCollapsablePanel to the main panel:

        if (hasTruePresentValues) {

        	CollapsablePanel phenDBCollapsablePanel = new CollapsablePanel(
        			iconFont, 
        			"phenDB attributes", 
        			phenDBPanel, 
        			true, 
        			12
        	);
            phenDBCollapsablePanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder, etchedBorder));
            phenDBCollapsablePanel.setToolTipText(
             		"For cases where a node was mapped to several genomes, microbetag returns the intersection of traits with the same prediction and the score is their average."
             );
            phenDBCollapsablePanel.toggleSelection();

            // Add the CollapsablePanel to the main panel
            panel.add(phenDBCollapsablePanel, gbc);
            gbc.gridy++;
        }

        
        
        //---------------------------panel for faprotax--------------------------

        JPanel faprotaxPanel = new JPanel(new GridBagLayout());
        GridBagConstraints fapGBC = new GridBagConstraints();
        fapGBC.fill = GridBagConstraints.HORIZONTAL;
        fapGBC.weightx = 1.0;
        fapGBC.gridx = 0;
        fapGBC.gridy = 0;
        fapGBC.anchor = GridBagConstraints.WEST;
        fapGBC.insets = new Insets(5, 5, 5, 5);
        

        boolean hasFaprotaxTerms = false;

        // Loop throughnodeTable columns for starting faprotax::
        for (CyColumn column: nodeTable.getColumns()) {
            String columnName = column.getName();
            if (columnName.startsWith("faprotax::")) {
                Object attrValue = nodeTable.getRow(node.getSUID()).get(columnName, column.getType());
                if (attrValue != null && attrValue instanceof Boolean) {

                    String displayName = columnName.split("::")[1];

                    hasFaprotaxTerms = true;
                    JTextArea label = new JTextArea(displayName);
                    ViewUtils.setJTextAreaAttributes(label);
                    faprotaxPanel.add(label, fapGBC);
                     
                    fapGBC.gridy++;
                }
            }
        }
        if (hasFaprotaxTerms) {
            CollapsablePanel faprotaxCollapsablePanel = new CollapsablePanel(
            		iconFont, 
            		"Faprotax Attributes", 
            		faprotaxPanel, 
            		true, 
            		12
            );
            faprotaxCollapsablePanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder, etchedBorder));
            faprotaxCollapsablePanel.toggleSelection();

            // Add the CollapsablePanel to the main panel
            panel.add(faprotaxCollapsablePanel, gbc);
            gbc.gridy++;
        }

        String nodeId = (idValue != null) ? idValue.toString() : "Selected Nodes";

        CollapsablePanel collapsablePanel = new CollapsablePanel(
        		iconFont, 
        		nodeId, 
        		panel, 
        		true, 
        		12
        );

        collapsablePanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder, etchedBorder));
        collapsablePanel.toggleSelection();
        return collapsablePanel;
        	
    }

   


// ------------------
//  All traits panel    
// ------------------

    
    private JPanel createAllTraitsPanel() {

    	AllTraitsPanel = new JPanel();
        AllTraitsPanel.setLayout(new BoxLayout(AllTraitsPanel, BoxLayout.Y_AXIS));
        
     // Create a new font with a larger size for category labels  
        Font largerFont = new Font(labelFont.getName(), labelFont.getStyle(), labelFont.getSize() + 1); 
        
        JButton unselectAllButton = new JButton("Unselect All");
        unselectAllButton.setFont(labelFont);
        unselectAllButton.setToolTipText("Press to unselect all attributes");
        unselectAllButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Bigger border
        unselectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Component comp : AllTraitsPanel.getComponents()) {
                    if (comp instanceof JCheckBox) {
                        ((JCheckBox) comp).setSelected(false);
                    }
                }
                
                filterNodesByPhendbAttribute();
            }
        });
        AllTraitsPanel.add(unselectAllButton);
        
        // Add space between button and toggle
        AllTraitsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10-pi height space
        
        	// Mode toggle checkbox
        	modeToggleCheckbox = new JCheckBox("Toggle -AND- Mode");
        	modeToggleCheckbox.setToolTipText("Check to have AND connections between attributes,"
        		+ "uncheck to have -OR- connections between attributes");
        	//modeToggleCheckbox.setFont(labelFont);     
        	modeToggleCheckbox.setFont(labelFont);
        	modeToggleCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                toggleFilterMode();
            	}
        	});       
        AllTraitsPanel.add(modeToggleCheckbox);
        
        // Add space between Toggle and attributes
        AllTraitsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10-pi height space

        // Get the list of attributes with namespaces phendb and faprotax
        List<String> phendbAttributeList = Mutils.getPhendbAttributes(currentNetwork);
        List<String> faprotaxAttributeList = Mutils.getFaprotaxAttributes(currentNetwork);

        
        // Create a map of categories to their corresponding attributes
        Map<String, List<String>> categoryToAttributesMap = new HashMap<>();
        for (String category : Mutils.getCategories()) {
            categoryToAttributesMap.put(category, new ArrayList<>());
        }

        // Add attributes to their corresponding category
        populateCategoryToAttributesMap(phendbAttributeList, categoryToAttributesMap, Mutils.PhenDb_NAMESPACE + "::");
        populateCategoryToAttributesMap(faprotaxAttributeList, categoryToAttributesMap, Mutils.Faprotax_NAMESPACE + "::");

        // Add checkboxes under category labels
        for (String category : Mutils.getCategories()) {
            //old part
            List<String> attributes = categoryToAttributesMap.get(category);
            if (!attributes.isEmpty()) {
                JLabel categoryLabel = new JLabel(category);
               
                categoryLabel.setFont(largerFont);
                AllTraitsPanel.add(categoryLabel);
                
                for (String attribute : attributes) {
                    String namespace = attribute.split("::")[0] + "::";
                    String attributeName = attribute.split("::")[1];
                    AllTraitsPanel.add(createCheckbox(attributeName, namespace));
                }
            }
        }
        

        CollapsablePanel collapsablePanel = new CollapsablePanel(
        		iconFont, 
        		"PhenDb/Faprotax Filters", 
        		AllTraitsPanel, 
        		true, 
        		12
        );
        collapsablePanel.setToolTipText("Show nodes that have Phendb and Faprotax attributes");
        collapsablePanel.setBorder(BorderFactory.createEtchedBorder());

        return collapsablePanel;
    }

    
    private void populateCategoryToAttributesMap(List<String> attributeList, Map<String, List<String>> categoryToAttributesMap, String namespace) {
        for (String attribute : attributeList) {
            String category = Mutils.getCategoryForAttribute(attribute);
            categoryToAttributesMap.computeIfAbsent(category, k -> new ArrayList<>());
            categoryToAttributesMap.get(category).add(namespace + attribute);
        }
    }

    
    private JCheckBox createCheckbox(String attributeName, String namespace) {
        JCheckBox checkBox = new JCheckBox(attributeName);
        checkBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                filterNodesByPhendbAttribute();
            }
        });
        checkBox.setActionCommand(namespace + attributeName);
        return checkBox;
    }

   
    private void updateAllTraitsPanel() { 	
    	
    	  if (AllTraitsPanel == null) return;
    	  AllTraitsPanel.removeAll();
    	    
    	  Font largerFont = new Font(labelFont.getName(), labelFont.getStyle(), labelFont.getSize() + 1);
    	    
    	    	JButton unselectAllButton = new JButton("Unselect All");
    	    	unselectAllButton.setFont(labelFont);
    	    	unselectAllButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Bigger border
    	    	unselectAllButton.addActionListener(new ActionListener() {
    	    		@Override
    	    		public void actionPerformed(ActionEvent e) {
    	    			for (Component comp : AllTraitsPanel.getComponents()) {
    	    				if (comp instanceof JCheckBox) {
    	    					((JCheckBox) comp).setSelected(false);
    	    				}
    	    			}
    	    			
    	    			filterNodesByPhendbAttribute();
    	    		}
    	    	});
            
            AllTraitsPanel.add(unselectAllButton);
            
            // Add space between buttons
            AllTraitsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10-pi height space
    	    
    	    // Mode toggle checkbox
            modeToggleCheckbox = new JCheckBox("Toggle -AND- Mode");
            modeToggleCheckbox.setToolTipText("Check to have AND connections between attributes,"
            		+ "uncheck to have -OR- connections between attributes");
    	    
            modeToggleCheckbox.setFont(labelFont);
    	    modeToggleCheckbox.addItemListener(new ItemListener() {
    	        public void itemStateChanged(ItemEvent e) {
    	            toggleFilterMode();
    	        }
    	    });
    	    AllTraitsPanel.add(modeToggleCheckbox);
    	    
    	    // Add space between buttons
            AllTraitsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10-pi height space
        
    	 // Get the list of attributes from phendb and faprotax
            List<String> phendbAttributeList = Mutils.getPhendbAttributes(currentNetwork);
            List<String> faprotaxAttributeList = Mutils.getFaprotaxAttributes(currentNetwork);
        
         // Create a map of categories to their corresponding attributes
            Map<String, List<String>> categoryToAttributesMap = new HashMap<>();
            for (String category : Mutils.getCategories()) {
                categoryToAttributesMap.put(category, new ArrayList<>());
            }

            // Add attributes to their corresponding category
            populateCategoryToAttributesMap(phendbAttributeList, categoryToAttributesMap, "phendb::");
            populateCategoryToAttributesMap(faprotaxAttributeList, categoryToAttributesMap, "faprotax::");

            // Add checkboxes under category labels
            for (String category : Mutils.getCategories()) {
            	
            
            	
                List<String> attributes = categoryToAttributesMap.get(category);
                if (!attributes.isEmpty()) {
                    JLabel categoryLabel = new JLabel(category);
                    categoryLabel.setFont(largerFont);
                    AllTraitsPanel.add(categoryLabel);
                    
                    for (String attribute : attributes) {
                        String namespace = attribute.split("::")[0] + "::";
                        String attributeName = attribute.split("::")[1];
                        AllTraitsPanel.add(createCheckbox(attributeName, namespace));
                       
                    } 
            }
       }
   return; }
            
    
    private void filterNodesByPhendbAttribute() {
    	
    	  CyNetworkView view = manager.getCurrentNetworkView();
    	    CyNetwork net = view.getModel();
    	    
    	    boolean isAndMode = modeToggleCheckbox.isSelected();
    	    boolean anyCheckboxSelected = isAnyCheckboxSelected();
    	    boolean showbooleanMspecies = manager.showMspecies();
    	    boolean showbooleanSingletons = manager.showSingletons();
    	    
    	    
    	 // Check if any phendb filter is selected
    	    boolean isAnyPhendbFilterSelected = isAnyCheckboxSelected();
    	    
    	 // Enable the showMspecies button if no phendb filter is selected,
    	    
    	    showMspecies.setEnabled(!isAnyPhendbFilterSelected);
    	    showSingletons.setEnabled(!isAnyPhendbFilterSelected);
    	    
    	    for (CyNode node : net.getNodeList()) {
    	        View<CyNode> nodeView = view.getNodeView(node);
    	        if (nodeView == null) continue;

    	        if (!anyCheckboxSelected) {
    	            if (showbooleanMspecies) {
    	                // If showMspecies is true, apply the doShowMspecies logic
    	                Mutils.doShowMspecies(view,  manager.showMspecies(), showbooleanSingletons,false);
    	            } else if (showbooleanSingletons) {
    	                // If showMspecies is false and showSingletons is true, hide singletons
    	            	  nodeView.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
    	                Mutils.hideSingletons(view, showbooleanSingletons);
    	            } else {
    	                // If neither showMspecies nor showSingletons is selected, make all nodes visible
    	                nodeView.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
    	            }
    	        } else {
    	            // Update visibility based on selected mode and checkboxes
    	            boolean isVisible = isAndMode ? checkNodeVisibilityForAnd(node, net) : checkNodeVisibilityForOr(node, net);
    	            nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, isVisible);
    	        }
    	    }
    	}
//    	    
//    	    for (CyNode node : net.getNodeList()) {
//    	        View<CyNode> nodeView = view.getNodeView(node);
//    	        if (nodeView == null) continue;
//
//    	        if (!anyCheckboxSelected) {
//    	        	
//    	            if (showMspecies) {
//    	                // If showMspecies is true, apply the doShowMspecies logic
//    	                Mutils.doShowMspecies(view, true, showSingletons); 
//    	              }else  {
//    	                	nodeView.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
//    	            }
//    	        } else {
//    	            // Update visibility based on selected mode and checkboxes
//    	            boolean isVisible = isAndMode ? checkNodeVisibilityForAnd(node, net) : checkNodeVisibilityForOr(node, net);
//    	            nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, isVisible);
//    	        }
//    	    }
//    	}
//    	        
//    
    	
    private void toggleFilterMode() {
       
        filterNodesByPhendbAttribute();
    }
          
    
    public boolean isAnyCheckboxSelected() {
        for (Component comp : AllTraitsPanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) comp;
                if (checkBox.isSelected()) {
                    return true;                   
                }
            }
        }
        return false;
    }
    
    
    private boolean checkNodeVisibilityForOr(CyNode node, CyNetwork net) {
    	for (Component comp : AllTraitsPanel.getComponents()) {
            if (comp instanceof JLabel) {
                continue; // Skip labels
            }
            if (comp instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) comp;
                if (checkBox.isSelected()) {
                    String attributeFullName = checkBox.getActionCommand();
                    String[] parts = attributeFullName.split("::");
                    if (parts.length != 2) continue;

                    String namespace = parts[0] + "::";
                    String attributeName = parts[1];

                    CyRow nodeRow = net.getRow(node);
                    Boolean attributeValue = nodeRow.get(namespace + attributeName, Boolean.class);
                    // Node is visible if any of the selected checkboxes match
                    if (attributeValue != null && attributeValue) {
                        return true;
                    }
                }
            }
        }
        return false; // Node is not visible if none of the selected checkboxes match
    }
    

    
    private boolean checkNodeVisibilityForAnd(CyNode node, CyNetwork net) {
    	for (Component comp : AllTraitsPanel.getComponents()) {
            if (comp instanceof JCheckBox && comp != modeToggleCheckbox) {
                JCheckBox checkBox = (JCheckBox) comp;
                if (checkBox.isSelected()) {
                    String attributeFullName = checkBox.getActionCommand();
                    String[] parts = attributeFullName.split("::");
                    if (parts.length != 2) continue;

                    String namespace = parts[0] + "::";
                    String attributeName = parts[1];

                    CyRow nodeRow = net.getRow(node);
                    Boolean attributeValue = nodeRow.get(namespace + attributeName, Boolean.class);
                    // Node is not visible if any of the selected checkboxes do not match
                    if (attributeValue == null || !attributeValue) {
                        return false;
                    }
                }
            }
        }
        return true; // Node is visible if all selected checkboxes match
    }
    
    
 
   
    //---------------------------------------------------------------------------------



    public void networkChanged(CyNetwork newNetwork) {
        this.currentNetwork = newNetwork;
        if (currentNetwork == null) {
            // Hide results panel?
           // if (PhendbScPanel != null)
             //   PhendbScPanel.removeAll();

            if (AllTraitsPanel != null)
                AllTraitsPanel.removeAll();
            return;

        }
        
 
        //  get the view for the new network since we haven't actually switched yet
        CyNetworkView networkView = Mutils.getNetworkView(manager, currentNetwork);
        if (networkView != null) {
            if (manager.highlightNeighbors()) {
                doHighlight(networkView);
            } else {
                clearHighlight(networkView);
            }
            
            if (manager.showSingletons()) {
    			Mutils.hideSingletons(networkView, true);
    		} else {
    			Mutils.hideSingletons(networkView, false);
    		}
            
            if (manager.showMspecies() ) {
            	Mutils.doShowMspecies(networkView, true, updating,true);
            }else {
            	Mutils.doShowMspecies(networkView, false, updating,true);
            }
               
        }
        
        updateNodesPanel();
        updateAllTraitsPanel();
        
    }









    //------------------------- Filter logic--------------------------------
    
    
   
    
    public void selectedNodes(Collection<CyNode> nodes) {
        // Clear the nodes panel
        nodesPanel.removeAll();
        EasyGBC c = new EasyGBC();
        Mutils.clearHighlight(manager, manager.getCurrentNetworkView());

        for (CyNode node: nodes) {
            JPanel newPanel = createNodePanel(node);

            
            newPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            nodesPanel.add(newPanel, c.anchor("west").down().expandHoriz());

            
        }

        if (manager.highlightNeighbors()) {
            doHighlight(manager.getCurrentNetworkView());
        } else {
            clearHighlight(manager.getCurrentNetworkView());
        }
        
        revalidate();
        repaint();
    }
    
    


    @Override
    void doFilter(String type) {}


    @Override
    void undoFilters() {
        CyNetworkView view = manager.getCurrentNetworkView();
        if (view != null) {
            for (View < CyNode > node: view.getNodeViews()) {
                node.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
            }
        }

    }

    @Override
    double initFilter(String type, String label) {
    	return 0;
    }


	@Override
	double initFilterSeed(String type, String text) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	@Override
	String initSign(String type, String text) {
		return "<";
	}
	

    /** A GTDB genome accession, e.g. GCA_000005845.2 or RS_GCF_000005845.2 */
    private static boolean isGtdbAccession(String genomeId) {
        return genomeId.matches("((RS|GB)_)?GC[AF]_\\d+(\\.\\d+)?");
    }

    private static boolean isMissing(String value) {
        return value == null || value.isEmpty() || value.equals("<NA>") || value.equals("NA") || value.equals("-");
    }
}

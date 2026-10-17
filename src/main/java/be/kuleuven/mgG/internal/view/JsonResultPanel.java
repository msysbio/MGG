package be.kuleuven.mgG.internal.view;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.tasks.SendDataToServerTaskFactory;
import be.kuleuven.mgG.internal.tasks.ShowResultsPanelTaskFactory;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class JsonResultPanel extends JPanel implements CytoPanelComponent {

	private JTable table;
    final MGGManager manager;
    private boolean registered = false;
    
    public JsonResultPanel(final MGGManager manager,JSONObject jsonObject) {

    	super(new BorderLayout());
        
        
        CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
        CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
        
        if (!registered) {
			manager.registerService(this, CytoPanelComponent.class, new Properties());
			registered = true;
		}
		if (cytoPanel.getState() == CytoPanelState.HIDE)
			cytoPanel.setState(CytoPanelState.DOCK);
        
        // Extract the JSONArray from the JSONObject
        JSONArray jsonArray = (JSONArray) jsonObject.get("data");
        
        createTable(jsonArray);
        
        JScrollPane scrollPane = new JScrollPane(table);
      
        this.manager = manager;
	
        // scroll bar policies
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        //  preferred size of the scroll pane
        scrollPane.setPreferredSize(new Dimension(800, 600));
        
        // Add the scroll pane to the center of the JSONDisplayPanel
        add(scrollPane, BorderLayout.CENTER);
        
        
        //button for SendDataToServerTask 
        JButton sendButton = new JButton("Get Annotated Network ");
        sendButton.addActionListener(new ActionListener() {  
            public void actionPerformed(ActionEvent e) {
              
            	 SendDataToServerTaskFactory sendFactory = new SendDataToServerTaskFactory(manager);
            	 if (!sendFactory.isReady()) {
            		 javax.swing.JOptionPane.showMessageDialog(JsonResultPanel.this,
            				 "The microbetag server set (Apps > MGG > Set microbetag server) needs your network too:\n"
            				 + "import it with Apps > MGG > Import Data > Import Current Network.",
            				 "MGG", javax.swing.JOptionPane.WARNING_MESSAGE);
            		 return;
            	 }
            	 TaskIterator taskIterator = sendFactory.createTaskIterator();
                 manager.executeTasks(taskIterator);
                 
                 //added this to start the factory for showResultsPaneltaskFactory
                 TaskIterator taskIterator1 = new ShowResultsPanelTaskFactory( manager).createTaskIterator();
                 manager.executeTasks(taskIterator1);
            }

        
    });
     // Set button appearance
        sendButton.setForeground(Color.BLACK); //  text color of the button
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD, 14f)); //  font style and size of the button text
        sendButton.setBackground(new Color(144, 238, 144)); //  background color of the button
        sendButton.setFocusPainted(false); // Remove the focus border 
        sendButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add padding 

        // rounded border for the button
        int borderRadius = 20;
        int borderThickness = 2;
        sendButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, borderThickness),
                BorderFactory.createEmptyBorder(borderRadius, borderRadius, borderRadius, borderRadius)));

        // hover effect for the button
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(Color.GREEN); //  background color when mouse enters 
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(144, 238, 144)); //  background color when mouse exits 
            }
        });
        
        // Add  button to JSONDisplayPanel
        add(sendButton, BorderLayout.NORTH);
    
    }
    
    private void createTable(JSONArray jsonArray) {
        DefaultTableModel tableModel = new DefaultTableModel();
        table = new JTable(tableModel);

        // Set column names
        JSONArray headers = (JSONArray) jsonArray.get(0);
        for (Object header : headers) {
            tableModel.addColumn(header.toString());
        }

        // Add  data to table model
        for (int i = 1; i < jsonArray.size(); i++) {
            JSONArray row = (JSONArray) jsonArray.get(i);
            Object[] rowData = new Object[row.size()];
            for (int j = 0; j < row.size(); j++) {
                rowData[j] = row.get(j);
            }
            tableModel.addRow(rowData);
        }
    }

    public void hideCytoPanel() {
		manager.unregisterService(this, CytoPanelComponent.class);
		registered = false;
	}

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;  
    }

    @Override
    public String getTitle() {
        return "JSON Display";
    }

    @Override
    public Icon getIcon() {
        return null;  
    }
}
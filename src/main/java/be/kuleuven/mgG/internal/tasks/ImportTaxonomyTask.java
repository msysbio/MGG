package be.kuleuven.mgG.internal.tasks;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.utils.CSVReader;

/**
 * Imports the taxonomy of the nodes of a network, for a network without an abundance table: a file of two columns,
 * the node names and their taxonomies (tab- or comma-separated, with or without a header). The server maps the
 * taxonomies to genomes, as it does with the last column of an abundance table.
 */
public class ImportTaxonomyTask extends AbstractTask {

    private final MGGManager mggManager;
    private final String filePath;

    public ImportTaxonomyTask(String filePath, MGGManager mggManager) {
        this.filePath = filePath;
        this.mggManager = mggManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(TaskMonitor taskMonitor) {
        taskMonitor.setTitle("Importing Taxonomy File");
        taskMonitor.setStatusMessage("Reading file");

        List<String[]> rows;
        try {
            rows = CSVReader.readCSV(taskMonitor, filePath);
        } catch (IOException e) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error while reading the file: " + e.getMessage());
            return;
        }

        // A header: a first row whose second cell is not a taxonomy (no rank delimiter in it)
        if (!rows.isEmpty() && rows.get(0).length >= 2 && !looksLikeTaxonomy(rows.get(0)[1])) {
            rows.remove(0);
        }

        JSONArray taxonomy = new JSONArray();
        JSONArray header = new JSONArray();
        header.add("seqId");
        header.add("taxonomy");
        taxonomy.add(header);
        int skipped = 0;
        for (String[] row : rows) {
            if (row.length < 2 || row[0].trim().isEmpty() || row[1].trim().isEmpty()) {
                skipped++;
                continue;
            }
            JSONArray entry = new JSONArray();
            entry.add(row[0].trim());
            entry.add(row[1].trim());
            taxonomy.add(entry);
        }

        if (taxonomy.size() == 1) {
            String message = "No node and taxonomy found in " + filePath + ".\n"
                + "The file needs two columns, tab- or comma-separated: the node names and their taxonomies, e.g.\n"
                + "bin_101\td__Bacteria;p__Bacillota;c__Bacilli;o__Lactobacillales;f__Streptococcaceae;g__Lactococcus;s__";
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, message.replace("\n", " "));
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, "Taxonomy not loaded",
                JOptionPane.ERROR_MESSAGE));
            return;
        }

        JSONObject taxonomyObject = new JSONObject();
        taxonomyObject.put("taxonomy", taxonomy);
        mggManager.setTaxonomyJsonObject(taxonomyObject);

        taskMonitor.setProgress(1.0);
        String message = "Taxonomies of " + (taxonomy.size() - 1) + " nodes loaded correctly"
            + (skipped > 0 ? " (" + skipped + " line(s) without a node or a taxonomy skipped)" : "") + "."
            + (mggManager.getJsonObject() != null
                ? "\nAn abundance table is imported too: its taxonomies are the ones sent to the server." : "")
            + mggManager.pendingNetworkNote();
        taskMonitor.setStatusMessage(message.replace("\n", " "));
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, "Information",
            JOptionPane.INFORMATION_MESSAGE));
    }

    private static boolean looksLikeTaxonomy(String cell) {
        return cell.contains(";") || cell.contains("|") || cell.contains("__");
    }
}

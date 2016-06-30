/*
 * Created on Jun 28, 2016
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.DialogControlPane;

/**
 * This class is used to update stable identifiers if something changes in some
 * instances. It is implemented as an AttributeEditListener to monitor changes.
 * However, the implementation has not checked instances that have not checked out
 * fully from gk_central. So for those instances, a QA check is needed.
 * @author gwu
 *
 */
public class StableIdentifierUpdater implements AttributeEditListener {
    private Component parentComp;
    private boolean needDialog = true;
    
    /**
     * Default constructor.
     */
    public StableIdentifierUpdater() {
    }

    @Override
    public void attributeEdit(AttributeEditEvent e) {
        GKInstance instance = e.getEditingInstance();
        // Just in case the data model changes in the future. Right now, this is a generic attribute.
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.stableIdentifier))
            return;
        try {
            GKInstance stid = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            if (stid == null)
                return; // Since we want to update stable id.
            // Update needs for these cases
            String attName = e.getAttributeName();
            if (instance.getSchemClass().isa(ReactomeJavaConstants.Regulation) &&
                attName.equals(ReactomeJavaConstants.regulatedEntity)) {
                updateStableId(instance);
            }
            else if (attName.equals(ReactomeJavaConstants.species))
                updateStableId(instance);
        }
        catch(Exception e1) {
            e1.printStackTrace(); // If we cannot update stable id, just ignore it and let QA handles it.
        }
    }
    
    private void updateStableId(GKInstance instance) throws Exception {
        GKInstance stid = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        if (stid == null)
            return; // Just in case
        StableIdentifierGenerator idGenerator = new StableIdentifierGenerator();
        String newIdentifier = idGenerator.generateIdentifier(instance);
        String oldIdentifier = (String) stid.getAttributeValue(ReactomeJavaConstants.identifier);
        if (newIdentifier.equals(oldIdentifier))
            return; // Nothing needs to be changed
        // Update it
        stid.setAttributeValue(ReactomeJavaConstants.identifier, newIdentifier);
        InstanceDisplayNameGenerator.setDisplayName(stid); // Need to update display name too
        AttributeEditManager.getManager().attributeEdit(stid, 
                                                        ReactomeJavaConstants.identifier);
        if (needDialog) {
            showDialog(stid);
        }
    }

    public Component getParentComp() {
        return parentComp;
    }

    public void setParentComp(Component parentComp) {
        this.parentComp = parentComp;
    }

    private void showDialog(GKInstance stid) {
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parentComp));
        dialog.setTitle("StableIdentifier Update");
        
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEtchedBorder());
        pane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 16, 4, 16);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0d;
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBackground(pane.getBackground());
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        String text = "The following StableIdentifier has updated because of your editing. " + 
                      "Don't forget to commit it to the database:\n\n" + stid;
        ta.setText(text);
        constraints.gridheight = 4;
        pane.add(ta, constraints);
        final JCheckBox box = new JCheckBox("Don't show this dialog again.");
        box.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                needDialog = !box.isSelected();
            }
        });
        constraints.gridy = 5;
        constraints.gridheight = 1;
        pane.add(box, constraints);
        dialog.getContentPane().add(pane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        dialog.setSize(325, 225);
        dialog.setLocationRelativeTo(parentComp);
        dialog.setModal(true);
        dialog.setVisible(true);
    }
    
}

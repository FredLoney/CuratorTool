/*
 * Created on Jul 7, 2008
 *
 */
package org.gk.render;


/**
 * This class is used to describe a process. The functions of this class are
 * refactored from RenderablePathway.
 * @author wgm
 *
 */
public class ProcessNode extends Node {
    
    public ProcessNode() {
        boundsBuffer = 8;
        // Make sure a ProcessNode cannot be copy/paste to make
        // preceding/following event links clear!
//        isTransferrable = false; // As of May 7, 2015, this is turned to off so that a sub-pathway can be linked to multiple places.
    }
    
    public ProcessNode(String displayName) {
        this();
        setDisplayName(displayName);
    }
        
    public String getType() {
        return "Process";
    }
    
}

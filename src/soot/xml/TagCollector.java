/* Soot - a J*va Optimization Framework
 * Copyright (C) 2004 Jennifer Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.xml;

import soot.*;
import soot.tagkit.*;
import java.util.*;
import java.io.*;
import soot.jimple.spark.ondemand.genericutil.Predicate;

public class TagCollector {

    private final ArrayList<Attribute> attributes;
    private final ArrayList<Key> keys;
    
    public TagCollector(){
        attributes = new ArrayList<Attribute>();
        keys = new ArrayList<Key>();
    }

    /** Convenience function for <code>collectTags(sc, true)</code>. */
    public void collectTags(SootClass sc){ 
		collectTags(sc, true); 
	}

	/** Collect tags from all fields and methods of <code>sc</code>.
	 * If <code>includeBodies</code> is true, then tags are also collected 
	 * from method bodies.
	 * @param sc   The class from which to collect the tags.
	 */
    public void collectTags(SootClass sc, boolean includeBodies){	        
		// tag the class
		collectClassTags(sc);

        // tag fields
        Iterator fit = sc.getFields().iterator();
		while (fit.hasNext()){
            SootField sf = (SootField)fit.next();
            collectFieldTags(sf);
        }
        
        // tag methods
        Iterator it = sc.getMethods().iterator();
		while (it.hasNext()) {
			SootMethod sm = (SootMethod)it.next();
			collectMethodTags(sm);
		
            if (!includeBodies || !sm.hasActiveBody()) continue;
			Body b = sm.getActiveBody();
            collectBodyTags(b);
        }
    }

    public void collectKeyTags(SootClass sc){
        Iterator it = sc.getTags().iterator();
        while (it.hasNext()){
            Object next = it.next();
            if (next instanceof KeyTag){
                KeyTag kt = (KeyTag)next;
                Key k = new Key(kt.red(), kt.green(), kt.blue(), kt.key());
                k.aType(kt.analysisType());
                keys.add(k);
            }
        }
    }

    public void printKeys(PrintWriter writerOut){
        Iterator<Key> it = keys.iterator();
        while (it.hasNext()){
            Key k = it.next();
            k.print(writerOut);
        }
    }
    
            
    private void collectHostTags(Host h) {
		Predicate<Tag> p = Predicate.truePred();
		collectHostTags(h, p);
	}

    private void collectHostTags(Host h, Predicate<Tag> include){
		if (!h.getTags().isEmpty()){
			Iterator tags = h.getTags().iterator();
            Attribute a = new Attribute();
		    while (tags.hasNext()){
			    Tag t = (Tag)tags.next();
			    if (include.test(t))
					a.addTag(t);
			}
            attributes.add(a);
		}
    }

	public void collectClassTags(SootClass sc) {
		// All classes are tagged with their source files which 
		// is not worth outputing because it can be inferred from 
		// other information (like the name of the XML file).
		Predicate<Tag> noSFTags = new Predicate<Tag>() {
				public boolean test (Tag t) {
					return !(t instanceof SourceFileTag);
				}
		};
		collectHostTags(sc, noSFTags);
	}

    public void collectFieldTags(SootField sf) {
		collectHostTags(sf);
	}

    public void collectMethodTags(SootMethod sm){
	    if (sm.hasActiveBody())
			collectHostTags(sm);			
    }
    
    public void collectBodyTags(Body b){
		Iterator itUnits = b.getUnits().iterator();
		while (itUnits.hasNext()) {
			Unit u = (Unit)itUnits.next();
			Iterator itTags = u.getTags().iterator();
            Attribute ua = new Attribute();
            JimpleLineNumberTag jlnt = null;
	    	while (itTags.hasNext()) {
	   		    Tag t = (Tag)itTags.next();
                ua.addTag(t);
                if (t instanceof JimpleLineNumberTag){
                    jlnt = (JimpleLineNumberTag)t;
                }
                //System.out.println("adding unit tag: "+t);
            }
            attributes.add(ua);
			Iterator valBoxIt = u.getUseAndDefBoxes().iterator();
			while (valBoxIt.hasNext()){
				ValueBox vb = (ValueBox)valBoxIt.next();
                //PosColorAttribute attr = new PosColorAttribute();
				if (!vb.getTags().isEmpty()){
			    	Iterator tagsIt = vb.getTags().iterator(); 
                    Attribute va = new Attribute();
			    	while (tagsIt.hasNext()) {
						Tag t = (Tag)tagsIt.next();
                        //System.out.println("adding vb tag: "+t);
					    va.addTag(t);
                        //System.out.println("vb: "+vb.getValue()+" tag: "+t);
                        if (jlnt != null) {
                            va.addTag(jlnt);
                        }
                    }
                    // also here add line tags of the unit
                    attributes.add(va);
                    //System.out.println("added att: "+va);
                }
            }
        }
    }
    
    public void printTags(PrintWriter writerOut){
        
        Iterator<Attribute> it = attributes.iterator();
        while (it.hasNext()){
            Attribute a = it.next();
            //System.out.println("will print attr: "+a);
            a.print(writerOut);
        }
    }
}

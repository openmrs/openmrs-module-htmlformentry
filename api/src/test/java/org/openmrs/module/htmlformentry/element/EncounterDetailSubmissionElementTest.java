package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;




public class EncounterDetailSubmissionElementTest extends BaseModuleContextSensitiveTest {
	
	Location ls=new Location();
	
	@SuppressWarnings("deprecation")
	@Test
	
	public void  getAllChildAndGrandLocation_shouldReturnNull() {
		
		List<Location> locations=new ArrayList<Location>();
		
		//Visit locations
    	Location visitLocationOne=new Location();
    	Location visitLocationTwo=new Location();
    	Location visitLocationThree=new Location();
    	//Child locations
    	Location visitChildLocationOne=new Location();
    	Location visitChildLocationTwo=new Location();
    	Location visithildLocationThree=new Location();
    	//Grand child locations
       	Location visitGrandChildLocationOne=new Location();
    	Location visitGrandChildLocationTwo=new Location();
    	Location visitGrandChildLocationThree=new Location();
    	//Setting names for the visit Locations
    	visitLocationOne.setName("Mulago Hospital");
    	visitLocationTwo.setName("Theater");
    	visitLocationThree.setName("Pharmacy");
    	//adding names for child locations child locations
    	visitChildLocationOne.setName("In patient");
        visitChildLocationTwo.setName("Out patient");
        visithildLocationThree.setName("Dispensary");
          
        //adding names for the grand locations  
     	visitGrandChildLocationOne.setName("room 3");
        visitGrandChildLocationTwo.setName("room 1");
        visitGrandChildLocationThree.setName("room 2");
		
		//adding child locations to a set
    	
    	Set<Location>childLocations=new HashSet<Location>();
    	childLocations.add(visitChildLocationOne);
    	childLocations.add(visitChildLocationTwo);
    	childLocations.add(visithildLocationThree);
    	
    	//adding grand children to a set
    	Set<Location>grandChildLocations=new HashSet<Location>();
    	grandChildLocations.add(visitGrandChildLocationOne);
    	grandChildLocations.add(visitGrandChildLocationTwo);
    	grandChildLocations.add(visitGrandChildLocationThree);
 
        //adding grand locations to visitChildLocationOne 
    	visitChildLocationOne .setChildLocations(grandChildLocations);

   
    	//adding child and grand to the parent location visitLocationOne
    	visitLocationOne.setChildLocations(childLocations);

    	
    	Set<Location>allocations=new HashSet<Location>();
    
    	allocations.add(visitLocationOne);
    	
    	

    	
	
    	//checking for all the grand and child within the availble locations
    	Set<Location>allchildAndGrandVisitLocations=new HashSet<Location>();
    	allchildAndGrandVisitLocations=EncounterDetailSubmissionElement.getAllChildAndGrandLocations(allocations);
   
    	locations.addAll(allchildAndGrandVisitLocations);
    	assertThat(locations.size(), is(3));
		 
	}

}

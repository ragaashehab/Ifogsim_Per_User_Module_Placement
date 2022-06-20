package org.fog.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;
///////////////////////////////////////////////////////////////////////////////////////////
//import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
//import org.cloudbus.cloudsim.sdn.example.VmSchedulerSpaceSharedEnergy;
//import org.fog.application.AppModule;
///////////////////////////////////////////////////////////////////////////////////////////
public class StreamOperatorScheduler extends VmSchedulerTimeSharedOverbookingEnergy{    
	////public class StreamOperatorScheduler extends VmSchedulerTimeSharedOverSubscription{

	//private List<Double> currentAllocatedMips= new ArrayList<Double>();

	public StreamOperatorScheduler(List<? extends Pe> pelist) {
		super(pelist);
	}
	
}

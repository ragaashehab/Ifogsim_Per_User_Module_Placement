package org.fog.scheduler;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.sdn.example.VmSchedulerSpaceSharedEnergy;
//import org.cloudbus.cloudsim.VmSchedulerTimeShared;
//import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
//import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;

public class StreamOperatorSchedulerSpaceShared extends VmSchedulerSpaceSharedEnergy{

	public StreamOperatorSchedulerSpaceShared(List<? extends Pe> pelist) {
		super(pelist);
	}
}

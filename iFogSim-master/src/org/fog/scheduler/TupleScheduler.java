package org.fog.scheduler;

import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ResCloudlet;

   public class TupleScheduler extends CloudletSchedulerTimeShared{
	//public class TupleScheduler extends CloudletSchedulerDynamicWorkload{
	
		public TupleScheduler(double mips, int numberOfPes) {
		//super(); // if CloudletSchedulerTimeShared 
		super(mips, numberOfPes);  // if my extended CloudletSchedulerTimeShared or dynamicworkload
		//System.out.println("mips "+mips+"  pes "+numberOfPes);
	}

	/**
	 * Get estimated cloudlet completion time.
	 * 
	 * @param rcl the rcl
	 * @param time the time
	 * @return the estimated finish time
	 */
	public double getEstimatedFinishTime(ResCloudlet rcl, double time) {
		//System.out.println("REMAINING CLOUDLET LENGTH : "+rcl.getRemainingCloudletLength()+"\tCLOUDLET LENGTH"+rcl.getCloudletLength());
		//System.out.println("CURRENT ALLOC MIPS FOR CLOUDLET : "+getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		
		/*>>>>>>>>>>>>>>>>>>>>*/
		/* edit made by HARSHIT GUPTA */
		
		System.out.println("CLOUDLET "+rcl.getCloudletId()+ " on vmid "+rcl.getCloudlet().getVmId()+ " length "+ rcl.getCloudletLength()+" remaining  "+ rcl.getRemainingCloudletLength());
		System.out.println("ALLOCATED MIPS FOR CLOUDLET "+rcl.getCloudletId()+" = "+getTotalCurrentAllocatedMipsForCloudlet(rcl, time));//);
		return time
				+ ((rcl.getRemainingCloudletLength()) / getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		
		
				
		//return ((rcl.getRemainingCloudletLength()) / getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		/*end of edit*/
		/*<<<<<<<<<<<<<<<<<<<<<*/
	}
	
//	public void cloudletFinish(ResCloudlet rcl) {
//		rcl.setCloudletStatus(Cloudlet.SUCCESS);
//		rcl.finalizeCloudlet();
//		getCloudletFinishedList().add(rcl);
//	}
	
}

package org.fog.placement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.HostDynamicWorkload;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

public class ControllerEdgeward extends Controller{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;

	private Map<String, ModulePlacement> appModulePlacementPolicy;
	
	private List<Integer> idOfEndDevices;// = new ArrayList<Integer>(); //ragaa
	private Map<Integer, Double> deadlineInfo;  //ragaa  mobile id has  priority
	private String placement; 
	
	
	public ControllerEdgeward(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
		super(name,fogDevices,sensors,actuators);
		this.applications = new HashMap<String, Application>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		for(FogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
		connectWithLatencies();
		
		idOfEndDevices = new ArrayList<Integer>(); //ragaa
		deadlineInfo = new	HashMap<Integer, Double>();  // mobile id need deadline double  mobile priority		// 
		//deadlineInfo=deadlineInfo.entrySet()
		  // .stream().sorted(Map.Entry.comparingByValue())
		   //.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
	}

	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices()){
			if(id==fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		for(FogDevice fogDevice : getFogDevices()){
			FogDevice parent = getFogDeviceById(fogDevice.getParentId());
			if(parent == null)
				continue;
			double latency = fogDevice.getUplinkLatency();
			parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			parent.getChildrenIds().add(fogDevice.getId());
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);		 

		for(FogDevice dev : getFogDevices()) {
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
			//while(true)			
				//dev.checkDevicePerformance();
		}
	} 		
	
	@Override
	public void processEvent(SimEvent ev) {
		//System.out.println("nowwwwwwwww ManagePerformance");
		//ManagePerformance(); 			//ragaa
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);			
			break;
		
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);			
			break;
		
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();			
			break;
			
		case FogEvents.STOP_SIMULATION:			
			//shutdownEntity(); //ragaa
			CloudSim.stopSimulation();			
			printTimeDetails();			
			//printinstcount();
			printvmlist();
			//printCputilization();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();			
			System.exit(0);
			break;			
		}
	}
	
	private void printCputilization() {
		// TODO Auto-generated method stub
		System.out.println("=========================================");
		System.out.println("CPU Utilization controlleredgeward");
		System.out.println("=========================================");
		for(FogDevice fogDevice : fogDevices){			
			fogDevice.Cputilization=fogDevice.Cputilization.entrySet()
					.stream().sorted(Map.Entry.comparingByKey())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			}
		for(FogDevice fogDevice : fogDevices){
			System.out.print(fogDevice.getName()+" : "+ "\t");
			for(double x: fogDevice.Cputilization.keySet()) {
				System.out.print((int)x+"\t");
				}
			System.out.println();
			System.out.print("uti"+ "\t");
			for(double x: fogDevice.Cputilization.keySet()) {				
				System.out.print(fogDevice.Cputilization.get(x)+"\t");
			}
			System.out.println();
			//System.out.print("rem uti ");
			for(int y=0; y<fogDevice.rem.size();y++) {				
				//System.out.print(fogDevice.rem.get(y) +"\t");
			}
			System.out.println();
			System.out.println();
		}		
	}

	private void printvmlist(){
		System.out.println("=========================================");		
		if (placement.equals("Module Placement Edgeward")) {			
			System.out.println("Vm_List,  Per_Module Placement Edgeward ");
			System.out.println("=========================================");
			for(FogDevice fogDevice : fogDevices){
				//System.out.println();
				System.out.print(fogDevice.getName()+" : ");
				for(Vm vm:fogDevice.getHost().getVmList()) {
					System.out.print(((AppModule)vm).getName()+"   ");
					}
				System.out.println();
				}
			}
		else {		
			System.out.println("Vm_List, Only Cloud Module placement " ); //+ placement
		System.out.println("=========================================");
		for(FogDevice fogDevice : fogDevices){			
			System.out.println();
			}
		}
	}
	
private void printinstcount() {
		// TODO Auto-generated method stub
	System.out.print("Number of instances, ");
	if (placement =="Module Placement Edgeward") {
		System.out.println(placement);
		System.out.println("=========================================");
		int Monitoring=0; int Filtering=0; int Client=0; int Caregiver=0; int Cloud_Analytics =0;		
		Application app = getApplications().get("ECG");
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(app.getAppId());
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();			
			for(FogDevice device : fogDevices){				
				//FogDevice device = getFogDeviceById(deviceId);
				Monitoring= Filtering= Client= Caregiver= Cloud_Analytics =0;
				System.out.print(String.format("%-15s%s" , device.getName(), "\t"));
				/*for(AppModule module : deviceToModuleMap.get(deviceId)){	} */
				for(Vm vm: device.getHost().getVmList()) {
					AppModule module = (AppModule)vm;
					if(module.getName().equals("Client"))  Client++;
					if(module.getName().equals("Filtering")) Filtering++;
					if(module.getName().equals("Monitoring")) Monitoring++;
					if(module.getName().equals("Caregiver"))  Caregiver++;
					if(module.getName().equals("Cloud_Analytics")) Cloud_Analytics++;  //Cloud_Analytics= module.getNumInstances();
				}
				
				System.out.print(" Client  "+ Client+ "\t");
				System.out.print(" Filtering "+ Filtering+ "\t");
				System.out.print(", Monitoring "+ Monitoring+ "\t");
				System.out.print(", Caregiver "+ Caregiver+ "\t");
				System.out.println(", Cloud_Analytics "+ Cloud_Analytics+ "\t");
				Monitoring= Filtering= Client= Caregiver= Cloud_Analytics =0;			
		}
	}
	else { 
		System.out.println("Only Cloud module placement");
		System.out.println("=========================================");
		int Monitoring=0; int Filtering=0; int Client=0; int Caregiver=0; int Cloud_Analytics =0;
		for(FogDevice fogDevice : fogDevices){
			
			System.out.print(" Client  "+ Client+ "\t");
			System.out.print(" Filtering "+ Filtering+ "\t");
			System.out.print(", Monitoring "+ Monitoring+ "\t");
			System.out.print(", Caregiver "+ Caregiver+ "\t");
			System.out.println(", Cloud_Analytics "+ Cloud_Analytics+ "\t");
			 Monitoring= Filtering= Client= Caregiver= Cloud_Analytics =0;
			}
	 }
	}
	
	
	private void printNetworkUsageDetails() {		
		System.out.println("Total network usage = "+"\t"+NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME);
		System.out.println("=====================================================");

	}

	private FogDevice getCloud(){
		for(FogDevice dev : getFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}
	
	private void printCostDetails(){
		System.out.println("=====================================================");
		System.out.println("Cost of execution in cloud = "+ "\t"+ getCloud().getTotalCost());
	}
	
	private void printPowerDetails() {
		System.out.println("=====================================================");
		System.out.println("Name" + "\t"+ "Energy Consumed"); //String.format("%-20s%s" ,
		System.out.println("=====================================================");
		for(FogDevice fogDevice : getFogDevices()){
			//if(fogDevice.getName()=="cloud" ||fogDevice.getName()=="proxy-server" || fogDevice.getName().equals("d-0") ||fogDevice.getName().equals("m-0-0")) //fogDevice.getName().startsWith("m")			
			System.out.println(fogDevice.getName()+"\t"+ fogDevice.getEnergyConsumption());
		}
	}

	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	private void printTimeDetails() {
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		//System.out.println("=========================================");
		//System.out.println("EXECUTION TIME : "+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		System.out.println("APPLICATION LOOP DELAYS (Mean)");
		System.out.println("=========================================");
		double mean=0.0;
		 for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){   
			//mean = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
			System.out.println(getStringForLoopId(loopId) + "\t"+TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
			//System.out.println(getStringForLoopId(loopId) + " ---> "+TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loopId));			
			//System.out.println(getStringForLoopId(loopId) + " \t "+TimeKeeper.getInstance().getCurrentAverageUser());
			//System.out.println(getStringForLoopId(loopId) + " ---> "+TimeKeeper.getInstance().getCurrentNumUser());
			
			//for(int i: TimeKeeper.getInstance().getCurrentAverageUser().keySet())
				//System.out.print(TimeKeeper.getInstance().getCurrentAverageUser().get(i)+ "\t");
///////////////////////////////////////////////////////////////////////////////////////////
			/*	 // per user delay arranged by the user priority ragaa
			for(String appId : getApplications().keySet()){
				Application app = getApplications().get(appId);			
				 Map<Integer, Double> appdeadline= app.getDeadlineInfo();
				 Map<Integer, Double> prioappdeadline = appdeadline.entrySet()
			                .stream()
			                .sorted(Map.Entry.comparingByValue())
			                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
				 System.out.println("mobid"+  "\t"+ "Name"+ "\t"+ "priority " +"\t"+ "                Delay ");				
				 for(int mobid: prioappdeadline.keySet()) {
					 for(int i: TimeKeeper.getInstance().getCurrentAverageUser().keySet()) {
						 if (mobid == i) {
							 System.out.println(mobid+  "\t"+ CloudSim.getEntityName(mobid)+ "\t"+ appdeadline.get(mobid)+ "\t"+ TimeKeeper.getInstance().getCurrentAverageUser().get(i));
							}
					}
				 }
			}
/////////////////////////////////////////////////////////////////////////////////////////*/
		 //} 
		// Map<Integer, Double> delayuser= TimeKeeper.getInstance().getCurrentAverageUser();
			if(this.placement.equals("Module Placement Edgeward")) {
			Map<Integer, Double> delayuser= TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loopId);
			List<Double> delays= new ArrayList<Double>();
			for(int mobid: delayuser.keySet()) {
				 delays.add(delayuser.get(mobid));
				 }
			double numi;  double num=0.0;
			 for (double i : delays) {
		            numi = Math.pow((i-mean) , 2);
		            num+=numi;
		        }
			System.out.println("Standard Deviation   "+ "\t"+ Math.sqrt(num/delays.size()));
			System.out.println("=========================================");			
			System.out.println("APPLICATION LOOP DELAYS (Users)");
			System.out.println("=========================================");
		 // per user delay arranged by the delay ragaa
				System.out.println("mobid"+  "\t"+ "Name"+ "\t"+ "       Delay ");				 
				delayuser = delayuser.entrySet()
			                .stream()
			                .sorted(Map.Entry.comparingByValue())
			                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));				 				 
				 for(int mobid: delayuser.keySet()) {					
					System.out.println(mobid+  "\t"+ CloudSim.getEntityName(mobid)+ "\t"+ delayuser.get(mobid));
					}
				 
			}
		
		/* System.out.println("=========================================");
		for (Integer cloudletId : TimeKeeper.getInstance().getTupleIdToCpuStartTime().keySet()) // Map<Integer, Double> tupleIdToCpuStartTime;
			System.out.println("cloudletid " + cloudletId + " CpuStartTime "+ TimeKeeper.getInstance().getTupleIdToCpuStartTime().get(cloudletId)+ "       executionTime "+ TimeKeeper.getInstance().getTupleIdToCpuEndTime().get(cloudletId));
		//TimeKeeper.getInstance().getTupleIdToCpuEndTime()
		//System.out.println("averageexecutiontimeperuser "+ /TimeKeeper.getInstance().getTupleIdToCpuStartTime().size());
		System.out.println("getTupleIdToCpuEndTime "+ TimeKeeper.getInstance().getTupleIdToCpuEndTime());		
		*/
/////////////////////////////////////////////////////////////////////////////////////////				 
			 
		System.out.println();
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");
		System.out.println("TupleType" + "\t"+ "EXECUTION DELAY");
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			System.out.println(tupleType +  "\t" +TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}
///////////////////////////////////////////////////////////////////////////////////
		System.out.println("=========================================");
		 } 
	}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected void manageResources(){
		
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		for(FogDevice device : fogDevices){
			if(device.Cputilization.containsKey(CloudSim.clock()))
				device.Cputilization.put(CloudSim.clock()+0.0000000000001, device.getHost().getUtilizationOfCpu());
			else
				device.Cputilization.put(CloudSim.clock(), device.getHost().getUtilizationOfCpu());
			}			
	}
	/*
	 for(FogDevice dev : getFogDevices()) {
			//System.out.println("controller deadlineInfo  "  +deadlineInfo);			
				//Integer fogId= fogDevice.getId();
				//for(Integer deviceId : deadlineInfo.keySet()) {			
					//FogDevice dev= getFogDeviceById(deviceId);	
					//System.out.println("controller manageperformance "  +deviceId + dev.getName());
					Pair<Integer,Vm> migrated=dev.checkDevicePerformance(); 
					if(migrated!= null) {
						System.out.println("controller manageperformance migrated "  + migrated.getKey()+ migrated.getValue());
						dev.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
						dev.getVmList().remove(migrated.getValue());				
						dev.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());				
						//System.out.println("appToModulesMapUsers " +appToModulesMapUsers);				
						Pair<Integer, String> devmod = new Pair<Integer, String>( migrated.getKey(),((AppModule)migrated.getValue()).getName());
						dev.appToModulesMapUsers.get("ECG").remove(devmod);
						}
					//if(!idOfEndDevices.contains(fogId))
	 */
	////////////////////////////////////////////////////////////////////////////////
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {
		////ragaa
		for(FogDevice fogDevice : fogDevices){
			fogDevice.getActiveApplications().clear();
			fogDevice.getHost().getVmScheduler().deallocatePesForAllVms();
			fogDevice.getApplicationMap().clear();
			fogDevice.getVmList().clear();
			fogDevice.getHost().vmDestroyAll();
		}
		////end ragaa
			
	}
	
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors){
			sensor.setApp(getApplications().get(sensor.getAppId()));
		}
		for(Actuator ac : actuators){
			ac.setApp(getApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(Actuator actuator : getActuators()){
					if(actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){		
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		System.out.println("=========================================");		
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(FogDevice fogDevice : fogDevices){
			//	sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
			Object[] data = new Object[2];
			data[0] =application;
			data[1]= modulePlacement; 
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, data); //ragaanew submit moduleplacement to the devices
		}
		
		
		
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);			
			 deadlineInfo= app.getDeadlineInfo();
			 idOfEndDevices= app.getidOfEndDevices();
			 placement = app.getplacement();
		}
	
	if(placement.equals("Module Placement Edgeward")) {		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			FogDevice device = getFogDeviceById(deviceId);
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
				//System.out.println("controlleredgeward submit module "+ module.getName()+ " to device "+ device.getName());
			}
		}
	}
	}
	
/*////////////ragaa  code transfered to fogdevice class as checkDevicePerformance()////////////////////
	private void ManagePerformance() {  // good = true   problem = false thus updatevmprocessing
		// TODO Auto-generated method stub
		System.out.println(CloudSim.clock()+"i am manageing the performance ======================");
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(app.getAppId());
			Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();			
			for(Integer deviceId : deviceToModuleMap.keySet()){
				FogDevice device = getFogDeviceById(deviceId);
				if (device.getPower()>=0.8) {
					System.out.println("device power" + device.getPower() + " return true " );
					for(AppModule module : deviceToModuleMap.get(deviceId)){
						List<Double> CurrentRequestedMips= module.getCurrentRequestedMips();
						List<Double> NewCurrentRequestedMips= new ArrayList<Double>();
						for(double x:CurrentRequestedMips) {
							x= x*0.5;
							NewCurrentRequestedMips.add(x);
							}
						System.out.println("NewCurrentRequestedMips "+NewCurrentRequestedMips );
						module.setCurrentRequestedMips(NewCurrentRequestedMips);							
					}
				} // end bad performance
				device.getHost().updateVmsProcessing(CloudSim.clock());
				manageResources();
				}// end devices
			}// end applications	
		//return false; boolean		
	}// end method
////////////end ragaa  code transfered to fogdevice class as checkDevicePerformance()////////////////////
	*/
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
}
//Map<Integer, List<Integer>> loopIdToTupleIds
// Map<Integer, Double> loopIdToCurrentAverage
// Map<Integer, Integer> loopIdToCurrentNum;

// double average = 0, count = 0;
/*for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
 	Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
	System.out.println(" startTime "+  startTime);
	Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
	System.out.println(" endTime "+  endTime);
	if(startTime == null || endTime == null) {
		System.out.println(" ooooh ");
		break;
}
	average += endTime-startTime;				
	count += 1;
	System.out.println(tupleId + " average  "+ average + "count: "+ count);
}
System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));
*/
